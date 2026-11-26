package me.jdevlin.reelkilla.widget

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
        if (intent.action == ACTION_ADD_30_MIN) {
            NextOpenTimeStore.advanceNextOpenAt(context, ADD_DURATION_MS)
            refreshAll(context)
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        internal const val ACTION_ADD_30_MIN = "me.jdevlin.reelkilla.widget.ADD_30_MIN"
        internal const val ADD_DURATION_MS = 30L * 60L * 1000L

        /** Re-renders every placed instance of the widget from the stored block end time. */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ReelkillaWidget::class.java))
            if (ids.isEmpty()) {
                return
            }

            val text = formatWidgetText(
                nextOpenAt = NextOpenTimeStore.getNextOpenAt(context),
                now = System.currentTimeMillis(),
                timeFormat = DateFormat.getTimeFormat(context)
            )

            val views = RemoteViews(context.packageName, R.layout.widget_reelkilla).apply {
                setTextViewText(R.id.widget_text, text)
                setOnClickPendingIntent(android.R.id.background, add30MinIntent(context))
            }
            manager.updateAppWidget(ids, views)
        }

        private fun add30MinIntent(context: Context): PendingIntent {
            val intent = Intent(context, ReelkillaWidget::class.java).setAction(ACTION_ADD_30_MIN)
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}
