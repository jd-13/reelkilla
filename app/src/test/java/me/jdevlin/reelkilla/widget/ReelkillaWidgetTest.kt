package me.jdevlin.reelkilla.widget

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import me.jdevlin.reelkilla.NextOpenTimeStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReelkillaWidgetTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val thirtyMin = ReelkillaWidget.ADD_DURATION_MS

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
}
