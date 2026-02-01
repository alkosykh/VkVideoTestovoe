package test.java;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.*;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.util.Map;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.HashMap;
import com.codeborne.selenide.WebDriverRunner;
import io.appium.java_client.android.connection.ConnectionStateBuilder;
import io.appium.java_client.android.connection.ConnectionState;
import org.openqa.selenium.By;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VKVideoTest extends BaseTest {

    @BeforeEach
     void setup() throws Exception {
        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName("Android")
                .setDeviceName("emulator-5554")
                .setAutomationName("UiAutomator2")
                .setAppPackage(APP_PACKAGE)
                .setAppActivity(APP_ACTIVITY)
                .setNoReset(true)
                .setAutoGrantPermissions(true)
                .setNewCommandTimeout(Duration.ofSeconds(120));

        driver = new AndroidDriver(new URL(APPIUM_SERVER), options);
        driver.activateApp(APP_PACKAGE);
        WebDriverRunner.setWebDriver(driver);
        wait = new WebDriverWait(driver, WAIT_TIMEOUT);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.terminateApp(APP_PACKAGE);
            driver.quit();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Позитив: видео воспроизводится")
    void testVideoPlayback() {
        try {
            setAirplaneMode(false);
            Thread.sleep(5000);
            Allure.step("Ожидание и закрытие окон", () -> {
                closePopups();
                try {
                    wait.until(d -> driver.findElements(AppiumBy.id(APP_PACKAGE + ":id/preview")).size() > 0);
                } catch (Exception ignored) {}
            });

            Allure.step("Клик по видео", () -> {
                WebElement videoTile = findVideoTile();
                Assertions.assertNotNull(videoTile, "Видео не найдено");
                videoTile.click();
            });

            Allure.step("Меряем таймер", () -> {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                List<WebElement> frame = driver.findElements(AppiumBy.id(APP_PACKAGE + ":id/video_subtitles"));
                if (!frame.isEmpty()) frame.get(0).click();
                checkVideoPlayback();
            });

        } catch (Exception e) {
            handleTestFailure(e);
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(2)
    @DisplayName("Негатив: видео не воспроизводится после отключения интернета")
    void testVideoPlaybackOffline() {
        try {
            setAirplaneMode(false);
            Thread.sleep(5000);
            
            Allure.step("Ожидание и закрытие окон", () -> {
                closePopups();
                try {
                    wait.until(d -> driver.findElements(AppiumBy.id(APP_PACKAGE + ":id/preview")).size() > 0);
                } catch (Exception ignored) {}
            });

            Allure.step("Открываем видео (без интернета)", () -> {
                setAirplaneMode(true);
                Thread.sleep(5000);
                driver.findElement(AppiumBy.androidUIAutomator("new UiScrollable(new UiSelector().scrollable(true)).scrollForward(5)"));
                Thread.sleep(2000);
                
                WebElement videoTile = findVideoTile();
                Assertions.assertNotNull(videoTile, "Видео не найдено");
                videoTile.click();
            });

            Allure.step("Проверяем что видео не продолжает воспроизводиться", () -> {
                List<WebElement> frame = driver.findElements(AppiumBy.id(APP_PACKAGE + ":id/video_subtitles"));
                if (!frame.isEmpty()) frame.get(0).click();
                
                checkVideoNotPlaying();
            });

        } catch (Exception e) {
            handleTestFailure(e);
            throw new RuntimeException(e);
        }
    }


}