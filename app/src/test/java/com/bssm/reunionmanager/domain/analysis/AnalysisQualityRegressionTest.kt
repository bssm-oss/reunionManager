package com.bssm.reunionmanager.domain.analysis

import com.bssm.reunionmanager.domain.model.AnalysisInput
import com.bssm.reunionmanager.domain.model.AnalysisReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisQualityRegressionTest {
    @Test
    fun finalizeReport_handlesRealisticKakaoQualityCases() {
        val cases = listOf(
            qualityCase("name missing", missingPerspective(), "정보 부족", "내 카톡 이름", "오랜만이야"),
            qualityCase("user sent three unanswered messages", userUnansweredRun(), "지금은 보류", "보내지 않습니다", "오랜만이야"),
            qualityCase("user sent two unanswered messages", userUnansweredRun(count = 2), "지금은 보류", "보내지 않습니다", "오랜만이야"),
            qualityCase("counterpart says do not contact", counterpartBoundary("이제 연락하지 말아줘"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart asks no more contact", counterpartBoundary("연락 안 했으면 좋겠어"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart says stop", counterpartBoundary("그만 보내줘"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart asks to delete phone number", counterpartBoundary("내 번호 지워줘"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart asks to delete contact", counterpartBoundary("연락처 삭제해줘"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart says leaving kakao room", counterpartBoundary("카톡방 나갈게"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart says chat room is left", counterpartBoundary("채팅방 나왔어"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart threatens report", counterpartBoundary("계속 이러면 신고할 거야"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart feels afraid", counterpartBoundary("네 연락 오는 거 무서워"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart calls it stalking", counterpartBoundary("이건 스토킹처럼 느껴져"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart says uncomfortable", counterpartBoundary("이런 연락은 불편해"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart says not okay", counterpartBoundary("지금은 괜찮지 않아"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart moved on", counterpartBoundary("나 새로 만나는 사람 있어"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart has new partner", counterpartBoundary("나 남자친구 생겼어"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart is seeing someone else", counterpartBoundary("다른 사람 만나고 있어"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart says each move on", counterpartBoundary("우리 이제 각자 잘 지내자"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart asks friend only", counterpartBoundary("친구로 지내는 게 좋겠어"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart says no romantic feeling", counterpartBoundary("연애 감정은 없어"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart says no feelings", counterpartBoundary("이제 마음이 없어"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart says no lingering feeling", counterpartBoundary("미련 없어"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart will contact later", counterpartBoundary("나중에 내가 연락할게"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart asks time", counterpartBoundary("생각할 시간이 필요해"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("counterpart asks to wait", counterpartBoundary("조금 기다려줘"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("relationship cleanup", counterpartBoundary("우리 관계는 정리하자"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("breakup", counterpartBoundary("이제 헤어지자"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("final end", counterpartBoundary("우리 끝이야"), "지금은 보류", "보내지 않습니다", "안부"),
            qualityCase("no pressure phrase", counterpartReply("부담 없으면 천천히 답해도 돼"), "아주 가볍게 가능", "메시지 봤어", "오랜만이야"),
            qualityCase("not trying to pressure", counterpartReply("부담 주려는 건 아니고 잘 지내는지만 궁금했어"), "아주 가볍게 가능", "메시지 봤어", "오랜만이야"),
            qualityCase("well-being reply", counterpartReply("잘 지내?"), "아주 가볍게 가능", "나는 잘 지내고 있어", "오랜만이야"),
            qualityCase("counterpart says good night", counterpartReply("잘 자"), "아주 가볍게 가능", "너도 잘 자", "안부"),
            qualityCase("counterpart says good work", counterpartReply("오늘 수고했어"), "아주 가볍게 가능", "너도 수고했어", "안부"),
            qualityCase("counterpart says get home safe", counterpartReply("조심히 들어가"), "아주 가볍게 가능", "조심히 들어가", "안부"),
            qualityCase("counterpart asks meal check", counterpartReply("밥 먹었어?"), "아주 가볍게 가능", "챙겨 먹었어", "안부"),
            qualityCase("schedule question", counterpartReply("토요일 저녁에 시간 돼?"), "아주 가볍게 가능", "가능한지 확인", "약속한 시간"),
            qualityCase("concrete meeting", counterpartReply("내일 7시에 카페에서 보자"), "아주 가볍게 가능", "약속한 시간", "안부부터"),
            qualityCase("counterpart asks why now", counterpartReply("왜 이제 와?"), "먼저 사과 필요", "미안", "안부"),
            qualityCase("counterpart questions sudden contact", counterpartReply("갑자기 왜 연락해?"), "먼저 사과 필요", "미안", "안부"),
            qualityCase("user drunk late night message", userImpairedTiming("술 마셔서 그런지 보고 싶어"), "지금은 보류", "보내지 않습니다", "오랜만이야"),
            qualityCase("user midnight impulse", userImpairedTiming("새벽에 잠이 안 와서 연락했어"), "지금은 보류", "보내지 않습니다", "오랜만이야"),
            qualityCase("user asks why no reply", userUnansweredPressure("왜 답이 없어?"), "지금은 보류", "보내지 않습니다", "오랜만이야"),
            qualityCase("user calls read ignored", userUnansweredPressure("읽씹이야?"), "지금은 보류", "보내지 않습니다", "오랜만이야"),
            qualityCase("personal signals in group chat", personalGroup(), "정보 부족", "보낼 문장을 만들지 않습니다", "오랜만이야"),
            qualityCase("technical group chat", technicalGroup(), "정보 부족", "보낼 문장을 만들지 않습니다", "오랜만이야"),
            qualityCase("two-person work chat", technicalTwoPerson(), "정보 부족", "보낼 문장을 만들지 않습니다", "오랜만이야"),
            qualityCase("light positive signal", lightPositive(), "아주 가볍게 가능", "짧게", "보내지 않습니다"),
        )

        assertEquals(48, cases.size)
        cases.forEach { case ->
            val report = AnalysisSafetyRules.finalizeReport(optimisticGemmaReport, case.input)

            assertEquals(case.name, case.expectedReadiness, report.contactReadiness)
            assertTrue(case.name, report.messageDraft.contains(case.messageMustContain))
            case.messageMustNotContain?.let { forbidden ->
                assertFalse(case.name, report.messageDraft.contains(forbidden))
            }
            assertFalse(case.name, report.messageDraft.contains("당장"))
            assertFalse(case.name, report.messageDraft.contains("집 앞"))
        }
    }

    @Test
    fun finalizeReport_keepsMessageDraftAsSingleCopyableLine() {
        val rawReport = optimisticGemmaReport.copy(
            messageDraft = """
                추천 문장:
                "오랜만이야. 잘 지내?"
                - 답은 천천히 해도 괜찮아.
            """.trimIndent(),
            caution = "집 앞에 찾아가서 당장 확인하세요.",
        )

        val report = AnalysisSafetyRules.finalizeReport(rawReport, lightPositive())

        assertEquals("오랜만이야. 잘 지내?", report.messageDraft)
        assertFalse(report.messageDraft.contains("\n"))
        assertEquals("답을 재촉하지 말고 상대의 속도를 존중하세요.", report.caution)
    }

    @Test
    fun finalizeReport_replacesHighPressureModelGuidance() {
        val rawReport = optimisticGemmaReport.copy(
            contactReadiness = "아주 가볍게 가능",
            reunionObjective = "다시 만나자고 설득해서 관계를 회복합니다.",
            nextStep = "지금 바로 집 앞에 찾아가 다시 시작하자고 말하세요.",
            messageDraft = "보고 싶어. 다시 시작하자. 한 번만 기회 줘.",
            alternativeDrafts = """
                사랑해. 다시 만나자.
                집 앞에서 기다릴게.
                오랜만이야. 잘 지내?
            """.trimIndent(),
            caution = "선물을 들고 직접 만나러 가세요.",
        )

        val report = AnalysisSafetyRules.finalizeReport(rawReport, lightPositive())

        assertEquals("아주 가볍게 가능", report.contactReadiness)
        assertEquals("가벼운 안부 한 문장으로 상대의 현재 온도를 확인하는 것이 목표입니다.", report.reunionObjective)
        assertEquals("긴 설명을 보내지 말고 최근 대화를 한 번 읽은 뒤 짧은 한 문장만 준비하세요.", report.nextStep)
        assertEquals("오랜만이야. 갑자기 부담 주려는 건 아니고, 괜찮다면 짧게 안부만 묻고 싶어.", report.messageDraft)
        assertEquals("답을 재촉하지 말고 상대의 속도를 존중하세요.", report.caution)
        assertFalse(report.alternativeDrafts.contains("사랑"))
        assertFalse(report.alternativeDrafts.contains("집 앞"))
        assertFalse(report.alternativeDrafts.contains("기회"))
    }

    @Test
    fun finalizeReport_replacesUnansweredPressureModelOutput() {
        val rawReport = optimisticGemmaReport.copy(
            contactReadiness = "아주 가볍게 가능",
            reunionObjective = "왜 답이 없는지 확인합니다.",
            nextStep = "읽씹이냐고 짧게 물어보세요.",
            messageDraft = "왜 답이 없어? 읽씹이야?",
            alternativeDrafts = """
                왜 답이 없어?
                읽씹이야?
                오랜만이야. 잘 지내?
            """.trimIndent(),
            caution = "답장 안 하면 다시 확인하세요.",
        )

        val report = AnalysisSafetyRules.finalizeReport(rawReport, lightPositive())

        assertEquals("아주 가볍게 가능", report.contactReadiness)
        assertEquals("가벼운 안부 한 문장으로 상대의 현재 온도를 확인하는 것이 목표입니다.", report.reunionObjective)
        assertEquals("긴 설명을 보내지 말고 최근 대화를 한 번 읽은 뒤 짧은 한 문장만 준비하세요.", report.nextStep)
        assertEquals("오랜만이야. 갑자기 부담 주려는 건 아니고, 괜찮다면 짧게 안부만 묻고 싶어.", report.messageDraft)
        assertEquals("답을 재촉하지 말고 상대의 속도를 존중하세요.", report.caution)
        assertFalse(report.alternativeDrafts.contains("왜 답"))
        assertFalse(report.alternativeDrafts.contains("읽씹"))
    }

    @Test
    fun finalizeReport_keepsEverydayCounterpartRepliesLowPressure() {
        val report = AnalysisSafetyRules.finalizeReport(
            optimisticGemmaReport.copy(
                messageDraft = "오랜만이야. 잘 지냈어?",
                alternativeDrafts = "오랜만이야\n잘 지내?\n잠깐 얘기할 수 있어?",
            ),
            counterpartReply("잘 자"),
        )

        assertEquals("응, 너도 잘 자.", report.messageDraft)
        assertTrue(report.alternativeDrafts.contains("편히 쉬어"))
        assertFalse(report.messageDraft.contains("오랜만"))
        assertFalse(report.alternativeDrafts.contains("안부 전하고 싶었어"))
    }

    private data class QualityCase(
        val name: String,
        val input: AnalysisInput,
        val expectedReadiness: String,
        val messageMustContain: String,
        val messageMustNotContain: String?,
    )

    private fun qualityCase(
        name: String,
        input: AnalysisInput,
        expectedReadiness: String,
        messageMustContain: String,
        messageMustNotContain: String? = null,
    ): QualityCase {
        return QualityCase(
            name = name,
            input = input,
            expectedReadiness = expectedReadiness,
            messageMustContain = messageMustContain,
            messageMustNotContain = messageMustNotContain,
        )
    }

    private fun missingPerspective(): AnalysisInput {
        return input(
            recentExcerpt = "현우: 오랜만이야\n민지: 나도 가끔 생각났어",
            signalExcerpt = "민지: 나도 가끔 생각났어",
            perspectiveSummary = "내 카톡 이름: 설정되지 않음\n마지막 메시지 발신자 역할: 알 수 없음\n관점 주의: 내 카톡 이름이 설정되지 않아 마지막 발신자가 사용자인지 상대인지 확정할 수 없습니다.",
        )
    }

    private fun counterpartBoundary(message: String): AnalysisInput {
        return input(
            recentExcerpt = "현우: 잠깐 이야기할 수 있을까?\n민지: $message",
            signalExcerpt = "민지: $message",
            perspectiveSummary = configuredPerspective("상대", counterpartFinalRun = 1, counterpartRecent = message),
        )
    }

    private fun userUnansweredRun(count: Int = 3): AnalysisInput {
        val recentLines = listOf(
            "현우: 혹시 잠깐 괜찮아?",
            "현우: 답 없어서 다시 남겨",
            "현우: 미안해. 오늘은 더 보내지 않을게",
        ).take(count)
        return input(
            recentExcerpt = recentLines.joinToString(separator = "\n"),
            signalExcerpt = recentLines.last(),
            perspectiveSummary = configuredPerspective("나", myFinalRun = count),
        )
    }

    private fun userImpairedTiming(message: String): AnalysisInput {
        return input(
            recentExcerpt = "민지: 잘 지내?\n현우: $message",
            signalExcerpt = "현우: $message",
            perspectiveSummary = configuredPerspective("나", myFinalRun = 1),
        )
    }

    private fun userUnansweredPressure(message: String): AnalysisInput {
        return input(
            recentExcerpt = "민지: 잘 지내?\n현우: $message",
            signalExcerpt = "현우: $message",
            perspectiveSummary = configuredPerspective("나", myFinalRun = 1),
        )
    }

    private fun counterpartReply(message: String): AnalysisInput {
        return input(
            recentExcerpt = "현우: 괜찮다면 짧게 이야기할 수 있을까?\n민지: $message",
            signalExcerpt = "민지: $message",
            perspectiveSummary = configuredPerspective("상대", counterpartFinalRun = 1, counterpartRecent = message),
        )
    }

    private fun technicalGroup(): AnalysisInput {
        return input(
            participants = listOf("현우", "민지", "준호"),
            recentExcerpt = "준호: RAG 테스트 결과 공유할게\n민지: LLM 모델 API 응답이 느려",
            signalExcerpt = "",
            perspectiveSummary = configuredPerspective("상대", counterpartFinalRun = 1, counterpartRecent = "LLM 모델 API 응답이 느려"),
        )
    }

    private fun personalGroup(): AnalysisInput {
        return input(
            participants = listOf("현우", "민지", "준호"),
            recentExcerpt = "준호: 다들 잘 지내?\n민지: 나도 가끔 생각났어",
            signalExcerpt = "민지: 나도 가끔 생각났어",
            perspectiveSummary = configuredPerspective("상대", counterpartFinalRun = 1, counterpartRecent = "나도 가끔 생각났어"),
        )
    }

    private fun technicalTwoPerson(): AnalysisInput {
        return input(
            recentExcerpt = "현우: 배포 로그 봤어?\n민지: API 테스트 자료 먼저 볼게",
            signalExcerpt = "",
            perspectiveSummary = configuredPerspective("상대", counterpartFinalRun = 1, counterpartRecent = "API 테스트 자료 먼저 볼게"),
        )
    }

    private fun lightPositive(): AnalysisInput {
        return input(
            recentExcerpt = "민지: 나도 가끔 생각났어\n현우: 잘 지내는지 궁금했어",
            signalExcerpt = "민지: 나도 가끔 생각났어",
            perspectiveSummary = configuredPerspective("나", myFinalRun = 1),
        )
    }

    private fun configuredPerspective(
        lastSenderRole: String,
        myFinalRun: Int = if (lastSenderRole == "나") 1 else 0,
        counterpartFinalRun: Int = if (lastSenderRole == "상대") 1 else 0,
        counterpartRecent: String = "나도 가끔 생각났어",
    ): String {
        return """
            내 카톡 이름: 현우
            상대 후보: 민지
            마지막 메시지 발신자 역할: $lastSenderRole
            마지막 연속 발화 역할: $lastSenderRole ${maxOf(myFinalRun, counterpartFinalRun)}개
            내 최근 메시지: 오랜만이야
            상대 최근 메시지: $counterpartRecent
            내 마지막 연속 발화: ${myFinalRun}개
            상대 마지막 연속 발화: ${counterpartFinalRun}개
        """.trimIndent()
    }

    private fun input(
        participants: List<String> = listOf("민지", "현우"),
        recentExcerpt: String,
        signalExcerpt: String,
        perspectiveSummary: String,
    ): AnalysisInput {
        val lastMessage = recentExcerpt.lineSequence().lastOrNull()?.substringAfter(": ") ?: ""
        return AnalysisInput(
            conversationTitle = "카카오톡 대화",
            participantNames = participants,
            messageCount = 12,
            excerpt = recentExcerpt,
            recentExcerpt = recentExcerpt,
            signalExcerpt = signalExcerpt,
            statsSummary = "마지막 메시지: $lastMessage\n마지막 발신자의 연속 발화: 1개\n마지막 메시지 이후 경과: 알 수 없음",
            perspectiveSummary = perspectiveSummary,
        )
    }

    private companion object {
        val optimisticGemmaReport = AnalysisReport(
            headline = "가벼운 안부",
            contactReadiness = "아주 가볍게 가능",
            evidence = "모델이 가볍게 가능하다고 판단했습니다.",
            relationshipSummary = "대화가 완전히 닫히지 않았다고 보았습니다.",
            reunionObjective = "짧은 안부로 반응을 확인합니다.",
            nextStep = "지금 짧게 연락하세요.",
            messageDraft = "오랜만이야. 괜찮다면 짧게 안부만 묻고 싶어.",
            alternativeDrafts = "오랜만이야\n잘 지내?\n잠깐 얘기할 수 있어?",
            caution = "답을 재촉하지 마세요.",
        )
    }
}
