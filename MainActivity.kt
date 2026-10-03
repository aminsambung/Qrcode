package com.example.qrpro

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.qrpro.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.cardScan.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }
        b.cardGenerate.setOnClickListener {
            startActivity(Intent(this, GenerateActivity::class.java))
        }
        b.cardHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }
}