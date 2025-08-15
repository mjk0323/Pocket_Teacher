package com.example.pocket_teacher

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings) // XML 파일명 확인 필수

        val root: View? = findViewById(R.id.settings_root)
        if (root == null) {
            return
        }

        // 시스템 인셋 적용
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 🔹 뒤로가기 버튼 기능 추가
        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
            finish() // 현재 액티비티 종료 → 이전 화면으로 복귀
        }

        // 자주 묻는 질문
        findViewById<LinearLayout>(R.id.button_faq)?.setOnClickListener {
            startActivity(Intent(this, FAQActivity::class.java))
        }

        // 고객의 소리(문의하기)
        findViewById<LinearLayout>(R.id.button_inquiry)?.setOnClickListener {
            startActivity(Intent(this, InquiryActivity::class.java))
        }

        // 문의 내역 확인
        findViewById<LinearLayout>(R.id.button_inquiry_history)?.setOnClickListener {
            startActivity(Intent(this, InquiryHistoryActivity::class.java))
        }
    }
}
