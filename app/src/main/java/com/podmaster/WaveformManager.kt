import android.media.audiofx.Visualizer
import kotlin.math.abs

class WaveformManager(private val audioSessionId: Int) {
    
    private var visualizer: Visualizer? = null
    private val waveformData = mutableListOf<Float>()
    private var onWaveformUpdateListener: ((FloatArray) -> Unit)? = null
    
    fun initialize() {
        visualizer = Visualizer(audioSessionId).apply {
            captureSize = Visualizer.getCaptureSizeRange()[1]
            setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        waveform?.let { processWaveform(it) }
                    }
                    
                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        // Process FFT data if needed
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                true,
                false
            )
            enabled = true
        }
    }
    
    private fun processWaveform(waveform: ByteArray) {
        val amplitudes = FloatArray(waveform.size) { i ->
            abs(waveform[i].toFloat()) / 128f
        }
        onWaveformUpdateListener?.invoke(amplitudes)
    }
    
    fun setWaveformUpdateListener(listener: (FloatArray) -> Unit) {
        onWaveformUpdateListener = listener
    }
    
    fun release() {
        visualizer?.release()
        visualizer = null
    }
}
