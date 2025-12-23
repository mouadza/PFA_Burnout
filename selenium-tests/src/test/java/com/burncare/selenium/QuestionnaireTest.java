package com.burncare.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;

import java.io.File;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests Selenium pour le questionnaire de burnout
 * Tests: SQ-01 à SQ-05
 */
public class QuestionnaireTest {

    private static String appUrl;
    private WebDriver driver;

    // Credentials pour utilisateur approuvé (à adapter selon votre base de données)
    private static final String TEST_EMAIL = "mouad@gmail.com";
    private static final String TEST_PASSWORD = "mouad1234";

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
        // URL par défaut pour Angular (peut être surchargée avec -Dapp.url=...)
        appUrl = System.getProperty("app.url", "http://localhost:4200");
    }

    @BeforeEach
    void setupTest() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--window-size=1400,900");
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
    }

    private WebDriverWait wait45() {
        return new WebDriverWait(driver, Duration.ofSeconds(45));
    }

    private WebDriverWait wait10() {
        return new WebDriverWait(driver, Duration.ofSeconds(10));
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

    private void waitDomReady(WebDriverWait wait) {
        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
        // Attendre que Angular soit prêt
        wait.until(d -> ((JavascriptExecutor) d).executeScript(
                "return typeof window.getAllAngularTestabilities === 'function' ? " +
                        "window.getAllAngularTestabilities().length === 0 : true"));
    }

    private void waitForAngular() {
        WebDriverWait wait = wait10();
        try {
            // Attendre que Angular soit stable
            wait.until(d -> {
                Object result = ((JavascriptExecutor) d).executeScript(
                        "return typeof window.getAllAngularTestabilities === 'function' ? " +
                                "window.getAllAngularTestabilities().every(t => t.isStable()) : true");
                return result != null && (Boolean) result;
            });
        } catch (TimeoutException ignored) {
            // Si Angular n'est pas détecté, continuer
        }
    }

    /**
     * Se connecter avec les identifiants de test
     */
    private void login(WebDriverWait wait) throws InterruptedException {
        driver.get(appUrl + "/login");
        waitDomReady(wait);
        waitForAngular();

        // Trouver les champs email et password (essayer plusieurs sélecteurs)
        WebElement emailInput = wait.until(d -> {
            List<WebElement> inputs = d.findElements(By.cssSelector(
                    "input[type='email'], input[name='email'], input[formcontrolname='email'], " +
                            "input[placeholder*='email' i], input[placeholder*='Email']"));
            for (WebElement input : inputs) {
                if (input.isDisplayed() && input.isEnabled()) {
                    return input;
                }
            }
            return null;
        });

        WebElement passwordInput = wait.until(d -> {
            List<WebElement> inputs = d.findElements(By.cssSelector(
                    "input[type='password'], input[name='password'], input[formcontrolname='password']"));
            for (WebElement input : inputs) {
                if (input.isDisplayed() && input.isEnabled()) {
                    return input;
                }
            }
            return null;
        });

        // Remplir les champs
        emailInput.clear();
        emailInput.sendKeys(TEST_EMAIL);
        passwordInput.clear();
        passwordInput.sendKeys(TEST_PASSWORD);

        // Cliquer sur le bouton de connexion
        WebElement loginButton = wait.until(d -> {
            List<WebElement> buttons = d.findElements(By.cssSelector("button[type='submit'], button"));
            for (WebElement btn : buttons) {
                if (!btn.isDisplayed() || !btn.isEnabled()) continue;
                String text = btn.getText().trim().toLowerCase();
                String type = btn.getAttribute("type");
                if (text.contains("se connecter") || text.contains("connexion") ||
                        text.contains("login") || "submit".equals(type)) {
                    return btn;
                }
            }
            return null;
        });

        if (loginButton != null) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", loginButton);
            Thread.sleep(500); // Petit délai pour la stabilité
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", loginButton);
        } else {
            throw new RuntimeException("Bouton de connexion non trouvé");
        }

        // Attendre la redirection vers user-home ou admin-home
        wait.until(d -> {
            String currentUrl = d.getCurrentUrl();
            boolean redirected = currentUrl.contains("/user-home") ||
                    currentUrl.contains("/admin-home") ||
                    (!currentUrl.contains("/login") && !currentUrl.endsWith("/"));
            return redirected;
        });

        waitDomReady(wait);
        waitForAngular();
        Thread.sleep(1000); // Attendre que la page soit complètement chargée
    }

    /**
     * Naviguer vers la page du questionnaire
     */
    private void navigateToQuestionnaire(WebDriverWait wait) {
        // Essayer d'abord via le menu sidebar
        try {
            WebElement questionnaireLink = wait10().until(d -> {
                List<WebElement> links = d.findElements(By.cssSelector("a, button, [routerlink], [routerlinkactive]"));
                for (WebElement link : links) {
                    String text = link.getText().toLowerCase();
                    if (text.contains("questionnaire")) {
                        return link;
                    }
                }
                return null;
            });
            if (questionnaireLink != null) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", questionnaireLink);
                wait.until(d -> d.getCurrentUrl().contains("/questionnaire"));
            } else {
                // Si pas de lien, naviguer directement
                driver.get(appUrl + "/questionnaire");
            }
        } catch (TimeoutException e) {
            // Si pas de sidebar trouvé, naviguer directement
            driver.get(appUrl + "/questionnaire");
        }

        waitDomReady(wait);
        waitForAngular();
    }

    /**
     * Sélectionner une réponse pour la question courante
     */
    private void selectAnswer(WebDriverWait wait, String answerText) {
        WebElement answerButton = wait.until(d -> {
            List<WebElement> buttons = d.findElements(By.cssSelector(".option-btn, button"));
            for (WebElement btn : buttons) {
                String text = btn.getText().trim();
                if (text.equalsIgnoreCase(answerText)) {
                    return btn;
                }
            }
            return null;
        });

        if (answerButton != null) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", answerButton);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", answerButton);
            waitForAngular();
        } else {
            throw new RuntimeException("Bouton de réponse non trouvé: " + answerText);
        }
    }

    /**
     * Cliquer sur le bouton Suivant/Envoyer
     */
    private void clickNextOrSubmit(WebDriverWait wait) {
        WebElement nextButton = wait.until(d -> {
            List<WebElement> buttons = d.findElements(By.cssSelector("button"));
            for (WebElement btn : buttons) {
                String text = btn.getText().trim().toLowerCase();
                if ((text.contains("suivant") || text.contains("envoyer") || text.contains("submit"))
                        && !text.contains("précédent")) {
                    // Vérifier que le bouton n'est pas désactivé
                    if (btn.isEnabled()) {
                        return btn;
                    }
                }
            }
            return null;
        });

        if (nextButton != null) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", nextButton);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextButton);
            waitForAngular();
        } else {
            throw new RuntimeException("Bouton Suivant/Envoyer non trouvé ou désactivé");
        }
    }

    /**
     * Vérifier si une alert est présente et récupérer son texte
     */
    private String getAlertText() {
        try {
            Alert alert = driver.switchTo().alert();
            String text = alert.getText();
            alert.accept();
            return text;
        } catch (NoAlertPresentException e) {
            return null;
        }
    }

    /**
     * Répondre à toutes les questions
     */
    private void answerAllQuestions(WebDriverWait wait, String answerText) {
        for (int i = 0; i < 12; i++) {
            selectAnswer(wait, answerText);
            if (i < 11) { // Ne pas cliquer sur "Envoyer" après la dernière question
                clickNextOrSubmit(wait);
                // Attendre que la prochaine question apparaisse
                wait.until(d -> {
                    List<WebElement> questions = d.findElements(By.cssSelector("h2, .question-card h2"));
                    return !questions.isEmpty();
                });
            }
        }
    }

    // ===================== TESTS =====================

    @Test
    @DisplayName("SQ-01 Accès au questionnaire")
    void SQ_01_accessQuestionnaire() throws InterruptedException {
        WebDriverWait wait = wait45();
        try {
            // Précondition: Utilisateur connecté et approuvé
            login(wait);

            // Accéder à la page Questionnaire
            navigateToQuestionnaire(wait);

            // Vérifier que la page du questionnaire s'affiche
            wait.until(d -> d.getCurrentUrl().contains("/questionnaire"));

            // Vérifier la présence des questions
            WebElement questionElement = wait.until(d -> {
                List<WebElement> questions = d.findElements(By.cssSelector("h2, .question-card h2, .question-card"));
                for (WebElement q : questions) {
                    String text = q.getText();
                    if (text.contains("Je me suis senti") || text.contains("question") || text.length() > 20) {
                        return q;
                    }
                }
                return null;
            });

            assertNotNull(questionElement, "La question du questionnaire doit être visible");

            // Vérifier la présence des boutons de réponse
            List<WebElement> answerButtons = driver.findElements(By.cssSelector(".option-btn, button"));
            int answerButtonCount = 0;
            for (WebElement btn : answerButtons) {
                String text = btn.getText().trim();
                if (text.equals("Jamais") || text.equals("Rarement") || text.equals("Parfois")
                        || text.equals("Souvent") || text.equals("Toujours")) {
                    answerButtonCount++;
                }
            }

            assertTrue(answerButtonCount >= 4,
                    "Au moins 4 boutons de réponse (Jamais, Rarement, Parfois, Souvent, Toujours) doivent être présents. Trouvé: " + answerButtonCount);

            // Vérifier la barre de progression
            WebElement progressBar = driver.findElement(By.cssSelector(".progress-bar, .progress"));
            assertNotNull(progressBar, "La barre de progression doit être visible");

        } catch (Exception e) {
            saveArtifacts("SQ_01_fail");
            throw e;
        }
    }

    @Test
    @DisplayName("SQ-02 Soumission du questionnaire incomplet")
    void SQ_02_submitIncompleteQuestionnaire() throws InterruptedException {
        WebDriverWait wait = wait45();
        try {
            // Précondition: Questionnaire ouvert
            login(wait);
            navigateToQuestionnaire(wait);

            // Répondre à quelques questions seulement (pas toutes)
            selectAnswer(wait, "Parfois");
            clickNextOrSubmit(wait);

            wait.until(d -> {
                List<WebElement> questions = d.findElements(By.cssSelector("h2, .question-card h2"));
                return !questions.isEmpty();
            });

            // Aller directement à la dernière question sans répondre
            for (int i = 1; i < 11; i++) {
                clickNextOrSubmit(wait);
                wait.until(d -> {
                    List<WebElement> questions = d.findElements(By.cssSelector("h2, .question-card h2"));
                    return !questions.isEmpty();
                });
            }

            // Cliquer sur "Envoyer" sans répondre à toutes les questions
            clickNextOrSubmit(wait);

            // Attendre l'alert (peut prendre un peu de temps)
            String alertText = null;
            for (int i = 0; i < 10; i++) {
                alertText = getAlertText();
                if (alertText != null && !alertText.isEmpty()) break;
                Thread.sleep(500);
            }

            assertNotNull(alertText, "Une alert doit apparaître pour indiquer que toutes les questions doivent être complétées");

            String alertTextLower = alertText.toLowerCase();
            assertTrue(alertTextLower.contains("pas répondu") ||
                            alertTextLower.contains("compléter") ||
                            alertTextLower.contains("questions manquantes") ||
                            alertTextLower.contains("répondre"),
                    "Le message d'erreur doit indiquer que toutes les questions doivent être complétées. Texte: " + alertText);

            // Vérifier que nous sommes toujours sur la page questionnaire
            assertTrue(driver.getCurrentUrl().contains("/questionnaire"),
                    "L'utilisateur doit rester sur la page questionnaire après une soumission incomplète");

        } catch (Exception e) {
            saveArtifacts("SQ_02_fail");
            throw e;
        }
    }

    @Test
    @DisplayName("SQ-03 Soumission du questionnaire avec réponses valides")
    void SQ_03_submitCompleteQuestionnaire() throws InterruptedException {
        WebDriverWait wait = wait45();
        try {
            // Précondition: Questionnaire ouvert
            login(wait);
            navigateToQuestionnaire(wait);

            // Répondre à toutes les questions
            answerAllQuestions(wait, "Parfois");

            // Cliquer sur "Envoyer" (dernière question)
            clickNextOrSubmit(wait);

            // Attendre la redirection vers la page de résultat
            wait.until(d -> d.getCurrentUrl().contains("/questionnaire-result"));
            waitDomReady(wait);
            waitForAngular();

            // Vérifier l'affichage du résultat
            WebElement resultCard = wait.until(d ->
                    d.findElement(By.cssSelector(".result-card, [class*='result']")));

            assertNotNull(resultCard, "La carte de résultat doit être visible");

            // Vérifier la présence du score (même si 0, l'élément doit exister)
            WebElement scoreElement = wait.until(d -> {
                List<WebElement> elements = d.findElements(By.cssSelector(".score-text, .score-circle, [class*='score']"));
                return elements.isEmpty() ? null : elements.get(0);
            });

            assertNotNull(scoreElement, "Le score doit être affiché dans la page de résultat");

            // Vérifier la présence d'un titre de résultat
            String pageText = driver.findElement(By.tagName("body")).getText().toLowerCase();
            assertTrue(pageText.contains("résultat") || pageText.contains("score") ||
                            pageText.contains("burnout") || pageText.contains("risque"),
                    "La page de résultat doit contenir des informations sur le résultat");

        } catch (Exception e) {
            saveArtifacts("SQ_03_fail");
            throw e;
        }
    }

    @Test
    @DisplayName("SQ-04 Affichage du score de burnout")
    void SQ_04_displayBurnoutScore() throws InterruptedException {
        WebDriverWait wait = wait45();
        try {
            // Précondition: Questionnaire soumis
            login(wait);
            navigateToQuestionnaire(wait);
            answerAllQuestions(wait, "Souvent");
            clickNextOrSubmit(wait);

            // Attendre la page de résultat
            wait.until(d -> d.getCurrentUrl().contains("/questionnaire-result"));
            waitDomReady(wait);
            waitForAngular();

            // Attendre que le score soit chargé (plus de "Chargement des résultats...")
            wait.until(d -> {
                String pageText = d.findElement(By.tagName("body")).getText();
                return !pageText.contains("Chargement des résultats");
            });

            // Vérifier la présence du score
            WebElement scoreElement = wait.until(d -> {
                List<WebElement> elements = d.findElements(By.cssSelector(".score-text"));
                for (WebElement el : elements) {
                    String text = el.getText().trim();
                    // Vérifier que c'est un nombre
                    if (!text.isEmpty() && text.matches("\\d+")) {
                        return el;
                    }
                }
                // Sinon chercher dans le texte de la page
                String pageText = d.findElement(By.tagName("body")).getText();
                if (pageText.matches(".*\\b\\d+\\b.*")) {
                    return d.findElement(By.tagName("body"));
                }
                return null;
            });

            assertNotNull(scoreElement, "Le score de burnout doit être affiché");

            // Vérifier que le score est un nombre valide (0-100)
            String scoreText = scoreElement.getText();
            String numericScore = scoreText.replaceAll("[^0-9]", "").trim();
            if (!numericScore.isEmpty()) {
                int score = Integer.parseInt(numericScore.substring(0, Math.min(3, numericScore.length())));
                assertTrue(score >= 0 && score <= 100,
                        "Le score doit être entre 0 et 100. Score trouvé: " + score);
            }

            // Vérifier la présence d'informations complémentaires (titre, message, etc.)
            String pageText = driver.findElement(By.tagName("body")).getText().toLowerCase();
            assertTrue(pageText.contains("résultat") || pageText.contains("risque") ||
                            pageText.contains("burnout"),
                    "La page doit contenir des informations supplémentaires sur le résultat");

        } catch (Exception e) {
            saveArtifacts("SQ_04_fail");
            throw e;
        }
    }

    @Test
    @DisplayName("SQ-05 Empêchement de la double soumission")
    void SQ_05_preventDoubleSubmission() throws InterruptedException {
        WebDriverWait wait = wait45();
        try {
            // Précondition: Questionnaire soumis une première fois
            login(wait);
            navigateToQuestionnaire(wait);
            answerAllQuestions(wait, "Rarement");

            // Cliquer plusieurs fois rapidement sur le bouton "Envoyer"
            WebElement submitButton = wait.until(d -> {
                List<WebElement> buttons = d.findElements(By.cssSelector("button"));
                for (WebElement btn : buttons) {
                    String text = btn.getText().trim().toLowerCase();
                    if ((text.contains("envoyer") || text.contains("submit")) && btn.isEnabled()) {
                        return btn;
                    }
                }
                return null;
            });

            assertNotNull(submitButton, "Le bouton Envoyer doit être présent");

            // Cliquer plusieurs fois rapidement
            for (int i = 0; i < 3; i++) {
                try {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitButton);
                    Thread.sleep(200); // Petit délai entre les clics
                } catch (Exception ignored) {}
            }

            // Attendre la redirection ou la désactivation du bouton
            Thread.sleep(1000);

            // Vérifier que le bouton est désactivé OU que nous sommes sur la page de résultat
            boolean isOnResultPage = driver.getCurrentUrl().contains("/questionnaire-result");
            boolean buttonDisabled = false;

            if (!isOnResultPage) {
                try {
                    WebElement currentButton = driver.findElement(By.cssSelector("button"));
                    String text = currentButton.getText().toLowerCase();
                    if (text.contains("envoyer") || text.contains("envoi")) {
                        buttonDisabled = !currentButton.isEnabled() || text.contains("envoi...");
                    }
                } catch (Exception ignored) {}
            }

            assertTrue(isOnResultPage || buttonDisabled,
                    "Le bouton doit être désactivé ou la soumission doit avoir abouti à la page de résultat");

            // Si on est sur la page de résultat, vérifier qu'il n'y a qu'un seul résultat
            if (isOnResultPage) {
                waitDomReady(wait);
                waitForAngular();

                // Vérifier qu'il n'y a qu'un seul élément de résultat principal
                List<WebElement> resultCards = driver.findElements(By.cssSelector(".result-card, [class*='result-card']"));
                assertTrue(resultCards.size() <= 1,
                        "Il ne doit y avoir qu'un seul résultat affiché, même avec plusieurs clics");
            }

        } catch (Exception e) {
            saveArtifacts("SQ_05_fail");
            throw e;
        }
    }
}

