package com.bssm.reunionmanager.data.analysis

import com.bssm.reunionmanager.domain.analysis.AnalysisProvider
import com.bssm.reunionmanager.domain.analysis.AnalysisSafetyRules
import com.bssm.reunionmanager.domain.model.AnalysisInput
import com.bssm.reunionmanager.domain.model.AnalysisReport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope

class LocalAnalysisSwarmProvider(
    private val draftProvider: AnalysisProvider,
    private val baselineProvider: AnalysisProvider,
) : AnalysisProvider {
    override suspend fun analyze(input: AnalysisInput): AnalysisReport = supervisorScope {
        val decisionDeferred = async { AnalysisSafetyRules.evaluate(input) }
        val lastMessageReviewDeferred = async { input.lastMessageReviewLabel() }
        val contextReviewDeferred = async { input.contextReviewLabel() }
        val baselineDeferred = async { AnalysisSafetyRules.finalizeReport(baselineProvider.analyze(input), input) }
        val decision = decisionDeferred.await()
        val review = SwarmReview(
            safety = decision.safetyReviewLabel(),
            lastMessage = lastMessageReviewDeferred.await(),
            context = contextReviewDeferred.await(),
        )

        when (decision.action) {
            AnalysisSafetyRules.AnalysisAction.RequirePerspective,
            AnalysisSafetyRules.AnalysisAction.HoldContact,
            AnalysisSafetyRules.AnalysisAction.CheckContext -> baselineDeferred.await().withSwarmEvidence(review)

            AnalysisSafetyRules.AnalysisAction.ReplyToCounterpart,
            AnalysisSafetyRules.AnalysisAction.ModelDraft -> {
                val draftDeferred = async { draftProvider.analyze(input) }
                val baseline = baselineDeferred.await()
                val rawDraft = runCatching { draftDeferred.await() }.getOrElse {
                    if (it is CancellationException) throw it
                    return@supervisorScope baseline.copy(
                        evidence = AnalysisSafetyRules.appendEvidence(
                            baseline.evidence,
                            "모델 응답 실패: 안전 정리로 대체했습니다.",
                        ),
                    ).withSwarmEvidence(review)
                }
                val sanitizedDraft = AnalysisSafetyRules.sanitizeReport(rawDraft, input)
                val selected = selectReport(
                    rawDraft = rawDraft,
                    sanitizedDraft = sanitizedDraft,
                    baseline = baseline,
                )
                AnalysisSafetyRules.finalizeReport(selected, input).withSwarmEvidence(review)
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
        review: SwarmReview,
    ): AnalysisReport {
        return copy(
            evidence = AnalysisSafetyRules.appendEvidence(
                evidence,
                "로컬 병렬 검수: 안전 ${review.safety}, 마지막 ${review.lastMessage}, 맥락 ${review.context}.",
            ),
        )
    }

    private fun AnalysisSafetyRules.AnalysisDecision.safetyReviewLabel(): String {
        return when {
            needsPerspectiveSetup -> "이름 필요"
            requiresHold -> "보류"
            hasWeakContext -> "맥락 확인"
            else -> "통과"
        }
    }

    private fun AnalysisInput.lastMessageReviewLabel(): String {
        val role = perspectiveValue("마지막 메시지 발신자 역할:").ifBlank { "알 수 없음" }
        val run = perspectiveValue("마지막 연속 발화 역할:")
        return if (run.isBlank()) role else "$role, $run"
    }

    private fun AnalysisInput.contextReviewLabel(): String {
        return if (AnalysisSafetyRules.hasWeakReunionContext(this)) "부족" else "충분"
    }

    private fun AnalysisInput.perspectiveValue(prefix: String): String {
        return perspectiveSummary.lineSequence()
            .firstOrNull { line -> line.trim().startsWith(prefix) }
            ?.substringAfter(prefix)
            ?.trim()
            .orEmpty()
    }

    private data class SwarmReview(
        val safety: String,
        val lastMessage: String,
        val context: String,
    )
}
