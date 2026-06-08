package com.bssm.reunionmanager.ui.screen.conversation

import com.bssm.reunionmanager.domain.model.AnalysisReport
import com.bssm.reunionmanager.domain.model.ConversationDetail
import com.bssm.reunionmanager.domain.model.ConversationMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationDetailScreenTest {
    @Test
    fun visibleMessagePreview_keepsOnlyRecentEightMessagesByDefault() {
        val messages = messages(count = 12)

        val preview = messages.visibleMessagePreview(showAllMessages = false)

        assertEquals((5L..12L).toList(), preview.map { it.id })
    }

    @Test
    fun visibleMessagePreview_keepsAllMessagesWhenExpanded() {
        val messages = messages(count = 12)

        val preview = messages.visibleMessagePreview(showAllMessages = true)

        assertEquals((1L..12L).toList(), preview.map { it.id })
    }

    @Test
    fun messagePreviewSummary_describesHiddenAndExpandedStates() {
        val messages = messages(count = 12)

        assertTrue(messages.hasHiddenMessages(showAllMessages = false))
        assertEquals(
            "최근 8개만 먼저 보기",
            messages.messagePreviewSummary(showAllMessages = false),
        )
        assertFalse(messages.hasHiddenMessages(showAllMessages = true))
        assertEquals(
            "전체 12개 메시지",
            messages.messagePreviewSummary(showAllMessages = true),
        )
    }

    @Test
    fun messagePreviewSummary_handlesShortConversationWithoutExtraAction() {
        val messages = messages(count = 3)

        assertFalse(messages.hasHiddenMessages(showAllMessages = false))
        assertEquals(
            "전체 3개 메시지",
            messages.messagePreviewSummary(showAllMessages = false),
        )
    }

    @Test
    fun analysisEntryButtonText_matchesWhetherResultAlreadyExists() {
        assertEquals(
            "다음 행동 정리하기",
            detail(latestAnalysis = null).analysisEntryButtonText(),
        )
        assertEquals(
            "정리 결과 보기",
            detail(latestAnalysis = report()).analysisEntryButtonText(),
        )
    }

    @Test
    fun detailHeadline_summarizesLatestAnalysisAsAction() {
        assertEquals(
            "오늘은 보내지 않는 쪽이 안전해요.",
            report(contactReadiness = "지금은 보류").detailHeadline(),
        )
        assertEquals(
            "먼저 확인할 정보가 있어요.",
            report(contactReadiness = "정보 부족").detailHeadline(),
        )
        assertEquals(
            "짧은 인정부터 준비해요.",
            report(contactReadiness = "먼저 사과 필요").detailHeadline(),
        )
        assertEquals(
            "짧게 답장하면 충분해요.",
            report(
                reunionObjective = "상대가 남긴 말에 답하는 것이 목표입니다.",
                nextStep = "상대의 마지막 메시지에 짧게 답하세요.",
            ).detailHeadline(),
        )
        assertEquals(
            "부담 없는 한 문장만 준비해요.",
            report().detailHeadline(),
        )
    }

    @Test
    fun detailNextAction_keepsLatestAnalysisCardShort() {
        assertEquals(
            "오늘은 보내지 말고 기다려요.",
            report(contactReadiness = "지금은 보류").detailNextAction(),
        )
        assertEquals(
            "내 이름과 최근 대화가 맞는지 확인하세요.",
            report(contactReadiness = "정보 부족").detailNextAction(),
        )
        assertEquals(
            52,
            report(nextStep = "가".repeat(120)).detailNextAction().length,
        )
    }

    private fun messages(count: Int): List<ConversationMessage> {
        return (1..count).map { index ->
            ConversationMessage(
                id = index.toLong(),
                senderName = if (index % 2 == 0) "민지" else "현우",
                sentAtEpochMillis = index.toLong(),
                content = "메시지 $index",
            )
        }
    }

    private fun detail(latestAnalysis: AnalysisReport?): ConversationDetail {
        return ConversationDetail(
            id = 1L,
            title = "샘플 채팅방",
            sourceName = "sample.txt",
            participantNames = listOf("현우", "민지"),
            messages = messages(count = 3),
            latestAnalysis = latestAnalysis,
        )
    }

    private fun report(
        contactReadiness: String = "아주 가볍게 가능",
        reunionObjective: String = "안부만 확인합니다.",
        nextStep: String = "짧게 답장하세요.",
    ): AnalysisReport {
        return AnalysisReport(
            headline = "상대 답장에 짧게 응답",
            contactReadiness = contactReadiness,
            evidence = "상대가 마지막에 답장을 남겼습니다.",
            relationshipSummary = "짧은 답장이 자연스럽습니다.",
            reunionObjective = reunionObjective,
            nextStep = nextStep,
            messageDraft = "메시지 봤어. 고마워.",
            alternativeDrafts = "메시지 봤어. 고마워.",
            caution = "답을 재촉하지 마세요.",
        )
    }
}
