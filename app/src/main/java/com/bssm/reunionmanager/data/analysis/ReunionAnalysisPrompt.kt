package com.bssm.reunionmanager.data.analysis

import com.bssm.reunionmanager.domain.model.AnalysisInput

object ReunionAnalysisPrompt {
    const val SYSTEM_PROMPT: String =
        "You are a cautious Korean relationship-repair assistant. " +
            "Optimize for dignity, safety, and low-pressure next actions, not for maximizing reunion."

    fun buildSinglePrompt(input: AnalysisInput): String {
        return buildString {
            appendLine(SYSTEM_PROMPT)
            append(buildUserPrompt(input))
        }
    }

    fun buildUserPrompt(input: AnalysisInput): String {
        return buildString {
            appendLine("Analyze the KakaoTalk transcript and return only compact JSON.")
            appendLine("Privately run four passes before writing JSON: evidence reader, boundary/safety critic, last-message interpreter, and message writer. Do not reveal these passes.")
            appendLine("Use only the provided transcript evidence. Do not infer love, intent, certainty, or hidden feelings.")
            appendLine("If rejection, boundary, new partner, moved-on, fear, harassment, repeated unanswered messages, impaired late-night contact, or self-stop signs appear, recommend waiting or not contacting.")
            appendLine("Treat statements like 새로 만나는 사람 있어, 각자 잘 지내자, 친구로 지내자, 나중에 내가 연락할게, 시간이 필요해, 마음 정리했어, 다시 볼 생각 없어 as boundaries.")
            appendLine("Never suggest persuasion, repeated messages, visiting home/school/work, gifts, emotional pressure, or asking for one last chance.")
            appendLine("If the user sent 2 or more final unanswered messages, be conservative. If 3 or more, hold contact.")
            appendLine("If the user's KakaoTalk name is missing or does not match participants, set contactReadiness to 정보 부족 and do not draft a contact message.")
            appendLine("If the transcript looks like a group, work, technical, transactional, wrong-number, or identity-uncertain chat, set contactReadiness to 정보 부족 and do not draft a contact message.")
            appendLine("If the last sender role is 상대, do not frame the action as a new first contact. Draft a short reply to the counterpart's last message only when no boundary blocks it.")
            appendLine("If the counterpart asks whether a date or time works, draft an availability-check reply, not a confirmed-plan reply.")
            appendLine("If the counterpart proposes a concrete time/place, acknowledge and confirm without escalating into reunion talk.")
            appendLine("Mention a late reply only when Conversation stats says 마지막 메시지 이후 경과 is at least 1 day.")
            appendLine("Return only valid JSON with exactly these string keys:")
            appendLine("headline, contactReadiness, evidence, relationshipSummary, reunionObjective, nextStep, messageDraft, alternativeDrafts, caution")
            appendLine("All values must be Korean. No markdown fences. No therapy claims. No certainty.")
            appendLine("contactReadiness must be one of: 지금은 보류, 먼저 사과 필요, 아주 가볍게 가능, 정보 부족.")
            appendLine("headline: one specific Korean summary under 24 characters.")
            appendLine("evidence: 2-3 short reasons grounded in stats/excerpts, separated by newline characters.")
            appendLine("relationshipSummary: one sentence grounded in last sender, silence duration, and signal excerpts.")
            appendLine("reunionObjective: safest immediate goal, not a broad relationship goal.")
            appendLine("nextStep: one clear action under 90 Korean characters.")
            appendLine("messageDraft: if contactReadiness is 지금은 보류 or 정보 부족, say not to send a message today or to check missing information. Otherwise one gentle copyable line under 70 Korean characters.")
            appendLine("alternativeDrafts: exactly 3 short candidate messages or no-send actions separated by newline characters.")
            appendLine("caution: one practical warning under 90 Korean characters.")
            appendLine()
            appendLine("Conversation title: ${input.conversationTitle}")
            appendLine("Participants: ${input.participantNames.joinToString()}")
            appendLine("Message count: ${input.messageCount}")
            appendLine()
            appendLine("Perspective summary:")
            appendLine(input.perspectiveSummary)
            appendLine()
            appendLine("Conversation stats:")
            appendLine(input.statsSummary)
            appendLine()
            appendLine("Important excerpt:")
            appendLine(input.excerpt)
            appendLine()
            appendLine("Recent excerpt:")
            appendLine(input.recentExcerpt)
            appendLine()
            appendLine("Signal excerpt:")
            appendLine(input.signalExcerpt.ifBlank { "No explicit signal messages were detected." })
        }
    }
}
