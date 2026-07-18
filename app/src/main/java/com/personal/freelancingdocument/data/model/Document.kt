package com.personal.freelancingdocument.data.model

/**
 * A single freelancing document as shown in the UI.
 * Backed by [com.personal.freelancingdocument.data.local.DocumentEntity] for local cache
 * and mirrored as a JSON file + media files inside the user's Google Drive
 * "Freelancing Document" folder.
 */
data class Document(
    val id: String,
    val subject: String,
    val description: String,
    val photoUris: List<MediaItem> = emptyList(),
    val videoUris: List<MediaItem> = emptyList(),
    val createdDate: Long,
    val lastUpdated: Long,
    val driveFileId: String? = null,
    val synced: Boolean = false
)

/**
 * A single media attachment (photo or video).
 * [localUri] points at the cached copy on device (content:// or file:// or cache path).
 * [driveFileId] is set once the file has been uploaded to Drive.
 */
data class MediaItem(
    val id: String,
    val localUri: String?,
    val driveFileId: String?,
    val driveDownloadUrl: String? = null,
    val fileName: String,
    val type: MediaType
)

enum class MediaType { PHOTO, VIDEO }
