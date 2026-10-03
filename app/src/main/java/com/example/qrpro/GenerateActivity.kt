package com.example.qrpro

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class GenerateActivity : AppCompatActivity() {

    // State
    private var qrBitmap: Bitmap? = null
    private var currentColor = Color.BLACK
    private var currentBg = Color.WHITE
    private var currentFrame = "none"      // none, blue, red, green, purple, gold
    private var currentTemplate = "none"   // none, dots, stars, hearts, flowers, circle
    private var labelText = ""
    private var useLogo = false

    private lateinit var ivQR: ImageView
    private lateinit var etInput: EditText
    private lateinit var etLabel: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate)

        etInput = findViewById(R.id.etInput)
        etLabel = findViewById(R.id.etLabel)
        ivQR = findViewById(R.id.ivQR)

        // Tombol warna QR
        findViewById<Button>(R.id.btnColorBlack).setOnClickListener { currentColor = Color.BLACK; regenerate() }
        findViewById<Button>(R.id.btnColorBlue).setOnClickListener { currentColor = Color.parseColor("#2196F3"); regenerate() }
        findViewById<Button>(R.id.btnColorRed).setOnClickListener { currentColor = Color.parseColor("#F44336"); regenerate() }
        findViewById<Button>(R.id.btnColorGreen).setOnClickListener { currentColor = Color.parseColor("#4CAF50"); regenerate() }
        findViewById<Button>(R.id.btnColorPurple).setOnClickListener { currentColor = Color.parseColor("#9C27B0"); regenerate() }

        // Tombol background
        findViewById<Button>(R.id.btnBgWhite).setOnClickListener { currentBg = Color.WHITE; regenerate() }
        findViewById<Button>(R.id.btnBgLight).setOnClickListener { currentBg = Color.parseColor("#F5F5F5"); regenerate() }
        findViewById<Button>(R.id.btnBgYellow).setOnClickListener { currentBg = Color.parseColor("#FFF9C4"); regenerate() }

        // Tombol frame
        findViewById<Button>(R.id.btnFrameNone).setOnClickListener { currentFrame = "none"; regenerate() }
        findViewById<Button>(R.id.btnFrameBlue).setOnClickListener { currentFrame = "blue"; regenerate() }
        findViewById<Button>(R.id.btnFrameRed).setOnClickListener { currentFrame = "red"; regenerate() }
        findViewById<Button>(R.id.btnFrameGreen).setOnClickListener { currentFrame = "green"; regenerate() }
        findViewById<Button>(R.id.btnFramePurple).setOnClickListener { currentFrame = "purple"; regenerate() }
        findViewById<Button>(R.id.btnFrameGold).setOnClickListener { currentFrame = "gold"; regenerate() }

        // Tombol template
        findViewById<Button>(R.id.btnTplNone).setOnClickListener { currentTemplate = "none"; regenerate() }
        findViewById<Button>(R.id.btnTplDots).setOnClickListener { currentTemplate = "dots"; regenerate() }
        findViewById<Button>(R.id.btnTplStars).setOnClickListener { currentTemplate = "stars"; regenerate() }
        findViewById<Button>(R.id.btnTplHearts).setOnClickListener { currentTemplate = "hearts"; regenerate() }
        findViewById<Button>(R.id.btnTplFlowers).setOnClickListener { currentTemplate = "flowers"; regenerate() }
        findViewById<Button>(R.id.btnTplCircle).setOnClickListener { currentTemplate = "circle"; regenerate() }

        // Toggle logo
        findViewById<Switch>(R.id.swLogo).setOnCheckedChangeListener { _, isChecked ->
            useLogo = isChecked
            regenerate()
        }

        // Tombol aksi
        findViewById<Button>(R.id.btnGenerate).setOnClickListener { regenerate() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { qrBitmap?.let { saveToGallery(it) } }
        findViewById<Button>(R.id.btnShare).setOnClickListener { qrBitmap?.let { shareImage(it) } }
    }

    private fun regenerate() {
        val text = etInput.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "Masukkan teks dulu", Toast.LENGTH_SHORT).show()
            return
        }
        labelText = etLabel.text.toString().trim()
        qrBitmap = buildFinalQR(text)
        ivQR.setImageBitmap(qrBitmap)
    }

    /**
     * Build QR + frame + template + label
     */
    private fun buildFinalQR(text: String): Bitmap {
        val qrSize = 800
        val padding = 100          // ruang untuk frame
        val labelHeight = if (labelText.isNotEmpty()) 160 else 0
        val totalSize = qrSize + padding * 2
        val totalHeight = totalSize + labelHeight

        // 1. Buat QR dasar
        val qr = generateQR(text, qrSize, currentColor, currentBg, useLogo)

        // 2. Buat canvas besar
        val result = Bitmap.createBitmap(totalSize, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 3. Background canvas
        canvas.drawColor(Color.WHITE)

        // 4. Gambar frame (kalau ada)
        drawFrame(canvas, totalSize, totalSize, currentFrame)

        // 5. Gambar template di sekeliling (kalau ada)
        drawTemplate(canvas, totalSize, totalSize, currentTemplate)

        // 6. Gambar QR di tengah
        val qrLeft = padding.toFloat()
        val qrTop = padding.toFloat()
        canvas.drawBitmap(qr, qrLeft, qrTop, null)

        // 7. Gambar label text di bawah (kalau ada)
        if (labelText.isNotEmpty()) {
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 80f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            val textY = totalSize + 100f
            canvas.drawText(labelText, totalSize / 2f, textY, textPaint)
        }

        return result
    }

    /**
     * QR dasar (warna + logo)
     */
    private fun generateQR(
        text: String,
        size: Int,
        fgColor: Int,
        bgColor: Int,
        withLogo: Boolean
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1
        )
        val writer = QRCodeWriter()
        val matrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints)

        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) fgColor else bgColor)
            }
        }
        return if (withLogo) addLogoCenter(bmp) else bmp
    }

    /**
     * Logo di tengah QR
     */
    private fun addLogoCenter(qr: Bitmap): Bitmap {
        val result = qr.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val size = result.width
        val logoSize = size / 5
        val cx = size / 2f
        val cy = size / 2f

        val bgPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }
        canvas.drawCircle(cx, cy, logoSize / 2f + 12f, bgPaint)

        val logoPaint = Paint().apply {
            color = Color.parseColor("#2196F3")
            isAntiAlias = true
        }
        canvas.drawCircle(cx, cy, logoSize / 2f, logoPaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = logoSize / 2f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText("QR", cx, textY, textPaint)

        return result
    }

    /**
     * Gambar frame (border berwarna)
     */
    private fun drawFrame(canvas: Canvas, w: Int, h: Int, frame: String) {
        val color = when (frame) {
            "blue"   -> Color.parseColor("#2196F3")
            "red"    -> Color.parseColor("#F44336")
            "green"  -> Color.parseColor("#4CAF50")
            "purple" -> Color.parseColor("#9C27B0")
            "gold"   -> Color.parseColor("#FFC107")
            else     -> return
        }

        val paint = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 30f
            isAntiAlias = true
        }

        val margin = 30f
        val rect = RectF(margin, margin, w - margin, h - margin)
        canvas.drawRoundRect(rect, 40f, 40f, paint)

        // Frame ganda (inner)
        val innerRect = RectF(margin + 20f, margin + 20f, w - margin - 20f, h - margin - 20f)
        val innerPaint = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        canvas.drawRoundRect(innerRect, 20f, 20f, innerPaint)
    }

    /**
     * Template dekorasi di sekeliling QR
     */
    private fun drawTemplate(canvas: Canvas, w: Int, h: Int, template: String) {
        when (template) {
            "dots"    -> drawDots(canvas, w, h)
            "stars"   -> drawStars(canvas, w, h)
            "hearts"  -> drawHearts(canvas, w, h)
            "flowers" -> drawFlowers(canvas, w, h)
            "circle"  -> drawCircleTemplate(canvas, w, h)
        }
    }

    private fun drawDots(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint().apply {
            color = Color.parseColor("#2196F3")
            isAntiAlias = true
        }
        val colors = listOf(
            Color.parseColor("#2196F3"),
            Color.parseColor("#F44336"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#FFC107"),
            Color.parseColor("#9C27B0")
        )
        val count = 20
        for (i in 0 until count) {
            paint.color = colors[i % colors.size]
            val angle = (2 * Math.PI * i / count).toFloat()
            val radius = (w / 2f) - 50f
            val x = (w / 2f) + radius * Math.cos(angle.toDouble()).toFloat()
            val y = (h / 2f) + radius * Math.sin(angle.toDouble()).toFloat()
            canvas.drawCircle(x, y, 15f, paint)
        }
    }

    private fun drawStars(canvas: Canvas, w: Int, h: Int) {
        val colors = listOf(
            Color.parseColor("#FFC107"),
            Color.parseColor("#FF5722"),
            Color.parseColor("#E91E63")
        )
        val count = 12
        for (i in 0 until count) {
            val paint = Paint().apply {
                color = colors[i % colors.size]
                isAntiAlias = true
            }
            val angle = (2 * Math.PI * i / count).toFloat()
            val radius = (w / 2f) - 50f
            val x = (w / 2f) + radius * Math.cos(angle.toDouble()).toFloat()
            val y = (h / 2f) + radius * Math.sin(angle.toDouble()).toFloat()
            drawStar(canvas, x, y, 30f, paint)
        }
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = android.graphics.Path()
        val points = 5
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) size else size / 2
            val angle = (Math.PI * i / points).toFloat() - (Math.PI / 2).toFloat()
            val x = cx + r * Math.cos(angle.toDouble()).toFloat()
            val y = cy + r * Math.sin(angle.toDouble()).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawHearts(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint().apply {
            color = Color.parseColor("#E91E63")
            isAntiAlias = true
        }
        val count = 10
        for (i in 0 until count) {
            val angle = (2 * Math.PI * i / count).toFloat()
            val radius = (w / 2f) - 60f
            val x = (w / 2f) + radius * Math.cos(angle.toDouble()).toFloat()
            val y = (h / 2f) + radius * Math.sin(angle.toDouble()).toFloat()
            drawHeart(canvas, x, y, 25f, paint)
        }
    }

    private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = android.graphics.Path()
        path.moveTo(cx, cy + size / 2)
        path.cubicTo(cx - size, cy - size / 2, cx - size / 2, cy - size, cx, cy - size / 2)
        path.cubicTo(cx + size / 2, cy - size, cx + size, cy - size / 2, cx, cy + size / 2)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawFlowers(canvas: Canvas, w: Int, h: Int) {
        val petalColor = Color.parseColor("#FFC107")
        val centerColor = Color.parseColor("#FF5722")
        val count = 8
        for (i in 0 until count) {
            val angle = (2 * Math.PI * i / count).toFloat()
            val radius = (w / 2f) - 55f
            val x = (w / 2f) + radius * Math.cos(angle.toDouble()).toFloat()
            val y = (h / 2f) + radius * Math.sin(angle.toDouble()).toFloat()
            drawFlower(canvas, x, y, 30f, petalColor, centerColor)
        }
    }

    private fun drawFlower(canvas: Canvas, cx: Float, cy: Float, size: Float, petalColor: Int, centerColor: Int) {
        val petalPaint = Paint().apply {
            color = petalColor
            isAntiAlias = true
        }
        val petals = 5
        for (i in 0 until petals) {
            val angle = (2 * Math.PI * i / petals).toFloat()
            val px = cx + (size / 2) * Math.cos(angle.toDouble()).toFloat()
            val py = cy + (size / 2) * Math.sin(angle.toDouble()).toFloat()
            canvas.drawCircle(px, py, size / 3, petalPaint)
        }
        val centerPaint = Paint().apply {
            color = centerColor
            isAntiAlias = true
        }
        canvas.drawCircle(cx, cy, size / 4, centerPaint)
    }

    private fun drawCircleTemplate(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint().apply {
            color = Color.parseColor("#2196F3")
            style = Paint.Style.STROKE
            strokeWidth = 8f
            isAntiAlias = true
        }
        canvas.drawCircle(w / 2f, h / 2f, (w / 2f) - 70f, paint)

        val paint2 = Paint().apply {
            color = Color.parseColor("#9C27B0")
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        canvas.drawCircle(w / 2f, h / 2f, (w / 2f) - 50f, paint2)
    }

    // =============== SAVE & SHARE ===============

    private fun saveToGallery(bitmap: Bitmap) {
        val name = "QR_${System.currentTimeMillis()}.png"
        val os: OutputStream?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/QRPro")
            }
            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            )
            os = uri?.let { contentResolver.openOutputStream(it) }
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "QRPro")
            if (!dir.exists()) dir.mkdirs()
            os = FileOutputStream(File(dir, name))
        }

        os?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        Toast.makeText(this, "Tersimpan di Galeri ✓", Toast.LENGTH_SHORT).show()
    }

    private fun shareImage(bitmap: Bitmap) {
        try {
            val dir = File(cacheDir, "qr")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "share_qr.png")
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            val uri = FileProvider.getUriForFile(
                this, "$packageName.fileprovider", file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Bagikan QR Code"))
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
