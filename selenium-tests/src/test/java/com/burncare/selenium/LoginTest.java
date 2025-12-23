package com.burncare.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;

import java.io.File;
import java.nio.file.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LoginTest {

    private static String appUrl;
    private WebDriver driver;

    private static final String USER_EMAIL = "mouad@gmail.com";
    private static final String USER_PASSWORD = "mouad1234";
    private static final String ADMIN_EMAIL = "admin@gmail.com";
    private static final String ADMIN_PASSWORD = "admin1234";
    private static final String UNAPPROVED_USER_EMAIL = "simo@gmail.com";
    private static final String UNAPPROVED_USER_PASSWORD = "simo1234";

    // ===================== SETUP / TEARDOWN (ONE WINDOW) =====================

    @BeforeAll
    void setupClass() {
        WebDriverManager.chromedriver().setup();
        appUrl = System.getProperty("app.url", "http://localhost:4200");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--window-size=1400,900");

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
    }

    @AfterAll
    void tearDown() {
        if (driver != null) driver.quit();
    }

    // ===================== UTILS =====================

    private WebDriverWait wait45() {
        return new WebDriverWait(driver, Duration.ofSeconds(45));
    }

    private void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private void waitDomReady(WebDriverWait wait) {
        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
    }

    private void clearSession() {
        try { driver.manage().deleteAllCookies(); } catch (Exception ignored) {}
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "window.localStorage.clear(); window.sessionStorage.clear();"
            );
        } catch (Exception ignored) {}
    }

    /** Always start each scenario from a clean /login state */
    private void openLoginClean(WebDriverWait wait) {
        driver.get(appUrl + "/login");
        waitDomReady(wait);
        sleep(300);
        clearSession();
        driver.navigate().refresh();
        waitDomReady(wait);
        sleep(300);
    }

    private WebElement emailInput(WebDriverWait wait) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[formcontrolname='email'], input[type='email']")
        ));
    }

    private WebElement passwordInput(WebDriverWait wait) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[formcontrolname='password'], input[type='password']")
        ));
    }

    private void clearAndType(WebElement el, String value) {
        el.click();
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        el.sendKeys(Keys.BACK_SPACE);
        el.sendKeys(value);
    }

    private void clickLoginButton(WebDriverWait wait) {
        WebElement btn = wait.until(d -> {
            for (WebElement b : d.findElements(By.cssSelector("button[type='submit'], button.btn-login"))) {
                if (b.isDisplayed() && b.isEnabled()) return b;
            }
            return null;
        });
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        sleep(500);
    }

    private void doLogin(WebDriverWait wait, String email, String pass) {
        clearAndType(emailInput(wait), email);
        clearAndType(passwordInput(wait), pass);
        clickLoginButton(wait);
    }

    private String getErrorMessage(WebDriverWait wait) {
        try {
            WebElement err = wait.until(d -> {
                try {
                    WebElement e = d.findElement(By.cssSelector(".error-message"));
                    if (e.isDisplayed() && !e.getText().trim().isEmpty()) return e;
                    return null;
                } catch (Exception ex) {
                    return null;
                }
            });
            return err.getText().trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String getPasswordValidationMessage(WebDriverWait wait) {
        try {
            WebElement pass = passwordInput(wait);
            WebElement formGroup = pass.findElement(By.xpath("./ancestor::div[contains(@class,'form-group')]"));
            WebElement msg = formGroup.findElement(By.cssSelector(".validation-message"));
            if (msg.isDisplayed()) {
                String t = msg.getText().trim();
                return t.isEmpty() ? null : t;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void saveArtifacts(String name) {
        try {
            Path outDir = Paths.get("target", "selenium-artifacts");
            Files.createDirectories(outDir);

            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(src.toPath(), outDir.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);

            Files.writeString(outDir.resolve(name + ".html"), driver.getPageSource());
        } catch (Exception ignored) {}
    }

    // ===================== ORDERED FLOW =====================

    @Test
    @DisplayName("SL-LOGIN — Strict order, end with USER logged in (single window)")
    void SL_Login_StrictOrder_EndUserLoggedIn() {

        WebDriverWait wait = wait45();
        List<String> failures = new ArrayList<>();

        record Scenario(String code, Runnable steps, Runnable checks) {}

        // ✅ Strict order + last step is USER login success
        List<Scenario> scenarios = List.of(

                // 1) Password required
                new Scenario("SL-VALIDATION Password Required",
                        () -> {
                            openLoginClean(wait);
                            clearAndType(emailInput(wait), "test@test.com");
                            WebElement p = passwordInput(wait);
                            p.click();
                            p.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                            p.sendKeys(Keys.BACK_SPACE);
                            p.sendKeys(Keys.TAB); // trigger blur
                            sleep(600);
                        },
                        () -> {
                            String msg = getPasswordValidationMessage(wait);
                            assertNotNull(msg, "No password required validation message");
                            String lower = msg.toLowerCase();
                            assertTrue(lower.contains("requis") || lower.contains("required"),
                                    "Unexpected validation: " + msg);
                        }
                ),

                // 2) Email or password incorrect
                new Scenario("SL-03 Invalid Credentials",
                        () -> {
                            openLoginClean(wait);
                            doLogin(wait, USER_EMAIL, "bad_password");
                        },
                        () -> {
                            String err = getErrorMessage(wait);
                            assertNotNull(err, "No error message displayed");
                            String lower = err.toLowerCase();
                            assertTrue(
                                    lower.contains("incorrect")
                                            || lower.contains("identifiant")
                                            || lower.contains("invalide")
                                            || lower.contains("invalid")
                                            || lower.contains("email ou mot de passe"),
                                    "Unexpected error: " + err
                            );
                        }
                ),

                // 3) User not approved
                new Scenario("SL-05 User Not Approved",
                        () -> {
                            openLoginClean(wait);
                            doLogin(wait, UNAPPROVED_USER_EMAIL, UNAPPROVED_USER_PASSWORD);
                        },
                        () -> {
                            String err = getErrorMessage(wait);
                            assertNotNull(err, "No error message for not approved user");
                            String lower = err.toLowerCase();
                            assertTrue(
                                    lower.contains("approuv")
                                            || lower.contains("activ")
                                            || lower.contains("en attente")
                                            || lower.contains("pending")
                                            || lower.contains("administrateur"),
                                    "Unexpected not-approved message: " + err
                            );
                        }
                ),

                // 4) Admin login success
                new Scenario("SL-02 Admin Login Success",
                        () -> {
                            openLoginClean(wait);
                            doLogin(wait, ADMIN_EMAIL, ADMIN_PASSWORD);
                            wait.until(d -> d.getCurrentUrl().contains("/admin-home"));
                        },
                        () -> {
                            String url = driver.getCurrentUrl();
                            assertTrue(url.contains("/admin-home"), "Expected /admin-home, got: " + url);
                        }
                ),

                // 5) FINAL: User logged in
                new Scenario("SL-01 Final User Login Success (END)",
                        () -> {
                            openLoginClean(wait);
                            doLogin(wait, USER_EMAIL, USER_PASSWORD);
                            wait.until(d -> d.getCurrentUrl().contains("/user-home"));
                        },
                        () -> {
                            String url = driver.getCurrentUrl();
                            assertTrue(url.contains("/user-home"), "Expected /user-home, got: " + url);
                        }
                )
        );

        System.out.println("▶ All tests executed in strict order (single window)");

        for (Scenario s : scenarios) {
            System.out.println("▶ Running: " + s.code);
            try {
                s.steps.run();
                s.checks.run();
                System.out.println("✅ PASS: " + s.code);
            } catch (AssertionError | RuntimeException ex) {
                System.out.println("❌ FAIL: " + s.code + " -> " + ex.getMessage());
                saveArtifacts(s.code.replaceAll("[^a-zA-Z0-9_-]+", "_") + "_FAIL");
                failures.add(s.code + " -> " + ex.getMessage());
                // continue
            }
        }

        // End-state requirement: user should be logged in
        // (Even if the final scenario failed, we already record it above.)
        if (!failures.isEmpty()) {
            fail("Some scenarios failed:\n- " + String.join("\n- ", failures));
        }

        System.out.println("✅ End: User logged in (/user-home)");
    }
}
