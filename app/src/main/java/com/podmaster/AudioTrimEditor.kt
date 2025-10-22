package com.podmaster

import java.io.File
import java.io.RandomAccessFile

class AudioTrimEditor {

    data class TrimRange(val startMs: Long, val endMs: Long)

    fun trimAudio(
        inputFile: File,
        outputFile: File,
        trimRange: TrimRange,
        sampleRate: Int = 44100
    ): Boolean {
        return try {
            val bytesPerSecond = sampleRate * 2 // 16-bit mono
            val startByte = (trimRange.startMs * bytesPerSecond / 1000).toInt()
            val endByte = (trimRange.endMs * bytesPerSecond / 1000).toInt()

            RandomAccessFile(inputFile, "r").use { input ->
                RandomAccessFile(outputFile, "rw").use { output ->
                    val header = ByteArray(44)
                    input.read(header)
                    output.write(header)

                    input.seek(44 + startByte.toLong())
                    val trimmedData = ByteArray(endByte - startByte)
                    input.readFully(trimmedData)
                    output.write(trimmedData)

                    updateWavHeader(output, trimmedData.size)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun updateWavHeader(file: RandomAccessFile, dataSize: Int) {
        file.seek(4)
        file.writeInt(Integer.reverseBytes(dataSize + 36))
        file.seek(40)
        file.writeInt(Integer.reverseBytes(dataSize))
    }
}
