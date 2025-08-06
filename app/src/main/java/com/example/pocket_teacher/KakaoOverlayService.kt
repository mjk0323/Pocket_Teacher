package com.example.pocket_teacher

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.RequiresApi

class KakaoOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val targetPackage = intent?.getStringExtra("target_package")
        val buttonInfoList = intent?.getStringArrayListExtra("button_info_list")

        if (targetPackage != null && buttonInfoList != null) {
            Log.e("OVERLAY_SERVICE", "========== 데이터 수신 ==========")
            Log.e("OVERLAY_SERVICE", "패키지: $targetPackage")
            Log.e("OVERLAY_SERVICE", "버튼 개수: ${buttonInfoList.size}개")

            buttonInfoList.forEachIndexed { index, buttonInfo ->
                val parts = buttonInfo.split("|")

                // 위치 정보
                val pos = parts[0].split(",")
                val left = pos[0].toInt()
                val top = pos[1].toInt()
                val right = pos[2].toInt()
                val bottom = pos[3].toInt()

                // 기타 정보들
                val isEnabled = parts[1].toBoolean()
                val size = parts[2]
                val text = parts[3].takeIf { it.isNotEmpty() }
                val description = parts[4].takeIf { it.isNotEmpty() }

                Log.e("OVERLAY_SERVICE", "버튼 $index:")
                Log.e("OVERLAY_SERVICE", "  - 위치: ($left,$top,$right,$bottom)")
                Log.e("OVERLAY_SERVICE", "  - 활성화: $isEnabled")
                Log.e("OVERLAY_SERVICE", "  - 크기: $size")
                Log.e("OVERLAY_SERVICE", "  - 텍스트: ${text ?: "없음"}")
                Log.e("OVERLAY_SERVICE", "  - 설명: ${description ?: "없음"}")
            }
            Log.e("OVERLAY_SERVICE", "================================")
        }

        showOverlay(buttonInfoList)

        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showOverlay(buttonInfoList: List<String>?) {
        if (buttonInfoList == null) return

        // 기존 오버레이가 있으면 제거
        if (overlayView != null) {
            windowManager.removeView(overlayView)
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // 오버레이 뷰 생성
        overlayView = LayoutInflater.from(this).inflate(R.layout.service_kakao_overlay, null)

        // 윈도우 매니저 파라미터 설정 - 터치 이벤트 완전히 통과시키기
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, // 전체 너비
            WindowManager.LayoutParams.MATCH_PARENT, // 전체 높이
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, // 터치 이벤트 완전히 통과
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

        // 오버레이 추가
        windowManager.addView(overlayView, params)

        // 버튼 정보에 따라 visibility 및 위치 업데이트
        updateOverlayVisibility(buttonInfoList)
    }

    private fun updateOverlayVisibility(buttonInfoList: List<String>) {
        overlayView?.let { view ->
            // 먼저 모든 컨테이너를 GONE으로 설정
            view.findViewById<LinearLayout>(R.id.search_container)?.visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.add_friend_container)?.visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.friend_container)?.visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.talk_music_container)?.visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.option_container)?.visibility = View.GONE

            view.findViewById<LinearLayout>(R.id.profile_container)?.visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.openchat_category_container)?.visibility = View.GONE

            view.findViewById<LinearLayout>(R.id.friend_container)?.visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.chat_container)?.visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.openchat_container)?.visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.shopping_container)?.visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.etc_container)?.visibility = View.GONE

            // 버튼 정보를 체크해서 해당하는 것들만 VISIBLE로 변경
            buttonInfoList.forEach { buttonInfo ->
                val parts = buttonInfo.split("|")
                val positions = parts[0].split(",")
                val left = positions[0].toInt()
                val top = positions[1].toInt()
                val right = positions[2].toInt()
                val bottom = positions[3].toInt()
                val centerX = (left + right) / 2
                val centerY = (top + bottom) / 2

                val text = parts[3].takeIf { it.isNotEmpty() } // 텍스트
                val description = parts[4].takeIf { it.isNotEmpty() } // 설명
                val combinedText = (description ?: "").lowercase()

                when {
                    // 상단 탭
                    combinedText.contains("검색") -> {
                        val container = view.findViewById<LinearLayout>(R.id.search_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            adjustContainerPosition(it, centerX, centerY + 100)
                        }
                    }
                    combinedText.contains("친구") && combinedText.contains("추가") -> {
                        val container = view.findViewById<LinearLayout>(R.id.add_friend_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            adjustContainerPosition(it, centerX, centerY) // 버튼 오른쪽 위
                        }
                    }
                    combinedText.contains("톡뮤직") -> {
                        val container = view.findViewById<LinearLayout>(R.id.talk_music_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            adjustContainerPosition(it, centerX, centerY)
                        }
                    }
                    combinedText.contains("옵션") && combinedText.contains("더보기") -> {
                        val container = view.findViewById<LinearLayout>(R.id.option_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            adjustContainerPosition(it, centerX, centerY)
                        }
                    }

                    // 오픈채팅 상단
                    combinedText.contains("지금") && combinedText.contains("뜨는") -> {
                        val container = view.findViewById<LinearLayout>(R.id.openchat_category_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            adjustContainerPosition(it, centerX, centerY)
                        }
                    }

                    // 하단 탭
                    combinedText.contains("친구") && combinedText.contains("탭") -> {
                        val container = view.findViewById<LinearLayout>(R.id.friend_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            adjustContainerPosition(it, centerX, centerY)
                        }
                    }
                    combinedText.contains("채팅 탭") && combinedText.contains("탭") -> {
                        val container = view.findViewById<LinearLayout>(R.id.chat_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            adjustContainerPosition(it, centerX, centerY)
                        }
                    }
                    combinedText.contains("Open") && combinedText.contains("Chat")
                            && combinedText.contains("Tab") -> {
                        val container = view.findViewById<LinearLayout>(R.id.openchat_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            adjustContainerPosition(it, centerX, centerY)
                        }
                    }
                    combinedText.contains("쇼핑 탭") -> {
                        val container = view.findViewById<LinearLayout>(R.id.shopping_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            adjustContainerPosition(it, centerX, centerY)
                        }
                    }
                    combinedText.contains("더보기 탭") -> {
                        val container = view.findViewById<LinearLayout>(R.id.etc_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            adjustContainerPosition(it, centerX, centerY)
                        }
                    }
                }
            }

            Log.e("OVERLAY", "오버레이 visibility 업데이트 완료")
        }
    }

    private fun adjustContainerPosition(container: View, x: Int, y: Int) {
        container.post {
            // 현재 LayoutParams 가져오기
            var layoutParams = container.layoutParams

            // LayoutParams가 FrameLayout.LayoutParams가 아니면 새로 생성
            if (layoutParams !is FrameLayout.LayoutParams) {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val frameParams = layoutParams as FrameLayout.LayoutParams

            // 절대 위치 설정
            frameParams.gravity = Gravity.NO_GRAVITY
            frameParams.leftMargin = x - 100 // 중앙 정렬을 위해 절반만큼 빼기
            frameParams.topMargin = y - 50
            frameParams.rightMargin = 0
            frameParams.bottomMargin = 0

            container.layoutParams = frameParams
            container.requestLayout()

            Log.e("OVERLAY", "간단 위치 조정: 컨테이너를 ($x, $y) 위치로 이동")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayView != null) {
            windowManager.removeView(overlayView)
        }
    }
}