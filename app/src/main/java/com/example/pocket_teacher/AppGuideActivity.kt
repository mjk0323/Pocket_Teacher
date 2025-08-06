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

class AppGuideActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_guide)

        checkOverlayPermission()
        if (!AccessibilityHelper.isAccessibilityServiceEnabled(this)) {
            // 다이얼로그로 안내 후 설정으로 이동
            AlertDialog.Builder(this)
                .setTitle("접근성 권한 필요")
                .setMessage("카카오톡 버튼 정보를 수집하려면 접근성 서비스를 켜야 합니다.")
                .setPositiveButton("설정으로 이동") { _, _ ->
                    AccessibilityHelper.openAccessibilitySettings(this)
                }
                .setNegativeButton("취소", null)
                .show()
        } else {
            // 이미 켜져있으면 정상 진행
            Log.d("ACCESS", "접근성 서비스 활성화됨!")
        }

        val kakaoPackage = "com.kakao.talk" // 카톡 앱 패키지 주소
        val intentKakao = packageManager.getLaunchIntentForPackage(kakaoPackage)
        val kakaoButton = findViewById<LinearLayout>(R.id.button_kakao)
        kakaoButton.setOnClickListener {
            // 카카오톡 설치 여부 체크
            if (intentKakao == null) {
                // 카카오톡이 설치되지 않은 경우 플레이스토어로 이동
                val playStoreIntent = Intent(Intent.ACTION_VIEW,
                    "market://details?id=$kakaoPackage".toUri())
                try {
                    startActivity(playStoreIntent)
                } catch (e: Exception) {
                    // 플레이스토어 앱이 없으면 웹으로
                    val webIntent = Intent(Intent.ACTION_VIEW,
                        "https://play.google.com/store/apps/details?id=$kakaoPackage".toUri())
                    startActivity(webIntent)
                }
                return@setOnClickListener
            }
            if (!Settings.canDrawOverlays(this)) {
                checkOverlayPermission()
                return@setOnClickListener   // 권한 없으면 중지
            }
            val overlayIntent = Intent(this, KakaoOverlayService::class.java)
            overlayIntent.putExtra("target_package", kakaoPackage)
            startActivity(intentKakao)  // 카톡 실행
            startService(overlayIntent) // 오버레이 실행
        }

        val youtubePackage = "com.google.android.youtube" // 유튜브 앱 패키지 주소
        val intentYoutube = packageManager.getLaunchIntentForPackage(youtubePackage)
        val youtubeButton = findViewById<LinearLayout>(R.id.button_youtube)
        youtubeButton.setOnClickListener {
            // 유튜브 설치 여부 체크
            if (intentYoutube == null) {
                // 유튜브가 설치되지 않은 경우 플레이스토어로 이동
                val playStoreIntent = Intent(Intent.ACTION_VIEW,
                    "market://details?id=$youtubePackage".toUri())
                try {
                    startActivity(playStoreIntent)
                } catch (e: Exception) {
                    // 플레이스토어 앱이 없으면 웹으로
                    val webIntent = Intent(Intent.ACTION_VIEW,
                        "https://play.google.com/store/apps/details?id=$youtubePackage".toUri())
                    startActivity(webIntent)
                }
                return@setOnClickListener
            }
            // 오버레이 권한 확인
            if (!Settings.canDrawOverlays(this)) {
                checkOverlayPermission()
                return@setOnClickListener   // 권한 없으면 중지
            }

            // 앱, 오버레이 실행
            val overlayIntent = Intent(this, YoutubeOverlayService::class.java)
            overlayIntent.putExtra("target_package", youtubePackage)
            startActivity(intentYoutube)  // 유튜브 실행
            startService(overlayIntent) // 오버레이 실행
        }

        val backButton = findViewById<LinearLayout>(R.id.button_back)
        backButton.setOnClickListener {
            finish() // 이전 화면으로 돌아감
        }
    }

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
