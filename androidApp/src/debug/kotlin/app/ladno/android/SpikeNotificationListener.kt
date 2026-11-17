package app.ladno.android

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class SpikeNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        Log.i(TAG, "connected")
        val active = try {
            activeNotifications
        } catch (_: SecurityException) {
            Log.i(TAG, "active_unavailable")
            return
        }
        active.orEmpty().forEach { inspect(it, "active") }
    }

    override fun onListenerDisconnected() {
        Log.i(TAG, "disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        inspect(sbn, "posted")
    }

    private fun inspect(sbn: StatusBarNotification, delivery: String) {
        if (sbn.packageName != NOTIFIER_PACKAGE) {
            Log.i(TAG, "$delivery package_rejected")
            return
        }

        val notification = sbn.notification
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            Log.i(TAG, "$delivery group_summary_skipped")
            return
        }

        val result = try {
            classify(notification.extras)
        } catch (_: RuntimeException) {
            // Bundle unparceling can fail. Never log the exception or the payload.
            "extras_unreadable"
        }
        Log.i(TAG, "$delivery $result")
    }

    private fun classify(extras: Bundle?): String {
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)
        val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
        val lines = extras?.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)

        return when {
            lines?.size == 2 && lines[0].matchesSample("T02 line 1") &&
                lines[1].matchesSample("T02 line 2") -> "textLines_match"
            bigText.matchesSample("T02 expanded text") && text.matchesSample("T02 preview") -> "bigText_match"
            text.matchesSample("T02 text") -> "text_match"
            text.matchesSample("T02 update before") -> "update_before_match"
            text.matchesSample("T02 update after") -> "update_after_match"
            text.matchesSample("T02 group child") -> "group_child_match"
            text.isNullOrEmpty() && bigText.isNullOrEmpty() && lines.isNullOrEmpty() -> "redacted_or_empty"
            else -> "unrecognized"
        }
    }

    // Compare only the fixed fixture length without copying or retaining text.
    private fun CharSequence?.matchesSample(sample: String): Boolean =
        this != null && length == sample.length && sample.indices.all { this[it] == sample[it] }

    private companion object {
        const val TAG = "LadnoT02"
        const val NOTIFIER_PACKAGE = "app.ladno.notifier"
    }
}
