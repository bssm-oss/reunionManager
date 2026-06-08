package com.bssm.reunionmanager.domain.usecase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bssm.reunionmanager.data.local.ReunionManagerDatabase
import com.bssm.reunionmanager.data.repository.ProviderSettingsRepository
import com.bssm.reunionmanager.domain.analysis.AnalysisProvider
import com.bssm.reunionmanager.domain.model.AnalysisInput
import com.bssm.reunionmanager.domain.model.AnalysisReport
import com.bssm.reunionmanager.domain.model.ProviderSettings
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.RandomAccessFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VerifyGemmaModelUseCaseTest {
    private lateinit var database: ReunionManagerDatabase
    private lateinit var providerSettingsRepository: ProviderSettingsRepository
    private lateinit var testModelFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            ReunionManagerDatabase::class.java,
        ).allowMainThreadQueries().build()
        providerSettingsRepository = ProviderSettingsRepository(database.providerSettingsDao())
        testModelFile = File(context.cacheDir, "verify-gemma-model.litertlm").also { file ->
            if (file.exists()) {
                file.delete()
            }
            RandomAccessFile(file, "rw").use { randomAccessFile ->
                randomAccessFile.setLength(17L * 1024L * 1024L)
            }
        }
    }

    @After
    fun tearDown() {
        testModelFile.delete()
        database.close()
    }

    @Test
    fun invoke_runsProviderWhenConfiguredModelLooksValid() = runTest {
        providerSettingsRepository.save(
            ProviderSettings(
                modelPath = testModelFile.absolutePath,
                modelName = testModelFile.name,
                userDisplayName = "현우",
            ),
        )
        var providerCalled = false
        val useCase = VerifyGemmaModelUseCase(
            providerSettingsRepository = providerSettingsRepository,
            currentTimeMillis = { 123_456L },
            gemmaProviderFactory = {
                StaticAnalysisProvider {
                    providerCalled = true
                }
            },
        )

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(testModelFile.name, result.getOrNull())
        assertTrue(providerCalled)
        val verifiedSettings = providerSettingsRepository.get()
        assertTrue(verifiedSettings.isModelVerified)
        assertEquals(testModelFile.absolutePath, verifiedSettings.verifiedModelPath)
        assertEquals(123_456L, verifiedSettings.verifiedAtEpochMillis)
    }

    @Test
    fun invoke_failsBeforeProviderWhenModelFileIsMissing() = runTest {
        providerSettingsRepository.save(
            ProviderSettings(
                modelPath = File(testModelFile.parentFile, "missing.litertlm").absolutePath,
                modelName = testModelFile.name,
                userDisplayName = "현우",
            ),
        )
        var providerCalled = false
        val useCase = VerifyGemmaModelUseCase(
            providerSettingsRepository = providerSettingsRepository,
            gemmaProviderFactory = {
                StaticAnalysisProvider {
                    providerCalled = true
                }
            },
        )

        val result = useCase()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("찾을 수 없습니다"))
        assertFalse(providerCalled)
        assertFalse(providerSettingsRepository.get().isModelVerified)
    }

    @Test
    fun invoke_returnsProviderFailureWhenRuntimeCannotGenerate() = runTest {
        providerSettingsRepository.save(
            ProviderSettings(
                modelPath = testModelFile.absolutePath,
                modelName = testModelFile.name,
                userDisplayName = "현우",
            ),
        )
        val useCase = VerifyGemmaModelUseCase(
            providerSettingsRepository = providerSettingsRepository,
            gemmaProviderFactory = { FailingProvider("native runtime failed") },
        )

        val result = useCase()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("native runtime failed"))
        assertFalse(providerSettingsRepository.get().isModelVerified)
    }

    private class StaticAnalysisProvider(
        private val onAnalyze: () -> Unit,
    ) : AnalysisProvider {
        override suspend fun analyze(input: AnalysisInput): AnalysisReport {
            onAnalyze()
            return AnalysisReport(
                headline = "모델 점검",
                contactReadiness = "아주 가볍게 가능",
                evidence = "테스트 응답",
                relationshipSummary = "상대가 마지막에 답장을 남긴 상태입니다.",
                reunionObjective = "짧게 답합니다.",
                nextStep = "짧은 답장만 준비하세요.",
                messageDraft = "메시지 봤어. 고마워.",
                alternativeDrafts = "메시지 봤어. 고마워.\n천천히 이야기하자\n답은 천천히 해도 돼",
                caution = "답을 재촉하지 마세요.",
            )
        }
    }

    private class FailingProvider(
        private val message: String,
    ) : AnalysisProvider {
        override suspend fun analyze(input: AnalysisInput): AnalysisReport {
            error(message)
        }
    }
}
