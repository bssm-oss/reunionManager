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
    providerConfigured: Boolean,
    onImportClick: () -> Unit,
    onConversationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(ScreenSectionSpacing),
    ) {
        ReunionBadge(text = "내 기기에서 보관")
        Text(
            text = "보내기 전, 차분히 정리해요.",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "카카오톡 대화에서 연락 판단, 오늘 할 일, 보낼 문장만 조용히 정리합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ReunionPrimaryButton(text = "카카오톡 대화 가져오기", onClick = onImportClick)
        ReunionSecondaryButton(text = "저장한 대화 보기", onClick = onConversationsClick)
        ReunionSecondaryButton(text = "분석 설정", onClick = onSettingsClick)
        ReunionPane(
            title = "결과",
            supportingText = "연락 판단, 오늘 할 일, 보낼 문장만 먼저 보여줍니다.",
        )
        ReunionPane(
            title = "AI 모드",
            supportingText = if (providerConfigured) {
                "분석과 초안 생성이 이 기기에서 실행됩니다."
            } else {
                "모델이 없으면 참고용 데모로 확인합니다."
            },
        ) {
            ReunionBadge(
                text = if (providerConfigured) "기기 내 실행" else "데모 모드",
                tone = if (providerConfigured) ReunionBadgeTone.Accent else ReunionBadgeTone.Neutral,
            )
        }
        ReunionPane(
            title = "저장한 대화",
            supportingText = "${conversationCount}개의 대화가 저장되어 있습니다.",
        ) {
            ReunionBadge(text = "${conversationCount}개 저장")
        }
    }
}
