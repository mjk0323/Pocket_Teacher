package com.example.pocket_teacher

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.annotation.RequiresApi

class KakaoOverlayService : Service() {

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

        // 앱 상태 체크 시작
        handler.postDelayed(checkRunnable, 5000) // 5초 후부터 시작

        return START_STICKY
    }

    // 카카오톡 프로세스 체크
    private fun checkAppStatus() {
        try {
            val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            val runningProcesses = activityManager.runningAppProcesses

            val isKakaoRunning = runningProcesses?.any { process ->
                process.processName == "com.kakao.talk"
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
            // 모든 컨테이너를 GONE으로 설정
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

                val description = parts[4].takeIf { it.isNotEmpty() } // 설명
                val combinedText = (description ?: "").lowercase()

                when {
                    // 상단 탭
                    combinedText.contains("검색") && ! combinedText.contains("상품을 검색해보세요")-> {
                        val container = view.findViewById<View>(R.id.search_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            topContainerPosition(it, left, top)
                        }
                    }
                    combinedText.contains("상품을 검색해보세요")-> {
                        val container = view.findViewById<View>(R.id.search_product_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            topContainerPosition(it, left, top)
                        }
                    }
                    combinedText.contains("친구 추가")-> {
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
                    combinedText.contains("옵션 더보기") -> {
                        val container = view.findViewById<View>(R.id.option_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            topContainerPosition(it, left, top)
                        }
                    }

                    // 오픈채팅 상단 (원본 유지)
                    combinedText.contains("지금 뜨는")-> {
                        val container = view.findViewById<View>(R.id.openchat_category_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            topContainerPosition(it, left, top)
                        }
                    }

                    // 하단 탭
                    combinedText.contains("친구 탭")-> {
                        val container = view.findViewById<View>(R.id.friend_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            bottomContainerPosition(it, left, top)
                        }
                    }
                    combinedText.contains("채팅 탭")-> {
                        val container = view.findViewById<View>(R.id.chat_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            bottomContainerPosition(it, left, top)
                        }
                    }
                    combinedText.contains("open chat tab")-> {
                        val container = view.findViewById<View>(R.id.openchat_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            bottomContainerPosition(it, left, top)
                        }
                    }
                    combinedText.contains("쇼핑 탭") -> {
                        val container = view.findViewById<View>(R.id.shopping_container)
                        container?.let {
                            it.visibility = View.VISIBLE
                            bottomContainerPosition(it, left, top)
                        }
                    }
                    combinedText.contains("더보기 탭") -> {
                        val container = view.findViewById<View>(R.id.etc_container)
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