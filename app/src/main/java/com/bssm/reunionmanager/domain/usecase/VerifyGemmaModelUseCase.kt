package com.bssm.reunionmanager.domain.usecase

import com.bssm.reunionmanager.data.repository.ProviderSettingsRepository
import com.bssm.reunionmanager.domain.analysis.AnalysisProvider
import com.bssm.reunionmanager.domain.analysis.AnalysisSafetyRules
import com.bssm.reunionmanager.domain.model.AnalysisInput
import com.bssm.reunionmanager.domain.model.GemmaModelFileRules
import com.bssm.reunionmanager.domain.model.ProviderSettings
import java.io.File

class VerifyGemmaModelUseCase(
    private val providerSettingsRepository: ProviderSettingsRepository,
    private val gemmaProviderFactory: (ProviderSettings) -> AnalysisProvider,
) {
    suspend operator fun invoke(): Result<String> {
        return runCatching {
            val settings = providerSettingsRepository.get()
            require(settings.isConfigured) {
                "모델 파일을 먼저 선택하세요."
            }

            val modelFile = File(settings.modelPath)
            require(modelFile.isFile) {
                "모델 파일을 찾을 수 없습니다. 모델을 다시 선택하세요."
            }
            val modelName = settings.modelName.ifBlank { modelFile.name }
            GemmaModelFileRules.requireSupportedFileName(modelName)
            GemmaModelFileRules.requirePlausibleModelSize(modelFile.length())

            val input = smokeInput(settings.userDisplayName.ifBlank { "나" })
            val report = AnalysisSafetyRules.finalizeReport(gemmaProviderFactory(settings).analyze(input), input)
            require(report.messageDraft.isNotBlank()) {
                "모델 응답을 계획 형식으로 읽지 못했습니다."
            }
            modelName
        }
    }

    private fun smokeInput(userDisplayName: String): AnalysisInput {
        return AnalysisInput(
            conversationTitle = "모델 점검 대화",
            participantNames = listOf(userDisplayName, "민지"),
            messageCount = 2,
            excerpt = "$userDisplayName: 오랜만이야\n민지: 나도 가끔 생각났어",
            recentExcerpt = "$userDisplayName: 오랜만이야\n민지: 나도 가끔 생각났어",
            signalExcerpt = "민지: 나도 가끔 생각났어",
            statsSummary = """
                마지막 메시지: 나도 가끔 생각났어
                마지막 발신자의 연속 발화: 1개
                마지막 메시지 이후 경과: 알 수 없음
            """.trimIndent(),
            perspectiveSummary = """
                내 카톡 이름: $userDisplayName
                상대 후보: 민지
                마지막 메시지 발신자 역할: 상대
                마지막 연속 발화 역할: 상대 1개
                내 최근 메시지: 오랜만이야
                상대 최근 메시지: 나도 가끔 생각났어
                내 마지막 연속 발화: 0개
                상대 마지막 연속 발화: 1개
            """.trimIndent(),
        )
    }
}
