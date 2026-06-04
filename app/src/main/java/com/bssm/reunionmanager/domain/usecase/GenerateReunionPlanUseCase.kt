package com.bssm.reunionmanager.domain.usecase

import com.bssm.reunionmanager.data.analysis.FakeAnalysisProvider
import com.bssm.reunionmanager.data.repository.AnalysisRepository
import com.bssm.reunionmanager.data.repository.ConversationRepository
import com.bssm.reunionmanager.data.repository.ProviderSettingsRepository
import com.bssm.reunionmanager.domain.analysis.AnalysisProvider
import com.bssm.reunionmanager.domain.analysis.AnalysisSafetyRules
import com.bssm.reunionmanager.domain.model.AnalysisInput
import com.bssm.reunionmanager.domain.model.AnalysisReport
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
            val settings = providerSettingsRepository.get()
            val input = conversationRepository.buildAnalysisInput(
                conversationId = conversationId,
                userDisplayName = settings.userDisplayName,
            )
                ?: throw IllegalArgumentException("Conversation not found.")

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

            val report = AnalysisSafetyRules.sanitizeReport(provider.analyze(input), input)
                .applySafetyGuardrails(input)
                .applyPerspectiveNotice(input)
            analysisRepository.saveLatest(
                conversationId = conversationId,
                providerType = providerType,
                report = report,
            )
            providerType
        }
    }

    private fun AnalysisReport.applySafetyGuardrails(input: AnalysisInput): AnalysisReport {
        return when {
            AnalysisSafetyRules.requiresHold(input) -> copy(
                contactReadiness = "지금은 보류",
                evidence = evidence.appendEvidence(AnalysisSafetyRules.guardrailEvidence(input)),
                reunionObjective = "상대에게 다시 부담을 주지 않는 것이 우선입니다.",
                nextStep = "오늘은 보내지 말고, 경계 표현이나 내가 반복해서 보낸 메시지를 먼저 다시 확인하세요.",
                messageDraft = "오늘은 보내지 않습니다. 상대가 먼저 답하거나 시간이 충분히 지난 뒤 다시 판단하세요.",
                alternativeDrafts = "오늘은 보내지 않기\n상대가 먼저 답하면 그때 짧게 답하기\n다시 보내기 전 최근 대화와 경계 표현 확인하기",
                caution = "경계 표현이나 무응답 신호가 있으면 다시 연락하지 않는 선택도 계획에 포함해야 합니다.",
            )

            AnalysisSafetyRules.needsUserPerspective(input) -> copy(
                headline = "내 이름 확인",
                contactReadiness = "정보 부족",
                evidence = evidence.appendEvidence(AnalysisSafetyRules.perspectiveSetupEvidence(input)),
                relationshipSummary = "내 카톡 이름이 없어 이 대화만으로는 답장인지 첫 연락인지 확정할 수 없습니다.",
                reunionObjective = "보낼 문장을 만들기보다 내 카톡 이름을 먼저 저장해 관점을 맞추는 것이 목표입니다.",
                nextStep = "설정에서 내 카톡 이름을 저장한 뒤 같은 대화를 다시 분석하세요.",
                messageDraft = "지금은 보낼 문장을 만들지 않습니다. 내 카톡 이름을 먼저 저장하세요.",
                alternativeDrafts = "내 카톡 이름 저장하기\n같은 대화 다시 분석하기\n최근 대화 파일인지 확인하기",
                caution = "발신자 관점이 틀리면 상대가 기다리는 상황을 새 연락처럼 잘못 해석할 수 있습니다.",
            )

            AnalysisSafetyRules.hasWeakReunionContext(input) -> copy(
                headline = "관계 맥락 확인",
                contactReadiness = "정보 부족",
                evidence = evidence.appendEvidence(AnalysisSafetyRules.weakContextEvidence(input)),
                relationshipSummary = "이 대화만으로는 재회 판단에 필요한 1:1 개인 관계 맥락이 충분하지 않습니다.",
                reunionObjective = "보낼 문장을 만들기보다 분석 대상 대화가 맞는지 먼저 확인하는 것이 목표입니다.",
                nextStep = "오늘은 보내지 말고, 1:1 개인 관계 대화인지 또는 더 관련 있는 대화 파일이 있는지 먼저 확인하세요.",
                messageDraft = "지금은 보낼 문장을 만들지 않습니다. 재회와 직접 관련 있는 대화를 먼저 확인하세요.",
                alternativeDrafts = "대화가 1:1 개인 관계인지 확인하기\n내 카톡 이름 저장하기\n더 관련 있는 대화 파일로 다시 분석하기",
                caution = "업무, 단체, 기술 대화는 재회 가능성 판단 근거로 부족할 수 있습니다.",
            )

            AnalysisSafetyRules.counterpartFinalRunCount(input) > 0 -> copy(
                evidence = evidence.appendEvidence("상대가 마지막에 메시지를 남긴 상태라 새 연락보다 짧은 답장이 더 자연스럽습니다."),
                reunionObjective = "새 연락을 시작하기보다 상대가 남긴 말에 짧고 낮은 압박으로 답하는 것이 목표입니다.",
                nextStep = "상대의 마지막 메시지에 바로 답하되, 재회 이야기보다 짧은 안부와 확인만 남기세요.",
                messageDraft = AnalysisSafetyRules.counterpartReplyDraft(input, messageDraft),
                alternativeDrafts = AnalysisSafetyRules.counterpartReplyAlternatives(input, alternativeDrafts),
            )

            else -> this
        }
    }

    private fun AnalysisReport.applyPerspectiveNotice(input: AnalysisInput): AnalysisReport {
        return if (input.perspectiveSummary.contains("내 카톡 이름: 설정되지 않음")) {
            copy(
                evidence = evidence.appendEvidence(
                    "관점 주의: 내 카톡 이름이 없어 마지막 발신자가 사용자인지 상대인지 확정하지 못했습니다.",
                ),
            )
        } else {
            this
        }
    }

    private fun String.appendEvidence(extra: String): String {
        return lineSequence()
            .plus(extra.lineSequence())
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(separator = "\n")
    }
}
