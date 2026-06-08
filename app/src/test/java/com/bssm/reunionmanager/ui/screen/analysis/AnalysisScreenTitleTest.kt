package com.bssm.reunionmanager.ui.screen.analysis

import com.bssm.reunionmanager.domain.model.AnalysisReport
import com.bssm.reunionmanager.ui.theme.ReunionBadgeTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisScreenTitleTest {
    @Test
    fun messageSectionTitle_returnsNoSendTitleWhenContactIsOnHold() {
        val report = report(contactReadiness = "지금은 보류")

        assertEquals("보내지 않기", report.messageSectionTitle())
    }

    @Test
    fun messageSectionTitle_returnsCheckTitleWhenInformationRequiresSetup() {
        val report = report(
            headline = "내 이름 확인",
            contactReadiness = "정보 부족",
            messageDraft = "지금은 보낼 문장을 만들지 않습니다. 내 카톡 이름을 먼저 저장하세요.",
            alternativeDrafts = "내 카톡 이름 저장하기\n같은 대화 다시 분석하기\n최근 대화 파일인지 확인하기",
        )

        assertEquals("확인할 일", report.messageSectionTitle())
    }

    @Test
    fun messageSectionTitle_returnsCheckTitleWhenInformationIsInsufficientEvenIfDraftExists() {
        val report = report(contactReadiness = "정보 부족")

        assertEquals("확인할 일", report.messageSectionTitle())
    }

    @Test
    fun messageSectionTitle_returnsReplyTitleWhenCounterpartIsWaiting() {
        val report = report(
            reunionObjective = "새 연락을 시작하기보다 상대가 남긴 말에 답하는 것이 목표입니다.",
            nextStep = "상대의 마지막 메시지에 바로 답하세요.",
        )

        assertEquals("답장 문장", report.messageSectionTitle())
    }

    @Test
    fun messageSectionTitle_returnsFirstContactTitleByDefault() {
        val report = report()

        assertEquals("첫 연락 문장", report.messageSectionTitle())
    }

    @Test
    fun alternativeSectionTitle_returnsActionTitleWhenContactIsOnHold() {
        val report = report(contactReadiness = "지금은 보류")

        assertEquals("다음 선택지", report.alternativeSectionTitle())
    }

    @Test
    fun alternativeSectionTitle_returnsActionTitleWhenInformationRequiresSetup() {
        val report = report(
            headline = "내 이름 확인",
            contactReadiness = "정보 부족",
            messageDraft = "지금은 보낼 문장을 만들지 않습니다. 내 카톡 이름을 먼저 저장하세요.",
            alternativeDrafts = "내 카톡 이름 저장하기\n같은 대화 다시 분석하기\n최근 대화 파일인지 확인하기",
        )

        assertEquals("다음 선택지", report.alternativeSectionTitle())
    }

    @Test
    fun alternativeSectionTitle_returnsActionTitleWhenInformationIsInsufficientEvenIfDraftExists() {
        val report = report(contactReadiness = "정보 부족")

        assertEquals("다음 선택지", report.alternativeSectionTitle())
    }

    @Test
    fun alternativeSectionTitle_returnsReplyCandidateTitleWhenCounterpartIsWaiting() {
        val report = report(
            reunionObjective = "새 연락을 시작하기보다 상대가 남긴 말에 답하는 것이 목표입니다.",
            nextStep = "상대의 마지막 메시지에 바로 답하세요.",
        )

        assertEquals("다른 답장 후보", report.alternativeSectionTitle())
    }

    @Test
    fun alternativeSectionTitle_returnsFirstContactCandidateTitleByDefault() {
        val report = report()

        assertEquals("다른 문장 후보", report.alternativeSectionTitle())
    }

    @Test
    fun readinessTone_mapsJudgmentToQuietUiTones() {
        assertEquals(ReunionBadgeTone.Error, report(contactReadiness = "지금은 보류").readinessTone())
        assertEquals(ReunionBadgeTone.Success, report(contactReadiness = "아주 가볍게 가능").readinessTone())
        assertEquals(ReunionBadgeTone.Accent, report(contactReadiness = "먼저 사과 필요").readinessTone())
        assertEquals(ReunionBadgeTone.Neutral, report(contactReadiness = "정보 부족").readinessTone())
    }

    @Test
    fun conclusionHeadline_summarizesActionWithoutExtraDetail() {
        assertEquals(
            "오늘은 보내지 않는 쪽이 안전합니다.",
            report(contactReadiness = "지금은 보류").conclusionHeadline(),
        )
        assertEquals(
            "먼저 확인할 정보가 있습니다.",
            report(contactReadiness = "정보 부족").conclusionHeadline(),
        )
        assertEquals(
            "새 연락보다 짧은 답장이 자연스럽습니다.",
            report(
                reunionObjective = "새 연락보다 상대가 남긴 말에 답하는 것이 목표입니다.",
                nextStep = "상대의 마지막 메시지에 바로 답하세요.",
            ).conclusionHeadline(),
        )
        assertEquals(
            "짧고 부담 없는 한 문장만 준비하세요.",
            report().conclusionHeadline(),
        )
    }

    @Test
    fun conclusionHeadline_prioritizesApologyEvenForCounterpartReply() {
        val report = report(
            contactReadiness = "먼저 사과 필요",
            reunionObjective = "상대가 남긴 말에 변명 없이 짧게 인정하는 것이 목표입니다.",
            nextStep = "상대의 마지막 메시지에 답하되 먼저 미안하다고만 전하세요.",
        )

        assertEquals("재회보다 짧은 인정이 먼저입니다.", report.conclusionHeadline())
    }

    @Test
    fun summaryTitle_usesSpecificHeadline() {
        val report = report(headline = "상대 답장에 짧게 응답")

        assertEquals("상대 답장에 짧게 응답", report.summaryTitle())
    }

    @Test
    fun summaryBody_includesSummaryAndGoalWithoutGrowingTooLong() {
        val report = report(
            relationshipSummary = "상대가 마지막에 답장을 남긴 상태라 새 연락보다 짧은 답장이 자연스럽습니다.",
            reunionObjective = "상대의 속도를 존중하면서 안부만 확인합니다.",
        )

        val body = report.summaryBody()

        assertEquals(
            "상대가 마지막에 답장을 남긴 상태라 새 연락보다 짧은 답장이 자연스럽습니다.\n목표: 상대의 속도를 존중하면서 안부만 확인합니다.",
            body,
        )
    }

    @Test
    fun summaryBody_limitsVeryLongText() {
        val report = report(
            relationshipSummary = "가".repeat(200),
            reunionObjective = "나".repeat(200),
        )

        assertEquals(180, report.summaryBody().length)
    }

    @Test
    fun evidenceBody_keepsOnlyThreeShortLines() {
        val report = report(
            evidence = "첫 줄\n둘째 줄\n셋째 줄\n넷째 줄",
        )

        assertEquals("첫 줄\n둘째 줄\n셋째 줄", report.evidenceBody())
    }

    @Test
    fun canCopyMessageDraft_returnsTrueOnlyForActualDrafts() {
        assertTrue(report().canCopyMessageDraft())
        assertTrue(
            report(
                reunionObjective = "새 연락보다 상대가 남긴 말에 답하는 것이 목표입니다.",
                nextStep = "상대의 마지막 메시지에 바로 답하세요.",
            ).canCopyMessageDraft(),
        )
        assertFalse(report(contactReadiness = "지금은 보류").canCopyMessageDraft())
        assertFalse(
            report(
                headline = "내 이름 확인",
                contactReadiness = "정보 부족",
                messageDraft = "지금은 보낼 문장을 만들지 않습니다. 내 카톡 이름을 먼저 저장하세요.",
                alternativeDrafts = "내 카톡 이름 저장하기\n같은 대화 다시 분석하기\n최근 대화 파일인지 확인하기",
            ).canCopyMessageDraft(),
        )
    }

    @Test
    fun copySafetyNote_usesOneShortLine() {
        val report = report(
            caution = "답을 재촉하지 마세요.\n상대가 답하지 않으면 기다리세요.",
        )

        assertEquals("답을 재촉하지 마세요.", report.copySafetyNote())
    }

    @Test
    fun copySafetyNote_limitsLongText() {
        val note = report(caution = "가".repeat(120)).copySafetyNote()

        assertEquals(44, note.length)
        assertTrue(note.endsWith("…"))
    }

    @Test
    fun copyPromptText_switchesToPostCopyGuardrail() {
        assertEquals(
            "보내기 전: 답을 재촉하지 마세요.",
            copyPromptText(copied = false, safetyNote = "답을 재촉하지 마세요."),
        )
        assertEquals(
            "복사 후: 답장이 없어도 추가로 보내지 마세요.",
            copyPromptText(copied = true, safetyNote = "답을 재촉하지 마세요."),
        )
    }

    @Test
    fun analysisGenerateButtonText_separatesInitialDraftFromRegeneration() {
        assertEquals(
            "다음 행동 정리하기",
            analysisGenerateButtonText(providerConfigured = true, hasReport = false),
        )
        assertEquals(
            "데모로 정리하기",
            analysisGenerateButtonText(providerConfigured = false, hasReport = false),
        )
        assertEquals(
            "다시 정리하기",
            analysisGenerateButtonText(providerConfigured = true, hasReport = true),
        )
        assertEquals(
            "다시 정리하기",
            analysisGenerateButtonText(providerConfigured = false, hasReport = true),
        )
    }

    private fun report(
        headline: String = "테스트",
        contactReadiness: String = "아주 가볍게 가능",
        evidence: String = "테스트 근거",
        relationshipSummary: String = "테스트 요약",
        reunionObjective: String = "가벼운 안부로 반응 가능성만 확인합니다.",
        nextStep: String = "짧은 한 문장만 준비하세요.",
        messageDraft: String = "오랜만이야. 잘 지내?",
        alternativeDrafts: String = "오랜만이야\n잘 지내?\n답은 천천히 해도 괜찮아",
        caution: String = "답을 재촉하지 마세요.",
    ): AnalysisReport {
        return AnalysisReport(
            headline = headline,
            contactReadiness = contactReadiness,
            evidence = evidence,
            relationshipSummary = relationshipSummary,
            reunionObjective = reunionObjective,
            nextStep = nextStep,
            messageDraft = messageDraft,
            alternativeDrafts = alternativeDrafts,
            caution = caution,
        )
    }
}
