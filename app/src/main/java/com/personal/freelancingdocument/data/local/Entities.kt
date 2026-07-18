package com.personal.freelancingdocument.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local cache of a document. Mirrors [com.personal.freelancingdocument.data.model.Document].
 * The Google account email is stored so that switching accounts on the same device
 * never shows another account's cached data.
 */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val accountEmail: String,
    val subject: String,
    val description: String,
    val createdDate: Long,
    val lastUpdated: Long,
    val driveFileId: String?,
    val synced: Boolean,
    val deletedLocally: Boolean = false
)

@Entity(
    tableName = "media_items",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId")]
)
data class MediaEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val localUri: String?,
    val driveFileId: String?,
    val driveDownloadUrl: String?,
    val fileName: String,
    val type: String, // "PHOTO" or "VIDEO"
    val synced: Boolean = false
)
