package com.example.pocket_teacher

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class InquiryHistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inquiryhistory)

        // ── 뒤로가기 모드: 공통 툴바의 왼쪽 아이콘을 뒤로가기 화살표로 세팅
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val leftBtn = toolbar.findViewById<ImageView>(R.id.btn_close)
        leftBtn.visibility = View.VISIBLE
        leftBtn.setImageResource(R.drawable.ic_arrow_back) // 화살표 아이콘 리소스
        leftBtn.contentDescription = "뒤로가기"
        leftBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 시스템 인셋 처리 (루트 id: inquiry_history_root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.inquiry_history_root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }
}
