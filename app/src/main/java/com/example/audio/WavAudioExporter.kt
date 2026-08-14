package com.example.audio

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.AutoMasterSettings
import com.example.model.AutoMixSettings
import com.example.model.StemTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavAudioExporter {

    private const val SAMPLE_RATE = 44100
    private const val CHANNELS = 2
    private const val BITS_PER_SAMPLE = 16

    /**
     * Exports full arrangement to a WAV file in app storage or MediaStore.
     */
    suspend fun exportWavFile(
        context: Context,
        title: String,
        stems: List<StemTrack>,
        mixSettings: AutoMixSettings,
        masterSettings: AutoMasterSettings
    ): File = withContext(Dispatchers.IO) {
        val maxLen = stems.maxOfOrNull { it.pcmData.size } ?: (SAMPLE_RATE * 16)
        val (leftMaster, rightMaster) = AudioMixMasterEngine.mixAndMaster(
            stems = stems,
            mixSettings = mixSettings,
            masterSettings = masterSettings,
            targetLengthSamples = maxLen
        )

        val cleanTitle = title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val fileName = "${cleanTitle}_${System.currentTimeMillis()}.wav"
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outputFile = File(exportDir, fileName)

        FileOutputStream(outputFile).use { fos ->
            writeWavData(fos, leftMaster, rightMaster, maxLen)
        }

        outputFile
    }

    /**
     * Saves exported WAV to device Public Music / Audio collection via MediaStore.
     */
    suspend fun saveToMediaStore(context: Context, wavFile: File, title: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, "${title}_Master.wav")
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.ARTIST, "AI Music Producer")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/AIMusicProducer")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    wavFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                }
            }
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Creates an Android Share Intent for the exported audio file.
     */
    fun createShareIntent(context: Context, wavFile: File, title: String): Intent {
        val uri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", wavFile)
        } catch (e: Exception) {
            Uri.fromFile(wavFile)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "$title - Produced by AI Music Producer")
            putExtra(Intent.EXTRA_TEXT, "Listen to this music arrangement produced with AI Music Producer!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "Export Master Audio")
    }

    private fun writeWavData(
        out: OutputStream,
        left: FloatArray,
        right: FloatArray,
        lengthSamples: Int
    ) {
        val totalAudioBytes = lengthSamples * CHANNELS * (BITS_PER_SAMPLE / 8)
        val totalDataLen = totalAudioBytes + 36

        // RIFF Header
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(totalDataLen)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16) // Subchunk1Size for PCM
        header.putShort(1.toShort()) // AudioFormat 1 = PCM
        header.putShort(CHANNELS.toShort())
        header.putInt(SAMPLE_RATE)
        header.putInt(SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8)) // ByteRate
        header.putShort((CHANNELS * (BITS_PER_SAMPLE / 8)).toShort()) // BlockAlign
        header.putShort(BITS_PER_SAMPLE.toShort())
        header.put("data".toByteArray())
        header.putInt(totalAudioBytes)

        out.write(header.array())

        // Stream PCM 16-bit stereo data
        val chunkSize = 4096
        val buffer = ByteBuffer.allocate(chunkSize * CHANNELS * 2).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until lengthSamples) {
            val lClamped = left[i].coerceIn(-1.0f, 1.0f)
            val rClamped = right[i].coerceIn(-1.0f, 1.0f)

            buffer.putShort((lClamped * 32767.0f).toInt().toShort())
            buffer.putShort((rClamped * 32767.0f).toInt().toShort())

            if (buffer.position() == buffer.capacity()) {
                out.write(buffer.array())
                buffer.clear()
            }
        }

        if (buffer.position() > 0) {
            out.write(buffer.array(), 0, buffer.position())
        }
    }
}
