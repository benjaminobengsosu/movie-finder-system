package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class SchemaProperty(
    @Json(name = "type") val type: String,
    @Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class ResponseSchema(
    @Json(name = "type") val type: String,
    @Json(name = "properties") val properties: Map<String, SchemaProperty>? = null,
    @Json(name = "required") val required: List<String>? = null,
    @Json(name = "items") val items: ResponseSchema? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "responseSchema") val responseSchema: ResponseSchema? = null,
    @Json(name = "temperature") val temperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)

// The structure of the JSON returned inside the candidate's text
@JsonClass(generateAdapter = true)
data class MovieIdentificationResult(
    @Json(name = "title") val title: String,
    @Json(name = "year") val year: String,
    @Json(name = "director") val director: String,
    @Json(name = "imdbUrl") val imdbUrl: String,
    @Json(name = "confidence") val confidence: String, // e.g. "High", "Medium", "Low"
    @Json(name = "rationale") val rationale: String,
    @Json(name = "actors") val actors: String, // e.g., "Leonardo DiCaprio, Joseph Gordon-Levitt"
    @Json(name = "genre") val genre: String, // e.g., "Sci-Fi / Thriller"
    @Json(name = "thematicElements") val thematicElements: String // e.g. "Duality, guilt, corporate espionage, dreams vs reality"
)

@JsonClass(generateAdapter = true)
data class RecommendedMovie(
    @Json(name = "title") val title: String,
    @Json(name = "year") val year: String,
    @Json(name = "director") val director: String,
    @Json(name = "genre") val genre: String,
    @Json(name = "rationale") val rationale: String,
    @Json(name = "imdbUrl") val imdbUrl: String
)
