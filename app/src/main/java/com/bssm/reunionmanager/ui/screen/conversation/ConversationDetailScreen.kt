package com.bssm.reunionmanager.ui.screen.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bssm.reunionmanager.domain.model.ConversationDetail
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ConversationDetailScreen(
    detail: ConversationDetail?,
    onOpenAnalysis: () -> Unit,
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

    if (detail == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Text(text = "Loading conversation...", style = MaterialTheme.typography.headlineSmall)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(text = detail.title, style = MaterialTheme.typography.headlineSmall)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "Participants", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = detail.participantNames.joinToString().ifBlank { "Unknown participants" },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        item {
            Button(onClick = onOpenAnalysis) {
                Text(text = "Open reunion plan")
            }
        }
        detail.latestAnalysis?.let { report ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = report.headline, style = MaterialTheme.typography.titleMedium)
                        Text(text = report.caution, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        items(detail.messages, key = { it.id }) { message ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(text = message.senderName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = formatter.format(Instant.ofEpochMilli(message.sentAtEpochMillis)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(text = message.content, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
