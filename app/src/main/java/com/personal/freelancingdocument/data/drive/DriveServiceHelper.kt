package com.personal.freelancingdocument.data.drive

import android.content.Context
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Thin wrapper around the Drive v3 REST client, scoped to the app-created folder tree:
 *
 * Freelancing Document/
 *   Documents/   (one JSON per document: subject, description, dates)
 *   Photos/      (uploaded photo files)
 *   Videos/      (uploaded video files)
 *   Backup/      (reserved for future full-export backups)
 *   Settings/    (small JSON blob for cross-device settings, optional)
 *
 * Uses the drive.file scope, so this app can only see/manage files it created itself —
 * never the rest of the user's Drive.
 */
class DriveServiceHelper(context: Context, credential: GoogleAccountCredential) {

    private val drive: Drive = Drive.Builder(
        AndroidHttp.newCompatibleTransport(),
        GsonFactory.getDefaultInstance(),
        credential
    ).setApplicationName("Freelancing Document").build()

    companion object {
        const val ROOT_FOLDER_NAME = "Freelancing Document"
        const val FOLDER_DOCUMENTS = "Documents"
        const val FOLDER_PHOTOS = "Photos"
        const val FOLDER_VIDEOS = "Videos"
        const val FOLDER_BACKUP = "Backup"
        const val FOLDER_SETTINGS = "Settings"
        private const val MIME_FOLDER = "application/vnd.google-apps.folder"
    }

    /** Finds the existing root folder or creates the full tree on first run. Idempotent. */
    suspend fun ensureFolderStructure(): FolderIds = withContext(Dispatchers.IO) {
        val root = findOrCreateFolder(ROOT_FOLDER_NAME, parentId = null)
        FolderIds(
            root = root,
            documents = findOrCreateFolder(FOLDER_DOCUMENTS, root),
            photos = findOrCreateFolder(FOLDER_PHOTOS, root),
            videos = findOrCreateFolder(FOLDER_VIDEOS, root),
            backup = findOrCreateFolder(FOLDER_BACKUP, root),
            settings = findOrCreateFolder(FOLDER_SETTINGS, root)
        )
    }

    private fun findOrCreateFolder(name: String, parentId: String?): String {
        val query = buildString {
            append("mimeType = '$MIME_FOLDER' and name = '$name' and trashed = false")
            if (parentId != null) append(" and '$parentId' in parents")
        }
        val result = drive.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        result.files.firstOrNull()?.let { return it.id }

        val metadata = DriveFile().apply {
            this.name = name
            mimeType = MIME_FOLDER
            if (parentId != null) parents = listOf(parentId)
        }
        return drive.files().create(metadata).setFields("id").execute().id
    }

    /** Uploads (or updates, if [existingFileId] is provided) a JSON document record. */
    suspend fun uploadJson(
        fileName: String,
        json: String,
        parentId: String,
        existingFileId: String? = null
    ): String = withContext(Dispatchers.IO) {
        val content = com.google.api.client.http.ByteArrayContent("application/json", json.toByteArray())
        if (existingFileId != null) {
            drive.files().update(existingFileId, DriveFile(), content).execute()
            existingFileId
        } else {
            val metadata = DriveFile().apply {
                name = fileName
                parents = listOf(parentId)
            }
            drive.files().create(metadata, content).setFields("id").execute().id
        }
    }

    /** Uploads a media file (photo/video) from an [InputStream]. */
    suspend fun uploadMedia(
        fileName: String,
        mimeType: String,
        inputStream: InputStream,
        parentId: String
    ): String = withContext(Dispatchers.IO) {
        val content = com.google.api.client.http.InputStreamContent(mimeType, inputStream)
        val metadata = DriveFile().apply {
            name = fileName
            parents = listOf(parentId)
        }
        drive.files().create(metadata, content).setFields("id").execute().id
    }

    suspend fun downloadFile(fileId: String): ByteArray = withContext(Dispatchers.IO) {
        val output = ByteArrayOutputStream()
        drive.files().get(fileId).executeMediaAndDownloadTo(output)
        output.toByteArray()
    }

    suspend fun deleteFile(fileId: String) = withContext(Dispatchers.IO) {
        drive.files().delete(fileId).execute()
    }

    /** Lists all document JSON files currently in the Documents folder (for full restore on new device). */
    suspend fun listDocumentFiles(documentsFolderId: String): List<DriveFile> = withContext(Dispatchers.IO) {
        drive.files().list()
            .setQ("'$documentsFolderId' in parents and trashed = false")
            .setSpaces("drive")
            .setFields("files(id, name, modifiedTime)")
            .execute().files
    }
}

data class FolderIds(
    val root: String,
    val documents: String,
    val photos: String,
    val videos: String,
    val backup: String,
    val settings: String
)
