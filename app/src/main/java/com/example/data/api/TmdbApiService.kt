package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface TmdbApiService {

    @GET("3/trending/movie/day")
    suspend fun getTrendingMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<TmdbMovie>

    @GET("3/genre/movie/list")
    suspend fun getMovieGenres(
        @Query("api_key") apiKey: String
    ): TmdbGenreResponse

    @GET("3/discover/movie")
    suspend fun discoverMovies(
        @Query("api_key") apiKey: String,
        @Query("with_genres") withGenres: String? = null,
        @Query("primary_release_year") primaryReleaseYear: Int? = null,
        @Query("vote_average.gte") minRating: Float? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<TmdbMovie>

    @GET("3/search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("primary_release_year") primaryReleaseYear: Int? = null,
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<TmdbMovie>

    @GET("3/movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TmdbMovie

    companion object {
        private const val BASE_URL = "https://api.themoviedb.org/"

        fun getApiKey(): String {
            return try {
                val key = com.example.BuildConfig.TMDB_API_KEY
                if (key.isBlank() || key == "MY_TMDB_API_KEY") "" else key
            } catch (_: Exception) {
                ""
            }
        }

        fun create(): TmdbApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            return retrofit.create(TmdbApiService::class.java)
        }
    }
}
