package com.bssm.reunionmanager.domain.model

data class AnalysisReport(
    val headline: String,
    val contactReadiness: String,
    val evidence: String,
    val relationshipSummary: String,
    val reunionObjective: String,
    val nextStep: String,
    val messageDraft: String,
    val alternativeDrafts: String,
    val caution: String,
)
