package com.bssm.reunionmanager.ui.screen.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.bssm.reunionmanager.ui.theme.reunionOutlinedTextFieldColors

@Composable
fun SettingsScreen(
    providerSettings: ProviderSettings,
    openRouterConfigured: Boolean,
    modelSettingsState: ModelSettingsUiState,
    onSave: (String, String, String, String) -> Unit,
    onModelFileSelected: (Uri) -> Unit,
    onVerifyModel: () -> Unit,
) {
    var userDisplayName by remember(providerSettings.userDisplayName) {
        mutableStateOf(providerSettings.userDisplayName)
    }
    var showTechnicalDetails by remember { mutableStateOf(false) }
    val modelStatusTitle = "기술 정보"
    val modelStatusBadge = when {
        openRouterConfigured -> "준비됨"
        !providerSettings.isConfigured -> "기본 정리"
        providerSettings.isModelVerified -> "준비됨"
        else -> "점검 전"
    }
    val modelStatusTone = when {
        openRouterConfigured -> ReunionBadgeTone.Success
        !providerSettings.isConfigured -> ReunionBadgeTone.Neutral
        providerSettings.isModelVerified -> ReunionBadgeTone.Success
        else -> ReunionBadgeTone.Accent
    }
    val modelStatusDescription = when {
        openRouterConfigured -> "플랜을 만들 때 필요한 대화 일부만 분석 연결에 사용합니다."
        !providerSettings.isConfigured -> "추가 설정 없이 기본 정리로 플랜을 만들 수 있어요."
        providerSettings.isModelVerified -> "기기 안의 분석 파일 실행을 확인했습니다."
        else -> "분석 파일은 저장됐고, 실행 전에는 기본 정리로 진행합니다."
    }
    val modelMessageTitle = modelMessageTitle(providerSettings, modelSettingsState.message)
    val modelMessageTone = if (providerSettings.isModelVerified) {
        ReunionBadgeTone.Success
    } else {
        ReunionBadgeTone.Accent
    }
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
        Text(
            text = "내 이름이 맞으면 플랜이 더 정확해져요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ReunionPane(
            title = "내 카톡 이름",
            supportingText = "카카오톡 대화방에서 보이는 이름 그대로 입력하세요.",
        ) {
            OutlinedTextField(
                value = userDisplayName,
                onValueChange = { userDisplayName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("내 카톡 이름") },
                singleLine = true,
                colors = reunionOutlinedTextFieldColors(),
            )
            ReunionSecondaryButton(
                text = "이름 저장",
                onClick = {
                    providerSettings.nameSaveRequest(userDisplayName).dispatchTo(onSave)
                },
            )
        }
        ReunionPane(
            title = "데이터 보관",
            supportingText = "가져온 대화와 플랜은 이 기기에 보관돼요. 필요 없으면 대화 기록에서 삭제할 수 있어요.",
        )
        ReunionPane(
            title = modelStatusTitle,
            supportingText = if (showTechnicalDetails) {
                modelStatusDescription
            } else {
                "분석 연결과 기기 분석 설정은 여기서만 확인해요."
            },
        ) {
            ReunionBadge(
                text = modelStatusBadge,
                tone = modelStatusTone,
            )
            ReunionSecondaryButton(
                text = if (showTechnicalDetails) "기술 정보 닫기" else "기술 정보 보기",
                onClick = { showTechnicalDetails = !showTechnicalDetails },
            )
            if (showTechnicalDetails && providerSettings.isConfigured) {
                ReunionSecondaryButton(
                    text = when {
                        modelSettingsState.isChecking -> "점검 중..."
                        providerSettings.isModelVerified -> "다시 점검"
                        else -> "실행 점검"
                    },
                    onClick = onVerifyModel,
                    enabled = !modelSettingsState.isChecking && !modelSettingsState.isLoading,
                )
            }
            if (showTechnicalDetails && modelSettingsState.isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            }
            if (showTechnicalDetails) {
                Text(
                    text = if (openRouterConfigured) {
                        "외부 분석 연결이 없을 때 기기 분석 파일을 사용할 수 있어요."
                    } else {
                        "선택한 분석 파일은 앱 안에만 보관됩니다."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ReunionSecondaryButton(
                    text = if (modelSettingsState.isLoading) "파일 저장 중..." else "분석 파일 선택",
                    onClick = { modelPickerLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                    enabled = !modelSettingsState.isLoading && !modelSettingsState.isChecking,
                )
            }
            if (showTechnicalDetails && modelSettingsState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            }
            if (showTechnicalDetails && providerSettings.isConfigured) {
                ReunionSecondaryButton(
                    text = "분석 파일 제거",
                    onClick = {
                        providerSettings.demoModeSaveRequest(userDisplayName).dispatchTo(onSave)
                    },
                )
            }
        }
        if (showTechnicalDetails) {
            modelSettingsState.message?.let { message ->
                ReunionEmptyState(
                    title = modelMessageTitle,
                    body = message,
                    tone = modelMessageTone,
                )
            }
        }
        if (showTechnicalDetails) {
            modelSettingsState.errorMessage?.let { errorMessage ->
                ReunionEmptyState(
                    title = "분석 파일을 확인하지 못했습니다",
                    body = errorMessage,
                    tone = ReunionBadgeTone.Error,
                )
            }
        }
    }
}

internal data class ProviderSettingsSaveRequest(
    val modelPath: String,
    val modelName: String,
    val backend: String,
    val userDisplayName: String,
)

internal fun ProviderSettings.nameSaveRequest(userDisplayName: String): ProviderSettingsSaveRequest {
    return ProviderSettingsSaveRequest(
        modelPath = modelPath,
        modelName = modelName,
        backend = backend.name,
        userDisplayName = userDisplayName,
    )
}

internal fun ProviderSettings.demoModeSaveRequest(userDisplayName: String): ProviderSettingsSaveRequest {
    return ProviderSettingsSaveRequest(
        modelPath = "",
        modelName = modelName,
        backend = backend.name,
        userDisplayName = userDisplayName,
    )
}

internal fun modelMessageTitle(
    providerSettings: ProviderSettings,
    message: String?,
): String {
    return when {
        message?.contains("제거") == true -> "분석 파일 제거됨"
        providerSettings.isModelVerified -> "분석 준비됨"
        else -> "분석 파일 저장됨"
    }
}

private fun ProviderSettingsSaveRequest.dispatchTo(
    onSave: (String, String, String, String) -> Unit,
) {
    onSave(modelPath, modelName, backend, userDisplayName)
}
