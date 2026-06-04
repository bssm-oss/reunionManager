package com.bssm.reunionmanager.ui.screen.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bssm.reunionmanager.domain.model.ProviderSettings
import com.bssm.reunionmanager.ui.ModelSettingsUiState
import com.bssm.reunionmanager.ui.theme.ReunionBadge
import com.bssm.reunionmanager.ui.theme.ReunionBadgeTone
import com.bssm.reunionmanager.ui.theme.ReunionEmptyState
import com.bssm.reunionmanager.ui.theme.ReunionPane
import com.bssm.reunionmanager.ui.theme.ReunionSecondaryButton
import com.bssm.reunionmanager.ui.theme.ScreenPadding
import com.bssm.reunionmanager.ui.theme.ScreenSectionSpacing

@Composable
fun SettingsScreen(
    providerSettings: ProviderSettings,
    modelSettingsState: ModelSettingsUiState,
    onSave: (String, String, String) -> Unit,
    onModelFileSelected: (Uri) -> Unit,
) {
    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(onModelFileSelected)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(ScreenSectionSpacing),
    ) {
        Text(text = "로컬 AI", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "모델 파일을 선택하면 재회 계획을 이 기기에서 만들 수 있습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ReunionPane(
            title = if (providerSettings.isConfigured) "모델 준비됨" else "모델 없음",
            supportingText = if (providerSettings.isConfigured) {
                providerSettings.modelName
            } else {
                "모델을 선택하기 전에는 데모 모드로 흐름을 확인합니다."
            },
        ) {
            ReunionBadge(
                text = if (providerSettings.isConfigured) "기기 내 실행" else "데모 모드",
                tone = if (providerSettings.isConfigured) ReunionBadgeTone.Accent else ReunionBadgeTone.Neutral,
            )
        }
        ReunionPane(
            title = "모델 파일",
            supportingText = "선택한 파일은 이 기기에만 저장됩니다.",
        ) {
            ReunionSecondaryButton(
                text = if (modelSettingsState.isLoading) "모델 복사 중..." else "모델 파일 선택",
                onClick = { modelPickerLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                enabled = !modelSettingsState.isLoading,
            )
            if (modelSettingsState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
        modelSettingsState.message?.let { message ->
            ReunionEmptyState(
                title = "모델 준비 완료",
                body = message,
                tone = ReunionBadgeTone.Success,
            )
        }
        modelSettingsState.errorMessage?.let { errorMessage ->
            ReunionEmptyState(
                title = "모델을 가져오지 못했습니다",
                body = errorMessage,
                tone = ReunionBadgeTone.Error,
            )
        }
        if (providerSettings.isConfigured) {
            ReunionSecondaryButton(
                text = "데모 모드 사용",
                onClick = {
                    onSave(
                        "",
                        providerSettings.modelName,
                        providerSettings.backend.name,
                    )
                },
            )
        }
    }
}
