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
            headline = "${input.conversationTitle} 대화 기반 재회 초안",
            relationshipSummary = "총 ${input.messageCount}개의 메시지를 기준으로 보면 $participantLabel 사이의 대화 흐름을 차분히 다시 정리할 필요가 있습니다.",
            reunionObjective = "감정 해석을 단정하지 않고, 다시 연락하기 전에 최근 대화 맥락과 상대 반응 패턴을 먼저 점검하는 것이 MVP의 기본 목표입니다.",
            nextStep = "최근 메시지에서 감정이 높아진 지점을 두세 개 추려 메모한 뒤, 짧고 압박이 적은 안부 메시지 초안을 준비해 보세요.",
            caution = "이 결과는 로컬 테스트용 가이드입니다. 실제 의도나 관계 상태를 확정적으로 판단하지 마세요.",
        )
    }
}
