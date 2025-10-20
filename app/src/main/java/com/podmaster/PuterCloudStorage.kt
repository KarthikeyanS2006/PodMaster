// File: app/src/main/java/com/podmaster/PuterCloudStorage.kt
package com.podmaster

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PuterCloudStorage {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * Upload file to Puter.js cloud storage (UNLIMITED FREE)
     * No API key needed - user authentication handled by Puter
     */
    suspend fun uploadFile(
        file: File,
        path: String = "podcasts/"
    ): Result<String> = suspendCancellableCoroutine { continuation ->

        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("audio/*".toMediaTypeOrNull())
                )
                .addFormDataPart("path", path)
                .build()

            val request = Request.Builder()
                .url("https://api.puter.com/upload")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val jsonResponse = JSONObject(response.body?.string() ?: "{}")
                        val fileUrl = jsonResponse.optString("url", "")
                        continuation.resume(Result.success(fileUrl))
                    } else {
                        continuation.resume(
                            Result.failure(Exception("Upload failed: ${response.code}"))
                        )
                    }
                }
            })

        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    /**
     * Download file from Puter cloud
     */
    suspend fun downloadFile(
        fileUrl: String,
        outputFile: File
    ): Result<File> = suspendCancellableCoroutine { continuation ->

        try {
            val request = Request.Builder()
                .url(fileUrl)
                .get()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        response.body?.byteStream()?.use { input ->
                            outputFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        continuation.resume(Result.success(outputFile))
                    } else {
                        continuation.resume(
                            Result.failure(Exception("Download failed: ${response.code}"))
                        )
                    }
                }
            })

        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}
