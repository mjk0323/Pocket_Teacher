package com.example.pocket_teacher

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AppAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.e("APP_ACCESS", "=====================")
        Log.e("APP_ACCESS", "AccessibilityService 연결됨")
        Log.e("APP_ACCESS", "=====================")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 카카오톡에서만 동작
        if (event?.packageName == "com.kakao.talk") {
            Log.e("APP_ACCESS", "카카오톡 이벤트 감지: ${event.eventType}")

            // 화면 변화나 윈도우 상태 변경시 버튼 수집
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

                collectButtons()
            }
        }
        // 유튜브에서만 동작
        else if (event?.packageName == "com.google.android.youtube") {
            Log.e("APP_ACCESS", "유튜브 이벤트 감지: ${event.eventType}")

            // 화면 변화나 윈도우 상태 변경시 버튼 수집
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

                collectButtons()
            }
        }
    }

    private fun collectButtons() {
        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            Log.e("APP_ACCESS", "===== 앱 버튼 수집 시작 =====")
            findButtonElements(rootNode)
            rootNode.recycle()
            Log.e("APP_ACCESS", "===== 앱 버튼 수집 완료 =====")
        }
    }

    private fun findButtonElements(node: AccessibilityNodeInfo) {
        // 버튼이나 클릭 가능한 요소 확인
        if (node.className == "android.widget.Button" ||
            node.className == "android.widget.ImageButton" ||
            node.isClickable) {

            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            Log.e("APP_BUTTON",
                "클래스: ${node.className} | " +
                        "위치: (${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}) | " +
                        "크기: ${bounds.width()}x${bounds.height()} | " +
                        "활성화: ${node.isEnabled} | " +
                        "텍스트: ${node.text ?: "null"} | " +
                        "설명: ${node.contentDescription ?: "null"}"
            )
        }

        // 자식 노드들도 탐색
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findButtonElements(child)
                child.recycle()
            }
        }
    }

    override fun onInterrupt() {
        Log.e("APP_ACCESS", "AccessibilityService 중단됨")
    }
}