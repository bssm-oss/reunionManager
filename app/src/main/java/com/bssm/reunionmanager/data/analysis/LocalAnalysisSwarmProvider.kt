package com.bssm.reunionmanager.data.analysis

import com.bssm.reunionmanager.domain.analysis.AnalysisProvider
import com.bssm.reunionmanager.domain.analysis.AnalysisSafetyRules
import com.bssm.reunionmanager.domain.model.AnalysisInput
import com.bssm.reunionmanager.domain.model.AnalysisReport
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class LocalAnalysisSwarmProvider(
    private val draftProvider: AnalysisProvider,
    private val baselineProvider: AnalysisProvider,
) : AnalysisProvider {
    override suspend fun analyze(input: AnalysisInput): AnalysisReport = coroutineScope {
        val decision = AnalysisSafetyRules.evaluate(input)
        val baselineDeferred = async { AnalysisSafetyRules.finalizeReport(baselineProvider.analyze(input), input) }

        when (decision.action) {
            AnalysisSafetyRules.AnalysisAction.RequirePerspective,
            AnalysisSafetyRules.AnalysisAction.HoldContact,
            AnalysisSafetyRules.AnalysisAction.CheckContext -> baselineDeferred.await().withSwarmEvidence(decision)

            AnalysisSafetyRules.AnalysisAction.ReplyToCounterpart,
            AnalysisSafetyRules.AnalysisAction.ModelDraft -> {
                val draftDeferred = async { draftProvider.analyze(input) }
                val baseline = baselineDeferred.await()
                val rawDraft = draftDeferred.await()
                val sanitizedDraft = AnalysisSafetyRules.sanitizeReport(rawDraft, input)
                val selected = selectReport(
                    rawDraft = rawDraft,
                    sanitizedDraft = sanitizedDraft,
                    baseline = baseline,
                )
                AnalysisSafetyRules.finalizeReport(selected, input).withSwarmEvidence(decision)
            }
        }
    }

    private fun selectReport(
        rawDraft: AnalysisReport,
        sanitizedDraft: AnalysisReport,
        baseline: AnalysisReport,
    ): AnalysisReport {
        val rawReadinessAllowed = AnalysisSafetyRules.isAllowedReadiness(rawDraft.contactReadiness)
        if (!rawReadinessAllowed) {
            return baseline.copy(
                evidence = AnalysisSafetyRules.appendEvidence(
                    baseline.evidence,
                    "모델 검수: 허용되지 않은 연락 판단을 내보내 로컬 기준으로 대체했습니다.",
                ),
            )
        }

        if (sanitizedDraft.contactReadiness == "정보 부족" && baseline.contactReadiness != "정보 부족") {
            return baseline.copy(
                evidence = AnalysisSafetyRules.appendEvidence(
                    baseline.evidence,
                    "모델 검수: 초안 근거가 약해 로컬 기준 답변으로 낮췄습니다.",
                ),
            )
        }

        return sanitizedDraft
    }

    private fun AnalysisReport.withSwarmEvidence(
        decision: AnalysisSafetyRules.AnalysisDecision,
    ): AnalysisReport {
        return copy(
            evidence = AnalysisSafetyRules.appendEvidence(
                evidence,
                "로컬 병렬 검수: 안전, 마지막 메시지, 관계 맥락을 각각 확인했습니다. (${decision.action.toEvidenceLabel()})",
            ),
        )
    }

    private fun AnalysisSafetyRules.AnalysisAction.toEvidenceLabel(): String {
        return when (this) {
            AnalysisSafetyRules.AnalysisAction.RequirePerspective -> "이름 확인"
            AnalysisSafetyRules.AnalysisAction.HoldContact -> "보류"
            AnalysisSafetyRules.AnalysisAction.CheckContext -> "맥락 확인"
            AnalysisSafetyRules.AnalysisAction.ReplyToCounterpart -> "답장"
            AnalysisSafetyRules.AnalysisAction.ModelDraft -> "초안 생성"
        }
    }
}
