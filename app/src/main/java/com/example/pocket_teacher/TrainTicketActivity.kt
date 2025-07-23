package com.example.pocket_teacher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

class TrainTicketActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_train_ticket)

        val registerButton = findViewById<LinearLayout>(R.id.button_register)
        registerButton.setOnClickListener {
            val url = "https://www.korail.com/ticket/membership/ageCheck"   // 회원가입
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }

        val loginButton = findViewById<LinearLayout>(R.id.button_login)
        loginButton.setOnClickListener {
            val url = "https://www.korail.com/ticket/search/general#"       // 회원 예매
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }

        val nonMemberButton = findViewById<LinearLayout>(R.id.button_non_member)
        nonMemberButton.setOnClickListener {
            val url = "https://www.korail.com/ticket/search/general"        // 비회원 예매
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }

        val checkBookingButton = findViewById<LinearLayout>(R.id.button_check_booking)
        checkBookingButton.setOnClickListener {
            val url = "https://www.korail.com/ticket/login?hiddenTabNms=" +
                    "nonMember&redirectUrl=/ticket/myticket/list"           // 예매 내역 조회
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }

        val backButton = findViewById<LinearLayout>(R.id.button_back)
        backButton.setOnClickListener {
            finish() // 이전 화면으로 돌아감
        }
    }
}
