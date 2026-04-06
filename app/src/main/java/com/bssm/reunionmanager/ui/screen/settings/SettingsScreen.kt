package com.bssm.reunionmanager.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.bssm.reunionmanager.domain.model.ProviderSettings
import com.bssm.reunionmanager.ui.theme.ReunionBadge
import com.bssm.reunionmanager.ui.theme.ReunionBadgeTone
import com.bssm.reunionmanager.ui.theme.ReunionPane
import com.bssm.reunionmanager.ui.theme.ReunionPrimaryButton
import com.bssm.reunionmanager.ui.theme.ScreenPadding
import com.bssm.reunionmanager.ui.theme.ScreenSectionSpacing
import com.bssm.reunionmanager.ui.theme.reunionOutlinedTextFieldColors

@Composable
fun SettingsScreen(
    providerSettings: ProviderSettings,
    onSave: (String, String, String) -> Unit,
) {
    var apiKey by rememberSaveable { mutableStateOf("") }
    var modelName by rememberSaveable { mutableStateOf(ProviderSettings.DEFAULT_MODEL) }
    var endpoint by rememberSaveable { mutableStateOf(ProviderSettings.DEFAULT_ENDPOINT) }

    LaunchedEffect(providerSettings) {
        apiKey = providerSettings.apiKey
        modelName = providerSettings.modelName
        endpoint = providerSettings.endpoint
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(ScreenSectionSpacing),
    ) {
        Text(text = "Save provider settings locally.", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Leave the API key empty to keep using the fake local provider for the MVP flow.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ReunionPane(
            title = "Provider status",
            supportingText = if (providerSettings.isConfigured) {
                "Gemini-compatible analysis is configured locally on this device."
            } else {
                "The app will continue using the fake local provider until you save an API key."
            },
        ) {
            ReunionBadge(
                text = if (providerSettings.isConfigured) "AI configured" else "Fake local provider",
                tone = if (providerSettings.isConfigured) ReunionBadgeTone.Accent else ReunionBadgeTone.Neutral,
            )
        }
        ReunionPane(
            title = "Local-only configuration",
            supportingText = "These settings are stored only on this device. Leaving the API key empty keeps the app on the fake local provider. Saving a Gemini endpoint does not send chat data by itself; chat excerpts are only sent when you generate a reunion plan.",
        ) {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Gemini API key") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = reunionOutlinedTextFieldColors(),
            )
            OutlinedTextField(
                value = modelName,
                onValueChange = { modelName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Model name") },
                singleLine = true,
                colors = reunionOutlinedTextFieldColors(),
            )
            OutlinedTextField(
                value = endpoint,
                onValueChange = { endpoint = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Endpoint") },
                singleLine = true,
                colors = reunionOutlinedTextFieldColors(),
            )
        }
        ReunionPrimaryButton(text = "Save local settings", onClick = { onSave(apiKey, modelName, endpoint) })
    }
}
