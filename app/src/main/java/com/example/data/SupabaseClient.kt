package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Supabase Cloud PostgreSQL REST client for Movie Finder.
 * Synchronizes users, movie discoveries, configuration restore points, and deleted archives
 * across the Android app and web versions.
 */
object SupabaseClient {

    private const val TAG = "SupabaseClient"

    // Default Supabase connection credentials
    // Note: Can be overridden at runtime via Settings or AppConfig
    var supabaseUrl: String = "https://aws-0-eu-west-1.pooler.supabase.com"
        private set
    var supabaseAnonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummy_anon_key_for_offline_first_mode"
        private set

    var isCloudSyncEnabled: Boolean = true
    var lastSyncTimestamp: Long = 0L
    var syncStatusText: String = "Cloud Ready (AWS Supabase)"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun configure(url: String, anonKey: String) {
        if (url.isNotBlank()) supabaseUrl = url.trim().removeSuffix("/")
        if (anonKey.isNotBlank()) supabaseAnonKey = anonKey.trim()
    }

    /**
     * Tests connectivity to the Supabase PostgreSQL REST instance.
     */
    suspend fun checkConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val success = response.isSuccessful || response.code == 404 || response.code == 401
                syncStatusText = if (success) "Connected to AWS Supabase" else "Offline (Room Local Mode)"
                lastSyncTimestamp = System.currentTimeMillis()
                Pair(success, "HTTP Status: ${response.code}")
            }
        } catch (e: Exception) {
            syncStatusText = "Local Room Mode (Network Offline)"
            Pair(false, e.localizedMessage ?: "Connection failed")
        }
    }

    /**
     * Pushes an AppUser to Supabase PostgreSQL table 'users'.
     */
    suspend fun upsertUser(user: AppUser): Boolean = withContext(Dispatchers.IO) {
        if (!isCloudSyncEnabled) return@withContext false
        try {
            val json = JSONObject().apply {
                put("id", user.id)
                put("email", user.email)
                put("name", user.name)
                put("role", user.role)
                put("salt", user.salt)
                put("password_hash", user.passwordHash)
                put("two_factor_enabled", user.twoFactorEnabled)
                put("two_factor_secret", user.twoFactorSecret ?: JSONObject.NULL)
                put("recovery_codes", user.recoveryCodes)
                put("created_at", user.createdAt)
                put("last_login_at", user.lastLoginAt)
                put("is_active", user.isActive)
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/users")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                lastSyncTimestamp = System.currentTimeMillis()
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud upsertUser fallback to local Room: ${e.message}")
            false
        }
    }

    /**
     * Archives a deleted record to Supabase table 'deleted_records_archive'.
     */
    suspend fun archiveDeletedRecord(record: DeletedRecord): Boolean = withContext(Dispatchers.IO) {
        if (!isCloudSyncEnabled) return@withContext false
        try {
            val json = JSONObject().apply {
                put("id", record.id)
                put("record_type", record.recordType)
                put("original_id", record.originalId)
                put("name", record.name)
                put("deleted_data_json", record.deletedDataJson)
                put("deleted_by_email", record.deletedByEmail)
                put("deleted_at", record.deletedAt)
                put("reason", record.reason)
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/deleted_records_archive")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud archiveDeletedRecord fallback to local Room: ${e.message}")
            false
        }
    }

    /**
     * Pushes a configuration restore point snapshot to Supabase table 'config_restore_points'.
     */
    suspend fun saveRestorePoint(point: ConfigRestorePoint): Boolean = withContext(Dispatchers.IO) {
        if (!isCloudSyncEnabled) return@withContext false
        try {
            val json = JSONObject().apply {
                put("id", point.id)
                put("name", point.name)
                put("theme", point.theme)
                put("maintenance_mode", point.maintenanceMode)
                put("guest_mode_enabled", point.guestModeEnabled)
                put("announcement_text", point.announcementText)
                put("security_strictness", point.securityStrictness)
                put("created_at", point.createdAt)
                put("created_by_email", point.createdByEmail)
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/config_restore_points")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud saveRestorePoint fallback to local Room: ${e.message}")
            false
        }
    }
}
