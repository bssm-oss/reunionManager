package com.bssm.reunionmanager.ui.screen.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bssm.reunionmanager.domain.model.ConversationDetail
import com.bssm.reunionmanager.ui.theme.ReunionBadge
import com.bssm.reunionmanager.ui.theme.ReunionBadgeTone
import com.bssm.reunionmanager.ui.theme.ReunionEmptyState
import com.bssm.reunionmanager.ui.theme.ReunionPane
import com.bssm.reunionmanager.ui.theme.ReunionPrimaryButton
import com.bssm.reunionmanager.ui.theme.ReunionSecondaryButton
import com.bssm.reunionmanager.ui.theme.ScreenPadding
import com.bssm.reunionmanager.ui.theme.ScreenSectionSpacing

@Composable
fun PlanCalendarScreen(
    detail: ConversationDetail?,
    onOpenAnalysis: () -> Unit,
) {
    if (detail == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ScreenPadding),
        ) {
            ReunionEmptyState(
                title = "플랜을 불러오는 중",
                body = "저장한 대화를 준비하고 있어요.",
            )
        }
        return
    }

    val report = detail.latestAnalysis
    if (report == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(ScreenSectionSpacing),
        ) {
            ReunionEmptyState(
                title = "아직 달력이 없어요",
                body = "먼저 플랜을 만들면 이번 주 할 일을 볼 수 있어요.",
            ) {
                ReunionPrimaryButton(
                    text = "플랜 만들기",
                    onClick = onOpenAnalysis,
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = ScreenPadding, vertical = ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(ScreenSectionSpacing),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "이번 주 플랜",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            ReunionPane(
                title = "지금 구간",
                supportingText = "회복 코스의 ${report.recoveryStageLabel()} 단계예요.",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ) {
                ReunionBadge(
                    text = report.recoveryStageLabel(),
                    tone = report.readinessTone(),
                )
                Text(
                    text = report.nextStepBodyForUi(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        items(report.weeklyPlanItems(), key = { item -> item.dayLabel }) { item ->
            CalendarDayPane(item)
        }
        item {
            ReunionSecondaryButton(
                text = "회복 맵 보기",
                onClick = onOpenAnalysis,
            )
        }
    }
}

@Composable
private fun CalendarDayPane(item: PlannerDayItem) {
    ReunionPane(
        containerColor = if (item.isToday) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = if (item.isToday) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = item.dayLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (item.isToday) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = item.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.isToday) {
                ReunionBadge(text = "오늘", tone = ReunionBadgeTone.Accent)
            }
        }
    }
}
