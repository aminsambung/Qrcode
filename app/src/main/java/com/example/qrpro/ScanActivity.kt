package com.example.qrpro

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.qrpro.api.ApiClient
import com.example.qrpro.db.AppDatabase
import com.example.qrpro.db.ScanHistory
import com.example.qrpro.utils.OverlayView
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var tvResult: android.widget.TextView
    private lateinit var tvFormat: android.widget.TextView
    private lateinit var resultCard: View
    private lateinit var btnOpen: android.widget.Button
    private lateinit var btnFlash: android.widget.ImageView

    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private var torchOn = false
    private var lastScanned = ""
    private var lastScanTime = 0L
    private val db by lazy { AppDatabase.getInstance(this) }

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
    )

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else {
                Toast.makeText(this, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { processImageFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        tvResult = findViewById(R.id.tvResult)
        tvFormat = findViewById(R.id.tvFormat)
        resultCard = findViewById(R.id.resultCard)
        btnOpen = findViewById(R.id.btnOpen)
        btnFlash = findViewById(R.id.btnFlash)
        cameraExecutor = Executors.newSingleThreadExecutor()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnGallery).setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            ))
        }
        findViewById<View>(R.id.btnCopy).setOnClickListener {
            if (lastScanned.isNotEmpty()) {
                val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("QR", lastScanned))
                Toast.makeText(this, "Tersalin ✓", Toast.LENGTH_SHORT).show()
            }
        }
        btnOpen.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(lastScanned)))
            } catch (e: Exception) {
                Toast.makeText(this, "Tidak bisa buka link", Toast.LENGTH_SHORT).show()
            }
        }
        btnFlash.setOnClickListener {
            camera?.let {
                torchOn = !torchOn
                it.cameraControl.enableTorch(torchOn)
                btnFlash.setColorFilter(
                    if (torchOn) 0xFFFFEB3B.toInt() else 0xFFFFFFFF.toInt()
                )
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) startCamera()
        else requestPermission.launch(Manifest.permission.CAMERA)
    }

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
                val media = imageProxy.image ?: run {
                    imageProxy.close(); return@setAnalyzer
                }
                val image = InputImage.fromMediaImage(
                    media, imageProxy.imageInfo.rotationDegrees
                )
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        val barcode = barcodes.firstOrNull() ?: run {
                            overlayView.setBarcodeRect(null)
                            return@addOnSuccessListener
                        }
                        barcode.rawValue?.let { value ->
                            // Bounding box
                            barcode.boundingBox?.let { box ->
                                val scaleX = previewView.width.toFloat() / imageProxy.width
                                val scaleY = previewView.height.toFloat() / imageProxy.height
                                val rect = RectF(
                                    box.left * scaleX,
                                    box.top * scaleY,
                                    box.right * scaleX,
                                    box.bottom * scaleY
                                )
                                overlayView.setBarcodeRect(rect)
                            }
                            handleResult(value, barcode.format)
                        }
                    }
                    .addOnCompleteListener { imageProxy.close() }
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

    private fun handleResult(value: String, format: Int) {
        val now = System.currentTimeMillis()
        if (value == lastScanned && now - lastScanTime < 2000) return
        lastScanned = value
        lastScanTime = now

        val formatName = when (format) {
            Barcode.FORMAT_QR_CODE -> "QR_CODE"
            Barcode.FORMAT_EAN_13 -> "EAN_13"
            Barcode.FORMAT_EAN_8 -> "EAN_8"
            Barcode.FORMAT_UPC_A -> "UPC_A"
            Barcode.FORMAT_CODE_128 -> "CODE_128"
            Barcode.FORMAT_CODE_39 -> "CODE_39"
            Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
            Barcode.FORMAT_PDF417 -> "PDF417"
            else -> "UNKNOWN"
        }

        runOnUiThread {
            resultCard.visibility = View.VISIBLE
            tvFormat.text = formatName
            tvResult.text = value
            btnOpen.visibility = if (value.startsWith("http")) View.VISIBLE else View.GONE
        }

        // Cek produk jika EAN/UPC
        val isProduct = format in listOf(
            Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A
        )

        lifecycleScope.launch {
            val existing = db.scanDao().findByContent(value)
            if (existing == null) {
                db.scanDao().insert(
                    ScanHistory(
                        content = value,
                        format = formatName,
                        type = "SCAN"
                    )
                )
            }

            if (isProduct) {
                try {
                    val response = ApiClient.productApi.getProduct(value)
                    if (response.status == 1 && response.product != null) {
                        val p = response.product
                        val name = p.productName ?: p.brands ?: "Produk tidak dikenal"
                        // Update DB
                        db.scanDao().findByContent(value)?.let { old ->
                            db.scanDao().delete(old)
                            db.scanDao().insert(old.copy(
                                productName = name,
                                productImage = p.imageUrl
                            ))
                        }
                        // Tampilkan info produk
                        runOnUiThread {
                            tvResult.text = "$value\n\n📦 $name"
                        }
                    }
                } catch (e: Exception) {
                    // Silent fail — tidak semua barcode ada di DB
                }
            }
        }
    }

    private fun processImageFromUri(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, uri)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val barcode = barcodes.firstOrNull()
                    if (barcode == null) {
                        Toast.makeText(this, "Tidak ada barcode di gambar",
                            Toast.LENGTH_SHORT).show()
                    } else {
                        barcode.rawValue?.let { value ->
                            handleResult(value, barcode.format)
                            Toast.makeText(this, "Berhasil scan dari galeri ✓",
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
