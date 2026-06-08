package com.bssm.reunionmanager.data.analysis

import com.bssm.reunionmanager.domain.analysis.AnalysisProvider
import com.bssm.reunionmanager.domain.model.AnalysisInput
import com.bssm.reunionmanager.domain.model.AnalysisReport
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAnalysisSwarmProviderTest {
    @Test
    fun analyze_usesBaselineWithoutCallingDraftWhenPerspectiveSetupIsRequired() = runTest {
        val draftProvider = CountingProvider(optimisticDraft)
        val provider = LocalAnalysisSwarmProvider(
            draftProvider = draftProvider,
            baselineProvider = StaticProvider(optimisticDraft),
        )

        val result = provider.analyze(input(perspectiveSummary = missingPerspective))

        assertEquals(0, draftProvider.callCount)
        assertEquals("정보 부족", result.contactReadiness)
        assertTrue(result.headline.contains("내 이름"))
        assertTrue(result.messageDraft.contains("내 카톡 이름"))
        assertTrue(result.evidence.contains("로컬 병렬 검수"))
    }

    @Test
    fun analyze_usesBaselineWithoutCallingDraftWhenCounterpartContactDistressRequiresHold() = runTest {
        val draftProvider = CountingProvider(optimisticDraft)
        val provider = LocalAnalysisSwarmProvider(
            draftProvider = draftProvider,
            baselineProvider = StaticProvider(optimisticDraft),
        )

        val result = provider.analyze(
            input(
                recentExcerpt = "현우: 잠깐 이야기할 수 있을까?\n민지: 네 연락 때문에 힘들어",
                signalExcerpt = "민지: 네 연락 때문에 힘들어",
                perspectiveSummary = configuredPerspective(lastSenderRole = "상대", counterpartFinalRun = 1),
            ),
        )

        assertEquals(0, draftProvider.callCount)
        assertEquals("지금은 보류", result.contactReadiness)
        assertTrue(result.messageDraft.contains("보내지 않습니다"))
        assertTrue(result.evidence.contains("로컬 병렬 검수"))
    }

    @Test
    fun analyze_usesBaselineWithoutCallingDraftWhenUserPromisedNoMoreContact() = runTest {
        val draftProvider = CountingProvider(optimisticDraft)
        val provider = LocalAnalysisSwarmProvider(
            draftProvider = draftProvider,
            baselineProvider = StaticProvider(optimisticDraft),
        )

        val result = provider.analyze(
            input(
                recentExcerpt = "민지: 잘 지내?\n현우: 이제 연락 안 할게",
                signalExcerpt = "현우: 이제 연락 안 할게",
                perspectiveSummary = configuredPerspective(lastSenderRole = "나", myFinalRun = 1),
            ),
        )

        assertEquals(0, draftProvider.callCount)
        assertEquals("지금은 보류", result.contactReadiness)
        assertTrue(result.evidence.contains("자제 약속"))
        assertTrue(result.messageDraft.contains("보내지 않습니다"))
    }

    @Test
    fun analyze_replacesInvalidDraftReadinessWithBaselineReport() = runTest {
        val provider = LocalAnalysisSwarmProvider(
            draftProvider = StaticProvider(
                optimisticDraft.copy(
                    contactReadiness = "적극 연락 가능",
                    messageDraft = "지금 당장 길게 설명하고 답을 받아야 해.",
                    alternativeDrafts = "바로 전화해\n길게 설명해\n집 앞에 찾아가",
                ),
            ),
            baselineProvider = StaticProvider(
                optimisticDraft.copy(
                    contactReadiness = "아주 가볍게 가능",
                    messageDraft = "오랜만이야. 괜찮다면 짧게 안부만 묻고 싶어.",
                ),
            ),
        )

        val result = provider.analyze(input())

        assertEquals("아주 가볍게 가능", result.contactReadiness)
        assertFalse(result.messageDraft.contains("당장"))
        assertFalse(result.alternativeDrafts.contains("집 앞"))
        assertTrue(result.evidence.contains("허용되지 않은 연락 판단"))
    }

    @Test
    fun analyze_rewritesGenericDraftWhenCounterpartIsWaiting() = runTest {
        val provider = LocalAnalysisSwarmProvider(
            draftProvider = StaticProvider(
                optimisticDraft.copy(
                    messageDraft = "오랜만이야. 잘 지냈어?",
                    alternativeDrafts = "오랜만이야\n잘 지내?\n잠깐 얘기할 수 있어?",
                ),
            ),
            baselineProvider = StaticProvider(optimisticDraft),
        )

        val result = provider.analyze(
            input(
                recentExcerpt = "민지: 잘 지내?",
                signalExcerpt = "민지: 잘 지내?",
                perspectiveSummary = configuredPerspective(lastSenderRole = "상대", counterpartFinalRun = 1),
            ),
        )

        assertEquals("아주 가볍게 가능", result.contactReadiness)
        assertTrue(result.messageDraft.contains("메시지 봤어"))
        assertTrue(result.messageDraft.contains("나는 잘 지내고 있어"))
        assertFalse(result.messageDraft.contains("오랜만이야"))
    }

    @Test
    fun analyze_rewritesPermissionSignalAsLowPressureReply() = runTest {
        val provider = LocalAnalysisSwarmProvider(
            draftProvider = StaticProvider(
                optimisticDraft.copy(
                    messageDraft = "오랜만이야. 잘 지냈어?",
                    alternativeDrafts = "오랜만이야\n잘 지내?\n잠깐 얘기할 수 있어?",
                ),
            ),
            baselineProvider = StaticProvider(optimisticDraft),
        )

        val result = provider.analyze(
            input(
                recentExcerpt = "현우: 괜찮다면 짧게 이야기할 수 있을까?\n민지: 연락해도 돼",
                signalExcerpt = "민지: 연락해도 돼",
                perspectiveSummary = configuredPerspective(lastSenderRole = "상대", counterpartFinalRun = 1),
            ),
        )

        assertEquals("아주 가볍게 가능", result.contactReadiness)
        assertTrue(result.messageDraft.contains("고마워"))
        assertTrue(result.messageDraft.contains("부담 없이"))
        assertFalse(result.messageDraft.contains("오랜만이야"))
        assertTrue(result.evidence.contains("로컬 병렬 검수"))
    }

    @Test
    fun analyze_addsConcreteParallelReviewEvidence() = runTest {
        val provider = LocalAnalysisSwarmProvider(
            draftProvider = StaticProvider(optimisticDraft),
            baselineProvider = StaticProvider(optimisticDraft),
        )

        val result = provider.analyze(input())

        assertTrue(result.evidence.contains("로컬 병렬 검수"))
        assertTrue(result.evidence.contains("안전 통과"))
        assertTrue(result.evidence.contains("마지막 나"))
        assertTrue(result.evidence.contains("맥락 충분"))
    }

    private class StaticProvider(
        private val report: AnalysisReport,
    ) : AnalysisProvider {
        override suspend fun analyze(input: AnalysisInput): AnalysisReport = report
    }

    private class CountingProvider(
        private val report: AnalysisReport,
    ) : AnalysisProvider {
        var callCount: Int = 0
            private set

        override suspend fun analyze(input: AnalysisInput): AnalysisReport {
            callCount += 1
            return report
        }
    }

    private companion object {
        val optimisticDraft = AnalysisReport(
            headline = "가벼운 안부",
            contactReadiness = "아주 가볍게 가능",
            evidence = "상대가 대화를 닫지 않았습니다.",
            relationshipSummary = "최근 대화는 짧은 안부 정도만 가능한 흐름입니다.",
            reunionObjective = "부담 없이 현재 온도만 확인합니다.",
            nextStep = "짧은 한 문장만 준비하세요.",
            messageDraft = "오랜만이야. 괜찮다면 짧게 안부만 묻고 싶어.",
            alternativeDrafts = "오랜만이야. 잘 지내?\n괜찮다면 안부만 묻고 싶어.\n답은 천천히 해도 괜찮아.",
            caution = "답을 재촉하지 마세요.",
        )

        const val missingPerspective = "내 카톡 이름: 설정되지 않음\n마지막 메시지 발신자 역할: 알 수 없음\n관점 주의: 내 카톡 이름이 설정되지 않아 마지막 발신자가 사용자인지 상대인지 확정할 수 없습니다."

        fun configuredPerspective(
            lastSenderRole: String = "나",
            myFinalRun: Int = if (lastSenderRole == "나") 1 else 0,
            counterpartFinalRun: Int = if (lastSenderRole == "상대") 1 else 0,
        ): String {
            return """
                내 카톡 이름: 현우
                상대 후보: 민지
                마지막 메시지 발신자 역할: $lastSenderRole
                마지막 연속 발화 역할: $lastSenderRole ${maxOf(myFinalRun, counterpartFinalRun)}개
                내 최근 메시지: 오랜만이야
                상대 최근 메시지: 잘 지내?
                내 마지막 연속 발화: ${myFinalRun}개
                상대 마지막 연속 발화: ${counterpartFinalRun}개
            """.trimIndent()
        }

        fun input(
            recentExcerpt: String = "민지: 나도 가끔 생각났어\n현우: 잘 지내는지 궁금했어",
            signalExcerpt: String = "민지: 나도 가끔 생각났어",
            perspectiveSummary: String = configuredPerspective(),
        ): AnalysisInput {
            return AnalysisInput(
                conversationTitle = "민지와의 대화",
                participantNames = listOf("민지", "현우"),
                messageCount = 8,
                excerpt = recentExcerpt,
                recentExcerpt = recentExcerpt,
                signalExcerpt = signalExcerpt,
                statsSummary = "마지막 메시지: 잘 지내는지 궁금했어\n마지막 발신자의 연속 발화: 1개\n마지막 메시지 이후 경과: 알 수 없음",
                perspectiveSummary = perspectiveSummary,
            )
        }
    }
}
