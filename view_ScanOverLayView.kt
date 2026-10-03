package com.example.qrpro.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class ScanOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val boxPaint = Paint().apply {
        color = Color.parseColor("#FFC107")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        color = Color.RED
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val dimPaint = Paint().apply {
        color = Color.parseColor("#99000000")
    }

    private var barcodeRect: RectF? = null
    private var scanLineY = 0f
    private var goingDown = true

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Animasi scan line
        scanLineY += if (goingDown) 8f else -8f
        if (scanLineY > height) goingDown = false
        if (scanLineY < 0) goingDown = true
        canvas.drawLine(0f, scanLineY, width.toFloat(), scanLineY, linePaint)
        invalidate()

        // Gambar bounding box jika ada
        barcodeRect?.let {
            canvas.drawRect(it, boxPaint)
        }
    }

    fun setBarcodeRect(rect: RectF?) {
        barcodeRect = rect
        invalidate()
    }
}