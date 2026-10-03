package com.example.qrpro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qrpro.db.AppDatabase
import com.example.qrpro.db.ScanHistory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private val db by lazy { AppDatabase.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnClear).setOnClickListener {
            lifecycleScope.launch { db.scanDao().clearAll() }
        }

        val rv = findViewById<RecyclerView>(R.id.rvHistory)
        rv.layoutManager = LinearLayoutManager(this)
        val adapter = HistoryAdapter()
        rv.adapter = adapter

        lifecycleScope.launch {
            db.scanDao().getAll().collectLatest { adapter.submit(it) }
        }
    }

    inner class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.VH>() {
        private var items: List<ScanHistory> = emptyList()
        fun submit(list: List<ScanHistory>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val ivIcon: ImageView = v.findViewById(R.id.ivIcon)
            val tvContent: TextView = v.findViewById(R.id.tvContent)
            val tvMeta: TextView = v.findViewById(R.id.tvMeta)

            fun bind(item: ScanHistory) {
                val display = item.productName ?: item.content
                tvContent.text = display
                val date = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                    .format(Date(item.timestamp))
                tvMeta.text = "${item.type} • ${item.format} • $date"
                ivIcon.setImageResource(
                    if (item.type == "GENERATE") android.R.drawable.ic_menu_edit
                    else android.R.drawable.ic_menu_camera
                )
                ivIcon.setColorFilter(
                    if (item.type == "GENERATE") 0xFF4CAF50.toInt()
                    else 0xFF2196F3.toInt()
                )
            }
        }
    }
}