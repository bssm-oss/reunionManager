package com.bssm.reunionmanager.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bssm.reunionmanager.data.analysis.FakeAnalysisProvider
import com.bssm.reunionmanager.data.importer.ParsedConversation
import com.bssm.reunionmanager.data.importer.ParsedMessage
import com.bssm.reunionmanager.data.local.ReunionManagerDatabase
import com.bssm.reunionmanager.data.repository.AnalysisRepository
import com.bssm.reunionmanager.data.repository.ConversationRepository
import com.bssm.reunionmanager.data.repository.ProviderSettingsRepository
import com.bssm.reunionmanager.domain.analysis.AnalysisProvider
import com.bssm.reunionmanager.domain.model.AnalysisInput
import com.bssm.reunionmanager.domain.model.AnalysisReport
import com.bssm.reunionmanager.domain.model.GemmaBackend
import com.bssm.reunionmanager.domain.model.ImportConversationResult
import com.bssm.reunionmanager.domain.model.ProviderSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

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
    fun invoke_requiresUserNameBeforeDraftingWhenFakeProviderIsUsed() = runTest {
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
            gemmaProviderFactory = { error("Gemma provider should not be used without a model path.") },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        assertEquals("fake", result.getOrNull())

        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        assertTrue(detail.latestAnalysis != null)
        val report = requireNotNull(detail.latestAnalysis)
        assertTrue(report.headline.contains("내 이름"))
        assertEquals("정보 부족", report.contactReadiness)
        assertTrue(report.evidence.contains("관점 확인"))
        assertTrue(report.messageDraft.contains("보낼 문장을 만들지 않습니다"))
        assertTrue(report.messageDraft.contains("내 카톡 이름"))
        assertTrue(report.alternativeDrafts.contains("내 카톡 이름 저장하기"))
        assertFalse(report.messageDraft.contains("오랜만이야"))
    }

    @Test
    fun invoke_usesFakeProviderWhenModelPathIsMissingAndUserNameIsConfigured() = runTest {
        val importedId = (conversationRepository.importConversation(
            parsedConversation = counterpartWaitingConversation,
            rawText = "fake configured raw text",
            sourceName = "fake-configured.txt",
        ) as ImportConversationResult.Imported).conversationId
        providerSettingsRepository.save(ProviderSettings(userDisplayName = "현우"))

        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = { error("Gemma provider should not be used without a model path.") },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        assertEquals("fake", result.getOrNull())
        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        val report = requireNotNull(detail.latestAnalysis)
        assertEquals("아주 가볍게 가능", report.contactReadiness)
        assertTrue(report.nextStep.contains("상대의 마지막 메시지"))
        assertTrue(report.messageDraft.contains("메시지 봤어"))
    }

    @Test
    fun invoke_fakeProviderStoresHoldResultWhenRecentMessagesLookUnanswered() = runTest {
        val importedId = (conversationRepository.importConversation(
            parsedConversation = unansweredConversation,
            rawText = "unanswered raw text",
            sourceName = "unanswered.txt",
        ) as ImportConversationResult.Imported).conversationId
        providerSettingsRepository.save(ProviderSettings(userDisplayName = "현우"))

        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = { error("Gemma provider should not be used without a model path.") },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        val report = requireNotNull(detail.latestAnalysis)
        assertEquals("지금은 보류", report.contactReadiness)
        assertTrue(report.evidence.contains("연속 발화"))
        assertTrue(report.messageDraft.contains("보내지 않습니다"))
    }

    @Test
    fun invoke_usesGemmaProviderWhenModelPathIsConfigured() = runTest {
        val importedId = importSampleConversation()
        providerSettingsRepository.save(
            ProviderSettings(
                modelPath = "/data/local/tmp/gemma-4-E4B-it.litertlm",
                modelName = "gemma-4-E4B-it.litertlm",
                backend = GemmaBackend.GPU,
                userDisplayName = "현우",
            ),
        )

        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = { settings ->
                assertEquals("/data/local/tmp/gemma-4-E4B-it.litertlm", settings.modelPath)
                assertEquals(GemmaBackend.GPU, settings.backend)
                StaticAnalysisProvider(
                    AnalysisReport(
                        headline = "Mock Gemma headline",
                        contactReadiness = "아주 가볍게 가능",
                        evidence = "Mock Gemma evidence",
                        relationshipSummary = "Mock Gemma relationship summary",
                        reunionObjective = "Mock Gemma reunion objective",
                        nextStep = "Mock Gemma next step",
                        messageDraft = "Mock Gemma message draft",
                        alternativeDrafts = "Mock option 1\nMock option 2\nMock option 3",
                        caution = "Mock Gemma caution",
                    ),
                )
            },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        assertEquals("gemma4", result.getOrNull())

        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        assertNotNull(detail.latestAnalysis)
        assertEquals("Mock Gemma headline", detail.latestAnalysis!!.headline)
        assertEquals("아주 가볍게 가능", detail.latestAnalysis!!.contactReadiness)
        assertEquals("Mock Gemma evidence", detail.latestAnalysis!!.evidence)
        assertEquals("Mock Gemma relationship summary", detail.latestAnalysis!!.relationshipSummary)
        assertEquals("Mock Gemma message draft", detail.latestAnalysis!!.messageDraft)
        assertTrue(detail.latestAnalysis!!.alternativeDrafts.contains("Mock option"))
    }

    @Test
    fun invoke_guardrailsConfiguredGemmaProviderWhenConversationLooksUnanswered() = runTest {
        val importedId = (conversationRepository.importConversation(
            parsedConversation = unansweredConversation,
            rawText = "configured unanswered raw text",
            sourceName = "configured-unanswered.txt",
        ) as ImportConversationResult.Imported).conversationId
        providerSettingsRepository.save(
            ProviderSettings(
                modelPath = "/data/local/tmp/gemma-4-E4B-it.litertlm",
                modelName = "gemma-4-E4B-it.litertlm",
                backend = GemmaBackend.CPU,
                userDisplayName = "현우",
            ),
        )

        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = {
                StaticAnalysisProvider(
                    AnalysisReport(
                        headline = "Mock Gemma optimistic headline",
                        contactReadiness = "아주 가볍게 가능",
                        evidence = "Mock Gemma missed the final run",
                        relationshipSummary = "Mock Gemma summary",
                        reunionObjective = "Mock Gemma objective",
                        nextStep = "Mock Gemma next step",
                        messageDraft = "오랜만이야. 갑자기 연락했어.",
                        alternativeDrafts = "오랜만이야\n잘 지내?\n잠깐 얘기할 수 있어?",
                        caution = "Mock Gemma caution",
                    ),
                )
            },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        assertEquals("gemma4", result.getOrNull())
        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        val report = requireNotNull(detail.latestAnalysis)
        assertEquals("지금은 보류", report.contactReadiness)
        assertTrue(report.evidence.contains("규칙 보정"))
        assertTrue(report.messageDraft.contains("보내지 않습니다"))
        assertTrue(report.alternativeDrafts.contains("오늘은 보내지 않기"))
        assertFalse(report.messageDraft.contains("오랜만이야"))
        assertFalse(report.nextStep.contains("Mock Gemma next step"))
    }

    @Test
    fun invoke_guardrailsConfiguredGemmaProviderWhenUserNameIsMissing() = runTest {
        val importedId = importSampleConversation()
        providerSettingsRepository.save(
            ProviderSettings(
                modelPath = "/data/local/tmp/gemma-4-E4B-it.litertlm",
                modelName = "gemma-4-E4B-it.litertlm",
                backend = GemmaBackend.CPU,
            ),
        )

        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = {
                StaticAnalysisProvider(
                    AnalysisReport(
                        headline = "Mock Gemma optimistic headline",
                        contactReadiness = "아주 가볍게 가능",
                        evidence = "Mock Gemma assumed the speaker perspective",
                        relationshipSummary = "Mock Gemma summary",
                        reunionObjective = "바로 안부를 보내 반응을 확인합니다.",
                        nextStep = "지금 짧게 연락하세요.",
                        messageDraft = "오랜만이야. 괜찮다면 짧게 안부만 묻고 싶어.",
                        alternativeDrafts = "오랜만이야\n잘 지내?\n잠깐 얘기할 수 있어?",
                        caution = "Mock Gemma caution",
                    ),
                )
            },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        assertEquals("gemma4", result.getOrNull())
        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        val report = requireNotNull(detail.latestAnalysis)
        assertEquals("내 이름 확인", report.headline)
        assertEquals("정보 부족", report.contactReadiness)
        assertTrue(report.evidence.contains("관점 확인"))
        assertTrue(report.messageDraft.contains("보낼 문장을 만들지 않습니다"))
        assertTrue(report.messageDraft.contains("내 카톡 이름"))
        assertTrue(report.alternativeDrafts.contains("내 카톡 이름 저장하기"))
        assertFalse(report.messageDraft.contains("오랜만이야"))
        assertFalse(report.nextStep.contains("지금 짧게"))
    }

    @Test
    fun invoke_guardrailsConfiguredGemmaProviderWhenCounterpartSetsBoundary() = runTest {
        val importedId = (conversationRepository.importConversation(
            parsedConversation = boundaryConversation,
            rawText = "configured boundary raw text",
            sourceName = "configured-boundary.txt",
        ) as ImportConversationResult.Imported).conversationId
        providerSettingsRepository.save(
            ProviderSettings(
                modelPath = "/data/local/tmp/gemma-4-E4B-it.litertlm",
                modelName = "gemma-4-E4B-it.litertlm",
                backend = GemmaBackend.CPU,
                userDisplayName = "현우",
            ),
        )

        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = {
                StaticAnalysisProvider(
                    AnalysisReport(
                        headline = "Mock Gemma ignored boundary headline",
                        contactReadiness = "아주 가볍게 가능",
                        evidence = "Mock Gemma missed explicit boundary",
                        relationshipSummary = "Mock Gemma summary",
                        reunionObjective = "Mock Gemma objective",
                        nextStep = "지금 바로 짧게 연락하세요.",
                        messageDraft = "오랜만이야. 잠깐 얘기할 수 있어?",
                        alternativeDrafts = "오랜만이야\n잘 지내?\n잠깐 얘기할 수 있어?",
                        caution = "Mock Gemma caution",
                    ),
                )
            },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        val report = requireNotNull(detail.latestAnalysis)
        assertEquals("지금은 보류", report.contactReadiness)
        assertTrue(report.evidence.contains("규칙 보정"))
        assertTrue(report.evidence.contains("연락하지"))
        assertTrue(report.messageDraft.contains("보내지 않습니다"))
        assertFalse(report.nextStep.contains("바로"))
    }

    @Test
    fun invoke_guardrailsConfiguredGemmaProviderWhenCounterpartHasMovedOn() = runTest {
        val importedId = (conversationRepository.importConversation(
            parsedConversation = movedOnConversation,
            rawText = "configured moved on raw text",
            sourceName = "configured-moved-on.txt",
        ) as ImportConversationResult.Imported).conversationId
        providerSettingsRepository.save(
            ProviderSettings(
                modelPath = "/data/local/tmp/gemma-4-E4B-it.litertlm",
                modelName = "gemma-4-E4B-it.litertlm",
                backend = GemmaBackend.CPU,
                userDisplayName = "현우",
            ),
        )

        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = {
                StaticAnalysisProvider(
                    AnalysisReport(
                        headline = "Mock Gemma optimistic headline",
                        contactReadiness = "아주 가볍게 가능",
                        evidence = "Mock Gemma missed moved-on context",
                        relationshipSummary = "Mock Gemma summary",
                        reunionObjective = "지금 안부를 보내 반응을 확인합니다.",
                        nextStep = "지금 짧게 연락하세요.",
                        messageDraft = "오랜만이야. 괜찮다면 짧게 안부만 묻고 싶어.",
                        alternativeDrafts = "오랜만이야\n잘 지내?\n잠깐 얘기할 수 있어?",
                        caution = "Mock Gemma caution",
                    ),
                )
            },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        val report = requireNotNull(detail.latestAnalysis)
        assertEquals("지금은 보류", report.contactReadiness)
        assertTrue(report.evidence.contains("규칙 보정"))
        assertTrue(report.evidence.contains("새로 만나는 사람"))
        assertTrue(report.messageDraft.contains("보내지 않습니다"))
        assertFalse(report.messageDraft.contains("안부"))
    }

    @Test
    fun invoke_guardrailsConfiguredGemmaProviderWhenChatLooksTechnicalOrGroup() = runTest {
        val importedId = (conversationRepository.importConversation(
            parsedConversation = technicalGroupConversation,
            rawText = "technical group raw text",
            sourceName = "technical-group.txt",
        ) as ImportConversationResult.Imported).conversationId
        providerSettingsRepository.save(
            ProviderSettings(
                modelPath = "/data/local/tmp/gemma-4-E4B-it.litertlm",
                modelName = "gemma-4-E4B-it.litertlm",
                backend = GemmaBackend.CPU,
                userDisplayName = "현우",
            ),
        )

        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = {
                StaticAnalysisProvider(
                    AnalysisReport(
                        headline = "Mock Gemma optimistic headline",
                        contactReadiness = "아주 가볍게 가능",
                        evidence = "Mock Gemma treated work chat as reunion evidence",
                        relationshipSummary = "Mock Gemma summary",
                        reunionObjective = "바로 안부를 보내 반응을 확인합니다.",
                        nextStep = "지금 짧게 연락하세요.",
                        messageDraft = "오랜만이야. 괜찮다면 짧게 안부만 묻고 싶어.",
                        alternativeDrafts = "오랜만이야\n잘 지내?\n잠깐 얘기할 수 있어?",
                        caution = "Mock Gemma caution",
                    ),
                )
            },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        val report = requireNotNull(detail.latestAnalysis)
        assertEquals("관계 맥락 확인", report.headline)
        assertEquals("정보 부족", report.contactReadiness)
        assertTrue(report.evidence.contains("맥락 확인"))
        assertTrue(report.messageDraft.contains("보낼 문장을 만들지 않습니다"))
        assertTrue(report.alternativeDrafts.contains("더 관련 있는 대화 파일"))
        assertFalse(report.messageDraft.contains("오랜만이야"))
        assertFalse(report.nextStep.contains("지금 짧게"))
    }

    @Test
    fun invoke_sanitizesConfiguredGemmaProviderShapeBeforeSaving() = runTest {
        val importedId = importSampleConversation()
        providerSettingsRepository.save(
            ProviderSettings(
                modelPath = "/data/local/tmp/gemma-4-E4B-it.litertlm",
                modelName = "gemma-4-E4B-it.litertlm",
                backend = GemmaBackend.CPU,
                userDisplayName = "현우",
            ),
        )

        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = {
                StaticAnalysisProvider(
                    AnalysisReport(
                        headline = "",
                        contactReadiness = "가능성이 높으니 적극적으로 연락",
                        evidence = "",
                        relationshipSummary = "",
                        reunionObjective = "",
                        nextStep = "",
                        messageDraft = "지금 당장 길게 연락해서 모든 감정을 설명하고 답을 받아내는 것이 좋습니다. 상대가 답하지 않아도 여러 번 보내서 마음을 정확히 확인하세요.",
                        alternativeDrafts = "바로 전화해\n길게 설명해\n답을 재촉해\n집 앞에 찾아가",
                        caution = "",
                    ),
                )
            },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        val report = requireNotNull(detail.latestAnalysis)
        assertEquals("정보 부족", report.contactReadiness)
        assertTrue(report.headline.isNotBlank())
        assertTrue(report.evidence.isNotBlank())
        assertTrue(report.messageDraft.length <= 90)
        assertFalse(report.messageDraft.contains("여러 번"))
        assertEquals(3, report.alternativeDrafts.lines().size)
        assertFalse(report.alternativeDrafts.contains("집 앞"))
    }

    @Test
    fun invoke_sanitizesConfiguredGemmaProviderWithVeryLongGapContext() = runTest {
        val importedId = (conversationRepository.importConversation(
            parsedConversation = longGapFirstContactConversation,
            rawText = "long gap first contact raw text",
            sourceName = "long-gap-first-contact.txt",
        ) as ImportConversationResult.Imported).conversationId
        providerSettingsRepository.save(
            ProviderSettings(
                modelPath = "/data/local/tmp/gemma-4-E4B-it.litertlm",
                modelName = "gemma-4-E4B-it.litertlm",
                backend = GemmaBackend.CPU,
                userDisplayName = "현우",
            ),
        )

        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = {
                StaticAnalysisProvider(
                    AnalysisReport(
                        headline = "",
                        contactReadiness = "적극 연락 가능",
                        evidence = "",
                        relationshipSummary = "",
                        reunionObjective = "",
                        nextStep = "",
                        messageDraft = "",
                        alternativeDrafts = "",
                        caution = "",
                    ),
                )
            },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        val report = requireNotNull(detail.latestAnalysis)
        assertEquals("정보 부족", report.contactReadiness)
        assertTrue(report.nextStep.contains("오늘은 보내지 말고"))
        assertTrue(report.messageDraft.contains("보낼 문장을 만들지 않습니다"))
        assertTrue(report.alternativeDrafts.contains("더 관련 있는 최근 대화"))
    }

    @Test
    fun invoke_fakeProviderTreatsOldLastMessageAsVeryLongGap() = runTest {
        val importedId = (conversationRepository.importConversation(
            parsedConversation = staleAfterExportConversation,
            rawText = "stale after export raw text",
            sourceName = "stale-after-export.txt",
        ) as ImportConversationResult.Imported).conversationId
        providerSettingsRepository.save(ProviderSettings(userDisplayName = "현우"))

        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = { error("Gemma provider should not be used without a model path.") },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        val report = requireNotNull(detail.latestAnalysis)
        assertEquals("아주 가볍게 가능", report.contactReadiness)
        assertTrue(report.evidence.contains("마지막 메시지 이후 경과"))
        assertTrue(report.nextStep.contains("오래 끊긴 대화"))
        assertTrue(report.messageDraft.contains("잘 지내는지만"))
    }

    @Test
    fun invoke_guardrailsConfiguredGemmaProviderToReplyWhenCounterpartIsWaiting() = runTest {
        val importedId = (conversationRepository.importConversation(
            parsedConversation = counterpartWaitingConversation,
            rawText = "counterpart waiting raw text",
            sourceName = "counterpart-waiting.txt",
        ) as ImportConversationResult.Imported).conversationId
        providerSettingsRepository.save(
            ProviderSettings(
                modelPath = "/data/local/tmp/gemma-4-E4B-it.litertlm",
                modelName = "gemma-4-E4B-it.litertlm",
                backend = GemmaBackend.CPU,
                userDisplayName = "현우",
            ),
        )

        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = {
                StaticAnalysisProvider(
                    AnalysisReport(
                        headline = "Mock Gemma first contact headline",
                        contactReadiness = "아주 가볍게 가능",
                        evidence = "Mock Gemma evidence",
                        relationshipSummary = "Mock Gemma relationship summary",
                        reunionObjective = "Mock Gemma reunion objective",
                        nextStep = "Mock Gemma next step",
                        messageDraft = "잘 지냈어?",
                        alternativeDrafts = "오랜만이야\n잘 지내?\n잠깐 얘기할 수 있어?",
                        caution = "Mock Gemma caution",
                    ),
                )
            },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        val report = requireNotNull(detail.latestAnalysis)
        assertEquals("아주 가볍게 가능", report.contactReadiness)
        assertTrue(report.evidence.contains("상대가 마지막에 메시지를 남긴 상태"))
        assertTrue(report.messageDraft.contains("메시지 봤어"))
        assertFalse(report.messageDraft.contains("답이 늦었네"))
        assertTrue(report.alternativeDrafts.contains("메시지 봤어"))
        assertFalse(report.nextStep.contains("Mock Gemma next step"))
    }

    @Test
    fun invoke_preservesSafeSpecificGemmaReplyWhenCounterpartIsWaiting() = runTest {
        val importedId = (conversationRepository.importConversation(
            parsedConversation = counterpartWaitingConversation,
            rawText = "specific counterpart waiting raw text",
            sourceName = "specific-counterpart-waiting.txt",
        ) as ImportConversationResult.Imported).conversationId
        providerSettingsRepository.save(
            ProviderSettings(
                modelPath = "/data/local/tmp/gemma-4-E4B-it.litertlm",
                modelName = "gemma-4-E4B-it.litertlm",
                backend = GemmaBackend.CPU,
                userDisplayName = "현우",
            ),
        )

        val specificDraft = "말해줘서 고마워. 나도 부담 없이 천천히 이야기하고 싶어."
        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = {
                StaticAnalysisProvider(
                    AnalysisReport(
                        headline = "Mock Gemma specific reply",
                        contactReadiness = "아주 가볍게 가능",
                        evidence = "상대가 괜찮다면 천천히 이야기해도 된다고 말했습니다.",
                        relationshipSummary = "상대가 대화를 완전히 닫지 않고 낮은 압박의 답장을 남긴 상태입니다.",
                        reunionObjective = "상대의 속도를 존중하면서 짧게 답합니다.",
                        nextStep = "상대가 남긴 말에 고맙다고 답합니다.",
                        messageDraft = specificDraft,
                        alternativeDrafts = "$specificDraft\n메시지 봤어. 고마워. 천천히 이야기해도 괜찮아.",
                        caution = "길게 설명하지 않습니다.",
                    ),
                )
            },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        val report = requireNotNull(detail.latestAnalysis)
        assertEquals(specificDraft, report.messageDraft)
        assertTrue(report.alternativeDrafts.lines().first().contains("말해줘서 고마워"))
    }

    @Test
    fun invoke_guardrailsConfiguredGemmaProviderUsesCounterpartLastMessageForReplyDraft() = runTest {
        val importedId = (conversationRepository.importConversation(
            parsedConversation = counterpartWellBeingConversation,
            rawText = "counterpart well being raw text",
            sourceName = "counterpart-well-being.txt",
        ) as ImportConversationResult.Imported).conversationId
        providerSettingsRepository.save(
            ProviderSettings(
                modelPath = "/data/local/tmp/gemma-4-E4B-it.litertlm",
                modelName = "gemma-4-E4B-it.litertlm",
                backend = GemmaBackend.CPU,
                userDisplayName = "현우",
            ),
        )

        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = {
                StaticAnalysisProvider(
                    AnalysisReport(
                        headline = "Mock Gemma generic reply",
                        contactReadiness = "아주 가볍게 가능",
                        evidence = "Mock Gemma evidence",
                        relationshipSummary = "Mock Gemma relationship summary",
                        reunionObjective = "Mock Gemma reunion objective",
                        nextStep = "Mock Gemma next step",
                        messageDraft = "잘 지냈어?",
                        alternativeDrafts = "잘 지냈어?\n잠깐 얘기할 수 있어?\n오랜만이야",
                        caution = "Mock Gemma caution",
                    ),
                )
            },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        val report = requireNotNull(detail.latestAnalysis)
        assertTrue(report.messageDraft.contains("메시지 봤어"))
        assertTrue(report.messageDraft.contains("나는 잘 지내고 있어"))
        assertTrue(report.alternativeDrafts.contains("나는 잘 지내고 있어"))
    }

    @Test
    fun invoke_guardrailsConfiguredGemmaProviderConfirmsCounterpartMeetingPlan() = runTest {
        val importedId = (conversationRepository.importConversation(
            parsedConversation = counterpartScheduledMeetingConversation,
            rawText = "counterpart scheduled meeting raw text",
            sourceName = "counterpart-scheduled-meeting.txt",
        ) as ImportConversationResult.Imported).conversationId
        providerSettingsRepository.save(
            ProviderSettings(
                modelPath = "/data/local/tmp/gemma-4-E4B-it.litertlm",
                modelName = "gemma-4-E4B-it.litertlm",
                backend = GemmaBackend.CPU,
                userDisplayName = "현우",
            ),
        )

        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = {
                StaticAnalysisProvider(
                    AnalysisReport(
                        headline = "Mock Gemma generic reply",
                        contactReadiness = "아주 가볍게 가능",
                        evidence = "Mock Gemma evidence",
                        relationshipSummary = "Mock Gemma relationship summary",
                        reunionObjective = "Mock Gemma reunion objective",
                        nextStep = "Mock Gemma next step",
                        messageDraft = "오랜만이야. 잘 지냈어?",
                        alternativeDrafts = "오랜만이야\n잘 지내?\n잠깐 얘기할 수 있어?",
                        caution = "Mock Gemma caution",
                    ),
                )
            },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        val report = requireNotNull(detail.latestAnalysis)
        assertTrue(report.messageDraft.contains("약속한 시간"))
        assertTrue(report.messageDraft.contains("고마워"))
        assertTrue(report.alternativeDrafts.contains("그때 보자"))
        assertFalse(report.messageDraft.contains("오랜만이야"))
    }

    @Test
    fun invoke_guardrailsConfiguredGemmaProviderAnswersScheduleQuestion() = runTest {
        val importedId = (conversationRepository.importConversation(
            parsedConversation = counterpartScheduleQuestionConversation,
            rawText = "counterpart schedule question raw text",
            sourceName = "counterpart-schedule-question.txt",
        ) as ImportConversationResult.Imported).conversationId
        providerSettingsRepository.save(
            ProviderSettings(
                modelPath = "/data/local/tmp/gemma-4-E4B-it.litertlm",
                modelName = "gemma-4-E4B-it.litertlm",
                backend = GemmaBackend.CPU,
                userDisplayName = "현우",
            ),
        )

        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = {
                StaticAnalysisProvider(
                    AnalysisReport(
                        headline = "Mock Gemma first contact",
                        contactReadiness = "아주 가볍게 가능",
                        evidence = "Mock Gemma evidence",
                        relationshipSummary = "Mock Gemma relationship summary",
                        reunionObjective = "새 연락을 시작합니다.",
                        nextStep = "지금 안부를 보냅니다.",
                        messageDraft = "오랜만이야. 잘 지냈어?",
                        alternativeDrafts = "오랜만이야\n잘 지내?\n잠깐 얘기할 수 있어?",
                        caution = "Mock Gemma caution",
                    ),
                )
            },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        val report = requireNotNull(detail.latestAnalysis)
        assertEquals("아주 가볍게 가능", report.contactReadiness)
        assertTrue(report.messageDraft.contains("가능한지 확인"))
        assertTrue(report.alternativeDrafts.contains("가능한 시간 확인"))
        assertFalse(report.messageDraft.contains("오랜만이야"))
        assertFalse(report.messageDraft.contains("약속한 시간"))
    }

    @Test
    fun invoke_returnsFailureWhenConfiguredGemmaProviderErrors() = runTest {
        val importedId = importSampleConversation()
        providerSettingsRepository.save(
            ProviderSettings(
                modelPath = "/data/local/tmp/missing-gemma-4-E4B-it.litertlm",
                modelName = "gemma-4-E4B-it.litertlm",
                backend = GemmaBackend.CPU,
            )
        )

        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = { FailingAnalysisProvider("모델 파일을 찾을 수 없습니다.") },
        )

        val result = useCase(importedId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("모델 파일을 찾을 수 없습니다."))

        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        assertFalse(detail.latestAnalysis != null)
    }

    private suspend fun importSampleConversation(): Long {
        return (conversationRepository.importConversation(
            parsedConversation = sampleParsedConversation,
            rawText = "analysis raw text",
            sourceName = "analysis.txt",
        ) as ImportConversationResult.Imported).conversationId
    }

    private class StaticAnalysisProvider(
        private val report: AnalysisReport,
    ) : AnalysisProvider {
        override suspend fun analyze(input: AnalysisInput): AnalysisReport = report
    }

    private class FailingAnalysisProvider(
        private val message: String,
    ) : AnalysisProvider {
        override suspend fun analyze(input: AnalysisInput): AnalysisReport {
            error(message)
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

        val unansweredConversation = ParsedConversation(
            title = "무응답 테스트",
            exportedAtEpochMillis = null,
            participants = listOf("민지", "현우"),
            messages = listOf(
                ParsedMessage(
                    senderName = "민지",
                    sentAtEpochMillis = 1_710_000_000_000,
                    content = "오늘은 조금 피곤해서 다음에 얘기하자",
                ),
                ParsedMessage(
                    senderName = "현우",
                    sentAtEpochMillis = 1_710_000_060_000,
                    content = "알겠어",
                ),
                ParsedMessage(
                    senderName = "현우",
                    sentAtEpochMillis = 1_710_010_000_000,
                    content = "혹시 잠깐 괜찮아?",
                ),
                ParsedMessage(
                    senderName = "현우",
                    sentAtEpochMillis = 1_710_010_060_000,
                    content = "답 없어서 다시 남겨",
                ),
                ParsedMessage(
                    senderName = "현우",
                    sentAtEpochMillis = 1_710_010_120_000,
                    content = "미안해. 오늘은 더 보내지 않을게",
                ),
            ),
        )

        val boundaryConversation = ParsedConversation(
            title = "경계 표현 테스트",
            exportedAtEpochMillis = null,
            participants = listOf("민지", "현우"),
            messages = listOf(
                ParsedMessage(
                    senderName = "현우",
                    sentAtEpochMillis = 1_710_000_000_000,
                    content = "잠깐만 이야기할 수 있을까?",
                ),
                ParsedMessage(
                    senderName = "민지",
                    sentAtEpochMillis = 1_710_000_060_000,
                    content = "이제 연락하지 말아줘. 우리 관계는 정리하자",
                ),
            ),
        )

        val movedOnConversation = ParsedConversation(
            title = "상대 거리두기 테스트",
            exportedAtEpochMillis = null,
            participants = listOf("민지", "현우"),
            messages = listOf(
                ParsedMessage(
                    senderName = "현우",
                    sentAtEpochMillis = 1_710_000_000_000,
                    content = "오랜만이야. 괜찮다면 한 번 이야기할 수 있을까?",
                ),
                ParsedMessage(
                    senderName = "민지",
                    sentAtEpochMillis = 1_710_000_060_000,
                    content = "나 새로 만나는 사람 있어",
                ),
                ParsedMessage(
                    senderName = "민지",
                    sentAtEpochMillis = 1_710_000_120_000,
                    content = "우리도 이제 각자 잘 지내자",
                ),
            ),
        )

        val technicalGroupConversation = ParsedConversation(
            title = "LLM 프로젝트 회의",
            exportedAtEpochMillis = null,
            participants = listOf("민지", "현우", "준호"),
            messages = listOf(
                ParsedMessage(
                    senderName = "현우",
                    sentAtEpochMillis = 1_710_000_000_000,
                    content = "오늘 RAG 테스트 결과 정리했어",
                ),
                ParsedMessage(
                    senderName = "민지",
                    sentAtEpochMillis = 1_710_000_060_000,
                    content = "OpenAI API 응답 지연이 있어서 모델 설정 다시 볼게",
                ),
                ParsedMessage(
                    senderName = "준호",
                    sentAtEpochMillis = 1_710_000_120_000,
                    content = "임베딩 배포 로그도 회의 전에 공유할게",
                ),
            ),
        )

        val longGapFirstContactConversation = ParsedConversation(
            title = "긴 공백 첫 연락 테스트",
            exportedAtEpochMillis = null,
            participants = listOf("민지", "현우"),
            messages = listOf(
                ParsedMessage(
                    senderName = "민지",
                    sentAtEpochMillis = 1_700_000_000_000,
                    content = "그동안 고마웠어",
                ),
                ParsedMessage(
                    senderName = "현우",
                    sentAtEpochMillis = 1_704_000_000_000,
                    content = "나도 고마웠어. 잘 지내",
                ),
            ),
        )

        val counterpartWaitingConversation = ParsedConversation(
            title = "상대 답장 대기 테스트",
            exportedAtEpochMillis = null,
            participants = listOf("민지", "현우"),
            messages = listOf(
                ParsedMessage(
                    senderName = "현우",
                    sentAtEpochMillis = 1_710_000_000_000,
                    content = "잘 지냈어?",
                ),
                ParsedMessage(
                    senderName = "민지",
                    sentAtEpochMillis = 1_710_000_060_000,
                    content = "응, 나도 가끔 생각났어",
                ),
                ParsedMessage(
                    senderName = "민지",
                    sentAtEpochMillis = 1_710_000_120_000,
                    content = "괜찮다면 천천히 이야기해도 돼",
                ),
            ),
        )

        val counterpartWellBeingConversation = ParsedConversation(
            title = "상대 안부 질문 테스트",
            exportedAtEpochMillis = null,
            participants = listOf("민지", "현우"),
            messages = listOf(
                ParsedMessage(
                    senderName = "현우",
                    sentAtEpochMillis = 1_710_000_000_000,
                    content = "오랜만이야",
                ),
                ParsedMessage(
                    senderName = "민지",
                    sentAtEpochMillis = 1_710_000_060_000,
                    content = "나도 가끔 생각났어",
                ),
                ParsedMessage(
                    senderName = "민지",
                    sentAtEpochMillis = 1_710_000_120_000,
                    content = "잘 지내?",
                ),
            ),
        )

        val counterpartScheduledMeetingConversation = ParsedConversation(
            title = "상대 약속 제안 테스트",
            exportedAtEpochMillis = null,
            participants = listOf("민지", "현우"),
            messages = listOf(
                ParsedMessage(
                    senderName = "현우",
                    sentAtEpochMillis = 1_710_000_000_000,
                    content = "괜찮다면 짧게 얼굴 볼 수 있을까?",
                ),
                ParsedMessage(
                    senderName = "민지",
                    sentAtEpochMillis = 1_710_000_060_000,
                    content = "내일 7시에 카페에서 보자",
                ),
            ),
        )

        val counterpartScheduleQuestionConversation = ParsedConversation(
            title = "상대 일정 질문 테스트",
            exportedAtEpochMillis = null,
            participants = listOf("민지", "현우"),
            messages = listOf(
                ParsedMessage(
                    senderName = "현우",
                    sentAtEpochMillis = 1_710_000_000_000,
                    content = "괜찮다면 짧게 얼굴 볼 수 있을까?",
                ),
                ParsedMessage(
                    senderName = "민지",
                    sentAtEpochMillis = 1_710_000_060_000,
                    content = "토요일 저녁에 시간 돼?",
                ),
            ),
        )

        val staleAfterExportConversation = ParsedConversation(
            title = "오래 지난 마지막 메시지 테스트",
            exportedAtEpochMillis = 1_708_000_000_000,
            participants = listOf("민지", "현우"),
            messages = listOf(
                ParsedMessage(
                    senderName = "민지",
                    sentAtEpochMillis = 1_700_000_000_000,
                    content = "나도 가끔 생각났어",
                ),
                ParsedMessage(
                    senderName = "현우",
                    sentAtEpochMillis = 1_700_000_060_000,
                    content = "나도 잘 지내는지 궁금했어",
                ),
            ),
        )
    }
}
