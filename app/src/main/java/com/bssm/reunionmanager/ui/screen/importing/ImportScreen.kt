package com.bssm.reunionmanager.ui.screen.importing

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bssm.reunionmanager.ui.ImportUiState

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
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Import a KakaoTalk export",
            style = MaterialTheme.typography.headlineSmall,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Supported file",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "The MVP currently supports KakaoTalk plain-text export files (.txt). The imported chat is parsed and stored only on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Button(
            onClick = { pickerLauncher.launch(arrayOf("text/plain")) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !importState.isLoading,
        ) {
            Text(text = "Choose .txt file")
        }
        if (importState.isLoading) {
            CircularProgressIndicator()
        }
        importState.message?.let { message ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = message, style = MaterialTheme.typography.bodyMedium)
                    importState.importedConversationId?.let { conversationId ->
                        Button(onClick = { onViewConversationClick(conversationId) }) {
                            Text(text = "Open imported chat")
                        }
                    }
                }
            }
        }
        importState.errorMessage?.let { errorMessage ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = errorMessage, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
