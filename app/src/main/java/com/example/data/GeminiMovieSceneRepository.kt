package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GeminiApiService
import com.example.data.api.GeminiRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.InlineData
import com.example.data.api.Part
import com.example.data.api.ResponseSchema
import com.example.data.api.SchemaProperty
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * Repository implementation that calls the Gemini API to analyze images of movie scenes
 * and returns parsed JSON containing title, year, director, and IMDb link.
 */
open class GeminiMovieSceneRepository(
    private val apiService: GeminiApiService = GeminiApiService.create(),
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
) : MovieSceneRepository {

    private val resultAdapter by lazy {
        moshi.adapter(MovieSceneResult::class.java)
    }

    override suspend fun processSceneImage(
        bitmap: Bitmap,
        contextPrompt: String,
        model: String
    ): MovieSceneResult = withContext(Dispatchers.IO) {
        val resized = resizeBitmapIfNeeded(bitmap, 1440)
        val base64Data = resized.toBase64(90)
        processImageInternal(
            inlineData = InlineData(mimeType = "image/jpeg", data = base64Data),
            contextPrompt = contextPrompt,
            model = model
        )
    }

    override suspend fun processSceneImage(
        imageBytes: ByteArray,
        mimeType: String,
        contextPrompt: String,
        model: String
    ): MovieSceneResult = withContext(Dispatchers.IO) {
        val base64Data = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        processImageInternal(
            inlineData = InlineData(mimeType = mimeType, data = base64Data),
            contextPrompt = contextPrompt,
            model = model
        )
    }

    override suspend fun processSceneImageAsJson(
        bitmap: Bitmap,
        contextPrompt: String,
        model: String
    ): JSONObject {
        val result = processSceneImage(bitmap, contextPrompt, model)
        return result.toJsonObject()
    }

    override suspend fun processSceneImageAsJsonString(
        bitmap: Bitmap,
        contextPrompt: String,
        model: String
    ): String {
        val result = processSceneImage(bitmap, contextPrompt, model)
        return result.toJsonString()
    }

    override suspend fun processSceneDescription(
        description: String,
        model: String
    ): MovieSceneResult = withContext(Dispatchers.IO) {
        processImageInternal(
            inlineData = null,
            contextPrompt = description,
            model = model
        )
    }

    /**
     * Convenience method to process an image and return parsed movie details.
     */
    suspend fun processImage(bitmap: Bitmap): MovieSceneResult {
        return processSceneImage(bitmap)
    }

    /**
     * Internal implementation to construct the Gemini request, execute it,
     * extract the response text, and parse the structured JSON.
     */
    private suspend fun processImageInternal(
        inlineData: InlineData?,
        contextPrompt: String,
        model: String
    ): MovieSceneResult {
        val activeKey = apiKey.ifBlank { BuildConfig.GEMINI_API_KEY }
        if (activeKey.isBlank() || activeKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException(
                "Gemini API Key is missing. Please configure it in the AI Studio Secrets panel."
            )
        }

        val promptBuilder = StringBuilder()
        if (inlineData != null) {
            promptBuilder.append(
                "You are tasked with identifying the exact film, movie, or television series shown in this picture. " +
                "This image may be a direct cinema still, streaming screenshot, film frame, or a camera photograph taken of a television, monitor, or laptop display playing a movie.\n\n" +
                "VISUAL DEDUCTION WORKFLOW:\n" +
                "1. Actor & Character Identification: Meticulously inspect faces, bone structure, eye shape, hairstyle, age, and expressions. Even if under prosthetics, period wigs, heavy makeup, or animated character design, recognize who the actors or characters are.\n" +
                "2. Costumes, Period & Styling: Examine outfits, armor, uniforms, badges, period attire, futuristic equipment, or civilian clothing to establish the era, setting, and exact production.\n" +
                "3. Props, Vehicles & Weapons: Look for distinctive vehicles, signature gadgets, iconic weapons, or props unique to this specific movie.\n" +
                "4. Legible Text & Subtitles: Read any visible subtitles, background signs, newspapers, computer screens, vehicle license plates, or logos.\n" +
                "5. Cinematography & Aesthetic: Analyze lighting, color grading (noir, vibrant technicolor, desaturated gritty, cyberpunk teal & orange, 80s film grain, anime style), aspect ratio, and director hallmarks.\n" +
                "6. Screen Glare / Artifact Tolerance: If the photo was taken of a TV/monitor screen with slight reflections, scan lines, tilt, or moiré, penetrate through the display artifacts to analyze the movie content itself.\n" +
                "7. In the 'rationale' field: Explain step-by-step the visual clues (actors recognized, costumes, setting, scene action) and how they conclusively identify this film.\n" +
                "8. Return the official canonical title, release year, director (or animation studio), valid IMDb link (e.g. https://www.imdb.com/title/tt1375666/), recognized cast, genre, and thematic elements.\n" +
                "9. Always deduce the single most probable movie from the visual clues. Do NOT return 'Unknown' unless the image is entirely pitch black, completely blank, or total non-visual noise."
            )
            if (contextPrompt.isNotBlank()) {
                promptBuilder.append("\n\nADDITIONAL USER CLUES / QUOTES: '").append(contextPrompt).append("'. Use both the picture and these clues together to pinpoint the movie.")
            }
        } else {
            promptBuilder.append(
                "You are tasked with identifying the exact film or show title from this movie scene description, dialogue quotes, or cinema plot clues: '$contextPrompt'.\n\n" +
                "ANALYSIS INSTRUCTIONS:\n" +
                "1. Recognize memorable quotes, distinctive dialogue, iconic character catchphrases, or specific plot sequences.\n" +
                "2. Cross-reference character names, conflicts, settings, and scenarios against cinema history.\n" +
                "3. In the 'rationale' field: Explain step-by-step why these clues uniquely match the identified film.\n" +
                "4. Return the official canonical title, release year, director, valid IMDb link (e.g. https://www.imdb.com/title/tt...), cast, genre, and thematic elements.\n" +
                "5. Deduce the most probable film title from the clues rather than giving up prematurely."
            )
        }

        val parts = mutableListOf<Part>()
        parts.add(Part(text = promptBuilder.toString()))
        if (inlineData != null) {
            parts.add(Part(inlineData = inlineData))
        }

        val request = GeminiRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = ResponseSchema(
                    type = "OBJECT",
                    properties = linkedMapOf(
                        "rationale" to SchemaProperty("STRING", "Detailed visual and contextual analysis: recognized actors, costumes, props, setting, cinematography, and step-by-step deduction leading to the film identification"),
                        "title" to SchemaProperty("STRING", "Exact official title of the movie or television show"),
                        "year" to SchemaProperty("STRING", "Year of release (e.g. 2014)"),
                        "director" to SchemaProperty("STRING", "Director's name or animation studio"),
                        "imdbLink" to SchemaProperty("STRING", "Valid IMDb title page link (e.g. https://www.imdb.com/title/tt1375666/)"),
                        "imdbUrl" to SchemaProperty("STRING", "Valid IMDb title page URL"),
                        "confidence" to SchemaProperty("STRING", "High, Medium, or Low"),
                        "actors" to SchemaProperty("STRING", "Main actors or characters identified in this scene"),
                        "genre" to SchemaProperty("STRING", "Film genres (e.g. Sci-Fi, Action, Drama)"),
                        "thematicElements" to SchemaProperty("STRING", "Key themes, motifs, or iconic scene elements")
                    ),
                    required = listOf("rationale", "title", "year", "director", "imdbLink")
                ),
                temperature = 0.2
            ),
            systemInstruction = Content(
                parts = listOf(
                    Part(
                        "You are a master film archivist, world-renowned cinema historian, and premier multimodal visual movie recognition engine. " +
                        "You possess exhaustive knowledge of world cinema, Hollywood classics, modern blockbusters, indie cinema, television series, anime, and international films. " +
                        "Your mission is to examine movie scene images, film frames, TV stills, or dialogue clues, perform rigorous visual deduction, and return exact, accurate movie metadata as a parsed JSON object matching the schema."
                    )
                )
            )
        )

        val response = apiService.generateContent(
            model = model.ifBlank { "gemini-3.5-flash" },
            apiKey = activeKey,
            request = request
        )

        val candidateText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Gemini API returned an empty candidate response.")

        val cleanJson = extractJsonFromString(candidateText)
        return parseJsonResult(cleanJson)
    }

    /**
     * Parses the JSON string into a [MovieSceneResult] instance.
     * Ensures title, year, director, and imdbLink are robustly populated,
     * even if surrounded by markdown code fences or conversational text.
     */
    fun parseJsonResult(jsonString: String): MovieSceneResult {
        val cleaned = extractJsonFromString(jsonString)
        return try {
            val parsed = resultAdapter.fromJson(cleaned)
            if (parsed != null && parsed.title.isNotBlank()) {
                val resolvedLink = when {
                    parsed.imdbLink.isNotBlank() -> normalizeImdbUrl(parsed.imdbLink)
                    parsed.imdbUrl.isNotBlank() -> normalizeImdbUrl(parsed.imdbUrl)
                    else -> "https://www.imdb.com"
                }
                parsed.copy(
                    imdbLink = resolvedLink,
                    imdbUrl = resolvedLink,
                    rawJson = cleaned
                )
            } else {
                parseWithJsonObjectFallback(cleaned)
            }
        } catch (_: Exception) {
            parseWithJsonObjectFallback(cleaned)
        }
    }

    private fun parseWithJsonObjectFallback(jsonString: String): MovieSceneResult {
        return try {
            val obj = JSONObject(jsonString)
            val title = obj.optString("title", "Unknown Title")
            val year = obj.optString("year", "")
            val director = obj.optString("director", "")
            val link = when {
                obj.has("imdbLink") && obj.getString("imdbLink").isNotBlank() -> obj.getString("imdbLink")
                obj.has("imdbUrl") && obj.getString("imdbUrl").isNotBlank() -> obj.getString("imdbUrl")
                else -> "https://www.imdb.com"
            }
            val normalizedLink = normalizeImdbUrl(link)
            MovieSceneResult(
                title = title,
                year = year,
                director = director,
                imdbLink = normalizedLink,
                imdbUrl = normalizedLink,
                confidence = obj.optString("confidence", "Medium"),
                rationale = obj.optString("rationale", ""),
                actors = obj.optString("actors", ""),
                genre = obj.optString("genre", ""),
                thematicElements = obj.optString("thematicElements", ""),
                rawJson = jsonString
            )
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse Gemini movie scene JSON: ${e.message}", e)
        }
    }

    private fun normalizeImdbUrl(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("www.imdb.com") -> "https://$trimmed"
            trimmed.startsWith("imdb.com") -> "https://$trimmed"
            trimmed.startsWith("tt") -> "https://www.imdb.com/title/$trimmed"
            trimmed.isNotBlank() -> "https://www.imdb.com/find?q=${trimmed.replace(" ", "+")}"
            else -> "https://www.imdb.com"
        }
    }

    fun extractJsonFromString(input: String): String {
        val trimmed = input.trim()
        val firstBracket = trimmed.indexOfAny(charArrayOf('{', '['))
        val lastBracket = trimmed.lastIndexOfAny(charArrayOf('}', ']'))
        if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
            return trimmed.substring(firstBracket, lastBracket + 1)
        }
        return trimmed
    }

    private fun resizeBitmapIfNeeded(bitmap: Bitmap, maxSize: Int = 1440): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val (newWidth, newHeight) = if (ratio > 1f) {
            maxSize to (maxSize / ratio).toInt()
        } else {
            (maxSize * ratio).toInt() to maxSize
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth.coerceAtLeast(1), newHeight.coerceAtLeast(1), true)
    }

    private fun Bitmap.toBase64(quality: Int = 90): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
