package com.bssm.reunionmanager.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [Index(value = ["sourceHash"], unique = true)],
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceName: String,
    val sourceHash: String,
    val importedAtEpochMillis: Long,
    val exportedAtEpochMillis: Long?,
    val participantCount: Int,
    val messageCount: Int,
)
