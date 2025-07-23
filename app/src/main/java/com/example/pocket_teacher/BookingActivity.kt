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
    }


}
