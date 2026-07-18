package com.personal.freelancingdocument.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.personal.freelancingdocument.data.model.Document
import com.personal.freelancingdocument.data.model.MediaType
import com.personal.freelancingdocument.ui.screens.*
import com.personal.freelancingdocument.ui.viewmodel.AuthState
import com.personal.freelancingdocument.ui.viewmodel.AuthViewModel
import com.personal.freelancingdocument.ui.viewmodel.DocumentViewModel
import com.personal.freelancingdocument.util.PreferencesManager
import kotlinx.coroutines.launch

private object Routes {
    const val LOGIN = "login"
    const val LIST = "list"
    const val NEW_DOCUMENT = "new_document"
    const val EDIT_DOCUMENT = "edit_document/{docId}"
    const val SETTINGS = "settings"
    fun editDocument(id: String) = "edit_document/$id"
}

@Composable
fun AppNavGraph(
    authViewModel: AuthViewModel,
    documentViewModel: DocumentViewModel,
    prefs: PreferencesManager
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> authViewModel.onSignInResult(result.data) }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.SignedIn -> {
                documentViewModel.initialize(state.email, state.folders)
                if (navController.currentDestination?.route == Routes.LOGIN || navController.currentDestination == null) {
                    navController.navigate(Routes.LIST) {
                        popUpTo(0)
                    }
                }
            }
            is AuthState.SignedOut -> {
                navController.navigate(Routes.LOGIN) { popUpTo(0) }
            }
            else -> Unit
        }
    }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {

        composable(Routes.LOGIN) {
            val state = authState
            LoginScreen(
                isSigningIn = state is AuthState.SigningIn || state is AuthState.Loading,
                statusMessage = (state as? AuthState.SigningIn)?.message,
                errorMessage = (state as? AuthState.Error)?.message,
                onSignInClick = { signInLauncher.launch(authViewModel.getSignInIntent()) }
            )
        }

        composable(Routes.LIST) {
            val documents by documentViewModel.documents.collectAsState()
            val query by documentViewModel.searchQuery.collectAsState()
            val syncing by documentViewModel.isSyncing.collectAsState()

            DocumentListScreen(
                documents = documents,
                searchQuery = query,
                isSyncing = syncing,
                onSearchQueryChange = documentViewModel::setSearchQuery,
                onDocumentClick = { navController.navigate(Routes.editDocument(it)) },
                onAddClick = { navController.navigate(Routes.NEW_DOCUMENT) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.NEW_DOCUMENT) {
            DocumentEditScreen(
                isNew = true,
                initialSubject = "",
                initialDescription = "",
                photos = emptyList(),
                videos = emptyList(),
                onSave = { subject, description ->
                    documentViewModel.createDocument(subject, description) {}
                },
                onAddPhoto = {},
                onAddVideo = {},
                onDeleteMedia = {},
                onDeleteDocument = null,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.EDIT_DOCUMENT,
            arguments = listOf(navArgument("docId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("docId") ?: ""

            var document by remember { mutableStateOf<Document?>(null) }
            val mediaFlow = remember(id) { documentViewModel.observeMedia(id) }
            val media by (mediaFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })

            LaunchedEffect(id) {
                document = documentViewModel.getDocument(id)
            }

            document?.let { doc ->
                DocumentEditScreen(
                    isNew = false,
                    initialSubject = doc.subject,
                    initialDescription = doc.description,
                    photos = media.filter { it.type == MediaType.PHOTO },
                    videos = media.filter { it.type == MediaType.VIDEO },
                    onSave = { subject, description ->
                        documentViewModel.updateDocument(id, subject, description)
                    },
                    onAddPhoto = { uri -> documentViewModel.addMedia(id, uri, "photo_${System.currentTimeMillis()}.jpg", MediaType.PHOTO) },
                    onAddVideo = { uri -> documentViewModel.addMedia(id, uri, "video_${System.currentTimeMillis()}.mp4", MediaType.VIDEO) },
                    onDeleteMedia = { item -> documentViewModel.deleteMedia(item, id) },
                    onDeleteDocument = { documentViewModel.deleteDocument(doc) },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.SETTINGS) {
            val theme by prefs.themeOption.collectAsState(initial = com.personal.freelancingdocument.util.ThemeOption.SYSTEM)
            val email by prefs.accountEmail.collectAsState(initial = null)
            val scope = rememberCoroutineScope()

            SettingsScreen(
                accountEmail = email,
                currentTheme = theme,
                onThemeChange = { option -> scope.launch { prefs.setThemeOption(option) } },
                onSyncNow = { documentViewModel.syncNow() },
                onLogout = {
                    authViewModel.signOut()
                    navController.navigate(Routes.LOGIN) { popUpTo(0) }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
