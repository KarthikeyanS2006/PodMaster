package com.podmaster.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class FirebaseManager {
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    suspend fun uploadAudio(file: File, filename: String): String = suspendCancellableCoroutine { continuation ->
        val audioRef = storageRef.child("audio/$filename")
        val uploadTask = audioRef.putFile(android.net.Uri.fromFile(file))

        uploadTask
            .addOnSuccessListener { taskSnapshot ->
                audioRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        continuation.resume(uri.toString())
                    }
                    .addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }

    suspend fun downloadAudio(url: String, outputFile: File): Boolean = suspendCancellableCoroutine { continuation ->
        storageRef.child(url).getFile(outputFile)
            .addOnSuccessListener {
                continuation.resume(true)
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }
}