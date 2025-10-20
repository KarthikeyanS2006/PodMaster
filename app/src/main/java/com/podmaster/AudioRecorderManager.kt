// File: AudioRecorderManager.kt
package com.podmaster

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentFilePath: String? = null
    private var isRecording = false

    fun startRecording(): String? {
        try {
            // Create output file
            val outputDir = context.getExternalFilesDir(null)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val outputFile = File(outputDir, "podcast_$timestamp.m4a")
            currentFilePath = outputFile.absolutePath

            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000) // 128kbps
                setAudioSamplingRate(44100) // 44.1kHz
                setOutputFile(currentFilePath)

                prepare()
                start()
                isRecording = true
            }

            return currentFilePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun stopRecording(): String? {
        try {
            if (isRecording) {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
                return currentFilePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecording) {
            mediaRecorder?.pause()
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecording) {
            mediaRecorder?.resume()
        }
    }

    fun isRecording(): Boolean = isRecording

    fun getAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
