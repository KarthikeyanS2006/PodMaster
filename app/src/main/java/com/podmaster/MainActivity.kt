// File: MainActivity.kt
package com.podmaster

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.podmaster.databinding.ActivityMainBinding
import com.podmaster.processor.FFmpegAudioProcessor
import com.podmaster.processor.ProfessionalAudioProcessor
import com.podmaster.upload.SpotifyUploader
import com.podmaster.upload.YouTubeUploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var audioRecorder: AudioRecorderManager
    private lateinit var ffmpegProcessor: FFmpegAudioProcessor
    private lateinit var puterStorage: PuterCloudStorage
    private lateinit var spotifyUploader: SpotifyUploader
    private lateinit var youtubeUploader: YouTubeUploader

    private var currentAudioFile: String? = null
    private var processedAudioFile: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private var recordingTime = 0L
    private var isRecording = false
    private var isPaused = false

    private val trimEditorLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val trimmedFile = result.data?.getStringExtra("TRIMMED_FILE")
            if (trimmedFile != null) {
                currentAudioFile = trimmedFile
                Toast.makeText(this, "Audio trimmed successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isRecording && !isPaused) {
                recordingTime += 1000
                updateTimerDisplay()
                handler.postDelayed(this, 1000)
            }
        }
    }

    private val amplitudeRunnable = object : Runnable {
        override fun run() {
            if (isRecording && !isPaused) {
                val amplitude = audioRecorder.getAmplitude()
                binding.waveformView.addAmplitude(amplitude.toFloat())
                handler.postDelayed(this, 100)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioRecorder = AudioRecorderManager(this)
        ffmpegProcessor = FFmpegAudioProcessor()
        puterStorage = PuterCloudStorage()
        spotifyUploader = SpotifyUploader(this)
        youtubeUploader = YouTubeUploader(this)

        checkPermissions()
        setupClickListeners()
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 100)
        }
    }

    private fun setupClickListeners() {
        binding.fabRecord.setOnClickListener {
            if (!isRecording) {
                startRecording()
            }
        }

        binding.fabPause.setOnClickListener {
            if (!isPaused) {
                pauseRecording()
            } else {
                resumeRecording()
            }
        }

        binding.fabStop.setOnClickListener {
            stopRecording()
        }

        binding.btnViewRecordings.setOnClickListener {
            startActivity(Intent(this, RecordingsActivity::class.java))
        }

        binding.btnProcessAudio.setOnClickListener {
            processAudio()
        }

        binding.btnExport.setOnClickListener {
            exportPodcast()
        }

        binding.btnUploadSpotify.setOnClickListener {
            uploadToSpotify()
        }

        binding.btnUploadYouTube.setOnClickListener {
            uploadToYouTube()
        }
    }

    private fun startRecording() {
        currentAudioFile = null
        processedAudioFile = null

        val filePath = audioRecorder.startRecording()
        if (filePath != null) {
            currentAudioFile = filePath
            isRecording = true
            isPaused = false
            recordingTime = 0

            binding.btnProcessAudio.visibility = View.GONE
            binding.btnExport.visibility = View.GONE
            binding.uploadLayout.visibility = View.GONE

            binding.tvStatus.text = "Recording..."
            binding.tvStatusIcon.text = "🔴"
            binding.fabRecord.visibility = View.GONE
            binding.fabPause.visibility = View.VISIBLE
            binding.fabStop.visibility = View.VISIBLE

            handler.post(timerRunnable)
            handler.post(amplitudeRunnable)

            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pauseRecording() {
        audioRecorder.pauseRecording()
        isPaused = true
        binding.tvStatus.text = "Paused"
        binding.tvStatusIcon.text = "⏸️"
        binding.fabPause.setImageResource(android.R.drawable.ic_media_play)
    }

    private fun resumeRecording() {
        audioRecorder.resumeRecording()
        isPaused = false
        binding.tvStatus.text = "Recording..."
        binding.tvStatusIcon.text = "🔴"
        binding.fabPause.setImageResource(android.R.drawable.ic_media_pause)
        handler.post(amplitudeRunnable)
    }

    private fun stopRecording() {
        val filePath = audioRecorder.stopRecording()
        isRecording = false
        isPaused = false
        handler.removeCallbacks(timerRunnable)
        handler.removeCallbacks(amplitudeRunnable)

        processedAudioFile = null

        binding.tvStatus.text = "Recording saved"
        binding.tvStatusIcon.text = "✅"
        binding.fabRecord.visibility = View.VISIBLE
        binding.fabPause.visibility = View.GONE
        binding.fabStop.visibility = View.GONE
        binding.btnProcessAudio.visibility = View.VISIBLE

        binding.btnExport.visibility = View.GONE
        binding.uploadLayout.visibility = View.GONE

        binding.waveformView.clear()

        if (filePath != null) {
            Toast.makeText(this, "Recording saved: ${File(filePath).name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processAudio() {
        currentAudioFile?.let { audioPath ->

            // Show mode selection dialog
            val modes = arrayOf(
                "🌿 Gentle - Minimal noise reduction\n(Preserves natural sound)",
                "⚖️ Balanced - Moderate enhancement\n(Recommended for most podcasts)",
                "⚡ Aggressive - Maximum noise removal\n(Clean professional sound)"
            )

            AlertDialog.Builder(this)
                .setTitle("Choose Processing Mode")
                .setSingleChoiceItems(modes, 1) { dialog, which ->
                    dialog.dismiss()

                    val mode = when (which) {
                        0 -> ProfessionalAudioProcessor.ProcessingMode.GENTLE
                        1 -> ProfessionalAudioProcessor.ProcessingMode.BALANCED
                        2 -> ProfessionalAudioProcessor.ProcessingMode.AGGRESSIVE
                        else -> ProfessionalAudioProcessor.ProcessingMode.BALANCED
                    }

                    val modeName = when (which) {
                        0 -> "Gentle"
                        1 -> "Balanced"
                        2 -> "Aggressive"
                        else -> "Balanced"
                    }

                    startProcessing(audioPath, mode, modeName)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun startProcessing(
        audioPath: String,
        mode: ProfessionalAudioProcessor.ProcessingMode,
        modeName: String
    ) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnProcessAudio.isEnabled = false

        lifecycleScope.launch {
            try {
                binding.tvStatus.text = "Loading audio..."
                val processor = ProfessionalAudioProcessor()

                val outputFile = File(
                    getExternalFilesDir(null),
                    "podcast_${modeName.lowercase()}_${System.currentTimeMillis()}.wav"
                )

                binding.tvStatus.text = "Starting $modeName processing..."

                val success = withContext(Dispatchers.IO) {
                    processor.processCompletePodcast(
                        File(audioPath),
                        outputFile,
                        mode,
                        { progress ->
                            runOnUiThread {
                                val statusText = when {
                                    progress < 15 -> "Analyzing audio..."
                                    progress < 30 -> "Detecting voice..."
                                    progress < 50 -> "Removing noise..."
                                    progress < 70 -> "Enhancing voice..."
                                    progress < 85 -> "Applying effects..."
                                    progress < 100 -> "Finalizing..."
                                    else -> "Complete!"
                                }
                                binding.tvStatus.text = "$statusText ($progress%)"
                            }
                        },
                        null  // Remove context to skip slow AI
                    )
                }

                if (success) {
                    processedAudioFile = outputFile.absolutePath
                    showSuccess("✨ $modeName processing complete!")
                    binding.tvStatusIcon.text = "✨"
                    binding.btnExport.visibility = View.VISIBLE
                } else {
                    showError("Processing failed")
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Processing error", e)
                showError("Error: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnProcessAudio.isEnabled = true
            }
        }
    }



    private fun uploadToSpotify() {
        processedAudioFile?.let { filePath ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://podcasters.spotify.com/")
            }

            try {
                startActivity(intent)
                Toast.makeText(
                    this,
                    "Opening Spotify for Podcasters",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to open Spotify", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadToYouTube() {
        processedAudioFile?.let { filePath ->
            youtubeUploader.uploadToYouTube(File(filePath), "My Podcast Episode")
        }
    }

    private fun exportPodcast() {
        processedAudioFile?.let { filePath ->
            val file = File(filePath)

            val options = arrayOf(
                "📤 Share File",
                "📋 Copy Path",
                "📁 View in Files"
            )

            AlertDialog.Builder(this)
                .setTitle("Export Recording")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> shareFile(file)
                        1 -> copyPath(filePath)
                        2 -> viewInFiles(filePath)
                    }
                }
                .show()

            binding.uploadLayout.visibility = View.VISIBLE
        }
    }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "com.podmaster.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "My Podcast - ${file.name}")
            putExtra(Intent.EXTRA_TEXT, "Check out my podcast recording!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share Podcast"))
    }

    private fun copyPath(path: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("File Path", path)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "✅ Path copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun viewInFiles(path: String) {
        Toast.makeText(this, "📁 Location:\n$path", Toast.LENGTH_LONG).show()
    }

    private fun updateTimerDisplay() {
        val seconds = (recordingTime / 1000) % 60
        val minutes = (recordingTime / (1000 * 60)) % 60
        val hours = (recordingTime / (1000 * 60 * 60))

        binding.tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun showSuccess(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            binding.tvStatus.text = message
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this, "❌ $message", Toast.LENGTH_LONG).show()
            binding.tvStatus.text = "Error"
            binding.tvStatusIcon.text = "❌"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
        handler.removeCallbacks(amplitudeRunnable)
    }
}
