package com.personal.freelancingdocument.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.personal.freelancingdocument.auth.GoogleAuthManager
import com.personal.freelancingdocument.data.drive.DriveServiceHelper
import com.personal.freelancingdocument.data.drive.FolderIds
import com.personal.freelancingdocument.data.model.Document
import com.personal.freelancingdocument.data.model.MediaItem
import com.personal.freelancingdocument.data.model.MediaType
import com.personal.freelancingdocument.data.repository.DocumentRepository
import com.personal.freelancingdocument.sync.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DocumentViewModel(application: Application) : AndroidViewModel(application) {

    private var repository: DocumentRepository? = null
    private var folders: FolderIds? = null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _documents = MutableStateFlow<List<Document>>(emptyList())
    val documents: StateFlow<List<Document>> = _documents.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /** Must be called once after sign-in completes (see AuthState.SignedIn). */
    fun initialize(accountEmail: String, folders: FolderIds) {
        this.folders = folders
        val account = GoogleSignIn.getLastSignedInAccount(getApplication())
        val driveService = account?.let {
            DriveServiceHelper(getApplication(), GoogleAuthManager(getApplication()).getDriveCredential(it))
        }
        repository = DocumentRepository(getApplication(), accountEmail, driveService)

        viewModelScope.launch {
            _isSyncing.value = true
            runCatching { repository?.restoreFromDrive(folders) }
            _isSyncing.value = false
        }

        viewModelScope.launch {
            _searchQuery.flatMapLatest { q ->
                val repo = repository!!
                if (q.isBlank()) repo.observeDocuments() else repo.searchDocuments(q)
            }.collect { _documents.value = it }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createDocument(subject: String, description: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val id = repository?.createDocument(subject, description) ?: return@launch
            syncNow()
            onCreated(id)
        }
    }

    fun updateDocument(id: String, subject: String, description: String) {
        viewModelScope.launch {
            repository?.updateDocument(id, subject, description)
            syncNow()
        }
    }

    fun deleteDocument(document: Document) {
        viewModelScope.launch {
            repository?.deleteDocument(document)
            syncNow()
        }
    }

    fun addMedia(documentId: String, uri: Uri, fileName: String, type: MediaType) {
        viewModelScope.launch {
            repository?.addMedia(documentId, uri, fileName, type)
            syncNow()
        }
    }

    fun deleteMedia(media: MediaItem, documentId: String) {
        viewModelScope.launch {
            repository?.deleteMedia(media.id, documentId, media.driveFileId)
        }
    }

    suspend fun getDocument(id: String): Document? = repository?.getDocument(id)

    fun observeMedia(documentId: String) = repository?.observeMedia(documentId)

    fun syncNow() {
        SyncWorker.triggerOneTime(getApplication())
    }
}
