package com.bssm.reunionmanager.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    conversationCount: Int,
    providerConfigured: Boolean,
    onImportClick: () -> Unit,
    onConversationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Import a KakaoTalk chat, keep it on-device, and build a careful reunion plan from the saved conversation.",
            style = MaterialTheme.typography.headlineSmall,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Local data only",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Imported chats and provider settings stay on this device. If you later run analysis with a configured AI endpoint, the app sends a compact chat excerpt only for that request.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Provider status",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (providerConfigured) {
                        "Gemini-compatible analysis is configured locally. No chat content is sent unless you explicitly generate a reunion plan."
                    } else {
                        "AI is not configured yet. You can still import and review chats, and the app will use a local fake provider for the MVP analysis flow."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Saved chats",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "$conversationCount conversation(s) are currently stored locally.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Button(onClick = onImportClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Import KakaoTalk .txt")
        }
        Button(onClick = onConversationsClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Browse saved chats")
        }
        Button(onClick = onSettingsClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Open AI settings")
        }
    }
}
