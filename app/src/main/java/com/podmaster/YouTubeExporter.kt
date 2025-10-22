package com.podmaster

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.Video
import com.google.api.services.youtube.model.VideoSnippet
import java.io.File

class YouTubeExporter(private val credential: GoogleAccountCredential) {
    
    private val youtube = YouTube.Builder(
        NetHttpTransport(),
        GsonFactory.getDefaultInstance(),
        credential
    ).setApplicationName("PodMaster").build()
    
    fun uploadPodcast(
        audioFile: File,
        title: String,
        description: String,
        onProgress: (Int) -> Unit,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val video = Video()
            val snippet = VideoSnippet()
            snippet.title = title
            snippet.description = description
            video.snippet = snippet
            
            val mediaContent = InputStreamContent(
                "audio/mpeg",
                audioFile.inputStream()
            )
            
            val insert = youtube.videos().insert(listOf("snippet", "status"), video, mediaContent)
            
            insert.mediaHttpUploader.apply {
                isDirectUploadEnabled = false
                chunkSize = 1024 * 1024 // 1MB chunks
                setProgressListener { uploader ->
                    val progress = (uploader.progress * 100).toInt()
                    onProgress(progress)
                }
            }
            
            val response = insert.execute()
            onSuccess(response.id)
            
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}