package com.example.data

import android.graphics.Bitmap
import org.json.JSONObject

/**
 * Repository interface for identifying movie scenes using Gemini multimodal AI models.
 */
interface MovieSceneRepository {
    /**
     * Processes an image of a movie scene and returns parsed [MovieSceneResult]
     * containing title, year, director, and IMDb link.
     */
    suspend fun processSceneImage(
        bitmap: Bitmap,
        contextPrompt: String = "",
        model: String = "gemini-3.5-flash"
    ): MovieSceneResult

    /**
     * Processes an image byte array of a movie scene and returns parsed [MovieSceneResult].
     */
    suspend fun processSceneImage(
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg",
        contextPrompt: String = "",
        model: String = "gemini-3.5-flash"
    ): MovieSceneResult

    /**
     * Processes an image of a movie scene and returns a parsed [JSONObject] containing
     * "title", "year", "director", and "imdbLink".
     */
    suspend fun processSceneImageAsJson(
        bitmap: Bitmap,
        contextPrompt: String = "",
        model: String = "gemini-3.5-flash"
    ): JSONObject

    /**
     * Processes an image of a movie scene and returns the raw parsed JSON string.
     */
    suspend fun processSceneImageAsJsonString(
        bitmap: Bitmap,
        contextPrompt: String = "",
        model: String = "gemini-3.5-flash"
    ): String

    /**
     * Convenience alias for [processSceneImage].
     */
    suspend fun identifyMovie(
        bitmap: Bitmap,
        contextPrompt: String = "",
        model: String = "gemini-3.5-flash"
    ): MovieSceneResult = processSceneImage(bitmap, contextPrompt, model)

    /**
     * Processes a textual scene description when no visual media is available.
     */
    suspend fun processSceneDescription(
        description: String,
        model: String = "gemini-3.5-flash"
    ): MovieSceneResult

    companion object {
        fun create(
            apiService: com.example.data.api.GeminiApiService = com.example.data.api.GeminiApiService.create(),
            apiKey: String = com.example.BuildConfig.GEMINI_API_KEY
        ): MovieSceneRepository = GeminiMovieSceneRepository(apiService, apiKey)

        operator fun invoke(
            apiService: com.example.data.api.GeminiApiService = com.example.data.api.GeminiApiService.create(),
            apiKey: String = com.example.BuildConfig.GEMINI_API_KEY
        ): MovieSceneRepository = GeminiMovieSceneRepository(apiService, apiKey)
    }
}
