package com.example.qrpro

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.qrpro.db.AppDatabase
import com.example.qrpro.db.ScanHistory
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class GenerateActivity : AppCompatActivity() {

    private var qrBitmap: Bitmap? = null
    private lateinit var ivQR: ImageView
    private lateinit var etInput: EditText
    private val db by lazy { AppDatabase.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate)

        ivQR = findViewById(R.id.ivQR)
        etInput = findViewById(R.id.etInput)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnShare = findViewById<Button>(R.id.btnShare)
        val btnGenerate = findViewById<Button>(R.id.btnGenerate)

        btnGenerate.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "Masukkan teks", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            qrBitmap = generateQR(text, 800)
            ivQR.setImageBitmap(qrBitmap)
            btnSave.visibility = Button.VISIBLE
            btnShare.visibility = Button.VISIBLE

            // Simpan ke history
            lifecycleScope.launch {
                db.scanDao().insert(ScanHistory(
                    content = text,
                    format = "QR_CODE",
                    type = "GENERATE"
                ))
            }
        }

        btnSave.setOnClickListener { qrBitmap?.let { saveToGallery(it) } }
        btnShare.setOnClickListener { qrBitmap?.let { shareImage(it) } }
    }

    private fun generateQR(text: String, size: Int): Bitmap {
        val writer = QRCodeWriter()
        val matrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
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
        val dir = File(cacheDir, "qr")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "share_qr.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Bagikan QR"))
    }
}
