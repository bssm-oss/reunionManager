package com.bssm.reunionmanager.ui.screen.settings

import com.bssm.reunionmanager.domain.model.GemmaBackend
import com.bssm.reunionmanager.domain.model.ProviderSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsScreenTest {
    @Test
    fun nameSaveRequest_preservesModelSettingsAndUsesCurrentNameField() {
        val settings = providerSettings(userDisplayName = "이전 이름")

        val request = settings.nameSaveRequest(userDisplayName = "현재 입력 이름")

        assertEquals("/data/local/tmp/gemma-4-E4B-it.litertlm", request.modelPath)
        assertEquals("gemma-4-E4B-it.litertlm", request.modelName)
        assertEquals("GPU", request.backend)
        assertEquals("현재 입력 이름", request.userDisplayName)
    }

    @Test
    fun demoModeSaveRequest_clearsModelPathButKeepsCurrentNameField() {
        val settings = providerSettings(userDisplayName = "이전 이름")

        val request = settings.demoModeSaveRequest(userDisplayName = "현재 입력 이름")

        assertEquals("", request.modelPath)
        assertEquals("gemma-4-E4B-it.litertlm", request.modelName)
        assertEquals("GPU", request.backend)
        assertEquals("현재 입력 이름", request.userDisplayName)
    }

    @Test
    fun modelMessageTitle_matchesModelStorageAction() {
        assertEquals(
            "AI 모델 파일 제거됨",
            modelMessageTitle(providerSettings(userDisplayName = "현우"), "모델 파일을 제거하고 기본 정리로 전환했습니다."),
        )
        assertEquals(
            "AI 모델 파일 저장됨",
            modelMessageTitle(providerSettings(userDisplayName = "현우"), "gemma 모델을 복사했습니다."),
        )
        assertEquals(
            "AI 모델 준비됨",
            modelMessageTitle(
                providerSettings(userDisplayName = "현우").copy(
                    verifiedModelPath = "/data/local/tmp/gemma-4-E4B-it.litertlm",
                    verifiedBackend = GemmaBackend.GPU,
                    verifiedAtEpochMillis = 1L,
                ),
                "gemma 모델 실행을 확인했습니다.",
            ),
        )
    }

    private fun providerSettings(userDisplayName: String): ProviderSettings {
        return ProviderSettings(
            modelPath = "/data/local/tmp/gemma-4-E4B-it.litertlm",
            modelName = "gemma-4-E4B-it.litertlm",
            backend = GemmaBackend.GPU,
            userDisplayName = userDisplayName,
        )
    }
}
