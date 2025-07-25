// BookingActivity.kt
package com.example.pocket_teacher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.LinearLayout

class BookingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        val trainTicketButton = findViewById<LinearLayout>(R.id.button_train_ticket)
        trainTicketButton.setOnClickListener {
            val intent = Intent(this, TrainTicketActivity::class.java)
            startActivity(intent)
        }

        val baseballTicketButton = findViewById<LinearLayout>(R.id.button_baseball_ticket)
        baseballTicketButton.setOnClickListener {
            val intent = Intent(this, BaseballTicketActivity::class.java)
            startActivity(intent)
        }

        val backButton = findViewById<LinearLayout>(R.id.button_back)
        backButton.setOnClickListener {
            finish() // 이전 화면으로 돌아감
        }
    }


}
