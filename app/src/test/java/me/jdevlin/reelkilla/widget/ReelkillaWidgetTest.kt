package me.jdevlin.reelkilla.widget

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import me.jdevlin.reelkilla.NextOpenTimeStore
import me.jdevlin.reelkilla.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReelkillaWidgetTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val thirtyMin = ReelkillaWidget.ADD_DURATION_MS

    private val alarms get() = shadowOf(context.getSystemService(AlarmManager::class.java))

    private fun placeWidget() {
        shadowOf(AppWidgetManager.getInstance(context))
            .createWidget(ReelkillaWidget::class.java, R.layout.widget_reelkilla)
    }

    private fun tap() {
        ReelkillaWidget().onReceive(context, Intent(ReelkillaWidget.ACTION_ADD_30_MIN))
    }

    @Test
    fun tapWithNoActiveBlockStartsThirtyMinuteBlock() {
        NextOpenTimeStore.setNextOpenAt(context, 0L)

        val before = System.currentTimeMillis()
        tap()
        val after = System.currentTimeMillis()

        val stored = NextOpenTimeStore.getNextOpenAt(context)
        assertTrue(
            "block should end 30 min from now",
            stored in (before + thirtyMin)..(after + thirtyMin)
        )
    }

    @Test
    fun tapWithActiveBlockExtendsItByThirtyMinutes() {
        val existing = System.currentTimeMillis() + 60L * 60L * 1000L
        NextOpenTimeStore.setNextOpenAt(context, existing)

        tap()

        assertEquals(existing + thirtyMin, NextOpenTimeStore.getNextOpenAt(context))
    }

    @Test
    fun repeatedTapsStack() {
        NextOpenTimeStore.setNextOpenAt(context, 0L)

        tap()
        val first = NextOpenTimeStore.getNextOpenAt(context)
        tap()

        assertEquals(first + thirtyMin, NextOpenTimeStore.getNextOpenAt(context))
    }

    @Test
    fun otherActionsDoNotChangeBlock() {
        NextOpenTimeStore.setNextOpenAt(context, 0L)

        ReelkillaWidget().onReceive(context, Intent("some.other.ACTION"))

        assertEquals(0L, NextOpenTimeStore.getNextOpenAt(context))
    }

    @Test
    fun activeBlockSchedulesAlarmAtBlockEnd() {
        val end = System.currentTimeMillis() + 60L * 60L * 1000L
        NextOpenTimeStore.setNextOpenAt(context, end)
        placeWidget()

        ReelkillaWidget.refreshAll(context)

        val alarm = alarms.peekNextScheduledAlarm()
        assertEquals(end, alarm?.triggerAtMs)
        assertEquals(1, alarms.scheduledAlarms.size)
    }

    @Test
    fun tapReschedulesAlarmToNewEnd() {
        NextOpenTimeStore.setNextOpenAt(context, 0L)
        placeWidget()

        tap()
        tap()

        assertEquals(1, alarms.scheduledAlarms.size)
        assertEquals(NextOpenTimeStore.getNextOpenAt(context), alarms.peekNextScheduledAlarm()?.triggerAtMs)
    }

    @Test
    fun noActiveBlockCancelsAlarm() {
        NextOpenTimeStore.setNextOpenAt(context, System.currentTimeMillis() + 60_000L)
        placeWidget()
        ReelkillaWidget.refreshAll(context)

        NextOpenTimeStore.setNextOpenAt(context, 0L)
        ReelkillaWidget.refreshAll(context)

        assertNull(alarms.peekNextScheduledAlarm())
    }

    @Test
    fun refreshBroadcastWithExpiredBlockCancelsAlarm() {
        NextOpenTimeStore.setNextOpenAt(context, System.currentTimeMillis() + 60_000L)
        placeWidget()
        ReelkillaWidget.refreshAll(context)

        NextOpenTimeStore.setNextOpenAt(context, System.currentTimeMillis() - 1L)
        ReelkillaWidget().onReceive(context, Intent(ReelkillaWidget.ACTION_REFRESH))

        assertNull(alarms.peekNextScheduledAlarm())
    }

    @Test
    fun noWidgetsPlacedSchedulesNoAlarm() {
        NextOpenTimeStore.setNextOpenAt(context, System.currentTimeMillis() + 60_000L)

        ReelkillaWidget.refreshAll(context)

        assertNull(alarms.peekNextScheduledAlarm())
    }

    @Test
    fun removingLastWidgetCancelsAlarm() {
        NextOpenTimeStore.setNextOpenAt(context, System.currentTimeMillis() + 60_000L)
        placeWidget()
        ReelkillaWidget.refreshAll(context)

        ReelkillaWidget().onDisabled(context)

        assertNull(alarms.peekNextScheduledAlarm())
    }
}
