package com.example.qrpro

import android.content.ContentValues
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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

    // === State ===
    private var qrBitmap: Bitmap? = null
    private var logoBitmap: Bitmap? = null

    private var currentColor1 = Color.BLACK
    private var currentColor2 = Color.BLACK          // untuk gradient
    private var useGradient = false
    private var currentBg = Color.WHITE
    private var currentFrame = "none"
    private var currentTemplate = "none"
    private var labelText = ""
    private var labelColor = Color.BLACK
    private var labelSize = 80f
    private var qrSize = 800                          // 500, 800, 1000, 1500
    private var outputFormat = "PNG"                  // PNG atau JPG
    private var useLogo = false
    private var useCustomLogo = false

    // === Views ===
    private lateinit var ivQR: ImageView
    private lateinit var etInput: EditText
    private lateinit var etLabel: EditText

    // === Image Picker ===
    private val pickLogo = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                logoBitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                useCustomLogo = true
                Toast.makeText(this, "Logo dipilih ✓", Toast.LENGTH_SHORT).show()
                regenerate()
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal load logo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate)

        // Bind views
        etInput = findViewById(R.id.etInput)
        etLabel = findViewById(R.id.etLabel)
        ivQR = findViewById(R.id.ivQR)

        // === Warna QR ===
        findViewById<Button>(R.id.btnColorBlack).setOnClickListener { currentColor1 = Color.BLACK; currentColor2 = Color.BLACK; useGradient = false; regenerate() }
        findViewById<Button>(R.id.btnColorBlue).setOnClickListener { currentColor1 = Color.parseColor("#2196F3"); currentColor2 = Color.parseColor("#2196F3"); useGradient = false; regenerate() }
        findViewById<Button>(R.id.btnColorRed).setOnClickListener { currentColor1 = Color.parseColor("#F44336"); currentColor2 = Color.parseColor("#F44336"); useGradient = false; regenerate() }
        findViewById<Button>(R.id.btnColorGreen).setOnClickListener { currentColor1 = Color.parseColor("#4CAF50"); currentColor2 = Color.parseColor("#4CAF50"); useGradient = false; regenerate() }
        findViewById<Button>(R.id.btnColorPurple).setOnClickListener { currentColor1 = Color.parseColor("#9C27B0"); currentColor2 = Color.parseColor("#9C27B0"); useGradient = false; regenerate() }
        findViewById<Button>(R.id.btnGradient).setOnClickListener {
            currentColor1 = Color.parseColor("#2196F3")
            currentColor2 = Color.parseColor("#9C27B0")
            useGradient = true
            regenerate()
        }

        // === Background ===
        findViewById<Button>(R.id.btnBgWhite).setOnClickListener { currentBg = Color.WHITE; regenerate() }
        findViewById<Button>(R.id.btnBgLight).setOnClickListener { currentBg = Color.parseColor("#F5F5F5"); regenerate() }
        findViewById<Button>(R.id.btnBgYellow).setOnClickListener { currentBg = Color.parseColor("#FFF9C4"); regenerate() }
        findViewById<Button>(R.id.btnBgPink).setOnClickListener { currentBg = Color.parseColor("#FCE4EC"); regenerate() }
        findViewById<Button>(R.id.btnBgMint).setOnClickListener { currentBg = Color.parseColor("#E0F2F1"); regenerate() }

        // === Frame ===
        findViewById<Button>(R.id.btnFrameNone).setOnClickListener { currentFrame = "none"; regenerate() }
        findViewById<Button>(R.id.btnFrameBlue).setOnClickListener { currentFrame = "blue"; regenerate() }
        findViewById<Button>(R.id.btnFrameRed).setOnClickListener { currentFrame = "red"; regenerate() }
        findViewById<Button>(R.id.btnFrameGreen).setOnClickListener { currentFrame = "green"; regenerate() }
        findViewById<Button>(R.id.btnFramePurple).setOnClickListener { currentFrame = "purple"; regenerate() }
        findViewById<Button>(R.id.btnFrameGold).setOnClickListener { currentFrame = "gold"; regenerate() }

        // === Template ===
        findViewById<Button>(R.id.btnTplNone).setOnClickListener { currentTemplate = "none"; regenerate() }
        findViewById<Button>(R.id.btnTplDots).setOnClickListener { currentTemplate = "dots"; regenerate() }
        findViewById<Button>(R.id.btnTplStars).setOnClickListener { currentTemplate = "stars"; regenerate() }
        findViewById<Button>(R.id.btnTplHearts).setOnClickListener { currentTemplate = "hearts"; regenerate() }
        findViewById<Button>(R.id.btnTplFlowers).setOnClickListener { currentTemplate = "flowers"; regenerate() }
        findViewById<Button>(R.id.btnTplCircle).setOnClickListener { currentTemplate = "circle"; regenerate() }

        // === Size ===
        findViewById<Button>(R.id.btnSize500).setOnClickListener { qrSize = 500; regenerate() }
        findViewById<Button>(R.id.btnSize800).setOnClickListener { qrSize = 800; regenerate() }
        findViewById<Button>(R.id.btnSize1000).setOnClickListener { qrSize = 1000; regenerate() }
        findViewById<Button>(R.id.btnSize1500).setOnClickListener { qrSize = 1500; regenerate() }

        // === Format ===
        findViewById<Button>(R.id.btnFormatPng).setOnClickListener { outputFormat = "PNG"; Toast.makeText(this, "Format: PNG", Toast.LENGTH_SHORT).show() }
        findViewById<Button>(R.id.btnFormatJpg).setOnClickListener { outputFormat = "JPG"; Toast.makeText(this, "Format: JPG", Toast.LENGTH_SHORT).show() }

        // === Label color ===
        findViewById<Button>(R.id.btnLabelBlack).setOnClickListener { labelColor = Color.BLACK; regenerate() }
        findViewById<Button>(R.id.btnLabelBlue).setOnClickListener { labelColor = Color.parseColor("#2196F3"); regenerate() }
        findViewById<Button>(R.id.btnLabelRed).setOnClickListener { labelColor = Color.parseColor("#F44336"); regenerate() }
        findViewById<Button>(R.id.btnLabelPurple).setOnClickListener { labelColor = Color.parseColor("#9C27B0"); regenerate() }

        // === Logo ===
        findViewById<Switch>(R.id.swLogo).setOnCheckedChangeListener { _, isChecked ->
            useLogo = isChecked
            regenerate()
        }
        findViewById<Button>(R.id.btnPickLogo).setOnClickListener {
            pickLogo.launch(PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            ))
        }

        // === Aksi ===
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

    // ============================================================
    //                    BUILD QR FINAL
    // ============================================================
    private fun buildFinalQR(text: String): Bitmap {
        val padding = qrSize / 8
        val labelHeight = if (labelText.isNotEmpty()) (qrSize / 6) else 0
        val totalSize = qrSize + padding * 2
        val totalHeight = totalSize + labelHeight

        // 1. Generate QR
        val qr = generateQR(text, qrSize, currentColor1, currentColor2, currentBg, useGradient, useLogo, useCustomLogo)

        // 2. Canvas
        val result = Bitmap.createBitmap(totalSize, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        // 3. Frame
        drawFrame(canvas, totalSize, totalSize, currentFrame)

        // 4. Template
        drawTemplate(canvas, totalSize, totalSize, currentTemplate)

        // 5. QR di tengah
        canvas.drawBitmap(qr, padding.toFloat(), padding.toFloat(), null)

        // 6. Label text
        if (labelText.isNotEmpty()) {
            val textPaint = Paint().apply {
                color = labelColor
                textSize = labelSize
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
                typeface = Typeface.DEFAULT_BOLD
            }
            val textY = totalSize + labelHeight / 2f + labelSize / 3f
            canvas.drawText(labelText, totalSize / 2f, textY, textPaint)
        }

        return result
    }

    /**
     * Generate QR dengan gradient + custom logo
     */
    private fun generateQR(
        text: String,
        size: Int,
        color1: Int,
        color2: Int,
        bgColor: Int,
        gradient: Boolean,
        withLogo: Boolean,
        customLogo: Boolean
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1
        )
        val writer = QRCodeWriter()
        val matrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints)

        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Background
        canvas.drawColor(bgColor)

        // Foreground (QR modules)
        val fgPaint = Paint().apply {
            isAntiAlias = false
            if (gradient) {
                shader = LinearGradient(
                    0f, 0f, size.toFloat(), size.toFloat(),
                    color1, color2, Shader.TileMode.CLAMP
                )
            } else {
                color = color1
            }
        }

        // Gambar QR module
        for (x in 0 until size) {
            for (y in 0 until size) {
                if (matrix[x, y]) {
                    canvas.drawPoint(x.toFloat(), y.toFloat(), fgPaint)
                }
            }
        }

        // Logo
        if (withLogo) {
            val logo = if (customLogo && logoBitmap != null) logoBitmap!! else null
            addLogo(bmp, logo)
        }

        return bmp
    }

    /**
     * Tambah logo di tengah QR (dengan background putih)
     */
    private fun addLogo(qr: Bitmap, customLogo: Bitmap?) {
        val canvas = Canvas(qr)
        val size = qr.width
        val logoSize = size / 5
        val cx = size / 2f
        val cy = size / 2f

        // Background putih lingkaran
        val bgPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }
        canvas.drawCircle(cx, cy, logoSize / 2f + 12f, bgPaint)

        if (customLogo != null) {
            // Pakai logo custom
            val scaled = Bitmap.createScaledBitmap(customLogo, logoSize, logoSize, true)
            val left = (cx - logoSize / 2f).toInt()
            val top = (cy - logoSize / 2f).toInt()
            canvas.drawBitmap(scaled, left.toFloat(), top.toFloat(), null)
        } else {
            // Logo default: lingkaran biru + text "QR"
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
        }
    }

    // ============================================================
    //                    FRAME
    // ============================================================
    private fun drawFrame(canvas: Canvas, w: Int, h: Int, frame: String) {
        val color = when (frame) {
            "blue"   -> Color.parseColor("#2196F3")
            "red"    -> Color.parseColor("#F44336")
            "green"  -> Color.parseColor("#4CAF50")
            "purple" -> Color.parseColor("#9C27B0")
            "gold"   -> Color.parseColor("#FFC107")
            else     -> return
        }

        val margin = w * 0.03f
        val stroke = w * 0.025f

        val paint = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = stroke
            isAntiAlias = true
        }
        canvas.drawRoundRect(
            RectF(margin, margin, w - margin, h - margin),
            w * 0.04f, w * 0.04f, paint
        )

        val innerMargin = margin + stroke * 0.7f
        val innerPaint = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = stroke * 0.3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(
            RectF(innerMargin, innerMargin, w - innerMargin, h - innerMargin),
            w * 0.02f, w * 0.02f, innerPaint
        )
    }

    // ============================================================
    //                    TEMPLATE
    // ============================================================
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
        val colors = listOf(
            Color.parseColor("#2196F3"), Color.parseColor("#F44336"),
            Color.parseColor("#4CAF50"), Color.parseColor("#FFC107"),
            Color.parseColor("#9C27B0")
        )
        val count = 24
        val radius = (w / 2f) - (w * 0.06f)
        val dotSize = w * 0.018f

        for (i in 0 until count) {
            val paint = Paint().apply {
                color = colors[i % colors.size]
                isAntiAlias = true
            }
            val angle = (2 * Math.PI * i / count).toFloat()
            val x = (w / 2f) + radius * Math.cos(angle.toDouble()).toFloat()
            val y = (h / 2f) + radius * Math.sin(angle.toDouble()).toFloat()
            canvas.drawCircle(x, y, dotSize, paint)
        }
    }

    private fun drawStars(canvas: Canvas, w: Int, h: Int) {
        val colors = listOf(
            Color.parseColor("#FFC107"),
            Color.parseColor("#FF5722"),
            Color.parseColor("#E91E63")
        )
        val count = 12
        val radius = (w / 2f) - (w * 0.06f)
        val starSize = w * 0.04f

        for (i in 0 until count) {
            val paint = Paint().apply {
                color = colors[i % colors.size]
                isAntiAlias = true
            }
            val angle = (2 * Math.PI * i / count).toFloat()
            val x = (w / 2f) + radius * Math.cos(angle.toDouble()).toFloat()
            val y = (h / 2f) + radius * Math.sin(angle.toDouble()).toFloat()
            drawStar(canvas, x, y, starSize, paint)
        }
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = Path()
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
        val radius = (w / 2f) - (w * 0.07f)
        val heartSize = w * 0.04f

        for (i in 0 until count) {
            val angle = (2 * Math.PI * i / count).toFloat()
            val x = (w / 2f) + radius * Math.cos(angle.toDouble()).toFloat()
            val y = (h / 2f) + radius * Math.sin(angle.toDouble()).toFloat()
            drawHeart(canvas, x, y, heartSize, paint)
        }
    }

    private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = Path()
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
        val radius = (w / 2f) - (w * 0.07f)
        val flowerSize = w * 0.05f

        for (i in 0 until count) {
            val angle = (2 * Math.PI * i / count).toFloat()
            val x = (w / 2f) + radius * Math.cos(angle.toDouble()).toFloat()
            val y = (h / 2f) + radius * Math.sin(angle.toDouble()).toFloat()
            drawFlower(canvas, x, y, flowerSize, petalColor, centerColor)
        }
    }

    private fun drawFlower(canvas: Canvas, cx: Float, cy: Float, size: Float, petalColor: Int, centerColor: Int) {
        val petalPaint = Paint().apply {
            color = petalColor
            isAntiAlias = true
        }
        for (i in 0 until 5) {
            val angle = (2 * Math.PI * i / 5).toFloat()
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
            strokeWidth = w * 0.01f
            isAntiAlias = true
        }
        canvas.drawCircle(w / 2f, h / 2f, (w / 2f) - (w * 0.09f), paint)

        val paint2 = Paint().apply {
            color = Color.parseColor("#9C27B0")
            style = Paint.Style.STROKE
            strokeWidth = w * 0.005f
            isAntiAlias = true
        }
        canvas.drawCircle(w / 2f, h / 2f, (w / 2f) - (w * 0.065f), paint2)
    }

    // ============================================================
    //                    SAVE & SHARE
    // ============================================================
    private fun saveToGallery(bitmap: Bitmap) {
        val ext = if (outputFormat == "PNG") "png" else "jpg"
        val mime = if (outputFormat == "PNG") "image/png" else "image/jpeg"
        val name = "QR_${System.currentTimeMillis()}.$ext"
        val os: OutputStream?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
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

        val format = if (outputFormat == "PNG")
            Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val quality = if (outputFormat == "PNG") 100 else 95

        os?.use { bitmap.compress(format, quality, it) }
        Toast.makeText(this, "Tersimpan ($outputFormat) ✓", Toast.LENGTH_SHORT).show()
    }

    private fun shareImage(bitmap: Bitmap) {
        try {
            val dir = File(cacheDir, "qr")
            if (!dir.exists()) dir.mkdirs()
            val ext = if (outputFormat == "PNG") "png" else "jpg"
            val file = File(dir, "share_qr.$ext")
            val format = if (outputFormat == "PNG")
                Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

            FileOutputStream(file).use {
                bitmap.compress(format, 100, it)
            }

            val uri = FileProvider.getUriForFile(
                this, "$packageName.fileprovider", file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (outputFormat == "PNG") "image/png" else "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Bagikan QR Code"))
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
