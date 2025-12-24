package com.example.test_selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LoginTest {

    private static String appUrl;
    private WebDriver driver;

    private static final String USER_EMAIL = "zaouia@gmail.com";
    private static final String USER_PASSWORD = "mouad1234";
    private static final String UNAPPROVED_USER_EMAIL = "simo@gmail.com";
    private static final String UNAPPROVED_USER_PASSWORD = "simo1234";

    // ======= Timeouts (important for Angular) =======
    private static final Duration EXPLICIT_WAIT = Duration.ofSeconds(25);
    private static final Duration PAGE_LOAD_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration SCRIPT_TIMEOUT = Duration.ofSeconds(30);

    @BeforeAll
    void setupClass() {
        WebDriverManager.chromedriver().setup();
        appUrl = System.getProperty("app.url", "http://localhost:4200");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--window-size=1400,900");
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();

        // Implicit 0 (use explicit waits only)
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        driver.manage().timeouts().pageLoadTimeout(PAGE_LOAD_TIMEOUT);
        driver.manage().timeouts().scriptTimeout(SCRIPT_TIMEOUT);
    }

    // ✅ Keep window open after tests (manual close)
    // @AfterAll
    // void tearDown() { if (driver != null) driver.quit(); }

    private WebDriverWait waitN() {
        return new WebDriverWait(driver, EXPLICIT_WAIT);
    }

    private void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private void waitDomReady(WebDriverWait wait) {
        wait.until(d -> "complete".equals(
                ((JavascriptExecutor) d).executeScript("return document.readyState")
        ));
    }

    // Angular stability (if available)
    private void waitAngularStable(WebDriverWait wait) {
        wait.until(d -> {
            try {
                Object res = ((JavascriptExecutor) d).executeScript(
                        "return (window.getAllAngularTestabilities && " +
                                "window.getAllAngularTestabilities().length > 0 && " +
                                "window.getAllAngularTestabilities().every(t => t.isStable())) || " +
                                "(!window.getAllAngularTestabilities);"
                );
                return Boolean.TRUE.equals(res);
            } catch (Exception e) {
                return true; // don't deadlock
            }
        });
    }

    private void clearSession() {
        try { driver.manage().deleteAllCookies(); } catch (Exception ignored) {}
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "try { window.localStorage && window.localStorage.clear(); } catch(e) {}" +
                            "try { window.sessionStorage && window.sessionStorage.clear(); } catch(e) {}"
            );
        } catch (Exception ignored) {}
    }

    /**
     * Stable open for Angular login page:
     * - open base (same-origin)
     * - clear cookies/storage
     * - open /login and wait for form
     */
    private void openLoginClean(WebDriverWait wait) {
        driver.get(appUrl);
        waitDomReady(wait);

        clearSession();
        driver.navigate().refresh();
        waitDomReady(wait);
        waitAngularStable(wait);

        driver.get(appUrl + "/login");
        waitDomReady(wait);
        waitAngularStable(wait);

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[formcontrolname='email'], input[type='email']")
        ));
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

    private void clearValue(WebElement el) {
        el.click();
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        el.sendKeys(Keys.BACK_SPACE);
    }

    private WebElement findVisibleEnabledButton(By by) {
        for (WebElement b : driver.findElements(by)) {
            try {
                if (b.isDisplayed() && b.isEnabled()) return b;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void clickLoginButton(WebDriverWait wait) {
        WebElement btn = wait.until(d -> {
            WebElement b = findVisibleEnabledButton(By.cssSelector("button[type='submit'], button.btn-login"));
            if (b != null) return b;

            // fallback: any visible button that looks like login
            for (WebElement x : d.findElements(By.cssSelector("button"))) {
                try {
                    if (!x.isDisplayed() || !x.isEnabled()) continue;
                    String t = (x.getText() == null ? "" : x.getText()).toLowerCase();
                    if (t.contains("login") || t.contains("connexion") || t.contains("se connecter")) return x;
                } catch (Exception ignored) {}
            }
            return null;
        });

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        sleep(250);
        waitAngularStable(wait);
    }

    private void doLogin(WebDriverWait wait, String email, String pass) {
        clearAndType(emailInput(wait), email);
        clearAndType(passwordInput(wait), pass);
        clickLoginButton(wait);
    }

    private boolean passwordLooksInvalid(WebElement pass) {
        String cls = pass.getAttribute("class");
        String aria = pass.getAttribute("aria-invalid");
        boolean classInvalid = cls != null && (cls.contains("ng-invalid") || cls.contains("is-invalid") || cls.contains("invalid"));
        boolean ariaInvalid = "true".equalsIgnoreCase(aria);
        return classInvalid || ariaInvalid;
    }

    private String getErrorMessage(WebDriverWait wait) {
        By[] selectors = new By[] {
                By.cssSelector(".error-message"),
                By.cssSelector(".alert-danger"),
                By.cssSelector(".mat-mdc-snack-bar-container"),
                By.cssSelector(".mat-snack-bar-container"),
                By.cssSelector(".toast-error"),
                By.cssSelector(".mat-error")
        };

        for (By by : selectors) {
            try {
                WebElement e = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
                String txt = e.getText();
                if (txt != null && !txt.trim().isEmpty()) return txt.trim();
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void saveArtifacts(String name) {
        try {
            Path outDir = Paths.get("target", "selenium-artifacts");
            Files.createDirectories(outDir);

            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(src.toPath(), outDir.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);

            Files.writeString(outDir.resolve(name + ".html"), driver.getPageSource());

            Files.writeString(outDir.resolve(name + ".txt"),
                    "URL: " + driver.getCurrentUrl() + System.lineSeparator() +
                            "readyState: " + ((JavascriptExecutor)driver).executeScript("return document.readyState")
            );
        } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("Single window ordered flow: START validation -> END user dashboard (keep open)")
    void orderedFlow_startValidation_endUserDashboard_keepOpen() {

        WebDriverWait wait = waitN();
        List<String> failures = new ArrayList<>();

        record Scenario(String code, Runnable steps, Runnable checks) {}

        // ✅ STRICT ORDER: starts with validation and ends with user login
        List<Scenario> scenarios = List.of(

                new Scenario("SL-VALIDATION Password Required",
                        () -> {
                            openLoginClean(wait);

                            clearAndType(emailInput(wait), "test@test.com");

                            WebElement pass = passwordInput(wait);
                            clearValue(pass);

                            pass.sendKeys(Keys.TAB);
                            clickLoginButton(wait);

                            sleep(50);
                        },
                        () -> {
                            String url = driver.getCurrentUrl();
                            if (!url.contains("/login")) {
                                throw new AssertionError("Expected to stay on /login page, but URL is: " + url);
                            }

                            WebElement pass = passwordInput(wait);
                            boolean invalid = passwordLooksInvalid(pass);

                            // Accept either invalid state OR visible validation message
                            boolean hasMsg = false;
                            try {
                                List<WebElement> msgs = driver.findElements(
                                        By.cssSelector(".validation-message, .mat-error, .invalid-feedback, .text-danger, small")
                                );
                                for (WebElement m : msgs) {
                                    if (!m.isDisplayed()) continue;
                                    String t = (m.getText() == null ? "" : m.getText()).toLowerCase();
                                    if (t.contains("requis") || t.contains("required") || t.contains("obligatoire") || t.contains("mot de passe")) {
                                        hasMsg = true;
                                        break;
                                    }
                                }
                            } catch (Exception ignored) {}

                            if (!invalid && !hasMsg) {
                                throw new AssertionError("Password Required validation not detected (no invalid state and no message).");

                            }
                            sleep(50);
                        }
                ),

                new Scenario("SL-03 Invalid Credentials",
                        () -> {
                            openLoginClean(wait);
                            doLogin(wait, USER_EMAIL, "bad_password");
                        },
                        () -> {
                            String err = getErrorMessage(wait);
                            if (err == null) throw new AssertionError("No error message displayed for invalid credentials");
                            sleep(50);
                        }
                ),

                new Scenario("SL-05 User Not Approved",
                        () -> {
                            openLoginClean(wait);
                            doLogin(wait, UNAPPROVED_USER_EMAIL, UNAPPROVED_USER_PASSWORD);
                        },
                        () -> {
                            String err = getErrorMessage(wait);
                            if (err == null) throw new AssertionError("No not-approved error message displayed");
                        }
                ),

                new Scenario("SL-01 Final User Login Success (END)",
                        () -> {
                            openLoginClean(wait);
                            doLogin(wait, USER_EMAIL, USER_PASSWORD);

                            // ✅ wait dashboard route
                            wait.until(d -> d.getCurrentUrl().contains("/user-home"));
                            waitAngularStable(wait);

                            // ✅ and ensure some page content exists (not empty spinner)
                            wait.until(d -> {
                                String body = d.findElement(By.tagName("body")).getText();
                                return body != null && body.trim().length() > 5;
                            });
                        },
                        () -> {
                            String url = driver.getCurrentUrl();
                            if (!url.contains("/user-home")) throw new AssertionError("Expected /user-home, got: " + url);
                        }
                )
        );

        System.out.println("▶ START ordered flow (single window): first VALIDATION, last USER login (/user-home)");

        for (Scenario s : scenarios) {
            System.out.println("▶ START: " + s.code);
            try {
                s.steps.run();
                s.checks.run();
                System.out.println("✅ END: " + s.code);
            } catch (Throwable ex) {
                System.out.println("❌ FAIL: " + s.code + " -> " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                saveArtifacts(s.code.replaceAll("[^a-zA-Z0-9_-]+", "_") + "_FAIL");
                failures.add(s.code + " -> " + ex.getMessage());
            }
        }

        if (!failures.isEmpty()) {
            fail("Some scenarios failed:\n- " + String.join("\n- ", failures));
        }

        // ✅ END: user stays on dashboard
        System.out.println("✅ END STATE: User logged in (/user-home). Browser will stay open.");
        System.out.println("Press ENTER in the console to finish the test (browser stays open until then)...");
        try {
            System.in.read();
        } catch (Exception ignored) {}
    }
}
