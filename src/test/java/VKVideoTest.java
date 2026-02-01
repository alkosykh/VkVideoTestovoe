import com.codeborne.selenide.WebDriverRunner;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.time.Duration;
import java.util.List;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VKVideoTest {
    private static AndroidDriver driver;
    private static WebDriverWait wait;
    private static final String APP_PACKAGE = "com.vk.vkvideo";
    private static final String APP_ACTIVITY = "com.vk.video.screens.main.MainActivity";
    private static final String APPIUM_SERVER = "http://127.0.0.1:4723";
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(20);

    @BeforeAll
    static void setup() throws Exception {
        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName("Android")
                .setDeviceName("emulator-5554")
                .setAutomationName("UiAutomator2")
                .setAppPackage(APP_PACKAGE)
                .setAppActivity(APP_ACTIVITY)
                .setNoReset(true)
                .setAutoGrantPermissions(true);

        driver = new AndroidDriver(new URL(APPIUM_SERVER), options);
        driver.activateApp(APP_PACKAGE);
        WebDriverRunner.setWebDriver(driver);
        wait = new WebDriverWait(driver, WAIT_TIMEOUT);
    }

    @AfterAll
    static void tearDown() {
        if (driver != null) {
            driver.terminateApp(APP_PACKAGE);
            driver.quit();
        }
    }

    @Test
    @Order(1)
    void testVideoPlayback() {
        try {
            Allure.step("1. Ожидание загрузки главного экрана и закрытие всплывающих окон", () -> {
                Thread.sleep(3000);
                closePopups();
            });

            Allure.step("2. Клик по первому доступному видео", () -> {
                WebElement videoTile = findVideoTile();
                assertNotNull(videoTile, "Видео не найдено на экране");
                videoTile.click();
            });

            Allure.step("3. Ждем 2 сек, кликаем по видео (чтобы показать плеер) и меряем таймер", () -> {
                Thread.sleep(5000);

                try {
                    List<WebElement> frame = driver.findElements(AppiumBy.id(APP_PACKAGE + ":id/video_subtitles"));
                    if (!frame.isEmpty()) {
                        frame.get(0).click();
                    } else {
                        driver.findElement(AppiumBy.xpath("//android.widget.FrameLayout[@resource-id=\"com.vk.vkvideo:id/video_subtitles\"]")).click();
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    // System.out.println("Не удалось кликнуть по video_subtitles: " + e.getMessage());
                }
                checkVideoPlayback();
            });

        } catch (Exception e) {
            handleTestFailure(e);
            throw e;
        }
    }

    private WebElement findVideoTile() {
        try {
            var videoTile = driver.findElements(
                    AppiumBy.xpath("//android.widget.ImageView[@resource-id=\"com.vk.vkvideo:id/preview\"]"));
            
            if (!videoTile.isEmpty()) {
                return videoTile.get(0);
            }

            var fallbackTile = driver.findElements(
                    AppiumBy.androidUIAutomator("new UiSelector().className(\"android.view.ViewGroup\").clickable(true).instance(0)"));
            
            return fallbackTile.isEmpty() ? null : fallbackTile.get(0);
        } catch (Exception e) {
            // System.out.println("Ошибка при поиске видео: " + e.getMessage());
            return null;
        }
    }

    private void checkVideoPlayback() throws Exception {
        By timerLocator = AppiumBy.id(APP_PACKAGE + ":id/current_progress");
        WebElement timerElement = wait.until(ExpectedConditions.visibilityOfElementLocated(timerLocator));

        String timeBefore = timerElement.getText();
        // System.out.println("Таймер (начало): " + timeBefore);

        Thread.sleep(1000);

        String timeAfter = timerElement.getText();;
        // System.out.println("Таймер (через 2 сек): " + timeAfter);

        boolean isPlaying = !timeBefore.equals(timeAfter);
        assertTrue(isPlaying, "Видео не воспроизводится");
        // System.out.println("Видео воспроизводится - время отсчитывается");
    }

    private void clickPlayButton() {
        try {
            var playBtn = $(AppiumBy.xpath("//*[contains(@content-desc, 'Play') or contains(@content-desc, 'Воспроизвести') or contains(@resource-id, 'play')]"));
            if (playBtn.exists()) {
                playBtn.click();
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            // System.out.println("Ошибка при клике Play: " + e.getMessage());
        }
    }

    private void clickCenterOfScreen() {
        try {
            $(AppiumBy.className("android.widget.FrameLayout")).click();
        } catch (Exception e) {
            // System.out.println("Ошибка при клике на центр экрана: " + e.getMessage());
        }
    }

    private void closePopups() {
        String[] popupXpaths = {
                "//android.widget.ImageView[@resource-id='" + APP_PACKAGE + ":id/close_btn_left']",
                "//android.widget.Button[@text='Закрыть']",
                "//android.widget.ImageView[@content-desc='Закрыть']",
                "//android.widget.Button[@resource-id='android:id/button2']",
                "//*[contains(@text, 'Позже')]",
                "//*[contains(@text, 'Skip')]",
                "//android.widget.Button[@resource-id='" + APP_PACKAGE + ":id/secondary_button']"
        };

        for (String xpath : popupXpaths) {
            try {
                List<WebElement> elements = driver.findElements(AppiumBy.xpath(xpath));
                if (!elements.isEmpty() && elements.get(0).isDisplayed()) {
                    elements.get(0).click();
                    // System.out.println("Закрыто окно: " + xpath);
                    break;
                }
            } catch (NoSuchElementException e) {
            }
        }
    }

    private void handleTestFailure(Exception e) {
        Allure.step("Обработка ошибки теста", () -> {
            // System.out.println("Ошибка теста: " + e.getMessage());
            
            byte[] screenshot = driver.getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
            Allure.addAttachment("Ошибка видео", "image/png", 
                    new ByteArrayInputStream(screenshot), "png");
            
            Allure.addAttachment("Исключение", "text/plain", e.toString());
            
            if (e.getMessage().contains("timeout")) {
                // System.out.println("Таймаут при воспроизведении");
            }
        });
    }
}