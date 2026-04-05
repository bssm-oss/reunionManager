package com.bssm.reunionmanager.ui.screen.analysis

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
import com.bssm.reunionmanager.domain.model.ConversationDetail
import com.bssm.reunionmanager.ui.AnalysisUiState

@Composable
fun AnalysisScreen(
    detail: ConversationDetail?,
    analysisState: AnalysisUiState?,
    onGenerate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = detail?.title ?: "Loading conversation...",
            style = MaterialTheme.typography.headlineSmall,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "How this works", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "The plan is generated from the saved local chat. If Gemini is not configured, the app uses a local fake provider. If Gemini is configured, generating a plan sends a compact chat excerpt to the saved endpoint for that request.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (analysisState?.isRunning == true) {
            CircularProgressIndicator()
        } else {
            Button(onClick = onGenerate, enabled = detail != null) {
                Text(text = "Generate reunion plan")
            }
        }

        analysisState?.providerType?.let { providerType ->
            Text(
                text = "Last generated with: $providerType",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        analysisState?.errorMessage?.let { errorMessage ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = errorMessage, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        detail?.latestAnalysis?.let { report ->
            AnalysisSectionCard(title = "Relationship summary", body = report.relationshipSummary)
            AnalysisSectionCard(title = "Reunion objective", body = report.reunionObjective)
            AnalysisSectionCard(title = "Next step", body = report.nextStep)
            AnalysisSectionCard(title = "Caution", body = report.caution)
        } ?: Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "No plan has been generated for this conversation yet.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun AnalysisSectionCard(title: String, body: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
