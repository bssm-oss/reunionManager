package com.bssm.reunionmanager.ui.screen.analysis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bssm.reunionmanager.domain.model.AnalysisReport
import com.bssm.reunionmanager.domain.model.ConversationDetail
import com.bssm.reunionmanager.ui.AnalysisUiState
import com.bssm.reunionmanager.ui.screen.common.needsPerspectiveSetup as needsPerspectiveSetupCommon
import com.bssm.reunionmanager.ui.screen.common.perspectiveNameButtonText
import com.bssm.reunionmanager.ui.screen.common.perspectiveNameOptions
import com.bssm.reunionmanager.ui.screen.common.perspectiveSetupSupportingText as perspectiveSetupSupportingTextCommon
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
    userDisplayName: String = "",
    providerConfigured: Boolean = true,
    onGenerate: () -> Unit,
    onOpenCalendar: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onSaveUserDisplayName: (String) -> Unit = {},
) {
    val needsPerspectiveSetup = detail?.participantNames
        ?.let { participantNames -> needsPerspectiveSetupForAnalysis(participantNames, userDisplayName) }
        ?: false
    val perspectiveOptions = perspectiveNameOptions(detail?.participantNames.orEmpty())
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
                    title = "플랜을 만드는 중",
                    body = "카톡 내용에서 다시 가까워질 단서를 찾고 있어요.",
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
                    supportingText = perspectiveSetupSupportingText(
                        participantNames = detail?.participantNames.orEmpty(),
                        userDisplayName = userDisplayName,
                    ),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    perspectiveOptions.forEach { participantName ->
                        ReunionSecondaryButton(
                            text = perspectiveNameButtonText(participantName),
                            onClick = { onSaveUserDisplayName(participantName) },
                        )
                    }
                    val directInputButtonText = "직접 입력하기"
                    if (perspectiveOptions.isEmpty()) {
                        ReunionPrimaryButton(
                            text = directInputButtonText,
                            onClick = onOpenSettings,
                        )
                    } else {
                        ReunionSecondaryButton(
                            text = directInputButtonText,
                            onClick = onOpenSettings,
                        )
                    }
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
                    title = "플랜을 만들지 못했습니다",
                    body = errorMessage,
                    tone = ReunionBadgeTone.Error,
                )
            }
        }
        report?.let { currentReport ->
            val messageTitle = currentReport.messageSectionTitle()
            item { AnalysisConclusionPane(currentReport) }
            item {
                WeeklyPlanPane(
                    report = currentReport,
                    onOpenCalendar = onOpenCalendar,
                )
            }
            item { RecoveryMapPane(currentReport) }
            item {
                AnalysisMessagePane(
                    title = messageTitle,
                    body = currentReport.messageBodyForUi(),
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
                    text = if (showDetails) "근거 닫기" else "근거 보기",
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
                val alternativeDraftsBody = currentReport.alternativeDraftsBody()
                if (alternativeDraftsBody.isNotBlank()) {
                    item {
                        AnalysisSectionPane(
                            title = currentReport.alternativeSectionTitle(),
                            body = alternativeDraftsBody,
                        )
                    }
                }
                item { AnalysisSectionPane(title = "대화 신호", body = currentReport.evidenceBody()) }
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
                        title = "아직 플랜이 없습니다",
                        body = "카톡 내용으로 회복 맵을 열어보세요.",
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyPlanPane(
    report: AnalysisReport,
    onOpenCalendar: () -> Unit,
) {
    val planItems = report.weeklyPlanItems().take(3)
    ReunionPane(
        title = "이번 주 플랜",
        supportingText = "하루에 하나씩만 확인해도 충분해요.",
    ) {
        planItems.forEach { item ->
            PlannerDayPreviewRow(item)
        }
        ReunionSecondaryButton(
            text = "달력으로 보기",
            onClick = onOpenCalendar,
        )
    }
}

@Composable
private fun PlannerDayPreviewRow(item: PlannerDayItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = if (item.isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = item.dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.isToday) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = item.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecoveryMapPane(report: AnalysisReport) {
    val activeStageIndex = report.recoveryStageIndex()

    ReunionPane(
        title = "재회 회복 맵",
        supportingText = "지금은 ${report.recoveryStageLabel()} 구간이에요.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RecoveryStageLabels.forEachIndexed { index, label ->
                RecoveryStageRow(
                    label = label,
                    index = index,
                    activeStageIndex = activeStageIndex,
                    evidenceItems = if (index == activeStageIndex) {
                        report.recoveryEvidenceItems()
                    } else {
                        emptyList()
                    },
                )
            }
        }
    }
}

@Composable
private fun RecoveryStageRow(
    label: String,
    index: Int,
    activeStageIndex: Int,
    evidenceItems: List<RecoveryEvidenceItem>,
) {
    val isActive = index == activeStageIndex
    val isPast = index < activeStageIndex
    val nodeColor = when {
        isActive -> MaterialTheme.colorScheme.primary
        isPast -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isActive || isPast) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val xOffset = if (index % 2 == 0) 0.dp else 26.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = xOffset),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(if (isActive) 52.dp else 42.dp),
            shape = CircleShape,
            color = nodeColor,
            border = BorderStroke(3.dp, MaterialTheme.colorScheme.surface),
            shadowElevation = if (isActive) 5.dp else 1.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (isActive) "지금" else (index + 1).toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = label,
                style = if (isActive) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                color = if (isActive) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (evidenceItems.isNotEmpty()) {
                EvidenceBubble(evidenceItems)
            }
        }
    }
}

@Composable
private fun EvidenceBubble(items: List<RecoveryEvidenceItem>) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
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
        title = "내 플랜",
        containerColor = containerColor,
    ) {
        ReunionBadge(text = report.recoveryStageLabel(), tone = tone)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = report.conclusionHeadline(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = report.summaryBody(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PlanPointRow(label = "목표", body = report.objectiveBodyForUi())
        PlanPointRow(label = "다음 선택", body = report.nextMoveBodyForUi())
        PlanPointRow(label = "지켜둘 선", body = report.cautionBodyForUi())
    }
}

@Composable
private fun PlanPointRow(
    label: String,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
        contactReadiness == "지금은 보류" -> "오늘은 쉬어가도 괜찮아요"
        contactReadiness == "정보 부족" -> "먼저 확인할 점"
        isCheckOnlyResult() -> "먼저 확인할 점"
        nextStep.contains("상대의 마지막 메시지") || reunionObjective.contains("상대가 남긴 말") -> "짧은 답장 참고"
        else -> "필요할 때 참고할 문장"
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
    return headline.trim()
        .softenedForPlanUi()
        .limitForUi(maxLength = 36)
        .ifBlank {
            when {
                contactReadiness == "지금은 보류" -> "부담을 낮추는 구간이에요."
                contactReadiness == "정보 부족" || isCheckOnlyResult() -> "상황을 먼저 읽는 구간이에요."
                contactReadiness == "먼저 사과 필요" -> "조심스럽게 낮추는 구간이에요."
                messageSectionTitle() == "짧은 답장 참고" -> "대화를 다시 열 수 있는 구간이에요."
                else -> "작은 연결을 준비할 수 있어요."
            }
        }
}

internal fun AnalysisReport.alternativeSectionTitle(): String {
    return when {
        contactReadiness == "지금은 보류" || contactReadiness == "정보 부족" || isCheckOnlyResult() -> "다음 선택"
        messageSectionTitle() == "짧은 답장 참고" -> "다른 답장 후보"
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
    return messageSectionTitle() == "짧은 답장 참고" || messageSectionTitle() == "필요할 때 참고할 문장"
}

internal fun AnalysisReport.nextStepBodyForUi(): String {
    return nextStep
        .softenedForPlanUi()
        .ifBlank { "카톡 내용을 기준으로 지금 할 수 있는 작은 움직임을 정리했어요." }
        .limitForUi(maxLength = 96)
}

internal fun AnalysisReport.objectiveBodyForUi(): String {
    return reunionObjective
        .softenedForPlanUi()
        .ifBlank { "상대에게 부담을 주지 않는 선에서 관계 온도를 확인해요." }
        .limitForUi(maxLength = 88)
}

internal fun AnalysisReport.nextMoveBodyForUi(): String {
    return nextStepBodyForUi()
}

internal fun AnalysisReport.cautionBodyForUi(): String {
    return caution
        .softenedForPlanUi()
        .ifBlank { "답을 재촉하지 않고 상대의 속도를 존중해요." }
        .limitForUi(maxLength = 88)
}

internal fun AnalysisReport.messageBodyForUi(): String {
    return when {
        canCopyMessageDraft() -> messageDraft.softenedForPlanUi().limitForUi(maxLength = 140)
        contactReadiness == "지금은 보류" -> "지금은 연락 문장보다 거리 조절이 먼저예요."
        contactReadiness == "정보 부족" || isCheckOnlyResult() -> "대화가 맞는지 확인하면 더 정확한 플랜을 만들 수 있어요."
        else -> messageDraft.softenedForPlanUi().limitForUi(maxLength = 140)
    }
}

internal fun AnalysisReport.copySafetyNote(): String {
    val firstLine = caution.lineSequence()
        .map { line -> line.trim() }
        .firstOrNull { line -> line.isNotBlank() }
        .orEmpty()

    return when {
        firstLine.isBlank() -> "보내기 전 한 번 더 쉬어가도 괜찮아요."
        firstLine.containsAny("불편", "경계", "연락하지", "멈") -> "상대가 불편하면 쉬어가요."
        firstLine.containsAny("확신", "부족", "확인") -> "확신이 없으면 보내지 않아도 돼요."
        firstLine.containsAny("답", "재촉", "추가", "기다", "속도") -> "보내기 전 한 번 더 쉬어가도 괜찮아요."
        else -> firstLine.limitForUi(maxLength = 28)
    }
}

internal fun analysisGenerateButtonText(providerConfigured: Boolean, hasReport: Boolean): String {
    return when {
        hasReport -> "플랜 다시 만들기"
        providerConfigured -> "플랜 만들기"
        else -> "플랜 만들기"
    }
}

internal fun needsPerspectiveSetupForAnalysis(
    participantNames: List<String>,
    userDisplayName: String,
): Boolean {
    return needsPerspectiveSetupCommon(participantNames, userDisplayName)
}

internal fun perspectiveSetupSupportingText(
    participantNames: List<String>,
    userDisplayName: String,
): String {
    return perspectiveSetupSupportingTextCommon(participantNames, userDisplayName)
}

internal fun copyPromptText(copied: Boolean, safetyNote: String): String {
    val note = safetyNote.ifBlank { "보내기 전 한 번 더 쉬어가도 괜찮아요." }
    return if (copied) {
        "복사됐어요. $note"
    } else {
        note
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
                text = if (copied) "복사됨" else "참고 문장 복사",
                onClick = onCopy,
            )
        }
    }
}

internal fun AnalysisReport.summaryTitle(): String {
    return headline.trim().ifBlank { "요약" }.limitForUi(maxLength = 32)
}

internal fun AnalysisReport.summaryBody(): String {
    return relationshipSummary.trim()
        .softenedForPlanUi()
        .ifBlank { "최근 카톡 내용을 기준으로 회복 단계를 정리했습니다." }
        .limitForUi(maxLength = 96)
}

internal fun AnalysisReport.evidenceBody(): String {
    return evidence.lineSequence()
        .map { line -> line.trim() }
        .filter { line -> line.isNotBlank() }
        .take(3)
        .joinToString(separator = "\n")
        .ifBlank { "최근 카톡 내용과 마지막 발신자 기준으로 판단했습니다." }
        .limitForUi(maxLength = 220)
}

internal fun AnalysisReport.alternativeDraftsBody(): String {
    val mainDraft = messageDraft.trim()
    val maxItems = if (alternativeSectionTitle() == "다음 선택") 3 else 2
    return alternativeDrafts.lineSequence()
        .map { line -> line.trim() }
        .filter { line -> line.isNotBlank() && line != mainDraft }
        .distinct()
        .take(maxItems)
        .joinToString(separator = "\n") { line -> line.limitForUi(maxLength = 54) }
}

internal data class RecoveryEvidenceItem(
    val label: String,
    val value: String,
)

internal data class PlannerDayItem(
    val dayLabel: String,
    val title: String,
    val body: String,
    val isToday: Boolean = false,
)

internal val RecoveryStageLabels = listOf(
    "상황 읽기",
    "부담 낮추기",
    "대화 열기",
    "감정 회복",
    "만남 준비",
)

internal fun AnalysisReport.recoveryStageIndex(): Int {
    return when (contactReadiness) {
        "정보 부족" -> 0
        "지금은 보류" -> 1
        "먼저 사과 필요" -> 1
        "아주 가볍게 가능" -> 2
        else -> 0
    }
}

internal fun AnalysisReport.recoveryStageLabel(): String {
    return RecoveryStageLabels[recoveryStageIndex()]
}

internal fun AnalysisReport.recoveryEvidenceItems(): List<RecoveryEvidenceItem> {
    return listOf(
        RecoveryEvidenceItem("상대 반응", counterpartReactionValue()),
        RecoveryEvidenceItem("연락 부담", contactBurdenValue()),
        RecoveryEvidenceItem("대화 여지", conversationOpeningValue()),
    )
}

internal fun AnalysisReport.weeklyPlanItems(): List<PlannerDayItem> {
    return when (contactReadiness) {
        "지금은 보류" -> listOf(
            PlannerDayItem("오늘", "거리 두기", "추가 연락 없이 마지막 흐름을 다시 봐요.", isToday = true),
            PlannerDayItem("내일", "부담 신호 체크", "상대가 불편했던 표현과 내가 반복한 말을 표시해요."),
            PlannerDayItem("3일", "내 마음 정리", "보내고 싶은 말보다 내가 원하는 관계의 속도를 적어요."),
            PlannerDayItem("4일", "새 반응 확인", "상대가 먼저 남긴 말이 있을 때만 다음 단계를 봐요."),
            PlannerDayItem("5일", "문장 보류", "아직 반응이 없으면 연락 문장은 그대로 쉬어둬요."),
            PlannerDayItem("6일", "대화 다시 읽기", "최근 흐름이 바뀌었는지 조용히 확인해요."),
            PlannerDayItem("7일", "플랜 다시 보기", "새 대화가 생기면 플랜을 다시 만들어요."),
        )
        "먼저 사과 필요" -> listOf(
            PlannerDayItem("오늘", "인정할 부분 정리", "내가 책임질 수 있는 한 문장만 적어요.", isToday = true),
            PlannerDayItem("내일", "변명 덜어내기", "설명과 요구를 빼고 사과 문장을 짧게 다듬어요."),
            PlannerDayItem("3일", "답 요구 빼기", "상대가 답하지 않아도 괜찮은 문장인지 확인해요."),
            PlannerDayItem("4일", "상대 속도 보기", "반응이 있으면 같은 속도로만 이어가요."),
            PlannerDayItem("5일", "감정 정리", "다시 만나자는 말보다 미안했던 지점을 정리해요."),
            PlannerDayItem("6일", "대화 여지 확인", "상대가 열어둔 주제가 있는지 다시 봐요."),
            PlannerDayItem("7일", "다음 단계 보기", "대화가 부드러워졌을 때만 다음 코스를 확인해요."),
        )
        "아주 가볍게 가능" -> {
            val replyLike = nextStep.contains("상대의 마지막 메시지") ||
                reunionObjective.contains("상대가 남긴 말")
            listOf(
                PlannerDayItem(
                    "오늘",
                    if (replyLike) "짧은 답장 준비" else "짧은 연결 준비",
                    if (replyLike) "상대가 남긴 말에 한 문장만 이어요." else "안부나 확인을 한 문장으로 줄여요.",
                    isToday = true,
                ),
                PlannerDayItem("내일", "반응 기다리기", "답장이 오면 같은 온도로만 이어가요."),
                PlannerDayItem("3일", "대화 여지 보기", "상대가 편하게 받은 주제만 남겨요."),
                PlannerDayItem("4일", "감정 회복 주제", "가벼웠던 기억이나 일상 얘기만 확인해요."),
                PlannerDayItem("5일", "속도 맞추기", "상대 답장 간격보다 빠르게 몰아가지 않아요."),
                PlannerDayItem("6일", "다음 대화 준비", "부담 없는 질문 하나만 남겨둬요."),
                PlannerDayItem("7일", "만남은 천천히", "대화가 자연스럽게 이어질 때만 다음 단계를 봐요."),
            )
        }
        else -> listOf(
            PlannerDayItem("오늘", "내 이름 확인", "대화방에서 내가 어떤 이름인지 먼저 맞춰요.", isToday = true),
            PlannerDayItem("내일", "대화 맥락 보기", "1:1 관계 대화가 맞는지 확인해요."),
            PlannerDayItem("3일", "최근 파일 확인", "더 최근 카톡 내용이 있으면 그 파일을 불러와요."),
            PlannerDayItem("4일", "상대 반응 찾기", "상대가 마지막에 남긴 말과 경계 표현을 봐요."),
            PlannerDayItem("5일", "연락 부담 보기", "내가 연속으로 보낸 메시지가 많은지 확인해요."),
            PlannerDayItem("6일", "플랜 다시 만들기", "관점이 맞으면 같은 대화로 플랜을 다시 만들어요."),
            PlannerDayItem("7일", "회복 코스 열기", "충분한 대화가 모이면 다음 단계를 확인해요."),
        )
    }
}

private fun AnalysisReport.counterpartReactionValue(): String {
    return when {
        contactReadiness == "정보 부족" || isCheckOnlyResult() -> "확인 필요"
        contactReadiness == "지금은 보류" -> "조심"
        contactReadiness == "먼저 사과 필요" -> "조심"
        nextStep.contains("상대의 마지막 메시지") || reunionObjective.contains("상대가 남긴 말") -> "짧게 열림"
        else -> "있음"
    }
}

private fun AnalysisReport.contactBurdenValue(): String {
    return when (contactReadiness) {
        "정보 부족" -> "확인 필요"
        "지금은 보류" -> "높음"
        "먼저 사과 필요" -> "높음"
        "아주 가볍게 가능" -> "낮음"
        else -> "조심"
    }
}

private fun AnalysisReport.conversationOpeningValue(): String {
    return when (contactReadiness) {
        "정보 부족" -> "확인 필요"
        "지금은 보류" -> "낮음"
        "먼저 사과 필요" -> "조심"
        "아주 가볍게 가능" -> "있음"
        else -> "조심"
    }
}

private fun String.limitForUi(maxLength: Int): String {
    return if (length <= maxLength) this else take(maxLength - 1).trimEnd() + "…"
}

private fun String.softenedForPlanUi(): String {
    return replace("상대의 마지막 메시지에 바로 답하세요.", "상대의 마지막 메시지에 짧게 답해볼 수 있어요.")
        .replace("오늘은 보내지 않기", "오늘은 거리 두기")
        .replace("오늘은 연락을 쉬기", "오늘은 연락 쉬기")
        .replace("오늘은 보내지 말고,", "오늘은 거리를 두고,")
        .replace("오늘은 보내지 말고", "오늘은 거리를 두고")
        .replace("오늘은 보내지 않습니다.", "오늘은 거리를 두는 쪽이 안정적이에요.")
        .replace("오늘은 연락을 쉬는 쪽이 안정적입니다.", "오늘은 연락을 쉬는 쪽이 안정적이에요.")
        .replace("바로 답하세요.", "짧게 열어둘 수 있어요.")
        .replace("답장하세요.", "답장해볼 수 있어요.")
        .replace("바로 답하되", "짧게 이어가되")
        .replace("전하세요.", "남겨볼 수 있어요.")
        .replace("정리해보세요.", "정리해봐요.")
        .replace("정리하세요.", "정리해보면 좋아요.")
        .replace("확인해보세요.", "확인해봐요.")
        .replace("확인하세요.", "확인해보면 좋아요.")
        .replace("준비하세요.", "준비해볼 수 있어요.")
        .replace("읽어보세요.", "읽어봐요.")
        .replace("읽으세요.", "읽어보면 좋아요.")
        .replace("기다리세요.", "기다려도 괜찮아요.")
        .replace("마세요.", "않아도 괜찮아요.")
}

private fun String.containsAny(vararg values: String): Boolean {
    return values.any { value -> contains(value) }
}
