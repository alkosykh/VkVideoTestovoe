package test.java;

import com.codeborne.selenide.WebDriverRunner;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.List;

import com.codeborne.selenide.WebDriverRunner;


public class BaseTest {
    protected static AndroidDriver driver;
    protected static WebDriverWait wait;
    protected static final String APP_PACKAGE = "com.vk.vkvideo";
    protected static final String APP_ACTIVITY = "com.vk.video.screens.main.MainActivity";
    protected static final String APPIUM_SERVER = "http://127.0.0.1:4723";
    protected static final Duration WAIT_TIMEOUT = Duration.ofSeconds(20);

    protected WebElement findVideoTile() {
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

    protected void checkVideoPlayback() throws Exception {
        var timerLocator = AppiumBy.id(APP_PACKAGE + ":id/current_progress");
        WebElement timerElement = wait.until(ExpectedConditions.visibilityOfElementLocated(timerLocator));

        String timeBefore = timerElement.getText();
        Thread.sleep(1000);
        String timeAfter = driver.findElement(timerLocator).getText();

        boolean isPlaying = !timeBefore.equals(timeAfter);
        if (!isPlaying) throw new AssertionError("Видео не воспроизводится");
    }

    protected void closePopups() {
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

    protected void handleTestFailure(Exception e) {
        try {
            if (driver != null && driver.getSessionId() != null) {
                byte[] screenshot = driver.getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
                Allure.addAttachment("Ошибка видео", "image/png", new ByteArrayInputStream(screenshot), "png");
            }
        } catch (Exception ignored) {}
        try { Allure.addAttachment("Исключение", "text/plain", e.toString()); } catch (Exception ignored) {}
    }
}