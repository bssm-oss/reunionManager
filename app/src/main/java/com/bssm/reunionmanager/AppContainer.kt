package com.bssm.reunionmanager

import android.content.Context
import com.bssm.reunionmanager.data.analysis.FakeAnalysisProvider
import com.bssm.reunionmanager.data.analysis.GeminiAnalysisProvider
import com.bssm.reunionmanager.data.importer.KakaoTalkConversationParser
import com.bssm.reunionmanager.data.local.ReunionManagerDatabase
import com.bssm.reunionmanager.data.repository.AnalysisRepository
import com.bssm.reunionmanager.data.repository.ConversationRepository
import com.bssm.reunionmanager.data.repository.ProviderSettingsRepository
import com.bssm.reunionmanager.domain.usecase.GenerateReunionPlanUseCase
import com.bssm.reunionmanager.domain.usecase.ImportConversationUseCase

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    val database: ReunionManagerDatabase by lazy {
        ReunionManagerDatabase.build(applicationContext)
    }

    val conversationRepository: ConversationRepository by lazy {
        ConversationRepository(
            database = database,
            conversationDao = database.conversationDao(),
            participantDao = database.participantDao(),
            messageDao = database.messageDao(),
            analysisResultDao = database.analysisResultDao(),
        )
    }

    val analysisRepository: AnalysisRepository by lazy {
        AnalysisRepository(database.analysisResultDao())
    }

    val providerSettingsRepository: ProviderSettingsRepository by lazy {
        ProviderSettingsRepository(database.providerSettingsDao())
    }

    private val parser: KakaoTalkConversationParser by lazy {
        KakaoTalkConversationParser()
    }

    private val fakeAnalysisProvider by lazy { FakeAnalysisProvider() }

    val importConversationUseCase: ImportConversationUseCase by lazy {
        ImportConversationUseCase(
            parser = parser,
            repository = conversationRepository,
        )
    }

    val generateReunionPlanUseCase: GenerateReunionPlanUseCase by lazy {
        GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = fakeAnalysisProvider,
            geminiProviderFactory = { settings -> GeminiAnalysisProvider(settings) },
        )
    }
}
