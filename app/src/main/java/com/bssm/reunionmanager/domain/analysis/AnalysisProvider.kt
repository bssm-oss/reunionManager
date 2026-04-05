package com.bssm.reunionmanager.domain.analysis

import com.bssm.reunionmanager.domain.model.AnalysisInput
import com.bssm.reunionmanager.domain.model.AnalysisReport

interface AnalysisProvider {
    suspend fun analyze(input: AnalysisInput): AnalysisReport
}
