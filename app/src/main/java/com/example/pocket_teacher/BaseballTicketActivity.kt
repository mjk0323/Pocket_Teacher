package com.example.pocket_teacher

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class BaseballTicketActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_baseball_ticket)

        val registerButton = findViewById<LinearLayout>(R.id.button_register)
        registerButton.setOnClickListener {
            val url = "https://m.ticketlink.co.kr/my"   // 회원가입
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }

        val loginButton = findViewById<LinearLayout>(R.id.button_login)
        loginButton.setOnClickListener {
            val url = "https://www.ticketlink.co.kr/sports"       // 회원 예매
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }

        val checkBookingButton = findViewById<LinearLayout>(R.id.button_check_booking)
        checkBookingButton.setOnClickListener {
            val url = "https://www.ticketlink.co.kr/my/reserve/gate/list?page=1&productClass=" +
                    "ALL&searchType=PERIOD&period=MONTH_1&targetDay=RESERVE&year=" +
                    "&month=&state=PARTIALLY_COMPLETE"                          // 예매 내역 조회
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }

        val backButton = findViewById<LinearLayout>(R.id.button_back)
        backButton.setOnClickListener {
            finish() // 이전 화면으로 돌아감
        }
    }
}