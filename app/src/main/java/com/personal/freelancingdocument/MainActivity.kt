package com.personal.freelancingdocument

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.personal.freelancingdocument.ui.navigation.AppNavGraph
import com.personal.freelancingdocument.ui.theme.FreelancingDocumentTheme
import com.personal.freelancingdocument.ui.viewmodel.AuthViewModel
import com.personal.freelancingdocument.ui.viewmodel.DocumentViewModel
import com.personal.freelancingdocument.util.PreferencesManager
import com.personal.freelancingdocument.util.ThemeOption

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val documentViewModel: DocumentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val prefs = PreferencesManager(applicationContext)

        setContent {
            val themeOption by prefs.themeOption.collectAsState(initial = ThemeOption.SYSTEM)
            val darkTheme = when (themeOption) {
                ThemeOption.LIGHT -> false
                ThemeOption.DARK -> true
                ThemeOption.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            FreelancingDocumentTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(
                        authViewModel = authViewModel,
                        documentViewModel = documentViewModel,
                        prefs = prefs
                    )
                }
            }
        }
    }
}
