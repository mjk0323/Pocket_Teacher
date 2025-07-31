package com.example.pocket_teacher

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

class BaseballTicketActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_baseball_ticket)

        checkOverlayPermission()

        // 회원가입 버튼 클릭 시 웹페이지 열기 + 오버레이 실행
        val registerButton = findViewById<LinearLayout>(R.id.button_register)
        registerButton.setOnClickListener {
            launchWebWithOverlay(
                url = "https://m.ticketlink.co.kr/my",
                overlayService = BaseballOverlayService::class.java
            )
        }

        // 회원 예매 버튼 클릭 시 웹페이지 열기 + 오버레이 실행
        val loginButton = findViewById<LinearLayout>(R.id.button_login)
        loginButton.setOnClickListener {
            launchWebWithOverlay(
                url = "https://www.ticketlink.co.kr/sports",
                overlayService = BaseballOverlayService::class.java
            )
        }

        // 예매 내역 조회 버튼 클릭 시 웹페이지 열기 + 오버레이 실행
        val checkBookingButton = findViewById<LinearLayout>(R.id.button_check_booking)
        checkBookingButton.setOnClickListener {
            launchWebWithOverlay(
                url = "https://www.ticketlink.co.kr/my/reserve/gate/list?page=1&productClass=ALL" +
                        "&searchType=PERIOD&period=MONTH_1&targetDay=RESERVE&year=&month=&state=PARTIALLY_COMPLETE",
                overlayService = BaseballOverlayService::class.java
            )
        }

        // 뒤로가기 버튼 클릭 시 이전 화면으로 돌아감
        val backButton = findViewById<LinearLayout>(R.id.button_back)
        backButton.setOnClickListener {
            finish()
        }
    }

    // 공통 함수: 웹 URL 열고 오버레이 실행
    private fun launchWebWithOverlay(url: String, overlayService: Class<*>) {
        if (!Settings.canDrawOverlays(this)) {
            checkOverlayPermission()
            return
        }

        val overlayIntent = Intent(this, overlayService)
        startService(overlayIntent)

        val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(browserIntent)
    }

    // 권한 체크 함수
    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("권한 필요")
                .setMessage("기능 사용을 위해 '다른 앱 위에 표시' 권한이 필요합니다.")
                .setPositiveButton("설정하기") { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }
}
