package com.bssm.reunionmanager.domain.model

data class AnalysisInput(
    val conversationTitle: String,
    val participantNames: List<String>,
    val messageCount: Int,
    val excerpt: String,
)
