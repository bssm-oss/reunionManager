package com.bssm.reunionmanager

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReunionManagerAppTest {
    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        launchMainActivity()
    }

    @Test
    fun homeScreen_showsCoreTrustSignals() {
        waitForText("Reunion Manager")
        assertNotNull(device.findObject(By.text("Local data only")))
        assertNotNull(device.findObject(By.text("Provider status")))
        assertNotNull(device.findObject(By.text("Import KakaoTalk .txt")))
    }

    @Test
    fun homeScreen_navigatesToImportScreen() {
        waitForText("Import KakaoTalk .txt").click()

        waitForText("Import a KakaoTalk export")
        assertNotNull(device.findObject(By.text("Choose .txt file")))
    }

    @Test
    fun homeScreen_navigatesToSettingsScreen() {
        waitForText("Open AI settings").click()

        waitForText("Local-only configuration")
        assertNotNull(device.findObject(By.text("Gemini API key")))
        assertNotNull(device.findObject(By.text("Save local settings")))
    }

    private fun launchMainActivity() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        ActivityScenario.launch<MainActivity>(intent)
        dismissBlockingDialogsIfNeeded()
        device.wait(Until.hasObject(By.pkg(context.packageName)), 10_000)
        device.waitForIdle()
    }

    private fun waitForText(text: String) =
        device.wait(Until.findObject(By.text(text)), 10_000)
            ?: throw AssertionError("Did not find text: $text")

    private fun dismissBlockingDialogsIfNeeded() {
        val waitButton = device.findObject(By.text("Wait"))
            ?: device.findObject(By.text("대기"))
        waitButton?.click()

        val closeButton = device.findObject(By.text("Close app"))
            ?: device.findObject(By.text("앱 닫기"))
        if (closeButton != null && device.findObject(By.pkg("com.bssm.reunionmanager")) == null) {
            waitButton?.click()
        }
    }
}
