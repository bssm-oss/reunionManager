package com.bssm.reunionmanager.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bssm.reunionmanager.ui.theme.ReunionBadge
import com.bssm.reunionmanager.ui.theme.ReunionBadgeTone
import com.bssm.reunionmanager.ui.theme.ReunionPane
import com.bssm.reunionmanager.ui.theme.ReunionPrimaryButton
import com.bssm.reunionmanager.ui.theme.ReunionSecondaryButton
import com.bssm.reunionmanager.ui.theme.ScreenPadding
import com.bssm.reunionmanager.ui.theme.ScreenSectionSpacing

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
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(ScreenSectionSpacing),
    ) {
        ReunionBadge(text = "Local-only workflow")
        Text(
            text = "Review KakaoTalk chats locally and build a careful reunion plan.",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Import a KakaoTalk chat, keep it on-device, and build a careful reunion plan from the saved conversation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ReunionPane(
            title = "Local data only",
            supportingText = "Imported chats and provider settings stay on this device. If you later run analysis with a configured AI endpoint, the app sends a compact chat excerpt only for that request.",
        )
        ReunionPane(
            title = "Provider status",
            supportingText = if (providerConfigured) {
                "Gemini-compatible analysis is configured locally. No chat content is sent unless you explicitly generate a reunion plan."
            } else {
                "AI is not configured yet. You can still import and review chats, and the app will use a local fake provider for the MVP analysis flow."
            },
        ) {
            ReunionBadge(
                text = if (providerConfigured) "AI configured" else "Using local provider",
                tone = if (providerConfigured) ReunionBadgeTone.Accent else ReunionBadgeTone.Neutral,
            )
        }
        ReunionPane(
            title = "Saved chats",
            supportingText = "$conversationCount conversation(s) are currently stored locally.",
        ) {
            ReunionBadge(text = "$conversationCount saved")
        }
        ReunionPrimaryButton(text = "Import KakaoTalk .txt", onClick = onImportClick)
        ReunionSecondaryButton(text = "Browse saved chats", onClick = onConversationsClick)
        ReunionSecondaryButton(text = "Open AI settings", onClick = onSettingsClick)
    }
}
