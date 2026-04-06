package com.bssm.reunionmanager.ui.screen.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bssm.reunionmanager.domain.model.ConversationDetail
import com.bssm.reunionmanager.ui.AnalysisUiState
import com.bssm.reunionmanager.ui.theme.ReunionBadge
import com.bssm.reunionmanager.ui.theme.ReunionBadgeTone
import com.bssm.reunionmanager.ui.theme.ReunionEmptyState
import com.bssm.reunionmanager.ui.theme.ReunionPane
import com.bssm.reunionmanager.ui.theme.ReunionPrimaryButton
import com.bssm.reunionmanager.ui.theme.ScreenPadding
import com.bssm.reunionmanager.ui.theme.ScreenSectionSpacing

@Composable
fun AnalysisScreen(
    detail: ConversationDetail?,
    analysisState: AnalysisUiState?,
    onGenerate: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = ScreenPadding, vertical = ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(ScreenSectionSpacing),
    ) {
        item {
            Text(
                text = detail?.title ?: "Loading conversation...",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        item {
            ReunionPane(
                title = "How this works",
                supportingText = "The plan is generated from the saved local chat. If Gemini is not configured, the app uses a local fake provider. If Gemini is configured, generating a plan sends a compact chat excerpt to the saved endpoint for that request.",
            ) {
                analysisState?.providerType?.let { providerType ->
                    ReunionBadge(
                        text = "Last generated with: $providerType",
                        tone = ReunionBadgeTone.Accent,
                    )
                }
            }
        }
        item {
            ReunionPane(
                title = "How to read this plan",
                supportingText = "Treat the generated reunion plan as guidance from the saved chat, not as certainty. Use it as a careful starting point before acting.",
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }
        item {
            if (analysisState?.isRunning == true) {
                ReunionEmptyState(
                    title = "Generating reunion plan",
                    body = "Preparing guidance from the saved local chat.",
                    tone = ReunionBadgeTone.Accent,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
            } else {
                ReunionPrimaryButton(
                    text = "Generate reunion plan",
                    onClick = onGenerate,
                    enabled = detail != null,
                )
            }
        }
        analysisState?.errorMessage?.let { errorMessage ->
            item {
                ReunionEmptyState(
                    title = "Plan generation failed",
                    body = errorMessage,
                    tone = ReunionBadgeTone.Error,
                )
            }
        }
        detail?.latestAnalysis?.let { report ->
            item { AnalysisSectionPane(title = "Relationship summary", body = report.relationshipSummary) }
            item { AnalysisSectionPane(title = "Reunion objective", body = report.reunionObjective) }
            item { AnalysisSectionPane(title = "Next step", body = report.nextStep) }
            item {
                AnalysisSectionPane(
                    title = "Caution",
                    body = report.caution,
                    tone = ReunionBadgeTone.Error,
                )
            }
        } ?: item {
            ReunionEmptyState(
                title = "No plan generated yet",
                body = "No plan has been generated for this conversation yet.",
            )
        }
    }
}

@Composable
private fun AnalysisSectionPane(
    title: String,
    body: String,
    tone: ReunionBadgeTone = ReunionBadgeTone.Neutral,
) {
    val containerColor = if (tone == ReunionBadgeTone.Error) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    ReunionPane(
        title = title,
        supportingText = body,
        containerColor = containerColor,
    )
}
