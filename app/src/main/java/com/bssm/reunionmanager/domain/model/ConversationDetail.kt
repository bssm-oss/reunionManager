package com.bssm.reunionmanager.domain.model

data class ConversationDetail(
    val id: Long,
    val title: String,
    val sourceName: String,
    val participantNames: List<String>,
    val messages: List<ConversationMessage>,
    val latestAnalysis: AnalysisReport?,
)
