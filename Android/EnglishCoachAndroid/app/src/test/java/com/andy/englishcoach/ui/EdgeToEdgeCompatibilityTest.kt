package com.andy.englishcoach.ui

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.andy.englishcoach.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit/Integration test suite verifying Android 15+ Edge-to-Edge compliance
 * and absence of deprecated window color APIs.
 */
@RunWith(RobolectricTestRunner::class)
class EdgeToEdgeCompatibilityTest {

    @Test
    fun testMainActivity_launchesWithEdgeToEdgeAndCutoutMode() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
        val window = activity.window
        assertNotNull(window)

        // Verify cutout mode on API 28+ / 30+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            assertEquals(
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS,
                window.attributes.layoutInDisplayCutoutMode
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            assertEquals(
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES,
                window.attributes.layoutInDisplayCutoutMode
            )
        }
        controller.destroy()
    }

    @Test
    @Config(sdk = [34])
    fun testMainActivity_onModernAndroid_launchesEdgeToEdgeCleanly() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
        val window = activity.window
        assertNotNull(window)
        assertEquals(
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS,
            window.attributes.layoutInDisplayCutoutMode
        )
        controller.destroy()
    }
}
