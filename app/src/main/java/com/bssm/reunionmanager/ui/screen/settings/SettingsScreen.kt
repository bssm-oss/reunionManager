package com.bssm.reunionmanager.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import com.bssm.reunionmanager.domain.model.ProviderSettings

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
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "Local-only configuration", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "These settings are stored only on this device. Leaving the API key empty keeps the app on the fake local provider. Saving a Gemini endpoint does not send chat data by itself; chat excerpts are only sent when you generate a reunion plan.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Gemini API key") },
            visualTransformation = PasswordVisualTransformation(),
        )
        OutlinedTextField(
            value = modelName,
            onValueChange = { modelName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Model name") },
        )
        OutlinedTextField(
            value = endpoint,
            onValueChange = { endpoint = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Endpoint") },
        )

        Button(onClick = { onSave(apiKey, modelName, endpoint) }) {
            Text(text = "Save local settings")
        }
    }
}
