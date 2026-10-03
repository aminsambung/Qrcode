package com.example.qrpro

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanActivity : AppCompatActivity() {

    // === Views ===
    private lateinit var previewView: PreviewView
    private lateinit var tvResult: TextView
    private lateinit var tvType: TextView
    private lateinit var tvHint: TextView
    private lateinit var resultCard: CardView
    private lateinit var btnAction: Button
    private lateinit var btnCopy: Button
    private lateinit var btnShare: Button
    private lateinit var btnFlash: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var btnGallery: ImageView

    // === Camera ===
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private var torchOn = false

    // === Scanner ===
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
    )

    // === State ===
    private var lastScanned = ""
    private var lastScanTime = 0L
    private var currentType = QrType.TEXT
    private var currentValue = ""

    // === Permission ===
    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else {
                Toast.makeText(this, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    // === Image Picker ===
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { processImageFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        // Bind views
        previewView = findViewById(R.id.previewView)
        tvResult = findViewById(R.id.tvResult)
        tvType = findViewById(R.id.tvType)
        tvHint = findViewById(R.id.tvHint)
        resultCard = findViewById(R.id.resultCard)
        btnAction = findViewById(R.id.btnAction)
        btnCopy = findViewById(R.id.btnCopy)
        btnShare = findViewById(R.id.btnShare)
        btnFlash = findViewById(R.id.btnFlash)
        btnBack = findViewById(R.id.btnBack)
        btnGallery = findViewById(R.id.btnGallery)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Aksi tombol
        btnBack.setOnClickListener { finish() }

        btnGallery.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            ))
        }

        btnFlash.setOnClickListener {
            camera?.let {
                torchOn = !torchOn
                it.cameraControl.enableTorch(torchOn)
                btnFlash.setColorFilter(
                    if (torchOn) Color.parseColor("#FFC107") else Color.WHITE
                )
            }
        }

        btnCopy.setOnClickListener { copyResult() }
        btnShare.setOnClickListener { shareResult() }
        btnAction.setOnClickListener { performAction() }

        // Permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) startCamera()
        else requestPermission.launch(Manifest.permission.CAMERA)
    }

    // ============================================================
    //                    CAMERA
    // ============================================================
    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val media = imageProxy.image
                if (media != null) {
                    val image = InputImage.fromMediaImage(
                        media, imageProxy.imageInfo.rotationDegrees
                    )
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            val barcode = barcodes.firstOrNull()
                            if (barcode != null) {
                                barcode.rawValue?.let { value ->
                                    handleResult(value, barcode.format)
                                }
                            }
                        }
                        .addOnCompleteListener { imageProxy.close() }
                } else {
                    imageProxy.close()
                }
            }

            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // ============================================================
    //                    HANDLE RESULT
    // ============================================================
    private fun handleResult(value: String, format: Int) {
        val now = System.currentTimeMillis()
        if (value == lastScanned && now - lastScanTime < 2000) return
        lastScanned = value
        lastScanTime = now

        // Detect QR type
        val type = detectType(value, format)
        currentType = type
        currentValue = value

        val formatName = when (format) {
            Barcode.FORMAT_QR_CODE -> "QR_CODE"
            Barcode.FORMAT_EAN_13 -> "EAN_13"
            Barcode.FORMAT_EAN_8 -> "EAN_8"
            Barcode.FORMAT_UPC_A -> "UPC_A"
            Barcode.FORMAT_UPC_E -> "UPC_E"
            Barcode.FORMAT_CODE_128 -> "CODE_128"
            Barcode.FORMAT_CODE_39 -> "CODE_39"
            else -> "UNKNOWN"
        }

        runOnUiThread {
            showResult(value, type, formatName)
        }
    }

    /**
     * Tampilkan hasil scan di card
     */
    private fun showResult(value: String, type: QrType, formatName: String) {
        resultCard.visibility = View.VISIBLE

        // Set warna card sesuai tipe
        val (typeLabel, typeColor) = when (type) {
            QrType.URL      -> "🌐 Link" to Color.parseColor("#2196F3")
            QrType.EMAIL    -> "📧 Email" to Color.parseColor("#9C27B0")
            QrType.PHONE    -> "📱 Telepon" to Color.parseColor("#4CAF50")
            QrType.SMS      -> "💬 SMS" to Color.parseColor("#FF9800")
            QrType.WIFI     -> "📶 WiFi" to Color.parseColor("#00BCD4")
            QrType.LOCATION -> "📍 Lokasi" to Color.parseColor("#F44336")
            QrType.PRODUCT  -> "📦 Produk" to Color.parseColor("#795548")
            QrType.TEXT     -> "📝 Teks" to Color.parseColor("#607D8B")
        }

        tvType.text = typeLabel
        tvType.setTextColor(typeColor)
        tvResult.text = value

        // Hint & button text
        val (hint, buttonText, buttonVisible) = when (type) {
            QrType.URL      -> Triple("Tap 'Buka Link' untuk buka di browser", "🌐 Buka Link", true)
            QrType.EMAIL    -> Triple("Tap 'Kirim Email' untuk buka Gmail", "📧 Kirim Email", true)
            QrType.PHONE    -> Triple("Tap 'Telepon' untuk menelepon", "📱 Telepon", true)
            QrType.SMS      -> Triple("Tap 'Kirim SMS' untuk kirim pesan", "💬 Kirim SMS", true)
            QrType.WIFI     -> Triple("Info jaringan WiFi terdeteksi", "📶 Info WiFi", true)
            QrType.LOCATION -> Triple("Tap 'Buka Maps' untuk lihat lokasi", "📍 Buka Maps", true)
            QrType.PRODUCT  -> Triple("Tap 'Cari Produk' untuk info lengkap", "📦 Cari Produk", true)
            QrType.TEXT     -> Triple("Tap 'Copy' untuk salin teks", "📋 Copy Teks", false)
        }

        tvHint.text = hint
        btnAction.text = buttonText
        btnAction.visibility = if (buttonVisible) View.VISIBLE else View.GONE
    }

    // ============================================================
    //                    DETECT TYPE
    // ============================================================
    private enum class QrType { URL, EMAIL, PHONE, SMS, WIFI, LOCATION, PRODUCT, TEXT }

    private fun detectType(value: String, format: Int): QrType {
        val lower = value.lowercase()

        // Barcode produk
        if (format in listOf(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
        ) return QrType.PRODUCT

        // URL
        if (lower.startsWith("http://") || lower.startsWith("https://") ||
            lower.startsWith("www.")) return QrType.URL

        // Email
        if (lower.startsWith("mailto:") ||
            (lower.contains("@") && lower.contains(".", lower.indexOf("@")) &&
                    !lower.contains(" "))) return QrType.EMAIL

        // Telepon
        if (lower.startsWith("tel:") || lower.startsWith("phone:")) return QrType.PHONE

        // SMS
        if (lower.startsWith("sms:") || lower.startsWith("smsto:")) return QrType.SMS

        // WiFi
        if (lower.startsWith("wifi:") || lower.startsWith("wifi:")) return QrType.WIFI

        // Lokasi
        if (lower.startsWith("geo:") ||
            lower.contains("maps.google.com") ||
            lower.contains("goo.gl/maps") ||
            lower.contains("google.com/maps")) return QrType.LOCATION

        return QrType.TEXT
    }

    // ============================================================
    //                    AKSI
    // ============================================================
    private fun performAction() {
        try {
            when (currentType) {
                QrType.URL -> {
                    val url = if (currentValue.startsWith("http")) currentValue
                    else "https://$currentValue"
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                QrType.EMAIL -> {
                    val email = currentValue
                        .replace("mailto:", "")
                        .substringBefore("?")
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$email")
                    }
                    startActivity(intent)
                }
                QrType.PHONE -> {
                    val phone = currentValue
                        .replace("tel:", "")
                        .replace("phone:", "")
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                }
                QrType.SMS -> {
                    val phone = currentValue
                        .replace("sms:", "")
                        .replace("smsto:", "")
                        .substringBefore("?")
                        .substringBefore(":")
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:$phone")))
                }
                QrType.WIFI -> {
                    showWifiInfo()
                }
                QrType.LOCATION -> {
                    var url = currentValue
                    if (url.startsWith("geo:")) {
                        val coords = url.replace("geo:", "").substringBefore("?")
                        url = "https://maps.google.com/?q=$coords"
                    }
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                QrType.PRODUCT -> {
                    searchProduct()
                }
                QrType.TEXT -> {
                    copyResult()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak bisa buka: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showWifiInfo() {
        try {
            val parts = currentValue.replace("WIFI:", "").split(";")
            var ssid = ""
            var pass = ""
            var type = ""
            for (part in parts) {
                when {
                    part.startsWith("S:") -> ssid = part.substring(2)
                    part.startsWith("P:") -> pass = part.substring(2)
                    part.startsWith("T:") -> type = part.substring(2)
                }
            }
            val msg = """
                📶 Info WiFi
                
                SSID: $ssid
                Password: $pass
                Tipe: $type
            """.trimIndent()

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("WiFi Terdeteksi")
                .setMessage(msg)
                .setPositiveButton("Copy Info") { _, _ ->
                    val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cb.setPrimaryClip(ClipData.newPlainText("WiFi", msg))
                    Toast.makeText(this, "Info WiFi tersalin", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Tutup", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal baca WiFi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchProduct() {
        val url = "https://www.google.com/search?q=barcode+$currentValue"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal buka browser", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyResult() {
        if (currentValue.isEmpty()) return
        val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("QR Result", currentValue))
        Toast.makeText(this, "Tersalin ✓", Toast.LENGTH_SHORT).show()
    }

    private fun shareResult() {
        if (currentValue.isEmpty()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, currentValue)
        }
        startActivity(Intent.createChooser(intent, "Bagikan"))
    }

    // ============================================================
    //                    SCAN FROM GALLERY
    // ============================================================
    private fun processImageFromUri(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, uri)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val barcode = barcodes.firstOrNull()
                    if (barcode == null) {
                        Toast.makeText(this, "Tidak ada QR di gambar",
                            Toast.LENGTH_SHORT).show()
                    } else {
                        barcode.rawValue?.let { value ->
                            handleResult(value, barcode.format)
                            Toast.makeText(this, "Berhasil scan ✓",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal scan gambar", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        scanner.close()
    }
}
