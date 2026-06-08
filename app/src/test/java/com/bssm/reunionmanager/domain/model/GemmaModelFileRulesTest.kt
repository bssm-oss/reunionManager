package com.bssm.reunionmanager.domain.model

import org.junit.Assert.assertTrue
import org.junit.Test

class GemmaModelFileRulesTest {
    @Test
    fun requireSupportedFileName_acceptsLiteRtLmFilesCaseInsensitively() {
        GemmaModelFileRules.requireSupportedFileName("gemma-4-E4B-it.LITERTLM")
    }

    @Test
    fun requireSupportedFileName_rejectsOtherFiles() {
        val result = runCatching {
            GemmaModelFileRules.requireSupportedFileName("gemma-4-E4B-it.txt")
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains(".litertlm"))
    }

    @Test
    fun requirePlausibleModelSize_rejectsTinyPlaceholderFiles() {
        val result = runCatching {
            GemmaModelFileRules.requirePlausibleModelSize(1024L)
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("너무 작습니다"))
    }

    @Test
    fun requirePlausibleModelSize_acceptsRealisticModelFiles() {
        GemmaModelFileRules.requirePlausibleModelSize(512L * 1024L * 1024L)
    }
}
