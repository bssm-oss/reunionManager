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
import com.bssm.reunionmanager.domain.model.ProviderSettings
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        waitForText("재회 플랜")
        assertNotNull(device.findObject(By.text("재회 가능성을\n높이는 플랜")))
        assertNotNull(device.findObject(By.text("카톡 내용 불러오기")))
        assertNull(device.findObject(By.text("AI로 정리")))
        assertNull(device.findObject(By.text("내 기기에서 보관")))
    }

    @Test
    fun homeScreen_navigatesToImportScreen() {
        clickText("카톡 내용 불러오기")

        waitForText("카톡 내용을 불러오면 내 상황에 맞는 재회 플랜을 만들 수 있어요.")
        assertNotNull(device.findObject(By.text("카톡 내용 불러오기")))
    }

    @Test
    fun homeScreen_prioritizesSavedConversationsWhenConversationExists() {
        runBlocking {
            application.appContainer.importConversationUseCase(
                sourceName = "sample.txt",
                rawText = sampleConversation,
            )
        }

        launchMainActivity()

        waitForText("지난 플랜")
        clickText("지난 플랜")
        waitForText("샘플 채팅방")
    }

    @Test
    fun homeScreen_navigatesToSettingsScreen() {
        clickText("설정")

        waitForText("내 카톡 이름")
        waitForText("데이터 보관")
        waitForText("기술 정보")
        if (openRouterConfigured()) {
            waitForText("OpenRouter로 플랜을 만들고, 필요한 대화 일부만 전송됩니다.")
            waitForText("OpenRouter 연결이 없을 때 쓰는 선택 기능입니다.")
        } else {
            waitForText("모델 파일")
            waitForText("선택한 파일은 앱 안에만 보관됩니다.")
        }
        assertNotNull(device.findObject(By.text("모델 파일 선택")))
        assertNotNull(device.findObject(By.text(if (openRouterConfigured()) "외부 연결" else "기본 정리")))
    }

    @Test
    fun settingsScreen_showsModelCheckWhenModelIsConfigured() {
        runBlocking {
            application.appContainer.providerSettingsRepository.save(
                ProviderSettings(
                    modelPath = "/data/local/tmp/gemma-4-E4B-it.litertlm",
                    modelName = "gemma-4-E4B-it.litertlm",
                    userDisplayName = "현우",
                ),
            )
        }

        launchMainActivity()

        clickText("설정")
        if (openRouterConfigured()) {
            waitForText("기술 정보")
            waitForText("OpenRouter로 플랜을 만들고, 필요한 대화 일부만 전송됩니다.")
            waitForText("외부 연결")
        } else {
            waitForText("기술 정보")
            waitForText("모델 파일은 저장됐고, 실행 전에는 기본 정리로 진행합니다.")
            waitForText("점검 전")
        }
        waitForText("실행 점검")
    }

    @Test
    fun settingsScreen_showsVerifiedModelStateWhenModelWasChecked() {
        val modelPath = "/data/local/tmp/gemma-4-E4B-it.litertlm"
        runBlocking {
            application.appContainer.providerSettingsRepository.save(
                ProviderSettings(
                    modelPath = modelPath,
                    modelName = "gemma-4-E4B-it.litertlm",
                    userDisplayName = "현우",
                    verifiedModelPath = modelPath,
                    verifiedAtEpochMillis = 100L,
                ),
            )
        }

        launchMainActivity()

        clickText("설정")
        if (openRouterConfigured()) {
            waitForText("기술 정보")
            waitForText("외부 연결")
        } else {
            waitForText("기술 정보")
            waitForText("선택한 모델 파일의 실행을 확인했습니다.")
            waitForText("기기 모델 준비")
        }
        waitForText("다시 점검")
    }

    @Test
    fun importedConversation_requiresUserNameBeforeAnalysis() {
        runBlocking {
            application.appContainer.importConversationUseCase(
                sourceName = "sample.txt",
                rawText = sampleConversation,
            )
        }

        launchMainActivity()

        clickText("지난 플랜")
        clickText("샘플 채팅방")
        clickText("플랜 만들기")
        waitForText("내 이름 확인")
        waitForText("현우 선택")
        waitForText("민지 선택")
        waitForText("직접 입력하기")
        assertNull(device.findObject(By.text("오랜만이야. 괜찮다면 짧게 안부만 묻고 싶어.")))
    }

    @Test
    fun importedConversation_detailPrioritizesActionAndRecentMessages() {
        runBlocking {
            application.appContainer.importConversationUseCase(
                sourceName = "long-sample.txt",
                rawText = longConversation,
            )
        }

        launchMainActivity()

        clickText("지난 플랜")
        clickText("긴 샘플 채팅방")
        waitForText("아직 플랜이 없어요")
        waitForText("최근 메시지")
        waitForText("최근 8개만 먼저 보기")
        waitForText("전체 메시지 보기")
        clickText("전체 메시지 보기")
        waitForText("전체 메시지")
    }

    @Test
    fun importedConversation_canBeBrowsedAndAnalyzedAfterUserNameIsSaved() {
        runBlocking {
            application.appContainer.providerSettingsRepository.save(
                ProviderSettings(userDisplayName = "현우"),
            )
            application.appContainer.importConversationUseCase(
                sourceName = "sample.txt",
                rawText = sampleConversation,
            )
        }

        launchMainActivity()

        clickText("지난 플랜")
        clickText("샘플 채팅방")
        clickText("플랜 만들기")
        waitForText("플랜 만들기")
        tapAnalysisPrimaryAction()
        val timeoutMillis = if (openRouterConfigured()) 70_000L else 20_000L
        waitForText("내 플랜", timeoutMillis = timeoutMillis)
        waitForText("목표", timeoutMillis = timeoutMillis)
        waitForText("다음 선택", timeoutMillis = timeoutMillis)
        waitForText("지켜둘 선", timeoutMillis = timeoutMillis)
        if (!openRouterConfigured()) {
            waitForText("대화를 다시 열 수 있는 구간이에요.", timeoutMillis = timeoutMillis)
            waitForText("보내기 전 한 번 더 쉬어가도 괜찮아요.", timeoutMillis = timeoutMillis)
        }
        findTextWithScroll("재회 회복 맵")
        findTextWithScroll("대화 열기")
        findTextWithScroll("상대 반응")
    }

    @Test
    fun importedConversation_withUncheckedModelStillUsesBasicAnalysis() {
        runBlocking {
            application.appContainer.providerSettingsRepository.save(
                ProviderSettings(
                    modelPath = "/data/local/tmp/gemma-4-E4B-it.litertlm",
                    modelName = "gemma-4-E4B-it.litertlm",
                    userDisplayName = "현우",
                ),
            )
            application.appContainer.importConversationUseCase(
                sourceName = "unchecked-model.txt",
                rawText = sampleConversation,
            )
        }

        launchMainActivity()

        waitForText("재회 플랜")
        clickText("지난 플랜")
        clickText("샘플 채팅방")
        clickText("플랜 만들기")
        waitForText("플랜 만들기")
        if (!openRouterConfigured()) {
            assertNull(device.findObject(By.text("기기에서 정리하기")))
        }
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

    private fun waitForTextContaining(text: String, timeoutMillis: Long = 10_000) =
        device.wait(Until.findObject(By.textContains(text)), timeoutMillis)
            ?: throw AssertionError("Did not find text containing: $text")

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
        repeat(6) {
            device.waitForIdle()
            val target = waitForText(text, timeoutMillis)
            if (clickObject(target)) return
            Thread.sleep(250)
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

    private fun tapAnalysisPrimaryAction() {
        device.click(
            device.displayWidth / 2,
            (device.displayHeight * 0.203f).toInt(),
        )
        device.waitForIdle()
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

    private fun openRouterConfigured(): Boolean = BuildConfig.OPENROUTER_API_KEY.isNotBlank()

    private companion object {
        val sampleConversation = """
            샘플 채팅방 카카오톡 대화
            저장한 날짜 : 2024-04-05 01:36:14

            --------------- 2024년 3월 27일 수요일 ---------------
            [현우] [오전 10:55] 오랜만이야
            [민지] [오전 10:56] 나도 가끔 생각났어
            [민지] [오전 10:57] 괜찮다면 천천히 이야기해도 돼
        """.trimIndent()

        val longConversation = """
            긴 샘플 채팅방 카카오톡 대화
            저장한 날짜 : 2024-04-05 01:36:14

            --------------- 2024년 3월 27일 수요일 ---------------
            [현우] [오전 10:51] 첫 번째 메시지
            [민지] [오전 10:52] 두 번째 메시지
            [현우] [오전 10:53] 세 번째 메시지
            [민지] [오전 10:54] 네 번째 메시지
            [현우] [오전 10:55] 다섯 번째 메시지
            [민지] [오전 10:56] 여섯 번째 메시지
            [현우] [오전 10:57] 일곱 번째 메시지
            [민지] [오전 10:58] 여덟 번째 메시지
            [현우] [오전 10:59] 아홉 번째 메시지
            [민지] [오전 11:00] 열 번째 메시지
        """.trimIndent()
    }
}
