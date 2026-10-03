package com.example.qrpro

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
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
import java.util.*

class GenerateActivity : AppCompatActivity() {

    private var qrBitmap: Bitmap? = null
    private lateinit var ivQR: ImageView
    private lateinit var etInput: EditText
    private var currentColor = Color.BLACK
    private var currentBg = Color.WHITE
    private var useLogo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate)

        etInput = findViewById(R.id.etInput)
        ivQR = findViewById(R.id.ivQR)
        val btnGenerate = findViewById<Button>(R.id.btnGenerate)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnShare = findViewById<Button>(R.id.btnShare)

        // Warna pilihan
        val btnBlack = findViewById<Button>(R.id.btnColorBlack)
        val btnBlue = findViewById<Button>(R.id.btnColorBlue)
        val btnRed = findViewById<Button>(R.id.btnColorRed)
        val btnGreen = findViewById<Button>(R.id.btnColorGreen)
        val btnPurple = findViewById<Button>(R.id.btnColorPurple)

        // Background pilihan
        val btnBgWhite = findViewById<Button>(R.id.btnBgWhite)
        val btnBgLight = findViewById<Button>(R.id.btnBgLight)
        val btnBgYellow = findViewById<Button>(R.id.btnBgYellow)

        // Toggle logo
        val swLogo = findViewById<Switch>(R.id.swLogo)

        btnBlack?.setOnClickListener { currentColor = Color.BLACK; regenerate() }
        btnBlue?.setOnClickListener { currentColor = Color.parseColor("#2196F3"); regenerate() }
        btnRed?.setOnClickListener { currentColor = Color.parseColor("#F44336"); regenerate() }
        btnGreen?.setOnClickListener { currentColor = Color.parseColor("#4CAF50"); regenerate() }
        btnPurple?.setOnClickListener { currentColor = Color.parseColor("#9C27B0"); regenerate() }

        btnBgWhite?.setOnClickListener { currentBg = Color.WHITE; regenerate() }
        btnBgLight?.setOnClickListener { currentBg = Color.parseColor("#F5F5F5"); regenerate() }
        btnBgYellow?.setOnClickListener { currentBg = Color.parseColor("#FFF9C4"); regenerate() }

        swLogo?.setOnCheckedChangeListener { _, isChecked ->
            useLogo = isChecked
            regenerate()
        }

        btnGenerate.setOnClickListener {
            regenerate()
        }

        btnSave.setOnClickListener {
            qrBitmap?.let { saveToGallery(it) }
        }

        btnShare.setOnClickListener {
            qrBitmap?.let { shareImage(it) }
        }
    }

    private fun regenerate() {
        val text = etInput.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "Masukkan teks dulu", Toast.LENGTH_SHORT).show()
            return
        }
        qrBitmap = generateQR(text, 800, currentColor, currentBg, useLogo)
        ivQR.setImageBitmap(qrBitmap)
    }

    /**
     * Generate QR Code dengan warna custom + logo (opsional)
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

        // Tambah logo di tengah kalau diaktifkan
        return if (withLogo) addLogoCenter(bmp) else bmp
    }

    /**
     * Tambah logo default di tengah QR
     */
    private fun addLogoCenter(qr: Bitmap): Bitmap {
        val result = qr.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val size = result.width
        val logoSize = size / 5  // logo 20% dari QR

        // Buat background putih lingkaran di tengah
        val paint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }
        val cx = size / 2f
        val cy = size / 2f
        canvas.drawCircle(cx, cy, logoSize / 2f + 10f, paint)

        // Buat logo sederhana (lingkaran warna)
        val logoPaint = Paint().apply {
            color = Color.parseColor("#2196F3")
            isAntiAlias = true
        }
        canvas.drawCircle(cx, cy, logoSize / 2f, logoPaint)

        // Tambah huruf "QR" di tengah
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = logoSize / 2f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText("QR", cx, textY, textPaint)

        return result
    }

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
