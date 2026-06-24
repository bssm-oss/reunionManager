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
            text = "카톡 내용을 불러오면 내 상황에 맞는 재회 플랜을 만들 수 있어요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ReunionPrimaryButton(
            text = if (importState.isLoading) "불러오는 중..." else "카톡 내용 불러오기",
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
                title = "카톡 내용을 읽는 중",
                body = "상대 반응과 연락 부담을 정리하고 있어요.",
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
                title = "카톡 내용을 불러왔어요",
                body = message,
                tone = ReunionBadgeTone.Success,
            ) {
                importState.importedConversationId?.let { conversationId ->
                    ReunionSecondaryButton(
                        text = "플랜 보기",
                        onClick = { onOpenPlanClick(conversationId) },
                    )
                }
            }
        }

        importState.errorMessage?.let { errorMessage ->
            ReunionEmptyState(
                title = "불러오지 못했습니다",
                body = errorMessage,
                tone = ReunionBadgeTone.Error,
            )
        }
    }
}
