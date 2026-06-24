package com.bssm.reunionmanager.data.analysis

import android.content.Context
import com.bssm.reunionmanager.domain.analysis.AnalysisProvider
import com.bssm.reunionmanager.domain.model.AnalysisInput
import com.bssm.reunionmanager.domain.model.AnalysisReport
import com.bssm.reunionmanager.domain.model.GemmaBackend
import com.bssm.reunionmanager.domain.model.ProviderSettings
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class Gemma4AnalysisProvider(
    private val context: Context,
    private val settings: ProviderSettings,
) : AnalysisProvider {
    override suspend fun analyze(input: AnalysisInput): AnalysisReport = withContext(Dispatchers.IO) {
        val modelFile = File(settings.modelPath)
        require(modelFile.isFile) {
            "모델 파일을 찾을 수 없습니다. 모델을 다시 선택하거나 안전 정리를 사용하세요."
        }

        val engineConfig = EngineConfig(
            modelPath = modelFile.absolutePath,
            backend = settings.backend.toLiteRtBackend(),
            cacheDir = context.cacheDir.absolutePath,
        )

        Engine(engineConfig).use { engine ->
            engine.initialize()
            val responseText = engine.createConversation().use { conversation ->
                conversation.sendMessage(ReunionAnalysisPrompt.buildSinglePrompt(input)).toText()
            }
            AnalysisJsonResponseParser.parse(responseText)
        }
    }

    private fun Message.toText(): String {
        return contents.contents
            .filterIsInstance<Content.Text>()
            .joinToString(separator = "") { it.text }
            .ifBlank { toString() }
    }

    private fun GemmaBackend.toLiteRtBackend(): Backend {
        return when (this) {
            GemmaBackend.CPU -> Backend.CPU()
            GemmaBackend.GPU -> Backend.GPU()
        }
    }
}
