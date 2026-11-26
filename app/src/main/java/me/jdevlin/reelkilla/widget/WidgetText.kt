package me.jdevlin.reelkilla.widget

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

const val WIDGET_IDLE_TEXT = "Unlocked"

/**
 * Text shown on the home screen widget.
 *
 * - No active block (`nextOpenAt <= now`) → [WIDGET_IDLE_TEXT]
 * - Block ends today → end time only, e.g. "14:35"
 * - Block ends on a later day → weekday prefix, e.g. "Thu 09:10"
 *
 * [timeFormat] should come from `android.text.format.DateFormat.getTimeFormat(context)`
 * so it follows the device's 12h/24h setting. Its time zone is overridden with [timeZone].
 */
fun formatWidgetText(
    nextOpenAt: Long,
    now: Long,
    timeFormat: DateFormat,
    timeZone: TimeZone = TimeZone.getDefault(),
    locale: Locale = Locale.getDefault(),
): String {
    if (nextOpenAt <= now) {
        return WIDGET_IDLE_TEXT
    }

    timeFormat.timeZone = timeZone
    val time = timeFormat.format(Date(nextOpenAt))

    if (isSameDay(nextOpenAt, now, timeZone)) {
        return time
    }

    val weekdayFormat = SimpleDateFormat("EEE", locale).apply { this.timeZone = timeZone }
    return "${weekdayFormat.format(Date(nextOpenAt))} $time"
}

private fun isSameDay(a: Long, b: Long, timeZone: TimeZone): Boolean {
    val calA = Calendar.getInstance(timeZone).apply { timeInMillis = a }
    val calB = Calendar.getInstance(timeZone).apply { timeInMillis = b }
    return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
        calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
}
