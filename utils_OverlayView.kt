package com.example.qrpro.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val boxPaint = Paint().apply {
        color = Color.parseColor("#FFEB3B")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val cornerPaint = Paint().apply {
        color = Color.parseColor("#FFEB3B")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var barcodeRect: RectF? = null
    private var scanLineY = 0f
    private var scanDirection = 1
    private val scanPaint = Paint().apply {
        color = Color.parseColor("#FF5252")
        strokeWidth = 4f
        isAntiAlias = true
    }

    init {
        // Animasi scan line
        post(object : Runnable {
            override fun run() {
                scanLineY += scanDirection * 8
                if (scanLineY > height - 100 || scanLineY < 100) {
                    scanDirection *= -1
                }
                invalidate()
                postDelayed(this, 16)
            }
        })
    }

    fun setBarcodeRect(rect: RectF?) {
        barcodeRect = rect
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Scan line
        if (barcodeRect == null) {
            canvas.drawLine(80f, scanLineY.toFloat(), width - 80f, scanLineY.toFloat(), scanPaint)
        }

        // Bounding box
        barcodeRect?.let { rect ->
            canvas.drawRoundRect(rect, 16f, 16f, boxPaint)
            // Corner accents
            val c = 40f
            // Top-left
            canvas.drawRect(rect.left, rect.top, rect.left + c, rect.top + 8f, cornerPaint)
            canvas.drawRect(rect.left, rect.top, rect.left + 8f, rect.top + c, cornerPaint)
            // Top-right
            canvas.drawRect(rect.right - c, rect.top, rect.right, rect.top + 8f, cornerPaint)
            canvas.drawRect(rect.right - 8f, rect.top, rect.right, rect.top + c, cornerPaint)
            // Bottom-left
            canvas.drawRect(rect.left, rect.bottom - 8f, rect.left + c, rect.bottom, cornerPaint)
            canvas.drawRect(rect.left, rect.bottom - c, rect.left + 8f, rect.bottom, cornerPaint)
            // Bottom-right
            canvas.drawRect(rect.right - c, rect.bottom - 8f, rect.right, rect.bottom, cornerPaint)
            canvas.drawRect(rect.right - 8f, rect.bottom - c, rect.right, rect.bottom, cornerPaint)
        }
    }
}