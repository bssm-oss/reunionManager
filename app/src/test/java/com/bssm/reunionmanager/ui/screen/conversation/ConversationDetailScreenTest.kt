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
            "전체 12개 중 최근 8개만 먼저 보여줍니다.",
            messages.messagePreviewSummary(showAllMessages = false),
        )
        assertFalse(messages.hasHiddenMessages(showAllMessages = true))
        assertEquals(
            "전체 12개 메시지를 보고 있습니다.",
            messages.messagePreviewSummary(showAllMessages = true),
        )
    }

    @Test
    fun messagePreviewSummary_handlesShortConversationWithoutExtraAction() {
        val messages = messages(count = 3)

        assertFalse(messages.hasHiddenMessages(showAllMessages = false))
        assertEquals(
            "전체 3개 메시지를 보고 있습니다.",
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

    private fun report(): AnalysisReport {
        return AnalysisReport(
            headline = "상대 답장에 짧게 응답",
            contactReadiness = "아주 가볍게 가능",
            evidence = "상대가 마지막에 답장을 남겼습니다.",
            relationshipSummary = "짧은 답장이 자연스럽습니다.",
            reunionObjective = "안부만 확인합니다.",
            nextStep = "짧게 답장하세요.",
            messageDraft = "메시지 봤어. 고마워.",
            alternativeDrafts = "메시지 봤어. 고마워.",
            caution = "답을 재촉하지 마세요.",
        )
    }
}
