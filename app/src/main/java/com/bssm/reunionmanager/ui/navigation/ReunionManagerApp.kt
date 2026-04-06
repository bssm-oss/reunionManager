package com.bssm.reunionmanager.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bssm.reunionmanager.ui.MainViewModel
import com.bssm.reunionmanager.ui.screen.analysis.AnalysisScreen
import com.bssm.reunionmanager.ui.screen.conversation.ConversationDetailScreen
import com.bssm.reunionmanager.ui.screen.conversation.ConversationListScreen
import com.bssm.reunionmanager.ui.screen.home.HomeScreen
import com.bssm.reunionmanager.ui.screen.importing.ImportScreen
import com.bssm.reunionmanager.ui.screen.settings.SettingsScreen
import com.bssm.reunionmanager.ui.theme.ReunionBadge
import com.bssm.reunionmanager.ui.theme.ReunionBadgeTone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReunionManagerApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val providerSettings by viewModel.providerSettings.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val analysisStates by viewModel.analysisStates.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTitle = when {
        currentRoute == ReunionDestination.Import.route -> ReunionDestination.Import.title
        currentRoute == ReunionDestination.Conversations.route -> ReunionDestination.Conversations.title
        currentRoute == ReunionDestination.Settings.route -> ReunionDestination.Settings.title
        currentRoute == ReunionDestination.ConversationDetail.route -> ReunionDestination.ConversationDetail.title
        currentRoute == ReunionDestination.Analysis.route -> ReunionDestination.Analysis.title
        else -> ReunionDestination.Home.title
    }
    val canGoBack = backStackEntry?.destination?.hierarchy?.any { it.route == ReunionDestination.Home.route } == false

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (currentRoute != ReunionDestination.Home.route) {
                                Text(
                                    text = ReunionDestination.Home.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = currentTitle,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = {
                        if (canGoBack) {
                            Surface(
                                modifier = Modifier.padding(start = 12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                IconButton(
                                    onClick = { navController.popBackStack() },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Text(text = "←", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    },
                    actions = {
                        ReunionBadge(
                            text = if (providerSettings.isConfigured) "AI configured" else "Local provider",
                            tone = if (providerSettings.isConfigured) ReunionBadgeTone.Accent else ReunionBadgeTone.Neutral,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ReunionDestination.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(route = ReunionDestination.Home.route) {
                HomeScreen(
                    conversationCount = conversations.size,
                    providerConfigured = providerSettings.isConfigured,
                    onImportClick = { navController.navigate(ReunionDestination.Import.route) },
                    onConversationsClick = { navController.navigate(ReunionDestination.Conversations.route) },
                    onSettingsClick = { navController.navigate(ReunionDestination.Settings.route) },
                )
            }
            composable(route = ReunionDestination.Import.route) {
                ImportScreen(
                    importState = importState,
                    onImportClick = viewModel::importConversation,
                    onViewConversationClick = { conversationId ->
                        viewModel.clearImportMessage()
                        navController.navigate(ReunionDestination.ConversationDetail.createRoute(conversationId))
                    },
                )
            }
            composable(route = ReunionDestination.Conversations.route) {
                ConversationListScreen(
                    conversations = conversations,
                    onConversationClick = { conversationId ->
                        navController.navigate(ReunionDestination.ConversationDetail.createRoute(conversationId))
                    },
                )
            }
            composable(route = ReunionDestination.Settings.route) {
                SettingsScreen(
                    providerSettings = providerSettings,
                    onSave = viewModel::saveProviderSettings,
                )
            }
            composable(
                route = ReunionDestination.ConversationDetail.route,
                arguments = listOf(navArgument("conversationId") { type = NavType.LongType }),
            ) { entry ->
                val conversationId = entry.arguments?.getLong("conversationId") ?: return@composable
                val detail by viewModel.observeConversationDetail(conversationId).collectAsStateWithLifecycle(initialValue = null)
                ConversationDetailScreen(
                    detail = detail,
                    onOpenAnalysis = {
                        navController.navigate(ReunionDestination.Analysis.createRoute(conversationId))
                    },
                )
            }
            composable(
                route = ReunionDestination.Analysis.route,
                arguments = listOf(navArgument("conversationId") { type = NavType.LongType }),
            ) { entry ->
                val conversationId = entry.arguments?.getLong("conversationId") ?: return@composable
                val detail by viewModel.observeConversationDetail(conversationId).collectAsStateWithLifecycle(initialValue = null)
                val analysisState = analysisStates[conversationId]
                AnalysisScreen(
                    detail = detail,
                    analysisState = analysisState,
                    onGenerate = { viewModel.generateAnalysis(conversationId) },
                )
            }
        }
    }
}
