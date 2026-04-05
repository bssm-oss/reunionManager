package com.bssm.reunionmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bssm.reunionmanager.data.local.entity.ParticipantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParticipantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ParticipantEntity>)

    @Query("SELECT * FROM participants WHERE conversationId = :conversationId ORDER BY name ASC")
    fun observeByConversationId(conversationId: Long): Flow<List<ParticipantEntity>>

    @Query("SELECT * FROM participants WHERE conversationId = :conversationId ORDER BY name ASC")
    suspend fun getByConversationId(conversationId: Long): List<ParticipantEntity>
}
