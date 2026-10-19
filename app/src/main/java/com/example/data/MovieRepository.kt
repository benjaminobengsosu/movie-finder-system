package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

class MovieRepository(
    private val dao: MovieMatchDao,
    val userDao: AppUserDao? = null,
    val deletedDao: DeletedRecordDao? = null,
    val restoreDao: ConfigRestorePointDao? = null,
    private val sceneRepository: MovieSceneRepository = GeminiMovieSceneRepository()
) : MovieSceneRepository by sceneRepository {

    // Movie Matches & Watchlist
    val allMatches: Flow<List<MovieMatch>> = dao.getAllMatches()
    val watchlistMatches: Flow<List<MovieMatch>> = dao.getWatchlistMatches()

    suspend fun insertMatch(match: MovieMatch): Long = withContext(Dispatchers.IO) {
        dao.insertMatch(match)
    }

    suspend fun updateMatch(match: MovieMatch) = withContext(Dispatchers.IO) {
        dao.updateMatch(match)
    }

    suspend fun deleteMatch(id: Int) = withContext(Dispatchers.IO) {
        val existing = dao.getMatchById(id)
        if (existing != null && deletedDao != null) {
            val json = JSONObject().apply {
                put("id", existing.id)
                put("title", existing.title)
                put("year", existing.year)
                put("director", existing.director)
                put("imdbUrl", existing.imdbUrl)
                put("genre", existing.genre)
            }
            deletedDao.insertDeletedRecord(
                DeletedRecord(
                    id = UUID.randomUUID().toString(),
                    recordType = "MOVIE",
                    originalId = existing.id.toString(),
                    name = existing.title,
                    deletedDataJson = json.toString(),
                    deletedByEmail = "admin@moviefinder.com",
                    reason = "User deleted movie record"
                )
            )
        }
        dao.deleteMatchById(id)
    }

    suspend fun getMatchById(id: Int): MovieMatch? = withContext(Dispatchers.IO) {
        dao.getMatchById(id)
    }

    suspend fun findMatchByTitle(title: String): MovieMatch? = withContext(Dispatchers.IO) {
        dao.findMatchByTitle(title)
    }

    suspend fun findMatchByTitleAndYear(title: String, year: String): MovieMatch? = withContext(Dispatchers.IO) {
        dao.findMatchByTitleAndYear(title, year)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dao.clearAllMatches()
    }

    // =========================================================================
    // User & Security Management
    // =========================================================================

    val allUsers: Flow<List<AppUser>> = userDao?.getAllUsers() ?: emptyFlow()

    suspend fun getUserByEmail(email: String): AppUser? = withContext(Dispatchers.IO) {
        userDao?.getUserByEmail(email)
    }

    suspend fun getUserById(id: String): AppUser? = withContext(Dispatchers.IO) {
        userDao?.getUserById(id)
    }

    suspend fun insertUser(user: AppUser) = withContext(Dispatchers.IO) {
        userDao?.insertUser(user)
        SupabaseClient.upsertUser(user)
    }

    suspend fun updateUser(user: AppUser) = withContext(Dispatchers.IO) {
        userDao?.updateUser(user)
        SupabaseClient.upsertUser(user)
    }

    suspend fun deleteUser(userId: String, deletedBy: String, reason: String): Boolean = withContext(Dispatchers.IO) {
        val user = userDao?.getUserById(userId) ?: return@withContext false
        if (deletedDao != null) {
            val userJson = JSONObject().apply {
                put("id", user.id)
                put("email", user.email)
                put("name", user.name)
                put("role", user.role)
                put("salt", user.salt)
                put("passwordHash", user.passwordHash)
                put("twoFactorEnabled", user.twoFactorEnabled)
                put("createdAt", user.createdAt)
            }
            val record = DeletedRecord(
                id = UUID.randomUUID().toString(),
                recordType = "USER",
                originalId = user.id,
                name = "${user.name} (${user.email})",
                deletedDataJson = userJson.toString(),
                deletedByEmail = deletedBy,
                reason = reason
            )
            deletedDao.insertDeletedRecord(record)
            SupabaseClient.archiveDeletedRecord(record)
        }
        userDao.deleteUserById(userId)
        true
    }

    suspend fun restoreDeletedUser(deletedRecordId: String): Boolean = withContext(Dispatchers.IO) {
        if (deletedDao == null || userDao == null) return@withContext false
        val records = withContext(Dispatchers.IO) {
            // Read record
            null
        }
        false
    }

    // =========================================================================
    // Deleted Records Archive (Audit / Recycle Bin Database)
    // =========================================================================

    val allDeletedRecords: Flow<List<DeletedRecord>> = deletedDao?.getAllDeletedRecords() ?: emptyFlow()

    suspend fun clearDeletedArchive() = withContext(Dispatchers.IO) {
        deletedDao?.clearArchive()
    }

    suspend fun deleteArchiveRecord(id: String) = withContext(Dispatchers.IO) {
        deletedDao?.deleteDeletedRecordById(id)
    }

    // =========================================================================
    // Configuration Restore Points (Snapshot Management)
    // =========================================================================

    val allRestorePoints: Flow<List<ConfigRestorePoint>> = restoreDao?.getAllRestorePoints() ?: emptyFlow()

    suspend fun createRestorePoint(point: ConfigRestorePoint) = withContext(Dispatchers.IO) {
        restoreDao?.insertRestorePoint(point)
        SupabaseClient.saveRestorePoint(point)
    }

    suspend fun getRestorePointById(id: String): ConfigRestorePoint? = withContext(Dispatchers.IO) {
        restoreDao?.getRestorePointById(id)
    }

    suspend fun deleteRestorePoint(id: String) = withContext(Dispatchers.IO) {
        restoreDao?.deleteRestorePointById(id)
    }
}
