package com.bssm.reunionmanager.domain.model

data class ConversationSummary(
    val id: Long,
    val title: String,
    val participantCount: Int,
    val messageCount: Int,
    val importedAtEpochMillis: Long,
    val sourceName: String,
    val latestAnalysisHeadline: String?,
    val latestAnalysisContactReadiness: String?,
    val latestAnalysisNextStep: String?,
    val latestAnalysisCreatedAtEpochMillis: Long?,
)
