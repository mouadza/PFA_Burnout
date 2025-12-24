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
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserApprovalTest {

    private static String appUrl;
    private WebDriver driver;

    // Credentials admin
    private static final String ADMIN_EMAIL = "admin@gmail.com";
    private static final String ADMIN_PASSWORD = "admin1234";

    // Credentials utilisateur test (générés dynamiquement pour éviter les conflits)
    private String testUserEmail;
    private static final String TEST_USER_PASSWORD = "test10";
    private static final String TEST_USER_FIRST_NAME = "Test";
    private static final String TEST_USER_LAST_NAME = "User";

    @BeforeAll
    void setupClass() {
        WebDriverManager.chromedriver().setup();
        appUrl = System.getProperty("app.url", "http://localhost:4200");

        // Générer un email unique pour éviter les conflits
        Random random = new Random();
        int randomNum = random.nextInt(10000);
        testUserEmail = "testuser" + randomNum + "@gmail.com";

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
        try { 
            Thread.sleep(ms); 
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
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
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(d -> {
                Object result = ((JavascriptExecutor) d).executeScript(
                    "return typeof angular !== 'undefined' && angular.element(document.body).injector().get('$http').pendingRequests.length === 0"
                );
                return result != null && (result.equals(true) || result.equals(0));
            });
        } catch (Exception e) {
            // Angular might not be available, continue anyway
        }
        sleep(80);
    }

    @Test
    @DisplayName("Scénario complet: Inscription → Login non approuvé → Admin approuve → Login réussi")
    void completeUserApprovalScenario() throws InterruptedException {
        WebDriverWait wait = wait45();
        
        try {
            System.out.println("\n=== DÉBUT DU SCÉNARIO COMPLET D'APPROBATION ===");
            System.out.println("Email utilisateur test: " + testUserEmail);

            // ===== ÉTAPE 1: INSCRIPTION D'UN NOUVEAU UTILISATEUR =====
            System.out.println("\n--- ÉTAPE 1: Inscription du nouvel utilisateur ---");
            driver.get(appUrl + "/register");
            waitDomReady(wait);
            sleep(80);
            waitForAngular();

            // Remplir le formulaire d'inscription
            WebElement firstNameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[formcontrolname='firstName'], input[placeholder*='Prénom']")
            ));
            clearAndType(firstNameInput, TEST_USER_FIRST_NAME);

            WebElement lastNameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[formcontrolname='lastName'], input[placeholder*='Nom']")
            ));
            clearAndType(lastNameInput, TEST_USER_LAST_NAME);

            WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[formcontrolname='email'], input[type='email']")
            ));
            clearAndType(emailInput, testUserEmail);

            WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[formcontrolname='password'], input[type='password']")
            ));
            clearAndType(passwordInput, TEST_USER_PASSWORD);

            // Sélectionner la profession
            WebElement professionSelect = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("select[formcontrolname='profession']")
            ));
            Select professionDropdown = new Select(professionSelect);
            professionDropdown.selectByValue("MEDECIN");

            sleep(80);

            // Cliquer sur le bouton d'inscription
            WebElement registerButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button[type='submit'], button.btn-register")
            ));
            clickElement(registerButton);

            // Attendre et gérer l'alerte de succès
            sleep(80);
            WebDriverWait alertWait = new WebDriverWait(driver, Duration.ofSeconds(10));
            try {
                Alert alert = alertWait.until(ExpectedConditions.alertIsPresent());
                String alertText = alert.getText();
                System.out.println("Alert reçue: " + alertText);
                alert.accept();
                System.out.println("✓ Alerte fermée");
            } catch (TimeoutException e) {
                System.out.println("⚠ Aucune alerte présente");
            }

            // Attendre un peu après la fermeture de l'alerte
            sleep(80);

            // Vérifier qu'on est redirigé vers /login
            wait.until(d -> {
                try {
                    String url = d.getCurrentUrl();
                    return url.contains("/login");
                } catch (UnhandledAlertException e) {
                    // Si une alerte est toujours présente, l'accepter
                    try {
                        driver.switchTo().alert().accept();
                    } catch (Exception ignored) {}
                    return false;
                }
            });
            System.out.println("✓ Inscription réussie, redirection vers /login");

            sleep(80);
            clearSession();

            // ===== ÉTAPE 2: TENTATIVE DE CONNEXION AVEC COMPTE NON APPROUVÉ =====
            System.out.println("\n--- ÉTAPE 2: Tentative de connexion avec compte non approuvé ---");
            driver.get(appUrl + "/login");
            waitDomReady(wait);
            sleep(80);
            waitForAngular();

            // Remplir le formulaire de connexion
            WebElement loginEmailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[formcontrolname='email'], input[type='email']")
            ));
            clearAndType(loginEmailInput, testUserEmail);

            WebElement loginPasswordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[formcontrolname='password'], input[type='password']")
            ));
            clearAndType(loginPasswordInput, TEST_USER_PASSWORD);

            sleep(80);

            // Cliquer sur le bouton de connexion
            WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button[type='submit'], button.btn-login")
            ));
            clickElement(loginButton);

            // Attendre le message d'erreur (5 secondes de chargement selon les specs)
            sleep(80);
            waitForAngular();

            // Vérifier le message d'erreur
            WebElement errorMessage = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".error-message .error-text, .error-message span")
            ));
            String errorText = errorMessage.getText().trim();
            System.out.println("Message d'erreur affiché: " + errorText);

            assertTrue(errorText.toLowerCase().contains("activé") || 
                      errorText.toLowerCase().contains("administrateur") ||
                      errorText.toLowerCase().contains("doit être"),
                "VÉRIFICATION ÉCHOUÉE: Le message d'erreur doit indiquer que le compte doit être activé. " +
                "Message trouvé: '" + errorText + "'");

            System.out.println("✓ Message d'erreur correctement affiché");

            // Vérifier qu'on reste sur la page de login
            assertTrue(driver.getCurrentUrl().contains("/login"),
                "VÉRIFICATION ÉCHOUÉE: L'utilisateur doit rester sur la page de login");

            clearSession();

            // ===== ÉTAPE 3: ADMIN SE CONNECTE =====
            System.out.println("\n--- ÉTAPE 3: Connexion de l'administrateur ---");
            driver.get(appUrl + "/login");
            waitDomReady(wait);
            sleep(80);
            waitForAngular();

            // Remplir le formulaire de connexion admin
            WebElement adminEmailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[formcontrolname='email'], input[type='email']")
            ));
            clearAndType(adminEmailInput, ADMIN_EMAIL);

            WebElement adminPasswordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[formcontrolname='password'], input[type='password']")
            ));
            clearAndType(adminPasswordInput, ADMIN_PASSWORD);

            sleep(80);

            // Cliquer sur le bouton de connexion
            WebElement adminLoginButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button[type='submit'], button.btn-login")
            ));
            clickElement(adminLoginButton);

            // Attendre la redirection vers admin-home
            wait.until(d -> d.getCurrentUrl().contains("/admin-home"));
            System.out.println("✓ Admin connecté avec succès");

            sleep(80);
            waitForAngular();

            // ===== ÉTAPE 4: ADMIN VA À GESTION DES UTILISATEURS =====
            System.out.println("\n--- ÉTAPE 4: Navigation vers Gestion Utilisateurs ---");
            
            // Cliquer sur le lien "Gestion Utilisateurs" dans la sidebar ou navigation
            WebElement usersLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(text(), 'Gestion') or contains(text(), 'Utilisateurs') or contains(@href, 'admin-users')]")
            ));
            clickElement(usersLink);

            // Attendre le chargement de la page
            wait.until(d -> d.getCurrentUrl().contains("/admin-users"));
            sleep(80);
            waitForAngular();
            
            // Attendre que la liste des utilisateurs soit chargée
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(".users-list, .user-card, .loading")
            ));
            // Attendre que le loading disparaisse
            try {
                wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector(".loading")
                ));
            } catch (TimeoutException e) {
                // Si pas de loading, continuer
            }
            sleep(80);
            System.out.println("✓ Page Gestion Utilisateurs chargée");

            // ===== ÉTAPE 5: ADMIN CLIQUE SUR LE COMPTE NON APPROUVÉ =====
            System.out.println("\n--- ÉTAPE 5: Recherche et clic sur le compte utilisateur test ---");
            
            // Trouver la carte utilisateur avec l'email du test
            WebElement userCard = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[contains(@class, 'user-card')]//p[contains(text(), '" + testUserEmail + "')]/ancestor::div[contains(@class, 'user-card')]")
            ));
            
            // Vérifier que le compte n'est pas approuvé (status-indicator n'a pas la classe 'active')
            WebElement statusIndicator = userCard.findElement(By.cssSelector(".status-indicator"));
            String statusClass = statusIndicator.getAttribute("class");
            assertFalse(statusClass.contains("active"),
                "VÉRIFICATION ÉCHOUÉE: Le compte devrait être non approuvé avant l'approbation");

            System.out.println("✓ Compte utilisateur trouvé et non approuvé");

            // Cliquer sur la carte utilisateur pour voir les détails
            System.out.println("Clic sur la carte utilisateur avec email: " + testUserEmail);
            clickElement(userCard);
            sleep(100);

            // Attendre le chargement de la page de détails
            wait.until(d -> {
                String url = d.getCurrentUrl();
                return url != null && url.contains("/admin-user-details");
            });
            System.out.println("URL actuelle après clic: " + driver.getCurrentUrl());
            sleep(80);
            waitForAngular();
            
            // Attendre que la page soit complètement chargée (pas en loading)
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector(".loading")
            ));
            // Attendre que les détails utilisateur soient présents
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(".user-details, .approval-section")
            ));
            sleep(80);
            System.out.println("✓ Page de détails utilisateur chargée");

            // ===== ÉTAPE 6: ADMIN APPROUVE LE COMPTE =====
            System.out.println("\n--- ÉTAPE 6: Approbation du compte utilisateur ---");
            
            // Essayer plusieurs sélecteurs pour trouver la checkbox
            WebElement approvalToggle = null;
            try {
                // Essayer d'abord avec le sélecteur spécifique dans toggle-switch
                approvalToggle = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".toggle-switch input[type='checkbox']")
                ));
                System.out.println("✓ Checkbox trouvée avec sélecteur .toggle-switch");
            } catch (TimeoutException e) {
                System.out.println("⚠ Tentative avec sélecteur alternatif...");
                try {
                    // Chercher toutes les checkboxes
                    List<WebElement> checkboxes = driver.findElements(By.cssSelector("input[type='checkbox']"));
                    System.out.println("Nombre de checkboxes trouvées: " + checkboxes.size());
                    
                    if (checkboxes.isEmpty()) {
                        // Essayer sans filtre displayed/enabled car le toggle peut cacher la checkbox
                        checkboxes = driver.findElements(By.xpath("//input[@type='checkbox']"));
                        System.out.println("Nombre de checkboxes (xpath): " + checkboxes.size());
                    }
                    
                    for (WebElement cb : checkboxes) {
                        // Ne pas vérifier isDisplayed() car dans un toggle-switch, elle peut être masquée visuellement
                        // mais reste cliquable via le label
                        try {
                            if (cb.isEnabled()) {
                                approvalToggle = cb;
                                System.out.println("✓ Checkbox trouvée (enabled)");
                                break;
                            }
                        } catch (Exception ex) {
                            // Si l'élément n'est plus dans le DOM, continuer
                            continue;
                        }
                    }
                    
                    // Si toujours null, prendre la première checkbox trouvée
                    if (approvalToggle == null && !checkboxes.isEmpty()) {
                        approvalToggle = checkboxes.get(0);
                        System.out.println("✓ Checkbox trouvée (première disponible)");
                    }
                } catch (Exception e2) {
                    System.err.println("✗ Impossible de trouver la checkbox: " + e2.getMessage());
                    e2.printStackTrace();
                }
            }
            
            if (approvalToggle == null) {
                // Sauvegarder les artifacts pour debug
                saveArtifacts("approval_checkbox_not_found");
                
                // Afficher le HTML de la page pour debug
                System.out.println("=== DEBUG: HTML de la page ===");
                System.out.println(driver.getPageSource().substring(0, Math.min(200, driver.getPageSource().length())));
                
                throw new RuntimeException("Checkbox d'approbation est null");
            }
            
            // Si la checkbox est dans un toggle-switch, on peut cliquer sur le label à la place
            try {
                WebElement label = approvalToggle.findElement(By.xpath("./ancestor::label[contains(@class, 'toggle-switch')]"));
                if (label != null) {
                    System.out.println("✓ Label toggle-switch trouvé, on utilisera le label pour cliquer");
                }
            } catch (Exception e) {
                // Pas de label, on utilisera la checkbox directement
            }
            
            // Vérifier l'état initial avec JavaScript si nécessaire
            boolean isCurrentlySelected;
            try {
                isCurrentlySelected = approvalToggle.isSelected();
            } catch (Exception e) {
                // Si isSelected() échoue, utiliser JavaScript
                Object result = ((JavascriptExecutor) driver).executeScript("return arguments[0].checked;", approvalToggle);
                isCurrentlySelected = Boolean.TRUE.equals(result);
            }
            System.out.println("État initial de la checkbox: " + (isCurrentlySelected ? "cochée" : "non cochée"));
            
            // Vérifier que le toggle n'est pas coché (compte non approuvé)
            if (!isCurrentlySelected) {
                // Scroll jusqu'à l'élément si nécessaire
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", approvalToggle);
                sleep(80);
                
                // Essayer de cliquer sur le label parent si disponible (plus fiable pour toggle-switch)
                try {
                    WebElement label = approvalToggle.findElement(By.xpath("./ancestor::label[contains(@class, 'toggle-switch')]"));
                    if (label != null) {
                        System.out.println("Clic sur le label toggle-switch");
                        clickElement(label);
                    } else {
                        clickElement(approvalToggle);
                    }
                } catch (Exception e) {
                    // Si pas de label, cliquer directement sur la checkbox avec JavaScript
                    System.out.println("Clic direct sur la checkbox avec JavaScript");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", approvalToggle);
                }
                
                System.out.println("✓ Toggle d'approbation cliqué");
                
                // Attendre que le changement soit sauvegardé (vérifier que isSaving disparaît)
                sleep(80);
                waitForAngular();
                
                // Re-find l'élément pour vérifier son nouvel état
                approvalToggle = driver.findElement(By.cssSelector(".toggle-switch input[type='checkbox']"));
                
                // Vérifier que le toggle est maintenant coché
                boolean isNowSelected;
                try {
                    isNowSelected = approvalToggle.isSelected();
                } catch (Exception e) {
                    Object result = ((JavascriptExecutor) driver).executeScript("return arguments[0].checked;", approvalToggle);
                    isNowSelected = Boolean.TRUE.equals(result);
                }
                
                assertTrue(isNowSelected,
                    "VÉRIFICATION ÉCHOUÉE: Le toggle devrait être coché après l'approbation. État actuel: " + isNowSelected);
                
                System.out.println("✓ Compte approuvé avec succès");
            } else {
                System.out.println("⚠ Compte déjà approuvé");
            }

            // ===== ÉTAPE 7: ADMIN SE DÉCONNECTE =====
            System.out.println("\n--- ÉTAPE 7: Déconnexion de l'administrateur ---");
            
            // Le bouton de déconnexion est dans le sidebar avec la classe logout-btn
            // Il peut être dans .sidebar-footer .logout-btn
            WebElement logoutButton = null;
            try {
                // Essayer d'abord avec le sélecteur CSS spécifique
                logoutButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector(".logout-btn, button.logout-btn, .sidebar-footer .logout-btn")
                ));
                System.out.println("✓ Bouton de déconnexion trouvé avec CSS");
            } catch (TimeoutException e) {
                // Essayer avec XPath
                try {
                    logoutButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(@class, 'logout-btn')] | //button[.//span[contains(text(), 'Déconnexion')]]")
                    ));
                    System.out.println("✓ Bouton de déconnexion trouvé avec XPath");
                } catch (TimeoutException e2) {
                    // Essayer de trouver par texte
                    try {
                        logoutButton = wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//button[contains(., 'Déconnexion')] | //button[.//span[contains(text(), 'Déconnexion')]]")
                        ));
                        System.out.println("✓ Bouton de déconnexion trouvé par texte");
                    } catch (TimeoutException e3) {
                        System.err.println("✗ Impossible de trouver le bouton de déconnexion");
                        saveArtifacts("logout_button_not_found");
                        throw new RuntimeException("Bouton de déconnexion introuvable", e3);
                    }
                }
            }
            
            // S'assurer que le sidebar est ouvert si nécessaire
            try {
                WebElement sidebar = driver.findElement(By.cssSelector(".sidebar"));
                String sidebarClass = sidebar.getAttribute("class");
                if (sidebarClass != null && sidebarClass.contains("collapsed")) {
                    // Ouvrir le sidebar
                    WebElement toggleBtn = driver.findElement(By.cssSelector(".toggle-btn"));
                    clickElement(toggleBtn);
                    sleep(80);
                }
            } catch (Exception e) {
                // Ignorer si on ne peut pas vérifier/ouvrir le sidebar
            }
            
            clickElement(logoutButton);

            // Attendre la redirection vers /login
            wait.until(d -> d.getCurrentUrl().contains("/login"));
            System.out.println("✓ Admin déconnecté avec succès");

            sleep(80);
            clearSession();

            // ===== ÉTAPE 8: L'UTILISATEUR PEUT MAINTENANT SE CONNECTER =====
            System.out.println("\n--- ÉTAPE 8: Connexion de l'utilisateur approuvé ---");
            driver.get(appUrl + "/login");
            waitDomReady(wait);
            sleep(80);
            waitForAngular();

            // Remplir le formulaire de connexion
            WebElement finalEmailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[formcontrolname='email'], input[type='email']")
            ));
            clearAndType(finalEmailInput, testUserEmail);

            WebElement finalPasswordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[formcontrolname='password'], input[type='password']")
            ));
            clearAndType(finalPasswordInput, TEST_USER_PASSWORD);

            sleep(80);

            // Cliquer sur le bouton de connexion
            WebElement finalLoginButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button[type='submit'], button.btn-login")
            ));
            clickElement(finalLoginButton);

            // Attendre la redirection vers user-home
            wait.until(d -> d.getCurrentUrl().contains("/user-home"));
            System.out.println("✓ Redirection vers user-home réussie");

            // Vérifier qu'on est bien sur la page user-home
            assertTrue(driver.getCurrentUrl().contains("/user-home"),
                "VÉRIFICATION ÉCHOUÉE: L'utilisateur devrait être redirigé vers /user-home après connexion réussie");

            // Attendre que la page soit complètement chargée
            sleep(80);
            waitForAngular();
            
            // Vérifier que le dashboard utilisateur est bien affiché (pas de message d'erreur)
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(".layout-container, .user-home-container, .dashboard")
            ));
            
            // Attendre un peu pour s'assurer que tout est chargé
            sleep(80);
            
            System.out.println("✓ Utilisateur connecté avec succès après approbation");
            System.out.println("✓ Dashboard utilisateur chargé et affiché");

            System.out.println("\n=== SCÉNARIO COMPLET RÉUSSI ===");
            System.out.println("✓ Toutes les étapes ont été exécutées avec succès:");
            System.out.println("  1. ✓ Inscription utilisateur");
            System.out.println("  2. ✓ Tentative de connexion (compte non approuvé)");
            System.out.println("  3. ✓ Connexion administrateur");
            System.out.println("  4. ✓ Navigation vers Gestion Utilisateurs");
            System.out.println("  5. ✓ Sélection du compte utilisateur");
            System.out.println("  6. ✓ Approbation du compte");
            System.out.println("  7. ✓ Déconnexion administrateur");
            System.out.println("  8. ✓ Connexion utilisateur approuvé");

        } catch (AssertionError e) {
            saveArtifacts("user_approval_scenario_fail");
            System.err.println("✗ SCÉNARIO ÉCHOUÉ: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            saveArtifacts("user_approval_scenario_error");
            System.err.println("✗ ERREUR LORS DU SCÉNARIO: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void clearAndType(WebElement element, String text) {
        element.click();
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        element.sendKeys(Keys.BACK_SPACE);
        element.sendKeys(text);
    }

    private void clickElement(WebElement element) {
        try {
            // Essayer de cliquer normalement
            element.click();
        } catch (WebDriverException e) {
            // Si échec, essayer avec JavaScript
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            } catch (Exception e2) {
                // Dernier recours: Actions
                new org.openqa.selenium.interactions.Actions(driver).moveToElement(element).click().perform();
            }
        }
    }

    private void saveArtifacts(String prefix) {
        try {
            Path artifactsDir = Paths.get("target", "selenium-artifacts");
            Files.createDirectories(artifactsDir);

            // Screenshot
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(screenshot.toPath(), artifactsDir.resolve(prefix + "_screenshot.png"), 
                StandardCopyOption.REPLACE_EXISTING);

            // HTML source
            String pageSource = driver.getPageSource();
            Files.write(artifactsDir.resolve(prefix + "_source.html"), pageSource.getBytes());

            System.out.println("Artifacts sauvegardés dans: " + artifactsDir.toString());
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde des artifacts: " + e.getMessage());
        }
    }
}

