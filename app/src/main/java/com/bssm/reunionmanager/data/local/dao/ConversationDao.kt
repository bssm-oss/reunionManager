package com.bssm.reunionmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bssm.reunionmanager.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ConversationEntity): Long

    @Query("SELECT * FROM conversations ORDER BY importedAtEpochMillis DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    fun observeById(conversationId: Long): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getById(conversationId: Long): ConversationEntity?

    @Query("SELECT id FROM conversations WHERE sourceHash = :sourceHash LIMIT 1")
    suspend fun findIdBySourceHash(sourceHash: String): Long?
}
