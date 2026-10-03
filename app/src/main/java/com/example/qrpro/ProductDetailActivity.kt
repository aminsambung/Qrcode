package com.example.qrpro

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProductDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        // Ambil data dari Intent (kalau ada)
        val content = intent.getStringExtra("content") ?: "Tidak ada data"

        // Tampilkan ke TextView
        val tvDetail = findViewById<TextView>(R.id.tvDetail)
        tvDetail?.text = content
    }
}
