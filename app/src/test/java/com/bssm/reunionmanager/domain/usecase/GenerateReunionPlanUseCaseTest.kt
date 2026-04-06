package com.bssm.reunionmanager.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bssm.reunionmanager.data.analysis.FakeAnalysisProvider
import com.bssm.reunionmanager.data.analysis.GeminiAnalysisProvider
import com.bssm.reunionmanager.data.importer.ParsedConversation
import com.bssm.reunionmanager.data.importer.ParsedMessage
import com.bssm.reunionmanager.data.local.ReunionManagerDatabase
import com.bssm.reunionmanager.data.repository.AnalysisRepository
import com.bssm.reunionmanager.data.repository.ConversationRepository
import com.bssm.reunionmanager.data.repository.ProviderSettingsRepository
import com.bssm.reunionmanager.domain.model.ProviderSettings
import com.bssm.reunionmanager.domain.model.ImportConversationResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import java.net.ServerSocket
import java.io.OutputStreamWriter
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GenerateReunionPlanUseCaseTest {
    private lateinit var database: ReunionManagerDatabase
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var analysisRepository: AnalysisRepository
    private lateinit var providerSettingsRepository: ProviderSettingsRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReunionManagerDatabase::class.java,
        ).allowMainThreadQueries().build()

        conversationRepository = ConversationRepository(
            database = database,
            conversationDao = database.conversationDao(),
            participantDao = database.participantDao(),
            messageDao = database.messageDao(),
            analysisResultDao = database.analysisResultDao(),
        )
        analysisRepository = AnalysisRepository(database.analysisResultDao())
        providerSettingsRepository = ProviderSettingsRepository(database.providerSettingsDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun invoke_usesFakeProviderWhenApiKeyIsMissing() = runTest {
        val importedId = (conversationRepository.importConversation(
            parsedConversation = sampleParsedConversation,
            rawText = "analysis raw text",
            sourceName = "analysis.txt",
        ) as ImportConversationResult.Imported).conversationId

        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            geminiProviderFactory = { settings -> GeminiAnalysisProvider(settings) },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        assertEquals("fake", result.getOrNull())

        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        assertTrue(detail.latestAnalysis != null)
        assertTrue(detail.latestAnalysis!!.headline.contains("재회 초안"))
    }

    @Test
    fun invoke_usesGeminiProviderWhenSettingsAreConfigured() = runTest {
        val importedId = importSampleConversation()
        val response = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\"headline\":\"Mock Gemini headline\",\"relationshipSummary\":\"Mock Gemini relationship summary\",\"reunionObjective\":\"Mock Gemini reunion objective\",\"nextStep\":\"Mock Gemini next step\",\"caution\":\"Mock Gemini caution\"}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()
        val serverSocket = ServerSocket(0)
        val serverThread = startMockGeminiServer(serverSocket, 200, response)

        try {
            providerSettingsRepository.save(
                ProviderSettings(
                    apiKey = "test-key",
                    modelName = "mock-model",
                    endpoint = "http://127.0.0.1:${serverSocket.localPort}/v1beta",
                ),
            )

            val useCase = GenerateReunionPlanUseCase(
                conversationRepository = conversationRepository,
                analysisRepository = analysisRepository,
                providerSettingsRepository = providerSettingsRepository,
                fakeAnalysisProvider = FakeAnalysisProvider(),
                geminiProviderFactory = { settings -> GeminiAnalysisProvider(settings) },
            )

            val result = useCase(importedId)

            assertTrue(result.isSuccess)
            assertEquals("gemini", result.getOrNull())

            val detail = conversationRepository.observeConversationDetail(importedId).first()
            requireNotNull(detail)
            assertNotNull(detail.latestAnalysis)
            assertEquals("Mock Gemini headline", detail.latestAnalysis!!.headline)
            assertEquals("Mock Gemini relationship summary", detail.latestAnalysis!!.relationshipSummary)
        } finally {
            serverSocket.close()
            serverThread.join(1_000)
        }
    }

    @Test
    fun invoke_returnsFailureWhenConfiguredGeminiProviderErrors() = runTest {
        val importedId = importSampleConversation()
        val serverSocket = ServerSocket(0)
        val serverThread = startMockGeminiServer(serverSocket, 503, "temporary failure")

        try {
            providerSettingsRepository.save(
                ProviderSettings(
                    apiKey = "test-key",
                    modelName = "mock-model",
                    endpoint = "http://127.0.0.1:${serverSocket.localPort}/v1beta",
                ),
            )

            val useCase = GenerateReunionPlanUseCase(
                conversationRepository = conversationRepository,
                analysisRepository = analysisRepository,
                providerSettingsRepository = providerSettingsRepository,
                fakeAnalysisProvider = FakeAnalysisProvider(),
                geminiProviderFactory = { settings -> GeminiAnalysisProvider(settings) },
            )

            val result = useCase(importedId)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()!!.message!!.contains("Gemini analysis failed with HTTP 503"))

            val detail = conversationRepository.observeConversationDetail(importedId).first()
            requireNotNull(detail)
            assertFalse(detail.latestAnalysis != null)
        } finally {
            serverSocket.close()
            serverThread.join(1_000)
        }
    }

    private suspend fun importSampleConversation(): Long {
        return (conversationRepository.importConversation(
            parsedConversation = sampleParsedConversation,
            rawText = "analysis raw text",
            sourceName = "analysis.txt",
        ) as ImportConversationResult.Imported).conversationId
    }

    private fun startMockGeminiServer(
        serverSocket: ServerSocket,
        statusCode: Int,
        responseBody: String,
    ) = thread(start = true) {
        serverSocket.use { socketServer ->
            socketServer.accept().use { socket ->
                val input = socket.getInputStream().bufferedReader()
                while (true) {
                    val line = input.readLine() ?: break
                    if (line.isBlank()) break
                }

                val responseBytes = responseBody.toByteArray()
                val statusText = if (statusCode in 200..299) "OK" else "Service Unavailable"
                val output = socket.getOutputStream()
                val writer = OutputStreamWriter(output)
                writer.write("HTTP/1.1 $statusCode $statusText\r\n")
                writer.write("Content-Type: application/json\r\n")
                writer.write("Content-Length: ${responseBytes.size}\r\n")
                writer.write("Connection: close\r\n")
                writer.write("\r\n")
                writer.flush()
                output.write(responseBytes)
                output.flush()
            }
        }
    }

    private companion object {
        val sampleParsedConversation = ParsedConversation(
            title = "분석 테스트",
            exportedAtEpochMillis = null,
            participants = listOf("민지", "현우"),
            messages = listOf(
                ParsedMessage(
                    senderName = "민지",
                    sentAtEpochMillis = 1_710_000_000_000,
                    content = "대화를 다시 정리해보자",
                ),
                ParsedMessage(
                    senderName = "현우",
                    sentAtEpochMillis = 1_710_000_060_000,
                    content = "좋아, 차분하게 얘기해보자",
                ),
            ),
        )
    }
}
