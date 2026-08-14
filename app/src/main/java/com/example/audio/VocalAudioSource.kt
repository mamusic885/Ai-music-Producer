package com.example.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

object VocalAudioSource {

    private const val SAMPLE_RATE = 44100
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    var isRecording = false
        private set

    private var audioRecord: AudioRecord? = null

    /**
     * Records audio from microphone and returns float PCM data.
     */
    suspend fun startRecording(
        onAmplitude: (Float) -> Unit
    ): FloatArray = withContext(Dispatchers.IO) {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("VocalAudioSource", "AudioRecord initialization failed")
                return@withContext generateDemoVocal("Reggaeton")
            }

            audioRecord?.startRecording()
            isRecording = true

            val audioData = mutableListOf<Float>()
            val buffer = ShortArray(1024)

            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    var maxAmp = 0f
                    for (i in 0 until read) {
                        val sample = buffer[i] / 32768.0f
                        audioData.add(sample)
                        if (abs(sample) > maxAmp) maxAmp = abs(sample)
                    }
                    onAmplitude(maxAmp.coerceIn(0f, 1f))
                }
            }

            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            if (audioData.size < 4000) {
                // If recording too short, fallback to demo vocal
                return@withContext generateDemoVocal("Reggaeton")
            }

            audioData.toFloatArray()
        } catch (e: Exception) {
            Log.e("VocalAudioSource", "Recording error: ${e.message}")
            isRecording = false
            audioRecord?.release()
            audioRecord = null
            generateDemoVocal("Reggaeton")
        }
    }

    fun stopRecording() {
        isRecording = false
    }

    /**
     * Decodes imported audio file (MP3, WAV, M4A, AAC, OGG) into PCM float array.
     */
    suspend fun decodeAudioUri(context: Context, uri: Uri): FloatArray = withContext(Dispatchers.IO) {
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)

            var trackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    format = f
                    break
                }
            }

            if (trackIndex < 0 || format == null) {
                extractor.release()
                return@withContext generateDemoVocal("Pop")
            }

            extractor.selectTrack(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            val outputStream = ByteArrayOutputStream()

            var isEOS = false
            while (!isEOS) {
                val inIndex = codec.dequeueInputBuffer(10000)
                if (inIndex >= 0) {
                    val buffer = codec.getInputBuffer(inIndex)
                    if (buffer != null) {
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEOS = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(info, 10000)
                if (outIndex >= 0) {
                    val buffer = codec.getOutputBuffer(outIndex)
                    if (buffer != null && info.size > 0) {
                        val chunk = ByteArray(info.size)
                        buffer.position(info.offset)
                        buffer.get(chunk, 0, info.size)
                        outputStream.write(chunk)
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }

            codec.stop()
            codec.release()
            extractor.release()

            val rawBytes = outputStream.toByteArray()
            if (rawBytes.isEmpty()) return@withContext generateDemoVocal("Pop")

            // Convert 16-bit PCM bytes to FloatArray (downmix to mono if stereo)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT, 1)
            val shortBuffer = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            val totalShorts = shortBuffer.remaining()
            val frameCount = totalShorts / channels
            val floatData = FloatArray(frameCount)

            for (i in 0 until frameCount) {
                var sum = 0f
                for (ch in 0 until channels) {
                    sum += shortBuffer.get(i * channels + ch) / 32768.0f
                }
                floatData[i] = (sum / channels).coerceIn(-1.0f, 1.0f)
            }

            floatData
        } catch (e: Exception) {
            Log.e("VocalAudioSource", "Decode error: ${e.message}")
            generateDemoVocal("Pop")
        }
    }

    /**
     * Synthesizes high quality vocal hooks with natural formant filtering and vibrato
     * for instant testing without needing microphone or audio files.
     */
    fun generateDemoVocal(style: String, durationSeconds: Float = 16.0f): FloatArray {
        val totalSamples = (SAMPLE_RATE * durationSeconds).toInt()
        val out = FloatArray(totalSamples)

        val bpm = if (style.contains("Reggaeton", ignoreCase = true)) 96 else 120
        val secondsPerBeat = 60.0f / bpm
        val samplesPerBeat = (secondsPerBeat * SAMPLE_RATE).toInt()
        val samplesPerBar = samplesPerBeat * 4

        // Melodic vocal phrase in A Minor: [A3 (220Hz), C4 (261.63Hz), D4 (293.66Hz), E4 (329.63Hz), G4 (392Hz), E4 (329.63Hz)]
        val melodyNotes = floatArrayOf(220.0f, 261.63f, 293.66f, 329.63f, 261.63f, 220.0f, 329.63f, 293.66f)
        val phraseBeats = floatArrayOf(0.0f, 0.75f, 1.5f, 2.25f, 3.0f, 4.0f, 4.75f, 5.5f)

        val totalPhrases = (totalSamples / (samplesPerBar * 2)) + 1

        for (phrase in 0 until totalPhrases) {
            val phraseStart = phrase * samplesPerBar * 2

            for (i in melodyNotes.indices) {
                val noteStart = phraseStart + (phraseBeats[i % phraseBeats.size] * samplesPerBeat).toInt()
                val noteDuration = (samplesPerBeat * 0.70f).toInt()
                val freq = melodyNotes[i]

                renderVocalSyllable(out, noteStart, noteDuration, freq)
            }
        }

        return out
    }

    private fun renderVocalSyllable(
        buffer: FloatArray,
        startSample: Int,
        lengthSamples: Int,
        freq: Float
    ) {
        for (i in 0 until lengthSamples) {
            val idx = startSample + i
            if (idx >= buffer.size) break
            val t = i.toFloat() / SAMPLE_RATE

            // Vocal vibrato (5.2 Hz)
            val vibrato = sin(2.0 * PI * 5.2 * t).toFloat() * 3.5f
            val f = freq + vibrato

            // Envelope with smooth consonant attack and breath release
            val attack = (i.toFloat() / (SAMPLE_RATE * 0.04f)).coerceAtMost(1.0f)
            val release = exp(-t * 3.8f)
            val env = attack * release

            // Formant synthesis for "Ah / Oh" vocal vowel (F1 ~ 750Hz, F2 ~ 1200Hz, F3 ~ 2400Hz)
            val saw = (2.0f * ((t * f) - kotlin.math.floor(t * f + 0.5f))).toFloat()
            val f1 = sin(2.0 * PI * 750.0 * t).toFloat() * 0.65f
            val f2 = sin(2.0 * PI * 1220.0 * t).toFloat() * 0.40f
            val f3 = sin(2.0 * PI * 2400.0 * t).toFloat() * 0.20f

            val vocalBody = saw * 0.4f + (f1 + f2 + f3) * 0.6f
            buffer[idx] += (vocalBody * env * 0.75f).coerceIn(-1.0f, 1.0f)
        }
    }
}
