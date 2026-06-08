package com.bssm.reunionmanager.data.analysis

import com.bssm.reunionmanager.domain.model.AnalysisInput
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAnalysisProviderTest {
    @Test
    fun analyze_recommendsWaitingWhenBoundarySignalsExist() = runTest {
        val provider = FakeAnalysisProvider()

        val result = provider.analyze(
            inputWithSignals(
                recentExcerpt = "Minji: 부담되니까 조금 천천히 얘기하자",
                signalExcerpt = "Minji: 부담되니까 조금 천천히 얘기하자",
                statsSummary = "마지막 메시지: 부담되니까 조금 천천히 얘기하자\n6시간 이상 긴 공백: 1회",
                perspectiveSummary = "내 카톡 이름: Alex\n상대 후보: Minji\n마지막 메시지 발신자 역할: 상대\n내 마지막 연속 발화: 0개\n상대 마지막 연속 발화: 1개",
            ),
        )

        assertTrue(result.headline.contains("보내지 않기"))
        assertTrue(result.contactReadiness.contains("보류"))
        assertTrue(result.evidence.contains("마지막 메시지"))
        assertTrue(result.messageDraft.contains("보내지 않습니다"))
        assertTrue(result.caution.contains("경계"))
    }

    @Test
    fun analyze_doesNotDraftWhenBoundarySignalsExistAndPerspectiveIsMissing() = runTest {
        val result = FakeAnalysisProvider().analyze(
            inputWithSignals(
                recentExcerpt = "Minji: 이제 연락하지 말아줘",
                signalExcerpt = "Minji: 이제 연락하지 말아줘",
                statsSummary = "마지막 메시지: 이제 연락하지 말아줘\n마지막 발신자의 연속 발화: 1개",
            ),
        )

        assertTrue(result.contactReadiness.contains("정보 부족"))
        assertTrue(result.messageDraft.contains("내 카톡 이름"))
        assertFalse(result.messageDraft.contains("오랜만이야"))
    }

    @Test
    fun analyze_recommendsWaitingWhenFinalSenderRepeatedlyTalkedWithoutReply() = runTest {
        val result = FakeAnalysisProvider().analyze(
            inputWithSignals(
                recentExcerpt = "Alex: 혹시 시간 될까\nAlex: 왜 답이 없을까\nAlex: 미안해. 다시 연락할게",
                signalExcerpt = "Alex: 미안해. 다시 연락할게",
                statsSummary = "마지막 메시지: 미안해. 다시 연락할게\n마지막 발신자의 연속 발화: 4개\n6시간 이상 긴 공백: 2회",
                perspectiveSummary = "내 카톡 이름: Alex\n상대 후보: Minji\n마지막 메시지 발신자 역할: 나\n마지막 연속 발화 역할: 나 4개\n내 마지막 연속 발화: 4개\n상대 마지막 연속 발화: 0개",
            ),
        )

        assertTrue(result.contactReadiness.contains("보류"))
        assertTrue(result.evidence.contains("연속 발화"))
        assertTrue(result.messageDraft.contains("보내지 않습니다"))
        assertTrue(result.alternativeDrafts.contains("오늘은 보내지 않기"))
    }

    @Test
    fun analyze_treatsCounterpartFinalMessagesAsReplyOpportunity() = runTest {
        val result = FakeAnalysisProvider().analyze(
            inputWithSignals(
                recentExcerpt = "Minji: 잘 지내?\nMinji: 괜찮으면 안부만 묻고 싶었어",
                signalExcerpt = "Minji: 괜찮으면 안부만 묻고 싶었어",
                statsSummary = "마지막 메시지: 괜찮으면 안부만 묻고 싶었어\n마지막 발신자의 연속 발화: 2개",
                perspectiveSummary = "내 카톡 이름: Alex\n상대 후보: Minji\n마지막 메시지 발신자 역할: 상대\n마지막 연속 발화 역할: 상대 2개\n내 최근 메시지: 잘 지냈어?\n상대 최근 메시지: 괜찮으면 안부만 묻고 싶었어\n내 마지막 연속 발화: 0개\n상대 마지막 연속 발화: 2개",
            ),
        )

        assertTrue(result.contactReadiness.contains("가볍게"))
        assertTrue(result.relationshipSummary.contains("괜찮으면 안부만 묻고 싶었어"))
        assertTrue(result.evidence.contains("상대 최근 메시지"))
        assertTrue(result.nextStep.contains("상대의 마지막 메시지"))
        assertTrue(result.messageDraft.contains("메시지 봤어"))
        assertFalse(result.messageDraft.contains("답이 늦었네"))
    }

    @Test
    fun analyze_doesNotTreatNegativeOkayPhraseAsPositiveSignal() = runTest {
        val result = FakeAnalysisProvider().analyze(
            inputWithSignals(
                recentExcerpt = "Minji: 지금은 괜찮지 않아\nMinji: 연락은 조금 불편해",
                signalExcerpt = "Minji: 지금은 괜찮지 않아\nMinji: 연락은 조금 불편해",
                statsSummary = "마지막 메시지: 연락은 조금 불편해\n마지막 발신자의 연속 발화: 2개",
                perspectiveSummary = "내 카톡 이름: Alex\n상대 후보: Minji\n마지막 메시지 발신자 역할: 상대\n상대 마지막 연속 발화: 2개",
            ),
        )

        assertTrue(result.contactReadiness.contains("보류"))
        assertTrue(result.messageDraft.contains("보내지 않습니다"))
    }

    @Test
    fun analyze_recommendsWaitingWhenCounterpartHasMovedOn() = runTest {
        val result = FakeAnalysisProvider().analyze(
            inputWithSignals(
                recentExcerpt = "Minji: 나 새로 만나는 사람 있어\nMinji: 우리도 이제 각자 잘 지내자",
                signalExcerpt = "Minji: 나 새로 만나는 사람 있어\nMinji: 우리도 이제 각자 잘 지내자",
                statsSummary = "마지막 메시지: 우리도 이제 각자 잘 지내자\n마지막 발신자의 연속 발화: 2개",
                perspectiveSummary = "내 카톡 이름: Alex\n상대 후보: Minji\n마지막 메시지 발신자 역할: 상대\n상대 마지막 연속 발화: 2개",
            ),
        )

        assertTrue(result.contactReadiness.contains("보류"))
        assertTrue(result.messageDraft.contains("보내지 않습니다"))
        assertFalse(result.messageDraft.contains("안부"))
    }

    @Test
    fun analyze_doesNotTreatNoPressurePhraseAsBoundarySignal() = runTest {
        val result = FakeAnalysisProvider().analyze(
            inputWithSignals(
                recentExcerpt = "Minji: 부담 없으면 천천히 답해도 돼\nMinji: 괜찮다면 안부만 묻고 싶었어",
                signalExcerpt = "Minji: 부담 없으면 천천히 답해도 돼",
                statsSummary = "마지막 메시지: 괜찮다면 안부만 묻고 싶었어\n마지막 발신자의 연속 발화: 2개",
                perspectiveSummary = "내 카톡 이름: Alex\n상대 후보: Minji\n마지막 메시지 발신자 역할: 상대\n상대 마지막 연속 발화: 2개",
            ),
        )

        assertFalse(result.contactReadiness.contains("보류"))
        assertTrue(result.messageDraft.contains("메시지 봤어"))
        assertFalse(result.messageDraft.contains("답이 늦었네"))
    }

    @Test
    fun analyze_treatsExplicitPermissionAsReplyOpportunity() = runTest {
        val result = FakeAnalysisProvider().analyze(
            inputWithSignals(
                recentExcerpt = "Alex: 괜찮다면 짧게 이야기할 수 있을까?\nMinji: 연락해도 돼",
                signalExcerpt = "Minji: 연락해도 돼",
                statsSummary = "마지막 메시지: 연락해도 돼\n마지막 발신자의 연속 발화: 1개",
                perspectiveSummary = "내 카톡 이름: Alex\n상대 후보: Minji\n마지막 메시지 발신자 역할: 상대\n마지막 연속 발화 역할: 상대 1개\n내 최근 메시지: 괜찮다면 짧게 이야기할 수 있을까?\n상대 최근 메시지: 연락해도 돼\n내 마지막 연속 발화: 0개\n상대 마지막 연속 발화: 1개",
            ),
        )

        assertTrue(result.contactReadiness.contains("가볍게"))
        assertTrue(result.messageDraft.contains("고마워"))
        assertTrue(result.messageDraft.contains("부담 없이"))
        assertFalse(result.messageDraft.contains("오랜만이야"))
    }

    @Test
    fun analyze_mentionsLateReplyOnlyWhenExportShowsDelay() = runTest {
        val result = FakeAnalysisProvider().analyze(
            inputWithSignals(
                recentExcerpt = "Minji: 잘 지내?",
                signalExcerpt = "Minji: 잘 지내?",
                statsSummary = "마지막 메시지: 잘 지내?\n마지막 메시지 이후 경과: 2일 0시간",
                perspectiveSummary = "내 카톡 이름: Alex\n상대 후보: Minji\n마지막 메시지 발신자 역할: 상대\n상대 마지막 연속 발화: 1개",
            ),
        )

        assertTrue(result.messageDraft.contains("답이 늦었네"))
        assertTrue(result.messageDraft.contains("나는 잘 지내고 있어"))
    }

    @Test
    fun analyze_recommendsApologyWhenApologySignalsExist() = runTest {
        val result = FakeAnalysisProvider().analyze(
            inputWithSignals(
                recentExcerpt = "Alex: 내가 너무 세게 말했어\nMinji: 그때 많이 힘들었어",
                signalExcerpt = "Alex: 미안해. 내가 사과해야 할 것 같아",
                statsSummary = "마지막 메시지: 그때 많이 힘들었어\n마지막 발신자의 연속 발화: 1개\n감정/경계 신호 메시지: 2개",
                perspectiveSummary = "내 카톡 이름: Alex\n상대 후보: Minji\n마지막 메시지 발신자 역할: 상대\n내 마지막 연속 발화: 0개\n상대 마지막 연속 발화: 1개",
            ),
        )

        assertTrue(result.contactReadiness.contains("사과"))
        assertTrue(result.reunionObjective.contains("사과"))
        assertTrue(result.messageDraft.contains("미안"))
    }

    @Test
    fun analyze_allowsLightContactWhenPositiveSignalsExist() = runTest {
        val result = FakeAnalysisProvider().analyze(
            inputWithSignals(
                recentExcerpt = "Minji: 나도 가끔 생각났어\nAlex: 잘 지내고 있어서 다행이야",
                signalExcerpt = "Minji: 나도 가끔 생각났어",
                statsSummary = "마지막 메시지: 잘 지내고 있어서 다행이야\n감정/경계 신호 메시지: 1개",
                perspectiveSummary = "내 카톡 이름: Alex\n상대 후보: Minji\n마지막 메시지 발신자 역할: 나\n내 마지막 연속 발화: 1개\n상대 마지막 연속 발화: 0개",
            ),
        )

        assertTrue(result.contactReadiness.contains("가볍게"))
        assertTrue(result.messageDraft.contains("안부"))
        assertTrue(result.alternativeDrafts.lines().size >= 3)
    }

    @Test
    fun analyze_usesSofterDraftWhenLastGapIsVeryLong() = runTest {
        val result = FakeAnalysisProvider().analyze(
            inputWithSignals(
                recentExcerpt = "Minji: 나도 가끔 생각났어\nAlex: 나도 잘 지내는지 궁금했어",
                signalExcerpt = "Minji: 나도 가끔 생각났어",
                statsSummary = "마지막 메시지: 나도 잘 지내는지 궁금했어\n마지막 메시지 전 공백: 45일 0시간\n감정/경계 신호 메시지: 1개",
                perspectiveSummary = "내 카톡 이름: Alex\n상대 후보: Minji\n마지막 메시지 발신자 역할: 나\n내 마지막 연속 발화: 1개\n상대 마지막 연속 발화: 0개",
            ),
        )

        assertTrue(result.contactReadiness.contains("가볍게"))
        assertTrue(result.nextStep.contains("오래 끊긴 대화"))
        assertTrue(result.messageDraft.contains("잘 지내는지만"))
    }

    @Test
    fun analyze_requiresUserNameBeforeDraftingWhenPerspectiveIsMissing() = runTest {
        val result = FakeAnalysisProvider().analyze(
            inputWithSignals(
                recentExcerpt = "Minji: 나도 가끔 생각났어\nAlex: 잘 지내고 있어서 다행이야",
                signalExcerpt = "Minji: 나도 가끔 생각났어",
                statsSummary = "마지막 메시지: 잘 지내고 있어서 다행이야\n감정/경계 신호 메시지: 1개",
            ),
        )

        assertTrue(result.headline.contains("내 이름"))
        assertTrue(result.contactReadiness.contains("정보 부족"))
        assertTrue(result.relationshipSummary.contains("내 카톡 이름"))
        assertTrue(result.messageDraft.contains("보낼 문장을 만들지 않습니다"))
        assertTrue(result.messageDraft.contains("내 카톡 이름"))
        assertTrue(result.alternativeDrafts.contains("내 카톡 이름 저장하기"))
    }

    @Test
    fun analyze_returnsInfoOnlyWhenChatLooksTechnicalOrGroupWithoutPersonalSignals() = runTest {
        val result = FakeAnalysisProvider().analyze(
            inputWithSignals(
                participantNames = listOf("Alex", "Minji", "Juno"),
                recentExcerpt = "Juno: RAG 테스트 결과 공유할게\nMinji: LLM 모델 API 응답이 느려",
                signalExcerpt = "",
                statsSummary = "마지막 메시지: LLM 모델 API 응답이 느려\n마지막 발신자의 연속 발화: 1개",
                perspectiveSummary = "내 카톡 이름: Alex\n상대 후보: Minji, Juno\n마지막 메시지 발신자 역할: 상대\n상대 최근 메시지: LLM 모델 API 응답이 느려\n내 마지막 연속 발화: 0개\n상대 마지막 연속 발화: 1개",
            ),
        )

        assertTrue(result.contactReadiness.contains("정보 부족"))
        assertTrue(result.headline.contains("관계 맥락"))
        assertTrue(result.messageDraft.contains("보낼 문장을 만들지 않습니다"))
        assertTrue(result.alternativeDrafts.contains("더 관련 있는 대화 파일"))
        assertTrue(result.caution.contains("업무"))
    }

    @Test
    fun analyze_confirmsConcreteMeetingPlanWhenCounterpartSuggestedIt() = runTest {
        val result = FakeAnalysisProvider().analyze(
            inputWithSignals(
                recentExcerpt = "Alex: 괜찮다면 짧게 얼굴 볼 수 있을까?\nMinji: 내일 7시에 카페에서 보자",
                signalExcerpt = "Minji: 내일 7시에 카페에서 보자",
                statsSummary = "마지막 메시지: 내일 7시에 카페에서 보자\n마지막 발신자의 연속 발화: 1개",
                perspectiveSummary = "내 카톡 이름: Alex\n상대 후보: Minji\n마지막 메시지 발신자 역할: 상대\n마지막 연속 발화 역할: 상대 1개\n내 최근 메시지: 괜찮다면 짧게 얼굴 볼 수 있을까?\n상대 최근 메시지: 내일 7시에 카페에서 보자\n내 마지막 연속 발화: 0개\n상대 마지막 연속 발화: 1개",
            ),
        )

        assertTrue(result.contactReadiness.contains("가볍게"))
        assertTrue(result.messageDraft.contains("약속한 시간"))
        assertTrue(result.messageDraft.contains("고마워"))
        assertTrue(result.alternativeDrafts.contains("그때 보자"))
        assertFalse(result.messageDraft.contains("안부부터"))
    }

    @Test
    fun analyze_answersScheduleQuestionWithoutPretendingItIsConfirmed() = runTest {
        val result = FakeAnalysisProvider().analyze(
            inputWithSignals(
                recentExcerpt = "Alex: 괜찮다면 짧게 얼굴 볼 수 있을까?\nMinji: 토요일 저녁에 시간 돼?",
                signalExcerpt = "Minji: 토요일 저녁에 시간 돼?",
                statsSummary = "마지막 메시지: 토요일 저녁에 시간 돼?\n마지막 발신자의 연속 발화: 1개",
                perspectiveSummary = "내 카톡 이름: Alex\n상대 후보: Minji\n마지막 메시지 발신자 역할: 상대\n마지막 연속 발화 역할: 상대 1개\n내 최근 메시지: 괜찮다면 짧게 얼굴 볼 수 있을까?\n상대 최근 메시지: 토요일 저녁에 시간 돼?\n내 마지막 연속 발화: 0개\n상대 마지막 연속 발화: 1개",
            ),
        )

        assertTrue(result.contactReadiness.contains("가볍게"))
        assertTrue(result.messageDraft.contains("가능한지 확인"))
        assertFalse(result.messageDraft.contains("약속한 시간"))
        assertFalse(result.messageDraft.contains("안부부터"))
    }

    private fun inputWithSignals(
        recentExcerpt: String,
        signalExcerpt: String,
        statsSummary: String,
        perspectiveSummary: String = "내 카톡 이름: 설정되지 않음\n마지막 메시지 발신자 역할: 알 수 없음\n마지막 연속 발화 역할: 알 수 없음 1개",
        participantNames: List<String> = listOf("Alex", "Minji"),
    ): AnalysisInput {
        return AnalysisInput(
            conversationTitle = "Spring meetup",
            participantNames = participantNames,
            messageCount = 12,
            excerpt = "Alex: hello\n$signalExcerpt\n$recentExcerpt",
            recentExcerpt = recentExcerpt,
            signalExcerpt = signalExcerpt,
            statsSummary = statsSummary,
            perspectiveSummary = perspectiveSummary,
        )
    }
}
