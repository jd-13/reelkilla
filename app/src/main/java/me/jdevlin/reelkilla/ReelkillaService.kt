package me.jdevlin.reelkilla

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ReelkillaService : AccessibilityService() {
    companion object {
        private const val TAG = "ReelkillaService"
        private const val CHANNEL_ID = "reel_alerts"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reel Alerts",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val pkg = event?.packageName?.toString() ?: return

            val rootNode = rootInActiveWindow ?: return
            val now = System.currentTimeMillis()

//            printNodeHierarchy(rootNode, 0)

            if (pkg == "com.instagram.android") {
                val nextOpenAt = NextOpenTimeStore.getNextOpenAt(this)

                if (nextOpenAt > now) {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }

                if (isReelsScreen(rootNode)) {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
            }
        }
    }

    fun printNodeHierarchy(node: AccessibilityNodeInfo, depth: Int) {
        val prefix = "  ".repeat(depth)
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val clazz = node.className?.toString() ?: ""
        val id = node.viewIdResourceName ?: ""
        val visible = node.isVisibleToUser.toString()
        Log.d(TAG, "$prefix$clazz id='$id' text='$text' desc='$desc' visible='$visible'")
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            printNodeHierarchy(child, depth + 1)
        }
    }

    fun isReelsScreen(root: AccessibilityNodeInfo): Boolean {
        // Example: look for a node with content description containing "Reels"
        val nodes = root.findAccessibilityNodeInfosByText("Reels")

        // We're looking for:
        // android.widget.FrameLayout: text='' desc='' visible='true'
        //   android.widget.Button: text='Reels' desc='' visible='true'
        //   android.widget.LinearLayout: text='' desc='' visible='true'

        for (node in nodes) {
            if (node == null) {
                continue
            }

            if (!node.isVisibleToUser) {
                continue
            }

            if (node.text != "Reels") {
                continue
            }

            val parent = node.parent
            if (parent == null) {
                continue
            }

            if (parent.className != "android.widget.FrameLayout" ||
                !parent.isVisibleToUser) {
                continue
            }

//            val sibling = parent.getChild(1)
//            if (sibling == null) {
//                continue
//            }
//
//            if (sibling.className != "android.widget.LinearLayout" || !sibling.isVisibleToUser) {
//                continue
//            }

            return true
        }

        return false
    }

    override fun onInterrupt() {}
}
