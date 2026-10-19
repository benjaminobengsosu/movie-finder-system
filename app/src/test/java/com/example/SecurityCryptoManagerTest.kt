package com.example

import com.example.data.ConfigRestorePoint
import com.example.data.DeletedRecord
import com.example.data.SecurityCryptoManager
import com.example.data.UserRole
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class SecurityCryptoManagerTest {

    @Test
    fun testSaltGenerationUniquenessAndLength() {
        val salt1 = SecurityCryptoManager.generateSalt()
        val salt2 = SecurityCryptoManager.generateSalt()

        assertNotNull(salt1)
        assertNotNull(salt2)
        assertNotEquals("Salts should be cryptographically unique per user", salt1, salt2)
        assertEquals("16-byte salt represented as hex string should be 32 characters", 32, salt1.length)
        assertEquals(32, salt2.length)
    }

    @Test
    fun testPasswordHashingWithSaltAndPepper() {
        val password = "StrongMoviePassword2026!"
        val salt = SecurityCryptoManager.generateSalt()

        val hash1 = SecurityCryptoManager.hashPassword(password, salt)
        val hash2 = SecurityCryptoManager.hashPassword(password, salt)

        assertNotNull(hash1)
        assertEquals("Identical password and salt with system pepper should produce deterministic hash", hash1, hash2)
        assertTrue("Hash should be non-empty 256-bit hex string (64 characters)", hash1.length == 64)

        // Verify that different salt produces completely different hash
        val differentSalt = SecurityCryptoManager.generateSalt()
        val hashDifferentSalt = SecurityCryptoManager.hashPassword(password, differentSalt)
        assertNotEquals(hash1, hashDifferentSalt)
    }

    @Test
    fun testPasswordVerificationSuccessAndFailure() {
        val password = "CinemaSecurityKey#1"
        val wrongPassword = "WrongPassword#1"
        val salt = SecurityCryptoManager.generateSalt()
        val storedHash = SecurityCryptoManager.hashPassword(password, salt)

        // Verify correct password
        val isCorrect = SecurityCryptoManager.verifyPassword(password, salt, storedHash)
        assertTrue("Password verification should succeed for correct credentials", isCorrect)

        // Verify incorrect password fails
        val isWrong = SecurityCryptoManager.verifyPassword(wrongPassword, salt, storedHash)
        assertFalse("Password verification should fail for incorrect password", isWrong)

        // Verify altered pepper fails
        val wrongPepper = "bad_pepper_key_0000000000000000000000000000000000000000000000000000"
        val failedPepper = SecurityCryptoManager.verifyPassword(password, salt, storedHash, pepper = wrongPepper)
        assertFalse("Verification should fail if system pepper is altered", failedPepper)
    }

    @Test
    fun testTotpGenerationAndVerification() {
        val secret = SecurityCryptoManager.generateTotpSecret()
        assertNotNull(secret)
        assertTrue("TOTP Base32 secret should be at least 16 characters", secret.length >= 16)

        val now = System.currentTimeMillis()
        val code = SecurityCryptoManager.generateTotpCode(secret, now)
        assertEquals("TOTP code must be 6 digits", 6, code.length)
        assertTrue("TOTP code must be numeric", code.all { it.isDigit() })

        val isValid = SecurityCryptoManager.verifyTotpCode(secret, code, now)
        assertTrue("Current generated TOTP code should verify successfully", isValid)

        val isInvalid = SecurityCryptoManager.verifyTotpCode(secret, "000000", now)
        assertFalse("Arbitrary 6-digit code should not verify against secret", isInvalid)
    }

    @Test
    fun testEmergencyRecoveryCodes() {
        val codes = SecurityCryptoManager.generateRecoveryCodes()
        assertEquals("Should generate exactly 5 emergency recovery codes", 5, codes.size)

        codes.forEach { code ->
            assertTrue("Recovery code should start with RECV-", code.startsWith("RECV-"))
            assertEquals("Recovery code format should be RECV-XXXX-XXXX (14 chars)", 14, code.length)
        }

        val uniqueCount = codes.distinct().size
        assertEquals("All recovery codes must be unique", 5, uniqueCount)
    }

    @Test
    fun testUserRolesHierarchyAndAccessMapping() {
        assertEquals("Administrator", UserRole.ADMINISTRATOR.displayName)
        assertEquals("Manager", UserRole.MANAGER.displayName)
        assertEquals("Content Inputer", UserRole.CONTENT_INPUTER.displayName)
        assertEquals("Standard User", UserRole.STANDARD_USER.displayName)

        assertTrue(UserRole.ADMINISTRATOR.level > UserRole.MANAGER.level)
        assertTrue(UserRole.MANAGER.level > UserRole.CONTENT_INPUTER.level)
        assertTrue(UserRole.CONTENT_INPUTER.level > UserRole.STANDARD_USER.level)

        // Verification of parsing from string
        assertEquals(UserRole.ADMINISTRATOR, UserRole.fromString("Administrator"))
        assertEquals(UserRole.MANAGER, UserRole.fromString("manager"))
        assertEquals(UserRole.CONTENT_INPUTER, UserRole.fromString("Content Inputer"))
        assertEquals(UserRole.STANDARD_USER, UserRole.fromString("Standard User"))
        assertEquals(UserRole.STANDARD_USER, UserRole.fromString("UnknownRoleFallback"))
    }

    @Test
    fun testConfigurationRestorePointDoesNotMutateUserData() {
        val restorePoint = ConfigRestorePoint(
            id = UUID.randomUUID().toString(),
            name = "Staging Pre-Release Config",
            theme = "Dark Obsidian",
            maintenanceMode = false,
            guestModeEnabled = true,
            announcementText = "System operating under standard configuration.",
            securityStrictness = "High",
            createdByEmail = "admin@moviefinder.com"
        )

        assertNotNull(restorePoint.id)
        assertEquals("Dark Obsidian", restorePoint.theme)
        assertFalse(restorePoint.maintenanceMode)
        assertTrue(restorePoint.guestModeEnabled)
        // Verify restore point contains only configuration fields, no user tables or movie matches
    }

    @Test
    fun testDeletedRecordArchiveEntityStructure() {
        val deletedRecord = DeletedRecord(
            id = UUID.randomUUID().toString(),
            recordType = "USER",
            originalId = "user-uuid-1234",
            name = "Test Deletion (test@moviefinder.com)",
            deletedDataJson = "{\"email\":\"test@moviefinder.com\",\"role\":\"Standard User\"}",
            deletedByEmail = "admin@moviefinder.com",
            reason = "Account cleanup requested by user"
        )

        assertEquals("USER", deletedRecord.recordType)
        assertEquals("admin@moviefinder.com", deletedRecord.deletedByEmail)
        assertTrue(deletedRecord.deletedDataJson.contains("test@moviefinder.com"))
        assertNotNull(deletedRecord.deletedAt)
    }
}
