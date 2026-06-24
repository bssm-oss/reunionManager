package com.bssm.reunionmanager.ui.screen.importing

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.bssm.reunionmanager.ui.ImportUiState
import com.bssm.reunionmanager.ui.theme.ReunionBadgeTone
import com.bssm.reunionmanager.ui.theme.ReunionEmptyState
import com.bssm.reunionmanager.ui.theme.ReunionPrimaryButton
import com.bssm.reunionmanager.ui.theme.ReunionSecondaryButton
import com.bssm.reunionmanager.ui.theme.ScreenPadding
import com.bssm.reunionmanager.ui.theme.ScreenSectionSpacing

@Composable
fun ImportScreen(
    importState: ImportUiState,
    onImportClick: (Uri) -> Unit,
    onOpenPlanClick: (Long) -> Unit,
) {
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(onImportClick)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(ScreenSectionSpacing),
    ) {
        Text(
            text = "카카오톡 내보내기 파일을 선택하세요. 대화는 기기에만 저장됩니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ReunionPrimaryButton(
            text = if (importState.isLoading) "가져오는 중..." else "대화 파일 선택",
            onClick = {
                pickerLauncher.launch(
                    arrayOf(
                        "text/plain",
                        "text/csv",
                        "application/csv",
                        "application/vnd.ms-excel",
                        "*/*",
                    ),
                )
            },
            enabled = !importState.isLoading,
        )

        if (importState.isLoading) {
            ReunionEmptyState(
                title = "가져오는 중",
                body = "선택한 파일을 이 기기에서 읽고 있습니다.",
                tone = ReunionBadgeTone.Accent,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            }
        }

        importState.message?.let { message ->
            ReunionEmptyState(
                title = "가져오기 완료",
                body = message,
                tone = ReunionBadgeTone.Success,
            ) {
                importState.importedConversationId?.let { conversationId ->
                    ReunionSecondaryButton(
                        text = "다음 행동 정리하기",
                        onClick = { onOpenPlanClick(conversationId) },
                    )
                }
            }
        }

        importState.errorMessage?.let { errorMessage ->
            ReunionEmptyState(
                title = "가져오지 못했습니다",
                body = errorMessage,
                tone = ReunionBadgeTone.Error,
            )
        }
    }
}
