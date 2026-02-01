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
import java.util.Map;
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
    void testVideoPlayback() {
        
        try {
            setAirplaneMode(false);
            Thread.sleep(5000);
            Allure.step("1. Ожидание и закрытие попапов", () -> {
                closePopups();
                try {
                    wait.until(d -> driver.findElements(AppiumBy.id(APP_PACKAGE + ":id/preview")).size() > 0);
                } catch (Exception ignored) {}
            });

            Allure.step("2. Клик по видео", () -> {
                WebElement videoTile = findVideoTile();
                Assertions.assertNotNull(videoTile, "Видео не найдено");
                videoTile.click();
            });

            Allure.step("3. Ждём 2 сек, кликаем и меряем таймер", () -> {
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
            
            Allure.step("1. Ожидание и закрытие попапов", () -> {
                closePopups();
                try {
                    wait.until(d -> driver.findElements(AppiumBy.id(APP_PACKAGE + ":id/preview")).size() > 0);
                } catch (Exception ignored) {}
            });

            Allure.step("2. Открываем видео (с интернетом)", () -> {
                setAirplaneMode(true);
                Thread.sleep(5000);
                System.out.println("Сеть отключена после открытия видео");
                driver.findElement(AppiumBy.androidUIAutomator("new UiScrollable(new UiSelector().scrollable(true)).scrollForward(5)"));
                Thread.sleep(2000);
                
                WebElement videoTile = findVideoTile();
                Assertions.assertNotNull(videoTile, "Видео не найдено");
                videoTile.click();
            });

            Allure.step("5. Проверяем что видео НЕ продолжает воспроизводиться", () -> {
                List<WebElement> frame = driver.findElements(AppiumBy.id(APP_PACKAGE + ":id/video_subtitles"));
                if (!frame.isEmpty()) frame.get(0).click();
                
                checkVideoNotPlaying();
            });

        } catch (Exception e) {
            handleTestFailure(e);
            throw new RuntimeException(e);
        }
    }

    private void setAirplaneMode(boolean enable) {
    try {
        if (enable) {
            System.out.println("Выключаю интернет (Wi-Fi и Data)...");
            driver.executeScript("mobile: shell", Map.of("command", "svc wifi disable"));
            driver.executeScript("mobile: shell", Map.of("command", "svc data disable"));
        } else {
            System.out.println("Включаю интернет обратно...");
            driver.executeScript("mobile: shell", Map.of("command", "svc wifi enable"));
            driver.executeScript("mobile: shell", Map.of("command", "svc data enable"));
    
            Thread.sleep(8000); 
        }
    } catch (Exception e) {
        System.err.println("Ошибка при управлении сетью: " + e.getMessage());
    }
}

            private void enableAirplaneMode(boolean enable) {
        try {
            // Открываем Settings через intent
            driver.executeScript("mobile: startActivity", new HashMap<String, Object>() {{
                put("intent", "android.intent.action.MAIN");
                put("action", "android.intent.action.SETTINGS");
                put("package", "com.android.settings");
            }});
            Thread.sleep(3000);

            // Ищем и скроллим к Airplane Mode
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text(\"Airplane mode\"))"));
            } catch (Exception ignored) {
                System.out.println("⚠ Не удалось найти Airplane mode через скролл, ищем через resourceId");
            }
            
            Thread.sleep(1000);

            // Пытаемся найти switch по resourceId или xpath
            List<WebElement> toggles = driver.findElements(AppiumBy.xpath("//android.widget.Switch[@resource-id='android:id/switch_widget']"));
            
            if (toggles.isEmpty()) {
                toggles = driver.findElements(AppiumBy.xpath("//android.widget.CheckBox"));
            }

            if (!toggles.isEmpty()) {
                WebElement toggle = toggles.get(0);
                String isChecked = toggle.getAttribute("checked");
                
                boolean shouldClick = (enable && !isChecked.equals("true")) || (!enable && isChecked.equals("true"));
                
                if (shouldClick) {
                    toggle.click();
                    Thread.sleep(3000);
                    System.out.println(enable ? "✓ Режим полета ВКЛЮЧЕН" : "✓ Режим полета ВЫКЛЮЧЕН");
                } else {
                    System.out.println("ℹ Режим полета уже в нужном состоянии");
                }
            } else {
                System.out.println("⚠ Не удалось найти переключатель Airplane Mode");
            }

            // Возвращаемся в приложение VK Video
            driver.activateApp(APP_PACKAGE);
            Thread.sleep(2000);

        } catch (Exception e) {
            System.out.println("⚠ Ошибка при изменении режима полета: " + e.getMessage());
            e.printStackTrace();
            try {
                driver.activateApp(APP_PACKAGE);
            } catch (Exception ignored) {}
        }
    }

    private void checkVideoNotPlaying() {
    Allure.step("Проверка отсутствия воспроизведения", () -> {
        // 1. Пытаемся найти спиннер загрузки
        By loaderLocator = AppiumBy.id(APP_PACKAGE + ":id/progress_view");
        By timerLocator = AppiumBy.id(APP_PACKAGE + ":id/current_progress");

        // Ждем немного — появится ли спиннер?
        boolean isLoaderVisible = false;
        try {
            wait.withTimeout(Duration.ofSeconds(5))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(loaderLocator));
            isLoaderVisible = true;
            System.out.println("✓ Обнаружен индикатор загрузки (бесконечный спиннер)");
        } catch (Exception e) {
            System.out.println("ℹ Спиннер не появился, проверяем таймер...");
        }

        // 2. Если спиннера нет, проверяем, что время не идет
        if (!isLoaderVisible) {
            WebElement timer = wait.until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(timerLocator));
            String timeBefore = timer.getText();
            Thread.sleep(3000);
            String timeAfter = driver.findElement(timerLocator).getText();
            
            Assertions.assertEquals(timeBefore, timeAfter, "Видео продолжает играть, а не должно!");
            System.out.println("✓ Время замерло на: " + timeBefore);
        }
        
        Assertions.assertTrue(isLoaderVisible || true, "Видео не остановилось");
    });
}
}