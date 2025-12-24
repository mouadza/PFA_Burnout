package com.example.test_selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QuestionnaireTest {

    private static String appUrl;
    private WebDriver driver;

    private static final String USER_EMAIL = "test6@gmail.com";
    private static final String USER_PASSWORD = "test";

    // ======= Tunables =======
    private static final Duration WAIT = Duration.ofSeconds(30);
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

        // IMPORTANT: no implicit wait (prevents hidden delays)
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        driver.manage().timeouts().pageLoadTimeout(PAGE_LOAD_TIMEOUT);
        driver.manage().timeouts().scriptTimeout(SCRIPT_TIMEOUT);
    }

    @AfterAll
    void tearDown() {
        if (driver != null) driver.quit();
    }

    private WebDriverWait waitN() {
        return new WebDriverWait(driver, WAIT);
    }

    // ======= Small utilities =======
    private void sleep(int ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }

    private void waitDomReady(WebDriverWait wait) {
        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
    }

    /**
     * Angular 2+ stable wait (fast + safe). If testability isn't available, it won't block.
     */
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
                return true;
            }
        });
    }

    private void clearSession() {
        try { driver.manage().deleteAllCookies(); } catch (Exception ignored) {}
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "try{localStorage.clear();}catch(e){} try{sessionStorage.clear();}catch(e){}"
            );
        } catch (Exception ignored) {}
    }

    private void clearAndType(WebElement el, String text) {
        el.click();
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        el.sendKeys(Keys.BACK_SPACE);
        el.sendKeys(text);
    }

    private void clickSafe(WebElement el) {
        try {
            el.click();
            return;
        } catch (Exception ignored) {}

        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
            return;
        } catch (Exception ignored) {}

        new Actions(driver).moveToElement(el).click().perform();
    }

    private void step(String label, Runnable run) {
        Instant start = Instant.now();
        System.out.println("\n--- " + label + " ---");
        try {
            run.run();
        } finally {
            long ms = Duration.between(start, Instant.now()).toMillis();
            System.out.println("✓ " + label + " (" + ms + " ms)");
        }
    }

    private void saveArtifacts(String prefix) {
        try {
            Path dir = Paths.get("target", "selenium-artifacts");
            Files.createDirectories(dir);

            File png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(png.toPath(), dir.resolve(prefix + ".png"), StandardCopyOption.REPLACE_EXISTING);

            Files.writeString(dir.resolve(prefix + ".html"), driver.getPageSource());
            Files.writeString(dir.resolve(prefix + ".txt"),
                    "URL: " + driver.getCurrentUrl() + System.lineSeparator() +
                            "readyState: " + ((JavascriptExecutor)driver).executeScript("return document.readyState"));
        } catch (Exception ignored) {}
    }

    // ======= Navigation helpers =======
    private WebElement byCss(WebDriverWait wait, String css) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(css)));
    }

    private WebElement byXpathClickable(WebDriverWait wait, String xp) {
        return wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xp)));
    }

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

        // login form ready
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[formcontrolname='email'], input[type='email']")
        ));
    }

    private void doLogin(WebDriverWait wait, String email, String password) {
        openLoginClean(wait);

        WebElement emailInput = byCss(wait, "input[formcontrolname='email'], input[type='email']");
        clearAndType(emailInput, email);

        WebElement passInput = byCss(wait, "input[formcontrolname='password'], input[type='password']");
        clearAndType(passInput, password);

        WebElement loginBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button[type='submit'], button.btn-login")
        ));
        clickSafe(loginBtn);

        wait.until(d -> d.getCurrentUrl().contains("/user-home"));
        waitAngularStable(wait);
    }

    private String readToastMessageFast() {
        // Very fast JS read (no long waits)
        try {
            Object txt = ((JavascriptExecutor) driver).executeScript(
                    "var toast=document.querySelector('.toast-message');" +
                            "if(!toast) return null;" +
                            "var span=toast.querySelector('span');" +
                            "var t=(span?span.textContent:toast.textContent);" +
                            "return t? t.trim(): null;"
            );
            return txt == null ? null : txt.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isOnQuestion(WebDriverWait wait, String containsText) {
        try {
            WebElement qn = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".question-number")));
            return qn.getText() != null && qn.getText().contains(containsText);
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    @DisplayName("Scénario complet: Login → Questionnaire incomplet → Questionnaire complet → Limite 24h")
    void completeQuestionnaireScenario() {

        WebDriverWait wait = waitN();

        try {
            System.out.println("\n=== DÉBUT SCÉNARIO QUESTIONNAIRE ===");

            step("ÉTAPE 1: Connexion", () -> doLogin(wait, USER_EMAIL, USER_PASSWORD));

            step("ÉTAPE 2: Aller au questionnaire", () -> {
                WebElement questionnaireButton = byXpathClickable(wait,
                        "//button[contains(@class, 'action-card') and .//h3[contains(text(), 'Questionnaire')]]"
                );
                clickSafe(questionnaireButton);

                wait.until(d -> d.getCurrentUrl().contains("/questionnaire"));
                waitAngularStable(wait);

                // wait one option button visible = questionnaire ready
                wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//button[contains(@class,'option-btn')]")
                ));
            });

            step("ÉTAPE 3: Répondre 10 questions puis soumettre incomplet", () -> {
                for (int i = 0; i < 10; i++) {
                    WebElement option = byXpathClickable(wait,
                            "//button[contains(@class,'option-btn') and contains(., 'Parfois')]"
                    );
                    clickSafe(option);

                    WebElement next = byXpathClickable(wait,
                            "//button[contains(@class,'next') and not(contains(., 'Envoyer'))]"
                    );
                    clickSafe(next);

                    // wait question refresh quickly (avoid heavy waits)
                    waitAngularStable(wait);
                }

                // Skip Q11 (go next without answering)
                WebElement nextBtnToLast = byXpathClickable(wait,
                        "//button[contains(@class,'next') and not(contains(., 'Envoyer'))]"
                );
                clickSafe(nextBtnToLast);
                waitAngularStable(wait);

                // Click Envoyer on last screen (your DOM uses same 'next' class)
                WebElement submitBtn = byXpathClickable(wait,
                        "//button[contains(@class,'next')]"
                );
                clickSafe(submitBtn);

                waitAngularStable(wait);

                // We expect it to return to first missing question (usually 11/12)
                // Wait a bit for UI update
                wait.until(d -> isOnQuestion(wait, "11/12") || isOnQuestion(wait, "Question 11"));
            });

            step("ÉTAPE 3b: Vérifier retour question 11 sans sélection", () -> {
                WebElement questionNumber = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".question-number")));
                String qtxt = questionNumber.getText().trim();
                assertTrue(qtxt.contains("11/12") || qtxt.contains("Question 11"),
                        "Doit retourner à la question 11. Trouvé: " + qtxt);

                List<WebElement> selected = driver.findElements(By.cssSelector(".option-btn.selected"));
                assertTrue(selected.isEmpty(), "Question 11 ne doit pas être sélectionnée.");
            });

            step("ÉTAPE 3c: Compléter les 2 dernières questions + Envoyer", () -> {
                for (int i = 0; i < 2; i++) {
                    WebElement option = byXpathClickable(wait,
                            "//button[contains(@class,'option-btn') and contains(., 'Parfois')]"
                    );
                    clickSafe(option);

                    // click next if exists (not on last)
                    List<WebElement> nextBtns = driver.findElements(By.xpath("//button[contains(@class,'next') and not(contains(., 'Envoyer'))]"));
                    if (!nextBtns.isEmpty()) {
                        clickSafe(nextBtns.get(0));
                        waitAngularStable(wait);
                    }
                }

                WebElement submitBtn = byXpathClickable(wait, "//button[contains(@class,'next')]");
                clickSafe(submitBtn);

                wait.until(d -> d.getCurrentUrl().contains("/questionnaire-result"));
                waitAngularStable(wait);
            });

            step("ÉTAPE 4: Vérifier résultat sauvegardé dans Mes résultats", () -> {
                driver.get(appUrl + "/user-home");
                waitDomReady(wait);
                waitAngularStable(wait);

                WebElement myResultsBtn = byXpathClickable(wait,
                        "//button[contains(@class, 'action-card') and .//h3[contains(translate(text(),'RÉ','ré'),'résultats') or contains(translate(text(),'RÉ','ré'),'resultats')]]"
                );
                clickSafe(myResultsBtn);

                wait.until(d -> d.getCurrentUrl().contains("/my-results"));
                waitAngularStable(wait);

                // Prefer waiting for result-card OR "no results" message
                wait.until(d -> {
                    List<WebElement> cards = d.findElements(By.cssSelector(".result-card"));
                    if (!cards.isEmpty()) return true;
                    String body = d.findElement(By.tagName("body")).getText().toLowerCase();
                    return body.contains("aucun") || body.contains("no result");
                });

                List<WebElement> cards = driver.findElements(By.cssSelector(".result-card"));
                assertTrue(cards.size() > 0, "Aucun résultat trouvé dans Mes résultats après soumission.");
            });

            step("ÉTAPE 5: Retour questionnaire + limite 24h au premier Suivant", () -> {
                driver.get(appUrl + "/questionnaire");
                waitDomReady(wait);
                waitAngularStable(wait);

                WebElement option = byXpathClickable(wait,
                        "//button[contains(@class,'option-btn') and contains(., 'Parfois')]"
                );
                clickSafe(option);

                WebElement next = byXpathClickable(wait,
                        "//button[contains(@class,'next') and not(contains(., 'Envoyer'))]"
                );
                clickSafe(next);

                // Wait briefly for toast
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
                shortWait.until(d -> readToastMessageFast() != null);

                String msg = readToastMessageFast();
                assertNotNull(msg, "Toast limite 24h non détecté.");
                assertTrue(
                        msg.contains("24") || msg.toLowerCase().contains("un seul test") || msg.toLowerCase().contains("réessayer"),
                        "Message limite 24h inattendu: " + msg
                );
            });

            // Stay on questionnaire (no redirect)
            assertTrue(driver.getCurrentUrl().contains("/questionnaire"),
                    "Doit rester sur /questionnaire après limitation. URL: " + driver.getCurrentUrl());

            System.out.println("\n=== SCÉNARIO COMPLET RÉUSSI ===");

        } catch (AssertionError e) {
            saveArtifacts("questionnaire_fail_assert");
            throw e;
        } catch (Exception e) {
            saveArtifacts("questionnaire_fail_exception");
            throw e;
        }
    }
}
