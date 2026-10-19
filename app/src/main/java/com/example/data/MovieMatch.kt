package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "movie_matches")
data class MovieMatch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val year: String,
    val director: String,
    val imdbUrl: String,
    val confidence: String,
    val rationale: String,
    val actors: String, // Comma-separated or serialized
    val genre: String,
    val searchType: String, // "VIDEO", "IMAGE", or "TEXT"
    val mediaUri: String? = null, // URI of picked image or video (if any)
    val textQuery: String? = null, // Input scene description (if any)
    val timestamp: Long = System.currentTimeMillis(),
    val thematicElements: String = "",
    val isInWatchlist: Boolean = false,
    val watchlistStatus: String = "Plan to Watch", // "Plan to Watch", "Watching", "Completed"
    val watchlistNotes: String = "",
    val customListCategory: String = "Must Watch", // "Must Watch", "Weekend Binge", "With Friends", "Top Favorites"
    val posterPath: String = ""
) : Serializable
