package com.bssm.reunionmanager.domain.analysis

import com.bssm.reunionmanager.domain.model.AnalysisInput
import com.bssm.reunionmanager.domain.model.AnalysisReport

object AnalysisSafetyRules {
    private const val HOLD = "지금은 보류"
    private const val APOLOGY = "먼저 사과 필요"
    private const val LIGHT_CONTACT = "아주 가볍게 가능"
    private const val UNKNOWN = "정보 부족"
    private const val MAX_DRAFT_LENGTH = 90
    private val allowedReadiness = listOf(HOLD, APOLOGY, LIGHT_CONTACT, UNKNOWN)
    private val unsafeDraftPhrases = listOf(
        "여러 번",
        "계속 보내",
        "답을 받아",
        "답을 요구",
        "답을 재촉",
        "재촉해",
        "집 앞",
        "찾아가",
        "당장",
    )
    private val hardBoundaryPhrases = listOf(
        "연락하지",
        "답장하지",
        "차단",
        "그만",
        "싫어",
        "하지마",
        "하지 마",
        "괜찮지 않아",
        "끝이야",
        "끝내자",
        "끝났",
        "각자 잘 지내",
        "각자 지내",
        "친구로 지내",
        "연애 감정 없",
        "좋은 사람 만났",
        "새로 만나는 사람",
        "새로운 사람",
        "만나는 사람 있어",
        "나중에 내가 연락",
        "내가 연락할게",
        "시간이 필요",
        "생각할 시간이 필요",
        "기다려줘",
        "정리하자",
        "정리하고 싶",
        "관계 정리",
        "헤어지자",
        "헤어졌",
    )
    private val softBoundaryExceptions = listOf(
        "부담 없",
        "부담없",
        "부담 없이",
        "부담없이",
        "부담 주려는 건 아니",
        "부담 주려는건 아니",
        "부담 가지지",
        "부담 느끼지 않아도",
        "불편하지 않",
        "불편한 건 아니",
        "불편한건 아니",
    )
    private val burdenBoundaryPatterns = listOf(
        Regex("부담\\s*(돼|되|스럽|스러|이야|이에요|입니다|느껴|감)"),
        Regex("부담[이은을도]?\\s*(돼|되|느껴)"),
    )
    private val discomfortBoundaryPatterns = listOf(
        Regex("불편\\s*(해|하|했|합니다|해요|해서|한|해져)"),
    )
    private val strongPersonalSignalPhrases = listOf(
        "미안",
        "사과",
        "보고싶",
        "보고 싶",
        "생각났",
        "잘 지내",
        "잘지내",
        "안부",
        "부담",
        "불편",
        "힘들",
        "헤어",
        "정리하자",
        "끝내자",
        "연락하지",
        "답장하지",
        "차단",
    )
    private val technicalOrTransactionalPhrases = listOf(
        "llm",
        "rag",
        "langchain",
        "openai",
        "api",
        "임베딩",
        "모델",
        "코드",
        "버그",
        "테스트",
        "배포",
        "프로젝트",
        "회의",
        "업무",
        "자료",
        "과제",
        "계약",
        "결제",
        "견적",
    )
    private val scheduleTimePhrases = listOf(
        "오늘",
        "내일",
        "모레",
        "이번 주",
        "이번주",
        "다음 주",
        "다음주",
        "주말",
        "월요일",
        "화요일",
        "수요일",
        "목요일",
        "금요일",
        "토요일",
        "일요일",
        "오전",
        "오후",
        "시",
        "분",
    )
    private val scheduleActionPhrases = listOf(
        "보자",
        "만나",
        "볼까",
        "만날까",
        "카페",
        "밥",
        "점심",
        "저녁",
        "약속",
    )

    fun requiresHold(input: AnalysisInput): Boolean {
        return hasBoundarySignal(input) ||
            userFinalRunCount(input) >= 3 ||
            unknownFinalRunCount(input) >= 3
    }

    fun hasWeakReunionContext(input: AnalysisInput): Boolean {
        if (hasStrongPersonalSignal(input)) {
            return false
        }
        return input.participantNames.size > 2 || looksTechnicalOrTransactional(input)
    }

    fun needsUserPerspective(input: AnalysisInput): Boolean {
        return input.participantNames.size >= 2 &&
            input.perspectiveSummary.contains("내 카톡 이름: 설정되지 않음")
    }

    fun sanitizeReport(report: AnalysisReport, input: AnalysisInput? = null): AnalysisReport {
        val readiness = report.contactReadiness.normalizeReadiness()
        return report.copy(
            contactReadiness = readiness,
            headline = report.headline.trim().ifBlank { "다시 연락하기 전 확인할 점" },
            evidence = report.evidence.trim().ifBlank { "대화 근거가 부족해 최근 흐름을 더 확인해야 합니다." },
            relationshipSummary = report.relationshipSummary.trim().ifBlank { "대화 흐름을 단정하지 말고 천천히 확인하세요." },
            reunionObjective = report.reunionObjective.trim().ifBlank { defaultObjective(readiness, input) },
            nextStep = report.nextStep.trim().ifBlank { defaultNextStep(readiness, input) },
            messageDraft = sanitizeDraft(readiness, report.messageDraft, input),
            alternativeDrafts = sanitizeAlternatives(readiness, report.alternativeDrafts),
            caution = report.caution.trim().ifBlank { "답을 재촉하지 말고 상대의 속도를 존중하세요." },
        )
    }

    fun guardrailEvidence(input: AnalysisInput): String {
        return listOfNotNull(
            "규칙 보정: 경계 신호나 무응답 위험이 있어 보류로 조정했습니다.",
            input.perspectiveSummary.lineSequence().firstOrNull { it.startsWith("마지막 메시지 발신자 역할:") },
            input.perspectiveSummary.lineSequence().firstOrNull { it.startsWith("내 마지막 연속 발화:") },
            firstBoundaryLine(input)?.let { "신호: $it" },
        ).joinToString(separator = "\n")
    }

    fun weakContextEvidence(input: AnalysisInput): String {
        return listOfNotNull(
            "맥락 확인: 재회 판단에 필요한 개인 관계 신호가 부족합니다.",
            "참여자 수: ${input.participantNames.size}명",
            input.statsSummary.lineSequence().firstOrNull { line -> line.startsWith("마지막 메시지:") },
            input.perspectiveSummary.lineSequence().firstOrNull { line -> line.startsWith("상대 최근 메시지:") },
        ).joinToString(separator = "\n")
    }

    fun perspectiveSetupEvidence(input: AnalysisInput): String {
        return listOfNotNull(
            "관점 확인: 내 카톡 이름이 없어 마지막 발신자가 나인지 상대인지 확정하지 못했습니다.",
            input.statsSummary.lineSequence().firstOrNull { line -> line.startsWith("마지막 메시지:") },
            input.perspectiveSummary.lineSequence().firstOrNull { line -> line.startsWith("마지막 메시지 발신자 역할:") },
        ).joinToString(separator = "\n")
    }

    fun counterpartFinalRunCount(input: AnalysisInput): Int {
        return input.perspectiveSummary.extractCountAfter("상대 마지막 연속 발화:")
    }

    fun counterpartReplyDraft(input: AnalysisInput, candidate: String? = null): String {
        val trimmedCandidate = candidate?.trim().orEmpty()
        if (trimmedCandidate.isSafeCounterpartReply()) {
            return trimmedCandidate
        }

        val lastMessage = input.lastRecentMessageContent()
        val opening = replyOpening(input)
        return when {
            containsAny(lastMessage, "잘 지내", "잘지내") -> {
                "${opening}나는 잘 지내고 있어. 괜찮다면 천천히 안부 나누자."
            }
            containsAny(lastMessage, "생각났", "보고 싶", "보고싶") -> {
                "${opening}나도 가끔 생각났어. 괜찮다면 짧게 안부부터 이야기해도 될까?"
            }
            containsAny(lastMessage, "고마워", "고맙") -> {
                "${opening}고마워. 부담 없으면 천천히 이야기해도 괜찮아."
            }
            containsAny(lastMessage, "미안", "사과") -> {
                "말해줘서 고마워. 나도 차분히 듣고 싶어. 부담 없으면 천천히 이야기하자."
            }
            lastMessage.hasConcreteScheduleSignal() -> {
                "${opening}좋아, 약속한 시간에 맞춰 갈게. 고마워."
            }
            lastMessage.hasScheduleQuestionSignal() -> {
                "${opening}좋아, 가능한지 확인해서 시간 맞춰볼게."
            }
            containsAny(lastMessage, "천천히", "괜찮다면", "이야기", "얘기", "대화", "통화") -> {
                "${opening}고마워. 나도 부담 없이 천천히 이야기하고 싶어."
            }
            containsAny(lastMessage, "만나", "볼 수", "시간", "밥", "카페") -> {
                "${opening}고마워. 괜찮다면 짧게 시간 맞춰보자."
            }
            else -> {
                "${opening}괜찮다면 짧게 안부부터 이야기해도 될까?"
            }
        }
    }

    fun counterpartReplyAlternatives(input: AnalysisInput, candidates: String? = null): String {
        val opening = replyOpening(input)
        val scheduleFallbacks = if (input.lastRecentMessageContent().hasConcreteScheduleSignal()) {
            listOf(
                counterpartReplyDraft(input),
                "${opening}좋아, 그때 보자. 고마워.",
                "${opening}확인했어. 늦지 않게 갈게.",
            )
        } else if (input.lastRecentMessageContent().hasScheduleQuestionSignal()) {
            listOf(
                counterpartReplyDraft(input),
                "${opening}좋아, 가능한 시간 확인해서 알려줄게.",
                "${opening}괜찮다면 시간 맞춰보자. 고마워.",
            )
        } else {
            listOf(
                counterpartReplyDraft(input),
                "${opening}부담 없으면 천천히 답할게.",
                "고마워. 나도 짧게 안부 전하고 싶었어.",
            )
        }
        val lines = candidates.orEmpty().lineSequence()
            .map { line -> line.trim() }
            .filter { line -> line.isSafeCounterpartReply() }
            .distinct()
            .take(3)
            .toMutableList()
        scheduleFallbacks.forEach { fallback ->
            if (lines.size < 3 && fallback !in lines) {
                lines += fallback
            }
        }
        return lines.take(3).joinToString(separator = "\n")
    }

    fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { keyword -> text.contains(keyword, ignoreCase = true) }
    }

    fun hasLongLastGap(input: AnalysisInput): Boolean {
        return input.recentSilenceDays() >= 7
    }

    fun hasVeryLongLastGap(input: AnalysisInput): Boolean {
        return input.recentSilenceDays() >= 30
    }

    fun hasDelayedCounterpartReply(input: AnalysisInput): Boolean {
        return input.afterLastMessageDays() >= 1
    }

    private fun String.normalizeReadiness(): String {
        val raw = trim()
        return allowedReadiness.firstOrNull { readiness -> raw.contains(readiness) } ?: UNKNOWN
    }

    private fun sanitizeDraft(readiness: String, draft: String, input: AnalysisInput?): String {
        val trimmed = draft.trim()
        return when {
            readiness == HOLD || readiness == UNKNOWN -> defaultDraft(readiness, input)
            trimmed.isBlank() || trimmed.length > MAX_DRAFT_LENGTH || trimmed.hasUnsafeDraftPhrase() -> {
                defaultDraft(readiness, input)
            }
            else -> trimmed
        }
    }

    private fun sanitizeAlternatives(readiness: String, alternatives: String): String {
        if (readiness == HOLD) {
            return defaultAlternatives(HOLD).joinToString(separator = "\n")
        }
        val lines = alternatives.lineSequence()
            .map { line -> line.trim() }
            .filter { line -> line.isNotBlank() }
            .filterNot { line -> line.hasUnsafeDraftPhrase() }
            .distinct()
            .take(3)
            .toMutableList()
        defaultAlternatives(readiness).forEach { fallback ->
            if (lines.size < 3 && fallback !in lines) {
                lines += fallback
            }
        }
        return lines.take(3).joinToString(separator = "\n")
    }

    private fun defaultObjective(readiness: String, input: AnalysisInput?): String {
        return when (readiness) {
            HOLD -> "상대에게 다시 부담을 주지 않는 것이 우선입니다."
            APOLOGY -> "관계 회복 요구보다 짧은 인정과 사과로 상대가 답할 여지를 남기는 것이 목표입니다."
            LIGHT_CONTACT -> if (input != null && hasVeryLongLastGap(input)) {
                "오래 끊긴 흐름을 한 번에 회복하려 하지 않고, 짧은 안부로 현재 온도만 확인하는 것이 목표입니다."
            } else {
                "가벼운 안부 한 문장으로 상대의 현재 온도를 확인하는 것이 목표입니다."
            }
            else -> "보낼 문장을 만들기보다 대화 맥락이 충분한지 먼저 확인하는 것이 목표입니다."
        }
    }

    private fun defaultNextStep(readiness: String, input: AnalysisInput?): String {
        return when (readiness) {
            HOLD -> "오늘은 보내지 말고 최근 대화의 경계 표현과 무응답 흐름을 먼저 확인하세요."
            APOLOGY -> "변명 없이 인정할 부분을 한 문장으로 정리하고 답장을 요구하지 마세요."
            UNKNOWN -> "오늘은 보내지 말고, 이 대화가 재회 판단에 충분한지 먼저 확인하세요."
            else -> if (input != null && hasVeryLongLastGap(input)) {
                "오래 끊긴 대화이므로 지난 일을 꺼내기보다 안부 한 문장만 준비하세요."
            } else if (input != null && hasLongLastGap(input)) {
                "며칠 이상 공백이 있었으니 이유를 길게 설명하지 말고 짧은 안부만 준비하세요."
            } else {
                "긴 설명을 보내지 말고 최근 대화를 한 번 읽은 뒤 짧은 한 문장만 준비하세요."
            }
        }
    }

    private fun defaultDraft(readiness: String, input: AnalysisInput?): String {
        return when (readiness) {
            HOLD -> "오늘은 보내지 않습니다. 상대가 먼저 답하거나 시간이 충분히 지난 뒤 다시 판단하세요."
            APOLOGY -> "오랜만이야. 그때 내가 부담스럽게 했다면 미안해. 답은 천천히 해도 괜찮아."
            UNKNOWN -> "지금은 보낼 문장을 만들지 않습니다. 최근 대화와 관계 맥락을 먼저 확인하세요."
            else -> if (input != null && hasVeryLongLastGap(input)) {
                "오랜만이야. 갑자기 긴 얘기하려는 건 아니고, 잘 지내는지만 궁금했어."
            } else if (input != null && hasLongLastGap(input)) {
                "며칠 지나 조심스럽지만, 괜찮다면 짧게 안부만 묻고 싶어."
            } else {
                "오랜만이야. 갑자기 부담 주려는 건 아니고, 괜찮다면 짧게 안부만 묻고 싶어."
            }
        }
    }

    private fun defaultAlternatives(readiness: String): List<String> {
        return when (readiness) {
            HOLD -> listOf(
                "오늘은 보내지 않기",
                "상대가 먼저 답하면 그때 짧게 답하기",
                "다시 보내기 전 최근 대화와 경계 표현 확인하기",
            )
            APOLOGY -> listOf(
                "오랜만이야. 그때 내가 부담스럽게 했다면 미안해.",
                "답을 바라기보다 내가 미안했던 부분을 짧게 전하고 싶었어.",
                "괜찮다면 나중에 천천히 이야기하고 싶어. 답은 안 해도 괜찮아.",
            )
            UNKNOWN -> listOf(
                "대화가 1:1 개인 관계인지 확인하기",
                "내 카톡 이름이 정확한지 확인하기",
                "더 관련 있는 최근 대화로 다시 분석하기",
            )
            else -> listOf(
                "오랜만이야. 잘 지내고 있는지 궁금해서 짧게 연락했어.",
                "갑자기 부담 주려는 건 아니고, 괜찮다면 안부만 묻고 싶어.",
                "답은 천천히 해도 괜찮아. 그냥 한 번 안부 전하고 싶었어.",
            )
        }
    }

    private fun String.hasUnsafeDraftPhrase(): Boolean {
        return unsafeDraftPhrases.any { phrase -> contains(phrase, ignoreCase = true) }
    }

    private fun String.isSafeCounterpartReply(): Boolean {
        val trimmed = trim()
        if (trimmed.isBlank() || trimmed.length > MAX_DRAFT_LENGTH || trimmed.hasUnsafeDraftPhrase()) {
            return false
        }
        if (containsAny(trimmed, "오랜만", "먼저 연락", "다시 연락", "잠깐 얘기할 수 있어")) {
            return false
        }
        return containsAny(
            trimmed,
            "고마워",
            "봤어",
            "확인",
            "나도",
            "천천히",
            "말해줘서",
            "안부",
            "부담 없이",
            "부담 없",
            "그때 보자",
            "늦지 않게",
        )
    }

    private fun String.hasConcreteScheduleSignal(): Boolean {
        val normalized = lowercase()
        return scheduleTimePhrases.any { phrase -> normalized.contains(phrase) } &&
            scheduleActionPhrases.any { phrase -> normalized.contains(phrase) } &&
            !hasScheduleQuestionSignal()
    }

    private fun String.hasScheduleQuestionSignal(): Boolean {
        val normalized = lowercase()
        if (!scheduleTimePhrases.any { phrase -> normalized.contains(phrase) }) {
            return false
        }
        return containsAny(
            normalized,
            "시간 돼",
            "시간 되",
            "시간 가능",
            "가능해",
            "가능할",
            "괜찮아",
            "괜찮을",
            "될까",
            "볼 수",
            "언제",
        )
    }

    private fun hasBoundarySignal(input: AnalysisInput): Boolean {
        return firstBoundaryLine(input) != null
    }

    private fun hasStrongPersonalSignal(input: AnalysisInput): Boolean {
        val combined = "${input.signalExcerpt}\n${input.recentExcerpt}\n${input.perspectiveSummary}".lowercase()
        return strongPersonalSignalPhrases.any { phrase -> combined.contains(phrase.lowercase()) }
    }

    private fun looksTechnicalOrTransactional(input: AnalysisInput): Boolean {
        val combined = "${input.conversationTitle}\n${input.excerpt}\n${input.recentExcerpt}".lowercase()
        val hitCount = technicalOrTransactionalPhrases.count { phrase -> combined.contains(phrase.lowercase()) }
        return hitCount >= 2
    }

    private fun AnalysisInput.lastRecentMessageContent(): String {
        return recentExcerpt.lineSequence()
            .lastOrNull { line -> line.isNotBlank() }
            ?.substringAfter(": ", missingDelimiterValue = "")
            ?.trim()
            .orEmpty()
    }

    private fun AnalysisInput.lastGapDays(): Int {
        val label = statsSummary.lineSequence()
            .firstOrNull { line -> line.startsWith("마지막 메시지 전 공백:") }
            ?.substringAfter(":")
            ?.trim()
            .orEmpty()
        return Regex("(\\d+)일").find(label)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: if (Regex("(\\d+)시간").find(label)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0 >= 24) {
                1
            } else {
                0
            }
    }

    private fun replyOpening(input: AnalysisInput): String {
        return if (hasDelayedCounterpartReply(input)) {
            "답이 늦었네. "
        } else {
            "메시지 봤어. "
        }
    }

    private fun AnalysisInput.afterLastMessageDays(): Int {
        val label = statsSummary.lineSequence()
            .firstOrNull { line -> line.startsWith("마지막 메시지 이후 경과:") }
            ?.substringAfter(":")
            ?.trim()
            .orEmpty()
        return Regex("(\\d+)일").find(label)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: if (Regex("(\\d+)시간").find(label)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0 >= 24) {
                1
            } else {
                0
            }
    }

    private fun AnalysisInput.recentSilenceDays(): Int {
        return maxOf(lastGapDays(), afterLastMessageDays())
    }

    private fun firstBoundaryLine(input: AnalysisInput): String? {
        return sequenceOf(input.signalExcerpt, input.recentExcerpt, input.excerpt)
            .flatMap { text -> text.lineSequence() }
            .firstOrNull { line -> line.hasBoundaryPhrase() }
    }

    private fun userFinalRunCount(input: AnalysisInput): Int {
        return input.perspectiveSummary.extractCountAfter("내 마지막 연속 발화:")
    }

    private fun unknownFinalRunCount(input: AnalysisInput): Int {
        return if (input.perspectiveSummary.contains("마지막 메시지 발신자 역할: 알 수 없음")) {
            input.perspectiveSummary.extractCountAfter("마지막 연속 발화 역할:")
        } else {
            0
        }
    }

    private fun String.hasBoundaryPhrase(): Boolean {
        val normalized = replace(Regex("\\s+"), " ").trim().lowercase()
        if (hardBoundaryPhrases.any { phrase -> normalized.contains(phrase) }) {
            return true
        }
        if (softBoundaryExceptions.any { phrase -> normalized.contains(phrase) }) {
            return false
        }
        return burdenBoundaryPatterns.any { pattern -> pattern.containsMatchIn(normalized) } ||
            discomfortBoundaryPatterns.any { pattern -> pattern.containsMatchIn(normalized) }
    }

    private fun String.extractCountAfter(label: String): Int {
        return lineSequence()
            .firstOrNull { line -> line.startsWith(label) }
            ?.substringAfter(label)
            ?.let { value -> Regex("\\d+").find(value)?.value }
            ?.toIntOrNull()
            ?: 0
    }
}
