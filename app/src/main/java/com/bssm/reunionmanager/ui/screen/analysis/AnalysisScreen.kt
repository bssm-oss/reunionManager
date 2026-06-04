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
                text = detail?.title ?: "대화를 불러오는 중",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        item {
            if (analysisState?.isRunning == true) {
                ReunionEmptyState(
                    title = "재회 계획을 만드는 중",
                    body = "저장한 대화에서 부담 없는 다음 행동을 정리하고 있습니다.",
                    tone = ReunionBadgeTone.Accent,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
            } else {
                ReunionPrimaryButton(
                    text = "재회 계획 만들기",
                    onClick = onGenerate,
                    enabled = detail != null,
                )
            }
        }
        item {
            ReunionPane(
                title = "행동 전 확인",
                supportingText = "상대의 마음을 확정하지 말고, 답을 재촉하지 않는 계획으로만 사용하세요.",
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }
        analysisState?.errorMessage?.let { errorMessage ->
            item {
                ReunionEmptyState(
                    title = "계획을 만들지 못했습니다",
                    body = errorMessage,
                    tone = ReunionBadgeTone.Error,
                )
            }
        }
        detail?.latestAnalysis?.let { report ->
            item { AnalysisSectionPane(title = "오늘 할 일", body = report.nextStep, tone = ReunionBadgeTone.Accent) }
            item { AnalysisSectionPane(title = "첫 연락 문장", body = report.messageDraft) }
            item { AnalysisSectionPane(title = "대화 흐름", body = report.relationshipSummary) }
            item { AnalysisSectionPane(title = "목표", body = report.reunionObjective) }
            item {
                AnalysisSectionPane(
                    title = "주의할 점",
                    body = report.caution,
                    tone = ReunionBadgeTone.Error,
                )
            }
        } ?: item {
            ReunionEmptyState(
                title = "아직 만든 계획이 없습니다",
                body = "준비되면 재회 계획을 만들어 보세요.",
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
    } else if (tone == ReunionBadgeTone.Accent) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    ReunionPane(
        title = title,
        supportingText = body,
        containerColor = containerColor,
    )
}
