// File: YouTubeUploader.kt
package com.podmaster.upload

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class YouTubeUploader(private val context: Context) {

    fun uploadToYouTube(audioFile: File, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "com.podmaster.provider",
            audioFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Podcast episode: $title")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.google.android.apps.youtube.creator")
        }

        try {
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            val chooser = Intent.createChooser(shareIntent, "Upload podcast to")
            context.startActivity(chooser)
        }
    }
}
