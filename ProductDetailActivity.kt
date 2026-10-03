package com.example.qrpro

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.qrpro.api.ProductApi
import kotlinx.coroutines.launch

class ProductDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        val barcode = intent.getStringExtra("barcode") ?: return
        val ivImage = findViewById<ImageView>(R.id.ivImage)
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvBrand = findViewById<TextView>(R.id.tvBrand)
        val tvQty = findViewById<TextView>(R.id.tvQty)
        val tvIngredients = findViewById<TextView>(R.id.tvIngredients)
        val tvBarcode = findViewById<TextView>(R.id.tvBarcode)

        tvBarcode.text = "Barcode: $barcode"
        tvName.text = "Memuat..."
        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        lifecycleScope.launch {
            try {
                val res = ProductApi.create().getProduct(barcode)
                val p = res.product
                if (p == null) {
                    tvName.text = "Produk tidak ditemukan"
                    return@launch
                }
                tvName.text = p.name ?: "-"
                tvBrand.text = p.brand ?: "-"
                tvQty.text = p.quantity ?: "-"
                tvIngredients.text = p.ingredients ?: "-"
                ivImage.load(p.imageUrl) {
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_report_image)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProductDetailActivity,
                    "Gagal memuat: ${e.message}", Toast.LENGTH_SHORT).show()
                tvName.text = "Gagal memuat produk"
            }
        }
    }
}