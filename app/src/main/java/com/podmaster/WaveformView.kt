// File: WaveformView.kt
package com.podmaster.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val amplitudes = mutableListOf<Float>()
    private var maxAmplitudes = 100 // Maximum number of bars to display

    private val paint = Paint().apply {
        color = Color.parseColor("#6200EE") // Purple
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }

    private val linePaint = Paint().apply {
        color = Color.parseColor("#03DAC5") // Teal
        strokeWidth = 2f
    }

    fun addAmplitude(amplitude: Float) {
        val normalizedAmplitude = amplitude.coerceIn(0f, 32767f) / 32767f
        amplitudes.add(normalizedAmplitude)

        // Keep only the last maxAmplitudes
        if (amplitudes.size > maxAmplitudes) {
            amplitudes.removeAt(0)
        }

        invalidate() // Redraw
    }

    fun clear() {
        amplitudes.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2

        // Draw center line
        canvas.drawLine(0f, centerY, width, centerY, linePaint)

        if (amplitudes.isEmpty()) return

        val barWidth = width / maxAmplitudes

        amplitudes.forEachIndexed { index, amplitude ->
            val x = index * barWidth + barWidth / 2
            val barHeight = amplitude * (height / 2) * 0.8f // 80% of half height

            // Draw bar from center up
            canvas.drawLine(
                x,
                centerY - barHeight,
                x,
                centerY + barHeight,
                paint
            )
        }
    }
}
