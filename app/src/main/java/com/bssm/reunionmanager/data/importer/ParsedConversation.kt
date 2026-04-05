package com.bssm.reunionmanager.data.importer

data class ParsedConversation(
    val title: String,
    val exportedAtEpochMillis: Long?,
    val participants: List<String>,
    val messages: List<ParsedMessage>,
)

data class ParsedMessage(
    val senderName: String,
    val sentAtEpochMillis: Long,
    val content: String,
)
