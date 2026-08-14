package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.model.ArrangementRecipe
import com.example.model.AutoMasterSettings
import com.example.model.AutoMixSettings
import com.example.model.AutoTuneSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiMusicProducerService {

    private const val TAG = "GeminiMusicService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Parses user instructions using Gemini 3.5 Flash (or smart fallback engine)
     * into a structured musical arrangement recipe.
     */
    suspend fun generateArrangementRecipe(prompt: String): ArrangementRecipe = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d(TAG, "Using intelligent local music composer engine for prompt: $prompt")
            return@withContext composeLocalArrangement(prompt)
        }

        try {
            val systemPrompt = """
                You are an expert AI Music Producer and mixing/mastering engineer.
                Analyze the user's music production prompt and return a valid JSON object specifying the musical arrangement.
                Keep the JSON schema strictly as follows:
                {
                  "genre": "Reggaeton" | "Trap" | "Pop" | "Afrobeats" | "Synthwave" | "Lo-Fi" | "EDM" | "R&B",
                  "bpm": integer between 70 and 160,
                  "musicalKey": "A Minor" | "C Major" | "D Minor" | "F Major" | "G Minor" | "E Minor" | "F# Minor",
                  "chordProgression": ["Am", "F", "C", "G"],
                  "drumStyle": string description,
                  "bassStyle": string description,
                  "chordStyle": string description,
                  "leadStyle": string description,
                  "fxStyle": string description,
                  "autoTuneKey": "A Minor",
                  "autoTuneSpeed": float between 0.3 and 1.0,
                  "kickSidechain": boolean,
                  "vocalClarity": boolean,
                  "reverbSpace": float between 0.1 and 0.8,
                  "analogWarmth": float between 0.2 and 0.8,
                  "loudnessTarget": float between 0.6 and 0.95
                }
                Output only the raw JSON. Do not include markdown codeblocks or extra text.
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "System: $systemPrompt\n\nUser Instruction: $prompt"))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
                })
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseString = response.body?.string() ?: ""
                val rootJson = JSONObject(responseString)
                val candidates = rootJson.optJSONArray("candidates")
                val text = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text", "") ?: ""

                val cleanJsonStr = text.replace("```json", "").replace("```", "").trim()
                if (cleanJsonStr.isNotEmpty()) {
                    return@withContext parseJsonRecipe(cleanJsonStr, prompt)
                }
            } else {
                Log.w(TAG, "Gemini API error code: ${response.code}, falling back to local composer")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API exception: ${e.message}, using local composer")
        }

        return@withContext composeLocalArrangement(prompt)
    }

    private fun parseJsonRecipe(jsonString: String, originalPrompt: String): ArrangementRecipe {
        return try {
            val json = JSONObject(jsonString)
            val genre = json.optString("genre", "Reggaeton")
            val bpm = json.optInt("bpm", 96).coerceIn(60, 180)
            val musicalKey = json.optString("musicalKey", "A Minor")

            val chordsArray = json.optJSONArray("chordProgression")
            val chords = mutableListOf<String>()
            if (chordsArray != null) {
                for (i in 0 until chordsArray.length()) {
                    chords.add(chordsArray.optString(i))
                }
            }
            val finalChords = if (chords.isNotEmpty()) chords else listOf("Am", "F", "C", "G")

            val drumStyle = json.optString("drumStyle", "Dembow Tresillo Groove")
            val bassStyle = json.optString("bassStyle", "Deep 808 Sub Punch")
            val chordStyle = json.optString("chordStyle", "Soft Nylon Plucks")
            val leadStyle = json.optString("leadStyle", "Spanish Guitar Lead")
            val fxStyle = json.optString("fxStyle", "Atmospheric Vinyl & Risers")

            val autoTuneKey = json.optString("autoTuneKey", musicalKey)
            val autoTuneSpeed = json.optDouble("autoTuneSpeed", 0.85).toFloat()

            val kickSidechain = json.optBoolean("kickSidechain", true)
            val vocalClarity = json.optBoolean("vocalClarity", true)
            val reverbSpace = json.optDouble("reverbSpace", 0.28).toFloat()

            val analogWarmth = json.optDouble("analogWarmth", 0.45).toFloat()
            val loudnessTarget = json.optDouble("loudnessTarget", 0.80).toFloat()

            ArrangementRecipe(
                genre = genre,
                bpm = bpm,
                musicalKey = musicalKey,
                chordProgression = finalChords,
                prompt = originalPrompt,
                drumStyle = drumStyle,
                bassStyle = bassStyle,
                chordStyle = chordStyle,
                leadStyle = leadStyle,
                fxStyle = fxStyle,
                autoTuneSettings = AutoTuneSettings(
                    isEnabled = true,
                    targetKey = autoTuneKey,
                    speed = autoTuneSpeed
                ),
                autoMixSettings = AutoMixSettings(
                    isEnabled = true,
                    kickSidechainDucking = kickSidechain,
                    vocalClarityBoost = vocalClarity,
                    reverbSpace = reverbSpace,
                    stereoWidth = 0.75f
                ),
                autoMasterSettings = AutoMasterSettings(
                    isEnabled = true,
                    analogWarmth = analogWarmth,
                    loudnessTarget = loudnessTarget,
                    bassPunch = 0.60f,
                    brickwallLimiter = true
                )
            )
        } catch (e: Exception) {
            composeLocalArrangement(originalPrompt)
        }
    }

    /**
     * Intelligent local music composer rule engine.
     * Evaluates prompt semantics to configure genre, BPM, key, chords, and mix parameters.
     */
    fun composeLocalArrangement(prompt: String): ArrangementRecipe {
        val lower = prompt.lowercase()

        val isReggaeton = lower.contains("reggaeton") || lower.contains("dembow") || lower.contains("latin") || lower.contains("bad bunny")
        val isTrap = lower.contains("trap") || lower.contains("808") || lower.contains("hip hop") || lower.contains("drill")
        val isAfro = lower.contains("afro") || lower.contains("dancehall") || lower.contains("amapiano")
        val isSynth = lower.contains("synth") || lower.contains("retro") || lower.contains("80s") || lower.contains("wave")
        val isLofi = lower.contains("lo-fi") || lower.contains("lofi") || lower.contains("chill") || lower.contains("relax")

        val genre = when {
            isReggaeton -> "Reggaeton"
            isTrap -> "Trap / Hip-Hop"
            isAfro -> "Afrobeats"
            isSynth -> "Synthwave 80s"
            isLofi -> "Acoustic Lo-Fi"
            else -> "Modern Pop / Urban"
        }

        val bpm = when {
            isReggaeton -> 96
            isTrap -> 140
            isAfro -> 104
            isSynth -> 124
            isLofi -> 78
            else -> 105
        }

        val key = when {
            lower.contains("d minor") || lower.contains("dm") -> "D Minor"
            lower.contains("c major") || lower.contains("c ") -> "C Major"
            lower.contains("f minor") || lower.contains("fm") -> "F Minor"
            lower.contains("g minor") || lower.contains("gm") -> "G Minor"
            lower.contains("e minor") || lower.contains("em") -> "E Minor"
            else -> "A Minor"
        }

        val chords = when (key) {
            "D Minor" -> listOf("Dm", "Bb", "F", "C")
            "C Major" -> listOf("C", "G", "Am", "F")
            "F Minor" -> listOf("Fm", "C#m", "Gm", "C")
            "G Minor" -> listOf("Gm", "Eb", "Bb", "F")
            "E Minor" -> listOf("Em", "C", "G", "D")
            else -> listOf("Am", "F", "C", "G")
        }

        val hasGuitar = lower.contains("guitar") || lower.contains("pluck")
        val hasDeepBass = lower.contains("deep bass") || lower.contains("808") || lower.contains("sub")

        return ArrangementRecipe(
            genre = genre,
            bpm = bpm,
            musicalKey = key,
            chordProgression = chords,
            prompt = prompt,
            drumStyle = if (isReggaeton) "Dembow Tresillo (3:3:2 syncopation)" else if (isTrap) "Hard 808 with rolling hi-hats" else "Dynamic rhythm kit",
            bassStyle = if (hasDeepBass) "808 Heavy Sub (saturated)" else "Warm rounded bassline",
            chordStyle = if (hasGuitar) "Acoustic & Nylon Guitar Plucks" else "Lush electric piano chords",
            leadStyle = if (hasGuitar) "Spanish Guitar Counter-melody" else "Singing lead melody",
            fxStyle = "Vinyl atmosphere, reverse cymbal risers & sub drops",
            autoTuneSettings = AutoTuneSettings(
                isEnabled = true,
                targetKey = key,
                speed = if (isTrap || isReggaeton) 0.88f else 0.65f
            ),
            autoMixSettings = AutoMixSettings(
                isEnabled = true,
                kickSidechainDucking = true,
                vocalClarityBoost = true,
                reverbSpace = if (isLofi || isSynth) 0.40f else 0.26f,
                stereoWidth = 0.75f
            ),
            autoMasterSettings = AutoMasterSettings(
                isEnabled = true,
                analogWarmth = if (isLofi || isReggaeton) 0.55f else 0.40f,
                loudnessTarget = 0.82f,
                bassPunch = if (isTrap || isReggaeton) 0.70f else 0.50f,
                brickwallLimiter = true
            )
        )
    }
}
