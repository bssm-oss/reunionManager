package com.bssm.reunionmanager.ui.screen.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.bssm.reunionmanager.domain.model.AnalysisReport
import com.bssm.reunionmanager.domain.model.ConversationDetail
import com.bssm.reunionmanager.ui.AnalysisUiState
import com.bssm.reunionmanager.ui.theme.ReunionBadge
import com.bssm.reunionmanager.ui.theme.ReunionBadgeTone
import com.bssm.reunionmanager.ui.theme.ReunionEmptyState
import com.bssm.reunionmanager.ui.theme.ReunionPane
import com.bssm.reunionmanager.ui.theme.ReunionPrimaryButton
import com.bssm.reunionmanager.ui.theme.ReunionSecondaryButton
import com.bssm.reunionmanager.ui.theme.ScreenPadding
import com.bssm.reunionmanager.ui.theme.ScreenSectionSpacing

@Composable
fun AnalysisScreen(
    detail: ConversationDetail?,
    analysisState: AnalysisUiState?,
    userDisplayNameConfigured: Boolean = true,
    providerConfigured: Boolean = true,
    onGenerate: () -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    val needsPerspectiveSetup = (detail?.participantNames?.size ?: 0) >= 2 && !userDisplayNameConfigured
    val report = detail?.latestAnalysis
    val clipboardManager = LocalClipboardManager.current
    var showDetails by remember(report) { mutableStateOf(false) }
    var copiedDraft by remember(report) { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = ScreenPadding, vertical = ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(ScreenSectionSpacing),
    ) {
        item {
            Text(
                text = detail?.title ?: "대화를 불러오는 중",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        item {
            if (analysisState?.isRunning == true) {
                ReunionEmptyState(
                    title = if (providerConfigured) "다음 행동을 정리하는 중" else "데모로 정리하는 중",
                    body = "저장한 대화에서 부담 없는 한 걸음을 고르고 있습니다.",
                    tone = ReunionBadgeTone.Accent,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
            } else if (needsPerspectiveSetup) {
                ReunionPane(
                    title = "내 이름 확인",
                    supportingText = "내 카톡 이름을 저장한 뒤 분석하세요.",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    ReunionPrimaryButton(
                        text = "내 카톡 이름 설정하기",
                        onClick = onOpenSettings,
                    )
                }
            } else {
                ReunionPrimaryButton(
                    text = analysisGenerateButtonText(
                        providerConfigured = providerConfigured,
                        hasReport = report != null,
                    ),
                    onClick = onGenerate,
                    enabled = detail != null,
                )
            }
        }
        analysisState?.errorMessage?.let { errorMessage ->
            item {
                ReunionEmptyState(
                    title = "계획을 만들지 못했습니다",
                    body = errorMessage,
                    tone = ReunionBadgeTone.Error,
                )
            }
        }
        report?.let { currentReport ->
            val messageTitle = currentReport.messageSectionTitle()
            item { AnalysisConclusionPane(currentReport) }
            item {
                AnalysisMessagePane(
                    title = messageTitle,
                    body = currentReport.messageDraft,
                    canCopy = currentReport.canCopyMessageDraft(),
                    safetyNote = currentReport.copySafetyNote(),
                    copied = copiedDraft,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(currentReport.messageDraft))
                        copiedDraft = true
                    },
                )
            }
            item {
                ReunionSecondaryButton(
                    text = if (showDetails) "상세 닫기" else "판단 근거 보기",
                    onClick = { showDetails = !showDetails },
                )
            }
            if (showDetails) {
                item {
                    AnalysisSectionPane(
                        title = currentReport.summaryTitle(),
                        body = currentReport.summaryBody(),
                    )
                }
                if (currentReport.alternativeDrafts.isNotBlank()) {
                    item { AnalysisSectionPane(title = currentReport.alternativeSectionTitle(), body = currentReport.alternativeDrafts) }
                }
                item { AnalysisSectionPane(title = "판단 근거", body = currentReport.evidenceBody()) }
                item {
                    AnalysisSectionPane(
                        title = "주의할 점",
                        body = currentReport.caution,
                        tone = ReunionBadgeTone.Error,
                    )
                }
            }
        } ?: run {
            if (!needsPerspectiveSetup) {
                item {
                    ReunionEmptyState(
                        title = "아직 정리한 내용이 없습니다",
                        body = "준비되면 다음 행동만 차분히 정리해 보세요.",
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalysisConclusionPane(report: AnalysisReport) {
    val tone = report.readinessTone()
    val containerColor = when (tone) {
        ReunionBadgeTone.Error -> MaterialTheme.colorScheme.errorContainer
        ReunionBadgeTone.Success -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
        ReunionBadgeTone.Accent -> MaterialTheme.colorScheme.primaryContainer
        ReunionBadgeTone.Neutral -> MaterialTheme.colorScheme.surface
    }

    ReunionPane(
        title = "오늘의 결론",
        containerColor = containerColor,
    ) {
        ReunionBadge(text = report.contactReadiness, tone = tone)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = report.conclusionHeadline(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = report.nextStep.limitForUi(maxLength = 96),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AnalysisSectionPane(
    title: String,
    body: String,
    tone: ReunionBadgeTone = ReunionBadgeTone.Neutral,
) {
    val containerColor = if (tone == ReunionBadgeTone.Error) {
        MaterialTheme.colorScheme.errorContainer
    } else if (tone == ReunionBadgeTone.Accent) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    ReunionPane(
        title = title,
        supportingText = body,
        containerColor = containerColor,
    )
}

internal fun AnalysisReport.messageSectionTitle(): String {
    return when {
        contactReadiness == "지금은 보류" -> "보내지 않기"
        contactReadiness == "정보 부족" -> "확인할 일"
        isCheckOnlyResult() -> "확인할 일"
        nextStep.contains("상대의 마지막 메시지") || reunionObjective.contains("상대가 남긴 말") -> "답장 문장"
        else -> "첫 연락 문장"
    }
}

internal fun AnalysisReport.readinessTone(): ReunionBadgeTone {
    return when (contactReadiness) {
        "지금은 보류" -> ReunionBadgeTone.Error
        "아주 가볍게 가능" -> ReunionBadgeTone.Success
        "먼저 사과 필요" -> ReunionBadgeTone.Accent
        else -> ReunionBadgeTone.Neutral
    }
}

internal fun AnalysisReport.conclusionHeadline(): String {
    return when {
        contactReadiness == "지금은 보류" -> "오늘은 보내지 않는 쪽이 안전합니다."
        contactReadiness == "정보 부족" || isCheckOnlyResult() -> "먼저 확인할 정보가 있습니다."
        messageSectionTitle() == "답장 문장" -> "새 연락보다 짧은 답장이 자연스럽습니다."
        contactReadiness == "먼저 사과 필요" -> "재회보다 짧은 인정이 먼저입니다."
        else -> "짧고 부담 없는 한 문장만 준비하세요."
    }
}

internal fun AnalysisReport.alternativeSectionTitle(): String {
    return when {
        contactReadiness == "지금은 보류" || contactReadiness == "정보 부족" || isCheckOnlyResult() -> "다음 선택지"
        messageSectionTitle() == "답장 문장" -> "다른 답장 후보"
        else -> "다른 문장 후보"
    }
}

private fun AnalysisReport.isCheckOnlyResult(): Boolean {
    return messageDraft.contains("보낼 문장을 만들지 않습니다") ||
        alternativeDrafts.contains("내 카톡 이름 저장하기") ||
        alternativeDrafts.contains("대화가 1:1 개인 관계인지 확인하기") ||
        headline.contains("관계 맥락") ||
        headline.contains("내 이름 확인")
}

internal fun AnalysisReport.canCopyMessageDraft(): Boolean {
    return messageSectionTitle() == "답장 문장" || messageSectionTitle() == "첫 연락 문장"
}

internal fun AnalysisReport.copySafetyNote(): String {
    return caution.lineSequence()
        .map { line -> line.trim() }
        .firstOrNull { line -> line.isNotBlank() }
        .orEmpty()
        .ifBlank { "답이 없어도 다시 보내지 마세요." }
        .limitForUi(maxLength = 44)
}

internal fun analysisGenerateButtonText(providerConfigured: Boolean, hasReport: Boolean): String {
    return when {
        hasReport -> "다시 정리하기"
        providerConfigured -> "다음 행동 정리하기"
        else -> "데모로 정리하기"
    }
}

internal fun copyPromptText(copied: Boolean, safetyNote: String): String {
    return if (copied) {
        "복사 후: 답장이 없어도 추가로 보내지 마세요."
    } else {
        "보내기 전: $safetyNote"
    }
}

@Composable
private fun AnalysisMessagePane(
    title: String,
    body: String,
    canCopy: Boolean,
    safetyNote: String,
    copied: Boolean,
    onCopy: () -> Unit,
) {
    ReunionPane(
        title = title,
        supportingText = body,
    ) {
        if (canCopy) {
            Text(
                text = copyPromptText(copied = copied, safetyNote = safetyNote),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ReunionSecondaryButton(
                text = if (copied) "복사됨" else "문장 복사",
                onClick = onCopy,
            )
        }
    }
}

internal fun AnalysisReport.summaryTitle(): String {
    return headline.trim().ifBlank { "요약" }.limitForUi(maxLength = 32)
}

internal fun AnalysisReport.summaryBody(): String {
    val lines = listOf(
        relationshipSummary.trim(),
        reunionObjective.trim().takeIf { it.isNotBlank() }?.let { "목표: $it" }.orEmpty(),
    ).filter { it.isNotBlank() }
    return lines.joinToString(separator = "\n")
        .ifBlank { "최근 대화 흐름을 기준으로 다음 행동만 간단히 정리했습니다." }
        .limitForUi(maxLength = 180)
}

internal fun AnalysisReport.evidenceBody(): String {
    return evidence.lineSequence()
        .map { line -> line.trim() }
        .filter { line -> line.isNotBlank() }
        .take(3)
        .joinToString(separator = "\n")
        .ifBlank { "최근 대화 흐름과 마지막 발신자 기준으로 판단했습니다." }
        .limitForUi(maxLength = 220)
}

private fun String.limitForUi(maxLength: Int): String {
    return if (length <= maxLength) this else take(maxLength - 1).trimEnd() + "…"
}
