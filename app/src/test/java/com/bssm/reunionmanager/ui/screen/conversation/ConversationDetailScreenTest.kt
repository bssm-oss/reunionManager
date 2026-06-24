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
            "플랜 만들기",
            detail(latestAnalysis = null).analysisEntryButtonText(),
        )
        assertEquals(
            "플랜 보기",
            detail(latestAnalysis = report()).analysisEntryButtonText(),
        )
    }

    @Test
    fun detailHeadline_summarizesLatestAnalysisAsAction() {
        assertEquals(
            "부담을 낮추는 구간이에요.",
            report(headline = "", contactReadiness = "지금은 보류").detailHeadline(),
        )
        assertEquals(
            "상황을 먼저 읽는 구간이에요.",
            report(headline = "", contactReadiness = "정보 부족").detailHeadline(),
        )
        assertEquals(
            "관계를 조심스럽게 낮추는 구간이에요.",
            report(headline = "", contactReadiness = "먼저 사과 필요").detailHeadline(),
        )
        assertEquals(
            "상대 답장에 짧게 응답",
            report(contactReadiness = "지금은 보류").detailHeadline(),
        )
        assertEquals(
            "대화를 다시 열 수 있는 구간이에요.",
            report(
                headline = "",
                reunionObjective = "상대가 남긴 말에 답하는 것이 목표입니다.",
                nextStep = "상대의 마지막 메시지에 짧게 답하세요.",
            ).detailHeadline(),
        )
        assertEquals(
            "작은 연결을 준비할 수 있어요.",
            report(headline = "").detailHeadline(),
        )
    }

    @Test
    fun detailNextAction_keepsLatestAnalysisCardShort() {
        assertEquals(
            "오늘은 거리를 두고 최근 대화를 확인해보면 좋아요.",
            report(contactReadiness = "지금은 보류", nextStep = "오늘은 보내지 말고 최근 대화를 확인하세요.").detailNextAction(),
        )
        assertEquals(
            "내 카톡 이름을 확인해보면 좋아요.",
            report(contactReadiness = "정보 부족", nextStep = "내 카톡 이름을 확인하세요.").detailNextAction(),
        )
        assertEquals(
            52,
            report(nextStep = "가".repeat(120)).detailNextAction().length,
        )
    }

    @Test
    fun deleteConversationSupportingText_keepsPrivacyCopyShort() {
        assertEquals(
            "필요 없어진 대화는 언제든 이 기기에서 지울 수 있어요.",
            deleteConversationSupportingText(isConfirmingDelete = false),
        )
        assertEquals(
            "이 대화와 플랜을 이 기기에서 삭제합니다.",
            deleteConversationSupportingText(isConfirmingDelete = true),
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
        headline: String = "상대 답장에 짧게 응답",
        contactReadiness: String = "아주 가볍게 가능",
        reunionObjective: String = "안부만 확인합니다.",
        nextStep: String = "짧게 답장하세요.",
    ): AnalysisReport {
        return AnalysisReport(
            headline = headline,
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
