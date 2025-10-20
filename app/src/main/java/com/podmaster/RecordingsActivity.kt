// File: RecordingsActivity.kt
package com.podmaster

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.podmaster.adapters.RecordingsAdapter
import com.podmaster.databinding.ActivityRecordingsBinding
import com.podmaster.models.Recording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class RecordingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordingsBinding
    private lateinit var adapter: RecordingsAdapter
    private var allRecordings = mutableListOf<Recording>()
    private var filteredRecordings = mutableListOf<Recording>()
    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlaying: Recording? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupSortOptions()
        loadRecordings()

        binding.fabDeleteAll.setOnClickListener {
            showDeleteAllDialog()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = RecordingsAdapter(
            onPlayClick = { recording -> playRecording(recording) },
            onShareClick = { recording -> shareRecording(recording) },
            onDeleteClick = { recording -> showDeleteDialog(recording) },
            onMoreClick = { recording -> showMoreOptions(recording) }
        )

        binding.recyclerViewRecordings.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewRecordings.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterRecordings(s.toString())
            }
        })
    }

    private fun setupSortOptions() {
        binding.chipGroupSort.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            when (checkedIds[0]) {
                R.id.chipDate -> sortByDate()
                R.id.chipName -> sortByName()
                R.id.chipSize -> sortBySize()
            }
        }
    }

    private fun loadRecordings() {
        lifecycleScope.launch {
            val recordings = withContext(Dispatchers.IO) {
                val recordingsDir = getExternalFilesDir(null)
                recordingsDir?.listFiles()
                    ?.filter { it.extension in listOf("m4a", "mp3", "wav", "aac") }
                    ?.map { file ->
                        Recording(
                            file = file,
                            duration = getAudioDuration(file)
                        )
                    }
                    ?.sortedByDescending { it.dateModified }
                    ?: emptyList()
            }

            allRecordings.clear()
            allRecordings.addAll(recordings)
            filteredRecordings.clear()
            filteredRecordings.addAll(recordings)

            updateUI()
        }
    }

    private fun getAudioDuration(file: File): Long {
        return try {
            val mp = MediaPlayer()
            mp.setDataSource(file.absolutePath)
            mp.prepare()
            val duration = mp.duration.toLong()
            mp.release()
            duration
        } catch (e: Exception) {
            0L
        }
    }

    private fun filterRecordings(query: String) {
        filteredRecordings.clear()

        if (query.isEmpty()) {
            filteredRecordings.addAll(allRecordings)
        } else {
            filteredRecordings.addAll(
                allRecordings.filter {
                    it.name.contains(query, ignoreCase = true)
                }
            )
        }

        updateUI()
    }

    private fun sortByDate() {
        filteredRecordings.sortByDescending { it.dateModified }
        updateUI()
    }

    private fun sortByName() {
        filteredRecordings.sortBy { it.name }
        updateUI()
    }

    private fun sortBySize() {
        filteredRecordings.sortByDescending { it.size }
        updateUI()
    }

    private fun updateUI() {
        adapter.submitList(filteredRecordings.toList())

        binding.tvRecordingsCount.text = "${filteredRecordings.size} recording(s)"

        if (filteredRecordings.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.recyclerViewRecordings.visibility = View.GONE
            binding.fabDeleteAll.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.recyclerViewRecordings.visibility = View.VISIBLE
            binding.fabDeleteAll.visibility = View.VISIBLE
        }
    }

    // Replace the playRecording function in RecordingsActivity.kt
    private fun playRecording(recording: Recording) {
        try {
            // Stop current playback
            mediaPlayer?.release()
            mediaPlayer = null

            // Check if file exists and is valid
            if (!recording.file.exists()) {
                Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
                return
            }

            if (recording.file.length() == 0L) {
                Toast.makeText(this, "File is empty", Toast.LENGTH_SHORT).show()
                return
            }

            // Start new playback
            mediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(recording.path)
                    setOnPreparedListener { mp ->
                        mp.start()
                        currentlyPlaying = recording
                        Toast.makeText(
                            this@RecordingsActivity,
                            "Playing: ${recording.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    setOnCompletionListener {
                        currentlyPlaying = null
                        Toast.makeText(
                            this@RecordingsActivity,
                            "Playback completed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    setOnErrorListener { _, what, extra ->
                        Toast.makeText(
                            this@RecordingsActivity,
                            "Playback error: Cannot play this file format",
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }

                    prepareAsync() // Use async prepare
                } catch (e: Exception) {
                    Toast.makeText(
                        this@RecordingsActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error playing audio: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun shareRecording(recording: Recording) {
        val uri = FileProvider.getUriForFile(
            this,
            "com.podmaster.provider",
            recording.file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, recording.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share recording"))
    }

    private fun showDeleteDialog(recording: Recording) {
        AlertDialog.Builder(this)
            .setTitle("Delete Recording")
            .setMessage("Are you sure you want to delete \"${recording.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                deleteRecording(recording)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteRecording(recording: Recording) {
        lifecycleScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                recording.file.delete()
            }

            if (deleted) {
                allRecordings.remove(recording)
                filteredRecordings.remove(recording)
                updateUI()
                Toast.makeText(this@RecordingsActivity, "Deleted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@RecordingsActivity, "Failed to delete", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteAllDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete All Recordings")
            .setMessage("Are you sure you want to delete ALL ${allRecordings.size} recordings? This cannot be undone.")
            .setPositiveButton("Delete All") { _, _ ->
                deleteAllRecordings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAllRecordings() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                allRecordings.forEach { it.file.delete() }
            }

            allRecordings.clear()
            filteredRecordings.clear()
            updateUI()

            Toast.makeText(this@RecordingsActivity, "All recordings deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMoreOptions(recording: Recording) {
        val options = arrayOf("Rename", "Details", "Open in Editor")

        AlertDialog.Builder(this)
            .setTitle(recording.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(recording)
                    1 -> showDetailsDialog(recording)
                    2 -> openInEditor(recording)
                }
            }
            .show()
    }

    private fun showRenameDialog(recording: Recording) {
        val input = android.widget.EditText(this)
        input.setText(recording.name)

        AlertDialog.Builder(this)
            .setTitle("Rename Recording")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotBlank()) {
                    renameRecording(recording, newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renameRecording(recording: Recording, newName: String) {
        lifecycleScope.launch {
            val renamed = withContext(Dispatchers.IO) {
                val newFile = File(recording.file.parent, newName)
                recording.file.renameTo(newFile)
            }

            if (renamed) {
                loadRecordings()
                Toast.makeText(this@RecordingsActivity, "Renamed", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@RecordingsActivity, "Failed to rename", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDetailsDialog(recording: Recording) {
        val details = """
            Name: ${recording.name}
            Size: ${recording.getSizeInMB()}
            Duration: ${recording.getDurationFormatted()}
            Date: ${recording.getFormattedDate()}
            Path: ${recording.path}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Recording Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun openInEditor(recording: Recording) {
        val intent = Intent(this, TrimEditorActivity::class.java)
        intent.putExtra("AUDIO_FILE_PATH", recording.path)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onResume() {
        super.onResume()
        loadRecordings() // Refresh list when returning
    }
}
