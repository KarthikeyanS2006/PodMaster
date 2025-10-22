package com.podmaster

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.io.File

class CloudStorageManager {
    private fun File.toUri(): Uri = Uri.fromFile(this)
    
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    
    fun uploadAudio(
        file: File,
        userId: String,
        onProgress: (Int) -> Unit,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val audioRef: StorageReference = storageRef
            .child("podcasts/$userId/${file.name}")
        
        val uploadTask: UploadTask = audioRef.putFile(file.toUri())
        
        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            onProgress(progress)
        }.addOnSuccessListener {
            audioRef.downloadUrl.addOnSuccessListener { uri ->
                onSuccess(uri.toString())
            }
        }.addOnFailureListener { exception ->
            onFailure(exception)
        }
    }
    
    fun downloadAudio(
        downloadUrl: String,
        destinationFile: File,
        onProgress: (Int) -> Unit,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val audioRef = storage.getReferenceFromUrl(downloadUrl)
        
        audioRef.getFile(destinationFile)
            .addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                onProgress(progress)
            }
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}