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
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout

class YoutubeOverlayService : Service() {
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
        overlayView = LayoutInflater.from(this).inflate(R.layout.service_youtube_overlay, null)

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
            view.findViewById<View>(R.id.cast_container)?.visibility = View.GONE
            view.findViewById<View>(R.id.search_container)?.visibility = View.GONE
            view.findViewById<View>(R.id.notifications_container)?.visibility = View.GONE
            view.findViewById<View>(R.id.like_container)?.visibility = View.GONE
            view.findViewById<View>(R.id.dislike_container)?.visibility = View.GONE
            view.findViewById<View>(R.id.comments_container)?.visibility = View.GONE
            view.findViewById<View>(R.id.share_container)?.visibility = View.GONE
            view.findViewById<View>(R.id.more_container)?.visibility = View.GONE
            // 하단
            view.findViewById<View>(R.id.home_container)?.visibility = View.GONE
            view.findViewById<View>(R.id.shorts_container)?.visibility = View.GONE
            view.findViewById<View>(R.id.create_container)?.visibility = View.GONE
            view.findViewById<View>(R.id.subscriptions_container)?.visibility = View.GONE
            view.findViewById<View>(R.id.you_container)?.visibility = View.GONE

            // 버튼 정보를 체크해서 해당하는 것들만 VISIBLE로 변경
            buttonInfoList.forEach { buttonInfo ->
                val parts = buttonInfo.split("|")
                val positions = parts[0].split(",")
                val left = positions[0].toInt()
                val top = positions[1].toInt()
                val right = positions[2].toInt()
                val bottom = positions[3].toInt()

                val description = parts[4].takeIf { it.isNotEmpty() } // 설명
                val combinedText = (description ?: "").lowercase()

                when {
                    combinedText.contains("cast") ||
                         (combinedText.contains("전송")&&combinedText.contains("버튼"))-> {
                        val container = view.findViewById<View>(R.id.cast_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            topContainerPosition(it, left, top)
                        }
                    }
                    combinedText.contains("search") || combinedText.contains("검색") -> {
                        val container = view.findViewById<View>(R.id.search_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            topContainerPosition(it, left, top)
                        }
                    }
                    combinedText.contains("notifications") || combinedText.contains("알림")-> {
                        val container = view.findViewById<View>(R.id.notifications_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            topContainerPosition(it, left, top)
                        }
                    }
                    combinedText.contains("more") ||
                        (combinedText.contains("옵션")&&combinedText.contains("더보기"))-> {
                        val container = view.findViewById<View>(R.id.more_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            topContainerPosition(it, left, top)
                        }
                    }
                    (combinedText.contains("like")&&combinedText.contains("this")
                            && combinedText.contains("video")) 
                            || (combinedText.contains("동영상을")&&combinedText.contains("좋아함")) -> {
                        val container = view.findViewById<View>(R.id.like_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            topContainerPosition(it, left, top)
                        }
                    }
                    (combinedText.contains("dislike")&&combinedText.contains("this")
                            && combinedText.contains("video"))
                            || (combinedText.contains("싫어요")&&combinedText.contains("표시"))    -> {
                        val container = view.findViewById<View>(R.id.dislike_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            topContainerPosition(it, left, top)
                        }
                    }
                    (combinedText.contains("view")&&combinedText.contains("comments"))
                            || (combinedText.contains("댓글")&&combinedText.contains("보기"))-> {
                        val container = view.findViewById<View>(R.id.comments_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            topContainerPosition(it, left, top)
                        }
                    }
                    combinedText.contains("share")&&combinedText.contains("this")
                            &&combinedText.contains("video")
                            || (combinedText.contains("동영상")&&combinedText.contains("공유"))    -> {
                        val container = view.findViewById<View>(R.id.share_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            topContainerPosition(it, left, top)
                        }
                    }
                    // 하단
                    combinedText.contains("home") || combinedText.contains("홈")-> {
                        val container = view.findViewById<View>(R.id.home_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            bottomContainerPosition(it, left, top)
                        }
                    }
                    combinedText.contains("shorts") -> {
                        val container = view.findViewById<View>(R.id.shorts_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            bottomContainerPosition(it, left, top)
                        }
                    }
                    combinedText.contains("create") || combinedText.contains("만들기") -> {
                        val container = view.findViewById<View>(R.id.create_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            bottomContainerPosition(it, left, top)
                        }
                    }
                    combinedText.contains("subscriptions") || combinedText.contains("구독") -> {
                        val container = view.findViewById<View>(R.id.subscriptions_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            bottomContainerPosition(it, left, top)
                        }
                    }
                    (combinedText.contains("you") && !combinedText.contains("tube"))
                            || (combinedText.contains("내") && combinedText.contains("프로필"))-> {
                        val container = view.findViewById<View>(R.id.you_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            bottomContainerPosition(it, left, top)
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