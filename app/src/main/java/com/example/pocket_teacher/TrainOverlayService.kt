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
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout

// 공통 배치 파라미터
private val GAP_DP = 12f          // 타겟-말풍선 최소 간격
private val SAFE_TOP_DP = 8f      // 상태바 등 최상단 안전 여백

private enum class Placement { TOP, BOTTOM, LEFT, RIGHT }

class TrainOverlayService : Service() {

    private val TAG = "TrainOverlayService"

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var isOverlayAdded = false

    // 웹뷰/툴바 보정
    private var offsetX = 0
    private var offsetY = 0

    /** 말풍선 배치 프로필 */
    private data class BubbleProfile(
        val order: List<Placement>,
        val dxDp: Float = 0f,
        val dyDp: Float = 0f,
        val snapRightEdge: Boolean = false // true면 화면 오른쪽 끝에 스냅(옵션 말풍선 전용)
    )

    // ▶ 프로필: 인접/SR은 오른쪽 끝 스냅 + 위로 확 올림, 열차조회는 사이드(우→좌) 우선
    private val bubbleProfiles: Map<Int, BubbleProfile> by lazy {
        mapOf(
            R.id.nearby_option_container to BubbleProfile(
                // 꼬리 제거라 방향은 의미 없음. 스냅+nudging만 사용
                order = listOf(Placement.TOP, Placement.BOTTOM, Placement.LEFT, Placement.RIGHT),
                dxDp = 0f, dyDp = -65f,  // ← 더 위로
                snapRightEdge = true
            ),
            R.id.sr_option_container to BubbleProfile(
                order = listOf(Placement.TOP, Placement.BOTTOM, Placement.LEFT, Placement.RIGHT),
                dxDp = 0f, dyDp = -65f,
                snapRightEdge = true
            ),
            R.id.etc_container to BubbleProfile(
                order = listOf(Placement.TOP, Placement.BOTTOM, Placement.LEFT, Placement.RIGHT),
                dxDp = -22f, dyDp = 0f
            ),
            // “6. 열차조회 클릭” : 버튼 오른쪽 → 왼쪽 → 위 → 아래 우선, 살짝 수평 여유
            R.id.train_search_container to BubbleProfile(
                order = listOf(Placement.RIGHT, Placement.LEFT, Placement.TOP, Placement.BOTTOM),
                dxDp = -120f, dyDp = -4f,   // 살짝 위로 맞춤
                snapRightEdge = false
            )
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Log.d(TAG, "onCreate()")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(TAG, "Settings.canDrawOverlays=${Settings.canDrawOverlays(this)}")
        }

        offsetX = intent?.getIntExtra("overlay_offset_x", 0) ?: 0
        offsetY = intent?.getIntExtra("overlay_offset_y", 0) ?: 0

        val buttonInfoList = intent?.getStringArrayListExtra("button_info_list")
        if (buttonInfoList == null) {
            Log.w(TAG, "buttonInfoList=null → 표시 건너뜀")
            return START_STICKY
        }
        showOverlay(buttonInfoList)
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showOverlay(buttonInfoList: List<String>) {
        // 기존 제거
        if (overlayView != null && isOverlayAdded) {
            runCatching {
                windowManager.removeView(overlayView)
                isOverlayAdded = false
            }.onFailure { Log.e(TAG, "기존 overlayView 제거 실패", it) }
        }

        overlayView = runCatching {
            LayoutInflater.from(this).inflate(R.layout.service_train_ticket_overlay, null)
        }.onFailure {
            Log.e(TAG, "overlayView inflate 실패", it)
        }.getOrNull() ?: return

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
            x = 0; y = 0
        }

        runCatching {
            windowManager.addView(overlayView, params)
            isOverlayAdded = true
        }.onFailure {
            Log.e(TAG, "overlay addView 실패", it); return
        }

        runCatching { updateOverlayVisibility(buttonInfoList) }
            .onFailure { Log.e(TAG, "updateOverlayVisibility 예외", it) }
    }

    private data class Node(val l:Int, val t:Int, val r:Int, val b:Int, val key:String)

    private fun updateOverlayVisibility(buttonInfoList: List<String>) {
        val root = overlayView ?: return

        // 전부 숨김
        val allIds = intArrayOf(
            R.id.departure_container,
            R.id.change_location_container,
            R.id.arrival_container,
            R.id.date_select_container,
            R.id.travel_type_container,
            R.id.passenger_count_container,
            R.id.etc_container,
            R.id.kakao_login_container,
            R.id.nearby_option_container,
            R.id.sr_option_container,
            R.id.train_search_container,
            R.id.highlight_box
        )
        allIds.forEach { id -> root.findViewById<View>(id)?.visibility = View.GONE }

        // 파싱
        val nodes = mutableListOf<Node>()
        buttonInfoList.forEachIndexed { i, raw ->
            try {
                val parts = raw.split("|")
                val pos = parts[0].split(",")
                val l = pos[0].trim().toInt()
                val t = pos[1].trim().toInt()
                val r = pos[2].trim().toInt()
                val b = pos[3].trim().toInt()
                val key = parts.joinToString("|") { it.trim() }.lowercase()
                nodes += Node(l,t,r,b,key)
            } catch (e: Throwable) {
                Log.e(TAG, "[node][$i] parse fail raw=$raw", e)
            }
        }

        // 공통 표시(프로필 적용)
        fun showAt(id: Int, tag: String, n: Node) {
            val v = root.findViewById<View>(id) ?: return
            v.visibility = View.VISIBLE
            placeBubbleWithProfile(id, v, n.l, n.t, n.r, n.b)
            Log.d(TAG, "[show] $tag @(${n.l},${n.t},${n.r},${n.b})")
        }

        // 주요 가이드 (출발역과 도착역 전환/ 왕복/편도 선택은 주석처리)
        nodes.forEach { n ->
            when {
                n.key.contains("출발역") -> showAt(R.id.departure_container, "출발역", n)

                //  출발↔도착 전환
//                 n.key.contains("출발지/도착지") || n.key.contains("전환") || n.key.contains("바꾸기") ->
//                     showAt(R.id.change_location_container, "출발↔도착 전환", n)

                n.key.contains("도착역") -> showAt(R.id.arrival_container, "도착역", n)

                // 왕복/편도 선택
//                 (n.key.contains("왕복") || n.key.contains("편도")) ->
//                     showAt(R.id.travel_type_container, "왕복/편도 선택", n)

                n.key.contains("출발일") -> showAt(R.id.date_select_container, "출발일", n)

                (n.key.contains("총") && n.key.contains("명")) ->
                    showAt(R.id.passenger_count_container, "인원수", n)

                (n.key.contains("검색") && n.key.contains("옵션")) ||
                        (n.key.contains("확대") && n.key.contains("펼치기")) ->
                    showAt(R.id.etc_container, "검색 옵션", n)

                n.key.contains("카카오") && n.key.contains("로그인") ->
                    showAt(R.id.kakao_login_container, "카카오로그인", n)
            }
        }


        // 옵션 패널 펼침 감지 → 인접역/SR (오른쪽 스냅 + 위로)
        val dm = resources.displayMetrics
        val mergeThreshold = (12 * dm.density).toInt()
        val header = nodes.filter { it.key.contains("옵션") }.minByOrNull { it.t }

        val radios = nodes
            .filter { it.key.contains("포함") || it.key.contains("미포함") }
            .filter { header == null || it.t > header.t - (8 * dm.density).toInt() }
            .sortedBy { it.t }
            .toMutableList()

        val lines = mutableListOf<Node>()
        var i = 0
        while (i < radios.size) {
            var j = i
            var minL = radios[i].l; var minT = radios[i].t
            var maxR = radios[i].r; var maxB = radios[i].b
            while (j + 1 < radios.size &&
                kotlin.math.abs(radios[j + 1].t - radios[j].t) <= mergeThreshold) {
                j++
                minL = minOf(minL, radios[j].l); minT = minOf(minT, radios[j].t)
                maxR = maxOf(maxR, radios[j].r); maxB = maxOf(maxB, radios[j].b)
            }
            lines += Node(minL, minT, maxR, maxB, "radio-line")
            i = j + 1
            if (lines.size == 2) break
        }

        if (lines.size >= 2) {
            showAt(R.id.nearby_option_container, "인접역", lines[0])
            showAt(R.id.sr_option_container, "SR연계",  lines[1])
        } else {
            root.findViewById<View>(R.id.nearby_option_container)?.visibility = View.GONE
            root.findViewById<View>(R.id.sr_option_container)?.visibility = View.GONE
        }

        // “열차 조회” : 웹에서 파싱한 버튼 사각형 기준 사이드 배치
        nodes.firstOrNull { it.key.contains("열차") && it.key.contains("조회") }?.let { n ->
            showAt(R.id.train_search_container, "열차조회", n)
        } ?: run {
            root.findViewById<View>(R.id.train_search_container)?.visibility = View.GONE
        }
    }

    // ---- 공통 배치 + 프로필 적용 ----

    private fun dp(v: Float): Int = (v * resources.displayMetrics.density).toInt()

    private fun placeBubbleWithProfile(
        containerId: Int, container: View,
        l: Int, t: Int, r: Int, b: Int
    ) {
        val profile = bubbleProfiles[containerId]
        if (profile == null) {
            placeBubble(container, l, t, r, b) // 기본
        } else {
            placeBubble(
                container, l, t, r, b,
                order = profile.order,
                dxDp = profile.dxDp,
                dyDp = profile.dyDp,
                snapRightEdge = profile.snapRightEdge
            )
        }
    }

    // 기본 placeBubble
    private fun placeBubble(
        container: View,
        l: Int, t: Int, r: Int, b: Int,
        order: List<Placement> = listOf(Placement.TOP, Placement.BOTTOM, Placement.LEFT, Placement.RIGHT)
    ) = placeBubble(container, l, t, r, b, order, dxDp = 0f, dyDp = 0f, snapRightEdge = false)

    // nudging & snap 지원 placeBubble
    private fun placeBubble(
        container: View,
        l: Int, t: Int, r: Int, b: Int,
        order: List<Placement>,
        dxDp: Float,
        dyDp: Float,
        snapRightEdge: Boolean
    ) {
        container.post {
            try {
                val lp = container.layoutParams as? ConstraintLayout.LayoutParams ?: return@post
                val dm = resources.displayMetrics
                val screenW = dm.widthPixels
                val screenH = dm.heightPixels

                val gap = (GAP_DP * dm.density).toInt()
                val safeTop = (SAFE_TOP_DP * dm.density).toInt()
                val margin = gap
                val arrowH = (6 * dm.density).toInt() // 꼬리 없는 말풍선도 여유 간격으로 사용

                val bw = if (container.measuredWidth > 0) container.measuredWidth else container.width
                val bh = if (container.measuredHeight > 0) container.measuredHeight else container.height

                val cx = (l + r) / 2
                val cy = (t + b) / 2

                // ---- 스냅: 오른쪽 끝(옵션 말풍선 전용)
                val base: Pair<Int, Int> = if (snapRightEdge) {
                    val x = (screenW - bw - margin) + offsetX
                    val y = (cy - bh / 2 + offsetY).coerceIn(safeTop, screenH - bh - margin)
                    x to y
                } else {
                    // 방향 우선순위대로 배치
                    fun tryTop(): Pair<Int, Int>? {
                        val x = (cx - bw / 2 + offsetX).coerceIn(margin, screenW - bw - margin)
                        val y = t - bh - gap - arrowH + offsetY
                        return if (y >= safeTop) x to y else null
                    }
                    fun tryBottom(): Pair<Int, Int>? {
                        val x = (cx - bw / 2 + offsetX).coerceIn(margin, screenW - bw - margin)
                        val y = b + gap + arrowH + offsetY
                        return if (y + bh <= screenH - margin) x to y else null
                    }
                    fun tryLeft(): Pair<Int, Int>? {
                        val x = l - bw - gap + offsetX
                        if (x < margin) return null
                        val y = (cy - bh / 2 + offsetY).coerceIn(safeTop, screenH - bh - margin)
                        return x to y
                    }
                    fun tryRight(): Pair<Int, Int>? {
                        val x = r + gap + offsetX
                        if (x + bw > screenW - margin) return null
                        val y = (cy - bh / 2 + offsetY).coerceIn(safeTop, screenH - bh - margin)
                        return x to y
                    }

                    var pos: Pair<Int, Int>? = null
                    for (p in order) {
                        pos = when (p) {
                            Placement.TOP -> tryTop()
                            Placement.BOTTOM -> tryBottom()
                            Placement.LEFT -> tryLeft()
                            Placement.RIGHT -> tryRight()
                        }
                        if (pos != null) break
                    }
                    pos ?: run {
                        // 최후의 수단: 위쪽 중앙
                        val x = (cx - bw / 2 + offsetX).coerceIn(margin, screenW - bw - margin)
                        val y = (t - bh - gap - arrowH + offsetY).coerceIn(safeTop, screenH - bh - margin)
                        x to y
                    }
                }

                // nudging + 최종 클램프
                val xNudged = (base.first + dp(dxDp)).coerceIn(margin, screenW - bw - margin)
                val yNudged = (base.second + dp(dyDp)).coerceIn(safeTop, screenH - bh - margin)

                lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                lp.leftMargin = xNudged
                lp.topMargin = yNudged
                lp.rightMargin = 0
                lp.bottomMargin = 0
                container.layoutParams = lp
                container.requestLayout()

                Log.d(TAG, "placeBubble order=$order snapRight=$snapRightEdge dx=$dxDp dy=$dyDp → $xNudged,$yNudged")
            } catch (t: Throwable) {
                Log.e(TAG, "placeBubble 예외", t)
            }
        }
    }

    override fun onDestroy() {
        if (overlayView != null && isOverlayAdded) {
            runCatching { windowManager.removeView(overlayView) }
            isOverlayAdded = false
        }
        overlayView = null
        super.onDestroy()
    }
}
