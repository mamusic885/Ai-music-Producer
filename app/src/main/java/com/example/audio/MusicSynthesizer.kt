package com.example.audio

import com.example.model.ArrangementRecipe
import com.example.model.StemTrack
import com.example.model.StemType
import com.example.ui.theme.StudioAnalogWarm
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioTurquoise
import com.example.ui.theme.StudioVUMeterGreen
import com.example.ui.theme.StudioVUMeterYellow
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

object MusicSynthesizer {

    private const val SAMPLE_RATE = 44100
    private val random = Random(42)

    // Chord to root note and frequencies mapping
    private val CHORD_FREQUENCIES = mapOf(
        // Minor chords
        "Am" to floatArrayOf(220.00f, 261.63f, 329.63f, 440.00f), // A3, C4, E4, A4
        "Dm" to floatArrayOf(146.83f, 220.00f, 293.66f, 349.23f), // D3, A3, D4, F4
        "Em" to floatArrayOf(164.81f, 246.94f, 329.63f, 392.00f), // E3, B3, E4, G4
        "Fm" to floatArrayOf(174.61f, 261.63f, 349.23f, 415.30f), // F3, C4, F4, Ab4
        "Gm" to floatArrayOf(196.00f, 293.66f, 392.00f, 466.16f), // G3, D4, G4, Bb4
        "Bm" to floatArrayOf(246.94f, 293.66f, 369.99f, 493.88f), // B3, D4, F#4, B4
        "C#m" to floatArrayOf(138.59f, 220.00f, 277.18f, 329.63f),
        "F#m" to floatArrayOf(185.00f, 277.18f, 369.99f, 440.00f),

        // Major chords
        "C" to floatArrayOf(130.81f, 261.63f, 329.63f, 392.00f),  // C3, C4, E4, G4
        "D" to floatArrayOf(146.83f, 293.66f, 369.99f, 440.00f),  // D3, D4, F#4, A4
        "E" to floatArrayOf(164.81f, 329.63f, 415.30f, 493.88f),  // E3, E4, G#4, B4
        "F" to floatArrayOf(174.61f, 261.63f, 349.23f, 440.00f),  // F3, C4, F4, A4
        "G" to floatArrayOf(196.00f, 293.66f, 392.00f, 493.88f),  // G3, D4, G4, B4
        "A" to floatArrayOf(220.00f, 277.18f, 329.63f, 440.00f),  // A3, C#4, E4, A4
        "Bb" to floatArrayOf(233.08f, 293.66f, 349.23f, 466.16f)  // Bb3, D4, F4, Bb4
    )

    private val CHORD_ROOT_BASS = mapOf(
        "Am" to 55.00f, // A1
        "Dm" to 73.42f, // D2
        "Em" to 82.41f, // E2
        "Fm" to 43.65f, // F1
        "Gm" to 49.00f, // G1
        "Bm" to 61.74f, // B1
        "C#m" to 69.30f, // C#2
        "F#m" to 46.25f, // F#1
        "C" to 65.41f,  // C2
        "D" to 73.42f,  // D2
        "E" to 82.41f,  // E2
        "F" to 43.65f,  // F1
        "G" to 49.00f,  // G1
        "A" to 55.00f,  // A1
        "Bb" to 58.27f  // Bb1
    )

    /**
     * Synthesizes all backing stems (Drums, Bass, Chords, Melody/Guitar, FX)
     * matching the given ArrangementRecipe and target duration.
     */
    fun generateArrangementStems(
        recipe: ArrangementRecipe,
        durationSeconds: Float = 16.0f
    ): List<StemTrack> {
        val totalSamples = (durationSeconds * SAMPLE_RATE).toInt()
        val bpm = recipe.bpm.coerceIn(60, 180)
        val secondsPerBeat = 60.0f / bpm
        val samplesPerBeat = (secondsPerBeat * SAMPLE_RATE).toInt()
        val samplesPerBar = samplesPerBeat * 4

        val chords = if (recipe.chordProgression.isNotEmpty()) recipe.chordProgression else listOf("Am", "F", "C", "G")
        val isReggaeton = recipe.genre.contains("Reggaeton", ignoreCase = true) ||
                recipe.prompt.contains("reggaeton", ignoreCase = true) ||
                recipe.prompt.contains("dembow", ignoreCase = true)
        val isTrap = recipe.genre.contains("Trap", ignoreCase = true) ||
                recipe.prompt.contains("trap", ignoreCase = true) ||
                recipe.prompt.contains("808", ignoreCase = true)

        // 1. Drums Stem
        val drumsData = synthesizeDrums(
            totalSamples,
            samplesPerBeat,
            samplesPerBar,
            isReggaeton,
            isTrap
        )

        // 2. Deep 808 Bass Stem
        val bassData = synthesizeDeepBass(
            totalSamples,
            samplesPerBeat,
            samplesPerBar,
            chords,
            isReggaeton,
            isTrap
        )

        // 3. Chords & Plucks Stem (Soft Plucks / Guitars)
        val chordsData = synthesizeChordsAndPlucks(
            totalSamples,
            samplesPerBeat,
            samplesPerBar,
            chords,
            recipe.prompt
        )

        // 4. Melody & Lead Guitar Stem
        val melodyData = synthesizeMelodyAndGuitar(
            totalSamples,
            samplesPerBeat,
            samplesPerBar,
            chords,
            recipe.prompt
        )

        // 5. FX & Ambience Stem
        val fxData = synthesizeFX(totalSamples, samplesPerBar)

        return listOf(
            StemTrack(
                id = "stem_drums",
                type = StemType.DRUMS,
                name = if (isReggaeton) "Dembow Drums" else if (isTrap) "Trap 808 Drums" else "Drums & Beat",
                color = StudioTurquoise,
                volume = 0.90f,
                pcmData = drumsData,
                waveformPoints = computeWaveformOverview(drumsData)
            ),
            StemTrack(
                id = "stem_bass",
                type = StemType.BASS,
                name = if (isTrap) "808 Sub Bass" else "Deep Bass",
                color = StudioVUMeterGreen,
                volume = 0.85f,
                pcmData = bassData,
                waveformPoints = computeWaveformOverview(bassData)
            ),
            StemTrack(
                id = "stem_chords",
                type = StemType.CHORDS,
                name = "Soft Plucks & Chords",
                color = StudioCyan,
                volume = 0.80f,
                pcmData = chordsData,
                waveformPoints = computeWaveformOverview(chordsData)
            ),
            StemTrack(
                id = "stem_melody",
                type = StemType.MELODY,
                name = "Acoustic Guitar & Lead",
                color = StudioMint,
                volume = 0.75f,
                pcmData = melodyData,
                waveformPoints = computeWaveformOverview(melodyData)
            ),
            StemTrack(
                id = "stem_fx",
                type = StemType.FX,
                name = "Atmosphere & Risers",
                color = StudioAnalogWarm,
                volume = 0.60f,
                pcmData = fxData,
                waveformPoints = computeWaveformOverview(fxData)
            )
        )
    }

    // ==========================================
    // 1. DRUMS SYNTHESIS (Reggaeton Dembow / Trap / Pop)
    // ==========================================
    private fun synthesizeDrums(
        totalSamples: Int,
        samplesPerBeat: Int,
        samplesPerBar: Int,
        isReggaeton: Boolean,
        isTrap: Boolean
    ): FloatArray {
        val out = FloatArray(totalSamples)

        val totalBars = (totalSamples / samplesPerBar) + 1
        for (bar in 0 until totalBars) {
            val barStart = bar * samplesPerBar

            // 4 beats per bar
            for (beat in 0..3) {
                val beatStart = barStart + beat * samplesPerBeat

                // Kick on each quarter note (1, 2, 3, 4)
                addKick(out, beatStart)

                if (isReggaeton) {
                    // REGGAETON DEMBOW TRESILLO PATTERN:
                    // Kick: 1, 2, 3, 4
                    // Snare/Clap on: "and" of 1 (offset 0.75 beat) and "and" of 2 (offset 0.5 beat)
                    // Exactly: Beat 1 -> offset 0.75 beat, Beat 2 -> offset 0.5 beat, Beat 3 -> offset 0.75 beat, Beat 4 -> offset 0.5 beat
                    if (beat == 0 || beat == 2) {
                        addSnareClap(out, (beatStart + samplesPerBeat * 0.75f).toInt(), isClap = true)
                    } else {
                        addSnareClap(out, (beatStart + samplesPerBeat * 0.50f).toInt(), isClap = true)
                    }

                    // Shaker/Hihat on 16ths
                    for (sixteenth in 0..3) {
                        val hatStart = beatStart + (sixteenth * samplesPerBeat / 4)
                        addHiHat(out, hatStart, isOpen = false, gain = if (sixteenth % 2 == 0) 0.35f else 0.22f)
                    }
                } else if (isTrap) {
                    // TRAP PATTERN:
                    // Snare on beat 3 (index 2)
                    if (beat == 2) {
                        addSnareClap(out, beatStart, isClap = false)
                    }
                    // Fast rolling 16th/32nd hi-hats
                    for (sixteenth in 0..3) {
                        val hatStart = beatStart + (sixteenth * samplesPerBeat / 4)
                        addHiHat(out, hatStart, isOpen = false, gain = 0.30f)
                    }
                } else {
                    // Standard Pop / Dance: Snare on 2 and 4
                    if (beat == 1 || beat == 3) {
                        addSnareClap(out, beatStart, isClap = false)
                    }
                    // Offbeat open hi-hat
                    addHiHat(out, (beatStart + samplesPerBeat * 0.5f).toInt(), isOpen = true, gain = 0.35f)
                    addHiHat(out, beatStart, isOpen = false, gain = 0.25f)
                }
            }
        }

        // Soft clip drum output
        for (i in out.indices) {
            out[i] = kotlin.math.tanh(out[i] * 1.15f)
        }

        return out
    }

    private fun addKick(buffer: FloatArray, startSample: Int) {
        val durationSamples = (SAMPLE_RATE * 0.38f).toInt() // 380ms punch
        for (i in 0 until durationSamples) {
            val idx = startSample + i
            if (idx >= buffer.size) break
            val t = i.toFloat() / SAMPLE_RATE

            // Frequency envelope: rapid drop from 160Hz down to 48Hz
            val freq = 48.0f + 112.0f * exp(-t * 32.0f)
            val phase = 2.0f * PI.toFloat() * freq * t
            // Amplitude envelope
            val env = exp(-t * 9.5f)
            // Transient click at the very start
            val click = if (i < 80) (random.nextFloat() * 2f - 1f) * 0.35f * (1f - i / 80f) else 0f

            val kickSample = (sin(phase) + click) * env * 0.95f
            buffer[idx] += kickSample
        }
    }

    private fun addSnareClap(buffer: FloatArray, startSample: Int, isClap: Boolean) {
        val durationSamples = (SAMPLE_RATE * 0.22f).toInt()
        for (i in 0 until durationSamples) {
            val idx = startSample + i
            if (idx >= buffer.size) break
            val t = i.toFloat() / SAMPLE_RATE

            val noise = (random.nextFloat() * 2f - 1f)
            val env = exp(-t * (if (isClap) 24f else 18f))

            // Body tone (220 Hz)
            val bodyTone = sin(2.0 * PI * 220.0 * t).toFloat() * exp(-t * 28.0f) * 0.45f
            val sample = (noise * 0.65f + bodyTone) * env * (if (isClap) 0.80f else 0.85f)

            buffer[idx] += sample
        }
    }

    private fun addHiHat(buffer: FloatArray, startSample: Int, isOpen: Boolean, gain: Float = 0.3f) {
        val durationSamples = (SAMPLE_RATE * (if (isOpen) 0.16f else 0.045f)).toInt()
        for (i in 0 until durationSamples) {
            val idx = startSample + i
            if (idx >= buffer.size) break
            val t = i.toFloat() / SAMPLE_RATE

            // Metallic high frequencies
            val noise = (random.nextFloat() * 2f - 1f)
            val metal = sin(2.0 * PI * 8500.0 * t).toFloat() * 0.5f + sin(2.0 * PI * 11200.0 * t).toFloat() * 0.5f
            val env = exp(-t * (if (isOpen) 28f else 110f))

            buffer[idx] += (noise * 0.7f + metal * 0.3f) * env * gain
        }
    }

    // ==========================================
    // 2. BASS SYNTHESIS (808 Sub / Reese Bass)
    // ==========================================
    private fun synthesizeDeepBass(
        totalSamples: Int,
        samplesPerBeat: Int,
        samplesPerBar: Int,
        chords: List<String>,
        isReggaeton: Boolean,
        isTrap: Boolean
    ): FloatArray {
        val out = FloatArray(totalSamples)
        val totalBars = (totalSamples / samplesPerBar) + 1

        for (bar in 0 until totalBars) {
            val chordName = chords[bar % chords.size]
            val rootFreq = CHORD_ROOT_BASS[chordName] ?: 55.0f
            val barStart = bar * samplesPerBar

            if (isReggaeton) {
                // Dembow bass: hits on beat 1, beat 2 (off), beat 3, beat 4
                val bassHits = listOf(0f, 0.75f, 2.0f, 2.75f)
                for (hitBeat in bassHits) {
                    val hitStart = barStart + (hitBeat * samplesPerBeat).toInt()
                    val hitDuration = (samplesPerBeat * 0.65f).toInt()
                    renderSubNote(out, hitStart, hitDuration, rootFreq, punch = 1.0f)
                }
            } else if (isTrap) {
                // Long sustained 808 boom on beat 1, slide on beat 3.5
                renderSubNote(out, barStart, (samplesPerBeat * 2.2f).toInt(), rootFreq, punch = 1.2f)
                renderSubNote(out, barStart + (samplesPerBeat * 2.5f).toInt(), (samplesPerBeat * 1.4f).toInt(), rootFreq * 1.12f, punch = 0.9f)
            } else {
                // Steady bass notes on 1 and 3
                renderSubNote(out, barStart, (samplesPerBeat * 1.8f).toInt(), rootFreq, punch = 0.8f)
                renderSubNote(out, barStart + samplesPerBeat * 2, (samplesPerBeat * 1.8f).toInt(), rootFreq, punch = 0.8f)
            }
        }

        return out
    }

    private fun renderSubNote(buffer: FloatArray, startSample: Int, lengthSamples: Int, freq: Float, punch: Float) {
        for (i in 0 until lengthSamples) {
            val idx = startSample + i
            if (idx >= buffer.size) break
            val t = i.toFloat() / SAMPLE_RATE

            val env = exp(-t * 2.8f) * (1.0f - exp(-t * 60.0f)) // smooth attack & natural decay
            // Fundamental sine + 2nd harmonic for rich saturation on phone speakers
            val fundamental = sin(2.0 * PI * freq * t).toFloat()
            val secondHarmonic = sin(2.0 * PI * (freq * 2.0f) * t).toFloat() * 0.28f
            val thirdHarmonic = sin(2.0 * PI * (freq * 3.0f) * t).toFloat() * 0.10f

            val raw = (fundamental + secondHarmonic + thirdHarmonic) * env * 0.70f * punch
            // Soft tube saturation
            buffer[idx] += kotlin.math.tanh(raw * 1.3f)
        }
    }

    // ==========================================
    // 3. CHORDS & PLUCKS (Karplus-Strong / Nylon Plucks)
    // ==========================================
    private fun synthesizeChordsAndPlucks(
        totalSamples: Int,
        samplesPerBeat: Int,
        samplesPerBar: Int,
        chords: List<String>,
        prompt: String
    ): FloatArray {
        val out = FloatArray(totalSamples)
        val totalBars = (totalSamples / samplesPerBar) + 1

        for (bar in 0 until totalBars) {
            val chordName = chords[bar % chords.size]
            val chordFreqs = CHORD_FREQUENCIES[chordName] ?: floatArrayOf(220f, 261.63f, 329.63f)
            val barStart = bar * samplesPerBar

            // Pluck arpeggio pattern across the bar (4 beats with 8th note rhythmic plucks)
            val pluckOffsets = listOf(0.0f, 0.5f, 1.0f, 1.75f, 2.0f, 2.5f, 3.0f, 3.5f)
            for ((idx, beatOffset) in pluckOffsets.withIndex()) {
                val pluckStart = barStart + (beatOffset * samplesPerBeat).toInt()
                val noteFreq = chordFreqs[idx % chordFreqs.size]
                renderKarplusPluck(out, pluckStart, noteFreq, durationSeconds = 0.85f, brightness = 0.65f)
            }

            // Warm Rhodes / Pad layer sustained across the bar
            for (freq in chordFreqs) {
                renderWarmPad(out, barStart, samplesPerBar, freq, gain = 0.16f)
            }
        }

        return out
    }

    /**
     * Karplus-Strong physical modeling algorithm for realistic acoustic/nylon string plucks.
     */
    private fun renderKarplusPluck(
        buffer: FloatArray,
        startSample: Int,
        freq: Float,
        durationSeconds: Float = 0.8f,
        brightness: Float = 0.6f
    ) {
        val period = (SAMPLE_RATE / freq).toInt().coerceIn(16, 2048)
        val totalLength = (SAMPLE_RATE * durationSeconds).toInt()

        // Initialize delay line with noise burst (string excitation)
        val delayLine = FloatArray(period) {
            (random.nextFloat() * 2f - 1f) * brightness
        }

        var delayIdx = 0
        var prevSample = 0f
        val decayFactor = 0.992f // Natural string resonance decay

        for (i in 0 until totalLength) {
            val outIdx = startSample + i
            if (outIdx >= buffer.size) break

            val current = delayLine[delayIdx]
            // Low-pass filter loop (string damping)
            val filtered = (current + prevSample) * 0.5f * decayFactor
            prevSample = filtered
            delayLine[delayIdx] = filtered

            delayIdx = (delayIdx + 1) % period
            buffer[outIdx] += current * 0.28f
        }
    }

    private fun renderWarmPad(
        buffer: FloatArray,
        startSample: Int,
        lengthSamples: Int,
        freq: Float,
        gain: Float
    ) {
        for (i in 0 until lengthSamples) {
            val idx = startSample + i
            if (idx >= buffer.size) break
            val t = i.toFloat() / SAMPLE_RATE

            // Smooth attack and release envelope
            val attack = (i.toFloat() / (SAMPLE_RATE * 0.15f)).coerceAtMost(1.0f)
            val release = ((lengthSamples - i).toFloat() / (SAMPLE_RATE * 0.15f)).coerceIn(0.0f, 1.0f)
            val env = attack * release

            // Warm dual-oscillator with slight detune for lush stereo chorus
            val osc1 = sin(2.0 * PI * freq * t).toFloat()
            val osc2 = sin(2.0 * PI * (freq * 1.003f) * t).toFloat() * 0.7f

            buffer[idx] += (osc1 + osc2) * env * gain
        }
    }

    // ==========================================
    // 4. MELODY & GUITAR (Lead Counter-Melody / Riffs)
    // ==========================================
    private fun synthesizeMelodyAndGuitar(
        totalSamples: Int,
        samplesPerBeat: Int,
        samplesPerBar: Int,
        chords: List<String>,
        prompt: String
    ): FloatArray {
        val out = FloatArray(totalSamples)
        val totalBars = (totalSamples / samplesPerBar) + 1

        for (bar in 0 until totalBars) {
            val chordName = chords[bar % chords.size]
            val chordFreqs = CHORD_FREQUENCIES[chordName] ?: floatArrayOf(220f, 261.63f, 329.63f)
            val barStart = bar * samplesPerBar

            // Melodic lead riff on beat 1.5, 2.5, 3.25, 3.75
            val riffBeats = listOf(1.5f, 2.0f, 2.75f, 3.5f)
            for ((noteIdx, beat) in riffBeats.withIndex()) {
                val noteStart = barStart + (beat * samplesPerBeat).toInt()
                // Higher octave melody note
                val leadFreq = chordFreqs[(noteIdx + 1) % chordFreqs.size] * 2.0f
                renderGuitarLeadNote(out, noteStart, leadFreq, durationSeconds = 0.65f)
            }
        }

        // Add Ping-Pong Delay Echo effect
        val delaySamples = (SAMPLE_RATE * 0.28f).toInt()
        val feedback = 0.38f
        for (i in delaySamples until out.size) {
            out[i] += out[i - delaySamples] * feedback
        }

        return out
    }

    private fun renderGuitarLeadNote(
        buffer: FloatArray,
        startSample: Int,
        freq: Float,
        durationSeconds: Float
    ) {
        val length = (SAMPLE_RATE * durationSeconds).toInt()
        for (i in 0 until length) {
            val idx = startSample + i
            if (idx >= buffer.size) break
            val t = i.toFloat() / SAMPLE_RATE

            // Vibrato LFO (5.5 Hz)
            val vibrato = sin(2.0 * PI * 5.5 * t).toFloat() * 4.0f
            val instantaneousFreq = freq + vibrato

            val env = exp(-t * 4.2f) * (1.0f - exp(-t * 90.0f))
            // Acoustic guitar harmonics (triangle-like overtone spectrum)
            val fundamental = sin(2.0 * PI * instantaneousFreq * t).toFloat()
            val second = sin(2.0 * PI * (instantaneousFreq * 2.0f) * t).toFloat() * 0.35f
            val third = sin(2.0 * PI * (instantaneousFreq * 3.0f) * t).toFloat() * 0.15f

            buffer[idx] += (fundamental + second + third) * env * 0.32f
        }
    }

    // ==========================================
    // 5. FX & AMBIENCE (Vinyl Crackle, Risers, Drops)
    // ==========================================
    private fun synthesizeFX(totalSamples: Int, samplesPerBar: Int): FloatArray {
        val out = FloatArray(totalSamples)

        // Subtle lo-fi vinyl atmosphere bed
        for (i in out.indices) {
            if (random.nextFloat() < 0.003f) {
                // Vinyl crackle pop
                val pop = (random.nextFloat() * 2f - 1f) * 0.18f
                out[i] += pop
            }
        }

        // Reverse cymbal riser at the end of every 4th bar
        val fourBars = samplesPerBar * 4
        var riserStart = fourBars - (SAMPLE_RATE * 1.5f).toInt()
        while (riserStart < totalSamples) {
            if (riserStart >= 0) {
                val riserLen = (SAMPLE_RATE * 1.5f).toInt()
                for (i in 0 until riserLen) {
                    val idx = riserStart + i
                    if (idx >= out.size) break
                    val t = i.toFloat() / riserLen // 0.0 to 1.0
                    val noise = (random.nextFloat() * 2f - 1f)
                    val env = t.pow(2.5f) // Exponential swelling build
                    out[idx] += noise * env * 0.25f
                }
            }
            riserStart += fourBars
        }

        return out
    }

    /**
     * Downsamples audio into 60 visual points for UI waveform rendering.
     */
    fun computeWaveformOverview(pcm: FloatArray, targetPoints: Int = 60): List<Float> {
        if (pcm.isEmpty()) return List(targetPoints) { 0.1f }
        val points = mutableListOf<Float>()
        val chunkSize = (pcm.size / targetPoints).coerceAtLeast(1)

        for (i in 0 until targetPoints) {
            val start = i * chunkSize
            val end = (start + chunkSize).coerceAtMost(pcm.size)
            var maxAmp = 0f
            for (j in start until end) {
                val amp = abs(pcm[j])
                if (amp > maxAmp) maxAmp = amp
            }
            points.add(maxAmp.coerceIn(0.08f, 1.0f))
        }
        return points
    }
}
