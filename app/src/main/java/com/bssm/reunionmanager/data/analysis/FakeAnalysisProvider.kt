package com.bssm.reunionmanager.data.analysis

import com.bssm.reunionmanager.domain.analysis.AnalysisProvider
import com.bssm.reunionmanager.domain.model.AnalysisInput
import com.bssm.reunionmanager.domain.model.AnalysisReport
import kotlinx.coroutines.delay

class FakeAnalysisProvider : AnalysisProvider {
    override suspend fun analyze(input: AnalysisInput): AnalysisReport {
        delay(50)
        val participantLabel = if (input.participantNames.isEmpty()) "대화 참여자" else input.participantNames.joinToString()
        return AnalysisReport(
            headline = "${input.conversationTitle}에서 바로 할 수 있는 첫 단계",
            relationshipSummary = "총 ${input.messageCount}개의 메시지를 보면 $participantLabel 사이에 다시 말을 꺼낼 여지는 있지만, 감정을 단정하기보다 대화의 온도를 먼저 낮추는 편이 안전합니다.",
            reunionObjective = "상대의 반응을 확인하는 짧은 안부로 시작하고, 바로 관계 회복을 요구하지 않는 것이 목표입니다.",
            nextStep = "오늘은 긴 설명을 보내지 말고, 최근 대화를 한 번 읽은 뒤 부담 없는 한 문장만 준비하세요.",
            messageDraft = "오랜만이야. 갑자기 부담 주려는 건 아니고, 괜찮다면 한 번 차분하게 이야기해보고 싶어.",
            caution = "답을 재촉하거나 지난 일을 한 번에 정리하려고 하면 부담이 커질 수 있습니다. 답장이 없으면 기다리는 쪽이 더 안전합니다.",
        )
    }
}
