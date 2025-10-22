package com.podmaster.processor

import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File

class FFmpegAudioProcessor {

    fun trimAudio(
        inputPath: String,
        outputPath: String,
        startTime: Float,
        duration: Float
    ): Boolean {
        val command = "-i $inputPath -ss $startTime -t $duration -c copy $outputPath"
        return FFmpeg.execute(command) == 0
    }

    fun normalizeVolume(
        inputPath: String,
        outputPath: String,
        targetLoudness: Float = -16.0f
    ): Boolean {
        val command = "-i $inputPath -filter:a loudnorm=I=$targetLoudness:LRA=11:TP=-1.5 $outputPath"
        return FFmpeg.execute(command) == 0
    }

    fun mergeAudioFiles(
        inputFiles: List<String>,
        outputPath: String
    ): Boolean {
        if (inputFiles.isEmpty()) return false

        val inputsCommand = inputFiles.joinToString(" ") { "-i $it" }
        val filterCommand = inputFiles.mapIndexed { i, _ -> "[$i:a]" }.joinToString("")
        val command = "$inputsCommand -filter_complex '${filterCommand}concat=n=${inputFiles.size}:v=0:a=1[out]' -map '[out]' $outputPath"
        
        return FFmpeg.execute(command) == 0
    }

    fun applyFade(
        inputPath: String,
        outputPath: String,
        fadeInDuration: Float = 2.0f,
        fadeOutDuration: Float = 2.0f,
        totalDuration: Float
    ): Boolean {
        val command = "-i $inputPath -af 'afade=t=in:st=0:d=$fadeInDuration,afade=t=out:st=${totalDuration - fadeOutDuration}:d=$fadeOutDuration' $outputPath"
        return FFmpeg.execute(command) == 0
    }

    fun convertFormat(
        inputPath: String,
        outputPath: String,
        format: String = "mp3",
        bitrate: String = "192k"
    ): Boolean {
        val command = "-i $inputPath -c:a libmp3lame -b:a $bitrate $outputPath"
        return FFmpeg.execute(command) == 0
    }

    fun removeSilence(
        inputPath: String,
        outputPath: String,
        noiseLevel: String = "-50dB",
        duration: Float = 0.5f
    ): Boolean {
        val command = "-i $inputPath -af silenceremove=stop_periods=-1:stop_duration=$duration:stop_threshold=$noiseLevel $outputPath"
        return FFmpeg.execute(command) == 0
    }
}