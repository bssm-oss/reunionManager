package com.bssm.reunionmanager.domain.usecase

import com.bssm.reunionmanager.data.analysis.FakeAnalysisProvider
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
    private val gemmaProviderFactory: (ProviderSettings) -> AnalysisProvider,
) {
    suspend operator fun invoke(conversationId: Long): Result<String> {
        return runCatching {
            val input = conversationRepository.buildAnalysisInput(conversationId)
                ?: throw IllegalArgumentException("Conversation not found.")
            val settings = providerSettingsRepository.get()

            val provider: AnalysisProvider
            val providerType: String
            // The fake provider keeps the MVP usable when no local Gemma model path is configured.
            if (settings.isConfigured) {
                provider = gemmaProviderFactory(settings)
                providerType = "gemma4"
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
            providerType
        }
    }
}
