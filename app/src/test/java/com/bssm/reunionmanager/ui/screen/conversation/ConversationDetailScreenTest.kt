package com.bssm.reunionmanager.ui.screen.conversation

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
}
