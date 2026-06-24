package com.bssm.reunionmanager.ui.screen.conversation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bssm.reunionmanager.domain.model.ConversationSummary
import com.bssm.reunionmanager.ui.theme.ReunionBadge
import com.bssm.reunionmanager.ui.theme.ReunionBadgeTone
import com.bssm.reunionmanager.ui.theme.ReunionEmptyState
import com.bssm.reunionmanager.ui.theme.ReunionPane
import com.bssm.reunionmanager.ui.theme.ScreenPadding
import com.bssm.reunionmanager.ui.theme.ScreenSectionSpacing

@Composable
fun ConversationListScreen(
    conversations: List<ConversationSummary>,
    onConversationClick: (Long) -> Unit,
) {
    if (conversations.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(ScreenSectionSpacing),
        ) {
            ReunionEmptyState(
                title = "아직 지난 플랜이 없어요",
                body = "대화 흐름을 불러오면 여기에서 다시 볼 수 있어요.",
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = ScreenPadding, vertical = ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "대화를 눌러 회복 플랜을 이어서 볼 수 있어요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(conversations, key = { it.id }) { conversation ->
            ReunionPane(
                modifier = Modifier.clickable { onConversationClick(conversation.id) },
                title = conversation.title,
                supportingText = conversation.summaryText(),
            ) {
                if (conversation.latestAnalysisHeadline != null) {
                    ReunionBadge(
                        text = "플랜 있음",
                        tone = ReunionBadgeTone.Accent,
                    )
                }
                Text(
                    text = conversation.sourceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun ConversationSummary.summaryText(): String {
    val base = "참여자 ${participantCount}명 · 메시지 ${messageCount}개"
    return latestAnalysisHeadline
        ?.takeIf { headline -> headline.isNotBlank() }
        ?.let { headline -> "$base\n최근 플랜: $headline" }
        ?: base
}
