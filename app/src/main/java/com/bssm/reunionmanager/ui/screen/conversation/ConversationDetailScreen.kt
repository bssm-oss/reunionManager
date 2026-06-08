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
                supportingText = detail.latestAnalysis?.detailHeadline() ?: "오늘 보낼지 말지만 먼저 정리할 수 있어요.",
                containerColor = detail.latestAnalysis?.detailContainerColor() ?: MaterialTheme.colorScheme.surface,
            ) {
                detail.latestAnalysis?.let { report ->
                    ReunionBadge(
                        text = report.contactReadiness,
                        tone = report.detailReadinessTone(),
                    )
                    Text(
                        text = report.detailNextAction(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ReunionPrimaryButton(
                    text = detail.analysisEntryButtonText(),
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
        showAllMessages -> "전체 ${size}개 메시지"
        size > RecentMessagePreviewCount -> "최근 ${RecentMessagePreviewCount}개만 먼저 보기"
        else -> "전체 ${size}개 메시지"
    }
}

private fun ConversationDetail.infoSummary(): String {
    val participants = participantNames.joinToString().ifBlank { "알 수 없음" }
    return "참여자 $participants\n메시지 ${messages.size}개"
}

internal fun ConversationDetail.analysisEntryButtonText(): String {
    return if (latestAnalysis == null) "다음 행동 정리하기" else "정리 결과 보기"
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

internal fun AnalysisReport.detailHeadline(): String {
    return when (contactReadiness) {
        "지금은 보류" -> "오늘은 보내지 않는 쪽이 안전해요."
        "정보 부족" -> "먼저 확인할 정보가 있어요."
        "먼저 사과 필요" -> "짧은 인정부터 준비해요."
        else -> if (detailLooksLikeReply()) {
            "짧게 답장하면 충분해요."
        } else {
            "부담 없는 한 문장만 준비해요."
        }
    }
}

internal fun AnalysisReport.detailNextAction(): String {
    return when (contactReadiness) {
        "지금은 보류" -> "오늘은 보내지 말고 기다려요."
        "정보 부족" -> "내 이름과 최근 대화가 맞는지 확인하세요."
        "먼저 사과 필요" -> nextStep.limitForDetail(maxLength = 52)
        else -> nextStep.limitForDetail(maxLength = 52)
    }
}

private fun AnalysisReport.detailLooksLikeReply(): Boolean {
    return nextStep.contains("상대의 마지막 메시지") || reunionObjective.contains("상대가 남긴 말")
}

private fun String.limitForDetail(maxLength: Int): String {
    return if (length <= maxLength) this else take(maxLength - 1).trimEnd() + "…"
}
