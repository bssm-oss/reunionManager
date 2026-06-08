package com.bssm.reunionmanager.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bssm.reunionmanager.ui.theme.ReunionBadge
import com.bssm.reunionmanager.ui.theme.ReunionBadgeTone
import com.bssm.reunionmanager.ui.theme.ReunionPane
import com.bssm.reunionmanager.ui.theme.ReunionPrimaryButton
import com.bssm.reunionmanager.ui.theme.ReunionSecondaryButton
import com.bssm.reunionmanager.ui.theme.ScreenPadding
import com.bssm.reunionmanager.ui.theme.ScreenSectionSpacing

@Composable
fun HomeScreen(
    conversationCount: Int,
    modelConfigured: Boolean,
    modelVerified: Boolean,
    onImportClick: () -> Unit,
    onConversationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val hasConversations = conversationCount > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(ScreenSectionSpacing),
    ) {
        ReunionBadge(text = "내 기기에서 보관")
        Text(
            text = "보내기 전, 한 번만 정리해요.",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "대화는 기기 안에 두고, 오늘 보낼지 말지만 먼저 봅니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ReunionPane(
            title = if (hasConversations) "이어볼 대화가 있어요" else "대화 파일부터 가져오세요",
            supportingText = homeStateDescription(
                conversationCount = conversationCount,
                modelConfigured = modelConfigured,
                modelVerified = modelVerified,
            ),
        ) {
            ReunionBadge(text = "${conversationCount}개 저장")
            ReunionBadge(
                text = modelModeBadge(
                    modelConfigured = modelConfigured,
                    modelVerified = modelVerified,
                ),
                tone = modelModeTone(
                    modelConfigured = modelConfigured,
                    modelVerified = modelVerified,
                ),
            )
        }

        if (hasConversations) {
            ReunionPrimaryButton(text = "저장한 대화 보기", onClick = onConversationsClick)
            ReunionSecondaryButton(text = "새 대화 가져오기", onClick = onImportClick)
        } else {
            ReunionPrimaryButton(text = "카카오톡 대화 가져오기", onClick = onImportClick)
        }
        ReunionSecondaryButton(text = "분석 설정", onClick = onSettingsClick)
    }
}

private fun homeStateDescription(
    conversationCount: Int,
    modelConfigured: Boolean,
    modelVerified: Boolean,
): String {
    val conversationText = if (conversationCount > 0) {
        "${conversationCount}개 대화에서 바로 다음 행동을 정리할 수 있어요."
    } else {
        "카카오톡 내보내기 파일 하나면 시작할 수 있어요."
    }
    return "$conversationText ${modelModeDescription(modelConfigured, modelVerified)}"
}

private fun modelModeDescription(modelConfigured: Boolean, modelVerified: Boolean): String {
    return when {
        modelVerified -> "AI는 이 기기에서 실행됩니다."
        modelConfigured -> "점검 전에는 기본 정리로 진행합니다."
        else -> "모델이 없어도 기본 정리로 확인할 수 있어요."
    }
}

private fun modelModeBadge(modelConfigured: Boolean, modelVerified: Boolean): String {
    return when {
        modelVerified -> "실행 확인됨"
        modelConfigured -> "점검 필요"
        else -> "기본 정리"
    }
}

private fun modelModeTone(modelConfigured: Boolean, modelVerified: Boolean): ReunionBadgeTone {
    return when {
        modelVerified -> ReunionBadgeTone.Success
        modelConfigured -> ReunionBadgeTone.Accent
        else -> ReunionBadgeTone.Neutral
    }
}
