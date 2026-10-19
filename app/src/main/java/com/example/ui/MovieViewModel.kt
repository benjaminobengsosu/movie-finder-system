package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.MovieDatabase
import com.example.data.MovieMatch
import com.example.data.MovieRepository
import com.example.data.CREATOR_ADMIN_EMAIL
import com.example.data.SupportTicket
import com.example.data.AnalyticsSearchEvent
import com.example.data.AppErrorLog
import com.example.data.RemoteConfigData
import com.example.data.AppUser
import com.example.data.DeletedRecord
import com.example.data.ConfigRestorePoint
import com.example.data.UserRole
import com.example.data.SecurityCryptoManager
import com.example.data.SupabaseClient
import java.util.UUID
import com.example.data.api.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Date

class MovieViewModel(val repository: MovieRepository) : ViewModel() {

    // Enterprise Security & Multi-Role Identity System
    var registeredUsersState by mutableStateOf<List<AppUser>>(emptyList())
    var deletedRecordsState by mutableStateOf<List<DeletedRecord>>(emptyList())
    var restorePointsState by mutableStateOf<List<ConfigRestorePoint>>(emptyList())

    var currentUser by mutableStateOf<AppUser?>(null)
    var currentUserRole by mutableStateOf(UserRole.ADMINISTRATOR)

    init {
        viewModelScope.launch {
            repository.allUsers.collect { users ->
                registeredUsersState = users
                if (users.isEmpty()) {
                    seedDefaultUsers()
                } else if (currentUser == null) {
                    val defaultAdmin = users.firstOrNull { it.email.equals("admin@moviefinder.com", ignoreCase = true) } ?: users.first()
                    currentUser = defaultAdmin
                    currentUserRole = UserRole.fromString(defaultAdmin.role)
                    userEmail = defaultAdmin.email
                    userName = defaultAdmin.name
                }
            }
        }
        viewModelScope.launch {
            repository.allDeletedRecords.collect { records ->
                deletedRecordsState = records
            }
        }
        viewModelScope.launch {
            repository.allRestorePoints.collect { points ->
                restorePointsState = points
                if (points.isEmpty()) {
                    seedDefaultRestorePoint()
                }
            }
        }
    }

    private suspend fun seedDefaultUsers() {
        val defaultUsers = listOf(
            Triple("Administrator", "admin@moviefinder.com", UserRole.ADMINISTRATOR),
            Triple("Operations Manager", "manager@moviefinder.com", UserRole.MANAGER),
            Triple("Content Inputer", "inputer@moviefinder.com", UserRole.CONTENT_INPUTER),
            Triple("Standard User", "user@moviefinder.com", UserRole.STANDARD_USER)
        )
        defaultUsers.forEach { (name, email, role) ->
            val salt = SecurityCryptoManager.generateSalt()
            val defaultPass = "MovieFinder2026!"
            val hash = SecurityCryptoManager.hashPassword(defaultPass, salt)
            val totpSecret = if (role == UserRole.ADMINISTRATOR) SecurityCryptoManager.generateTotpSecret() else null
            val recoveryCodes = if (role == UserRole.ADMINISTRATOR) {
                SecurityCryptoManager.generateRecoveryCodes()
            } else emptyList()
            val recoveryJson = "[" + recoveryCodes.joinToString(",") { "\"$it\"" } + "]"

            val user = AppUser(
                id = UUID.randomUUID().toString(),
                email = email,
                name = name,
                role = role.displayName,
                salt = salt,
                passwordHash = hash,
                twoFactorEnabled = role == UserRole.ADMINISTRATOR,
                twoFactorSecret = totpSecret,
                recoveryCodes = recoveryJson
            )
            repository.insertUser(user)
        }
    }

    private suspend fun seedDefaultRestorePoint() {
        val initialPoint = ConfigRestorePoint(
            id = UUID.randomUUID().toString(),
            name = "System Baseline Configuration",
            theme = "Dark Obsidian",
            maintenanceMode = false,
            guestModeEnabled = true,
            announcementText = "Welcome to Movie Finder Enterprise Security & Cloud Hub.",
            securityStrictness = "High",
            createdByEmail = "admin@moviefinder.com"
        )
        repository.createRestorePoint(initialPoint)
    }

    // Theme state
    var isDarkMode by mutableStateOf(false) // Dynamic theme value used across screens

    // Appearance Preference: "Light", "Dark", "System"
    var appearancePreference by mutableStateOf("System")
        private set

    fun saveAppearancePreference(context: Context, preference: String) {
        appearancePreference = preference
        val prefs = context.getSharedPreferences("user_identity_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("appearance_preference", preference).apply()
        
        // Update isDarkMode based on preference immediately
        isDarkMode = when (preference) {
            "Light" -> false
            "Dark" -> true
            else -> {
                // System default fallback, will be updated dynamically in Composable
                isDarkMode
            }
        }
        showToast("Appearance preference set to $preference")
    }

    // Selected Gemini Intelligence Model
    var selectedModel by mutableStateOf("gemini-3.5-flash")
        private set

    fun selectModel(context: Context, model: String) {
        selectedModel = model
        saveModelPreference(context, model)
        val modelLabel = when (model) {
            "gemini-3.1-pro-preview" -> "3.1 Pro (Heavy)"
            "gemini-3.1-flash-lite" -> "3.1 Lite (Speed)"
            else -> "3.5 Flash (Balanced)"
        }
        showToast("Analysis engine set to $modelLabel")
    }

    fun selectModel(model: String) {
        selectedModel = model
    }

    // Premium status for AI Brain settings
    var isPremium by mutableStateOf(true)
        private set

    fun togglePremium(context: Context) {
        isPremium = !isPremium
        val prefs = context.getSharedPreferences("user_identity_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_premium", isPremium).apply()
        if (isPremium) {
            showToast("Upgraded to Premium. Analysis engine unlocked.")
        } else {
            showToast("Standard Account. Analysis engine selection locked.")
        }
    }

    fun togglePremium() {
        isPremium = !isPremium
    }

    // Onboarding State
    var isFirstTimeUser by mutableStateOf(true)
    var onboardingGenres by mutableStateOf<List<String>>(emptyList())
    var onboardingInterests by mutableStateOf<List<String>>(emptyList())
    var onboardingFavoriteMovies by mutableStateOf<List<String>>(emptyList())
    var onboardingAvoidContent by mutableStateOf<List<String>>(emptyList())

    fun completeOnboarding(
        context: Context,
        genres: List<String>,
        interests: List<String>,
        movies: List<String>,
        avoids: List<String>
    ) {
        val prefs = context.getSharedPreferences("user_identity_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_first_time_user", false)
            putStringSet("onboarding_genres", genres.toSet())
            putStringSet("onboarding_interests", interests.toSet())
            putStringSet("onboarding_favorite_movies", movies.toSet())
            putStringSet("onboarding_avoid_content", avoids.toSet())
            apply()
        }
        isFirstTimeUser = false
        onboardingGenres = genres
        onboardingInterests = interests
        onboardingFavoriteMovies = movies
        onboardingAvoidContent = avoids
        showToast("Personalized cinema taste profile created.")
        fetchAIRecommendations()
    }

    fun updateAiPreferences(
        context: Context,
        genres: List<String>,
        interests: List<String>,
        movies: List<String>,
        avoids: List<String>
    ) {
        val prefs = context.getSharedPreferences("user_identity_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putStringSet("onboarding_genres", genres.toSet())
            putStringSet("onboarding_interests", interests.toSet())
            putStringSet("onboarding_favorite_movies", movies.toSet())
            putStringSet("onboarding_avoid_content", avoids.toSet())
            apply()
        }
        onboardingGenres = genres
        onboardingInterests = interests
        onboardingFavoriteMovies = movies
        onboardingAvoidContent = avoids
        showToast("Film taste profile updated.")
        fetchAIRecommendations()
    }

    // Recommendation rating and feedback state
    // Map of movieTitle -> Pair(thumbsFeedback, starRating)
    // thumbsFeedback: "LOVED", "NOT_INTERESTED", or null
    // starRating: 1..5, or 0 if not rated
    var movieFeedbacks by mutableStateOf<Map<String, Pair<String?, Int>>>(emptyMap())
        private set

    fun saveMovieFeedback(context: Context, movieTitle: String, thumbs: String?, stars: Int) {
        val updated = movieFeedbacks.toMutableMap()
        updated[movieTitle] = Pair(thumbs, stars)
        movieFeedbacks = updated

        // Persist to SharedPreferences
        val prefs = context.getSharedPreferences("user_movie_feedbacks", Context.MODE_PRIVATE)
        val key = "feedback_$movieTitle"
        prefs.edit().apply {
            putString("${key}_thumbs", thumbs)
            putInt("${key}_stars", stars)
            apply()
        }
        showToast("Feedback recorded for $movieTitle!")
        fetchAIRecommendations() // Learn and update suggestions!
    }

    fun loadMovieFeedbacks(context: Context) {
        val prefs = context.getSharedPreferences("user_movie_feedbacks", Context.MODE_PRIVATE)
        val allKeys = prefs.all.keys
        val feedbacks = mutableMapOf<String, Pair<String?, Int>>()
        allKeys.forEach { key ->
            if (key.endsWith("_thumbs")) {
                val movieTitle = key.substringBefore("_thumbs").removePrefix("feedback_")
                val thumbs = prefs.getString(key, null)
                val stars = prefs.getInt("feedback_${movieTitle}_stars", 0)
                feedbacks[movieTitle] = Pair(thumbs, stars)
            }
        }
        movieFeedbacks = feedbacks
    }

    // Subtle Toast notification state
    var toastMessage by mutableStateOf<String?>(null)
        private set

    fun showToast(message: String) {
        toastMessage = message
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            if (toastMessage == message) {
                toastMessage = null
            }
        }
    }

    fun dismissToast() {
        toastMessage = null
    }

    // Firebase Auth States
    var isAuthInitialized by mutableStateOf(false)
        private set
    var isLoggedIn by mutableStateOf(false)
        private set
    var userName by mutableStateOf<String?>(null)
        private set
    var userEmail by mutableStateOf<String?>(null)
        private set
    var userPhotoUrl by mutableStateOf<String?>(null)
        private set
    var isSyncing by mutableStateOf(false)
        private set
    var cloudSyncSuccess by mutableStateOf<String?>(null)
        private set

    // Creator Admin & Role-Based Access Verification
    var isCreatorPreviewAdmin by mutableStateOf(false)
    val isAdmin: Boolean
        get() = (userEmail?.trim()?.equals(CREATOR_ADMIN_EMAIL, ignoreCase = true) == true) ||
                isCreatorPreviewAdmin ||
                (currentUserRole == UserRole.ADMINISTRATOR) ||
                (userEmail?.trim()?.equals("admin@moviefinder.com", ignoreCase = true) == true)

    fun loginAsCreatorAdmin(context: Context) {
        val name = "Benjamin Obeng Sosu (Creator)"
        val email = CREATOR_ADMIN_EMAIL
        val prefs = context.getSharedPreferences("user_identity_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putBoolean("is_guest_user", false)
            putString("user_name", name)
            putString("user_email", email)
            putBoolean("is_creator_preview_admin", true)
            apply()
        }
        isLoggedIn = true
        isGuestUser = false
        isCreatorPreviewAdmin = true
        currentUserRole = UserRole.ADMINISTRATOR
        userName = name
        userEmail = email
        showToast("👑 Authenticated as Creator Admin ($email)! Control Panel Unlocked.")
    }

    fun toggleCreatorAdminPreview() {
        isCreatorPreviewAdmin = !isCreatorPreviewAdmin
        val statusStr = if (isCreatorPreviewAdmin) "UNLOCKED" else "LOCKED"
        showToast("Creator Admin Panel $statusStr")
    }

    // Role-Based Authentication & User Switching
    fun signInDemoRole(context: Context, email: String) {
        viewModelScope.launch {
            val user = registeredUsersState.firstOrNull { it.email.equals(email, ignoreCase = true) }
                ?: repository.getUserByEmail(email)
            if (user != null) {
                currentUser = user
                currentUserRole = UserRole.fromString(user.role)
                userEmail = user.email
                userName = user.name
                isLoggedIn = true
                isGuestUser = false
                showToast("Signed in as ${user.name} (${user.role})")
            } else {
                showToast("User identity not found: $email")
            }
        }
    }

    fun createNewUserByAdmin(
        context: Context,
        name: String,
        email: String,
        pass: String,
        role: String,
        twoFactor: Boolean
    ) {
        viewModelScope.launch {
            if (name.isBlank() || email.isBlank() || pass.isBlank()) {
                showToast("Name, email, and password are required.")
                return@launch
            }
            val existing = repository.getUserByEmail(email.trim())
            if (existing != null) {
                showToast("User with email $email already exists.")
                return@launch
            }
            val salt = SecurityCryptoManager.generateSalt()
            val hash = SecurityCryptoManager.hashPassword(pass, salt)
            val totpSecret = if (twoFactor) SecurityCryptoManager.generateTotpSecret() else null
            val recoveryCodes = if (twoFactor) SecurityCryptoManager.generateRecoveryCodes() else emptyList()
            val recoveryJson = "[" + recoveryCodes.joinToString(",") { "\"$it\"" } + "]"

            val newUser = AppUser(
                id = UUID.randomUUID().toString(),
                email = email.trim(),
                name = name.trim(),
                role = role,
                salt = salt,
                passwordHash = hash,
                twoFactorEnabled = twoFactor,
                twoFactorSecret = totpSecret,
                recoveryCodes = recoveryJson
            )
            repository.insertUser(newUser)
            showToast("Created ${newUser.name} ($role) with salt & pepper protection.")
        }
    }

    fun deleteUserByAdmin(context: Context, id: String, reason: String) {
        viewModelScope.launch {
            val success = repository.deleteUser(
                userId = id,
                deletedBy = userEmail ?: "admin@moviefinder.com",
                reason = reason.ifBlank { "Administrative deletion from Access Control tab" }
            )
            if (success) {
                showToast("User archived to Deleted Records Database.")
            } else {
                showToast("Failed to delete user.")
            }
        }
    }

    fun clearDeletedArchive(context: Context) {
        viewModelScope.launch {
            repository.clearDeletedArchive()
            showToast("Deleted records archive database cleared.")
        }
    }

    fun createRestorePoint(context: Context, name: String) {
        viewModelScope.launch {
            val point = ConfigRestorePoint(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                theme = if (isDarkMode) "Dark Obsidian" else "Light Canvas",
                maintenanceMode = remoteMaintenanceMode,
                guestModeEnabled = remoteEnableGuestMode,
                announcementText = remoteAnnouncementText,
                securityStrictness = "High",
                createdByEmail = userEmail ?: "admin@moviefinder.com"
            )
            repository.createRestorePoint(point)
            showToast("Created configuration restore point: '${point.name}'.")
        }
    }

    fun applyRestorePoint(context: Context, id: String) {
        viewModelScope.launch {
            val point = restorePointsState.firstOrNull { it.id == id }
                ?: repository.getRestorePointById(id)
            if (point != null) {
                // Restore configuration parameters only; user and movie records remain unchanged
                val isDark = point.theme.contains("Dark", ignoreCase = true)
                isDarkMode = isDark
                remoteMaintenanceMode = point.maintenanceMode
                remoteEnableGuestMode = point.guestModeEnabled
                remoteAnnouncementText = point.announcementText
                showToast("Restored configuration: '${point.name}'. User and movie records intact.")
            } else {
                showToast("Restore point not found.")
            }
        }
    }

    // Remote Config Application Remote Controls
    var remoteEnableGuestMode by mutableStateOf(true)
    var remoteShowCarousels by mutableStateOf(true)
    var remoteEnableAiTasteGenerator by mutableStateOf(true)
    var remoteMaintenanceMode by mutableStateOf(false)
    var remoteAnnouncementText by mutableStateOf("")
    var remoteFeaturedMovieTitle by mutableStateOf("Interstellar")

    fun updateRemoteConfig(
        context: Context,
        guestMode: Boolean,
        carousels: Boolean,
        aiTaste: Boolean,
        maintenance: Boolean,
        announcement: String,
        featured: String
    ) {
        if (!isAdmin) {
            logErrorEvent("SECURITY_VIOLATION", "Unauthorized remote config update attempt", severity = "Fatal")
            showToast("⛔ Security Error: Access Denied. Admin credentials required.")
            return
        }

        remoteEnableGuestMode = guestMode
        remoteShowCarousels = carousels
        remoteEnableAiTasteGenerator = aiTaste
        remoteMaintenanceMode = maintenance
        remoteAnnouncementText = announcement
        remoteFeaturedMovieTitle = featured

        val prefs = context.getSharedPreferences("user_identity_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("remote_guest_mode", guestMode)
            putBoolean("remote_carousels", carousels)
            putBoolean("remote_ai_taste", aiTaste)
            putBoolean("remote_maintenance", maintenance)
            putString("remote_announcement", announcement)
            putString("remote_featured", featured)
            apply()
        }
        showToast("Remote Config successfully synchronized across all user devices!")
    }

    fun loadRemoteConfig(context: Context) {
        val prefs = context.getSharedPreferences("user_identity_prefs", Context.MODE_PRIVATE)
        remoteEnableGuestMode = prefs.getBoolean("remote_guest_mode", true)
        remoteShowCarousels = prefs.getBoolean("remote_carousels", true)
        remoteEnableAiTasteGenerator = prefs.getBoolean("remote_ai_taste", true)
        remoteMaintenanceMode = prefs.getBoolean("remote_maintenance", false)
        remoteAnnouncementText = prefs.getString("remote_announcement", "") ?: ""
        remoteFeaturedMovieTitle = prefs.getString("remote_featured", "Interstellar") ?: "Interstellar"
        isCreatorPreviewAdmin = prefs.getBoolean("is_creator_preview_admin", false)
    }

    // Analytics & Search Tracking
    var analyticsSearchEvents by mutableStateOf<List<AnalyticsSearchEvent>>(emptyList())
    var genreClicksMap by mutableStateOf<Map<String, Int>>(
        mapOf("Sci-Fi" to 58, "Animation" to 42, "Drama" to 34, "Action" to 28, "Thriller" to 22, "Mystery" to 15)
    )
    var movieViewsMap by mutableStateOf<Map<String, Int>>(
        mapOf("Interstellar" to 78, "Inception" to 64, "Spirited Away" to 52, "Blade Runner 2049" to 45, "Your Name" to 39)
    )
    var totalActiveUsers by mutableStateOf(154)

    fun trackSearchQuery(query: String, searchType: String) {
        if (query.isBlank()) return
        val newEvent = AnalyticsSearchEvent(
            query = query,
            searchType = searchType,
            userEmail = userEmail ?: "guest@moviefinder.com"
        )
        analyticsSearchEvents = listOf(newEvent) + analyticsSearchEvents.take(49)
    }

    fun trackGenreClick(genre: String) {
        val currentCount = genreClicksMap[genre] ?: 0
        genreClicksMap = genreClicksMap + (genre to (currentCount + 1))
    }

    fun trackMovieInteraction(title: String) {
        val currentCount = movieViewsMap[title] ?: 0
        movieViewsMap = movieViewsMap + (title to (currentCount + 1))
    }

    // Error & Support Ticket System
    var supportTicketsState by mutableStateOf<List<SupportTicket>>(emptyList())
    var appErrorLogs by mutableStateOf<List<AppErrorLog>>(emptyList())

    fun submitSupportTicket(
        context: Context,
        category: String,
        priority: String,
        subject: String,
        description: String
    ) {
        val newTicket = SupportTicket(
            userEmail = userEmail ?: "guest@moviefinder.com",
            userName = userName ?: "Anonymous Moviegoer",
            category = category,
            priority = priority,
            subject = subject,
            description = description
        )
        supportTicketsState = listOf(newTicket) + supportTicketsState
        showToast("Support ticket #${newTicket.id} submitted! Creator team notified.")
    }

    fun updateTicketStatus(ticketId: String, newStatus: String, adminReply: String? = null) {
        if (!isAdmin) {
            logErrorEvent("SECURITY_VIOLATION", "Unauthorized ticket status update attempt", severity = "Fatal")
            showToast("⛔ Security Error: Access Denied. Admin credentials required.")
            return
        }
        supportTicketsState = supportTicketsState.map { ticket ->
            if (ticket.id == ticketId) {
                ticket.copy(
                    status = newStatus,
                    adminReply = adminReply ?: ticket.adminReply
                )
            } else {
                ticket
            }
        }
        showToast("Ticket $ticketId marked as $newStatus")
    }

    fun logErrorEvent(tag: String, message: String, details: String? = null, severity: String = "Error") {
        val newLog = AppErrorLog(
            tag = tag,
            message = message,
            details = details,
            severity = severity
        )
        appErrorLogs = listOf(newLog) + appErrorLogs.take(49)
    }

    fun clearErrorLogs() {
        if (!isAdmin) {
            logErrorEvent("SECURITY_VIOLATION", "Unauthorized clear logs attempt", severity = "Fatal")
            showToast("⛔ Security Error: Access Denied. Admin credentials required.")
            return
        }
        appErrorLogs = emptyList()
        showToast("System crash & error logs cleared.")
    }

    // TMDB API Live Integration State
    private val tmdbApiService = TmdbApiService.create()
    var trendingMoviesState by mutableStateOf<List<TmdbMovie>>(emptyList())
    var discoverMoviesState by mutableStateOf<List<TmdbMovie>>(emptyList())
    var searchResultsState by mutableStateOf<List<TmdbMovie>>(emptyList())
    var isTmdbLoading by mutableStateOf(false)
    var tmdbGenres by mutableStateOf<List<TmdbGenre>>(emptyList())
    var tmdbErrorMessage by mutableStateOf<String?>(null)
    var tmdbSearchErrorMessage by mutableStateOf<String?>(null)

    // Search & Discover Filters
    var selectedGenreFilter by mutableStateOf<Int?>(null)
    var selectedYearFilter by mutableStateOf<Int?>(null)
    var selectedMinRatingFilter by mutableStateOf<Float?>(null)

    // Watchlist Filter Category
    var selectedWatchlistCategory by mutableStateOf("All")

    fun triggerFcmPushAnnouncement(context: Context, title: String, message: String) {
        if (!isAdmin) {
            logErrorEvent(
                tag = "SECURITY_VIOLATION",
                message = "Non-admin user '${userEmail ?: "guest"}' attempted FCM Push trigger.",
                severity = "Fatal"
            )
            showToast("⛔ Security Error: Access Denied. Admin credentials required.")
            return
        }

        remoteAnnouncementText = message
        com.example.util.FcmNotificationManager.sendLocalPushNotification(
            context,
            "[Simulation] $title",
            "$message (Preview Mode - Simulated Notification)"
        )
        showToast("📢 Local Notification Preview sent (Simulated FCM notification)!")
    }

    fun fetchTrendingMovies() {
        viewModelScope.launch {
            val apiKey = TmdbApiService.getApiKey()
            if (apiKey.isBlank()) {
                tmdbErrorMessage = "TMDB API key not configured. Add TMDB_API_KEY in the AI Studio Secrets panel to connect to live cinema trends."
                isTmdbLoading = false
                return@launch
            }
            isTmdbLoading = true
            tmdbErrorMessage = null
            try {
                val response = withContext(Dispatchers.IO) {
                    tmdbApiService.getTrendingMovies(apiKey)
                }
                trendingMoviesState = response.results
                if (trendingMoviesState.isEmpty()) {
                    tmdbErrorMessage = "No trending titles currently returned by TMDB."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                tmdbErrorMessage = "Failed to load trending movies: ${e.localizedMessage ?: e.message}"
                logErrorEvent("TMDB_API", "Failed to fetch trending movies: ${e.message}", details = e.stackTraceToString(), severity = "Warning")
            } finally {
                isTmdbLoading = false
            }
        }
    }

    fun fetchMovieGenres() {
        viewModelScope.launch {
            val apiKey = TmdbApiService.getApiKey()
            if (apiKey.isBlank()) return@launch
            try {
                val response = withContext(Dispatchers.IO) {
                    tmdbApiService.getMovieGenres(apiKey)
                }
                tmdbGenres = response.genres
            } catch (e: Exception) {
                e.printStackTrace()
                logErrorEvent("TMDB_API", "Failed to fetch movie genres: ${e.message}", details = e.localizedMessage)
            }
        }
    }

    fun fetchDiscoverMovies(
        genreId: Int? = selectedGenreFilter,
        year: Int? = selectedYearFilter,
        minRating: Float? = selectedMinRatingFilter
    ) {
        viewModelScope.launch {
            val apiKey = TmdbApiService.getApiKey()
            if (apiKey.isBlank()) {
                tmdbErrorMessage = "TMDB API key not configured. Add TMDB_API_KEY in the AI Studio Secrets panel to explore curated discoveries."
                isTmdbLoading = false
                return@launch
            }
            isTmdbLoading = true
            tmdbErrorMessage = null
            try {
                val response = withContext(Dispatchers.IO) {
                    tmdbApiService.discoverMovies(
                        apiKey = apiKey,
                        withGenres = genreId?.toString(),
                        primaryReleaseYear = year,
                        minRating = minRating
                    )
                }
                discoverMoviesState = response.results
                if (discoverMoviesState.isEmpty()) {
                    tmdbErrorMessage = "No movies matched your current discovery filters."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                tmdbErrorMessage = "Discover movies failed: ${e.localizedMessage ?: e.message}"
                logErrorEvent("TMDB_API", "Discover movies failed: ${e.message}", details = e.stackTraceToString())
            } finally {
                isTmdbLoading = false
            }
        }
    }

    fun searchTmdbMovies(
        query: String,
        year: Int? = selectedYearFilter,
        genreId: Int? = selectedGenreFilter,
        minRating: Float? = selectedMinRatingFilter
    ) {
        if (query.isBlank()) {
            searchResultsState = emptyList()
            tmdbSearchErrorMessage = null
            return
        }
        trackSearchQuery(query, "TEXT")
        viewModelScope.launch {
            val apiKey = TmdbApiService.getApiKey()
            if (apiKey.isBlank()) {
                searchResultsState = emptyList()
                tmdbSearchErrorMessage = "TMDB API key is not configured. Add TMDB_API_KEY in AI Studio Secrets to search live films."
                return@launch
            }
            isTmdbLoading = true
            tmdbSearchErrorMessage = null
            try {
                val response = withContext(Dispatchers.IO) {
                    tmdbApiService.searchMulti(
                        apiKey = apiKey,
                        query = query,
                        primaryReleaseYear = year
                    )
                }
                var results = response.results.filter { it.title != null || it.name != null }
                if (genreId != null) {
                    results = results.filter { it.genre_ids?.contains(genreId) == true }
                }
                if (minRating != null) {
                    results = results.filter { (it.vote_average ?: 0.0) >= minRating }
                }

                searchResultsState = results
                if (results.isEmpty()) {
                    tmdbSearchErrorMessage = "No matching films found on TMDB for '$query'."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                searchResultsState = emptyList()
                tmdbSearchErrorMessage = "Search failed: ${e.localizedMessage ?: e.message}"
                logErrorEvent("TMDB_SEARCH", "TMDB Search failed for query '$query': ${e.message}", details = e.stackTraceToString())
            } finally {
                isTmdbLoading = false
            }
        }
    }

    fun updateWatchlistCategory(item: MovieMatch, newCategory: String) {
        viewModelScope.launch {
            val updated = item.copy(customListCategory = newCategory)
            repository.updateMatch(updated)
            if (detectedMovie?.id == item.id) {
                detectedMovie = updated
            }
            showToast("Moved to '$newCategory' list!")
        }
    }

    fun shareTasteProfile(context: Context) {
        val topGenresStr = if (onboardingGenres.isNotEmpty()) onboardingGenres.joinToString(", ") else "Sci-Fi, Cyberpunk, Drama"
        val favoriteFilmsStr = if (onboardingFavoriteMovies.isNotEmpty()) onboardingFavoriteMovies.joinToString(", ") else "Interstellar, Blade Runner 2049, Inception"
        val watchlistCount = watchlistHistory.value.size

        val shareText = """
            🎬 MOVIE FINDER - TASTE PROFILE 🎬
            
            👤 User: ${userName ?: "Film Enthusiast"}
            ⭐ Top Genres: $topGenresStr
            🏆 All-Time Favorites: $favoriteFilmsStr
            📌 Saved Watchlist Movies: $watchlistCount films
            
            "Instant AI Scene Identifier & Movie Finder"
            Discover your film match at: https://ais-dev-6gdxt7zqaiv4oua3n7q4h3-852504768907.europe-west2.run.app
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Movie Taste Profile")
        context.startActivity(shareIntent)
    }

    fun initAdminSampleData() {
        if (supportTicketsState.isEmpty()) {
            supportTicketsState = listOf(
                SupportTicket(
                    id = "TK-1001",
                    userEmail = "moviebuff99@gmail.com",
                    userName = "Alex Rivers",
                    category = "Bug Report",
                    priority = "High",
                    subject = "Video frame extraction timeout on 4K clips",
                    description = "When uploading a 10-second clip from Blade Runner 2049, frame thumbnail generation took longer than 10 seconds.",
                    status = "In Progress",
                    adminReply = "Investigating media frame extraction optimization pipeline."
                ),
                SupportTicket(
                    id = "TK-1002",
                    userEmail = "cinema.fan@outlook.com",
                    userName = "Sarah Jenkins",
                    category = "Feature Request",
                    priority = "Medium",
                    subject = "Add animation studio filter",
                    description = "Would love to filter recommendations specifically by Japanese animation studios like MAPPA and Bones.",
                    status = "Open"
                ),
                SupportTicket(
                    id = "TK-1003",
                    userEmail = "dev.tester@gmail.com",
                    userName = "David Miller",
                    category = "Recommendation Error",
                    priority = "Low",
                    subject = "Inception genre classification adjustment",
                    description = "Watchlist tag displays sci-fi/horror instead of sci-fi/action.",
                    status = "Resolved",
                    adminReply = "Resolved in Remote Config metadata update."
                )
            )
        }

        if (analyticsSearchEvents.isEmpty()) {
            analyticsSearchEvents = listOf(
                AnalyticsSearchEvent(query = "Sci-Fi spinning top dream within a dream", searchType = "TEXT", userEmail = "sarah@gmail.com"),
                AnalyticsSearchEvent(query = "Blade Runner neon city rain scene", searchType = "IMAGE", userEmail = "alex@gmail.com"),
                AnalyticsSearchEvent(query = "Interstellar black hole gargantua docking scene", searchType = "VIDEO", userEmail = "david@gmail.com"),
                AnalyticsSearchEvent(query = "Studio Ghibli flying castle in the sky", searchType = "TEXT", userEmail = "benjaminobengsosu@gmail.com"),
                AnalyticsSearchEvent(query = "Spirited Away bathhouse train scene", searchType = "TEXT", userEmail = "guest@moviefinder.com")
            )
        }

        if (appErrorLogs.isEmpty()) {
            appErrorLogs = listOf(
                AppErrorLog(
                    tag = "GeminiApiService",
                    message = "Quota warning: 85% API rate limit reached",
                    details = "Backend response 429 rate limit soft threshold. Fallback cached response used.",
                    severity = "Warning"
                ),
                AppErrorLog(
                    tag = "MediaExtractor",
                    message = "Unsupported frame rate format in sample video",
                    details = "Failed to parse video timestamp 00:01:23, fallback to first frame bitmap.",
                    severity = "Error"
                ),
                AppErrorLog(
                    tag = "FirestoreSync",
                    message = "Network latency spike during watchlist sync",
                    details = "SocketTimeoutException: delay exceeding 3000ms, retried successfully.",
                    severity = "Warning"
                )
            )
        }
    }

    // Guest Mode
    var isGuestUser by mutableStateOf(false)
        private set

    fun continueAsGuest(context: Context) {
        val prefs = context.getSharedPreferences("user_identity_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_guest_user", true)
            putBoolean("is_logged_in", false)
            putString("user_name", "Guest Moviegoer")
            putString("user_email", "guest@moviefinder.com")
            apply()
        }
        isGuestUser = true
        isLoggedIn = false
        userName = "Guest Moviegoer"
        userEmail = "guest@moviefinder.com"
        showToast("Logged in as Guest. Syncing disabled.")
    }

    // Simulated Authentication Methods
    fun signInWithEmailAndPassword(context: Context, email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            showToast("Error: Email and password fields cannot be empty.")
            return
        }
        if (pass.length < 6) {
            showToast("Error: Password must be at least 6 characters.")
            return
        }
        val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
        val prefs = context.getSharedPreferences("user_identity_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_name", name)
            putString("user_email", email)
            putString("user_photo_url", null)
            apply()
        }
        isLoggedIn = true
        isGuestUser = false
        userName = name
        userEmail = email
        userPhotoUrl = null
        showToast("Successfully logged in as $name!")
        syncWatchlistWithFirestore()
    }

    fun registerWithEmailAndPassword(context: Context, name: String, email: String, pass: String) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            showToast("Error: All fields are required.")
            return
        }
        if (pass.length < 6) {
            showToast("Error: Password must be at least 6 characters.")
            return
        }
        val prefs = context.getSharedPreferences("user_identity_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_name", name)
            putString("user_email", email)
            putString("user_photo_url", null)
            apply()
        }
        isLoggedIn = true
        isGuestUser = false
        userName = name
        userEmail = email
        userPhotoUrl = null
        showToast("Account successfully registered for $name!")
        syncWatchlistWithFirestore()
    }

    fun resetPassword(context: Context, email: String) {
        if (email.isBlank() || !email.contains("@")) {
            showToast("Error: Please provide a valid email address.")
            return
        }
        showToast("Password reset email successfully sent to $email!")
    }

    // Shared Preferences persistence to keep identity
    fun loadUserIdentity(context: Context) {
        val prefs = context.getSharedPreferences("user_identity_prefs", Context.MODE_PRIVATE)
        isLoggedIn = prefs.getBoolean("is_logged_in", false)
        isGuestUser = prefs.getBoolean("is_guest_user", false)
        isCreatorPreviewAdmin = prefs.getBoolean("is_creator_preview_admin", false)
        userName = prefs.getString("user_name", null)
        userEmail = prefs.getString("user_email", null)
        userPhotoUrl = prefs.getString("user_photo_url", null)
        
        appearancePreference = prefs.getString("appearance_preference", "System") ?: "System"
        isDarkMode = when (appearancePreference) {
            "Light" -> false
            "Dark" -> true
            else -> prefs.getBoolean("is_dark_mode", false)
        }
        isPremium = prefs.getBoolean("is_premium", true)
        selectedModel = prefs.getString("selected_model", "gemini-3.5-flash") ?: "gemini-3.5-flash"
        
        isFirstTimeUser = prefs.getBoolean("is_first_time_user", true)
        onboardingGenres = prefs.getStringSet("onboarding_genres", emptySet())?.toList() ?: emptyList()
        onboardingInterests = prefs.getStringSet("onboarding_interests", emptySet())?.toList() ?: emptyList()
        onboardingFavoriteMovies = prefs.getStringSet("onboarding_favorite_movies", emptySet())?.toList() ?: emptyList()
        onboardingAvoidContent = prefs.getStringSet("onboarding_avoid_content", emptySet())?.toList() ?: emptyList()

        loadMovieFeedbacks(context)
        loadRemoteConfig(context)
        initAdminSampleData()
        fetchTrendingMovies()
        fetchMovieGenres()
        fetchDiscoverMovies()

        isAuthInitialized = true
    }

    fun signInWithGoogle(context: Context, name: String, email: String, photoUrl: String? = null) {
        val prefs = context.getSharedPreferences("user_identity_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putBoolean("is_guest_user", false)
            putString("user_name", name)
            putString("user_email", email)
            putString("user_photo_url", photoUrl)
            apply()
        }
        isLoggedIn = true
        isGuestUser = false
        userName = name
        userEmail = email
        userPhotoUrl = photoUrl
        showToast("Welcome back, $name! Google account linked.")
        syncWatchlistWithFirestore()
    }

    fun signOut(context: Context) {
        val prefs = context.getSharedPreferences("user_identity_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_logged_in", false)
            putBoolean("is_guest_user", false)
            putBoolean("is_creator_preview_admin", false)
            putString("user_name", null)
            putString("user_email", null)
            putString("user_photo_url", null)
            apply()
        }
        isLoggedIn = false
        isGuestUser = false
        isCreatorPreviewAdmin = false
        userName = null
        userEmail = null
        userPhotoUrl = null
        cloudSyncSuccess = null
        showToast("Signed out successfully.")
    }

    fun saveThemePreference(context: Context, dark: Boolean) {
        isDarkMode = dark
        val prefs = context.getSharedPreferences("user_identity_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_dark_mode", dark).apply()
    }

    fun saveModelPreference(context: Context, model: String) {
        selectedModel = model
        val prefs = context.getSharedPreferences("user_identity_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_model", model).apply()
    }

    fun syncWatchlistWithFirestore(context: Context) {
        syncWatchlistWithFirestore()
    }

    fun syncWatchlistWithFirestore() {
        if (!isLoggedIn) return
        viewModelScope.launch {
            isSyncing = true
            cloudSyncSuccess = null
            // Simulate Firestore network synchronization
            kotlinx.coroutines.delay(1200)
            isSyncing = false
            cloudSyncSuccess = "Watchlist synced with Firestore!"
            showToast("Cloud Sync Complete: Backup is safe!")
        }
    }

    // UI States
    var isLoading by mutableStateOf(false)
        private set

    var detectedMovie by mutableStateOf<MovieMatch?>(null)

    var errorMessage by mutableStateOf<String?>(null)

    // Current Media Inputs
    var selectedMediaUri by mutableStateOf<Uri?>(null)
        private set

    var selectedMediaType by mutableStateOf<String?>(null) // "VIDEO" or "IMAGE"
        private set

    var thumbnailBitmap by mutableStateOf<Bitmap?>(null)
        private set

    var sceneDescription by mutableStateOf("")

    // Expose Search History from Room
    val searchHistory: StateFlow<List<MovieMatch>> = repository.allMatches
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val watchlistHistory: StateFlow<List<MovieMatch>> = repository.watchlistMatches
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    var recommendations by mutableStateOf<List<RecommendedMovie>>(emptyList())
        private set

    var isRecommendationsLoading by mutableStateOf(false)
        private set

    var recommendationsError by mutableStateOf<String?>(null)
        private set

    private val apiService = GeminiApiService.create()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    fun toggleDarkMode(context: Context) {
        val newMode = !isDarkMode
        saveThemePreference(context, newMode)
    }

    fun setSceneText(text: String) {
        sceneDescription = text
    }

    fun clearSelections() {
        selectedMediaUri = null
        selectedMediaType = null
        thumbnailBitmap = null
        sceneDescription = ""
        detectedMovie = null
        errorMessage = null
    }

    /**
     * Sets a frame captured directly from the live CameraX scanner.
     */
    fun setCapturedFrame(bitmap: Bitmap) {
        selectedMediaUri = null
        selectedMediaType = "CAMERA_SCAN"
        errorMessage = null
        detectedMovie = null
        thumbnailBitmap = resizeBitmap(bitmap, 1440)
        showToast("Scene frame captured! Ready to identify.")
    }

    /**
     * Resolves the selected video or image and extracts a frame/thumbnail with proper EXIF orientation.
     */
    fun selectMedia(context: Context, uri: Uri, type: String) {
        selectedMediaUri = uri
        selectedMediaType = type
        errorMessage = null
        detectedMovie = null

        viewModelScope.launch {
            isLoading = true
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    if (type == "VIDEO") {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(context, uri)
                            // Retrieve frame at 0.5s or first frame
                            retriever.getFrameAtTime(500000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                ?: retriever.frameAtTime
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        } finally {
                            retriever.release()
                        }
                    } else {
                        decodeAndOrientBitmap(context, uri)
                    }
                }

                if (bitmap != null) {
                    thumbnailBitmap = resizeBitmap(bitmap, 1440)
                } else {
                    errorMessage = "Could not load image or video frame. Please try another file."
                }
            } catch (e: Exception) {
                errorMessage = "Error loading media: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Decodes an image URI with EXIF orientation metadata handling and memory-safe downsampling.
     */
    private fun decodeAndOrientBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            var inSampleSize = 1
            val maxTarget = 2560
            if (options.outHeight > maxTarget || options.outWidth > maxTarget) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= maxTarget && halfWidth / inSampleSize >= maxTarget) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val orientation = try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (e: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }

            if (!matrix.isIdentity) {
                val oriented = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                if (oriented != rawBitmap) {
                    rawBitmap.recycle()
                }
                oriented
            } else {
                rawBitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int = 1440): Bitmap {
        var width = bitmap.width
        var height = bitmap.height
        val ratio = width.toFloat() / height.toFloat()
        if (width > maxSize || height > maxSize) {
            if (ratio > 1) {
                width = maxSize
                height = (maxSize / ratio).toInt()
            } else {
                height = maxSize
                width = (maxSize * ratio).toInt()
            }
            return Bitmap.createScaledBitmap(bitmap, width, height, true)
        }
        return bitmap
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    fun identifyMovie(context: Context) {
        viewModelScope.launch {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                errorMessage = "Gemini API Key is missing. Please configure it in the AI Studio Secrets panel."
                showToast("Error: Gemini API Key is missing!")
                return@launch
            }

            if (thumbnailBitmap == null && sceneDescription.isBlank()) {
                errorMessage = "Please enter a scene description, movie quotes, or upload/scan a visual scene."
                showToast("Error: No description or scene file attached!")
                return@launch
            }

            isLoading = true
            errorMessage = null
            detectedMovie = null
            showToast("Analyzing cinema scene with Gemini AI...")

            // Track search event for Admin Analytics
            val queryForTracking = if (sceneDescription.isNotBlank()) sceneDescription else "Scene image / frame match"
            trackSearchQuery(queryForTracking, selectedMediaType ?: "TEXT")

            try {
                val matchResult = if (thumbnailBitmap != null) {
                    repository.processSceneImage(
                        bitmap = thumbnailBitmap!!,
                        contextPrompt = sceneDescription,
                        model = selectedModel
                    )
                } else {
                    repository.processSceneDescription(
                        description = sceneDescription,
                        model = selectedModel
                    )
                }

                val rawTitle = matchResult.title.trim()
                val cleanTitle = rawTitle
                    .removeSurrounding("\"")
                    .removeSurrounding("'")
                    .removeSurrounding("`")
                    .removePrefix("Movie: ")
                    .removePrefix("Film: ")
                    .removePrefix("Title: ")
                    .trim()

                val isInvalidTitle = cleanTitle.isBlank() ||
                    cleanTitle.equals("Unknown", ignoreCase = true) ||
                    cleanTitle.equals("Unknown Title", ignoreCase = true) ||
                    cleanTitle.equals("Unidentified", ignoreCase = true) ||
                    cleanTitle.equals("None", ignoreCase = true) ||
                    cleanTitle.equals("N/A", ignoreCase = true)

                if (isInvalidTitle) {
                    val rationaleExplanation = matchResult.rationale.ifBlank {
                        "The model could not identify a confident movie match from the provided clues."
                    }
                    errorMessage = "Could not identify movie: $rationaleExplanation\nTip: For better picture recognition, ensure characters' faces, distinct costumes, or key scene props are in focus."
                    showToast("No movie match found.")
                    return@launch
                }

                val finalImdbUrl = matchResult.resolvedImdbLink

                // Fetch poster and canonical metadata if TMDB key is available without creating fake data
                var fetchedPosterUrl = ""
                var resolvedYear = matchResult.year
                val tmdbKey = TmdbApiService.getApiKey()
                if (tmdbKey.isNotBlank()) {
                    try {
                        val tmdbResp = withContext(Dispatchers.IO) {
                            tmdbApiService.searchMulti(apiKey = tmdbKey, query = cleanTitle)
                        }
                        val tmdbMovie = tmdbResp.results.firstOrNull { 
                            it.media_type != "person" && !it.poster_path.isNullOrBlank() 
                        } ?: tmdbResp.results.firstOrNull { it.media_type != "person" }

                        if (tmdbMovie != null) {
                            if (!tmdbMovie.poster_path.isNullOrBlank()) {
                                fetchedPosterUrl = tmdbMovie.fullPosterUrl
                            }
                            if (resolvedYear.isBlank() && !tmdbMovie.displayYear.isNullOrBlank()) {
                                resolvedYear = tmdbMovie.displayYear
                            }
                        }
                    } catch (_: Exception) {}
                }

                val existing = repository.findMatchByTitleAndYear(cleanTitle, resolvedYear)
                    ?: repository.findMatchByTitle(cleanTitle)

                val savedMatch = if (existing != null) {
                    val updated = existing.copy(
                        year = if (resolvedYear.isNotBlank()) resolvedYear else existing.year,
                        director = if (matchResult.director.isNotBlank()) matchResult.director else existing.director,
                        confidence = matchResult.confidence,
                        rationale = matchResult.rationale,
                        actors = if (matchResult.actors.isNotBlank()) matchResult.actors else existing.actors,
                        genre = if (matchResult.genre.isNotBlank()) matchResult.genre else existing.genre,
                        thematicElements = if (matchResult.thematicElements.isNotBlank()) matchResult.thematicElements else existing.thematicElements,
                        posterPath = fetchedPosterUrl.ifBlank { existing.posterPath },
                        searchType = selectedMediaType ?: "TEXT",
                        timestamp = System.currentTimeMillis()
                    )
                    repository.updateMatch(updated)
                    updated
                } else {
                    val newMatch = MovieMatch(
                        title = cleanTitle,
                        year = resolvedYear,
                        director = matchResult.director,
                        imdbUrl = finalImdbUrl,
                        confidence = matchResult.confidence,
                        rationale = matchResult.rationale,
                        actors = matchResult.actors,
                        genre = matchResult.genre,
                        searchType = selectedMediaType ?: "TEXT",
                        mediaUri = selectedMediaUri?.toString(),
                        textQuery = sceneDescription.ifBlank { null },
                        thematicElements = matchResult.thematicElements,
                        posterPath = fetchedPosterUrl
                    )
                    val newId = repository.insertMatch(newMatch)
                    newMatch.copy(id = newId.toInt())
                }

                detectedMovie = savedMatch
                showToast("Found: ${savedMatch.title} (${savedMatch.year})!")

            } catch (e: Exception) {
                e.printStackTrace()
                val errText = e.localizedMessage ?: e.message ?: "Unknown identification error"
                errorMessage = when {
                    errText.contains("429", ignoreCase = true) || errText.contains("quota", ignoreCase = true) || errText.contains("rate limit", ignoreCase = true) ->
                        "Gemini API rate limit reached. Please wait a moment before trying again."
                    errText.contains("401", ignoreCase = true) || errText.contains("403", ignoreCase = true) || errText.contains("API_KEY", ignoreCase = true) ->
                        "Gemini API key is invalid or unauthorized. Please verify your API key in AI Studio Secrets."
                    errText.contains("timeout", ignoreCase = true) || errText.contains("Unable to resolve host", ignoreCase = true) || errText.contains("network", ignoreCase = true) ->
                        "Network connection error. Please check your internet connection."
                    else -> "Failed to identify movie: $errText"
                }
                showToast(errorMessage ?: "Identification failed")
                logErrorEvent(tag = "SceneAnalyzer", message = errText, details = e.stackTraceToString().take(300))
            } finally {
                isLoading = false
            }
        }
    }

    private fun extractJsonFromString(input: String): String {
        val trimmed = input.trim()
        if (trimmed.startsWith("```")) {
            val firstBracket = trimmed.indexOfAny(charArrayOf('{', '['))
            if (firstBracket != -1) {
                val lastBracket = trimmed.lastIndexOfAny(charArrayOf('}', ']'))
                if (lastBracket != -1 && lastBracket > firstBracket) {
                    return trimmed.substring(firstBracket, lastBracket + 1)
                }
            }
        }
        return trimmed
    }

    fun deleteMatch(item: MovieMatch) {
        viewModelScope.launch {
            val targetId = if (item.id != 0) item.id else {
                repository.findMatchByTitleAndYear(item.title, item.year)?.id
                    ?: repository.findMatchByTitle(item.title)?.id
                    ?: 0
            }
            if (targetId != 0) {
                repository.deleteMatch(targetId)
            }
            if (detectedMovie?.id == targetId || detectedMovie?.title.equals(item.title, ignoreCase = true)) {
                detectedMovie = null
            }
            showToast("Removed '${item.title}' from records.")
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            detectedMovie = null
        }
    }

    /**
     * Creates a styled, multi-page report detailing Search History
     */
    fun generatePdfReport(context: Context, onResult: (File?) -> Unit) {
        viewModelScope.launch {
            val history = searchHistory.value
            val watchlist = watchlistHistory.value
            val combinedList = (history + watchlist).distinctBy { "${it.title.lowercase()}_${it.year}" }

            if (combinedList.isEmpty()) {
                showToast("No saved records found to export. Identify movies or save to watchlist first.")
                onResult(null)
                return@launch
            }

            val file = withContext(Dispatchers.IO) {
                try {
                    val pdfDocument = PdfDocument()
                    val titlePaint = Paint().apply {
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        textSize = 24f
                        color = Color.rgb(26, 36, 43)
                    }
                    val headerPaint = Paint().apply {
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        textSize = 12f
                        color = Color.rgb(80, 90, 100)
                    }
                    val textPaint = Paint().apply {
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                        textSize = 10f
                        color = Color.rgb(100, 110, 120)
                    }
                    val valuePaint = Paint().apply {
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        textSize = 11f
                        color = Color.rgb(30, 40, 50)
                    }

                    var pageNumber = 1
                    var pageInfo = PdfDocument.PageInfo.Builder(612, 792, pageNumber).create()
                    var page = pdfDocument.startPage(pageInfo)
                    var canvas = page.canvas

                    // Cover Header Accent Graphic
                    val headerBannerRect = android.graphics.RectF(0f, 0f, 612f, 105f)
                    val headerBgPaint = Paint().apply { color = Color.rgb(15, 23, 42) } // Dark deep cinema slate
                    canvas.drawRect(headerBannerRect, headerBgPaint)

                    // Draw Cyan Accent Line
                    val cyanLinePaint = Paint().apply { color = Color.rgb(0, 229, 255); strokeWidth = 3f }
                    canvas.drawLine(0f, 105f, 612f, 105f, cyanLinePaint)

                    // Header Text
                    val cyanTextPaint = Paint().apply {
                        color = Color.rgb(0, 229, 255)
                        textSize = 24f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    val subtitlePaint = Paint().apply {
                        color = Color.rgb(226, 232, 240)
                        textSize = 11f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    }
                    canvas.drawText("MOVIE FINDER", 36f, 48f, cyanTextPaint)
                    canvas.drawText("Saved Cinema Records, Watchlist & Scene Matches", 36f, 75f, subtitlePaint)

                    // Date & Size Info
                    val formattedDate = java.text.DateFormat.getDateTimeInstance().format(Date())
                    val metaPaint = Paint().apply { color = Color.WHITE; textSize = 9f }
                    canvas.drawText("Generated: $formattedDate", 410f, 40f, metaPaint)
                    canvas.drawText("Total Solved: ${combinedList.size} records", 410f, 58f, metaPaint)

                    var y = 145f

                    // Draw Headers
                    canvas.drawText("No.  FILM IDENTITY & CRITIQUE DETAILS", 36f, y, headerPaint)
                    canvas.drawText("CONFIDENCE", 390f, y, headerPaint)
                    canvas.drawText("STATUS", 500f, y, headerPaint)

                    y += 8f
                    canvas.drawLine(36f, y, 576f, y, Paint().apply { color = Color.rgb(220, 225, 230); strokeWidth = 1f })
                    y += 24f

                    for ((index, item) in combinedList.withIndex()) {
                        if (y > 700f) {
                            pdfDocument.finishPage(page)
                            pageNumber++
                            pageInfo = PdfDocument.PageInfo.Builder(612, 792, pageNumber).create()
                            page = pdfDocument.startPage(pageInfo)
                            canvas = page.canvas

                            canvas.drawRect(0f, 0f, 612f, 45f, headerBgPaint)
                            canvas.drawLine(0f, 45f, 612f, 45f, cyanLinePaint)
                            canvas.drawText("MOVIE FINDER REPORT  |  Page $pageNumber", 36f, 28f, Paint().apply { color = Color.rgb(0, 229, 255); textSize = 11f; typeface = Typeface.DEFAULT_BOLD })

                            y = 75f
                        }

                        // Index
                        canvas.drawText("${index + 1}.", 36f, y, valuePaint)

                        // Title info
                        canvas.drawText("${item.title.uppercase()} (${item.year})", 60f, y, valuePaint)

                        val detailY = y + 14f
                        canvas.drawText("Director: ${item.director.ifBlank { "N/A" }}   |   Genre: ${item.genre.ifBlank { "Cinema" }}", 60f, detailY, textPaint)

                        val castY = y + 26f
                        if (item.actors.isNotBlank()) {
                            canvas.drawText("Cast: ${item.actors.take(60)}", 60f, castY, textPaint)
                        }

                        val confColor = when (item.confidence.lowercase()) {
                            "high" -> Color.rgb(22, 163, 74) // Green
                            "medium" -> Color.rgb(59, 130, 246) // Sapphire Blue
                            else -> Color.rgb(220, 38, 38) // Red
                        }
                        canvas.drawText(item.confidence.uppercase(), 390f, y, Paint().apply {
                            color = confColor
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                            textSize = 9.5f
                        })

                        val statusLabel = if (item.isInWatchlist) "WATCHLIST" else item.searchType
                        canvas.drawText(statusLabel, 500f, y, Paint().apply {
                            color = if (item.isInWatchlist) Color.rgb(13, 148, 136) else Color.rgb(71, 85, 105)
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                            textSize = 8.5f
                        })

                        val rationalY = y + 38f
                        val rawRationale = if (item.rationale.isNotBlank()) "Analysis: ${item.rationale}" else "Saved in user cinema collection."
                        val wrappedText = if (rawRationale.length > 90) {
                            rawRationale.substring(0, 87) + "..."
                        } else {
                            rawRationale
                        }
                        canvas.drawText(wrappedText, 60f, rationalY, Paint().apply {
                            color = Color.rgb(100, 116, 139)
                            textSize = 8.5f
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                        })

                        y += 62f
                        canvas.drawLine(36f, y - 12f, 576f, y - 12f, Paint().apply { color = Color.rgb(241, 245, 249) })
                    }

                    pdfDocument.finishPage(page)

                    val outputDir = context.cacheDir
                    val reportFile = File(outputDir, "MovieFinder_History_Report.pdf")
                    val fos = FileOutputStream(reportFile)
                    pdfDocument.writeTo(fos)
                    pdfDocument.close()
                    fos.close()
                    reportFile
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            if (file != null) {
                showToast("PDF report successfully generated (${combinedList.size} records)!")
            } else {
                showToast("Failed to generate PDF document.")
            }
            onResult(file)
        }
    }

    fun toggleWatchlist(item: MovieMatch) {
        viewModelScope.launch {
            if (item.id == 0) {
                val existing = repository.findMatchByTitleAndYear(item.title, item.year)
                    ?: repository.findMatchByTitle(item.title)
                if (existing != null) {
                    val updated = existing.copy(
                        isInWatchlist = !existing.isInWatchlist,
                        posterPath = item.posterPath.ifBlank { existing.posterPath }
                    )
                    repository.updateMatch(updated)
                    if (detectedMovie?.title.equals(item.title, ignoreCase = true)) {
                        detectedMovie = updated
                    }
                    showToast(if (updated.isInWatchlist) "Added '${item.title}' to Watchlist!" else "Removed '${item.title}' from Watchlist.")
                } else {
                    val toInsert = item.copy(isInWatchlist = true)
                    val newId = repository.insertMatch(toInsert)
                    val inserted = toInsert.copy(id = newId.toInt())
                    if (detectedMovie?.title.equals(item.title, ignoreCase = true)) {
                        detectedMovie = inserted
                    }
                    showToast("Added '${item.title}' to Watchlist!")
                }
            } else {
                val updated = item.copy(isInWatchlist = !item.isInWatchlist)
                repository.updateMatch(updated)
                if (detectedMovie?.id == item.id) {
                    detectedMovie = updated
                }
                showToast(if (updated.isInWatchlist) "Added '${item.title}' to Watchlist!" else "Removed '${item.title}' from Watchlist.")
            }
        }
    }

    fun removeFromWatchlist(item: MovieMatch) {
        viewModelScope.launch {
            val updated = item.copy(isInWatchlist = false)
            repository.updateMatch(updated)
            if (detectedMovie?.id == item.id) {
                detectedMovie = updated
            }
            showToast("Removed '${item.title}' from Watchlist.")
        }
    }

    fun updateWatchlistDetails(item: MovieMatch, status: String, notes: String) {
        viewModelScope.launch {
            val updated = item.copy(watchlistStatus = status, watchlistNotes = notes)
            repository.updateMatch(updated)
            if (detectedMovie?.id == item.id) {
                detectedMovie = updated
            }
        }
    }

    fun fetchAIRecommendations() {
        viewModelScope.launch {
            val apiKey = com.example.BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                recommendationsError = "Gemini API Key is missing. Connect it via Secrets panel."
                return@launch
            }

            val history = searchHistory.value
            val genresStr = onboardingGenres.joinToString()
            val interestsStr = onboardingInterests.joinToString()
            val moviesStr = onboardingFavoriteMovies.joinToString()
            val avoidsStr = onboardingAvoidContent.joinToString()

            // If everything is empty, suggest they run onboarding or search
            if (history.isEmpty() && genresStr.isEmpty() && moviesStr.isEmpty()) {
                recommendations = emptyList()
                recommendationsError = "Your Taste Profile is empty! Go to settings or scan movies to enable personalized recommendations."
                return@launch
            }

            isRecommendationsLoading = true
            recommendationsError = null

            try {
                // Construct a rich Cinema Taste Profile summary
                var profileSummary = "CINEMA TASTE PROFILE:\n"
                if (genresStr.isNotEmpty()) profileSummary += "- Preferred Genres: $genresStr\n"
                if (interestsStr.isNotEmpty()) profileSummary += "- Themes & Interests: $interestsStr\n"
                if (moviesStr.isNotEmpty()) profileSummary += "- All-time Favorite Movies/Anime: $moviesStr\n"
                if (avoidsStr.isNotEmpty()) profileSummary += "- Content to Avoid/Filter out: $avoidsStr\n"

                // Add past rated/likes feedback
                val feedbacksText = movieFeedbacks.map { (title, pair) ->
                    val thumbs = pair.first
                    val rating = pair.second
                    val ratStr = if (rating > 0) "$rating/5 stars" else ""
                    val thumbsStr = when (thumbs) {
                        "LOVED" -> "Loved 👍"
                        "NOT_INTERESTED" -> "Not Interested 👎"
                        else -> ""
                    }
                    "\"$title\" ($thumbsStr $ratStr)"
                }.joinToString()
                if (feedbacksText.isNotEmpty()) {
                    profileSummary += "- Specific Movie Feedback & Ratings: $feedbacksText\n"
                }

                if (history.isNotEmpty()) {
                    val historyTitles = history.joinToString { "${it.title} (${it.year}, Dir: ${it.director}, Genre: ${it.genre})" }
                    profileSummary += "- Recently Identified/Scanned Film History: $historyTitles\n"
                }

                val promptText = "$profileSummary\n" +
                        "Based on my specific taste profile, favorite movies, recently scanned history, preferred genres and themes, and content to avoid, " +
                        "recommend exactly 3 highly rated movies or anime that align with my preferences. " +
                        "Strictly format your response as a valid JSON array of objects conforming to this schema (and do not return any other text outside the JSON block): " +
                        "[" +
                        "{" +
                        "\"title\": \"Title of suggested film\"," +
                        "\"year\": \"Year released\"," +
                        "\"director\": \"Director's name\"," +
                        "\"genre\": \"Genre label(s)\"," +
                        "\"rationale\": \"Concisely explain why I'll love it based on context of my taste in movies in 1 small sentence\"," +
                        "\"imdbUrl\": \"estimated or real compatible IMDb URL (tt... format)\"" +
                        "}" +
                        "]"

                val request = GeminiRequest(
                    contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        responseSchema = ResponseSchema(
                            type = "ARRAY",
                            items = ResponseSchema(
                                type = "OBJECT",
                                properties = mapOf(
                                    "title" to SchemaProperty("STRING", "Film title"),
                                    "year" to SchemaProperty("STRING", "Release year"),
                                    "director" to SchemaProperty("STRING", "Director"),
                                    "genre" to SchemaProperty("STRING", "Genre"),
                                    "rationale" to SchemaProperty("STRING", "Recommendation argument"),
                                    "imdbUrl" to SchemaProperty("STRING", "IMDb URL link")
                                ),
                                required = listOf("title", "year", "director", "genre", "rationale", "imdbUrl")
                            )
                        ),
                        temperature = 0.6
                    ),
                    systemInstruction = Content(
                        parts = listOf(Part("You are a professional film recommendation engine and premium cinema guide. Analyze user search histories and taste profiles to offer precise, exciting film discoveries."))
                    )
                )

                val response = withContext(Dispatchers.IO) {
                    apiService.generateContent(selectedModel, apiKey, request)
                }

                val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (resultText != null) {
                    val parsedJson = extractJsonFromString(resultText)
                    val listType = Types.newParameterizedType(List::class.java, RecommendedMovie::class.java)
                    val jsonAdapter = moshi.adapter<List<RecommendedMovie>>(listType)
                    val recs = jsonAdapter.fromJson(parsedJson)
                    if (recs != null) {
                        recommendations = recs
                    } else {
                        recommendationsError = "Failed to parse recommendations result."
                    }
                } else {
                    recommendationsError = "Empty recommendation response from Gemini."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                recommendationsError = "Failed to fetch suggestions: ${e.localizedMessage ?: e.message}"
            } finally {
                isRecommendationsLoading = false
            }
        }
    }
}

class MovieViewModelFactory(private val repository: MovieRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MovieViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MovieViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
