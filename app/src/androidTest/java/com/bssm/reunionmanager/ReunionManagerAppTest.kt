package com.bssm.reunionmanager

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class ReunionManagerAppTest {
    private lateinit var device: UiDevice
    private lateinit var application: ReunionManagerApplication

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        application = ApplicationProvider.getApplicationContext()
        application.appContainer.database.clearAllTables()
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
        findTextWithScroll("Open AI settings").click()

        waitForText("Local-only configuration")
        assertNotNull(device.findObject(By.text("Gemini API key")))
        assertNotNull(device.findObject(By.text("Save local settings")))
    }

    @Test
    fun importedConversation_canBeBrowsedAndAnalyzed() {
        runBlocking {
            application.appContainer.importConversationUseCase(
                sourceName = "sample.txt",
                rawText = sampleConversation,
            )
        }

        launchMainActivity()

        waitForText("Browse saved chats").click()
        waitForText("샘플 채팅방").click()
        waitForText("Open reunion plan").click()
        waitForText("Generate reunion plan").click()
        waitForText("Relationship summary")
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

    private fun findTextWithScroll(text: String): UiObject2 {
        repeat(5) {
            waitForTextOrNull(text)?.let { return it }
            device.swipe(
                device.displayWidth / 2,
                (device.displayHeight * 0.8).toInt(),
                device.displayWidth / 2,
                (device.displayHeight * 0.2).toInt(),
                20,
            )
            device.waitForIdle()
        }
        throw AssertionError("Did not find text after scrolling: $text")
    }

    private fun waitForTextOrNull(text: String): UiObject2? =
        device.wait(Until.findObject(By.text(text)), 2_000)

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

    private companion object {
        val sampleConversation = """
            샘플 채팅방 카카오톡 대화
            저장한 날짜 : 2024-04-05 01:36:14

            --------------- 2024년 3월 27일 수요일 ---------------
            [민지] [오전 10:55] 안녕
            [현우] [오전 10:56] 오랜만이야
        """.trimIndent()
    }
}
