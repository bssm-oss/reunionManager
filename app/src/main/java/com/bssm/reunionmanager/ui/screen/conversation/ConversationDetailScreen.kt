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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bssm.reunionmanager.domain.model.ConversationDetail
import com.bssm.reunionmanager.ui.theme.ReunionBadge
import com.bssm.reunionmanager.ui.theme.ReunionEmptyState
import com.bssm.reunionmanager.ui.theme.ReunionPane
import com.bssm.reunionmanager.ui.theme.ReunionPrimaryButton
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
                title = "대화 요약",
                supportingText = "참여자 ${detail.participantNames.size}명 · 메시지 ${detail.messages.size}개",
            ) {
                ReunionBadge(text = "기기 내 저장")
            }
        }
        item {
            ReunionPane(
                title = "참여자",
                supportingText = detail.participantNames.joinToString().ifBlank { "알 수 없음" },
            )
        }
        item {
            ReunionPrimaryButton(text = "다음 행동 정리하기", onClick = onOpenAnalysis)
        }
        detail.latestAnalysis?.let { report ->
            item {
                ReunionPane(
                    title = "최근 정리",
                    supportingText = report.headline,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = report.caution,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Text(text = "메시지", style = MaterialTheme.typography.titleLarge)
        }
        items(detail.messages, key = { it.id }) { message ->
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
