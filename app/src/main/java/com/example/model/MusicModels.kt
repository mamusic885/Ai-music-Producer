package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioTurquoise
import com.example.ui.theme.StudioVUMeterGreen
import com.example.ui.theme.StudioVUMeterYellow
import com.example.ui.theme.StudioAnalogWarm

enum class StemType(val displayName: String, val iconName: String) {
    VOCAL("Vocal", "mic"),
    DRUMS("Drums & Beat", "drum"),
    BASS("Deep Bass", "speaker"),
    CHORDS("Chords & Plucks", "piano"),
    MELODY("Melody & Guitar", "guitar"),
    FX("FX & Ambience", "sparkles")
}

data class StemTrack(
    val id: String,
    val type: StemType,
    val name: String,
    val color: Color,
    val volume: Float = 0.85f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val pcmData: FloatArray = FloatArray(0),
    val peakLevel: Float = 0f,
    val waveformPoints: List<Float> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StemTrack) return false
        return id == other.id &&
                type == other.type &&
                name == other.name &&
                volume == other.volume &&
                isMuted == other.isMuted &&
                isSolo == other.isSolo &&
                peakLevel == other.peakLevel &&
                waveformPoints == other.waveformPoints
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + volume.hashCode()
        result = 31 * result + isMuted.hashCode()
        result = 31 * result + isSolo.hashCode()
        return result
    }
}

data class AutoTuneSettings(
    val isEnabled: Boolean = true,
    val targetKey: String = "A Minor",
    val speed: Float = 0.85f, // 0.0 (natural) to 1.0 (hard auto-tune / T-Pain / modern trap)
    val formantPreserved: Boolean = true
)

data class AutoMixSettings(
    val isEnabled: Boolean = true,
    val kickSidechainDucking: Boolean = true,
    val vocalClarityBoost: Boolean = true,
    val reverbSpace: Float = 0.28f,
    val stereoWidth: Float = 0.70f,
    val lowEndCut: Boolean = true
)

data class AutoMasterSettings(
    val isEnabled: Boolean = true,
    val analogWarmth: Float = 0.45f,
    val loudnessTarget: Float = 0.80f,
    val bassPunch: Float = 0.55f,
    val brickwallLimiter: Boolean = true
)

data class ArrangementRecipe(
    val genre: String = "Reggaeton",
    val bpm: Int = 96,
    val musicalKey: String = "A Minor",
    val chordProgression: List<String> = listOf("Am", "F", "C", "G"),
    val prompt: String = "Reggaeton beat, soft plucks in intro, guitar in background, deep bass, emotional chords",
    val drumStyle: String = "Dembow Tresillo Groove",
    val bassStyle: String = "808 Sub Punch",
    val chordStyle: String = "Soft Nylon Plucks & Warm Rhodes",
    val leadStyle: String = "Spanish Guitar Melody",
    val fxStyle: String = "Atmospheric Vinyl & Sub Drops",
    val autoTuneSettings: AutoTuneSettings = AutoTuneSettings(),
    val autoMixSettings: AutoMixSettings = AutoMixSettings(),
    val autoMasterSettings: AutoMasterSettings = AutoMasterSettings()
)

data class ProducerProject(
    val id: String = "proj_01",
    val title: String = "AI Arrangement #1",
    val recipe: ArrangementRecipe = ArrangementRecipe(),
    val stems: List<StemTrack> = emptyList(),
    val durationSeconds: Float = 16.0f,
    val sampleRate: Int = 44100
)

enum class GenerationStep(val stepName: String, val detail: String, val progress: Float) {
    IDLE("Ready", "Waiting for instructions", 0f),
    ANALYZING_VOCAL("Analyzing Vocal", "Detecting pitch, tempo, and key harmonics...", 0.2f),
    AI_COMPOSING("AI Composing Arrangement", "Generating chords, melody, and rhythm recipe...", 0.45f),
    SYNTHESIZING_STEMS("Synthesizing Stems", "Generating Dembow drums, 808 bass, and guitar plucks...", 0.7f),
    AUTO_TUNING("Applying Auto-Tune", "Pitch-snapping vocal to scale...", 0.85f),
    AUTO_MIX_MASTER("Auto-Mix & Auto-Master", "Sidechain ducking, tape warmth & limiter...", 0.95f),
    COMPLETED("Arrangement Ready", "Playback and export ready!", 1.0f)
}
