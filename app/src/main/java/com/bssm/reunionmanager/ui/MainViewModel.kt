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
import com.bssm.reunionmanager.domain.model.ImportConversationResult
import com.bssm.reunionmanager.domain.model.ProviderSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    fun observeConversationDetail(conversationId: Long): Flow<ConversationDetail?> {
        return appContainer.conversationRepository.observeConversationDetail(conversationId)
    }

    fun importConversation(uri: Uri) {
        viewModelScope.launch {
            _importState.value = ImportUiState(isLoading = true)

            runCatching {
                val sourceName = resolveDisplayName(contentResolver, uri)
                val rawText = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("The selected file could not be read.")
                appContainer.importConversationUseCase(sourceName = sourceName, rawText = rawText)
            }.onSuccess { result ->
                _importState.value = when (result) {
                    is ImportConversationResult.Imported -> ImportUiState(
                        importedConversationId = result.conversationId,
                        message = "Chat imported and stored on this device.",
                    )

                    is ImportConversationResult.Duplicate -> ImportUiState(
                        importedConversationId = result.conversationId,
                        message = "This chat was already imported earlier, so the existing local copy was reused.",
                    )
                }
            }.onFailure { throwable ->
                _importState.value = ImportUiState(errorMessage = throwable.message ?: "Import failed.")
            }
        }
    }

    fun clearImportMessage() {
        _importState.value = ImportUiState()
    }

    fun saveProviderSettings(apiKey: String, modelName: String, endpoint: String) {
        viewModelScope.launch {
            appContainer.providerSettingsRepository.save(
                ProviderSettings(
                    apiKey = apiKey.trim(),
                    modelName = modelName.trim().ifBlank { ProviderSettings.DEFAULT_MODEL },
                    endpoint = endpoint.trim().ifBlank { ProviderSettings.DEFAULT_ENDPOINT },
                ),
            )
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
                        states + (conversationId to AnalysisUiState(errorMessage = throwable.message ?: "Analysis failed."))
                    }
                }
        }
    }

    private fun resolveDisplayName(contentResolver: ContentResolver, uri: Uri): String {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            cursor.readDisplayName()
        } ?: uri.lastPathSegment ?: "kakaotalk-export.txt"
    }

    private fun Cursor.readDisplayName(): String {
        val index = getColumnIndex(OpenableColumns.DISPLAY_NAME)
        return if (index >= 0 && moveToFirst()) getString(index) else "kakaotalk-export.txt"
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
