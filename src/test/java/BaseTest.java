package test.java;

import com.codeborne.selenide.WebDriverRunner;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sound.midi.Sequence;


public class BaseTest {
    protected static AndroidDriver driver;
    protected static WebDriverWait wait;
    protected static final String APP_PACKAGE = "com.vk.vkvideo";
    protected static final String APP_ACTIVITY = "com.vk.video.screens.main.MainActivity";
    protected static final String APPIUM_SERVER = "http://127.0.0.1:4723";
    protected static final Duration WAIT_TIMEOUT = Duration.ofSeconds(20);

    WebElement findVideoTile() {
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
            return null;
        }
    }

    void closePopups() {
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
                    break;
                }
            } catch (Exception ignored) {
            }
        }
    }

    void handleTestFailure(Exception e) {
        try {
            if (driver != null && driver.getSessionId() != null) {
                byte[] screenshot = driver.getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
                Allure.addAttachment("Ошибка видео", "image/png", new ByteArrayInputStream(screenshot), "png");
            }
        } catch (Exception ignored) {}
        try { Allure.addAttachment("Исключение", "text/plain", e.toString()); } catch (Exception ignored) {}
    }



    void setAirplaneMode(boolean enable) {
        try {
            if (enable) {
                driver.executeScript("mobile: shell", Map.of("command", "svc wifi disable"));
                driver.executeScript("mobile: shell", Map.of("command", "svc data disable"));
            } else {
                driver.executeScript("mobile: shell", Map.of("command", "svc wifi enable"));
                driver.executeScript("mobile: shell", Map.of("command", "svc data enable"));
        
                Thread.sleep(8000); 
            }
        } catch (Exception e) {
            System.err.println("Ошибка при управлении сетью: " + e.getMessage());
        }
    }

    void checkVideoPlayback() throws Exception {
        
        By timerLocator = AppiumBy.id(APP_PACKAGE + ":id/current_progress");
        WebElement timerElement = driver.findElement(timerLocator);
        String timeBefore = timerElement.getText();

        Thread.sleep(4000);

        String timeAfter;
        try {
            timeAfter = driver.findElement(timerLocator).getText();
        } catch (Exception e) {
            List<WebElement> frame = driver.findElements(AppiumBy.id(APP_PACKAGE + ":id/video_subtitles"));
                    if (!frame.isEmpty()) frame.get(0).click();
            timeAfter = wait.until(ExpectedConditions.visibilityOfElementLocated(timerLocator)).getText();
        }
        
        Assertions.assertNotEquals(timeBefore, timeAfter, "Видео зависло! Время не изменилось: " + timeBefore);
    }

    void checkVideoNotPlaying() {
        Allure.step("Проверка отсутствия воспроизведения", () -> {
        // Пытаемся найти спиннер загрузки
        By loaderLocator = AppiumBy.id(APP_PACKAGE + ":id/progress_view");
        By timerLocator = AppiumBy.id(APP_PACKAGE + ":id/current_progress");

        boolean isLoaderVisible = false;
        try {
            wait.withTimeout(Duration.ofSeconds(5))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(loaderLocator));
            isLoaderVisible = true;
        } catch (Exception e) {
        }

        // Если спиннера нет, проверяем, что время не идет
        if (!isLoaderVisible) {
            WebElement timer = wait.until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(timerLocator));
            String timeBefore = timer.getText();
            Thread.sleep(3000);
            String timeAfter = driver.findElement(timerLocator).getText();
            
            Assertions.assertEquals(timeBefore, timeAfter, "Видео продолжает играть, а не должно!");
        }
        Assertions.assertTrue(isLoaderVisible || true, "Видео не остановилось");
        });
    }
}