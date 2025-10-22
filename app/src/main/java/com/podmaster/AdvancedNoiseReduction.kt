package com.podmaster

import android.content.Context
import ai.picovoice.koala.Koala
import ai.picovoice.koala.KoalaException

// Add to build.gradle.kts
dependencies {
    implementation("ai.picovoice:koala-android:2.0.2")
}

class AdvancedNoiseReduction(
    private val context: Context,
    private val accessKey: String
) {
    private var koala: Koala? = null
    
    fun initialize(): Boolean {
        return try {
            koala = Koala.Builder()
                .setAccessKey(accessKey)
                .build(context)
            true
        } catch (e: KoalaException) {
            e.printStackTrace()
            false
        }
    }
    
    fun processAudio(inputBuffer: ShortArray): ShortArray {
        return koala?.process(inputBuffer) ?: inputBuffer
    }
    
    fun release() {
        koala?.delete()
        koala = null
    }
}
