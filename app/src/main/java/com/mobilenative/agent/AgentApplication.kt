package com.mobilenative.agent

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class AgentApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize Timber logging for diagnostics
        Timber.plant(Timber.DebugTree())
        Timber.i("Mobile-Native Agent Application Initialized")
    }
}
