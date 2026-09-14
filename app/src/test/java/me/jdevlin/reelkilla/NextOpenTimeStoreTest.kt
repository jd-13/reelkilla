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
}
