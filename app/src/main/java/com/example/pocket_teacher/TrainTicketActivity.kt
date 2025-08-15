package com.example.pocket_teacher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

class TrainTicketActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TrainTicketActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_train_ticket)

        Log.d(TAG, "onCreate()")

        checkOverlayPermission()

        // 접근성 권한 확인
        val accEnabled = AccessibilityHelper.isAccessibilityServiceEnabled(this)
        Log.d(TAG, "접근성 서비스 활성화 여부: $accEnabled")
        if (!accEnabled) {
            AlertDialog.Builder(this)
                .setTitle("접근성 권한 필요")
                .setMessage("예매 페이지 버튼 정보를 수집하려면 접근성 서비스를 켜야 합니다.")
                .setPositiveButton("설정으로 이동") { _, _ ->
                    Log.d(TAG, "접근성 설정 화면으로 이동")
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
                overlayService = TrainOverlayService::class.java,
                source = "registerButton"
            )
        }

        val loginButton = findViewById<LinearLayout>(R.id.button_login)
        loginButton.setOnClickListener {
            launchWebWithOverlay(
                url = "https://www.korail.com/ticket/search/general#",
                overlayService = TrainOverlayService::class.java,
                source = "loginButton"
            )
        }

        val nonMemberButton = findViewById<LinearLayout>(R.id.button_non_member)
        nonMemberButton.setOnClickListener {
            launchWebWithOverlay(
                url = "https://www.korail.com/ticket/search/general",
                overlayService = TrainOverlayService::class.java,
                source = "nonMemberButton"
            )
        }

        val checkBookingButton = findViewById<LinearLayout>(R.id.button_check_booking)
        checkBookingButton.setOnClickListener {
            launchWebWithOverlay(
                url = "https://www.korail.com/ticket/login?hiddenTabNms=nonMember&redirectUrl=/ticket/myticket/list",
                overlayService = TrainOverlayService::class.java,
                source = "checkBookingButton"
            )
        }

        val backButton = findViewById<LinearLayout>(R.id.button_back)
        backButton.setOnClickListener {
            Log.d(TAG, "뒤로가기 클릭 -> finish()")
            finish()
        }
    }

    /**
     * 공통: 웹 URL 열고 오버레이 실행
     * - 여기서 서비스 호출 직전에 로그 찍어 서비스 실행 여부를 추적
     */
    private fun launchWebWithOverlay(url: String, overlayService: Class<*>, source: String) {
        Log.d(TAG, "launchWebWithOverlay() from=$source, url=$url")

        // 오버레이 권한 재확인
        val canOverlay = Settings.canDrawOverlays(this)
        Log.d(TAG, "Settings.canDrawOverlays=$canOverlay")
        if (!canOverlay) {
            Log.w(TAG, "오버레이 권한 없음 → 권한 요청 다이얼로그 표시")
            checkOverlayPermission()
            return
        }

        // 서비스 시작 (여기가 핵심 로그 포인트)
        try {
            val overlayIntent = Intent(this, overlayService).apply {
                // 서비스에서 디버깅할 때 누가 호출했는지 식별용
                putExtra("caller", TAG)
                putExtra("caller_source", source)
                // 필요하다면 추가 페이로드도 넣기
                // putStringArrayListExtra("button_info_list", ...)
            }
            Log.d("Launcher", "startService(TrainOverlayService) 호출: ${overlayService.name}, from=$source")
            startService(overlayIntent)
        } catch (t: Throwable) {
            Log.e(TAG, "startService() 중 예외 발생", t)
        }

        // 브라우저/크롬 커스텀탭 등으로 이동
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
            Log.d(TAG, "브라우저로 이동: $url")
            startActivity(browserIntent)
        } catch (t: Throwable) {
            Log.e(TAG, "브라우저 열기 실패: $url", t)
        }
    }

    // 권한 체크 + 안내
    private fun checkOverlayPermission() {
        val canOverlay = Settings.canDrawOverlays(this)
        Log.d(TAG, "checkOverlayPermission() canDrawOverlays=$canOverlay")
        if (!canOverlay) {
            AlertDialog.Builder(this)
                .setTitle("권한 필요")
                .setMessage("오버레이 기능을 사용하려면 '다른 앱 위에 표시' 권한이 필요합니다.")
                .setPositiveButton("설정하기") { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    Log.d(TAG, "오버레이 권한 설정 화면으로 이동")
                    startActivity(intent)
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }
}
