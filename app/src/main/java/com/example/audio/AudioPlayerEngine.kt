package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.model.AutoMasterSettings
import com.example.model.AutoMixSettings
import com.example.model.StemTrack
import com.example.model.StemType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

class AudioPlayerEngine {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_STEREO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    var isPlaying = false
        private set

    @Volatile
    var currentSamplePosition = 0

    @Volatile
    var isLooping = true

    private var currentStems: List<StemTrack> = emptyList()
    private var currentMixSettings: AutoMixSettings = AutoMixSettings()
    private var currentMasterSettings: AutoMasterSettings = AutoMasterSettings()
    private var totalDurationSamples = SAMPLE_RATE * 16

    var onProgressUpdate: ((Float, Long) -> Unit)? = null
    var onStemVUMeterUpdate: ((Map<String, Float>, Float) -> Unit)? = null

    fun updateStems(
        stems: List<StemTrack>,
        mixSettings: AutoMixSettings = currentMixSettings,
        masterSettings: AutoMasterSettings = currentMasterSettings
    ) {
        currentStems = stems
        currentMixSettings = mixSettings
        currentMasterSettings = masterSettings
        val maxLen = stems.maxOfOrNull { it.pcmData.size } ?: (SAMPLE_RATE * 16)
        if (maxLen > 0) {
            totalDurationSamples = maxLen
        }
    }

    fun play() {
        if (isPlaying) return
        isPlaying = true

        val bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(8192)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_CONFIG)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        playbackJob = scope.launch {
            val chunkSize = 2048
            val pcmOutShorts = ShortArray(chunkSize * 2) // Stereo (L + R interleaved)

            while (isActive && isPlaying) {
                val stemsSnapshot = currentStems
                val hasSolo = stemsSnapshot.any { it.isSolo }
                val activeStems = stemsSnapshot.filter { if (hasSolo) it.isSolo else !it.isMuted }

                val stemPeaks = mutableMapOf<String, Float>()
                var masterPeak = 0f

                val mix = currentMixSettings
                val master = currentMasterSettings

                for (frame in 0 until chunkSize) {
                    val sampleIdx = currentSamplePosition + frame
                    val actualIdx = if (totalDurationSamples > 0) sampleIdx % totalDurationSamples else 0

                    var left = 0f
                    var right = 0f

                    for (stem in activeStems) {
                        val pcm = stem.pcmData
                        val sample = if (actualIdx < pcm.size) pcm[actualIdx] * stem.volume else 0f
                        val absSample = abs(sample)

                        val prevPeak = stemPeaks[stem.id] ?: 0f
                        if (absSample > prevPeak) {
                            stemPeaks[stem.id] = absSample
                        }

                        // Auto-Mix stereo spread
                        if (stem.type == StemType.CHORDS || stem.type == StemType.MELODY) {
                            val width = mix.stereoWidth
                            val panL = 1.0f + (if (stem.type == StemType.CHORDS) -0.18f else 0.18f) * width
                            val panR = 1.0f + (if (stem.type == StemType.CHORDS) 0.18f else -0.18f) * width
                            left += sample * panL
                            right += sample * panR
                        } else {
                            left += sample
                            right += sample
                        }
                    }

                    // Auto-Master saturation
                    if (master.isEnabled && master.analogWarmth > 0.05f) {
                        val drive = 1.0f + master.analogWarmth * 0.7f
                        left = kotlin.math.tanh(left * drive) / drive
                        right = kotlin.math.tanh(right * drive) / drive
                    }

                    // Brickwall limiter
                    val maxS = max(abs(left), abs(right))
                    if (maxS > masterPeak) masterPeak = maxS

                    val leftClamped = left.coerceIn(-0.98f, 0.98f)
                    val rightClamped = right.coerceIn(-0.98f, 0.98f)

                    pcmOutShorts[frame * 2] = (leftClamped * 32767.0f).toInt().toShort()
                    pcmOutShorts[frame * 2 + 1] = (rightClamped * 32767.0f).toInt().toShort()
                }

                audioTrack?.write(pcmOutShorts, 0, pcmOutShorts.size)

                currentSamplePosition += chunkSize
                if (currentSamplePosition >= totalDurationSamples) {
                    if (isLooping) {
                        currentSamplePosition = 0
                    } else {
                        pause()
                        currentSamplePosition = 0
                    }
                }

                val progress = if (totalDurationSamples > 0) currentSamplePosition.toFloat() / totalDurationSamples else 0f
                val currentMs = (currentSamplePosition * 1000L) / SAMPLE_RATE
                onProgressUpdate?.invoke(progress.coerceIn(0f, 1f), currentMs)
                onStemVUMeterUpdate?.invoke(stemPeaks, masterPeak.coerceIn(0f, 1f))
            }
        }
    }

    fun pause() {
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.e("AudioPlayerEngine", "Error pausing AudioTrack: ${e.message}")
        }
    }

    fun seekToFraction(fraction: Float) {
        val target = (fraction.coerceIn(0f, 1f) * totalDurationSamples).toInt()
        currentSamplePosition = target
    }

    fun release() {
        pause()
    }
}
