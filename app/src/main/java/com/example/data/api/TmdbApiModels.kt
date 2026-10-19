package com.example.data.api

import java.io.Serializable

data class TmdbMovie(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    val poster_path: String? = null,
    val backdrop_path: String? = null,
    val release_date: String? = null,
    val first_air_date: String? = null,
    val vote_average: Double? = 0.0,
    val vote_count: Int? = 0,
    val popularity: Double? = 0.0,
    val genre_ids: List<Int>? = emptyList(),
    val media_type: String? = null
) : Serializable {
    val displayTitle: String get() = title ?: name ?: "Unknown Title"
    val displayYear: String get() = (release_date ?: first_air_date ?: "").take(4).ifBlank { "2024" }
    val fullPosterUrl: String get() = poster_path?.let { "https://image.tmdb.org/t/p/w500$it" } ?: ""
    val fullBackdropUrl: String get() = backdrop_path?.let { "https://image.tmdb.org/t/p/w1280$it" } ?: ""
}

data class TmdbPaginatedResponse<T>(
    val page: Int? = 1,
    val results: List<T> = emptyList(),
    val total_pages: Int? = 1,
    val total_results: Int? = 0
) : Serializable

data class TmdbGenre(
    val id: Int,
    val name: String
) : Serializable

data class TmdbGenreResponse(
    val genres: List<TmdbGenre> = emptyList()
) : Serializable
