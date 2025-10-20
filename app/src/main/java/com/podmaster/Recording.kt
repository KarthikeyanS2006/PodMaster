// File: Recording.kt
package com.podmaster.models

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class Recording(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val size: Long = file.length(),
    val duration: Long = 0, // in milliseconds
    val dateModified: Long = file.lastModified()
) {
    fun getSizeInMB(): String {
        val sizeInMB = size / (1024.0 * 1024.0)
        return String.format("%.2f MB", sizeInMB)
    }

    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(dateModified))
    }

    fun getDurationFormatted(): String {
        if (duration == 0L) return "Unknown"
        val seconds = (duration / 1000) % 60
        val minutes = (duration / (1000 * 60)) % 60
        val hours = (duration / (1000 * 60 * 60))

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}
