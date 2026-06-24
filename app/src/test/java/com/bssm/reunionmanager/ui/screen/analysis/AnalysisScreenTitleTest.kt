package com.bssm.reunionmanager.ui.screen.analysis

import com.bssm.reunionmanager.domain.model.AnalysisReport
import com.bssm.reunionmanager.ui.screen.common.perspectiveNameButtonText
import com.bssm.reunionmanager.ui.screen.common.perspectiveNameOptions
import com.bssm.reunionmanager.ui.theme.ReunionBadgeTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisScreenTitleTest {
    @Test
    fun messageSectionTitle_returnsNoSendTitleWhenContactIsOnHold() {
        val report = report(contactReadiness = "지금은 보류")

        assertEquals("지금은 보류해도 괜찮아요", report.messageSectionTitle())
    }

    @Test
    fun messageSectionTitle_returnsCheckTitleWhenInformationRequiresSetup() {
        val report = report(
            headline = "내 이름 확인",
            contactReadiness = "정보 부족",
            messageDraft = "지금은 보낼 문장을 만들지 않습니다. 내 카톡 이름을 먼저 저장하세요.",
            alternativeDrafts = "내 카톡 이름 저장하기\n같은 대화 다시 분석하기\n최근 대화 파일인지 확인하기",
        )

        assertEquals("먼저 확인할 것", report.messageSectionTitle())
    }

    @Test
    fun messageSectionTitle_returnsCheckTitleWhenInformationIsInsufficientEvenIfDraftExists() {
        val report = report(contactReadiness = "정보 부족")

        assertEquals("먼저 확인할 것", report.messageSectionTitle())
    }

    @Test
    fun messageSectionTitle_returnsReplyTitleWhenCounterpartIsWaiting() {
        val report = report(
            reunionObjective = "새 연락을 시작하기보다 상대가 남긴 말에 답하는 것이 목표입니다.",
            nextStep = "상대의 마지막 메시지에 바로 답하세요.",
        )

        assertEquals("짧은 답장 참고", report.messageSectionTitle())
    }

    @Test
    fun messageSectionTitle_returnsFirstContactTitleByDefault() {
        val report = report()

        assertEquals("필요할 때 참고할 문장", report.messageSectionTitle())
    }

    @Test
    fun alternativeSectionTitle_returnsActionTitleWhenContactIsOnHold() {
        val report = report(contactReadiness = "지금은 보류")

        assertEquals("다음 선택지", report.alternativeSectionTitle())
    }

    @Test
    fun alternativeSectionTitle_returnsActionTitleWhenInformationRequiresSetup() {
        val report = report(
            headline = "내 이름 확인",
            contactReadiness = "정보 부족",
            messageDraft = "지금은 보낼 문장을 만들지 않습니다. 내 카톡 이름을 먼저 저장하세요.",
            alternativeDrafts = "내 카톡 이름 저장하기\n같은 대화 다시 분석하기\n최근 대화 파일인지 확인하기",
        )

        assertEquals("다음 선택지", report.alternativeSectionTitle())
    }

    @Test
    fun alternativeSectionTitle_returnsActionTitleWhenInformationIsInsufficientEvenIfDraftExists() {
        val report = report(contactReadiness = "정보 부족")

        assertEquals("다음 선택지", report.alternativeSectionTitle())
    }

    @Test
    fun alternativeSectionTitle_returnsReplyCandidateTitleWhenCounterpartIsWaiting() {
        val report = report(
            reunionObjective = "새 연락을 시작하기보다 상대가 남긴 말에 답하는 것이 목표입니다.",
            nextStep = "상대의 마지막 메시지에 바로 답하세요.",
        )

        assertEquals("다른 답장 후보", report.alternativeSectionTitle())
    }

    @Test
    fun alternativeSectionTitle_returnsFirstContactCandidateTitleByDefault() {
        val report = report()

        assertEquals("다른 문장 후보", report.alternativeSectionTitle())
    }

    @Test
    fun readinessTone_mapsJudgmentToQuietUiTones() {
        assertEquals(ReunionBadgeTone.Error, report(contactReadiness = "지금은 보류").readinessTone())
        assertEquals(ReunionBadgeTone.Success, report(contactReadiness = "아주 가볍게 가능").readinessTone())
        assertEquals(ReunionBadgeTone.Accent, report(contactReadiness = "먼저 사과 필요").readinessTone())
        assertEquals(ReunionBadgeTone.Neutral, report(contactReadiness = "정보 부족").readinessTone())
    }

    @Test
    fun conclusionHeadline_summarizesActionWithoutExtraDetail() {
        assertEquals(
            "상대가 시간을 요청함",
            report(headline = "상대가 시간을 요청함", contactReadiness = "지금은 보류").conclusionHeadline(),
        )
        assertEquals(
            "부담을 낮추는 구간이에요.",
            report(headline = "", contactReadiness = "지금은 보류").conclusionHeadline(),
        )
        assertEquals(
            "상황을 먼저 읽는 구간이에요.",
            report(headline = "", contactReadiness = "정보 부족").conclusionHeadline(),
        )
        assertEquals(
            "대화를 다시 열 수 있는 구간이에요.",
            report(
                headline = "",
                reunionObjective = "새 연락보다 상대가 남긴 말에 답하는 것이 목표입니다.",
                nextStep = "상대의 마지막 메시지에 바로 답하세요.",
            ).conclusionHeadline(),
        )
        assertEquals(
            "작은 연결을 준비할 수 있어요.",
            report(headline = "").conclusionHeadline(),
        )
    }

    @Test
    fun conclusionHeadline_keepsSpecificApologyHeadline() {
        val report = report(
            headline = "먼저 짧은 인정",
            contactReadiness = "먼저 사과 필요",
            reunionObjective = "상대가 남긴 말에 변명 없이 짧게 인정하는 것이 목표입니다.",
            nextStep = "상대의 마지막 메시지에 답하되 먼저 미안하다고만 전하세요.",
        )

        assertEquals("먼저 짧은 인정", report.conclusionHeadline())
    }

    @Test
    fun summaryTitle_usesSpecificHeadline() {
        val report = report(headline = "상대 답장에 짧게 응답")

        assertEquals("상대 답장에 짧게 응답", report.summaryTitle())
    }

    @Test
    fun summaryBody_keepsOnlyOneFocusedLine() {
        val report = report(
            relationshipSummary = "상대가 마지막에 답장을 남긴 상태라 새 연락보다 짧은 답장이 자연스럽습니다.",
            reunionObjective = "상대의 속도를 존중하면서 안부만 확인합니다.",
            nextStep = "상대의 마지막 메시지에 바로 답하세요.",
        )

        assertEquals(
            "상대가 마지막에 답장을 남긴 상태라 새 연락보다 짧은 답장이 자연스럽습니다.",
            report.summaryBody(),
        )
    }

    @Test
    fun summaryBody_limitsVeryLongText() {
        val report = report(
            relationshipSummary = "가".repeat(200),
            reunionObjective = "나".repeat(200),
        )

        assertEquals(96, report.summaryBody().length)
    }

    @Test
    fun evidenceBody_keepsOnlyThreeShortLines() {
        val report = report(
            evidence = "첫 줄\n둘째 줄\n셋째 줄\n넷째 줄",
        )

        assertEquals("첫 줄\n둘째 줄\n셋째 줄", report.evidenceBody())
    }

    @Test
    fun alternativeDraftsBody_hidesRepeatedMainDraftAndKeepsTwoCandidateMessages() {
        val report = report(
            messageDraft = "메시지 봤어. 나는 잘 지내고 있어. 괜찮다면 천천히 안부 나누자.",
            alternativeDrafts = "메시지 봤어. 나는 잘 지내고 있어. 괜찮다면 천천히 안부 나누자.\n메시지 봤어. 부담 없으면 천천히 답할게.\n고마워. 나도 짧게 안부 전하고 싶었어.",
        )

        assertEquals(
            "메시지 봤어. 부담 없으면 천천히 답할게.\n고마워. 나도 짧게 안부 전하고 싶었어.",
            report.alternativeDraftsBody(),
        )
    }

    @Test
    fun alternativeDraftsBody_keepsThreeActionsForCheckOnlyResults() {
        val report = report(
            contactReadiness = "정보 부족",
            messageDraft = "지금은 보낼 문장을 만들지 않습니다.",
            alternativeDrafts = "내 카톡 이름 저장하기\n같은 대화 다시 분석하기\n최근 대화 파일인지 확인하기",
        )

        assertEquals(3, report.alternativeDraftsBody().lines().size)
    }

    @Test
    fun canCopyMessageDraft_returnsTrueOnlyForActualDrafts() {
        assertTrue(report().canCopyMessageDraft())
        assertTrue(
            report(
                reunionObjective = "새 연락보다 상대가 남긴 말에 답하는 것이 목표입니다.",
                nextStep = "상대의 마지막 메시지에 바로 답하세요.",
            ).canCopyMessageDraft(),
        )
        assertFalse(report(contactReadiness = "지금은 보류").canCopyMessageDraft())
        assertFalse(
            report(
                headline = "내 이름 확인",
                contactReadiness = "정보 부족",
                messageDraft = "지금은 보낼 문장을 만들지 않습니다. 내 카톡 이름을 먼저 저장하세요.",
                alternativeDrafts = "내 카톡 이름 저장하기\n같은 대화 다시 분석하기\n최근 대화 파일인지 확인하기",
            ).canCopyMessageDraft(),
        )
    }

    @Test
    fun planUiText_softensHoldDirectives() {
        val report = report(
            contactReadiness = "지금은 보류",
            nextStep = "오늘은 보내지 말고 최근 대화를 확인하세요.",
            messageDraft = "오늘은 보내지 않습니다. 상대가 먼저 답하면 다시 판단하세요.",
        )

        assertEquals("오늘은 거리를 두고 최근 대화를 확인해보면 좋아요.", report.nextStepBodyForUi())
        assertEquals("지금은 보낼 문장보다 거리 조절이 먼저예요.", report.messageBodyForUi())
    }

    @Test
    fun planUiText_keepsSituationSpecificPlanFields() {
        val report = report(
            reunionObjective = "상대가 시간이 필요하다고 말해 거리 조절을 우선합니다.",
            nextStep = "오늘은 보내지 말고 상대 마지막 말을 다시 읽으세요.",
            caution = "답을 재촉하지 마세요.",
        )

        assertEquals("상대가 시간이 필요하다고 말해 거리 조절을 우선합니다.", report.objectiveBodyForUi())
        assertEquals("오늘은 거리를 두고 상대 마지막 말을 다시 읽어보면 좋아요.", report.nextMoveBodyForUi())
        assertEquals("답을 재촉하지 않아도 괜찮아요.", report.cautionBodyForUi())
    }

    @Test
    fun copySafetyNote_usesOneShortLine() {
        val report = report(
            caution = "답을 재촉하지 마세요.\n상대가 답하지 않으면 기다리세요.",
        )

        assertEquals("보내기 전 한 번 더 쉬어가도 괜찮아요.", report.copySafetyNote())
    }

    @Test
    fun copySafetyNote_limitsLongText() {
        val note = report(caution = "가".repeat(120)).copySafetyNote()

        assertEquals(28, note.length)
        assertTrue(note.endsWith("…"))
    }

    @Test
    fun copySafetyNote_simplifiesBoundaryAndUncertainCautions() {
        assertEquals(
            "상대가 불편하면 쉬어가요.",
            report(caution = "상대가 다시 불편함을 보이면 추가 메시지를 보내지 마세요.").copySafetyNote(),
        )
        assertEquals(
            "확신이 없으면 보내지 않아도 돼요.",
            report(caution = "확신이 부족할 때는 보내지 않고 대화 맥락을 먼저 확인하세요.").copySafetyNote(),
        )
    }

    @Test
    fun copyPromptText_switchesToPostCopyGuardrail() {
        assertEquals(
            "보내기 전 한 번 더 쉬어가도 괜찮아요.",
            copyPromptText(copied = false, safetyNote = "보내기 전 한 번 더 쉬어가도 괜찮아요."),
        )
        assertEquals(
            "복사됐어요. 보내기 전 한 번 더 쉬어가도 괜찮아요.",
            copyPromptText(copied = true, safetyNote = "보내기 전 한 번 더 쉬어가도 괜찮아요."),
        )
    }

    @Test
    fun copyPromptText_preservesContextualSafetyAfterCopy() {
        assertEquals(
            "복사됐어요. 상대가 불편하면 쉬어가요.",
            copyPromptText(copied = true, safetyNote = "상대가 불편하면 쉬어가요."),
        )
        assertEquals(
            "보내기 전 한 번 더 쉬어가도 괜찮아요.",
            copyPromptText(copied = false, safetyNote = ""),
        )
    }

    @Test
    fun analysisGenerateButtonText_separatesInitialDraftFromRegeneration() {
        assertEquals(
            "플랜 만들기",
            analysisGenerateButtonText(providerConfigured = true, hasReport = false),
        )
        assertEquals(
            "플랜 만들기",
            analysisGenerateButtonText(providerConfigured = false, hasReport = false),
        )
        assertEquals(
            "플랜 다시 만들기",
            analysisGenerateButtonText(providerConfigured = true, hasReport = true),
        )
        assertEquals(
            "플랜 다시 만들기",
            analysisGenerateButtonText(providerConfigured = false, hasReport = true),
        )
    }

    @Test
    fun recoveryStageIndex_mapsReadinessToRecoveryMap() {
        assertEquals(0, report(contactReadiness = "정보 부족").recoveryStageIndex())
        assertEquals(1, report(contactReadiness = "지금은 보류").recoveryStageIndex())
        assertEquals(1, report(contactReadiness = "먼저 사과 필요").recoveryStageIndex())
        assertEquals(2, report(contactReadiness = "아주 가볍게 가능").recoveryStageIndex())
        assertEquals("대화 열기", report(contactReadiness = "아주 가볍게 가능").recoveryStageLabel())
    }

    @Test
    fun recoveryEvidenceItems_keepCurrentNodeBubbleShortAndConcrete() {
        val holdItems = report(contactReadiness = "지금은 보류").recoveryEvidenceItems()

        assertEquals(
            listOf(
                RecoveryEvidenceItem("상대 반응", "조심"),
                RecoveryEvidenceItem("연락 부담", "높음"),
                RecoveryEvidenceItem("대화 여지", "낮음"),
            ),
            holdItems,
        )

        val openItems = report(
            reunionObjective = "상대가 남긴 말에 답하는 것이 목표입니다.",
            nextStep = "상대의 마지막 메시지에 짧게 답하세요.",
        ).recoveryEvidenceItems()

        assertEquals("짧게 열림", openItems.first().value)
        assertEquals("낮음", openItems[1].value)
        assertEquals("있음", openItems[2].value)
    }

    @Test
    fun needsPerspectiveSetupForAnalysis_detectsMissingOrMismatchedName() {
        val participants = listOf("민지", "현우")

        assertTrue(needsPerspectiveSetupForAnalysis(participants, userDisplayName = ""))
        assertTrue(needsPerspectiveSetupForAnalysis(participants, userDisplayName = "현우님"))
        assertFalse(needsPerspectiveSetupForAnalysis(participants, userDisplayName = "현우"))
        assertFalse(needsPerspectiveSetupForAnalysis(listOf("현우"), userDisplayName = ""))
    }

    @Test
    fun perspectiveSetupSupportingText_explainsMismatchedNameWithoutExtraDetail() {
        assertEquals(
            "내 카톡 이름을 저장한 뒤 분석하세요.",
            perspectiveSetupSupportingText(participantNames = listOf("민지", "현우"), userDisplayName = ""),
        )
        assertEquals(
            "저장한 이름이 이 대화에 없어요. 카카오톡에 보이는 이름으로 고친 뒤 분석하세요.",
            perspectiveSetupSupportingText(participantNames = listOf("민지", "현우"), userDisplayName = "현우님"),
        )
    }

    @Test
    fun perspectiveNameOptions_keepsSimpleParticipantPickerSmall() {
        assertEquals(
            listOf("민지", "현우"),
            perspectiveNameOptions(listOf(" 민지 ", "현우", "현우", "")),
        )
        assertEquals(
            emptyList<String>(),
            perspectiveNameOptions(listOf("A", "B", "C", "D", "E")),
        )
    }

    @Test
    fun perspectiveNameButtonText_keepsActionDirect() {
        assertEquals("현우 선택", perspectiveNameButtonText(" 현우 "))
    }

    private fun report(
        headline: String = "테스트",
        contactReadiness: String = "아주 가볍게 가능",
        evidence: String = "테스트 근거",
        relationshipSummary: String = "테스트 요약",
        reunionObjective: String = "가벼운 안부로 반응 가능성만 확인합니다.",
        nextStep: String = "짧은 한 문장만 준비하세요.",
        messageDraft: String = "오랜만이야. 잘 지내?",
        alternativeDrafts: String = "오랜만이야\n잘 지내?\n답은 천천히 해도 괜찮아",
        caution: String = "답을 재촉하지 마세요.",
    ): AnalysisReport {
        return AnalysisReport(
            headline = headline,
            contactReadiness = contactReadiness,
            evidence = evidence,
            relationshipSummary = relationshipSummary,
            reunionObjective = reunionObjective,
            nextStep = nextStep,
            messageDraft = messageDraft,
            alternativeDrafts = alternativeDrafts,
            caution = caution,
        )
    }
}
