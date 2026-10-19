package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieMatchDao {
    @Query("SELECT * FROM movie_matches ORDER BY timestamp DESC")
    fun getAllMatches(): Flow<List<MovieMatch>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MovieMatch): Long

    @Update
    suspend fun updateMatch(match: MovieMatch)

    @Query("SELECT * FROM movie_matches WHERE isInWatchlist = 1 ORDER BY timestamp DESC")
    fun getWatchlistMatches(): Flow<List<MovieMatch>>

    @Query("SELECT * FROM movie_matches WHERE id = :id LIMIT 1")
    suspend fun getMatchById(id: Int): MovieMatch?

    @Query("SELECT * FROM movie_matches WHERE LOWER(title) = LOWER(:title) LIMIT 1")
    suspend fun findMatchByTitle(title: String): MovieMatch?

    @Query("SELECT * FROM movie_matches WHERE LOWER(title) = LOWER(:title) AND year = :year LIMIT 1")
    suspend fun findMatchByTitleAndYear(title: String, year: String): MovieMatch?

    @Query("DELETE FROM movie_matches WHERE id = :id")
    suspend fun deleteMatchById(id: Int)

    @Query("DELETE FROM movie_matches")
    suspend fun clearAllMatches()
}
