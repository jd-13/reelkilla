package me.jdevlin.reelkilla.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.widget.RemoteViews
import me.jdevlin.reelkilla.NextOpenTimeStore
import me.jdevlin.reelkilla.R

/**
 * 1x1 home screen widget showing when the Instagram block ends. Tapping it adds 30 minutes.
 *
 * While a block is active, a single alarm is kept scheduled for its end so the widget
 * switches back to [WIDGET_IDLE_TEXT].
 *
 * Called by the system after the widget is placed, after boot and after an app update.
 */
class ReelkillaWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        refreshAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ADD_30_MIN -> {
                NextOpenTimeStore.advanceNextOpenAt(context, ADD_DURATION_MS)
                refreshAll(context)
            }
            ACTION_REFRESH -> refreshAll(context)
            else -> super.onReceive(context, intent)
        }
    }

    override fun onDisabled(context: Context) {
        // Last widget removed.
        cancelExpiryAlarm(context)
    }

    companion object {
        internal const val ACTION_ADD_30_MIN = "me.jdevlin.reelkilla.widget.ADD_30_MIN"
        internal const val ACTION_REFRESH = "me.jdevlin.reelkilla.widget.REFRESH"
        internal const val ADD_DURATION_MS = 30L * 60L * 1000L

        private const val REQUEST_ADD_30_MIN = 0
        private const val REQUEST_REFRESH = 1

        /** Re-renders every placed instance of the widget from the stored block end time. */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ReelkillaWidget::class.java))
            if (ids.isEmpty()) {
                cancelExpiryAlarm(context)
                return
            }

            val nextOpenAt = NextOpenTimeStore.getNextOpenAt(context)
            val now = System.currentTimeMillis()
            val text = formatWidgetText(
                nextOpenAt = nextOpenAt,
                now = now,
                timeFormat = DateFormat.getTimeFormat(context)
            )

            val views = RemoteViews(context.packageName, R.layout.widget_reelkilla).apply {
                setTextViewText(R.id.widget_text, text)
                setOnClickPendingIntent(android.R.id.background, add30MinIntent(context))
            }
            manager.updateAppWidget(ids, views)

            if (nextOpenAt > now) {
                scheduleExpiryAlarm(context, nextOpenAt)
            } else {
                cancelExpiryAlarm(context)
            }
        }

        /**
         * Inexact and non-waking: needs no exact-alarm permission, and if the device is asleep
         * the alarm fires on next wake, which is the first time anyone could see the widget.
         * Reusing the same PendingIntent replaces any previously scheduled alarm.
         */
        private fun scheduleExpiryAlarm(context: Context, triggerAt: Long) {
            context.getSystemService(AlarmManager::class.java)
                .set(AlarmManager.RTC, triggerAt, refreshIntent(context))
        }

        private fun cancelExpiryAlarm(context: Context) {
            context.getSystemService(AlarmManager::class.java).cancel(refreshIntent(context))
        }

        private fun refreshIntent(context: Context): PendingIntent {
            val intent = Intent(context, ReelkillaWidget::class.java).setAction(ACTION_REFRESH)
            return PendingIntent.getBroadcast(
                context,
                REQUEST_REFRESH,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        private fun add30MinIntent(context: Context): PendingIntent {
            val intent = Intent(context, ReelkillaWidget::class.java).setAction(ACTION_ADD_30_MIN)
            return PendingIntent.getBroadcast(
                context,
                REQUEST_ADD_30_MIN,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}
