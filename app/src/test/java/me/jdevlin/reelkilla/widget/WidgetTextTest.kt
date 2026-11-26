package me.jdevlin.reelkilla.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class WidgetTextTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val locale = Locale.UK

    // Thursday 2026-11-26 10:00:00 UTC
    private val now = 1_795_687_200_000L
    private val minute = 60_000L
    private val hour = 60 * minute

    private fun format(nextOpenAt: Long, pattern: String = "HH:mm") =
        formatWidgetText(nextOpenAt, now, SimpleDateFormat(pattern, locale), utc, locale)

    @Test
    fun zeroMeansIdle() {
        assertEquals(WIDGET_IDLE_TEXT, format(0L))
    }

    @Test
    fun pastValueMeansIdle() {
        assertEquals(WIDGET_IDLE_TEXT, format(now - minute))
    }

    @Test
    fun exactlyNowMeansIdle() {
        assertEquals(WIDGET_IDLE_TEXT, format(now))
    }

    @Test
    fun laterTodayShowsTimeOnly() {
        assertEquals("10:30", format(now + 30 * minute))
    }

    @Test
    fun lastMinuteOfTodayShowsTimeOnly() {
        assertEquals("23:59", format(now + 13 * hour + 59 * minute))
    }

    @Test
    fun tomorrowShowsWeekdayPrefix() {
        assertEquals("Fri 09:10", format(now + 23 * hour + 10 * minute))
    }

    @Test
    fun midnightTomorrowShowsWeekdayPrefix() {
        assertEquals("Fri 00:00", format(now + 14 * hour))
    }

    @Test
    fun followsTwelveHourFormat() {
        assertEquals("2:35 pm", format(now + 4 * hour + 35 * minute, "h:mm a").lowercase())
    }
}
