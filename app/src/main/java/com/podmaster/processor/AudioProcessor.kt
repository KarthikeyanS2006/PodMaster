// File: processor/AudioProcessor.kt - COMPLETE REPLACEMENT
package com.podmaster.processor

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.abs

class AudioProcessor {

    /**
     * Remove silence from audio file using MediaExtractor/MediaCodec
     */
    fun removeSilenceFromFile(inputFile: File, outputFile: File): Boolean {
        return try {
            // For M4A/AAC files, we need to decode first
            val rawAudio = decodeAudioFile(inputFile)
            if (rawAudio.isEmpty()) {
                Log.e("AudioProcessor", "Failed to decode audio")
                // Fallback: just copy file
                inputFile.copyTo(outputFile, overwrite = true)
                return true
            }

            val processed = removeSilenceFromBytes(rawAudio)
            FileOutputStream(outputFile).use { it.write(processed) }
            true
        } catch (e: Exception) {
            Log.e("AudioProcessor", "Error removing silence: ${e.message}")
            // Fallback: copy original
            try {
                inputFile.copyTo(outputFile, overwrite = true)
                true
            } catch (ex: Exception) {
                false
            }
        }
    }

    /**
     * Decode audio file to raw PCM data
     */
    private fun decodeAudioFile(file: File): ByteArray {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)

            // Find audio track
            var trackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    trackIndex = i
                    break
                }
            }

            if (trackIndex < 0) {
                Log.e("AudioProcessor", "No audio track found")
                return ByteArray(0)
            }

            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)

            // Create decoder
            val codec = MediaCodec.createDecoderByType(mime!!)
            codec.configure(format, null, null, 0)
            codec.start()

            val outputList = mutableListOf<Byte>()
            val bufferInfo = MediaCodec.BufferInfo()
            var isEOS = false

            while (!isEOS) {
                // Feed input
                val inputIndex = codec.dequeueInputBuffer(10000)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)
                    val sampleSize = extractor.readSampleData(inputBuffer!!, 0)

                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEOS = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                // Get output
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)
                    val chunk = ByteArray(bufferInfo.size)
                    outputBuffer?.get(chunk)
                    outputList.addAll(chunk.toList())

                    codec.releaseOutputBuffer(outputIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                }
            }

            codec.stop()
            codec.release()
            extractor.release()

            outputList.toByteArray()
        } catch (e: Exception) {
            Log.e("AudioProcessor", "Decode error: ${e.message}")
            extractor.release()
            ByteArray(0)
        }
    }

    /**
     * Remove silence from raw audio bytes
     */
    private fun removeSilenceFromBytes(audioData: ByteArray): ByteArray {
        if (audioData.size < 2) return audioData

        val silenceThreshold = 500
        val result = mutableListOf<Byte>()

        var i = 0
        while (i < audioData.size - 1) {
            val sample = ((audioData[i + 1].toInt() shl 8) or (audioData[i].toInt() and 0xFF)).toShort()

            if (abs(sample.toInt()) > silenceThreshold) {
                result.add(audioData[i])
                result.add(audioData[i + 1])
            }

            i += 2
        }

        return if (result.isEmpty()) audioData else result.toByteArray()
    }

    /**
     * Apply noise gate
     */
    fun applyNoiseGate(inputFile: File, outputFile: File, threshold: Int = 800): Boolean {
        return try {
            val audioData = inputFile.readBytes()
            val result = mutableListOf<Byte>()

            var i = 0
            while (i < audioData.size - 1) {
                val sample = ((audioData[i + 1].toInt() shl 8) or (audioData[i].toInt() and 0xFF)).toShort()

                if (abs(sample.toInt()) > threshold) {
                    result.add(audioData[i])
                    result.add(audioData[i + 1])
                } else {
                    result.add(0)
                    result.add(0)
                }

                i += 2
            }

            FileOutputStream(outputFile).use {
                it.write(if (result.isEmpty()) audioData else result.toByteArray())
            }
            true
        } catch (e: Exception) {
            Log.e("AudioProcessor", "Noise gate error: ${e.message}")
            try {
                inputFile.copyTo(outputFile, overwrite = true)
                true
            } catch (ex: Exception) {
                false
            }
        }
    }

    /**
     * Normalize volume
     */
    fun normalizeVolume(inputFile: File, outputFile: File): Boolean {
        return try {
            val audioData = inputFile.readBytes()

            if (audioData.size < 2) {
                inputFile.copyTo(outputFile, overwrite = true)
                return true
            }

            // Find max amplitude
            var maxAmplitude = 0
            var i = 0
            while (i < audioData.size - 1) {
                val sample = ((audioData[i + 1].toInt() shl 8) or (audioData[i].toInt() and 0xFF)).toShort()
                maxAmplitude = maxOf(maxAmplitude, abs(sample.toInt()))
                i += 2
            }

            if (maxAmplitude == 0) {
                inputFile.copyTo(outputFile, overwrite = true)
                return true
            }

            // Calculate normalization factor
            val targetMax = 25000 // Leave headroom
            val factor = targetMax.toFloat() / maxAmplitude

            // Apply normalization
            val normalized = ByteArray(audioData.size)
            i = 0
            while (i < audioData.size - 1) {
                val sample = ((audioData[i + 1].toInt() shl 8) or (audioData[i].toInt() and 0xFF)).toShort()
                val normalizedSample = (sample * factor).toInt().coerceIn(-32768, 32767).toShort()

                normalized[i] = (normalizedSample.toInt() and 0xFF).toByte()
                normalized[i + 1] = (normalizedSample.toInt() shr 8 and 0xFF).toByte()

                i += 2
            }

            FileOutputStream(outputFile).use { it.write(normalized) }
            true
        } catch (e: Exception) {
            Log.e("AudioProcessor", "Normalize error: ${e.message}")
            try {
                inputFile.copyTo(outputFile, overwrite = true)
                true
            } catch (ex: Exception) {
                false
            }
        }
    }
}
