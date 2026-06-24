package com.bssm.reunionmanager.domain.usecase

import com.bssm.reunionmanager.data.analysis.FakeAnalysisProvider
import com.bssm.reunionmanager.data.repository.AnalysisRepository
import com.bssm.reunionmanager.data.repository.ConversationRepository
import com.bssm.reunionmanager.data.repository.ProviderSettingsRepository
import com.bssm.reunionmanager.domain.analysis.AnalysisProvider
import com.bssm.reunionmanager.domain.analysis.AnalysisSafetyRules
import com.bssm.reunionmanager.domain.model.ProviderSettings

class GenerateReunionPlanUseCase(
    private val conversationRepository: ConversationRepository,
    private val analysisRepository: AnalysisRepository,
    private val providerSettingsRepository: ProviderSettingsRepository,
    private val fakeAnalysisProvider: FakeAnalysisProvider,
    private val gemmaProviderFactory: (ProviderSettings) -> AnalysisProvider,
    private val openRouterProviderFactory: () -> AnalysisProvider? = { null },
) {
    suspend operator fun invoke(conversationId: Long): Result<String> {
        return runCatching {
            val settings = providerSettingsRepository.get()
            val input = conversationRepository.buildAnalysisInput(
                conversationId = conversationId,
                userDisplayName = settings.userDisplayName,
            )
                ?: throw IllegalArgumentException("Conversation not found.")

            val provider: AnalysisProvider
            val providerType: String
            val openRouterProvider = openRouterProviderFactory()
            if (openRouterProvider != null) {
                provider = openRouterProvider
                providerType = "openrouter-deepseek-v4-flash"
            } else if (settings.isModelVerified) {
                provider = gemmaProviderFactory(settings)
                providerType = "gemma4"
            } else {
                provider = fakeAnalysisProvider
                providerType = "fake"
            }

            val report = AnalysisSafetyRules.finalizeReport(provider.analyze(input), input)
            analysisRepository.saveLatest(
                conversationId = conversationId,
                providerType = providerType,
                report = report,
            )
            providerType
        }
    }
}
