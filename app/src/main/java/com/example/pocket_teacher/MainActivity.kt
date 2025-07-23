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

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 시스템 인셋 적용
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 앱 안내 버튼 클릭 시 AppGuideActivity로 이동
        val appGuideButton = findViewById<LinearLayout>(R.id.button_app_guide)
        appGuideButton.setOnClickListener {
            val intent = Intent(this, AppGuideActivity::class.java)
            startActivity(intent)
        }

        // 예매하기 버튼 클릭 시 BookingActivity로 이동
        val bookingButton = findViewById<LinearLayout>(R.id.button_booking)
        bookingButton.setOnClickListener {
            val intent = Intent(this, BookingActivity::class.java)
            startActivity(intent)
        }

        // 닫기 버튼 처리
        val closeButton = findViewById<ImageView>(R.id.btn_close)
        closeButton.visibility = View.VISIBLE

        closeButton.setOnClickListener {
            finish()
        }
    }
}
