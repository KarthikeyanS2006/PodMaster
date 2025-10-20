// File: processor/ProfessionalAudioProcessor.kt
package com.podmaster.processor
import android.content.Context
import android.media.*
import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.*

class ProfessionalAudioProcessor {

    companion object {
        private const val TAG = "ProAudioProcessor"
        private const val SAMPLE_RATE = 44100
        private const val CHANNELS = 1
        private const val BIT_DEPTH = 16
        private const val WINDOW_SIZE = 2048
        private const val HOP_SIZE = 512
    }

    enum class ProcessingMode {
        GENTLE,      // Minimal noise reduction, preserves everything
        BALANCED,    // Moderate noise reduction (recommended)
        AGGRESSIVE   // Maximum noise reduction, removes more
    }

    /**
     * Process with selectable mode
     */
    fun processCompletePodcast(
        inputFile: File,
        outputFile: File,
        mode: ProcessingMode = ProcessingMode.BALANCED,
        progressCallback: ((Int) -> Unit)? = null,
        context: Context? = null  // ADD THIS
    ): Boolean {
        return try {
            Log.d(TAG, "Starting AI-powered processing with mode: $mode")
            progressCallback?.invoke(5)

            val rawAudio = extractRawAudio(inputFile)
            if (rawAudio.isEmpty()) {
                Log.e(TAG, "Failed to extract audio")
                return false
            }
            Log.d(TAG, "Extracted ${rawAudio.size} samples")

            val settings = getModeSettings(mode)

            progressCallback?.invoke(15)

            // AI NOISE SUPPRESSION
            val aiProcessed = if (context != null ) {
                progressCallback?.invoke(25)
                val aiSuppressor = AINoiseSuppresor(context)
                aiSuppressor.initialize()
                val aggressiveness = when(mode) {
                    ProcessingMode.GENTLE -> 0.3f
                    ProcessingMode.BALANCED -> 0.6f
                    ProcessingMode.AGGRESSIVE -> 0.9f
                }
                val result = aiSuppressor.suppressNoise(rawAudio, aggressiveness)
                aiSuppressor.release()
                progressCallback?.invoke(40)
                result
            } else {
                progressCallback?.invoke(40)
                rawAudio
            }

            // Voice detection
            val voiceSegments = detectVoiceSegments(aiProcessed, settings)
            Log.d(TAG, "Found ${voiceSegments.size} voice segments")

            // Enhancement
            progressCallback?.invoke(60)
            val enhanced = enhanceVoice(aiProcessed, voiceSegments, settings)
            Log.d(TAG, "Enhancement complete")

            // De-ess
            progressCallback?.invoke(75)
            val deEssed = applyDeEsser(enhanced, settings)

            // Compress
            progressCallback?.invoke(85)
            val compressed = applySmartCompression(deEssed, voiceSegments, settings)

            // Normalize
            progressCallback?.invoke(95)
            val normalized = normalizeToTarget(compressed, settings)

            saveAsWAV(normalized, outputFile)
            progressCallback?.invoke(100)

            Log.d(TAG, "AI processing complete!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Processing error", e)
            false
        }
    }

    // Simplified voice enhancement (after AI has removed noise)
    private fun enhanceVoice(
        audio: FloatArray,
        voiceSegments: List<IntRange>,
        settings: ModeSettings
    ): FloatArray {
        val result = FloatArray(audio.size)

        for (i in audio.indices) {
            val isVoice = voiceSegments.any { i in it }
            result[i] = if (isVoice) {
                (audio[i] * settings.voiceBoost).coerceIn(-1.0f, 1.0f)
            } else {
                audio[i] * 0.5f // Reduce non-voice parts
            }
        }

        return result
    }


    /**
     * Processing settings for each mode
     */
    private data class ModeSettings(
        val noiseThresholdMultiplier: Float,
        val voiceBoost: Float,
        val noiseReduction: Float,
        val compressionRatio: Float,
        val deEsserStrength: Float,
        val voiceDetectionThreshold: Float
    )

    private fun getModeSettings(mode: ProcessingMode): ModeSettings {
        return when (mode) {
            ProcessingMode.GENTLE -> ModeSettings(
                noiseThresholdMultiplier = 1.5f,
                voiceBoost = 1.15f,
                noiseReduction = 0.6f,             // Keep 60% (remove 40%)
                compressionRatio = 2.0f,
                deEsserStrength = 0.8f,
                voiceDetectionThreshold = 0.002f
            )

            ProcessingMode.BALANCED -> ModeSettings(
                noiseThresholdMultiplier = 2.2f,
                voiceBoost = 1.4f,
                noiseReduction = 0.3f,             // Keep 30% (remove 70%)
                compressionRatio = 3.0f,
                deEsserStrength = 0.7f,
                voiceDetectionThreshold = 0.003f
            )

            // Fix 1: Optimized AGGRESSIVE settings for better clarity and noise reduction
            ProcessingMode.AGGRESSIVE -> ModeSettings(
                noiseThresholdMultiplier = 3.0f,   // High threshold
                voiceBoost = 1.7f,                 // Strong voice boost
                noiseReduction = 0.05f,            // Keep only 5% (remove 95%!) - Extreme reduction requires careful VAD
                compressionRatio = 3.5f,           // Strong compression
                deEsserStrength = 0.65f,
                voiceDetectionThreshold = 0.002f   // CRITICAL: Must be sensitive to correctly detect all voice
            )
        }
    }

    private fun extractRawAudio(inputFile: File): FloatArray {
        val extractor = MediaExtractor()
        val result = mutableListOf<Float>()

        try {
            extractor.setDataSource(inputFile.absolutePath)

            var audioTrack = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    audioTrack = i
                    break
                }
            }

            if (audioTrack < 0) return FloatArray(0)

            extractor.selectTrack(audioTrack)
            val format = extractor.getTrackFormat(audioTrack)
            val mime = format.getString(MediaFormat.KEY_MIME)!!

            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var isEOS = false

            while (!isEOS) {
                val inputIndex = codec.dequeueInputBuffer(10000)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)!!
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)

                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEOS = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)!!
                    val samples = ShortArray(bufferInfo.size / 2)
                    outputBuffer.asShortBuffer().get(samples)

                    samples.forEach { sample ->
                        result.add(sample.toFloat() / 32768.0f)
                    }

                    codec.releaseOutputBuffer(outputIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                }
            }

            codec.stop()
            codec.release()
            extractor.release()

        } catch (e: Exception) {
            Log.e(TAG, "Extract error", e)
            extractor.release()
        }

        return result.toFloatArray()
    }

    private fun analyzeNoiseProfile(audio: FloatArray): Float {
        val energies = mutableListOf<Float>()

        for (i in 0 until audio.size - WINDOW_SIZE step HOP_SIZE) {
            val window = audio.sliceArray(i until min(i + WINDOW_SIZE, audio.size))
            val energy = window.map { it * it }.average().toFloat()
            energies.add(energy)
        }

        energies.sort()
        val noiseEnergy = energies.take(energies.size / 10).average().toFloat()
        return sqrt(noiseEnergy)
    }

    private fun detectVoiceSegments(audio: FloatArray, settings: ModeSettings): List<IntRange> {
        val segments = mutableListOf<IntRange>()
        var isVoice = false
        var segmentStart = 0
        var silenceCounter = 0
        val maxSilenceFrames = 3 // Allow 3 frames of "silence" before ending voice segment

        for (i in 0 until audio.size - WINDOW_SIZE step HOP_SIZE) {
            val window = audio.sliceArray(i until min(i + WINDOW_SIZE, audio.size))

            // Calculate energy
            val energy = window.map { it * it }.average()

            // Calculate zero crossing rate
            var zeroCrossings = 0
            for (j in 1 until window.size) {
                if ((window[j] >= 0 && window[j - 1] < 0) ||
                    (window[j] < 0 && window[j - 1] >= 0)) {
                    zeroCrossings++
                }
            }
            val zcr = zeroCrossings.toFloat() / window.size

            // Voice detection with better thresholds
            val hasVoice = energy > settings.voiceDetectionThreshold.toDouble() &&
                    zcr > 0.015 && // Lower ZCR threshold
                    zcr < 0.35     // Higher ZCR threshold

            if (hasVoice) {
                if (!isVoice) {
                    // Voice started
                    segmentStart = max(0, i - HOP_SIZE * 2) // Include a bit before
                    isVoice = true
                }
                silenceCounter = 0 // Reset silence counter
            } else if (isVoice) {
                // Potential silence
                silenceCounter++
                if (silenceCounter > maxSilenceFrames) {
                    // Voice ended
                    segments.add(segmentStart until (i + HOP_SIZE * 2)) // Include a bit after
                    isVoice = false
                    silenceCounter = 0
                }
            }
        }

        if (isVoice) {
            segments.add(segmentStart until audio.size)
        }

        return segments
    }

    // FIX 2: Enhanced logic for smoother noise reduction during speech
    private fun enhanceVoiceReduceNoise(
        audio: FloatArray,
        voiceSegments: List<IntRange>,
        noiseProfile: Float,
        settings: ModeSettings
    ): FloatArray {
        val result = FloatArray(audio.size)
        val reductionFactor = 1.0f - settings.noiseReduction // How much to *reduce* the noise

        for (i in audio.indices) {
            val isVoiceSegment = voiceSegments.any { i in it }
            val sample = audio[i]
            val absSample = abs(sample)

            if (isVoiceSegment) {
                // VOICE SEGMENT
                if (absSample > noiseProfile * settings.noiseThresholdMultiplier) {
                    // Strong Voice - boost it
                    result[i] = (sample * settings.voiceBoost).coerceIn(-1.0f, 1.0f)
                } else if (absSample > noiseProfile * 1.5f) {
                    // Low-level speech - apply only a mild noise reduction
                    // Blends reduction and original signal: 1.0f is original, reductionFactor is heavily reduced
                    val blend = 1.0f - (reductionFactor * 0.5f)
                    result[i] = sample * blend
                } else {
                    // Noise during speech - reduce with the main reduction factor
                    result[i] = sample * settings.noiseReduction
                }
            } else {
                // SILENCE SEGMENT
                if (absSample > noiseProfile * 1.5f) {
                    // Potential quiet noise - reduce heavily
                    result[i] = sample * settings.noiseReduction
                } else {
                    // Base noise - max reduction (more than during speech)
                    result[i] = sample * settings.noiseReduction * 0.5f
                }
            }
        }

        return result
    }

    // FIX 3: Adjusted de-esser to target only the loudest 's' sounds more accurately
    private fun applyDeEsser(audio: FloatArray, settings: ModeSettings): FloatArray {
        val result = audio.copyOf()

        for (i in 0 until audio.size - 512 step 256) {
            val window = audio.sliceArray(i until min(i + 512, audio.size))
            // Simple high frequency energy check
            val highFreqEnergy = window.takeLast(128).map { it * it }.average()

            if (highFreqEnergy > 0.08) { // Increased trigger threshold (was 0.05)
                for (j in i until min(i + 512, result.size)) {
                    // Only de-ess the loudest, most offensive peaks
                    if (abs(result[j]) > 0.6f) { // Increased volume threshold (was 0.3f)
                        result[j] *= settings.deEsserStrength
                    }
                }
            }
        }

        return result
    }

    // FIX 4: Adjusted compressor to work on the whole signal for consistent volume
    private fun applySmartCompression(
        audio: FloatArray,
        voiceSegments: List<IntRange>, // Kept for future logic, but not used in core compression
        settings: ModeSettings
    ): FloatArray {
        val result = FloatArray(audio.size)
        val threshold = 0.5f // The level above which to start compression
        val makeupGain = 1.2f // Adds 20% post-compression gain to increase overall loudness

        for (i in audio.indices) {
            val sample = audio[i]
            val absSample = abs(sample)

            val processedSample = if (absSample > threshold) {
                // Compression logic
                val excess = absSample - threshold
                val compressed = threshold + (excess / settings.compressionRatio)
                if (sample >= 0) compressed else -compressed
            } else {
                sample // Pass through if below threshold
            }

            // Apply makeup gain to increase the overall volume
            result[i] = (processedSample * makeupGain).coerceIn(-1.0f, 1.0f)
        }

        return result
    }

    private fun normalizeToTarget(audio: FloatArray, settings: ModeSettings): FloatArray {
        val voiceSamples = audio.filter { abs(it) > 0.1f }
        if (voiceSamples.isEmpty()) return audio

        val rms = sqrt(voiceSamples.map { it * it }.average().toFloat())
        val targetRMS = 0.3f
        val gain = if (rms > 0) (targetRMS / rms).coerceIn(0.5f, 2.5f) else 1.0f

        return FloatArray(audio.size) { i ->
            (audio[i] * gain).coerceIn(-0.95f, 0.95f)
        }
    }

    private fun saveAsWAV(audio: FloatArray, outputFile: File) {
        val sampleData = ShortArray(audio.size) { i ->
            (audio[i] * 32767).toInt().coerceIn(-32768, 32767).toShort()
        }

        RandomAccessFile(outputFile, "rw").use { raf ->
            raf.writeBytes("RIFF")
            writeIntLE(raf, 36 + sampleData.size * 2)
            raf.writeBytes("WAVE")
            raf.writeBytes("fmt ")
            writeIntLE(raf, 16)
            writeShortLE(raf, 1)
            writeShortLE(raf, CHANNELS.toShort())
            writeIntLE(raf, SAMPLE_RATE)
            writeIntLE(raf, SAMPLE_RATE * CHANNELS * BIT_DEPTH / 8)
            writeShortLE(raf, (CHANNELS * BIT_DEPTH / 8).toShort())
            writeShortLE(raf, BIT_DEPTH.toShort())
            raf.writeBytes("data")
            writeIntLE(raf, sampleData.size * 2)

            sampleData.forEach { sample ->
                writeShortLE(raf, sample)
            }
        }
    }

    private fun writeIntLE(raf: RandomAccessFile, value: Int) {
        raf.writeByte(value and 0xFF)
        raf.writeByte((value shr 8) and 0xFF)
        raf.writeByte((value shr 16) and 0xFF)
        raf.writeByte((value shr 24) and 0xFF)
    }

    private fun writeShortLE(raf: RandomAccessFile, value: Short) {
        raf.writeByte(value.toInt() and 0xFF)
        raf.writeByte((value.toInt() shr 8) and 0xFF)
    }
}