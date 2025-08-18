package com.example.pocket_teacher

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout

class YoutubeOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    // 카카오톡 종료 감지를 위한 핸들러 추가
    private val handler = Handler(Looper.getMainLooper())
    private val checkRunnable = object : Runnable {
        override fun run() {
            checkAppStatus()
            handler.postDelayed(this, 3000) // 3초마다 체크
        }
    }

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

    // 유튜브 프로세스 체크
    private fun checkAppStatus() {
        try {
            val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            val runningProcesses = activityManager.runningAppProcesses

            val isKakaoRunning = runningProcesses?.any { process ->
                process.processName == "com.google.andriod.youtube"
            } ?: false

            if (!isKakaoRunning) {
                Log.d("OVERLAY_SERVICE", "카카오톡 프로세스 종료됨 - 서비스 종료")
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e("OVERLAY_SERVICE", "앱 상태 체크 실패: ${e.message}")
        }
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
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

        windowManager.addView(overlayView, params)

        updateOverlayVisibility(buttonInfoList)
    }

    private fun updateOverlayVisibility(buttonInfoList: List<String>) {
        overlayView?.let { view ->
            // 모두 GONE
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

            // 필요한 것만 VISIBLE
            buttonInfoList.forEach { buttonInfo ->
                val parts = buttonInfo.split("|")
                val positions = parts[0].split(",")
                val left = positions[0].toInt()
                val top = positions[1].toInt()
                val right = positions[2].toInt()
                val bottom = positions[3].toInt()

                val description = parts[4].takeIf { it.isNotEmpty() }
                val combinedText = (description ?: "").lowercase()

                when {
                    combinedText.contains("cast") ||
                            (combinedText.contains("전송") && combinedText.contains("버튼")) -> {
                        val c = view.findViewById<View>(R.id.cast_container)
                        c?.let { it.visibility = View.VISIBLE; topContainerPosition(it, left, top) }
                    }
                    combinedText.contains("search") || combinedText.contains("검색") -> {
                        val c = view.findViewById<View>(R.id.search_container)
                        c?.let { it.visibility = View.VISIBLE; topContainerPosition(it, left, top) }
                    }
                    combinedText.contains("notifications") || combinedText.contains("알림") -> {
                        val c = view.findViewById<View>(R.id.notifications_container)
                        c?.let { it.visibility = View.VISIBLE; topContainerPosition(it, left, top) }
                    }
                    combinedText.contains("more") ||
                            (combinedText.contains("옵션") && combinedText.contains("더보기")) -> {
                        val c = view.findViewById<View>(R.id.more_container)
                        c?.let { it.visibility = View.VISIBLE; topContainerPosition(it, left, top) }
                    }
                    (combinedText.contains("like") && combinedText.contains("this") && combinedText.contains("video")) ||
                            (combinedText.contains("동영상을") && combinedText.contains("좋아함")) -> {
                        val c = view.findViewById<View>(R.id.like_container)
                        c?.let { it.visibility = View.VISIBLE; topContainerPosition(it, left, top) }
                    }
                    (combinedText.contains("dislike") && combinedText.contains("this") && combinedText.contains("video")) ||
                            (combinedText.contains("싫어요") && combinedText.contains("표시")) -> {
                        val c = view.findViewById<View>(R.id.dislike_container)
                        c?.let { it.visibility = View.VISIBLE; topContainerPosition(it, left, top) }
                    }
                    (combinedText.contains("view") && combinedText.contains("comments")) ||
                            (combinedText.contains("댓글") && combinedText.contains("보기")) -> {
                        val c = view.findViewById<View>(R.id.comments_container)
                        c?.let { it.visibility = View.VISIBLE; topContainerPosition(it, left, top) }
                    }
                    (combinedText.contains("share") && combinedText.contains("this") && combinedText.contains("video")) ||
                            (combinedText.contains("동영상") && combinedText.contains("공유")) -> {
                        val c = view.findViewById<View>(R.id.share_container)
                        c?.let { it.visibility = View.VISIBLE; topContainerPosition(it, left, top) }
                    }

                    // 하단
                    combinedText.contains("home") || combinedText.contains("홈") -> {
                        val c = view.findViewById<View>(R.id.home_container)
                        c?.let { it.visibility = View.VISIBLE; bottomContainerPosition(it, left, top) }
                    }
                    combinedText.contains("shorts") -> {
                        val c = view.findViewById<View>(R.id.shorts_container)
                        c?.let { it.visibility = View.VISIBLE; bottomContainerPosition(it, left, top) }
                    }
                    combinedText.contains("create") || combinedText.contains("만들기") -> {
                        val c = view.findViewById<View>(R.id.create_container)
                        c?.let { it.visibility = View.VISIBLE; bottomContainerPosition(it, left, top) }
                    }
                    combinedText.contains("subscriptions") || combinedText.contains("구독") -> {
                        val c = view.findViewById<View>(R.id.subscriptions_container)
                        c?.let { it.visibility = View.VISIBLE; bottomContainerPosition(it, left, top) }
                    }
                    (combinedText.contains("you") && !combinedText.contains("tube")) ||
                            (combinedText.contains("내") && combinedText.contains("프로필")) ||
                            combinedText.contains("내 페이지") ||
                            combinedText.contains("내페이지") ||
                            combinedText.contains("profile") ||
                            combinedText.contains("me") -> {
                        val c = view.findViewById<View>(R.id.you_container)
                        c?.let { it.visibility = View.VISIBLE; bottomContainerPosition(it, left, top) }
                    }
                }
            }
        }
    }

    private fun topContainerPosition(container: View, x: Int, y: Int) {
        container.post {
            val p = container.layoutParams as ConstraintLayout.LayoutParams
            p.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            p.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            p.leftMargin = x - 30
            p.topMargin = y
            p.rightMargin = 0
            p.bottomMargin = 0
            container.layoutParams = p
            container.requestLayout()

            // 버튼 아래에 말풍선 → 꼬리는 위
            setBalloonTail(container, tailOnTop = true)
        }
    }

    private fun bottomContainerPosition(container: View, x: Int, y: Int) {
        container.post {
            val params = container.layoutParams as ConstraintLayout.LayoutParams
            val parent = container.parent as View

            if (parent.width == 0 || parent.height == 0 ||
                container.measuredWidth == 0 || container.measuredHeight == 0) {
                container.post { bottomContainerPosition(container, x, y) }
                return@post
            }

            val pw = parent.width
            val ph = parent.height
            val w = container.measuredWidth
            val h = container.measuredHeight

            val gap = (6 * resources.displayMetrics.density).toInt()
            val pad = (6 * resources.displayMetrics.density).toInt()

            val left = x.coerceIn(pad, (pw - w - pad).coerceAtLeast(pad))
            var top = y - h - gap
            top = top.coerceIn(pad, ph - h - pad)
            val bottomMargin = ph - (top + h)

            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            params.topToTop = ConstraintLayout.LayoutParams.UNSET
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET
            params.leftMargin = left
            params.topMargin = 0
            params.rightMargin = 0
            params.bottomMargin = bottomMargin

            container.layoutParams = params
            container.requestLayout()

            // 버튼 위에 말풍선 → 꼬리는 아래
            setBalloonTail(container, tailOnTop = false)
        }
    }

    private fun setBalloonTail(container: View, tailOnTop: Boolean) {
        val ll = container as? LinearLayout ?: return

        var tail: ImageView? = null
        var bubble: TextView? = null
        for (i in 0 until ll.childCount) {
            when (val child = ll.getChildAt(i)) {
                is ImageView -> if (tail == null) tail = child
                is TextView -> if (bubble == null) bubble = child
            }
        }
        val tailView = tail ?: return
        val bubbleView = bubble ?: return

        // 위/아래 재배치 + 방향 변경
        if (tailOnTop) {
            if (ll.indexOfChild(tailView) != 0) {
                ll.removeView(tailView)
                ll.addView(tailView, 0)
            }
            if (ll.indexOfChild(bubbleView) != 1) {
                ll.removeView(bubbleView)
                ll.addView(bubbleView, 1)
            }
            tailView.rotation = 180f
        } else {
            if (ll.indexOfChild(bubbleView) != 0) {
                ll.removeView(bubbleView)
                ll.addView(bubbleView, 0)
            }
            if (ll.indexOfChild(tailView) != ll.childCount - 1) {
                ll.removeView(tailView)
                ll.addView(tailView)
            }
            tailView.rotation = 0f
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 핸들러 정리
        handler.removeCallbacks(checkRunnable)

        if (overlayView != null) {
            windowManager.removeView(overlayView)
        }
    }
}