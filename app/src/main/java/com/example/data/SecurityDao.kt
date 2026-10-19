package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUserDao {
    @Query("SELECT * FROM app_users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<AppUser>>

    @Query("SELECT * FROM app_users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): AppUser?

    @Query("SELECT * FROM app_users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): AppUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: AppUser)

    @Update
    suspend fun updateUser(user: AppUser)

    @Query("DELETE FROM app_users WHERE id = :id")
    suspend fun deleteUserById(id: String)

    @Query("SELECT COUNT(*) FROM app_users")
    suspend fun getUserCount(): Int
}

@Dao
interface DeletedRecordDao {
    @Query("SELECT * FROM deleted_records_archive ORDER BY deletedAt DESC")
    fun getAllDeletedRecords(): Flow<List<DeletedRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedRecord(record: DeletedRecord)

    @Query("DELETE FROM deleted_records_archive WHERE id = :id")
    suspend fun deleteDeletedRecordById(id: String)

    @Query("DELETE FROM deleted_records_archive")
    suspend fun clearArchive()

    @Query("SELECT COUNT(*) FROM deleted_records_archive")
    suspend fun getDeletedCount(): Int
}

@Dao
interface ConfigRestorePointDao {
    @Query("SELECT * FROM config_restore_points ORDER BY createdAt DESC")
    fun getAllRestorePoints(): Flow<List<ConfigRestorePoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestorePoint(point: ConfigRestorePoint)

    @Query("SELECT * FROM config_restore_points WHERE id = :id LIMIT 1")
    suspend fun getRestorePointById(id: String): ConfigRestorePoint?

    @Query("DELETE FROM config_restore_points WHERE id = :id")
    suspend fun deleteRestorePointById(id: String)

    @Query("SELECT COUNT(*) FROM config_restore_points")
    suspend fun getRestorePointCount(): Int
}
