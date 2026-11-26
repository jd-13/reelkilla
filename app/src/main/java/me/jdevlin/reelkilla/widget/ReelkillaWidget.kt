package me.jdevlin.reelkilla.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.text.format.DateFormat
import android.widget.RemoteViews
import me.jdevlin.reelkilla.NextOpenTimeStore
import me.jdevlin.reelkilla.R

/**
 * 1x1 home screen widget showing when the Instagram block ends.
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

    companion object {
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
            }
            manager.updateAppWidget(ids, views)
        }
    }
}
