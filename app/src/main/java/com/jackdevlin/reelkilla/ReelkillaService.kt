package com.jackdevlin.reelkilla

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat

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

//            printNodeHierarchy(rootNode, 0)

            // Detect Instagram Reels activity (name may vary with updates)
            if (pkg == "com.instagram.android" && isReelsScreen(rootNode)) {
                // Simulate a Back button press
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        }
    }

    fun printNodeHierarchy(node: AccessibilityNodeInfo, depth: Int) {
        val prefix = "  ".repeat(depth)
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val clazz = node.className?.toString() ?: ""
        Log.d(TAG, "$prefix$clazz: text='$text' desc='$desc'")

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            printNodeHierarchy(child, depth + 1)
        }
    }

    fun isReelsScreen(root: AccessibilityNodeInfo): Boolean {
        // Example: look for a node with content description containing "Reels"
        val nodes = root.findAccessibilityNodeInfosByText("Reels")

        // We're looking for:
        // android.widget.HorizontalScrollView: text='' desc=''
        //   android.widget.LinearLayout: text='' desc='Reels'
        //     android.widget.TextView: text='Reels' desc=''
        //   android.widget.LinearLayout: text='' desc='Friends'
        //     android.widget.TextView: text='Friends' desc=''

        for (node in nodes) {
            val parent = node.parent
            if (parent.className != "android.widget.LinearLayout") {
                continue
            }
            if (parent.contentDescription != "Reels") {
                continue
            }

            return true
        }

        return false
    }

    override fun onInterrupt() {}
}
