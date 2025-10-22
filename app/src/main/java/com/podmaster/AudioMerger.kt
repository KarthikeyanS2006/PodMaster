package com.podmaster

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class AudioMerger {

    fun mergeAudioFiles(
        introFile: File?,
        mainFile: File,
        outroFile: File?,
        outputFile: File
    ): Boolean {
        return try {
            FileOutputStream(outputFile).use { output ->
                // Write intro if exists
                introFile?.let { mergeFile(it, output) }
                
                // Write main content
                mergeFile(mainFile, output)
                
                // Write outro if exists
                outroFile?.let { mergeFile(it, output) }
            }
            
            // Update final WAV header
            updateFinalWavHeader(outputFile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun mergeFile(file: File, output: FileOutputStream) {
        FileInputStream(file).use { input ->
            // Skip WAV header (44 bytes)
            input.skip(44)
            input.copyTo(output)
        }
    }
    
    private fun updateFinalWavHeader(file: File) {
        // Minimal approach: recalc data length and write at header positions
        try {
            val totalDataSize = (file.length() - 44).toInt()
            java.io.RandomAccessFile(file, "rw").use { raf ->
                raf.seek(4)
                raf.writeInt(Integer.reverseBytes(totalDataSize + 36))
                raf.seek(40)
                raf.writeInt(Integer.reverseBytes(totalDataSize))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
