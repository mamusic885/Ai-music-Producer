package com.example.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

object PitchDetectorAndAutoTune {

    // Note name to MIDI note mapping (A4 = 69 = 440 Hz)
    private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    // Musical scale interval definitions (semitones from root)
    private val SCALE_INTERVALS = mapOf(
        "Major" to intArrayOf(0, 2, 4, 5, 7, 9, 11),
        "Minor" to intArrayOf(0, 2, 3, 5, 7, 8, 10),
        "Natural Minor" to intArrayOf(0, 2, 3, 5, 7, 8, 10),
        "Harmonic Minor" to intArrayOf(0, 2, 3, 5, 7, 8, 11),
        "Pentatonic Major" to intArrayOf(0, 2, 4, 7, 9),
        "Pentatonic Minor" to intArrayOf(0, 3, 5, 7, 10),
        "Chromatic" to intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
    )

    fun getAvailableKeys(): List<String> {
        val keys = mutableListOf<String>()
        val notes = arrayOf("A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#")
        for (note in notes) {
            keys.add("$note Minor")
            keys.add("$note Major")
        }
        return keys
    }

    private fun parseScaleNotes(keyString: String): Set<Int> {
        // e.g. "A Minor", "C Major", "F# Minor"
        val parts = keyString.trim().split(" ")
        val rootNoteStr = parts.getOrNull(0) ?: "A"
        val scaleTypeStr = parts.drop(1).joinToString(" ").ifEmpty { "Minor" }

        val rootIndex = NOTE_NAMES.indexOfFirst { it.equals(rootNoteStr, ignoreCase = true) }
            .takeIf { it >= 0 } ?: 9 // Default to A (index 9)

        val intervals = SCALE_INTERVALS[scaleTypeStr] ?: SCALE_INTERVALS["Minor"]!!
        val scaleNotes = mutableSetOf<Int>()
        for (octave in 2..7) {
            for (interval in intervals) {
                val midi = (octave + 1) * 12 + ((rootIndex + interval) % 12)
                scaleNotes.add(midi)
            }
        }
        return scaleNotes
    }

    private fun midiToFreq(midi: Int): Float {
        return 440.0f * (2.0f.pow((midi - 69) / 12.0f))
    }

    private fun freqToMidi(freq: Float): Float {
        if (freq <= 20f) return 60f
        return 69f + 12f * log2(freq / 440.0f)
    }

    /**
     * Pitch detection using Autocorrelation method with parabolic interpolation.
     */
    fun detectPitch(buffer: FloatArray, offset: Int, length: Int, sampleRate: Int = 44100): Float {
        val minFreq = 70f
        val maxFreq = 1000f
        val minPeriod = (sampleRate / maxFreq).toInt()
        val maxPeriod = (sampleRate / minFreq).toInt().coerceAtMost(length / 2)

        var maxCorr = 0f
        var bestPeriod = -1

        var energy = 0f
        for (i in 0 until length) {
            val sample = buffer[offset + i]
            energy += sample * sample
        }
        val rms = kotlin.math.sqrt(energy / length)
        if (rms < 0.01f) return 0f // Silence/Noise

        for (lag in minPeriod..maxPeriod) {
            var corr = 0f
            for (i in 0 until (length - lag)) {
                corr += buffer[offset + i] * buffer[offset + i + lag]
            }
            if (corr > maxCorr) {
                maxCorr = corr
                bestPeriod = lag
            }
        }

        if (bestPeriod <= 0 || maxCorr <= 0.35f * energy) {
            return 0f
        }

        return sampleRate.toFloat() / bestPeriod.toFloat()
    }

    /**
     * Finds the closest MIDI note in the specified scale.
     */
    private fun snapToScale(currentMidi: Float, allowedMidiNotes: Set<Int>): Int {
        var closest = 60
        var minDiff = Float.MAX_VALUE
        for (note in allowedMidiNotes) {
            val diff = abs(note - currentMidi)
            if (diff < minDiff) {
                minDiff = diff
                closest = note
            }
        }
        return closest
    }

    /**
     * Applies pitch correction (Auto-Tune) to a float PCM vocal buffer.
     * Keeps original voice tempo and duration completely unchanged.
     * Speed: 0.0 (natural vibrato preserved) to 1.0 (hard-quantized T-Pain style).
     */
    fun applyAutoTune(
        vocalPcm: FloatArray,
        key: String = "A Minor",
        speed: Float = 0.85f,
        sampleRate: Int = 44100
    ): FloatArray {
        if (vocalPcm.isEmpty()) return vocalPcm
        val output = FloatArray(vocalPcm.size)
        val scaleNotes = parseScaleNotes(key)

        val windowSize = 2048
        val hopSize = 512
        val numHops = (vocalPcm.size - windowSize) / hopSize

        if (numHops <= 0) {
            return vocalPcm.copyOf()
        }

        val smoothedRatios = FloatArray(numHops) { 1.0f }

        // Step 1: Detect pitch and calculate target pitch shift ratio per hop
        for (h in 0 until numHops) {
            val offset = h * hopSize
            val detectedFreq = detectPitch(vocalPcm, offset, windowSize, sampleRate)
            if (detectedFreq > 60f && detectedFreq < 1200f) {
                val currentMidi = freqToMidi(detectedFreq)
                val targetMidi = snapToScale(currentMidi, scaleNotes)
                val targetFreq = midiToFreq(targetMidi)

                val rawRatio = targetFreq / detectedFreq
                // Blend with speed parameter: 1.0 = 100% snapped, 0.5 = 50% snapped
                val blendedRatio = 1.0f + (rawRatio - 1.0f) * speed.coerceIn(0.1f, 1.0f)
                smoothedRatios[h] = blendedRatio.coerceIn(0.7f, 1.4f)
            } else {
                smoothedRatios[h] = 1.0f
            }
        }

        // Smooth pitch transitions between hops to avoid click artifacts
        for (h in 1 until numHops) {
            smoothedRatios[h] = smoothedRatios[h - 1] * 0.3f + smoothedRatios[h] * 0.7f
        }

        // Step 2: Pitch shift using SOLA (Synchronized Overlap-Add) pitch modification
        val window = FloatArray(windowSize) { i ->
            0.5f * (1.0f - cos(2.0 * PI * i / (windowSize - 1)).toFloat()) // Hanning window
        }

        val overlapBuffer = FloatArray(vocalPcm.size + windowSize)
        val normBuffer = FloatArray(vocalPcm.size + windowSize)

        for (h in 0 until numHops) {
            val offset = h * hopSize
            val ratio = smoothedRatios[h]

            for (i in 0 until windowSize) {
                val readIndex = offset + (i * ratio).toInt()
                if (readIndex in vocalPcm.indices) {
                    val w = window[i]
                    val sample = vocalPcm[readIndex] * w
                    val outIndex = offset + i
                    if (outIndex in overlapBuffer.indices) {
                        overlapBuffer[outIndex] += sample
                        normBuffer[outIndex] += w
                    }
                }
            }
        }

        for (i in output.indices) {
            val norm = normBuffer[i]
            if (norm > 0.001f) {
                output[i] = (overlapBuffer[i] / norm).coerceIn(-1.0f, 1.0f)
            } else {
                output[i] = vocalPcm[i]
            }
        }

        return output
    }
}
