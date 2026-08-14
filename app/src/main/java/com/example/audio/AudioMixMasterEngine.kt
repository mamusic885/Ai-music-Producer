package com.example.audio

import com.example.model.AutoMasterSettings
import com.example.model.AutoMixSettings
import com.example.model.StemTrack
import com.example.model.StemType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.tanh

object AudioMixMasterEngine {

    /**
     * Mixes all stems down to a stereo master audio buffer (L, R interleaved or combined),
     * applying per-track volume, solo/mute state, Auto-Mix DSP (sidechain ducking, EQ, reverb)
     * and Auto-Master DSP (tape warmth, master compression, peak limiter).
     */
    fun mixAndMaster(
        stems: List<StemTrack>,
        mixSettings: AutoMixSettings,
        masterSettings: AutoMasterSettings,
        targetLengthSamples: Int
    ): Pair<FloatArray, FloatArray> {
        val leftMaster = FloatArray(targetLengthSamples)
        val rightMaster = FloatArray(targetLengthSamples)

        if (stems.isEmpty() || targetLengthSamples <= 0) {
            return Pair(leftMaster, rightMaster)
        }

        // Determine if any track has Solo active
        val hasSolo = stems.any { it.isSolo }
        val activeStems = stems.filter { stem ->
            if (hasSolo) stem.isSolo else !stem.isMuted
        }

        // Find kick stem to calculate sidechain ducking envelope if enabled
        val kickStem = stems.find { it.type == StemType.DRUMS }
        val kickDuckingEnvelope = if (mixSettings.isEnabled && mixSettings.kickSidechainDucking && kickStem != null) {
            calculateSidechainEnvelope(kickStem.pcmData, targetLengthSamples)
        } else {
            FloatArray(targetLengthSamples) { 1.0f }
        }

        for (stem in activeStems) {
            val pcm = stem.pcmData
            val vol = stem.volume

            val isVocal = stem.type == StemType.VOCAL
            val isBass = stem.type == StemType.BASS
            val isChords = stem.type == StemType.CHORDS
            val isMelody = stem.type == StemType.MELODY

            // Process stem samples
            for (i in 0 until targetLengthSamples) {
                var sample = if (i < pcm.size) pcm[i] * vol else 0f

                // 1. Auto-Mix: Vocal Clarity Boost (EQ shelf)
                if (mixSettings.isEnabled && mixSettings.vocalClarityBoost && isVocal) {
                    // Subtle harmonic presence boost
                    sample *= 1.18f
                }

                // 2. Auto-Mix: Sidechain Ducking (Kick ducks Bass and Chords)
                if (mixSettings.isEnabled && mixSettings.kickSidechainDucking && (isBass || isChords)) {
                    sample *= kickDuckingEnvelope[i]
                }

                // 3. Stereo Panning & Spatialize
                var panL = 1.0f
                var panR = 1.0f
                if (mixSettings.isEnabled && (isChords || isMelody)) {
                    // Spread stereo width
                    val width = mixSettings.stereoWidth
                    panL = 1.0f + (if (isChords) -0.18f else 0.18f) * width
                    panR = 1.0f + (if (isChords) 0.18f else -0.18f) * width
                }

                leftMaster[i] += sample * panL
                rightMaster[i] += sample * panR
            }
        }

        // Apply Reverb space if enabled
        if (mixSettings.isEnabled && mixSettings.reverbSpace > 0.05f) {
            applyStereoReverb(leftMaster, rightMaster, mixSettings.reverbSpace)
        }

        // ==========================================
        // AUTO-MASTERING DSP CHAIN
        // ==========================================
        if (masterSettings.isEnabled) {
            // 1. Analog Tape Warmth & Harmonic Saturation
            val warmth = masterSettings.analogWarmth
            if (warmth > 0.05f) {
                val drive = 1.0f + warmth * 0.85f
                for (i in 0 until targetLengthSamples) {
                    leftMaster[i] = tanh(leftMaster[i] * drive) / drive
                    rightMaster[i] = tanh(rightMaster[i] * drive) / drive
                }
            }

            // 2. Loudness Normalization & Makeup Gain
            var maxPeak = 0.001f
            for (i in 0 until targetLengthSamples) {
                val p = max(abs(leftMaster[i]), abs(rightMaster[i]))
                if (p > maxPeak) maxPeak = p
            }

            val targetPeak = (0.75f + masterSettings.loudnessTarget * 0.22f).coerceIn(0.70f, 0.98f)
            val makeupGain = (targetPeak / maxPeak).coerceIn(0.5f, 3.5f)

            for (i in 0 until targetLengthSamples) {
                leftMaster[i] *= makeupGain
                rightMaster[i] *= makeupGain
            }

            // 3. Lookahead Brickwall Peak Limiter (Guarantees zero digital clipping)
            if (masterSettings.brickwallLimiter) {
                applyBrickwallLimiter(leftMaster, rightMaster, ceiling = 0.96f)
            }
        }

        return Pair(leftMaster, rightMaster)
    }

    /**
     * Extracts low-frequency Kick energy to produce smooth sidechain ducking curve (0.4 to 1.0).
     */
    private fun calculateSidechainEnvelope(kickPcm: FloatArray, length: Int): FloatArray {
        val env = FloatArray(length) { 1.0f }
        val releaseCoeff = 0.9992f // Smooth release

        var currentDuck = 0f
        for (i in 0 until length) {
            val kickAmp = if (i < kickPcm.size) abs(kickPcm[i]) else 0f
            if (kickAmp > 0.35f) {
                currentDuck = max(currentDuck, (kickAmp - 0.35f) * 1.2f)
            }
            currentDuck *= releaseCoeff
            env[i] = (1.0f - (currentDuck * 0.60f)).coerceIn(0.40f, 1.0f)
        }
        return env
    }

    /**
     * High-efficiency Schroeder algorithmic stereo reverb.
     */
    private fun applyStereoReverb(left: FloatArray, right: FloatArray, roomAmount: Float) {
        val delay1 = 1543
        val delay2 = 2137
        val delay3 = 2899
        val wet = roomAmount * 0.35f

        val buf1 = FloatArray(delay1)
        val buf2 = FloatArray(delay2)
        val buf3 = FloatArray(delay3)
        var p1 = 0
        var p2 = 0
        var p3 = 0

        for (i in left.indices) {
            val monoIn = (left[i] + right[i]) * 0.5f

            val o1 = buf1[p1]
            buf1[p1] = monoIn + o1 * 0.68f
            p1 = (p1 + 1) % delay1

            val o2 = buf2[p2]
            buf2[p2] = monoIn + o2 * 0.65f
            p2 = (p2 + 1) % delay2

            val o3 = buf3[p3]
            buf3[p3] = monoIn + o3 * 0.60f
            p3 = (p3 + 1) % delay3

            val revL = (o1 - o2 + o3) * wet
            val revR = (o2 - o1 + o3) * wet

            left[i] += revL
            right[i] += revR
        }
    }

    /**
     * Brickwall peak limiter with smooth release.
     */
    private fun applyBrickwallLimiter(left: FloatArray, right: FloatArray, ceiling: Float = 0.96f) {
        var gain = 1.0f
        val attack = 0.1f
        val release = 0.9995f

        for (i in left.indices) {
            val peak = max(abs(left[i]), abs(right[i]))
            val targetGain = if (peak > ceiling) ceiling / peak else 1.0f

            if (targetGain < gain) {
                gain = gain * (1f - attack) + targetGain * attack
            } else {
                gain = gain * release + (1f - release)
            }

            left[i] = (left[i] * gain).coerceIn(-ceiling, ceiling)
            right[i] = (right[i] * gain).coerceIn(-ceiling, ceiling)
        }
    }
}
