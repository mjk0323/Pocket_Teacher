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
import android.widget.AbsoluteLayout
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
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
            view.findViewById<View>(R.id.search_container)?.visibility = View.GONE
            view.findViewById<View>(R.id.add_friend_container)?.visibility = View.GONE
            view.findViewById<View>(R.id.friend_container)?.visibility = View.GONE
            view.findViewById<View>(R.id.talk_music_container)?.visibility = View.GONE
            view.findViewById<View>(R.id.option_container)?.visibility = View.GONE

            view.findViewById<View>(R.id.profile_container)?.visibility = View.GONE
            view.findViewById<View>(R.id.openchat_category_container)?.visibility = View.GONE

            view.findViewById<View>(R.id.friend_container)?.visibility = View.GONE
            view.findViewById<View>(R.id.chat_container)?.visibility = View.GONE
            view.findViewById<View>(R.id.openchat_container)?.visibility = View.GONE
            view.findViewById<View>(R.id.shopping_container)?.visibility = View.GONE
            view.findViewById<View>(R.id.etc_container)?.visibility = View.GONE

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
                        val container = view.findViewById<View>(R.id.search_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            topContainerPosition(it, left, top)
                        }
                    }
                    combinedText.contains("친구") && combinedText.contains("추가") -> {
                        val container = view.findViewById<View>(R.id.add_friend_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            topContainerPosition(it, left, top) // 버튼 오른쪽 위
                        }
                    }
                    combinedText.contains("톡뮤직") -> {
                        val container = view.findViewById<View>(R.id.talk_music_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            topContainerPosition(it, left, top)
                        }
                    }
                    combinedText.contains("옵션") && combinedText.contains("더보기") -> {
                        val container = view.findViewById<View>(R.id.option_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            topContainerPosition(it, left, top)
                        }
                    }

                    // 오픈채팅 상단
                    combinedText.contains("지금") && combinedText.contains("뜨는") -> {
                        val container = view.findViewById<View>(R.id.openchat_category_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            topContainerPosition(it, left, top)
                        }
                    }

                    // 하단 탭 : y좌표 top-200으로 설정 필요
                    combinedText.contains("친구") && combinedText.contains("탭") -> {
                        val container = view.findViewById<View>(R.id.friend_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            bottomContainerPosition(it, left, top-200)
                        }
                    }
                    combinedText.contains("채팅 탭") && combinedText.contains("탭") -> {
                        val container = view.findViewById<View>(R.id.chat_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            bottomContainerPosition(it, left, top-200)
                        }
                    }
                    combinedText.contains("open")-> {
                        val container = view.findViewById<View>(R.id.openchat_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            bottomContainerPosition(it, left, top-200)
                        }
                    }
                    combinedText.contains("쇼핑 탭") -> {
                        val container = view.findViewById<View>(R.id.shopping_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            bottomContainerPosition(it, left, top-200)
                        }
                    }
                    combinedText.contains("더보기 탭") -> {
                        val container = view.findViewById<View>(R.id.etc_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            bottomContainerPosition(it, left, top-200)
                        }
                    }
                }
            }
        }
    }

    private fun topContainerPosition(container: View, x: Int, y: Int) {
        container.post {
            // ConstraintLayout.LayoutParams로 캐스팅
            val constraintParams = container.layoutParams as ConstraintLayout.LayoutParams

            // 부모의 왼쪽 위 모서리를 기준점으로 설정
            constraintParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            constraintParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID

            // 절대 좌표를 마진으로 설정
            constraintParams.leftMargin = x-30
            constraintParams.topMargin = y

            // 다른 마진은 0으로 초기화
            constraintParams.rightMargin = 0
            constraintParams.bottomMargin = 0

            container.layoutParams = constraintParams
            container.requestLayout()
        }
    }

    private fun bottomContainerPosition(container: View, x: Int, y: Int) {
        container.post {
            // ConstraintLayout.LayoutParams로 캐스팅
            val constraintParams = container.layoutParams as ConstraintLayout.LayoutParams

            // 부모의 왼쪽 위 모서리를 기준점으로 설정
            constraintParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            constraintParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

            // 절대 좌표를 마진으로 설정
            constraintParams.leftMargin = x
            constraintParams.topMargin = 0

            // 다른 마진은 0으로 초기화
            constraintParams.rightMargin = 0
            constraintParams.bottomMargin = -y

            container.layoutParams = constraintParams
            container.requestLayout()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayView != null) {
            windowManager.removeView(overlayView)
        }
    }
}