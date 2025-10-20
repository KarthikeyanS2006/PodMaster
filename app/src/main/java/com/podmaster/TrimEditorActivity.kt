// File: TrimEditorActivity.kt
package com.podmaster

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.podmaster.databinding.ActivityTrimEditorBinding
import com.podmaster.processor.FFmpegAudioProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class TrimEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrimEditorBinding
    private lateinit var ffmpegProcessor: FFmpegAudioProcessor
    private var audioFilePath: String? = null
    private var audioDuration: Float = 0f
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrimEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ffmpegProcessor = FFmpegAudioProcessor()
        audioFilePath = intent.getStringExtra("AUDIO_FILE_PATH")

        if (audioFilePath == null) {
            Toast.makeText(this, "No audio file provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadAudioInfo()
        setupListeners()
    }

    private fun loadAudioInfo() {
        audioFilePath?.let { path ->
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                audioDuration = (duration / 1000f)
            }

            binding.sliderEnd.value = audioDuration
            binding.sliderEnd.valueTo = audioDuration
            binding.sliderStart.valueTo = audioDuration

            updateTimeDisplays()
        }
    }

    private fun setupListeners() {
        binding.sliderStart.addOnChangeListener { _, value, _ ->
            if (value >= binding.sliderEnd.value) {
                binding.sliderStart.value = binding.sliderEnd.value - 1
            }
            updateTimeDisplays()
        }

        binding.sliderEnd.addOnChangeListener { _, value, _ ->
            if (value <= binding.sliderStart.value) {
                binding.sliderEnd.value = binding.sliderStart.value + 1
            }
            updateTimeDisplays()
        }

        binding.btnSaveTrim.setOnClickListener {
            saveTrimmedAudio()
        }
    }

    private fun updateTimeDisplays() {
        val startSeconds = binding.sliderStart.value.toInt()
        val endSeconds = binding.sliderEnd.value.toInt()

        binding.tvStartTime.text = formatTime(startSeconds)
        binding.tvEndTime.text = formatTime(endSeconds)
    }

    private fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }

    private fun saveTrimmedAudio() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnSaveTrim.isEnabled = false

        lifecycleScope.launch {
            try {
                val startTime = binding.sliderStart.value
                val endTime = binding.sliderEnd.value
                val duration = endTime - startTime

                val outputFile = File(
                    getExternalFilesDir(null),
                    "trimmed_${System.currentTimeMillis()}.m4a"
                )

                val success = withContext(Dispatchers.IO) {
                    ffmpegProcessor.trimAudio(
                        audioFilePath!!,
                        outputFile.absolutePath,
                        startTime,
                        duration
                    )
                }

                if (success) {
                    Toast.makeText(
                        this@TrimEditorActivity,
                        "Trimmed audio saved",
                        Toast.LENGTH_LONG
                    ).show()

                    intent.putExtra("TRIMMED_FILE", outputFile.absolutePath)
                    setResult(RESULT_OK, intent)
                    finish()
                } else {
                    Toast.makeText(
                        this@TrimEditorActivity,
                        "Failed to trim audio",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@TrimEditorActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnSaveTrim.isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
