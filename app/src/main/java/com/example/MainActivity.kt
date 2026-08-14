package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.model.StemType
import com.example.ui.components.AiPromptArrangementSection
import com.example.ui.components.AutoMixMasterSection
import com.example.ui.components.AutoTuneSection
import com.example.ui.components.ExportBottomSheet
import com.example.ui.components.MultiTrackMixerSection
import com.example.ui.components.StudioTopBar
import com.example.ui.components.TransportBottomDock
import com.example.ui.components.VocalInputSection
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StudioBgDark
import com.example.ui.viewmodel.MusicProducerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MusicProducerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MusicProducerScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicProducerScreen(viewModel: MusicProducerViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val vocalStem = uiState.stems.find { it.type == StemType.VOCAL }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = StudioBgDark,
        topBar = {
            StudioTopBar(
                recipe = uiState.recipe,
                masterPeakLevel = uiState.masterPeakLevel,
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            TransportBottomDock(
                isPlaying = uiState.isPlaying,
                playbackProgress = uiState.playbackProgress,
                playbackTimeMs = uiState.playbackTimeMs,
                totalDurationMs = uiState.totalDurationMs,
                isLooping = uiState.isLooping,
                onPlayPauseToggle = { viewModel.togglePlayPause() },
                onSeek = { viewModel.seekToFraction(it) },
                onLoopToggle = { viewModel.toggleLoop() },
                onExportClick = { viewModel.openExportDialog() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(StudioBgDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Vocal Recording / Import & Waveform Section
                VocalInputSection(
                    vocalStem = vocalStem,
                    isRecording = uiState.isRecording,
                    recordingDurationSec = uiState.recordingDurationSec,
                    liveMicLevel = uiState.liveMicLevel,
                    selectedVocalSource = uiState.selectedVocalSource,
                    playbackProgress = uiState.playbackProgress,
                    onStartRecord = { viewModel.startRecording() },
                    onStopRecord = { viewModel.stopRecording() },
                    onImportAudio = { viewModel.importAudioUri(context, it) },
                    onSelectPreset = { viewModel.loadPresetDemoVocal(it) }
                )

                // 2. Auto-Tune DSP Quantizer Section
                AutoTuneSection(
                    settings = uiState.autoTuneSettings,
                    onSettingsChanged = { viewModel.updateAutoTuneSettings(it) }
                )

                // 3. AI Producer Prompt & Arrangement Generation Section
                AiPromptArrangementSection(
                    promptText = uiState.promptText,
                    onPromptChanged = { viewModel.onPromptChanged(it) },
                    isGenerating = uiState.isGenerating,
                    generationStep = uiState.generationStep,
                    onGenerate = { viewModel.generateArrangement() }
                )

                // 4. Multi-Track Channel Strips (Vocal, Drums, Bass, Chords, Melody, FX)
                MultiTrackMixerSection(
                    stems = uiState.stems,
                    stemPeaks = uiState.stemPeaks,
                    onVolumeChanged = { id, vol -> viewModel.setStemVolume(id, vol) },
                    onMuteToggle = { id -> viewModel.toggleStemMute(id) },
                    onSoloToggle = { id -> viewModel.toggleStemSolo(id) }
                )

                // 5. Auto-Mix & Auto-Mastering Rack
                AutoMixMasterSection(
                    mixSettings = uiState.autoMixSettings,
                    masterSettings = uiState.autoMasterSettings,
                    isExpanded = uiState.isMixMasterRackExpanded,
                    onToggleExpand = { viewModel.toggleMixMasterRack() },
                    onMixSettingsChanged = { viewModel.updateAutoMixSettings(it) },
                    onMasterSettingsChanged = { viewModel.updateAutoMasterSettings(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Export Master Audio Bottom Sheet
    if (uiState.showExportDialog) {
        ExportBottomSheet(
            recipe = uiState.recipe,
            isExporting = uiState.isExporting,
            exportedFile = uiState.exportedFile,
            onDismiss = { viewModel.closeExportDialog() },
            onSaveToLibrary = { viewModel.saveMasterToLibrary(context) },
            onShare = { viewModel.shareExportedMaster(context) },
            sheetState = sheetState
        )
    }
}
