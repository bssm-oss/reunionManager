package com.bssm.reunionmanager.ui.screen.importing

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bssm.reunionmanager.ui.ImportUiState
import com.bssm.reunionmanager.ui.theme.ReunionBadgeTone
import com.bssm.reunionmanager.ui.theme.ReunionEmptyState
import com.bssm.reunionmanager.ui.theme.ReunionPane
import com.bssm.reunionmanager.ui.theme.ReunionPrimaryButton
import com.bssm.reunionmanager.ui.theme.ReunionSecondaryButton
import com.bssm.reunionmanager.ui.theme.ScreenPadding
import com.bssm.reunionmanager.ui.theme.ScreenSectionSpacing

@Composable
fun ImportScreen(
    importState: ImportUiState,
    onImportClick: (Uri) -> Unit,
    onViewConversationClick: (Long) -> Unit,
) {
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(onImportClick)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(ScreenSectionSpacing),
    ) {
        Text(
            text = "Import a KakaoTalk export",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Choose a supported plain-text export and keep the imported chat on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ReunionPane(
            title = "Supported file",
            supportingText = "The MVP currently supports KakaoTalk plain-text export files (.txt). The imported chat is parsed and stored only on this device.",
        )
        ReunionPrimaryButton(
            text = if (importState.isLoading) "Importing locally..." else "Choose .txt file",
            onClick = { pickerLauncher.launch(arrayOf("text/plain")) },
            enabled = !importState.isLoading,
        )

        if (importState.isLoading) {
            ReunionEmptyState(
                title = "Import in progress",
                body = "The selected file is being read and parsed locally on this device.",
                tone = ReunionBadgeTone.Accent,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            }
        }

        importState.message?.let { message ->
            ReunionEmptyState(
                title = "Import status",
                body = message,
                tone = ReunionBadgeTone.Success,
            ) {
                importState.importedConversationId?.let { conversationId ->
                    ReunionSecondaryButton(
                        text = "Open imported chat",
                        onClick = { onViewConversationClick(conversationId) },
                    )
                }
            }
        }

        importState.errorMessage?.let { errorMessage ->
            ReunionEmptyState(
                title = "Import failed",
                body = errorMessage,
                tone = ReunionBadgeTone.Error,
            )
        }
    }
}
