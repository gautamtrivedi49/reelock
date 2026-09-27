package com.reelockapp.blocker

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Watches Instagram and YouTube for the specific screens that show Reels /
 * Shorts, and presses "back" the moment one is detected.
 *
 * HOW IT WORKS
 * Instagram and YouTube tag their UI elements with internal resource IDs
 * (e.g. "com.instagram.android:id/clips_viewer_view_pager"). This service
 * scans the on-screen view tree for IDs containing telltale substrings like
 * "reel", "clips", or "shorts" and backs out when it finds one.
 *
 * LIMITATIONS (read this before relying on it)
 * - These resource IDs are internal implementation details that Instagram
 *   and YouTube can rename in any app update, silently breaking detection.
 *   If blocking stops working after an app update, the ID substrings below
 *   are the first thing to check/update (use a layout inspector to find the
 *   new ones).
 * - It only fires after a Reels/Shorts screen has already started to render,
 *   so there's a brief flash before it backs out.
 * - It cannot distinguish "Shorts recommended in the home feed" from the
 *   dedicated Shorts player in every YouTube version; behavior may vary
 *   across YouTube app versions.
 */
class ReelsBlockerService : AccessibilityService() {

    companion object {
        private const val TAG = "ReelsBlockerService"

        // Substrings (lowercase) that show up in resource IDs for Reels /
        // Shorts screens across the apps we watch. Extend this list if a
        // new app version stops being caught.
        private val BLOCKED_ID_SUBSTRINGS = listOf(
            "clips_viewer",        // Instagram Reels viewer
            "clips_swipe_refresh", // Instagram Reels feed container
            "reel_player",         // YouTube Shorts player
            "reel_recycler",       // YouTube Shorts shelf/recycler
            "reel_watch"           // YouTube Shorts watch fragment
        )

        private const val MIN_MS_BETWEEN_BACK_ACTIONS = 800L
        private const val MAX_NODES_TO_SCAN = 400
    }

    private var lastActionAtMs = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val root = rootInActiveWindow ?: return
        try {
            if (treeContainsBlockedScreen(root)) {
                maybeGoBack()
            }
        } catch (t: Throwable) {
            // Accessibility node trees can be mutated mid-traversal by the
            // app being inspected; never let that crash the service.
            Log.w(TAG, "Error scanning node tree", t)
        } finally {
            root.recycle()
        }
    }

    private fun treeContainsBlockedScreen(root: AccessibilityNodeInfo): Boolean {
        var nodesScanned = 0
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)

        while (stack.isNotEmpty() && nodesScanned < MAX_NODES_TO_SCAN) {
            val node = stack.removeLast()
            nodesScanned++

            val resourceId = node.viewIdResourceName?.lowercase()
            if (resourceId != null && BLOCKED_ID_SUBSTRINGS.any { resourceId.contains(it) }) {
                return true
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { stack.addLast(it) }
            }
        }
        return false
    }

    private fun maybeGoBack() {
        val now = System.currentTimeMillis()
        if (now - lastActionAtMs < MIN_MS_BETWEEN_BACK_ACTIONS) return
        lastActionAtMs = now
        performGlobalAction(GLOBAL_ACTION_BACK)
        StatsManager.recordBlock(applicationContext)
    }

    override fun onInterrupt() {
        // No cleanup needed.
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "ReelsBlockerService connected and watching for Reels/Shorts")
    }
}
