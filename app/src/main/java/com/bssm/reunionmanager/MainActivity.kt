package com.bssm.reunionmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bssm.reunionmanager.ui.navigation.ReunionManagerApp
import com.bssm.reunionmanager.ui.theme.ReunionManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReunionManagerTheme {
                ReunionManagerApp()
            }
        }
    }
}
