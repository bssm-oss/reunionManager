package com.bssm.reunionmanager.ui.screen.conversation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bssm.reunionmanager.domain.model.ConversationSummary

@Composable
fun ConversationListScreen(
    conversations: List<ConversationSummary>,
    onConversationClick: (Long) -> Unit,
) {
    if (conversations.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "No chats saved yet.", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Import a KakaoTalk .txt export first. Saved chats remain local to this device.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(conversations, key = { it.id }) { conversation ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onConversationClick(conversation.id) },
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(text = conversation.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${conversation.participantCount} participant(s) · ${conversation.messageCount} message(s)",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(text = conversation.sourceName, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
