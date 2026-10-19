package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * Four distinct user roles supported across the application.
 */
enum class UserRole(val displayName: String, val level: Int, val description: String) {
    ADMINISTRATOR(
        "Administrator",
        4,
        "Full system administration, user management, deleted data audit, and restore points"
    ),
    MANAGER(
        "Manager",
        3,
        "Operational management, remote announcements, analytics, and credential reports"
    ),
    CONTENT_INPUTER(
        "Content Inputer",
        2,
        "Movie scene tagging, manual catalog curation, and AI scene testing"
    ),
    STANDARD_USER(
        "Standard User",
        1,
        "Core movie scene identification, personal watchlists, and profile preferences"
    );

    companion object {
        fun fromString(role: String?): UserRole {
            return entries.firstOrNull { 
                it.name.equals(role, ignoreCase = true) || 
                it.displayName.equals(role, ignoreCase = true) 
            } ?: STANDARD_USER
        }
    }
}

/**
 * User account entity with cryptographic salt, peppered hash, and 2FA credentials.
 */
@Entity(tableName = "app_users")
data class AppUser(
    @PrimaryKey val id: String,
    val email: String,
    val name: String,
    val role: String = UserRole.STANDARD_USER.displayName,
    val salt: String, // Hex-encoded 16-byte random salt
    val passwordHash: String, // PBKDF2 HMAC-SHA256 derived hash
    val twoFactorEnabled: Boolean = false,
    val twoFactorSecret: String? = null, // Base32 encoded TOTP key
    val recoveryCodes: String = "[]", // JSON array of hashed emergency single-use backup codes
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) : Serializable

/**
 * Archive database entity storing all data that has been deleted by an administrator.
 * Preserves a full historical audit trail.
 */
@Entity(tableName = "deleted_records_archive")
data class DeletedRecord(
    @PrimaryKey val id: String,
    val recordType: String, // "USER", "MOVIE", "CONFIG"
    val originalId: String,
    val name: String,
    val deletedDataJson: String,
    val deletedByEmail: String,
    val deletedAt: Long = System.currentTimeMillis(),
    val reason: String = "Administrative deletion"
) : Serializable

/**
 * Configuration-only restore point entity.
 * Captures system configuration snapshots without altering or deleting user data or movie records.
 */
@Entity(tableName = "config_restore_points")
data class ConfigRestorePoint(
    @PrimaryKey val id: String,
    val name: String,
    val theme: String = "Dark Obsidian",
    val maintenanceMode: Boolean = false,
    val guestModeEnabled: Boolean = true,
    val announcementText: String = "",
    val securityStrictness: String = "High",
    val createdAt: Long = System.currentTimeMillis(),
    val createdByEmail: String = "admin@moviefinder.com"
) : Serializable
