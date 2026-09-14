package me.jdevlin.reelkilla

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NextOpenTimeStoreTest {
    @Test
    fun defaultValueIsEpochStart() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val value = NextOpenTimeStore.getNextOpenAt(context)

        assertTrue("default value should be epoch start", value == 0L)
    }

    @Test
    fun setValuePersistsAcrossReads() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expected = 1_700_000_000_000L

        NextOpenTimeStore.setNextOpenAt(context, expected)
        val actual = NextOpenTimeStore.getNextOpenAt(context)

        assertTrue("saved value should be returned on the next read", actual == expected)
    }

    @Test
    fun zeroValueMeansAvailableNow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        NextOpenTimeStore.setNextOpenAt(context, 0L)
        val value = NextOpenTimeStore.getNextOpenAt(context)

        assertTrue("zero should mean available now", value == 0L)
    }

    @Test
    fun advancingFromZeroMovesForwardFromNow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val before = System.currentTimeMillis()
        val value = NextOpenTimeStore.advanceNextOpenAt(context, 30L * 60L * 1000L)
        val after = System.currentTimeMillis()

        assertTrue("advanced value should be now + 30 minutes", value in (before + 30L * 60L * 1000L)..(after + 30L * 60L * 1000L))
    }
}
