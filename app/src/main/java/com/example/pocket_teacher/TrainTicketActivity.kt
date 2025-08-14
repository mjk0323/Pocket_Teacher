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

class TrainTicketActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_train_ticket)

        checkOverlayPermission()
        if (!AccessibilityHelper.isAccessibilityServiceEnabled(this)) {
            // 다이얼로그로 안내 후 설정으로 이동
            AlertDialog.Builder(this)
                .setTitle("접근성 권한 필요")
                .setMessage("예매 페이지 버튼 정보를 수집하려면 접근성 서비스를 켜야 합니다.")
                .setPositiveButton("설정으로 이동") { _, _ ->
                    AccessibilityHelper.openAccessibilitySettings(this)
                }
                .setNegativeButton("취소", null)
                .show()
        }

        // 각 버튼에 대해 웹페이지 + 오버레이 실행
        val registerButton = findViewById<LinearLayout>(R.id.button_register)
        registerButton.setOnClickListener {
            launchWebWithOverlay(
                url = "https://www.korail.com/ticket/membership/ageCheck",
                overlayService = TrainOverlayService::class.java
            )
        }

        val loginButton = findViewById<LinearLayout>(R.id.button_login)
        loginButton.setOnClickListener {
            launchWebWithOverlay(
                url = "https://www.korail.com/ticket/search/general#",
                overlayService = TrainOverlayService::class.java
            )
        }

        val nonMemberButton = findViewById<LinearLayout>(R.id.button_non_member)
        nonMemberButton.setOnClickListener {
            launchWebWithOverlay(
                url = "https://www.korail.com/ticket/search/general",
                overlayService = TrainOverlayService::class.java
            )
        }

        val checkBookingButton = findViewById<LinearLayout>(R.id.button_check_booking)
        checkBookingButton.setOnClickListener {
            launchWebWithOverlay(
                url = "https://www.korail.com/ticket/login?hiddenTabNms=nonMember&redirectUrl=/ticket/myticket/list",
                overlayService = TrainOverlayService::class.java
            )
        }

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
        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("권한 필요")
                .setMessage("오버레이 기능을 사용하려면 다른 앱 위에 표시 권한이 필요합니다.")
                .setPositiveButton("설정하기") { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:$packageName".toUri()
                    )
                    startActivity(intent)
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }
}
