package com.personal.freelancingdocument.data.repository

import android.content.Context
import android.net.Uri
import com.personal.freelancingdocument.data.drive.DriveServiceHelper
import com.personal.freelancingdocument.data.drive.FolderIds
import com.personal.freelancingdocument.data.local.AppDatabase
import com.personal.freelancingdocument.data.local.DocumentEntity
import com.personal.freelancingdocument.data.local.MediaEntity
import com.personal.freelancingdocument.data.model.Document
import com.personal.freelancingdocument.data.model.MediaItem
import com.personal.freelancingdocument.data.model.MediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.util.UUID

/**
 * Single source of truth for documents. UI reads from Room (fast, works offline).
 * Writes go to Room immediately, then are queued for Drive sync (either immediately
 * if online, or later by [com.personal.freelancingdocument.sync.SyncWorker]).
 */
class DocumentRepository(
    private val context: Context,
    private val accountEmail: String,
    private val drive: DriveServiceHelper?
) {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.documentDao()

    fun observeDocuments(): Flow<List<Document>> =
        dao.observeDocuments(accountEmail).map { list -> list.map { it.toDomain() } }

    fun searchDocuments(query: String): Flow<List<Document>> =
        dao.searchDocuments(accountEmail, query).map { list -> list.map { it.toDomain() } }

    fun observeMedia(documentId: String): Flow<List<MediaItem>> =
        dao.observeMedia(documentId).map { list -> list.map { it.toDomain() } }

    suspend fun getDocument(id: String): Document? {
        val entity = dao.getDocument(id) ?: return null
        val media = dao.getMedia(id).map { it.toDomain() }
        return entity.toDomain().copy(
            photoUris = media.filter { it.type == MediaType.PHOTO },
            videoUris = media.filter { it.type == MediaType.VIDEO }
        )
    }

    /** Creates a new document locally (instantly available) and syncs to Drive in the background. */
    suspend fun createDocument(subject: String, description: String): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        dao.upsert(
            DocumentEntity(
                id = id,
                accountEmail = accountEmail,
                subject = subject,
                description = description,
                createdDate = now,
                lastUpdated = now,
                driveFileId = null,
                synced = false
            )
        )
        return id
    }

    suspend fun updateDocument(id: String, subject: String, description: String) {
        val existing = dao.getDocument(id) ?: return
        dao.upsert(
            existing.copy(
                subject = subject,
                description = description,
                lastUpdated = System.currentTimeMillis(),
                synced = false
            )
        )
    }

    suspend fun deleteDocument(document: Document) {
        // Soft-delete locally for instant UI feedback; hard-delete (+ Drive cleanup) happens in sync.
        dao.markDeleted(document.id)
    }

    suspend fun addMedia(documentId: String, localUri: Uri, fileName: String, type: MediaType) {
        dao.upsertMedia(
            MediaEntity(
                id = UUID.randomUUID().toString(),
                documentId = documentId,
                localUri = localUri.toString(),
                driveFileId = null,
                driveDownloadUrl = null,
                fileName = fileName,
                type = type.name,
                synced = false
            )
        )
        touch(documentId)
    }

    suspend fun deleteMedia(mediaId: String, documentId: String, driveFileId: String?) {
        drive?.let { d -> driveFileId?.let { runCatching { d.deleteFile(it) } } }
        dao.deleteMedia(mediaId)
        touch(documentId)
    }

    private suspend fun touch(documentId: String) {
        val doc = dao.getDocument(documentId) ?: return
        dao.upsert(doc.copy(lastUpdated = System.currentTimeMillis(), synced = false))
    }

    // ---------- Drive sync ----------

    /** Pushes every unsynced local change to Drive. Call from SyncWorker or "Sync now". */
    suspend fun pushPendingChanges(folders: FolderIds) {
        val service = drive ?: return
        val unsyncedDocs = dao.getUnsyncedDocuments()
        for (doc in unsyncedDocs) {
            val json = documentToJson(doc)
            val fileId = service.uploadJson(
                fileName = "${doc.id}.json",
                json = json,
                parentId = folders.documents,
                existingFileId = doc.driveFileId
            )
            dao.upsert(doc.copy(driveFileId = fileId, synced = true))
        }

        val unsyncedMedia = dao.getUnsyncedMedia()
        for (media in unsyncedMedia) {
            val uri = media.localUri?.let { Uri.parse(it) } ?: continue
            val stream = context.contentResolver.openInputStream(uri) ?: continue
            val parent = if (media.type == "PHOTO") folders.photos else folders.videos
            val mime = if (media.type == "PHOTO") "image/*" else "video/*"
            val fileId = stream.use { service.uploadMedia(media.fileName, mime, it, parent) }
            dao.upsertMedia(media.copy(driveFileId = fileId, synced = true))
        }
    }

    /** Pulls all documents + media metadata from Drive on first login / new device. */
    suspend fun restoreFromDrive(folders: FolderIds) {
        val service = drive ?: return
        val files = service.listDocumentFiles(folders.documents)
        val entities = mutableListOf<DocumentEntity>()
        for (file in files) {
            val bytes = runCatching { service.downloadFile(file.id) }.getOrNull() ?: continue
            val json = JSONObject(String(bytes))
            entities.add(jsonToDocument(json, file.id))
        }
        dao.upsertAll(entities)
    }

    private fun documentToJson(doc: DocumentEntity): String {
        val obj = JSONObject()
        obj.put("id", doc.id)
        obj.put("subject", doc.subject)
        obj.put("description", doc.description)
        obj.put("createdDate", doc.createdDate)
        obj.put("lastUpdated", doc.lastUpdated)
        return obj.toString()
    }

    private fun jsonToDocument(obj: JSONObject, driveFileId: String): DocumentEntity {
        return DocumentEntity(
            id = obj.optString("id", UUID.randomUUID().toString()),
            accountEmail = accountEmail,
            subject = obj.optString("subject"),
            description = obj.optString("description"),
            createdDate = obj.optLong("createdDate", System.currentTimeMillis()),
            lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis()),
            driveFileId = driveFileId,
            synced = true
        )
    }
}

private fun DocumentEntity.toDomain() = Document(
    id = id,
    subject = subject,
    description = description,
    createdDate = createdDate,
    lastUpdated = lastUpdated,
    driveFileId = driveFileId,
    synced = synced
)

private fun MediaEntity.toDomain() = MediaItem(
    id = id,
    localUri = localUri,
    driveFileId = driveFileId,
    driveDownloadUrl = driveDownloadUrl,
    fileName = fileName,
    type = if (type == "PHOTO") MediaType.PHOTO else MediaType.VIDEO
)
