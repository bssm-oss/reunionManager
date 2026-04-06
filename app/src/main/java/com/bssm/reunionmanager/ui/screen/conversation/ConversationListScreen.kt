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
                title = "No chats saved yet.",
                body = "Import a KakaoTalk .txt export first. Saved chats remain local to this device.",
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Saved chats", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Review saved conversations on this device and reopen a chat when you need it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            ReunionPane(
                title = "Local chat library",
                supportingText = "Saved chats remain local to this device.",
            ) {
                ReunionBadge(text = "${conversations.size} stored chat(s)")
            }
        }
        items(conversations, key = { it.id }) { conversation ->
            ReunionPane(
                modifier = Modifier.clickable { onConversationClick(conversation.id) },
                title = conversation.title,
                supportingText = "${conversation.participantCount} participant(s) · ${conversation.messageCount} message(s)",
            ) {
                Text(
                    text = conversation.sourceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
