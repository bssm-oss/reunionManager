package com.bssm.reunionmanager.data.analysis

import com.bssm.reunionmanager.domain.analysis.AnalysisProvider
import com.bssm.reunionmanager.domain.analysis.AnalysisSafetyRules
import com.bssm.reunionmanager.domain.model.AnalysisInput
import com.bssm.reunionmanager.domain.model.AnalysisReport
import kotlinx.coroutines.delay

class FakeAnalysisProvider : AnalysisProvider {
    override suspend fun analyze(input: AnalysisInput): AnalysisReport {
        delay(50)
        val participantLabel = if (input.participantNames.isEmpty()) "대화 참여자" else input.participantNames.joinToString()
        val readiness = input.inferReadiness()
        val evidence = input.buildEvidence()
        val counterpartIsWaiting = AnalysisSafetyRules.counterpartFinalRunCount(input) > 0
        val longGap = AnalysisSafetyRules.hasLongLastGap(input)
        val veryLongGap = AnalysisSafetyRules.hasVeryLongLastGap(input)
        val weakContext = AnalysisSafetyRules.hasWeakReunionContext(input)
        val needsPerspectiveSetup = AnalysisSafetyRules.needsUserPerspective(input)
        val counterpartRecentMessage = input.perspectiveValue("상대 최근 메시지:")
        return AnalysisReport(
            headline = when (readiness) {
                "지금은 보류" -> "오늘은 보내지 않기"
                "먼저 사과 필요" -> "짧은 사과 먼저"
                "아주 가볍게 가능" -> when {
                    counterpartIsWaiting -> "상대 답장에 짧게 응답"
                    veryLongGap -> "오래 지난 안부만"
                    else -> "가벼운 안부만"
                }
                "정보 부족" -> if (weakContext) {
                    "관계 맥락 확인"
                } else if (needsPerspectiveSetup) {
                    "내 이름 확인"
                } else {
                    "확신보다 낮은 압박"
                }
                else -> "확신보다 낮은 압박"
            },
            contactReadiness = readiness,
            evidence = evidence,
            relationshipSummary = if (counterpartRecentMessage.isNotBlank() && counterpartRecentMessage != "없음") {
                "$participantLabel 대화에서 상대의 최근 말은 \"$counterpartRecentMessage\"입니다. 이 말과 공백, 마지막 발신자 흐름을 함께 봐야 합니다."
            } else if (weakContext) {
                "$participantLabel 대화는 재회 판단에 필요한 1:1 개인 관계 신호가 부족합니다. 업무, 단체, 기술 맥락일 수 있어 먼저 대화 파일을 확인해야 합니다."
            } else if (needsPerspectiveSetup) {
                "내 카톡 이름이 없어 이 대화만으로는 답장인지 첫 연락인지 확정할 수 없습니다."
            } else {
                "총 ${input.messageCount}개의 메시지와 최근 흐름을 보면 $participantLabel 사이의 온도를 단정하기보다 마지막 발신자, 긴 공백, 경계 표현을 먼저 확인해야 합니다."
            },
            reunionObjective = when (readiness) {
                "지금은 보류" -> "상대에게 다시 부담을 주지 않는 것이 우선입니다. 바로 연락하기보다 상황을 정리하세요."
                "먼저 사과 필요" -> "관계 회복 요구보다 짧은 인정과 사과로 상대가 답할 여지를 남기는 것이 목표입니다."
                "아주 가볍게 가능" -> if (counterpartIsWaiting) {
                    "새 연락을 시작하기보다 상대가 남긴 말에 짧고 낮은 압박으로 답하는 것이 목표입니다."
                } else if (veryLongGap) {
                    "오래 끊긴 흐름을 한 번에 회복하려 하지 않고, 짧은 안부로 현재 온도만 확인하는 것이 목표입니다."
                } else {
                    "가벼운 안부 한 문장으로 상대의 현재 온도를 확인하는 것이 목표입니다."
                }
                else -> if (weakContext) {
                    "보낼 문장을 만들기보다 분석 대상 대화가 맞는지 먼저 확인하는 것이 목표입니다."
                } else if (needsPerspectiveSetup) {
                    "보낼 문장을 만들기보다 내 카톡 이름을 먼저 저장해 관점을 맞추는 것이 목표입니다."
                } else {
                    "보낼 문장을 만들기보다 이 대화가 재회 판단에 충분한지 먼저 확인하는 것이 목표입니다."
                }
            },
            nextStep = when (readiness) {
                "지금은 보류" -> "오늘은 보내지 말고, 상대가 부담을 표현한 부분과 내가 반복해서 보낸 메시지를 먼저 다시 읽으세요."
                "먼저 사과 필요" -> "변명 없이 한 문장으로 인정할 부분을 정리한 뒤, 답장을 요구하지 않는 문장만 준비하세요."
                "아주 가볍게 가능" -> if (counterpartIsWaiting) {
                    "상대의 마지막 메시지에 바로 답하되, 재회 이야기보다 짧은 안부와 확인만 남기세요."
                } else if (veryLongGap) {
                    "오래 끊긴 대화이므로 지난 일을 꺼내기보다 안부 한 문장만 준비하세요."
                } else if (longGap) {
                    "며칠 이상 공백이 있었으니 이유를 길게 설명하지 말고 짧은 안부만 준비하세요."
                } else {
                    "긴 설명을 보내지 말고, 최근 대화를 한 번 읽은 뒤 부담 없는 한 문장만 준비하세요."
                }
                else -> if (weakContext) {
                    "오늘은 보내지 말고, 1:1 개인 관계 대화인지 또는 더 관련 있는 대화 파일이 있는지 먼저 확인하세요."
                } else if (needsPerspectiveSetup) {
                    "설정에서 내 카톡 이름을 저장한 뒤 같은 대화를 다시 분석하세요."
                } else if (longGap) {
                    "공백이 있으므로 바로 보내지 말고 최근 대화가 더 있는지 먼저 확인하세요."
                } else {
                    "오늘은 보내지 말고, 최근 대화가 재회 판단에 충분한지 먼저 확인하세요."
                }
            },
            messageDraft = when (readiness) {
                "지금은 보류" -> "오늘은 보내지 않습니다. 상대가 먼저 답하거나 시간이 충분히 지난 뒤 다시 판단하세요."
                "먼저 사과 필요" -> "오랜만이야. 그때 내가 부담스럽게 했다면 미안해. 답은 천천히 해도 괜찮아."
                "아주 가볍게 가능" -> if (counterpartIsWaiting) {
                    AnalysisSafetyRules.counterpartReplyDraft(input)
                } else if (veryLongGap) {
                    "오랜만이야. 갑자기 긴 얘기하려는 건 아니고, 잘 지내는지만 궁금했어."
                } else if (longGap) {
                    "며칠 지나 조심스럽지만, 괜찮다면 짧게 안부만 묻고 싶어."
                } else {
                    "오랜만이야. 갑자기 부담 주려는 건 아니고, 괜찮다면 짧게 안부만 묻고 싶어."
                }
                else -> if (weakContext) {
                    "지금은 보낼 문장을 만들지 않습니다. 재회와 직접 관련 있는 대화를 먼저 확인하세요."
                } else if (needsPerspectiveSetup) {
                    "지금은 보낼 문장을 만들지 않습니다. 내 카톡 이름을 먼저 저장하세요."
                } else {
                    "지금은 보낼 문장을 만들지 않습니다. 최근 대화와 관계 맥락을 먼저 확인하세요."
                }
            },
            alternativeDrafts = input.buildAlternativeDrafts(readiness),
            caution = when (readiness) {
                "지금은 보류" -> "상대가 경계나 거절을 표현했다면 다시 연락하지 않는 선택도 계획에 포함해야 합니다."
                "정보 부족" -> if (weakContext) {
                    "업무, 단체, 기술 대화는 재회 가능성 판단 근거로 부족할 수 있습니다."
                } else if (needsPerspectiveSetup) {
                    "발신자 관점이 틀리면 상대가 기다리는 상황을 새 연락처럼 잘못 해석할 수 있습니다."
                } else {
                    "답을 재촉하거나 지난 일을 한 번에 정리하려고 하면 부담이 커질 수 있습니다. 답장이 없으면 기다리는 쪽이 더 안전합니다."
                }
                else -> "답을 재촉하거나 지난 일을 한 번에 정리하려고 하면 부담이 커질 수 있습니다. 답장이 없으면 기다리는 쪽이 더 안전합니다."
            },
        )
    }

    private fun AnalysisInput.inferReadiness(): String {
        val combined = "${signalExcerpt}\n${recentExcerpt}\n${statsSummary}\n${perspectiveSummary}"
        return when {
            AnalysisSafetyRules.requiresHold(this) -> "지금은 보류"
            AnalysisSafetyRules.needsUserPerspective(this) -> "정보 부족"
            AnalysisSafetyRules.hasWeakReunionContext(this) -> "정보 부족"
            AnalysisSafetyRules.counterpartFinalRunCount(this) > 0 &&
                AnalysisSafetyRules.containsAny(
                    combined,
                    "생각났",
                    "잘 지내",
                    "잘지내",
                    "괜찮",
                    "좋아",
                    "고마워",
                    "안부",
                    "보자",
                    "만나",
                    "카페",
                    "밥",
                    "시간",
                    "약속",
                ) -> "아주 가볍게 가능"
            AnalysisSafetyRules.containsAny(combined, "미안", "사과", "화나", "힘들") -> "먼저 사과 필요"
            AnalysisSafetyRules.containsAny(combined, "보고싶", "보고 싶", "생각났", "잘 지내", "잘지내", "괜찮", "좋아") -> "아주 가볍게 가능"
            else -> "정보 부족"
        }
    }

    private fun AnalysisInput.buildEvidence(): String {
        val signalLine = signalExcerpt.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() }
        val lastLine = statsSummary.lineSequence().firstOrNull { it.startsWith("마지막 메시지:") }
        return listOfNotNull(
            lastLine,
            statsSummary.lineSequence().firstOrNull { it.startsWith("마지막 발신자의 연속 발화:") },
            perspectiveSummary.lineSequence().firstOrNull { it.startsWith("마지막 메시지 발신자 역할:") },
            perspectiveSummary.lineSequence().firstOrNull { it.startsWith("내 마지막 연속 발화:") },
            perspectiveSummary.lineSequence().firstOrNull { it.startsWith("상대 마지막 연속 발화:") },
            perspectiveSummary.lineSequence().firstOrNull { it.startsWith("상대 최근 메시지:") },
            signalLine?.let { "감정/경계 신호: $it" },
            statsSummary.lineSequence().firstOrNull { it.startsWith("마지막 메시지 이후 경과:") },
            statsSummary.lineSequence().firstOrNull { it.startsWith("긴 공백:") || it.startsWith("6시간 이상") },
        ).joinToString(separator = "\n").ifBlank {
            "최근 대화와 감정 신호가 부족해 낮은 압박의 안부만 안전합니다."
        }
    }

    private fun AnalysisInput.buildAlternativeDrafts(readiness: String): String {
        return when (readiness) {
            "지금은 보류" -> listOf(
                "오늘은 보내지 않기",
                "상대가 먼저 답하면 그때 짧게 답하기",
                "다시 보내기 전 최근 대화와 경계 표현 다시 확인하기",
            )
            "아주 가볍게 가능" -> if (counterpartFinalRunCount() > 0) {
                AnalysisSafetyRules.counterpartReplyAlternatives(this).lines()
            } else {
                listOf(
                    "오랜만이야. 잘 지내고 있는지 궁금해서 짧게 연락했어.",
                    "갑자기 부담 주려는 건 아니고, 괜찮다면 안부만 묻고 싶어.",
                    "답은 천천히 해도 괜찮아. 그냥 한 번 안부 전하고 싶었어.",
                )
            }
            "먼저 사과 필요" -> listOf(
                "오랜만이야. 그때 내가 부담스럽게 했다면 미안해.",
                "답을 바라기보다 내가 미안했던 부분을 짧게 전하고 싶었어.",
                "괜찮다면 나중에 천천히 이야기하고 싶어. 답은 안 해도 괜찮아.",
            )
            else -> if (AnalysisSafetyRules.hasWeakReunionContext(this)) {
                listOf(
                    "대화가 1:1 개인 관계인지 확인하기",
                    "내 카톡 이름 저장하기",
                    "더 관련 있는 대화 파일로 다시 분석하기",
                )
            } else if (AnalysisSafetyRules.needsUserPerspective(this)) {
                listOf(
                    "내 카톡 이름 저장하기",
                    "같은 대화 다시 분석하기",
                    "최근 대화 파일인지 확인하기",
                )
            } else {
                listOf(
                    "대화가 1:1 개인 관계인지 확인하기",
                    "내 카톡 이름이 정확한지 확인하기",
                    "더 관련 있는 최근 대화로 다시 분석하기",
                )
            }
        }.joinToString(separator = "\n")
    }

    private fun AnalysisInput.counterpartFinalRunCount(): Int {
        return AnalysisSafetyRules.counterpartFinalRunCount(this)
    }

    private fun AnalysisInput.perspectiveValue(label: String): String {
        return perspectiveSummary.lineSequence()
            .firstOrNull { line -> line.startsWith(label) }
            ?.substringAfter(label)
            ?.trim()
            .orEmpty()
    }
}
