package com.podmaster

import android.content.Context
import android.media.AudioRecord
import android.media.audiofx.NoiseSuppressor

class NoiseReductionManager(private val context: Context) {
    
    enum class NoiseReductionLevel {
        OFF,      // No noise reduction
        MINIMAL,  // Light filtering
        MODERATE, // Balanced filtering
        AGGRESSIVE // Maximum noise suppression
    }
    
    private var noiseSuppressor: NoiseSuppressor? = null
    private var currentLevel: NoiseReductionLevel = NoiseReductionLevel.MODERATE
    
    fun initialize(audioRecord: AudioRecord): Boolean {
        return try {
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(audioRecord.audioSessionId)
                noiseSuppressor?.enabled = true
                applyNoiseReductionLevel(currentLevel)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun setNoiseReductionLevel(level: NoiseReductionLevel) {
        currentLevel = level
        applyNoiseReductionLevel(level)
    }
    
    private fun applyNoiseReductionLevel(level: NoiseReductionLevel) {
        noiseSuppressor?.let { suppressor ->
            when (level) {
                NoiseReductionLevel.OFF -> {
                    suppressor.enabled = false
                }
                NoiseReductionLevel.MINIMAL -> {
                    suppressor.enabled = true
                    // Apply minimal settings
                }
                NoiseReductionLevel.MODERATE -> {
                    suppressor.enabled = true
                    // Apply moderate settings
                }
                NoiseReductionLevel.AGGRESSIVE -> {
                    suppressor.enabled = true
                    // Apply aggressive settings
                }
            }
        }
    }
    
    fun release() {
        noiseSuppressor?.release()
        noiseSuppressor = null
    }
}
