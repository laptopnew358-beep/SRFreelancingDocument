package com.personal.freelancingdocument

import android.app.Application
import com.personal.freelancingdocument.sync.SyncWorker

class FreelancingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SyncWorker.schedulePeriodic(this)
    }
}
