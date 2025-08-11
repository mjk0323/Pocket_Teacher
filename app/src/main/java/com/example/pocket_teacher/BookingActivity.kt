package com.example.pocket_teacher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.LinearLayout

// 전역 변수로 간단하게 처리
object ButtonState {
    var selectedService: String? = null
}

class BookingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        val trainTicketButton = findViewById<LinearLayout>(R.id.button_train_ticket)
        trainTicketButton.setOnClickListener {
            ButtonState.selectedService = "TRAIN"
            val intent = Intent(this, TrainTicketActivity::class.java)
            startActivity(intent)
        }

        val baseballTicketButton = findViewById<LinearLayout>(R.id.button_baseball_ticket)
        baseballTicketButton.setOnClickListener {
            ButtonState.selectedService = "BASEBALL"
            val intent = Intent(this, BaseballTicketActivity::class.java)
            startActivity(intent)
        }

        val backButton = findViewById<LinearLayout>(R.id.button_back)
        backButton.setOnClickListener {
            finish() // 이전 화면으로 돌아감
        }
    }
}
