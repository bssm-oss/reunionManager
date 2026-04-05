package com.bssm.reunionmanager.data.analysis

import com.bssm.reunionmanager.domain.model.AnalysisInput
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAnalysisProviderTest {
    @Test
    fun analyze_returnsTentativeGuidance() = runTest {
        val provider = FakeAnalysisProvider()

        val result = provider.analyze(
            AnalysisInput(
                conversationTitle = "Spring meetup",
                participantNames = listOf("Alex", "Minji"),
                messageCount = 12,
                excerpt = "Alex: hello",
            ),
        )

        assertTrue(result.headline.contains("Spring meetup"))
        assertTrue(result.caution.contains("확정적으로 판단하지 마세요"))
    }
}
