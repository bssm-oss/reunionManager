package com.bssm.reunionmanager.ui.screen.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bssm.reunionmanager.domain.model.AnalysisReport
import com.bssm.reunionmanager.domain.model.ConversationDetail
import com.bssm.reunionmanager.domain.model.ConversationMessage
import com.bssm.reunionmanager.ui.theme.ReunionBadge
import com.bssm.reunionmanager.ui.theme.ReunionBadgeTone
import com.bssm.reunionmanager.ui.theme.ReunionEmptyState
import com.bssm.reunionmanager.ui.theme.ReunionPane
import com.bssm.reunionmanager.ui.theme.ReunionPrimaryButton
import com.bssm.reunionmanager.ui.theme.ReunionSecondaryButton
import com.bssm.reunionmanager.ui.theme.ScreenPadding
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
                .padding(ScreenPadding),
        ) {
            ReunionEmptyState(
                title = "대화를 불러오는 중",
                body = "저장한 대화를 준비하고 있습니다.",
            )
        }
        return
    }

    var showAllMessages by remember(detail.id) { mutableStateOf(false) }
    val visibleMessages = detail.messages.visibleMessagePreview(showAllMessages)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = ScreenPadding, vertical = ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = detail.title, style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = detail.sourceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            ReunionPane(
                title = detail.latestAnalysis?.let { "최근 정리" } ?: "아직 정리하지 않았어요",
                supportingText = detail.latestAnalysis?.headline ?: "대화를 훑기 전에 오늘 할 일만 먼저 정리할 수 있어요.",
                containerColor = detail.latestAnalysis?.detailContainerColor() ?: MaterialTheme.colorScheme.surface,
            ) {
                detail.latestAnalysis?.let { report ->
                    ReunionBadge(
                        text = report.contactReadiness,
                        tone = report.detailReadinessTone(),
                    )
                    Text(
                        text = report.nextStep.limitForDetail(maxLength = 88),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ReunionPrimaryButton(
                    text = if (detail.latestAnalysis == null) "다음 행동 정리하기" else "다시 정리하기",
                    onClick = onOpenAnalysis,
                )
            }
        }
        item {
            ReunionPane(
                title = "대화 정보",
                supportingText = detail.infoSummary(),
            ) {
                ReunionBadge(text = "기기 내 저장")
            }
        }
        item {
            ReunionPane(
                title = if (showAllMessages) "전체 메시지" else "최근 메시지",
                supportingText = detail.messages.messagePreviewSummary(showAllMessages),
            ) {
                if (detail.messages.hasHiddenMessages(showAllMessages)) {
                    ReunionSecondaryButton(
                        text = "전체 메시지 보기",
                        onClick = { showAllMessages = true },
                    )
                } else if (showAllMessages && detail.messages.size > RecentMessagePreviewCount) {
                    ReunionSecondaryButton(
                        text = "최근 메시지만 보기",
                        onClick = { showAllMessages = false },
                    )
                }
            }
        }
        items(visibleMessages, key = { it.id }) { message ->
            ReunionPane(
                title = message.senderName,
                supportingText = formatter.format(Instant.ofEpochMilli(message.sentAtEpochMillis)),
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private const val RecentMessagePreviewCount = 8

internal fun List<ConversationMessage>.visibleMessagePreview(showAllMessages: Boolean): List<ConversationMessage> {
    return if (showAllMessages) this else takeLast(RecentMessagePreviewCount)
}

internal fun List<ConversationMessage>.hasHiddenMessages(showAllMessages: Boolean): Boolean {
    return !showAllMessages && size > RecentMessagePreviewCount
}

internal fun List<ConversationMessage>.messagePreviewSummary(showAllMessages: Boolean): String {
    return when {
        isEmpty() -> "저장된 메시지가 없습니다."
        showAllMessages -> "전체 ${size}개 메시지를 보고 있습니다."
        size > RecentMessagePreviewCount -> "전체 ${size}개 중 최근 ${RecentMessagePreviewCount}개만 먼저 보여줍니다."
        else -> "전체 ${size}개 메시지를 보고 있습니다."
    }
}

private fun ConversationDetail.infoSummary(): String {
    val participants = participantNames.joinToString().ifBlank { "알 수 없음" }
    return "참여자 $participants\n메시지 ${messages.size}개"
}

private fun AnalysisReport.detailReadinessTone(): ReunionBadgeTone {
    return when (contactReadiness) {
        "지금은 보류" -> ReunionBadgeTone.Error
        "아주 가볍게 가능" -> ReunionBadgeTone.Success
        "먼저 사과 필요" -> ReunionBadgeTone.Accent
        else -> ReunionBadgeTone.Neutral
    }
}

@Composable
private fun AnalysisReport.detailContainerColor() = when (detailReadinessTone()) {
    ReunionBadgeTone.Error -> MaterialTheme.colorScheme.errorContainer
    ReunionBadgeTone.Success -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
    ReunionBadgeTone.Accent -> MaterialTheme.colorScheme.primaryContainer
    ReunionBadgeTone.Neutral -> MaterialTheme.colorScheme.surface
}

private fun String.limitForDetail(maxLength: Int): String {
    return if (length <= maxLength) this else take(maxLength - 1).trimEnd() + "…"
}
