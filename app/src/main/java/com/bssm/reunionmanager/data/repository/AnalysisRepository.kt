package com.bssm.reunionmanager.data.repository

import com.bssm.reunionmanager.data.local.dao.AnalysisResultDao
import com.bssm.reunionmanager.data.local.entity.AnalysisResultEntity
import com.bssm.reunionmanager.domain.model.AnalysisReport

class AnalysisRepository(
    private val analysisResultDao: AnalysisResultDao,
) {
    suspend fun saveLatest(
        conversationId: Long,
        providerType: String,
        report: AnalysisReport,
    ) {
        analysisResultDao.insert(
            AnalysisResultEntity(
                conversationId = conversationId,
                providerType = providerType,
                createdAtEpochMillis = System.currentTimeMillis(),
                headline = report.headline,
                relationshipSummary = report.relationshipSummary,
                reunionObjective = report.reunionObjective,
                nextStep = report.nextStep,
                caution = report.caution,
            ),
        )
    }
}
