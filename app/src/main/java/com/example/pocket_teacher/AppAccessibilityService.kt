package com.example.pocket_teacher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AppAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        // Accessibility Service 연결
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when(event?.packageName){
            "com.kakao.talk"->
                when(event.eventType){
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ->
                        collectButtons("com.kakao.talk")
                }
            "com.google.android.youtube"->
                when(event.eventType){
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ->
                        collectButtons("com.google.android.youtube")
                }
            "com.android.chrome" ->
                when(event.eventType) {
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ->
                        collectButtons("com.android.chrome")
                }
            "com.sec.android.app.sbrowser" ->
                when(event.eventType) {
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ->
                        collectButtons("com.sec.android.app.sbrowser")
                }
            "org.mozilla.firefox" ->
                when(event.eventType) {
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ->
                        collectButtons("org.mozilla.firefox")
                }
            "com.brave.browser"->
                when(event.eventType) {
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ->
                        collectButtons("com.brave.browser")
                }

        }
    }

    private fun collectButtons(packageName: String) {
        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            // 앱 버튼 수집
            val buttonInfoList = mutableListOf<String>()
            findButtonElements(rootNode, buttonInfoList)
            rootNode.recycle()

            val intent = when(packageName) {
                "com.kakao.talk" -> Intent(this, KakaoOverlayService::class.java)
                "com.google.android.youtube" -> Intent(this, YoutubeOverlayService::class.java)
                "com.android.chrome",           // Chrome
                "com.sec.android.app.sbrowser", // Samsung Internet
                "org.mozilla.firefox",          // Firefox
                "com.brave.browser"             // Brave Browser
                    -> when(ButtonState.selectedService) {
                    "TRAIN" -> Intent(this, TrainOverlayService::class.java)
                    "BASEBALL" -> Intent(this, BaseballOverlayService::class.java)
                    else -> null
                }
                else -> null
            }

            // intent가 null이 아닐 때만 실행
            intent?.let {
                it.putExtra("target_package", packageName)
                it.putStringArrayListExtra("button_info_list", ArrayList(buttonInfoList))
                startService(it)
            }
        }
    }

    private fun findButtonElements(node: AccessibilityNodeInfo, buttonInfoList: MutableList<String>) {
        // 버튼이나 클릭 가능한 요소 확인
        if (node.className == "android.widget.Button" ||
            node.className == "android.widget.ImageButton" ||
            node.isClickable ||
            node.isLongClickable) {

            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            val buttonInfo = "${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}|" +
                    "${node.isEnabled}|" +
                    "${bounds.width()}x${bounds.height()}|" +
                    "${node.text ?: ""}|" +
                    "${node.contentDescription ?: ""}"
            buttonInfoList.add(buttonInfo)
        }

        // 자식 노드들도 탐색
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findButtonElements(child, buttonInfoList)
                child.recycle()
            }
        }
    }

    override fun onInterrupt() {
        Log.e("APP_ACCESS", "AccessibilityService 중단됨")
    }
}