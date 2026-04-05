package com.bssm.reunionmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bssm.reunionmanager.data.local.entity.AnalysisResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AnalysisResultEntity): Long

    @Query(
        """
        SELECT * FROM analysis_results
        WHERE conversationId = :conversationId
        ORDER BY createdAtEpochMillis DESC
        LIMIT 1
        """,
    )
    fun observeLatestForConversation(conversationId: Long): Flow<AnalysisResultEntity?>

    @Query(
        """
        SELECT * FROM analysis_results
        WHERE conversationId = :conversationId
        ORDER BY createdAtEpochMillis DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestForConversation(conversationId: Long): AnalysisResultEntity?
}
