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

    private fun providerSettings(userDisplayName: String): ProviderSettings {
        return ProviderSettings(
            modelPath = "/data/local/tmp/gemma-4-E4B-it.litertlm",
            modelName = "gemma-4-E4B-it.litertlm",
            backend = GemmaBackend.GPU,
            userDisplayName = userDisplayName,
        )
    }
}
