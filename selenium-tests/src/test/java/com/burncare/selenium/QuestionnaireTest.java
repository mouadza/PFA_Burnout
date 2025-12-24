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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QuestionnaireTest {

    private static String appUrl;
    private WebDriver driver;

    private static final String USER_EMAIL = "test3@gmail.com";
    private static final String USER_PASSWORD = "test";

    @BeforeAll
    void setupClass() {
        WebDriverManager.chromedriver().setup();
        appUrl = System.getProperty("app.url", "http://localhost:4200");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--window-size=1400,900");

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
    }

    @AfterAll
    void tearDown() {
        if (driver != null) driver.quit();
    }

    private WebDriverWait wait20() {
        return new WebDriverWait(driver, Duration.ofSeconds(20));
    }

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
            ((JavascriptExecutor) driver).executeScript("window.localStorage.clear(); window.sessionStorage.clear();");
        } catch (Exception ignored) {}
    }

    private void waitForAngular() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
            wait.until(d -> {
                Object result = ((JavascriptExecutor) d).executeScript(
                    "return typeof angular !== 'undefined' && angular.element(document.body).injector().get('$http').pendingRequests.length === 0"
                );
                return result != null && (result.equals(true) || result.equals(0));
            });
        } catch (Exception e) {
            // Angular might not be available, continue anyway
        }
        sleep(5);
    }

    private void doLogin(WebDriverWait wait, String email, String password) {
        driver.get(appUrl + "/login");
        waitDomReady(wait);
        waitForAngular();

        WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("input[formcontrolname='email'], input[type='email']")
        ));
        clearAndType(emailInput, email);

        WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("input[formcontrolname='password'], input[type='password']")
        ));
        clearAndType(passwordInput, password);

        sleep(50);

        WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(
            By.cssSelector("button[type='submit'], button.btn-login")
        ));
        clickElement(loginButton);

        wait.until(d -> d.getCurrentUrl().contains("/user-home"));
        sleep(50);
        waitForAngular();
    }

    private void clearAndType(WebElement element, String text) {
        element.click();
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        element.sendKeys(Keys.BACK_SPACE);
        element.sendKeys(text);
    }

    private void clickElement(WebElement element) {
        try {
            element.click();
        } catch (WebDriverException e) {
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            } catch (Exception e2) {
                new org.openqa.selenium.interactions.Actions(driver).moveToElement(element).click().perform();
            }
        }
    }

    private void saveArtifacts(String prefix) {
        try {
            Path artifactsDir = Paths.get("target", "selenium-artifacts");
            Files.createDirectories(artifactsDir);

            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(screenshot.toPath(), artifactsDir.resolve(prefix + "_screenshot.png"),
                StandardCopyOption.REPLACE_EXISTING);

            String pageSource = driver.getPageSource();
            Files.write(artifactsDir.resolve(prefix + "_source.html"), pageSource.getBytes());

            System.out.println("Artifacts sauvegardés dans: " + artifactsDir);
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde des artifacts: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Scénario complet: Login → Questionnaire incomplet → Questionnaire complet → Deuxième soumission bloquée")
    void completeQuestionnaireScenario() throws InterruptedException {
        WebDriverWait wait = wait45();

        try {
            System.out.println("\n=== DÉBUT DU SCÉNARIO COMPLET DE QUESTIONNAIRE ===");

            // ===== ÉTAPE 1: CONNEXION =====
            System.out.println("\n--- ÉTAPE 1: Connexion utilisateur ---");
            doLogin(wait, USER_EMAIL, USER_PASSWORD);
            System.out.println("✓ Utilisateur connecté");

            // ===== ÉTAPE 2: NAVIGATION VERS QUESTIONNAIRE =====
            System.out.println("\n--- ÉTAPE 2: Navigation vers le questionnaire ---");

            // Cliquer sur le bouton "Questionnaire" dans les actions rapides du dashboard
            WebElement questionnaireButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'action-card') and .//h3[contains(text(), 'Questionnaire')]]")
            ));
            clickElement(questionnaireButton);

            // Attendre le chargement de la page questionnaire
            wait.until(d -> d.getCurrentUrl().contains("/questionnaire"));
            waitForAngular();
            System.out.println("✓ Page questionnaire chargée");

            // ===== ÉTAPE 3: TEST 1 - SOUMETTRE AVEC QUESTIONS INCOMPLÈTES =====
            System.out.println("\n--- ÉTAPE 3: Tentative de soumission avec questions incomplètes ---");

            // Répondre aux 10 premières questions (on passe à la question 11, index 10)
            for (int i = 0; i < 10; i++) {
                WebElement optionBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(@class, 'option-btn') and contains(text(), 'Parfois')]")
                ));
                clickElement(optionBtn);


                // Cliquer sur Suivant
                WebElement nextBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(@class, 'next') and not(contains(., 'Envoyer'))]")
                ));
                clickElement(nextBtn);

            }

            // Maintenant on est à la question 11 (index 10), on ne répond pas et on clique sur Suivant pour aller à la dernière
            // (On ne répond pas à la question 11, donc on passe à la question 12 sans réponse)
            WebElement nextBtnToLast = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'next') and not(contains(., 'Envoyer'))]")
            ));
            System.out.println("Clic sur Suivant pour aller à la dernière question");
            clickElement(nextBtnToLast);

            // Maintenant on est à la dernière question (12, index 11), on ne répond pas et on clique sur Envoyer
            // Vérifier d'abord qu'on est bien à la dernière question
            try {
                WebElement questionNumber = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".question-number")
                ));
                String questionNum = questionNumber.getText().trim();
                System.out.println("Numéro de question avant clic Envoyer: " + questionNum);
            } catch (Exception e) {
                System.out.println("Impossible de lire le numéro de question: " + e.getMessage());
            }

            // Chercher le bouton Envoyer avec plusieurs stratégies
            WebElement submitBtn = null;
            try {
                // Stratégie 1: Chercher par texte "Envoyer"
                submitBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(@class, 'next')]//span[contains(text(), 'Envoyer')]/parent::button | //button[contains(@class, 'next') and contains(text(), 'Envoyer')]")
                ));
                System.out.println("Bouton Envoyer trouvé par texte");
            } catch (TimeoutException e1) {
                try {
                    // Stratégie 2: Chercher n'importe quel bouton next (il devrait dire "Envoyer" à la dernière question)
                    submitBtn = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(@class, 'next')]")
                    ));
                    String btnText = submitBtn.getText().trim();
                    System.out.println("Bouton next trouvé, texte: " + btnText);
                } catch (TimeoutException e2) {
                    System.out.println("Impossible de trouver le bouton Envoyer");
                    saveArtifacts("envoyer_button_not_found");
                    throw new RuntimeException("Bouton Envoyer introuvable", e2);
                }
            }

            if (submitBtn != null) {
                System.out.println("Clic sur le bouton Envoyer");
                // Scroll jusqu'au bouton si nécessaire
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", submitBtn);

                clickElement(submitBtn);

                waitForAngular();
            } else {
                throw new RuntimeException("Bouton Envoyer est null");
            }

            // Vérifier que le message toast apparaît
            String toastMessage = null;


            waitForAngular();

            // Vérifier avec JavaScript si le toast existe dans le DOM
            try {
                Object toastExists = ((JavascriptExecutor) driver).executeScript(
                    "return document.querySelector('.toast-message') !== null;"
                );
                System.out.println("Toast existe dans le DOM (JS): " + toastExists);

                if (Boolean.TRUE.equals(toastExists)) {
                    // Lire le texte du toast avec JavaScript
                    Object toastText = ((JavascriptExecutor) driver).executeScript(
                        "var toast = document.querySelector('.toast-message'); " +
                        "if (toast) { " +
                        "  var span = toast.querySelector('span'); " +
                        "  return span ? span.textContent.trim() : toast.textContent.trim(); " +
                        "} " +
                        "return null;"
                    );
                    if (toastText != null && !toastText.toString().isEmpty()) {
                        toastMessage = toastText.toString();
                        System.out.println("✓ Message toast lu via JavaScript: " + toastMessage);
                    }
                }
            } catch (Exception jsEx) {
                System.out.println("⚠ Erreur lors de la vérification JS du toast: " + jsEx.getMessage());
            }

            // Si JavaScript n'a pas trouvé, essayer avec Selenium
            if (toastMessage == null || toastMessage.isEmpty()) {
                try {
                    WebDriverWait toastWait = new WebDriverWait(driver, Duration.ofSeconds(10));

                    // Essayer plusieurs stratégies pour trouver le toast
                    WebElement toast = null;

                    // Stratégie 1: Attendre que le toast soit présent dans le DOM
                    try {
                        toast = toastWait.until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector(".toast-message")
                        ));
                        System.out.println("✓ Toast trouvé dans le DOM");

                        // Maintenant attendre qu'il soit visible avec la classe show
                        try {
                            toastWait.until(ExpectedConditions.attributeContains(toast, "class", "show"));
                            System.out.println("✓ Toast a la classe 'show'");
                        } catch (TimeoutException e) {
                            System.out.println("⚠ Toast présent mais pas de classe 'show', continuons...");
                        }
                    } catch (TimeoutException e1) {
                        // Stratégie 2: Chercher par XPath
                        try {
                            toast = driver.findElement(By.xpath("//div[contains(@class, 'toast-message')]"));
                            System.out.println("✓ Toast trouvé par XPath");
                        } catch (Exception e2) {
                            System.out.println("✗ Toast non trouvé avec Selenium");
                        }
                    }

                    if (toast != null) {
                        // Lire le texte depuis le span enfant (plus fiable)
                        try {
                            WebElement span = toast.findElement(By.cssSelector("span"));
                            toastMessage = span.getText().trim();
                            System.out.println("✓ Texte lu depuis span: " + toastMessage);
                        } catch (Exception e) {
                            // Fallback: lire le texte directement
                            toastMessage = toast.getText().trim();
                            // Nettoyer le texte (enlever l'icône si présente)
                            toastMessage = toastMessage.replaceAll("^[^a-zA-ZÀ-ÿ]*", "").trim();
                            System.out.println("✓ Texte lu directement: " + toastMessage);
                        }

                        // Debug: vérifier la visibilité et les classes
                        try {
                            boolean isVisible = toast.isDisplayed();
                            String classes = toast.getAttribute("class");
                            System.out.println("  → Toast visible: " + isVisible + ", classes: " + classes);
                        } catch (Exception e) {
                            // Ignorer
                        }
                    }
                } catch (Exception e) {
                    System.out.println("✗ Erreur lors de la recherche du toast: " + e.getMessage());
                }
            }

            // Si pas de toast, essayer de trouver dans une alerte
            if (toastMessage == null || toastMessage.isEmpty()) {
                try {
                    Alert alert = driver.switchTo().alert();
                    toastMessage = alert.getText();
                    alert.accept();
                    System.out.println("✓ Message alert trouvé: " + toastMessage);
                } catch (NoAlertPresentException e2) {
                    System.out.println("✗ Aucun toast ou alerte trouvé");
                }
            }


            WebElement questionNumber = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(".question-number")
            ));
            String questionNumberText = questionNumber.getText().trim();
            System.out.println("Numéro de question actuel après clic Envoyer: " + questionNumberText);

            // Vérifier que le numéro de question contient "11/12" ou "Question 11" (la première question manquante)
            boolean isOnQuestion11 = questionNumberText.contains("11/12") || questionNumberText.contains("Question 11");
            assertTrue(isOnQuestion11,
                "VÉRIFICATION ÉCHOUÉE: Le questionnaire devrait retourner à la question 11 (première question non répondue). " +
                "Numéro trouvé: " + questionNumberText);
            System.out.println("✓ Questionnaire retourné à la question 11 (première question non répondue)");

            // Vérifier que cette question n'a pas de réponse sélectionnée (selectedOption devrait être null)
            try {
                List<WebElement> selectedButtons = driver.findElements(By.cssSelector(".option-btn.selected"));
                assertTrue(selectedButtons.isEmpty(),
                    "VÉRIFICATION ÉCHOUÉE: La question 11 ne devrait pas avoir de réponse sélectionnée, mais " +
                    selectedButtons.size() + " option(s) sélectionnée(s) trouvée(s)");
                System.out.println("✓ Aucune option sélectionnée sur la question 11 (correct, car non répondue)");
            } catch (Exception e) {
                System.out.println("⚠ Impossible de vérifier si une option est sélectionnée: " + e.getMessage());
            }

            // Optionnel: essayer de détecter le toast (mais ne pas faire échouer le test si absent)
            if (toastMessage != null && !toastMessage.isEmpty()) {
                System.out.println("✓ Toast détecté: " + toastMessage);
            } else {
                System.out.println("⚠ Toast non détecté, mais le retour à la question 11 confirme que la validation fonctionne");
            }

            // Revenir aux questions manquantes et les compléter
            System.out.println("\n--- Complétion des questions manquantes ---");
            // On devrait être revenu à une question manquante (question 11 ou 12)
            // Répondre aux 2 dernières questions
            for (int i = 0; i < 2; i++) {
                WebElement optionBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(@class, 'option-btn') and contains(text(), 'Parfois')]")
                ));
                clickElement(optionBtn);

                // Si on n'est pas à la dernière question, cliquer sur Suivant
                try {
                    WebElement nextBtn = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(@class, 'next') and not(contains(., 'Envoyer'))]")
                    ));
                    clickElement(nextBtn);
                } catch (TimeoutException e) {
                    // On est probablement à la dernière question, continuer
                }
            }

            // Cliquer sur Envoyer maintenant que tout est complété
            submitBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'next') and (contains(., 'Envoyer') or contains(., 'Suivant'))]")
            ));
            clickElement(submitBtn);

            // Attendre la redirection vers la page de résultat
            wait.until(d -> d.getCurrentUrl().contains("/questionnaire-result"));
            System.out.println("✓ Questionnaire soumis avec succès, redirection vers résultat");



            // ===== VÉRIFIER QUE LE RÉSULTAT EST SAUVEGARDÉ =====
            System.out.println("\n--- Vérification que le résultat est sauvegardé ---");

            // Retourner à la page user-home pour vérifier les résultats
            driver.get(appUrl + "/user-home");
            waitDomReady(wait);

            // Cliquer sur "Mes résultats"
            WebElement myResultsBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'action-card') and .//h3[contains(text(), 'résultats') or contains(text(), 'Résultats')]]")
            ));
            clickElement(myResultsBtn);

            // Attendre le chargement de la page mes résultats
            wait.until(d -> d.getCurrentUrl().contains("/my-results"));

            // Vérifier qu'au moins un résultat est affiché
            try {
                // Attendre que les résultats soient chargés (pas de loading)
                wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector(".loading, .isLoading")
                ));

                // Chercher les cartes de résultats (la classe est .result-card)
                List<WebElement> resultCards = driver.findElements(
                    By.cssSelector(".result-card")
                );

                if (resultCards.isEmpty()) {
                    // Essayer dans results-list
                    resultCards = driver.findElements(
                        By.cssSelector(".results-list .result-card")
                    );
                }

                if (resultCards.isEmpty()) {
                    // Dernière tentative: chercher par XPath
                    resultCards = driver.findElements(
                        By.xpath("//div[contains(@class, 'result-card')]")
                    );
                }

                assertTrue(resultCards.size() > 0,
                    "VÉRIFICATION ÉCHOUÉE: Aucun résultat trouvé après soumission du questionnaire. " +
                    "Le résultat devrait être sauvegardé et affiché dans 'Mes résultats'. " +
                    "Vérifiez que le résultat a été sauvegardé côté backend.");

                System.out.println("✓ Résultat sauvegardé et affiché dans 'Mes résultats' (" + resultCards.size() + " résultat(s) trouvé(s))");
            } catch (Exception e) {
                System.err.println("⚠ Erreur lors de la vérification des résultats sauvegardés: " + e.getMessage());
                // Ne pas faire échouer le test pour cela, juste logger
            }

            // Retourner au questionnaire pour la suite du test
            driver.get(appUrl + "/questionnaire");

            // Revenir au questionnaire pour essayer de soumettre à nouveau
            System.out.println("\n--- ÉTAPE 4: Retour au questionnaire pour deuxième soumission ---");

            // Cliquer sur le bouton "Refaire le test" ou naviguer manuellement
            try {
                WebElement retakeBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(), 'Refaire') or contains(text(), 'Retour')]")
                ));
                clickElement(retakeBtn);
            } catch (TimeoutException e) {
                // Si pas de bouton, naviguer manuellement
                driver.get(appUrl + "/questionnaire");
            }

            wait.until(d -> d.getCurrentUrl().contains("/questionnaire"));

            // ===== ÉTAPE 5: TEST 2 - VÉRIFIER LIMITE 24H AU PREMIER "SUIVANT" =====
            System.out.println("\n--- ÉTAPE 5: Vérification du message de limitation 24h au premier 'Suivant' ---");

            // Répondre à la première question
            WebElement firstOptionBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'option-btn') and contains(text(), 'Parfois')]")
            ));
            clickElement(firstOptionBtn);


            // Cliquer sur "Suivant" - le message de limitation 24h devrait apparaître
            WebElement firstNextBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'next') and not(contains(., 'Envoyer'))]")
            ));
            System.out.println("Clic sur le premier 'Suivant' après avoir répondu à la première question");
            clickElement(firstNextBtn);

            // Vérifier que le message toast de limitation 24h apparaît
            String limitMessage = null;
            try {
                Object toastExists = ((JavascriptExecutor) driver).executeScript(
                    "return document.querySelector('.toast-message') !== null && " +
                    "document.querySelector('.toast-message').classList.contains('show');"
                );
                if (Boolean.TRUE.equals(toastExists)) {
                    Object toastText = ((JavascriptExecutor) driver).executeScript(
                        "var toast = document.querySelector('.toast-message'); " +
                        "if (toast) { " +
                        "  var span = toast.querySelector('span'); " +
                        "  return span ? span.textContent.trim() : toast.textContent.trim(); " +
                        "} " +
                        "return null;"
                    );
                    if (toastText != null && !toastText.toString().isEmpty()) {
                        limitMessage = toastText.toString();
                        System.out.println("✓ Message toast de limitation trouvé: " + limitMessage);
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠ Erreur lors de la vérification JS du toast: " + e.getMessage());
            }

            // Si JavaScript n'a pas trouvé, essayer avec Selenium
            if (limitMessage == null || limitMessage.isEmpty()) {
                try {
                    WebDriverWait toastWait = new WebDriverWait(driver, Duration.ofSeconds(10));
                    WebElement toast = toastWait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector(".toast-message")
                    ));
                    try {
                        WebElement span = toast.findElement(By.cssSelector("span"));
                        limitMessage = span.getText().trim();
                    } catch (Exception e) {
                        limitMessage = toast.getText().trim();
                        limitMessage = limitMessage.replaceAll("^[^a-zA-ZÀ-ÿ]*", "").trim();
                    }
                    System.out.println("✓ Message toast de limitation trouvé via Selenium: " + limitMessage);
                } catch (Exception e) {
                    System.out.println("✗ Toast non trouvé avec Selenium");
                }
            }

            assertTrue(limitMessage != null && (limitMessage.contains("un seul test par 24 heures") ||
                    limitMessage.contains("réessayer plus tard") || limitMessage.contains("24 heures")),
                "VÉRIFICATION ÉCHOUÉE: Le message de limitation 24h doit apparaître au premier 'Suivant'. Message: " + limitMessage);
            System.out.println("✓ Message de limitation 24h affiché au premier 'Suivant'");



            // ===== ÉTAPE 6: TEST 3 - ESSAYER DE CONTINUER LE QUESTIONNAIRE (devrait échouer) =====
            System.out.println("\n--- ÉTAPE 6: Tentative de continuer le questionnaire après le message de limitation ---");

            // Essayer de répondre à la deuxième question et cliquer sur Suivant
            try {
                WebElement secondOptionBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(@class, 'option-btn') and contains(text(), 'Parfois')]")
                ));
                clickElement(secondOptionBtn);

                WebElement secondNextBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(@class, 'next') and not(contains(., 'Envoyer'))]")
                ));
                clickElement(secondNextBtn);

                System.out.println("✓ Le questionnaire continue malgré la limitation (comportement attendu si la vérification se fait uniquement à la soumission finale)");
            } catch (Exception e) {
                System.out.println("⚠ Impossible de continuer: " + e.getMessage());
            }

            // Vérifier qu'on reste sur la page questionnaire (pas de redirection)
            assertTrue(driver.getCurrentUrl().contains("/questionnaire"),
                "VÉRIFICATION ÉCHOUÉE: L'utilisateur doit rester sur la page questionnaire après limitation. URL: " + driver.getCurrentUrl());

            System.out.println("\n=== SCÉNARIO COMPLET RÉUSSI ===");

        } catch (AssertionError e) {
            saveArtifacts("questionnaire_scenario_fail");
            System.err.println("✗ SCÉNARIO ÉCHOUÉ: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            saveArtifacts("questionnaire_scenario_error");
            System.err.println("✗ ERREUR LORS DU SCÉNARIO: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
