package com.bssm.reunionmanager

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
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
        waitForText("재회 매니저")
        assertNotNull(device.findObject(By.text("내 기기에서 보관")))
        assertNotNull(device.findObject(By.text("결과")))
        assertNotNull(device.findObject(By.text("카카오톡 대화 가져오기")))
    }

    @Test
    fun homeScreen_navigatesToImportScreen() {
        clickText("카카오톡 대화 가져오기")

        waitForText("카카오톡 대화 가져오기")
        assertNotNull(device.findObject(By.text("대화 파일 선택")))
    }

    @Test
    fun homeScreen_navigatesToSettingsScreen() {
        clickTextWithScroll("로컬 AI 설정")

        waitForText("모델 파일")
        assertNotNull(device.findObject(By.text("모델 파일 선택")))
        assertNotNull(device.findObject(By.text("데모 모드")))
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

        clickText("저장한 대화 보기")
        clickText("샘플 채팅방")
        clickText("재회 계획 만들기")
        waitForText("행동 전 확인")
        waitForText("아직 만든 계획이 없습니다")
        clickText("재회 계획 만들기")
        waitForText("첫 연락 문장", timeoutMillis = 20_000)
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

    private fun waitForText(text: String, timeoutMillis: Long = 10_000) =
        device.wait(Until.findObject(By.text(text)), timeoutMillis)
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

    private fun clickText(text: String, timeoutMillis: Long = 10_000) {
        repeat(3) {
            val target = waitForText(text, timeoutMillis)
            if (clickObject(target)) return
            device.waitForIdle()
        }
        throw AssertionError("Could not click text: $text")
    }

    private fun clickTextWithScroll(text: String) {
        repeat(5) {
            waitForTextOrNull(text)?.let { target ->
                if (clickObject(target)) return
            }
            device.swipe(
                device.displayWidth / 2,
                (device.displayHeight * 0.8).toInt(),
                device.displayWidth / 2,
                (device.displayHeight * 0.2).toInt(),
                20,
            )
            device.waitForIdle()
        }
        throw AssertionError("Could not click text after scrolling: $text")
    }

    private fun clickObject(target: UiObject2): Boolean {
        return try {
            val bounds = target.visibleBounds
            device.click(bounds.centerX(), bounds.centerY())
            device.waitForIdle()
            true
        } catch (_: StaleObjectException) {
            false
        }
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
