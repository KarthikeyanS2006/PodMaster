<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">PodMaster</string>
    <string name="upload_success">Audio uploaded successfully</string>
    <string name="upload_failed">Failed to upload audio</string>import android.util.AttributeSet
    <string name="download_success">Audio downloaded successfully</string>
    <string name="download_failed">Failed to download audio</string>ContextCompat
    <string name="record_start">Start Recording</string>
    <string name="record_stop">Stop Recording</string>
    <string name="record_pause">Pause Recording</string> WaveformView @JvmOverloads constructor(
    <string name="record_resume">Resume Recording</string>    context: Context,







</resources>    <string name="upload_youtube">Upload to YouTube</string>    <string name="upload_spotify">Upload to Spotify</string>    <string name="share_recording">Share Recording</string>    <string name="enhance_audio">Enhance Audio Quality</string>    <string name="processing">Processing audio...</string>    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val waveformPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.waveform_line)
        strokeWidth = 4f























































}    }        }            )                waveformPaint                x, centerY + barHeight,                x, centerY - barHeight,            canvas.drawLine(            val barHeight = amplitude * (height / 2)            val x = index * barWidth + barWidth / 2        amplitudes.forEachIndexed { index, amplitude ->        // Draw waveform        }            canvas.drawRect(left, 0f, right, height, selectedPaint)            val right = selectedEndPosition * barWidth            val left = selectedStartPosition * barWidth        if (selectedStartPosition >= 0 && selectedEndPosition >= 0) {        // Draw selection background if exists        val barWidth = width / amplitudes.size        val centerY = height / 2        val height = height.toFloat()        val width = width.toFloat()        if (amplitudes.isEmpty()) return                super.onDraw(canvas)    override fun onDraw(canvas: Canvas) {    }        invalidate()        selectedEndPosition = -1        selectedStartPosition = -1        amplitudes = FloatArray(0)    fun clear() {    }        invalidate()        selectedEndPosition = endPos        selectedStartPosition = startPos    fun setSelection(startPos: Int, endPos: Int) {    }        invalidate()        amplitudes = newAmplitudes    fun updateAmplitudes(newAmplitudes: FloatArray) {    private var selectedEndPosition = -1    private var selectedStartPosition = -1    private var amplitudes = FloatArray(0)    }        style = Paint.Style.FILL        isAntiAlias = true
        strokeWidth = 4f
        color = ContextCompat.getColor(context, R.color.waveform_selected)
    private val selectedPaint = Paint().apply {

    }
        style = Paint.Style.STROKE        isAntiAlias = true