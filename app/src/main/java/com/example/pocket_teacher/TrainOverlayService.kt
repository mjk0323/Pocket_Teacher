package com.example.pocket_teacher

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout

// ===== 간격/보정 상수 =====
private const val GAP_DP_BASE = 20f     // 기본 버튼-말풍선 간격(유지)
private const val EXTRA_STEP_DP = 8f    // 겹칠 때마다 추가로 늘릴 간격(계단식)
private const val EXTRA_MAX_STEPS = 4   // 최대 재시도 단계 수
private const val SAFE_TOP_DP = 8f      // 화면 최상단 안전 여백
private const val ARROW_FALLBACK_DP = 8f// 꼬리(삼각형) 높이 기본값(리소스 미측정 시)

class TrainOverlayService : Service() {

    private val TAG = "TrainOverlayService"

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var isOverlayAdded = false

    // 웹 주소창/툴바 움직임 등에 대한 공통 오프셋
    private var offsetX = 0
    private var offsetY = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Log.d(TAG, "onCreate()")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand flags=$flags startId=$startId intent=$intent")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(TAG, "Settings.canDrawOverlays=${Settings.canDrawOverlays(this)}")
        }

        // 공통 오프셋(없으면 0)
        offsetX = intent?.getIntExtra("overlay_offset_x", 0) ?: 0
        offsetY = intent?.getIntExtra("overlay_offset_y", 0) ?: 0
        Log.d(TAG, "overlay offset => x=$offsetX, y=$offsetY")

        val targetPackage = intent?.getStringExtra("target_package")
        val buttonInfoList = intent?.getStringArrayListExtra("button_info_list")
        Log.d(TAG, "target_package=$targetPackage, button_info_list size=${buttonInfoList?.size}")

        if (buttonInfoList == null) {
            Log.w(TAG, "buttonInfoList=null → 표시 건너뜀")
            return START_STICKY
        }

        showOverlay(buttonInfoList)
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showOverlay(buttonInfoList: List<String>) {
        // 기존 오버레이 제거
        if (overlayView != null && isOverlayAdded) {
            runCatching {
                windowManager.removeView(overlayView)
                isOverlayAdded = false
            }.onFailure { Log.e(TAG, "기존 overlayView 제거 실패", it) }
        }

        // inflate
        overlayView = runCatching {
            LayoutInflater.from(this).inflate(R.layout.service_train_ticket_overlay, null)
        }.onFailure {
            Log.e(TAG, "overlayView inflate 실패", it)
        }.getOrNull() ?: return

        // 전체 화면, 터치 통과
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        runCatching {
            windowManager.addView(overlayView, params)
            isOverlayAdded = true
            Log.d(TAG, "overlay addView 성공")
        }.onFailure {
            Log.e(TAG, "overlay addView 실패", it)
            return
        }

        // 내용 반영
        runCatching { updateOverlayVisibility(buttonInfoList) }
            .onFailure { Log.e(TAG, "updateOverlayVisibility 예외", it) }
    }

    private fun updateOverlayVisibility(buttonInfoList: List<String>) {
        val root = overlayView ?: return

        // 전부 GONE
        val allIds = intArrayOf(
            R.id.departure_container,
            R.id.change_location_container,
            R.id.arrival_container,
            R.id.date_select_container,
            R.id.travel_type_container,
            R.id.passenger_count_container,
            R.id.etc_container,
            R.id.kakao_login_container,
            R.id.highlight_box
        )
        allIds.forEach { id -> root.findViewById<View>(id)?.visibility = View.GONE }

        var matched = 0

        buttonInfoList.forEachIndexed { idx, raw ->
            try {
                val parts = raw.split("|")
                val pos = parts[0].split(",")
                val l = pos[0].toInt()
                val t = pos[1].toInt()
                val r = pos[2].toInt()
                val b = pos[3].toInt()

                val text = parts.getOrNull(3)?.takeIf { it.isNotEmpty() }
                val desc = parts.getOrNull(4)?.takeIf { it.isNotEmpty() }
                val key = (text ?: desc ?: "").lowercase()

                fun showAt(id: Int, tag: String) {
                    val v = root.findViewById<View>(id) ?: return
                    v.visibility = View.VISIBLE
                    placeBubble(v, l, t, r, b) // 자동 방향 + 버튼 가릴 때만 계단식 간격 증가
                    matched++
                    Log.d(TAG, "[$idx] $tag 매칭: rect=($l,$t,$r,$b) text=$text desc=$desc")
                }

                when {
                    key.contains("출발역") -> showAt(R.id.departure_container, "출발역")
                    key.contains("출발지/도착지") -> showAt(R.id.change_location_container, "출발지/도착지")
                    key.contains("도착역") -> showAt(R.id.arrival_container, "도착역")
                    key.contains("왕복") || key.contains("편도") -> showAt(R.id.travel_type_container, "여행종류")
                    key.contains("출발일") -> showAt(R.id.date_select_container, "출발일")
                    key.contains("총") && key.contains("명") -> showAt(R.id.passenger_count_container, "인원수")
                    key.contains("확대") && key.contains("펼치기") -> showAt(R.id.etc_container, "기타")
                    key.contains("카카오") && key.contains("로그인") -> showAt(R.id.kakao_login_container, "카카오로그인")
                    else -> Log.d(TAG, "[$idx] 매칭 실패: '$key' @ ($l,$t,$r,$b)")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "[$idx] 파싱 실패 raw=$raw", t)
            }
        }

        if (matched == 0) Log.w(TAG, "매칭된 말풍선이 없음")
    }

    // ===== 꼬리 방향 열거형 =====
    private enum class TailDir { UP, DOWN, LEFT, RIGHT }

    // ===== 말풍선 꼬리 전환(위/아래/좌/우) =====
    private fun setBalloonTail(container: View, dir: TailDir) {
        val ll = container as? LinearLayout ?: return

        var tail: ImageView? = null
        var bubble: TextView? = null
        for (i in 0 until ll.childCount) {
            when (val child = ll.getChildAt(i)) {
                is ImageView -> if (tail == null) tail = child
                is TextView  -> if (bubble == null) bubble = child
            }
        }
        val tailView = tail ?: return
        val bubbleView = bubble ?: return

        fun placeFirst(v: View) { ll.removeView(v); ll.addView(v, 0) }
        fun placeLast(v: View)  { ll.removeView(v); ll.addView(v) }

        when (dir) {
            TailDir.UP -> {
                ll.orientation = LinearLayout.VERTICAL
                // 아래쪽으로 꼬리: [꼬리, 버블] + 180°
                if (ll.indexOfChild(tailView) != 0) placeFirst(tailView)
                if (ll.indexOfChild(bubbleView) != 1) { ll.removeView(bubbleView); ll.addView(bubbleView, 1) }
                tailView.rotation = 180f
            }
            TailDir.DOWN -> {
                ll.orientation = LinearLayout.VERTICAL
                // 위쪽으로 꼬리: [버블, 꼬리] + 0°
                if (ll.indexOfChild(bubbleView) != 0) placeFirst(bubbleView)
                if (ll.indexOfChild(tailView) != ll.childCount - 1) placeLast(tailView)
                tailView.rotation = 0f
            }
            TailDir.LEFT -> {
                ll.orientation = LinearLayout.HORIZONTAL
                // 오른쪽 향함: [꼬리, 버블] + 90°
                if (ll.indexOfChild(tailView) != 0) placeFirst(tailView)
                if (ll.indexOfChild(bubbleView) != 1) { ll.removeView(bubbleView); ll.addView(bubbleView, 1) }
                tailView.rotation = 90f
            }
            TailDir.RIGHT -> {
                ll.orientation = LinearLayout.HORIZONTAL
                // 왼쪽 향함: [버블, 꼬리] + 270°
                if (ll.indexOfChild(bubbleView) != 0) placeFirst(bubbleView)
                if (ll.indexOfChild(tailView) != ll.childCount - 1) placeLast(tailView)
                tailView.rotation = 270f
            }
        }
    }

    // ===== 말풍선/꼬리 "실측" 유틸 =====
    private data class BubbleSize(val bw: Int, val bh: Int, val arrowH: Int)

    private fun measureBubble(container: View): BubbleSize {
        val dm = resources.displayMetrics
        val atMostW = MeasureSpec.makeMeasureSpec(dm.widthPixels, MeasureSpec.AT_MOST)
        val atMostH = MeasureSpec.makeMeasureSpec(dm.heightPixels, MeasureSpec.AT_MOST)

        // 미측정 상태에서도 즉시 사이즈 확보
        container.measure(atMostW, atMostH)
        val bw = container.measuredWidth.coerceAtLeast(container.width)
        val bh = container.measuredHeight.coerceAtLeast(container.height)

        // 꼬리 실제 높이(리소스 기반) 추출
        var tailView: ImageView? = null
        if (container is LinearLayout) {
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child is ImageView) { tailView = child; break }
            }
        }
        val arrowH = when {
            tailView == null -> (ARROW_FALLBACK_DP * dm.density).toInt()
            else -> {
                tailView!!.measure(atMostW, atMostH)
                tailView!!.measuredHeight.coerceAtLeast((ARROW_FALLBACK_DP * dm.density).toInt())
            }
        }
        return BubbleSize(bw, bh, arrowH)
    }

    /**
     * 버튼 사각형(l,t,r,b)의 가운데 기준으로
     * 1) 위 우선 → 아래 → 왼쪽 → 오른쪽 순서로 배치
     * 2) "기본 간격" 유지하되, 겹치면 EXTRA_STEP 만큼 늘리며 재시도(최대 EXTRA_MAX_STEPS)
     * 3) 실제 배치 방향에 맞춰 꼬리 전환
     */
    private fun placeBubble(container: View, l: Int, t: Int, r: Int, b: Int) {
        container.post {
            try {
                val lp = container.layoutParams as? ConstraintLayout.LayoutParams ?: return@post
                val dm = resources.displayMetrics
                val screenW = dm.widthPixels
                val screenH = dm.heightPixels

                val safeTop = (SAFE_TOP_DP * dm.density).toInt()
                val baseGap = (GAP_DP_BASE * dm.density).toInt()
                val extraStep = (EXTRA_STEP_DP * dm.density).toInt()
                val margin = baseGap

                val size = measureBubble(container)
                val bw = size.bw
                val bh = size.bh
                var arrowH = size.arrowH

                val cx = (l + r) / 2
                val cy = (t + b) / 2

                fun intersects(btnL: Int, btnT: Int, btnR: Int, btnB: Int, x: Int, y: Int, w: Int, h: Int): Boolean {
                    val bl = x
                    val bt = y
                    val br = x + w
                    val bb = y + h
                    return !(br <= btnL || bl >= btnR || bb <= btnT || bt >= btnB)
                }

                data class Candidate(val x: Int, val y: Int, val dir: TailDir)

                fun tryTop(extra: Int): Candidate? {
                    val x = (cx - bw / 2 + offsetX).coerceIn(margin, screenW - bw - margin)
                    val y = t - bh - baseGap - extra - arrowH + offsetY
                    if (y < safeTop) return null
                    return Candidate(x, y, TailDir.DOWN)
                }

                fun tryBottom(extra: Int): Candidate? {
                    val x = (cx - bw / 2 + offsetX).coerceIn(margin, screenW - bw - margin)
                    val y = b + baseGap + extra + arrowH + offsetY
                    if (y + bh > screenH - margin) return null
                    return Candidate(x, y, TailDir.UP)
                }

                fun tryLeft(extra: Int): Candidate? {
                    val x = l - bw - baseGap - extra + offsetX
                    if (x < margin) return null
                    val y = (cy - bh / 2 + offsetY).coerceIn(safeTop, screenH - bh - margin)
                    return Candidate(x, y, TailDir.RIGHT)
                }

                fun tryRight(extra: Int): Candidate? {
                    val x = r + baseGap + extra + offsetX
                    if (x + bw > screenW - margin) return null
                    val y = (cy - bh / 2 + offsetY).coerceIn(safeTop, screenH - bh - margin)
                    return Candidate(x, y, TailDir.LEFT)
                }

                // 계단식(0, step, 2*step, ...)으로 겹침이 해소될 때까지 시도
                var chosen: Candidate? = null
                loop@ for (step in 0..EXTRA_MAX_STEPS) {
                    val extra = step * extraStep

                    val order = arrayOf(::tryTop, ::tryBottom, ::tryLeft, ::tryRight)
                    for (fn in order) {
                        val cand = fn(extra) ?: continue
                        if (!intersects(l, t, r, b, cand.x, cand.y, bw, bh)) {
                            chosen = cand
                            break@loop
                        }
                    }
                }

                // 그래도 못 찾으면 최후 보정(위쪽 기준, 화면 클램프)
                if (chosen == null) {
                    val x = (cx - bw / 2 + offsetX).coerceIn(margin, screenW - bw - margin)
                    val y = (t - bh - baseGap - arrowH + offsetY).coerceIn(safeTop, screenH - bh - margin)
                    chosen = Candidate(x, y, TailDir.DOWN)
                }

                // 적용
                lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                lp.leftMargin = chosen!!.x
                lp.topMargin = chosen!!.y
                lp.rightMargin = 0
                lp.bottomMargin = 0
                container.layoutParams = lp
                container.requestLayout()

                // 꼬리 반영
                setBalloonTail(container, chosen!!.dir)

                Log.d(
                    TAG,
                    "placeBubble => pos=${chosen!!.x},${chosen!!.y}, dir=${chosen!!.dir} " +
                            "rect=($l,$t,$r,$b) bubble=${bw}x$bh arrowH=$arrowH offset=($offsetX,$offsetY)"
                )
            } catch (e: Throwable) {
                Log.e(TAG, "placeBubble 예외", e)
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        if (overlayView != null && isOverlayAdded) {
            runCatching { windowManager.removeView(overlayView) }
                .onSuccess { Log.d(TAG, "overlay 제거 완료") }
                .onFailure { Log.e(TAG, "overlay 제거 실패", it) }
            isOverlayAdded = false
        }
        overlayView = null
        super.onDestroy()
    }
}
