package com.personal.freelancingdocument.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents WHERE accountEmail = :accountEmail AND deletedLocally = 0 ORDER BY lastUpdated DESC")
    fun observeDocuments(accountEmail: String): Flow<List<DocumentEntity>>

    @Query("""
        SELECT * FROM documents 
        WHERE accountEmail = :accountEmail AND deletedLocally = 0 
        AND subject LIKE '%' || :query || '%' 
        ORDER BY lastUpdated DESC
    """)
    fun searchDocuments(accountEmail: String, query: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocument(id: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE synced = 0")
    suspend fun getUnsyncedDocuments(): List<DocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(documents: List<DocumentEntity>)

    @Query("UPDATE documents SET deletedLocally = 1 WHERE id = :id")
    suspend fun markDeleted(id: String)

    @Delete
    suspend fun delete(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun hardDelete(id: String)

    // ---- Media ----

    @Query("SELECT * FROM media_items WHERE documentId = :documentId")
    fun observeMedia(documentId: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE documentId = :documentId")
    suspend fun getMedia(documentId: String): List<MediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMedia(media: MediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMediaAll(media: List<MediaEntity>)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMedia(id: String)

    @Query("SELECT * FROM media_items WHERE synced = 0")
    suspend fun getUnsyncedMedia(): List<MediaEntity>
}
