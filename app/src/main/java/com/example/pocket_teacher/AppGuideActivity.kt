package com.example.pocket_teacher

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class AppGuideActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_guide)

        val kakaoPackage = "com.kakao.talk" // 카톡 앱 패키지 주소
        val intentKakao = packageManager.getLaunchIntentForPackage(kakaoPackage)
        val kakaoButton = findViewById<LinearLayout>(R.id.button_kakao)
        kakaoButton.setOnClickListener {
            startActivity(intentKakao)
        }

        val youtubePackage = "com.google.android.youtube" // 유튜브 앱 패키지 주소
        val intentYoutube = packageManager.getLaunchIntentForPackage(youtubePackage)
        val youtubeButton = findViewById<LinearLayout>(R.id.button_youtube)
        youtubeButton.setOnClickListener {
            startActivity(intentYoutube)
        }

        val backButton = findViewById<LinearLayout>(R.id.button_back)
        backButton.setOnClickListener {
            finish() // 이전 화면으로 돌아감
        }
    }
}

