package com.podmaster

import android.app.Application
import com.google.firebase.FirebaseApp

class PodMasterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}