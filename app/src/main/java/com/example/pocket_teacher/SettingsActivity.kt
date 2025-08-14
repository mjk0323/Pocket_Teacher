package com.example.pocket_teacher

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings) // ← 실제 XML이 맞는지 확인!

        // settings_root가 없으면 바로 알리고 리턴 (크래시 방지)
        val root: View? = findViewById(R.id.settings_root)
        if (root == null) {
            // 필요하면 Log.e(...)로 찍어도 됨
            return
        }

        // 시스템 인셋 적용
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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
