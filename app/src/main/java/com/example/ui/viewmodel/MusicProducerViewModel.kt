package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiMusicProducerService
import com.example.audio.AudioPlayerEngine
import com.example.audio.MusicSynthesizer
import com.example.audio.PitchDetectorAndAutoTune
import com.example.audio.VocalAudioSource
import com.example.audio.WavAudioExporter
import com.example.model.ArrangementRecipe
import com.example.model.AutoMasterSettings
import com.example.model.AutoMixSettings
import com.example.model.AutoTuneSettings
import com.example.model.GenerationStep
import com.example.model.StemTrack
import com.example.model.StemType
import com.example.ui.theme.StudioTurquoise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class MusicProducerUiState(
    val promptText: String = "Reggaeton beat, soft plucks in intro, guitar in background, deep bass, emotional chords",
    val isRecording: Boolean = false,
    val recordingDurationSec: Int = 0,
    val liveMicLevel: Float = 0f,
    val selectedVocalSource: String = "Preset Demo", // "Microphone", "Imported", "Preset Demo"
    val vocalPcm: FloatArray = FloatArray(0),
    val stems: List<StemTrack> = emptyList(),
    val recipe: ArrangementRecipe = ArrangementRecipe(),
    val isGenerating: Boolean = false,
    val generationStep: GenerationStep = GenerationStep.IDLE,
    val isPlaying: Boolean = false,
    val playbackProgress: Float = 0f,
    val playbackTimeMs: Long = 0L,
    val totalDurationMs: Long = 16000L,
    val isLooping: Boolean = true,
    val masterPeakLevel: Float = 0f,
    val stemPeaks: Map<String, Float> = emptyMap(),
    val autoTuneSettings: AutoTuneSettings = AutoTuneSettings(),
    val autoMixSettings: AutoMixSettings = AutoMixSettings(),
    val autoMasterSettings: AutoMasterSettings = AutoMasterSettings(),
    val isMixMasterRackExpanded: Boolean = false,
    val showExportDialog: Boolean = false,
    val isExporting: Boolean = false,
    val exportedFile: File? = null
)

class MusicProducerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MusicProducerUiState())
    val uiState: StateFlow<MusicProducerUiState> = _uiState.asStateFlow()

    private val playerEngine = AudioPlayerEngine()
    private var recordingTimerJob: Job? = null

    init {
        // Setup player callbacks
        playerEngine.onProgressUpdate = { progress, currentMs ->
            _uiState.value = _uiState.value.copy(
                playbackProgress = progress,
                playbackTimeMs = currentMs
            )
        }
        playerEngine.onStemVUMeterUpdate = { peaks, masterPeak ->
            _uiState.value = _uiState.value.copy(
                stemPeaks = peaks,
                masterPeakLevel = masterPeak
            )
        }

        // Initialize with default demo vocal and default arrangement so user hears sound instantly
        initializeDefaultProject()
    }

    private fun initializeDefaultProject() {
        viewModelScope.launch(Dispatchers.Default) {
            val demoVocal = VocalAudioSource.generateDemoVocal("Reggaeton", 16.0f)
            val recipe = ArrangementRecipe()
            val vocalStem = createVocalStem(demoVocal)
            val backingStems = MusicSynthesizer.generateArrangementStems(recipe, 16.0f)
            val allStems = listOf(vocalStem) + backingStems

            _uiState.value = _uiState.value.copy(
                vocalPcm = demoVocal,
                recipe = recipe,
                stems = allStems,
                autoTuneSettings = recipe.autoTuneSettings,
                autoMixSettings = recipe.autoMixSettings,
                autoMasterSettings = recipe.autoMasterSettings
            )
            playerEngine.updateStems(allStems, recipe.autoMixSettings, recipe.autoMasterSettings)
        }
    }

    fun onPromptChanged(newPrompt: String) {
        _uiState.value = _uiState.value.copy(promptText = newPrompt)
    }

    // ==========================================
    // VOCAL RECORDING & IMPORT
    // ==========================================
    fun startRecording() {
        if (_uiState.value.isRecording) return
        playerEngine.pause()
        _uiState.value = _uiState.value.copy(
            isRecording = true,
            recordingDurationSec = 0,
            isPlaying = false
        )

        recordingTimerJob = viewModelScope.launch {
            while (_uiState.value.isRecording) {
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    recordingDurationSec = _uiState.value.recordingDurationSec + 1
                )
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val recordedPcm = VocalAudioSource.startRecording { amp ->
                _uiState.value = _uiState.value.copy(liveMicLevel = amp)
            }

            withContext(Dispatchers.Main) {
                if (recordedPcm.isNotEmpty()) {
                    loadNewVocal(recordedPcm, "Microphone")
                }
            }
        }
    }

    fun stopRecording() {
        VocalAudioSource.stopRecording()
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        _uiState.value = _uiState.value.copy(
            isRecording = false,
            liveMicLevel = 0f
        )
    }

    fun importAudioUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            Toast.makeText(context, "Decoding audio file...", Toast.LENGTH_SHORT).show()
            val decodedPcm = VocalAudioSource.decodeAudioUri(context, uri)
            loadNewVocal(decodedPcm, "Imported Audio")
            Toast.makeText(context, "Vocal imported successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    fun loadPresetDemoVocal(style: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val pcm = VocalAudioSource.generateDemoVocal(style, 16.0f)
            loadNewVocal(pcm, "Preset: $style")
        }
    }

    private fun loadNewVocal(pcm: FloatArray, sourceName: String) {
        val vocalStem = createVocalStem(pcm)
        val updatedStems = _uiState.value.stems.map {
            if (it.type == StemType.VOCAL) vocalStem else it
        }.ifEmpty { listOf(vocalStem) }

        _uiState.value = _uiState.value.copy(
            vocalPcm = pcm,
            selectedVocalSource = sourceName,
            stems = updatedStems
        )
        playerEngine.updateStems(updatedStems, _uiState.value.autoMixSettings, _uiState.value.autoMasterSettings)
    }

    private fun createVocalStem(pcm: FloatArray): StemTrack {
        val processedPcm = if (_uiState.value.autoTuneSettings.isEnabled) {
            PitchDetectorAndAutoTune.applyAutoTune(
                vocalPcm = pcm,
                key = _uiState.value.autoTuneSettings.targetKey,
                speed = _uiState.value.autoTuneSettings.speed
            )
        } else {
            pcm
        }

        return StemTrack(
            id = "stem_vocal",
            type = StemType.VOCAL,
            name = if (_uiState.value.autoTuneSettings.isEnabled) "Auto-Tuned Vocal" else "Vocal Lead",
            color = StudioTurquoise,
            volume = 0.95f,
            pcmData = processedPcm,
            waveformPoints = MusicSynthesizer.computeWaveformOverview(processedPcm)
        )
    }

    // ==========================================
    // AI ARRANGEMENT GENERATION
    // ==========================================
    fun generateArrangement() {
        if (_uiState.value.isGenerating) return
        playerEngine.pause()
        _uiState.value = _uiState.value.copy(isPlaying = false, isGenerating = true)

        viewModelScope.launch(Dispatchers.Default) {
            // Step 1: Analyzing Vocal
            _uiState.value = _uiState.value.copy(generationStep = GenerationStep.ANALYZING_VOCAL)
            delay(500)

            // Step 2: AI Composing Arrangement via Gemini
            _uiState.value = _uiState.value.copy(generationStep = GenerationStep.AI_COMPOSING)
            val recipe = GeminiMusicProducerService.generateArrangementRecipe(_uiState.value.promptText)
            delay(400)

            // Step 3: Synthesizing Stems
            _uiState.value = _uiState.value.copy(generationStep = GenerationStep.SYNTHESIZING_STEMS)
            val durationSec = 16.0f
            val backingStems = MusicSynthesizer.generateArrangementStems(recipe, durationSec)
            delay(400)

            // Step 4: Applying Auto-Tune to Vocal
            _uiState.value = _uiState.value.copy(generationStep = GenerationStep.AUTO_TUNING)
            val currentVocal = if (_uiState.value.vocalPcm.isEmpty()) {
                VocalAudioSource.generateDemoVocal(recipe.genre, durationSec)
            } else {
                _uiState.value.vocalPcm
            }
            val autoTunedVocal = PitchDetectorAndAutoTune.applyAutoTune(
                vocalPcm = currentVocal,
                key = recipe.autoTuneSettings.targetKey,
                speed = recipe.autoTuneSettings.speed
            )
            val vocalStem = StemTrack(
                id = "stem_vocal",
                type = StemType.VOCAL,
                name = "Vocal (Auto-Tune ${recipe.autoTuneSettings.targetKey})",
                color = StudioTurquoise,
                volume = 0.95f,
                pcmData = autoTunedVocal,
                waveformPoints = MusicSynthesizer.computeWaveformOverview(autoTunedVocal)
            )

            // Step 5: Auto-Mix & Auto-Master
            _uiState.value = _uiState.value.copy(generationStep = GenerationStep.AUTO_MIX_MASTER)
            val fullStems = listOf(vocalStem) + backingStems
            delay(350)

            // Step 6: Completed
            _uiState.value = _uiState.value.copy(
                recipe = recipe,
                vocalPcm = currentVocal,
                stems = fullStems,
                autoTuneSettings = recipe.autoTuneSettings,
                autoMixSettings = recipe.autoMixSettings,
                autoMasterSettings = recipe.autoMasterSettings,
                isGenerating = false,
                generationStep = GenerationStep.COMPLETED,
                playbackProgress = 0f,
                playbackTimeMs = 0L
            )

            playerEngine.updateStems(fullStems, recipe.autoMixSettings, recipe.autoMasterSettings)
            playerEngine.seekToFraction(0f)
            playerEngine.play()
            _uiState.value = _uiState.value.copy(isPlaying = true)
        }
    }

    // ==========================================
    // TRANSPORT & MIXER CONTROLS
    // ==========================================
    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            playerEngine.pause()
            _uiState.value = _uiState.value.copy(isPlaying = false)
        } else {
            playerEngine.updateStems(
                _uiState.value.stems,
                _uiState.value.autoMixSettings,
                _uiState.value.autoMasterSettings
            )
            playerEngine.play()
            _uiState.value = _uiState.value.copy(isPlaying = true)
        }
    }

    fun seekToFraction(fraction: Float) {
        playerEngine.seekToFraction(fraction)
        _uiState.value = _uiState.value.copy(
            playbackProgress = fraction,
            playbackTimeMs = (fraction * _uiState.value.totalDurationMs).toLong()
        )
    }

    fun toggleLoop() {
        val newLoop = !_uiState.value.isLooping
        playerEngine.isLooping = newLoop
        _uiState.value = _uiState.value.copy(isLooping = newLoop)
    }

    fun setStemVolume(stemId: String, volume: Float) {
        val updatedStems = _uiState.value.stems.map {
            if (it.id == stemId) it.copy(volume = volume.coerceIn(0f, 1f)) else it
        }
        _uiState.value = _uiState.value.copy(stems = updatedStems)
        playerEngine.updateStems(updatedStems, _uiState.value.autoMixSettings, _uiState.value.autoMasterSettings)
    }

    fun toggleStemMute(stemId: String) {
        val updatedStems = _uiState.value.stems.map {
            if (it.id == stemId) it.copy(isMuted = !it.isMuted) else it
        }
        _uiState.value = _uiState.value.copy(stems = updatedStems)
        playerEngine.updateStems(updatedStems, _uiState.value.autoMixSettings, _uiState.value.autoMasterSettings)
    }

    fun toggleStemSolo(stemId: String) {
        val target = _uiState.value.stems.find { it.id == stemId } ?: return
        val willSolo = !target.isSolo
        val updatedStems = _uiState.value.stems.map {
            if (it.id == stemId) it.copy(isSolo = willSolo) else it
        }
        _uiState.value = _uiState.value.copy(stems = updatedStems)
        playerEngine.updateStems(updatedStems, _uiState.value.autoMixSettings, _uiState.value.autoMasterSettings)
    }

    // ==========================================
    // AUTO-TUNE & DSP SETTINGS
    // ==========================================
    fun updateAutoTuneSettings(settings: AutoTuneSettings) {
        _uiState.value = _uiState.value.copy(autoTuneSettings = settings)
        viewModelScope.launch(Dispatchers.Default) {
            val vocalPcm = _uiState.value.vocalPcm
            if (vocalPcm.isNotEmpty()) {
                val tuned = if (settings.isEnabled) {
                    PitchDetectorAndAutoTune.applyAutoTune(vocalPcm, settings.targetKey, settings.speed)
                } else {
                    vocalPcm
                }
                val updatedStems = _uiState.value.stems.map {
                    if (it.type == StemType.VOCAL) {
                        it.copy(
                            name = if (settings.isEnabled) "Auto-Tuned Vocal (${settings.targetKey})" else "Vocal Lead",
                            pcmData = tuned,
                            waveformPoints = MusicSynthesizer.computeWaveformOverview(tuned)
                        )
                    } else it
                }
                _uiState.value = _uiState.value.copy(stems = updatedStems)
                playerEngine.updateStems(updatedStems, _uiState.value.autoMixSettings, _uiState.value.autoMasterSettings)
            }
        }
    }

    fun updateAutoMixSettings(settings: AutoMixSettings) {
        _uiState.value = _uiState.value.copy(autoMixSettings = settings)
        playerEngine.updateStems(_uiState.value.stems, settings, _uiState.value.autoMasterSettings)
    }

    fun updateAutoMasterSettings(settings: AutoMasterSettings) {
        _uiState.value = _uiState.value.copy(autoMasterSettings = settings)
        playerEngine.updateStems(_uiState.value.stems, _uiState.value.autoMixSettings, settings)
    }

    fun toggleMixMasterRack() {
        _uiState.value = _uiState.value.copy(
            isMixMasterRackExpanded = !_uiState.value.isMixMasterRackExpanded
        )
    }

    // ==========================================
    // EXPORT
    // ==========================================
    fun openExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = true)
    }

    fun closeExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = false)
    }

    fun exportMasterWav(context: Context, onComplete: ((File) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            val file = WavAudioExporter.exportWavFile(
                context = context,
                title = _uiState.value.recipe.genre + "_Master",
                stems = _uiState.value.stems,
                mixSettings = _uiState.value.autoMixSettings,
                masterSettings = _uiState.value.autoMasterSettings
            )
            _uiState.value = _uiState.value.copy(
                isExporting = false,
                exportedFile = file
            )
            onComplete?.invoke(file)
        }
    }

    fun shareExportedMaster(context: Context) {
        val file = _uiState.value.exportedFile
        if (file != null) {
            val intent = WavAudioExporter.createShareIntent(context, file, _uiState.value.recipe.genre + " Master")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            exportMasterWav(context) { newFile ->
                val intent = WavAudioExporter.createShareIntent(context, newFile, _uiState.value.recipe.genre + " Master")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }

    fun saveMasterToLibrary(context: Context) {
        viewModelScope.launch {
            exportMasterWav(context) { file ->
                viewModelScope.launch {
                    val uri = WavAudioExporter.saveToMediaStore(context, file, _uiState.value.recipe.genre)
                    if (uri != null) {
                        Toast.makeText(context, "Saved to Music/AIMusicProducer!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Master audio saved to app cache.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerEngine.release()
    }
}
