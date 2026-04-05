package com.bssm.reunionmanager.domain.model

data class AnalysisReport(
    val headline: String,
    val relationshipSummary: String,
    val reunionObjective: String,
    val nextStep: String,
    val caution: String,
)
