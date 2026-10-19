package com.example

import android.graphics.Bitmap
import com.example.data.GeminiMovieSceneRepository
import com.example.data.MovieSceneResult
import com.example.data.api.Candidate
import com.example.data.api.Content
import com.example.data.api.GeminiApiService
import com.example.data.api.GeminiRequest
import com.example.data.api.GeminiResponse
import com.example.data.api.Part
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MovieSceneRepositoryTest {

    @Test
    fun `MovieSceneResult converts to valid JSONObject with title year director and imdbLink`() {
        val result = MovieSceneResult(
            title = "Blade Runner 2049",
            year = "2017",
            director = "Denis Villeneuve",
            imdbLink = "https://www.imdb.com/title/tt1856101/"
        )

        val json = result.toJsonObject()
        assertEquals("Blade Runner 2049", json.getString("title"))
        assertEquals("2017", json.getString("year"))
        assertEquals("Denis Villeneuve", json.getString("director"))
        assertEquals("https://www.imdb.com/title/tt1856101/", json.getString("imdbLink"))
    }

    @Test
    fun `parseJsonResult parses raw JSON correctly`() {
        val repo = GeminiMovieSceneRepository()
        val rawJson = """
            {
                "title": "Inception",
                "year": "2010",
                "director": "Christopher Nolan",
                "imdbLink": "https://www.imdb.com/title/tt1375666/",
                "confidence": "High",
                "rationale": "Spinning totem on table",
                "actors": "Leonardo DiCaprio, Elliot Page",
                "genre": "Sci-Fi"
            }
        """.trimIndent()

        val parsed = repo.parseJsonResult(rawJson)
        assertEquals("Inception", parsed.title)
        assertEquals("2010", parsed.year)
        assertEquals("Christopher Nolan", parsed.director)
        assertEquals("https://www.imdb.com/title/tt1375666/", parsed.imdbLink)
        assertEquals("https://www.imdb.com/title/tt1375666/", parsed.resolvedImdbLink)
    }

    @Test
    fun `parseJsonResult handles markdown code fences`() {
        val repo = GeminiMovieSceneRepository()
        val fencedJson = """
            ```json
            {
                "title": "Interstellar",
                "year": "2014",
                "director": "Christopher Nolan",
                "imdbLink": "https://www.imdb.com/title/tt0816692/"
            }
            ```
        """.trimIndent()

        val parsed = repo.parseJsonResult(fencedJson)
        assertEquals("Interstellar", parsed.title)
        assertEquals("2014", parsed.year)
        assertEquals("Christopher Nolan", parsed.director)
        assertEquals("https://www.imdb.com/title/tt0816692/", parsed.imdbLink)
    }

    @Test
    fun `processSceneImage calls Gemini API and returns parsed movie scene result`() = runTest {
        val fakeService = object : GeminiApiService {
            override suspend fun generateContent(
                model: String,
                apiKey: String,
                request: GeminiRequest
            ): GeminiResponse {
                val jsonPayload = """
                    {
                        "title": "Spirited Away",
                        "year": "2001",
                        "director": "Hayao Miyazaki",
                        "imdbLink": "https://www.imdb.com/title/tt0245429/",
                        "confidence": "High",
                        "rationale": "Train traveling over flooded tracks",
                        "actors": "Rumi Hiiragi, Miyu Irino",
                        "genre": "Animation / Fantasy"
                    }
                """.trimIndent()

                return GeminiResponse(
                    candidates = listOf(
                        Candidate(
                            content = Content(
                                parts = listOf(Part(text = jsonPayload))
                            )
                        )
                    )
                )
            }
        }

        val repository = GeminiMovieSceneRepository(
            apiService = fakeService,
            apiKey = "test-api-key"
        )

        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val result = repository.processSceneImage(bitmap)

        assertNotNull(result)
        assertEquals("Spirited Away", result.title)
        assertEquals("2001", result.year)
        assertEquals("Hayao Miyazaki", result.director)
        assertEquals("https://www.imdb.com/title/tt0245429/", result.imdbLink)
        assertEquals("https://www.imdb.com/title/tt0245429/", result.resolvedImdbLink)

        val json = repository.processSceneImageAsJson(bitmap)
        assertEquals("Spirited Away", json.getString("title"))
        assertEquals("2001", json.getString("year"))
        assertEquals("Hayao Miyazaki", json.getString("director"))
        assertEquals("https://www.imdb.com/title/tt0245429/", json.getString("imdbLink"))
    }
}
