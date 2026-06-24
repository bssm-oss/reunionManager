package com.bssm.reunionmanager.ui.screen.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bssm.reunionmanager.R
import com.bssm.reunionmanager.domain.model.ConversationSummary
import com.bssm.reunionmanager.ui.theme.ReunionBadge
import com.bssm.reunionmanager.ui.theme.ReunionBadgeTone
import com.bssm.reunionmanager.ui.theme.ReunionPane
import com.bssm.reunionmanager.ui.theme.ReunionPrimaryButton
import com.bssm.reunionmanager.ui.theme.ReunionSecondaryButton
import com.bssm.reunionmanager.ui.theme.ScreenPadding
import com.bssm.reunionmanager.ui.theme.ScreenSectionSpacing

@Composable
fun HomeScreen(
    conversations: List<ConversationSummary>,
    onImportClick: () -> Unit,
    onOpenPlanClick: (Long) -> Unit,
    onOpenCalendarClick: (Long) -> Unit,
) {
    val latestPlan = conversations.latestPlannedConversation()
    val latestConversation = latestPlan ?: conversations.maxByOrNull { conversation -> conversation.importedAtEpochMillis }

    if (latestConversation == null) {
        EmptyHome(onImportClick = onImportClick)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = ScreenPadding, vertical = ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(ScreenSectionSpacing),
    ) {
        item {
            PlannerHero(
                title = if (latestPlan == null) "첫 플랜을\n만들어볼까요" else "오늘의\n재회 플랜",
                body = if (latestPlan == null) {
                    "카톡 내용을 바탕으로 내 상황에 맞는 회복 순서를 만들어요."
                } else {
                    "무리하지 않고 다시 가까워질 순서를 이어가요."
                },
            )
        }
        if (latestPlan == null) {
            item {
                ReunionPane(
                    title = "가장 최근 대화",
                    supportingText = latestConversation.title,
                ) {
                    Text(
                        text = "플랜을 만들면 오늘 할 일과 이번 주 흐름을 여기서 바로 볼 수 있어요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ReunionPrimaryButton(
                        text = "플랜 만들기",
                        onClick = { onOpenPlanClick(latestConversation.id) },
                    )
                }
            }
        } else {
            item {
                TodayPlanPane(
                    conversation = latestPlan,
                    onOpenPlanClick = { onOpenPlanClick(latestPlan.id) },
                    onOpenCalendarClick = { onOpenCalendarClick(latestPlan.id) },
                )
            }
            item { WeekPreviewPane(latestPlan) }
            item { RecoveryCoursePane(latestPlan) }
        }
        item {
            ReunionSecondaryButton(
                text = "카톡 내용 더 불러오기",
                onClick = onImportClick,
            )
        }
    }
}

@Composable
private fun EmptyHome(
    onImportClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(ScreenPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            PlannerLogo(modifier = Modifier.size(116.dp))
            Text(
                text = "재회 가능성을\n높이는 플랜",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "카톡 내용을 보고 다시 가까워질 길을 정리해요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
            ReunionPrimaryButton(
                text = "카톡 내용 불러오기",
                onClick = onImportClick,
            )
        }
    }
}

@Composable
private fun PlannerHero(
    title: String,
    body: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlannerLogo(modifier = Modifier.size(82.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlannerLogo(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
    ) {
        Image(
            painter = painterResource(id = R.drawable.reunion_logo_mark),
            contentDescription = "재회 플랜 로고",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun TodayPlanPane(
    conversation: ConversationSummary,
    onOpenPlanClick: () -> Unit,
    onOpenCalendarClick: () -> Unit,
) {
    ReunionPane(
        title = "오늘 할 일",
        supportingText = conversation.homeTodayBody(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReunionBadge(
                text = conversation.homeStageLabel(),
                tone = conversation.homeTone(),
            )
            ReunionBadge(text = "1개만")
        }
        Text(
            text = conversation.homeTodayTitle(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        ReunionPrimaryButton(
            text = "플랜 이어보기",
            onClick = onOpenPlanClick,
        )
        ReunionSecondaryButton(
            text = "이번 주 보기",
            onClick = onOpenCalendarClick,
        )
    }
}

@Composable
private fun WeekPreviewPane(conversation: ConversationSummary) {
    ReunionPane(
        title = "이번 주",
        supportingText = "하루에 하나씩만 봐도 충분해요.",
    ) {
        conversation.homeWeekItems().forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = if (item.day == "오늘") {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = item.day,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.day == "오늘") {
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecoveryCoursePane(conversation: ConversationSummary) {
    val activeIndex = conversation.homeStageIndex()
    ReunionPane(
        title = "회복 코스",
        supportingText = "${HomeRecoveryStages[activeIndex]} 구간을 지나고 있어요.",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            HomeRecoveryStages.forEachIndexed { index, label ->
                CourseNode(
                    label = label,
                    index = index,
                    activeIndex = activeIndex,
                )
            }
        }
    }
}

@Composable
private fun CourseNode(
    label: String,
    index: Int,
    activeIndex: Int,
) {
    val isActive = index == activeIndex
    val isPast = index < activeIndex
    val nodeColor = when {
        isActive -> MaterialTheme.colorScheme.primary
        isPast -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            modifier = Modifier.size(if (isActive) 38.dp else 32.dp),
            shape = CircleShape,
            color = nodeColor,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = (index + 1).toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isActive || isPast) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal data class HomeWeekItem(
    val day: String,
    val title: String,
    val body: String,
)

internal val HomeRecoveryStages = listOf(
    "상황\n읽기",
    "부담\n낮추기",
    "대화\n열기",
    "감정\n회복",
    "만남\n준비",
)

internal fun List<ConversationSummary>.latestPlannedConversation(): ConversationSummary? {
    return filter { conversation -> conversation.latestAnalysisHeadline != null }
        .maxByOrNull { conversation ->
            conversation.latestAnalysisCreatedAtEpochMillis ?: conversation.importedAtEpochMillis
        }
}

internal fun ConversationSummary.homeStageIndex(): Int {
    return when (latestAnalysisContactReadiness) {
        "정보 부족" -> 0
        "지금은 보류" -> 1
        "먼저 사과 필요" -> 1
        "아주 가볍게 가능" -> 2
        else -> 0
    }
}

internal fun ConversationSummary.homeStageLabel(): String {
    return HomeRecoveryStages[homeStageIndex()].replace("\n", " ")
}

internal fun ConversationSummary.homeTone(): ReunionBadgeTone {
    return when (latestAnalysisContactReadiness) {
        "지금은 보류" -> ReunionBadgeTone.Error
        "아주 가볍게 가능" -> ReunionBadgeTone.Success
        "먼저 사과 필요" -> ReunionBadgeTone.Accent
        else -> ReunionBadgeTone.Neutral
    }
}

internal fun ConversationSummary.homeTodayTitle(): String {
    return when (latestAnalysisContactReadiness) {
        "지금은 보류" -> "연락보다 회복 리듬 잡기"
        "먼저 사과 필요" -> "사과 문장 한 줄 정리"
        "아주 가볍게 가능" -> "짧은 연결 준비"
        "정보 부족" -> "상황 먼저 확인"
        else -> latestAnalysisHeadline?.ifBlank { null } ?: "오늘의 플랜 보기"
    }
}

internal fun ConversationSummary.homeTodayBody(): String {
    return latestAnalysisNextStep
        ?.softenedForHome()
        ?.limitForHome(maxLength = 74)
        ?.takeIf { text -> text.isNotBlank() }
        ?: when (latestAnalysisContactReadiness) {
            "지금은 보류" -> "오늘은 상대 반응과 내 마음을 차분히 다시 읽는 날이에요."
            "먼저 사과 필요" -> "변명 없이 인정할 부분만 짧게 정리해요."
            "아주 가볍게 가능" -> "길게 설명하지 않고 한 문장만 준비해요."
            else -> "대화가 내 상황에 맞는지 먼저 확인해요."
        }
}

internal fun ConversationSummary.homeWeekItems(): List<HomeWeekItem> {
    return when (latestAnalysisContactReadiness) {
        "지금은 보류" -> listOf(
            HomeWeekItem("오늘", "거리 두기", "추가 연락 없이 마지막 흐름만 봐요."),
            HomeWeekItem("내일", "부담 신호 체크", "상대가 불편했던 표현을 표시해요."),
            HomeWeekItem("3일", "플랜 다시 보기", "새 반응이 있을 때만 다음 단계를 봐요."),
        )
        "먼저 사과 필요" -> listOf(
            HomeWeekItem("오늘", "인정할 부분 정리", "내가 책임질 수 있는 한 줄만 남겨요."),
            HomeWeekItem("내일", "문장 덜어내기", "설명과 요구를 빼고 짧게 다듬어요."),
            HomeWeekItem("3일", "반응 기다리기", "답을 재촉하지 않는 흐름을 유지해요."),
        )
        "아주 가볍게 가능" -> listOf(
            HomeWeekItem("오늘", "짧은 연결 준비", "안부나 답장을 한 문장으로 줄여요."),
            HomeWeekItem("내일", "상대 반응 보기", "답장이 오면 같은 속도로만 이어가요."),
            HomeWeekItem("3일", "대화 여지 확인", "가볍게 이어지는 주제만 남겨요."),
        )
        else -> listOf(
            HomeWeekItem("오늘", "내 이름 확인", "대화방에서 내가 어떤 이름인지 맞춰요."),
            HomeWeekItem("내일", "대화 흐름 보기", "최근 대화가 충분한지 확인해요."),
            HomeWeekItem("3일", "플랜 다시 만들기", "관점이 맞으면 플랜을 다시 열어요."),
        )
    }
}

private fun String.limitForHome(maxLength: Int): String {
    return if (length <= maxLength) this else take(maxLength - 1).trimEnd() + "…"
}

private fun String.softenedForHome(): String {
    return replace("오늘은 연락을 쉬고,", "오늘은 연락을 쉬면서")
        .replace("오늘은 보내지 말고,", "오늘은 거리를 두고")
        .replace("오늘은 보내지 말고", "오늘은 거리를 두고")
        .replace("바로 답하되", "짧게 이어가되")
        .replace("준비하세요.", "준비해요.")
        .replace("확인하세요.", "확인해요.")
        .replace("정리하세요.", "정리해요.")
        .replace("읽어보세요.", "읽어봐요.")
        .replace("보세요.", "봐요.")
        .replace("마세요.", "않아도 괜찮아요.")
}
