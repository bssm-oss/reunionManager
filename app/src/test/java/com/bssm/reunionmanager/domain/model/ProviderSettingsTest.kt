package com.bssm.reunionmanager.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderSettingsTest {
    @Test
    fun isModelVerified_requiresMatchingPathBackendAndTimestamp() {
        assertTrue(
            ProviderSettings(
                modelPath = "/models/gemma.litertlm",
                backend = GemmaBackend.CPU,
                verifiedModelPath = "/models/gemma.litertlm",
                verifiedBackend = GemmaBackend.CPU,
                verifiedAtEpochMillis = 100L,
            ).isModelVerified,
        )

        assertFalse(
            ProviderSettings(
                modelPath = "/models/gemma.litertlm",
                backend = GemmaBackend.CPU,
                verifiedModelPath = "/models/other.litertlm",
                verifiedBackend = GemmaBackend.CPU,
                verifiedAtEpochMillis = 100L,
            ).isModelVerified,
        )
        assertFalse(
            ProviderSettings(
                modelPath = "/models/gemma.litertlm",
                backend = GemmaBackend.GPU,
                verifiedModelPath = "/models/gemma.litertlm",
                verifiedBackend = GemmaBackend.CPU,
                verifiedAtEpochMillis = 100L,
            ).isModelVerified,
        )
        assertFalse(
            ProviderSettings(
                modelPath = "/models/gemma.litertlm",
                backend = GemmaBackend.CPU,
                verifiedModelPath = "/models/gemma.litertlm",
                verifiedBackend = GemmaBackend.CPU,
            ).isModelVerified,
        )
    }
}
