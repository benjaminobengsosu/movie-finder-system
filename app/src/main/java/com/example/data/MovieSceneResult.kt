package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.json.JSONObject

/**
 * Data class representing the parsed movie scene identification result returned by Gemini AI.
 * Contains title, release year, director, and IMDb link.
 */
@JsonClass(generateAdapter = true)
data class MovieSceneResult(
    @Json(name = "title") val title: String = "",
    @Json(name = "year") val year: String = "",
    @Json(name = "director") val director: String = "",
    @Json(name = "imdbLink") val imdbLink: String = "",
    @Json(name = "imdbUrl") val imdbUrl: String = "",
    @Json(name = "confidence") val confidence: String = "High",
    @Json(name = "rationale") val rationale: String = "",
    @Json(name = "actors") val actors: String = "",
    @Json(name = "genre") val genre: String = "",
    @Json(name = "thematicElements") val thematicElements: String = "",
    @Transient val rawJson: String = ""
) {
    /**
     * Resolves the IMDb link whether supplied under [imdbLink] or [imdbUrl].
     */
    val resolvedImdbLink: String
        get() = when {
            imdbLink.isNotBlank() -> imdbLink
            imdbUrl.isNotBlank() -> imdbUrl
            else -> "https://www.imdb.com"
        }

    /**
     * Converts this result to a parsed [JSONObject] containing title, year, director, and imdbLink.
     */
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("title", title)
            put("year", year)
            put("director", director)
            put("imdbLink", resolvedImdbLink)
            put("imdbUrl", resolvedImdbLink)
            if (confidence.isNotBlank()) put("confidence", confidence)
            if (rationale.isNotBlank()) put("rationale", rationale)
            if (actors.isNotBlank()) put("actors", actors)
            if (genre.isNotBlank()) put("genre", genre)
            if (thematicElements.isNotBlank()) put("thematicElements", thematicElements)
        }
    }

    /**
     * Returns the formatted JSON string representing this result.
     */
    fun toJsonString(): String {
        return toJsonObject().toString(2)
    }
}
