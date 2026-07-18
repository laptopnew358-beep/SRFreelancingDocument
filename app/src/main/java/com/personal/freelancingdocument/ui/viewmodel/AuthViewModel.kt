package com.personal.freelancingdocument.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.personal.freelancingdocument.auth.GoogleAuthManager
import com.personal.freelancingdocument.data.drive.DriveServiceHelper
import com.personal.freelancingdocument.data.drive.FolderIds
import com.personal.freelancingdocument.sync.SyncWorker
import com.personal.freelancingdocument.util.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    data object Loading : AuthState()
    data object SignedOut : AuthState()
    data class SigningIn(val message: String) : AuthState()
    data class SignedIn(val email: String, val displayName: String?, val folders: FolderIds) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = GoogleAuthManager(application)
    private val prefs = PreferencesManager(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        val existing = authManager.getLastSignedInAccount()
        if (existing != null) {
            completeSignIn(existing)
        } else {
            _authState.value = AuthState.SignedOut
        }
    }

    fun getSignInIntent(): Intent = authManager.getSignInIntent()

    fun onSignInResult(data: Intent?) {
        _authState.value = AuthState.SigningIn("Signing in…")
        authManager.handleSignInResult(data).fold(
            onSuccess = { account -> completeSignIn(account) },
            onFailure = { e -> _authState.value = AuthState.Error(e.message ?: "Sign-in failed") }
        )
    }

    private fun completeSignIn(account: GoogleSignInAccount) {
        val email = account.email ?: run {
            _authState.value = AuthState.Error("Google account has no email"); return
        }
        _authState.value = AuthState.SigningIn("Setting up your Drive folder…")
        viewModelScope.launch {
            prefs.setAccount(email, account.displayName)
            try {
                val credential = authManager.getDriveCredential(account)
                val driveService = DriveServiceHelper(getApplication(), credential)
                val folders = driveService.ensureFolderStructure()
                prefs.setDriveRootFolderId(folders.root)
                _authState.value = AuthState.SignedIn(email, account.displayName, folders)
                SyncWorker.triggerOneTime(getApplication())
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Could not set up Drive folder: ${e.message}")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut {
                viewModelScope.launch {
                    prefs.clearAll()
                    _authState.value = AuthState.SignedOut
                }
            }
        }
    }
}
