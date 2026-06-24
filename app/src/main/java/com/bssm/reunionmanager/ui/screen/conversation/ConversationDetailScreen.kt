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
import com.bssm.reunionmanager.ui.screen.common.needsPerspectiveSetup
import com.bssm.reunionmanager.ui.screen.common.perspectiveNameButtonText
import com.bssm.reunionmanager.ui.screen.common.perspectiveNameOptions
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
    userDisplayName: String = "",
    onOpenAnalysis: () -> Unit,
    onSaveUserDisplayName: (String) -> Unit = {},
    onDeleteConversation: () -> Unit = {},
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
    var isConfirmingDelete by remember(detail.id) { mutableStateOf(false) }
    val visibleMessages = detail.messages.visibleMessagePreview(showAllMessages)
    val perspectiveOptions = perspectiveNameOptions(detail.participantNames)
    val showPerspectivePicker = needsPerspectiveSetup(
        participantNames = detail.participantNames,
        userDisplayName = userDisplayName,
    ) && perspectiveOptions.isNotEmpty()

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
                title = detail.latestAnalysis?.let { "최근 플랜" } ?: "아직 플랜이 없어요",
                supportingText = detail.latestAnalysis?.detailHeadline() ?: "카톡 내용으로 재회 플랜을 만들 수 있어요.",
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
                ReunionBadge(text = "비공개 보관")
            }
        }
        if (showPerspectivePicker) {
            item {
                ReunionPane(
                    title = "내 이름 선택",
                    supportingText = "대화방에서 내 이름을 고르면 분석이 정확해져요.",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    perspectiveOptions.forEach { participantName ->
                        ReunionSecondaryButton(
                            text = perspectiveNameButtonText(participantName),
                            onClick = { onSaveUserDisplayName(participantName) },
                        )
                    }
                }
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
        item {
            ReunionPane(
                title = "대화 관리",
                supportingText = deleteConversationSupportingText(isConfirmingDelete),
                containerColor = if (isConfirmingDelete) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            ) {
                if (isConfirmingDelete) {
                    ReunionSecondaryButton(
                        text = "삭제 취소",
                        onClick = { isConfirmingDelete = false },
                    )
                    ReunionSecondaryButton(
                        text = "정말 삭제",
                        onClick = onDeleteConversation,
                    )
                } else {
                    ReunionSecondaryButton(
                        text = "대화 삭제",
                        onClick = { isConfirmingDelete = true },
                    )
                }
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
    return if (latestAnalysis == null) "플랜 만들기" else "플랜 보기"
}

internal fun deleteConversationSupportingText(isConfirmingDelete: Boolean): String {
    return if (isConfirmingDelete) {
        "이 대화와 플랜을 이 기기에서 삭제합니다."
    } else {
        "필요 없어진 대화는 언제든 이 기기에서 지울 수 있어요."
    }
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
        "지금은 보류" -> "부담을 낮추는 구간이에요."
        "정보 부족" -> "상황을 먼저 읽는 구간이에요."
        "먼저 사과 필요" -> "관계를 조심스럽게 낮추는 구간이에요."
        else -> if (detailLooksLikeReply()) {
            "대화를 다시 열 수 있는 구간이에요."
        } else {
            "작은 연결을 준비할 수 있어요."
        }
    }
}

internal fun AnalysisReport.detailNextAction(): String {
    return when (contactReadiness) {
        "지금은 보류" -> "상대 반응을 더 지켜보면 안정적이에요."
        "정보 부족" -> "내 이름과 최근 대화가 맞는지 보면 좋아요."
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
