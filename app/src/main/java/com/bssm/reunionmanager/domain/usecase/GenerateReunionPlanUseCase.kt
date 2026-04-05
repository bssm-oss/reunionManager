package com.bssm.reunionmanager.domain.usecase

import com.bssm.reunionmanager.data.analysis.FakeAnalysisProvider
import com.bssm.reunionmanager.data.analysis.GeminiAnalysisProvider
import com.bssm.reunionmanager.data.repository.AnalysisRepository
import com.bssm.reunionmanager.data.repository.ConversationRepository
import com.bssm.reunionmanager.data.repository.ProviderSettingsRepository
import com.bssm.reunionmanager.domain.analysis.AnalysisProvider
import com.bssm.reunionmanager.domain.model.ProviderSettings

class GenerateReunionPlanUseCase(
    private val conversationRepository: ConversationRepository,
    private val analysisRepository: AnalysisRepository,
    private val providerSettingsRepository: ProviderSettingsRepository,
    private val fakeAnalysisProvider: FakeAnalysisProvider,
    private val geminiProviderFactory: (ProviderSettings) -> GeminiAnalysisProvider,
) {
    suspend operator fun invoke(conversationId: Long): Result<String> {
        val input = conversationRepository.buildAnalysisInput(conversationId)
            ?: return Result.failure(IllegalArgumentException("Conversation not found."))
        val settings = providerSettingsRepository.get()

        val provider: AnalysisProvider
        val providerType: String
        if (settings.isConfigured) {
            provider = geminiProviderFactory(settings)
            providerType = "gemini"
        } else {
            provider = fakeAnalysisProvider
            providerType = "fake"
        }

        val report = provider.analyze(input)
        analysisRepository.saveLatest(
            conversationId = conversationId,
            providerType = providerType,
            report = report,
        )
        return Result.success(providerType)
    }
}
