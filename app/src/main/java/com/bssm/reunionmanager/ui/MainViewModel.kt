package com.bssm.reunionmanager.ui

import android.app.Application
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bssm.reunionmanager.ReunionManagerApplication
import com.bssm.reunionmanager.domain.model.ConversationDetail
import com.bssm.reunionmanager.domain.model.ConversationSummary
import com.bssm.reunionmanager.domain.model.GemmaBackend
import com.bssm.reunionmanager.domain.model.ImportConversationResult
import com.bssm.reunionmanager.domain.model.ProviderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContainer = (application as ReunionManagerApplication).appContainer
    private val contentResolver = application.contentResolver

    val conversations: StateFlow<List<ConversationSummary>> = appContainer.conversationRepository
        .observeConversationSummaries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val providerSettings: StateFlow<ProviderSettings> = appContainer.providerSettingsRepository
        .observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProviderSettings(),
        )

    private val _importState = MutableStateFlow(ImportUiState())
    val importState: StateFlow<ImportUiState> = _importState.asStateFlow()

    private val _analysisStates = MutableStateFlow<Map<Long, AnalysisUiState>>(emptyMap())
    val analysisStates: StateFlow<Map<Long, AnalysisUiState>> = _analysisStates.asStateFlow()

    private val _modelSettingsState = MutableStateFlow(ModelSettingsUiState())
    val modelSettingsState: StateFlow<ModelSettingsUiState> = _modelSettingsState.asStateFlow()

    fun observeConversationDetail(conversationId: Long): Flow<ConversationDetail?> {
        return appContainer.conversationRepository.observeConversationDetail(conversationId)
    }

    fun importConversation(uri: Uri) {
        viewModelScope.launch {
            _importState.value = ImportUiState(isLoading = true)

            runCatching {
                withContext(Dispatchers.IO) {
                    val sourceName = resolveDisplayName(contentResolver, uri)
                    val rawText = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("선택한 파일을 읽지 못했습니다.")
                    appContainer.importConversationUseCase(sourceName = sourceName, rawText = rawText)
                }
            }.onSuccess { result ->
                _importState.value = when (result) {
                    is ImportConversationResult.Imported -> ImportUiState(
                        importedConversationId = result.conversationId,
                        message = "대화가 이 기기에 저장되었습니다.",
                    )

                    is ImportConversationResult.Duplicate -> ImportUiState(
                        importedConversationId = result.conversationId,
                        message = "이미 가져온 대화라 저장된 대화를 다시 사용합니다.",
                    )
                }
            }.onFailure { throwable ->
                _importState.value = ImportUiState(errorMessage = throwable.message ?: "대화를 가져오지 못했습니다.")
            }
        }
    }

    fun clearImportMessage() {
        _importState.value = ImportUiState()
    }

    fun saveProviderSettings(
        modelPath: String,
        modelName: String,
        backend: String,
        userDisplayName: String,
    ) {
        viewModelScope.launch {
            appContainer.providerSettingsRepository.save(
                ProviderSettings(
                    modelPath = modelPath.trim(),
                    modelName = modelName.trim().ifBlank { ProviderSettings.DEFAULT_MODEL },
                    backend = GemmaBackend.fromStoredValue(backend),
                    userDisplayName = userDisplayName.trim(),
                ),
            )
        }
    }

    fun importGemmaModel(uri: Uri) {
        viewModelScope.launch {
            _modelSettingsState.value = ModelSettingsUiState(isLoading = true)

            runCatching {
                val sourceName = resolveDisplayName(contentResolver, uri)
                require(sourceName.endsWith(".litertlm", ignoreCase = true)) {
                    "모델 파일을 선택하세요."
                }
                val destination = copyModelToAppStorage(uri, sourceName)
                val currentSettings = appContainer.providerSettingsRepository.get()
                appContainer.providerSettingsRepository.save(
                    ProviderSettings(
                        modelPath = destination.absolutePath,
                        modelName = sourceName,
                        backend = currentSettings.backend,
                        userDisplayName = currentSettings.userDisplayName,
                    ),
                )
                sourceName
            }.onSuccess { modelName ->
                _modelSettingsState.value = ModelSettingsUiState(
                    message = "$modelName 모델을 사용할 수 있습니다.",
                )
            }.onFailure { throwable ->
                _modelSettingsState.value = ModelSettingsUiState(
                    errorMessage = throwable.message ?: "모델을 가져오지 못했습니다.",
                )
            }
        }
    }

    fun generateAnalysis(conversationId: Long) {
        _analysisStates.update { states ->
            states + (conversationId to AnalysisUiState(isRunning = true))
        }

        viewModelScope.launch {
            appContainer.generateReunionPlanUseCase(conversationId)
                .onSuccess { providerType ->
                    _analysisStates.update { states ->
                        states + (conversationId to AnalysisUiState(providerType = providerType))
                    }
                }
                .onFailure { throwable ->
                    _analysisStates.update { states ->
                        states + (conversationId to AnalysisUiState(errorMessage = throwable.message ?: "계획을 만들지 못했습니다."))
                    }
                }
        }
    }

    private fun resolveDisplayName(contentResolver: ContentResolver, uri: Uri): String {
        val fallbackName = uri.lastPathSegment ?: "kakaotalk-export.txt"
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            cursor.readDisplayName(fallbackName)
        } ?: fallbackName
    }

    private fun Cursor.readDisplayName(fallbackName: String): String {
        val index = getColumnIndex(OpenableColumns.DISPLAY_NAME)
        return if (index >= 0 && moveToFirst()) getString(index) else fallbackName
    }

    private suspend fun copyModelToAppStorage(uri: Uri, sourceName: String): File = withContext(Dispatchers.IO) {
        val modelsDir = File(getApplication<Application>().filesDir, "models").apply { mkdirs() }
        val safeName = sourceName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val destination = File(modelsDir, safeName)
        val tempDestination = File(modelsDir, "$safeName.tmp")

        if (tempDestination.exists()) {
            tempDestination.delete()
        }
        contentResolver.openInputStream(uri)?.use { input ->
            tempDestination.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: error("선택한 모델 파일을 읽지 못했습니다.")

        if (destination.exists()) {
            destination.delete()
        }
        check(tempDestination.renameTo(destination)) {
            "모델 파일을 이 기기에 저장하지 못했습니다."
        }
        destination
    }
}

data class ImportUiState(
    val isLoading: Boolean = false,
    val importedConversationId: Long? = null,
    val message: String? = null,
    val errorMessage: String? = null,
)

data class AnalysisUiState(
    val isRunning: Boolean = false,
    val providerType: String? = null,
    val errorMessage: String? = null,
)

data class ModelSettingsUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
)
