package com.personal.freelancingdocument.sync

import android.content.Context
import androidx.work.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.personal.freelancingdocument.auth.GoogleAuthManager
import com.personal.freelancingdocument.data.drive.DriveServiceHelper
import com.personal.freelancingdocument.data.repository.DocumentRepository
import java.util.concurrent.TimeUnit

/**
 * Pushes locally-queued changes to Google Drive whenever the network is available.
 * Scheduled both as periodic background work and triggered manually via "Sync Now".
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext) ?: return Result.success()
        return try {
            val authManager = GoogleAuthManager(applicationContext)
            val credential = authManager.getDriveCredential(account)
            val driveService = DriveServiceHelper(applicationContext, credential)
            val folders = driveService.ensureFolderStructure()

            val repository = DocumentRepository(applicationContext, account.email ?: "", driveService)
            repository.pushPendingChanges(folders)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "freelancing_document_periodic_sync"
        private const val ONE_TIME_WORK_NAME = "freelancing_document_manual_sync"

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        /** Triggered by the "Sync Now" button, or right after saving a change while online. */
        fun triggerOneTime(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME, ExistingWorkPolicy.REPLACE, request
            )
        }
    }
}
