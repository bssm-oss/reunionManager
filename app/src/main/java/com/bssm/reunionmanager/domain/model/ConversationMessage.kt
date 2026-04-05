package com.bssm.reunionmanager.domain.model

data class ConversationMessage(
    val id: Long,
    val senderName: String,
    val sentAtEpochMillis: Long,
    val content: String,
)
