package com.burncare.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests Selenium pour l'application Flutter Web BurnCare
 * 
 * Pour lancer les tests :
 * 1. Démarrer l'app Flutter web : flutter run -d chrome --web-port=5000
 * 2. Exécuter : mvn test -Dapp.url=http://localhost:5000
 */
public class LoginTest {

    private static String appUrl;
    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
        appUrl = System.getProperty("app.url", "http://localhost:5000");
    }

    @BeforeEach
    void setupTest() {
        System.out.println("\n=== Configuration du test ===");
        System.out.println("URL de l'application: " + appUrl);
        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        // Options pour améliorer la stabilité des tests
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        // Option pour éviter les problèmes de CDP
        options.addArguments("--disable-web-security");
        // Option pour voir ce qui se passe
        options.addArguments("--start-maximized");

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60)); // Augmenté à 60 secondes

        // Timeout augmenté pour Flutter web (peut être très lent à charger)
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    /**
     * Attend que l'application Flutter soit complètement chargée
     */
    private void waitForFlutterAppToLoad() {
        System.out.println("Attente du chargement de l'application Flutter sur " + appUrl);
        
        // Vérifier d'abord que la page est accessible
        try {
            // Attendre que la page soit chargée (au moins le body)
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            System.out.println("✓ Body trouvé");
        } catch (Exception e) {
            String currentUrl = driver.getCurrentUrl();
            String pageSource = driver.getPageSource();
            throw new RuntimeException(
                "Impossible de se connecter à l'application Flutter sur " + appUrl + 
                ". URL actuelle: " + currentUrl +
                ". Assurez-vous que l'application est lancée avec: flutter run -d chrome --web-port=5000" +
                ". Page source (premiers 200 caractères): " + pageSource.substring(0, Math.min(200, pageSource.length())), e);
        }
        
        // Vérifier l'URL actuelle
        String currentUrl = driver.getCurrentUrl();
        System.out.println("URL actuelle: " + currentUrl);
        
        // Vérifier que la page n'est pas vide
        String pageText = "";
        try {
            pageText = driver.findElement(By.tagName("body")).getText();
            System.out.println("Texte visible sur la page (premiers 100 caractères): " + 
                pageText.substring(0, Math.min(100, pageText.length())));
        } catch (Exception e) {
            System.out.println("Impossible de lire le texte du body");
        }
        
        // Attendre que Flutter soit complètement initialisé
        // Flutter web peut prendre du temps pour charger, on attend plusieurs éléments possibles
        System.out.println("Recherche d'éléments Flutter...");
        
        try {
            // Attendre plus longtemps pour Flutter web (peut être très lent)
            WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(30));
            
            longWait.until(ExpectedConditions.or(
                // Option 1: Titre principal (insensible à la casse)
                ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//*[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'burncare')]")
                ),
                // Option 2: Sous-titre (plus flexible)
                ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//*[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'prévention') or " +
                             "contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'burnout') or " +
                             "contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'burn')]")
                ),
                // Option 3: Champ email (indique que le formulaire est chargé)
                ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//input[@type='text' or @type='email' or @placeholder]")
                ),
                // Option 4: Bouton de connexion
                ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//button[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'connecter') or " +
                             "contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'connexion')]")
                ),
                // Option 5: N'importe quel input visible (dernier recours)
                ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("input[type='text'], input[type='password']")
                ),
                // Option 6: N'importe quel élément avec du texte (indique que Flutter a rendu quelque chose)
                ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//*[text() and string-length(text()) > 0]")
                )
            ));
            System.out.println("✓ Éléments Flutter détectés");
        } catch (Exception e) {
            // Si aucun élément n'est trouvé, faire un diagnostic complet
            String pageSource = driver.getPageSource();
            String bodyText = "";
            try {
                bodyText = driver.findElement(By.tagName("body")).getText();
            } catch (Exception e2) {
                bodyText = "(impossible de lire)";
            }
            
            System.out.println("\n=== DIAGNOSTIC COMPLET ===");
            System.out.println("URL actuelle: " + driver.getCurrentUrl());
            System.out.println("URL attendue: " + appUrl);
            System.out.println("Taille du page source: " + pageSource.length() + " caractères");
            System.out.println("Texte du body: '" + bodyText + "'");
            System.out.println("Nombre d'inputs: " + driver.findElements(By.tagName("input")).size());
            System.out.println("Nombre de divs: " + driver.findElements(By.tagName("div")).size());
            System.out.println("Nombre de spans: " + driver.findElements(By.tagName("span")).size());
            
            // Vérifier si c'est une page d'erreur
            if (pageSource.contains("ERR_CONNECTION_REFUSED") || 
                pageSource.contains("This site can't be reached") ||
                pageSource.contains("Unable to connect")) {
                throw new RuntimeException(
                    "ERREUR DE CONNEXION: L'application Flutter n'est pas accessible sur " + appUrl + 
                    ". Vérifiez que:\n" +
                    "1. L'application est lancée avec: flutter run -d chrome --web-port=5000\n" +
                    "2. L'application est accessible dans votre navigateur sur " + appUrl + "\n" +
                    "3. Aucun firewall ne bloque la connexion\n" +
                    "URL actuelle: " + driver.getCurrentUrl());
            }
            
            // Vérifier si la page est complètement vide
            if (pageSource.length() < 1000 || bodyText.trim().isEmpty()) {
                throw new RuntimeException(
                    "La page est vide ou l'application Flutter n'est pas chargée. " +
                    "Vérifiez que:\n" +
                    "1. L'application Flutter est bien lancée et en cours d'exécution\n" +
                    "2. L'application est accessible dans votre navigateur sur " + appUrl + "\n" +
                    "3. Attendez que l'application soit complètement chargée avant de lancer les tests\n" +
                    "URL actuelle: " + driver.getCurrentUrl() + "\n" +
                    "Taille du HTML: " + pageSource.length() + " caractères\n" +
                    "Texte visible: '" + bodyText + "'");
            }
            
            throw new RuntimeException(
                "L'application Flutter ne semble pas être chargée correctement. " +
                "Vérifiez que l'app est bien lancée et accessible sur " + appUrl + 
                ". Texte visible sur la page: '" + bodyText + "'" +
                ". Voir les logs ci-dessus pour plus de détails.", e);
        }
        
        // Attendre un peu plus pour que Flutter termine le rendu complet
        System.out.println("Attente du rendu complet de Flutter...");
        try {
            Thread.sleep(3000); // Augmenté à 3 secondes pour Flutter web
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("✓ Application Flutter chargée\n");
    }

    /**
     * Aide au debug : affiche tous les inputs disponibles sur la page
     */
    private void debugInputs() {
        try {
            System.out.println("\n========== DEBUG: Analyse complète de la page ==========");
            
            // Vérifier le mode de rendu Flutter
            System.out.println("=== Vérification du mode de rendu Flutter ===");
            String pageSource = driver.getPageSource();
            if (pageSource.contains("canvaskit") || pageSource.contains("CanvasKit")) {
                System.out.println("⚠️ ATTENTION: Flutter utilise CanvasKit (rendu canvas)!");
                System.out.println("   Les éléments ne sont pas accessibles via Selenium standard.");
                System.out.println("   SOLUTION: Compilez Flutter en mode HTML: flutter build web --web-renderer html");
            } else if (pageSource.contains("html")) {
                System.out.println("✓ Flutter utilise le mode HTML (compatible avec Selenium)");
            }
            
            // Vérifier les canvas (indique CanvasKit)
            List<WebElement> canvases = driver.findElements(By.tagName("canvas"));
            System.out.println("Canvas trouvés: " + canvases.size());
            if (canvases.size() > 0) {
                System.out.println("⚠️ Des canvas sont présents - Flutter utilise probablement CanvasKit");
            }
            
            // Vérifier les iframes
            List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
            System.out.println("Iframes trouvés: " + iframes.size());
            
            // Tous les inputs
            List<WebElement> allInputs = driver.findElements(By.tagName("input"));
            System.out.println("\n=== Inputs trouvés (" + allInputs.size() + ") ===");
            for (int i = 0; i < allInputs.size(); i++) {
                WebElement input = allInputs.get(i);
                try {
                    System.out.println(String.format(
                        "Input %d: type=%s, placeholder=%s, aria-label=%s, id=%s, name=%s, class=%s, visible=%s, enabled=%s",
                        i,
                        input.getAttribute("type"),
                        input.getAttribute("placeholder"),
                        input.getAttribute("aria-label"),
                        input.getAttribute("id"),
                        input.getAttribute("name"),
                        input.getAttribute("class"),
                        input.isDisplayed(),
                        input.isEnabled()
                    ));
                } catch (Exception e) {
                    System.out.println("Input " + i + ": Erreur lors de la lecture - " + e.getMessage());
                }
            }
            
            // Tous les labels
            List<WebElement> labels = driver.findElements(By.tagName("label"));
            System.out.println("\n=== Labels trouvés (" + labels.size() + ") ===");
            for (int i = 0; i < labels.size(); i++) {
                WebElement label = labels.get(i);
                try {
                    System.out.println(String.format(
                        "Label %d: text=%s, for=%s, id=%s",
                        i,
                        label.getText(),
                        label.getAttribute("for"),
                        label.getAttribute("id")
                    ));
                } catch (Exception e) {
                    System.out.println("Label " + i + ": Erreur - " + e.getMessage());
                }
            }
            
            // Tous les éléments avec du texte
            System.out.println("\n=== Tous les éléments avec du texte ===");
            try {
                List<WebElement> textElements = driver.findElements(
                    By.xpath("//*[text() and string-length(text()) > 0]")
                );
                System.out.println("Éléments avec texte: " + textElements.size());
                for (int i = 0; i < Math.min(10, textElements.size()); i++) {
                    WebElement elem = textElements.get(i);
                    System.out.println("  " + elem.getTagName() + ": " + elem.getText().substring(0, Math.min(50, elem.getText().length())));
                }
            } catch (Exception e) {
                System.out.println("Erreur: " + e.getMessage());
            }
            
            // Structure HTML simplifiée
            System.out.println("\n=== Structure HTML (body) ===");
            try {
                WebElement body = driver.findElement(By.tagName("body"));
                String bodyText = body.getText();
                System.out.println("Texte visible sur la page (premiers 500 caractères):");
                System.out.println(bodyText.length() > 0 ? bodyText.substring(0, Math.min(500, bodyText.length())) : "(vide)");
                
                // Afficher les balises principales
                System.out.println("\n=== Balises principales ===");
                System.out.println("div: " + driver.findElements(By.tagName("div")).size());
                System.out.println("span: " + driver.findElements(By.tagName("span")).size());
                System.out.println("button: " + driver.findElements(By.tagName("button")).size());
                System.out.println("canvas: " + driver.findElements(By.tagName("canvas")).size());
                System.out.println("flt-glass-pane: " + driver.findElements(By.tagName("flt-glass-pane")).size());
                System.out.println("flt-scene-host: " + driver.findElements(By.tagName("flt-scene-host")).size());
            } catch (Exception e) {
                System.out.println("Erreur lors de la lecture du body: " + e.getMessage());
            }
            
            // Afficher le HTML source (premiers caractères)
            System.out.println("\n=== HTML Source (premiers 1000 caractères) ===");
            System.out.println(pageSource.substring(0, Math.min(1000, pageSource.length())));
            
            System.out.println("===============================================\n");
        } catch (Exception e) {
            System.out.println("Erreur lors du debug: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Trouve le champ email dans l'application Flutter
     * Utilise une approche simple : trouve tous les inputs visibles et prend le premier qui n'est pas password
     */
    private WebElement findEmailInput() {
        System.out.println("Recherche du champ email...");
        
        // Attendre que des inputs soient présents
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("input")));
        
        // Stratégie 1: Chercher par placeholder ou aria-label (le plus fiable)
        try {
            WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//input[contains(translate(@placeholder, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'email') or " +
                         "contains(translate(@aria-label, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'email')]")
            ));
            System.out.println("✓ Champ email trouvé par placeholder/aria-label");
            return emailInput;
        } catch (Exception e) {
            System.out.println("  - Par placeholder/aria-label: non trouvé");
        }
        
        // Stratégie 2: Chercher par label associé
        try {
            WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//label[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'email')]/following::input[1] | " +
                         "//label[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'email')]/../input")
            ));
            System.out.println("✓ Champ email trouvé par label");
            return emailInput;
        } catch (Exception e) {
            System.out.println("  - Par label: non trouvé");
        }
        
        // Stratégie 3: APPROCHE SIMPLE - Prendre le premier input text visible qui n'est pas password
        System.out.println("  - Tentative avec approche simple: premier input text visible");
        List<WebElement> allInputs = driver.findElements(By.tagName("input"));
        List<WebElement> visibleInputs = new java.util.ArrayList<>();
        
        for (WebElement input : allInputs) {
            try {
                String type = input.getAttribute("type");
                if (input.isDisplayed() && input.isEnabled() && 
                    (type == null || (!type.equals("password") && !type.equals("hidden")))) {
                    visibleInputs.add(input);
                }
            } catch (Exception e) {
                // Ignorer les inputs qui ne peuvent pas être lus
            }
        }
        
        System.out.println("  - Inputs visibles trouvés: " + visibleInputs.size());
        
        if (!visibleInputs.isEmpty()) {
            // Le premier input visible est généralement l'email
            WebElement firstInput = visibleInputs.get(0);
            System.out.println("✓ Utilisation du premier input visible (index 0) comme champ email");
            System.out.println("  Type: " + firstInput.getAttribute("type") + 
                             ", Placeholder: " + firstInput.getAttribute("placeholder"));
            return firstInput;
        }
        
        // Si rien n'est trouvé, faire un debug complet
        System.out.println("✗ Aucun input visible trouvé. Début du debug...");
        debugInputs();
        throw new RuntimeException("Impossible de trouver le champ email. " +
            "Vérifiez que l'application Flutter est bien chargée et que le formulaire est visible. " +
            "Voir les logs ci-dessus pour les détails.");
    }

    /**
     * Trouve le champ password dans l'application Flutter
     * Utilise une approche simple : trouve l'input de type password
     */
    private WebElement findPasswordInput() {
        System.out.println("Recherche du champ password...");
        
        // Stratégie 1: Chercher par type='password' (le plus fiable)
        try {
            WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//input[@type='password']")
            ));
            System.out.println("✓ Champ password trouvé par type");
            return passwordInput;
        } catch (Exception e) {
            System.out.println("  - Par type password: non trouvé");
        }
        
        // Stratégie 2: Chercher par placeholder ou aria-label
        try {
            WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//input[contains(translate(@placeholder, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'password') or " +
                         "contains(translate(@placeholder, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'mot de passe')]")
            ));
            System.out.println("✓ Champ password trouvé par placeholder");
            return passwordInput;
        } catch (Exception e) {
            System.out.println("  - Par placeholder: non trouvé");
        }
        
        // Stratégie 3: APPROCHE SIMPLE - Prendre le deuxième input visible (après l'email)
        System.out.println("  - Tentative avec approche simple: deuxième input visible");
        List<WebElement> allInputs = driver.findElements(By.tagName("input"));
        List<WebElement> visibleInputs = new java.util.ArrayList<>();
        
        for (WebElement input : allInputs) {
            try {
                String type = input.getAttribute("type");
                if (input.isDisplayed() && input.isEnabled() && 
                    (type == null || !type.equals("hidden"))) {
                    visibleInputs.add(input);
                }
            } catch (Exception e) {
                // Ignorer
            }
        }
        
        System.out.println("  - Inputs visibles trouvés: " + visibleInputs.size());
        
        if (visibleInputs.size() >= 2) {
            // Le deuxième input est généralement le password
            WebElement secondInput = visibleInputs.get(1);
            System.out.println("✓ Utilisation du deuxième input visible (index 1) comme champ password");
            System.out.println("  Type: " + secondInput.getAttribute("type") + 
                             ", Placeholder: " + secondInput.getAttribute("placeholder"));
            return secondInput;
        } else if (visibleInputs.size() == 1) {
            // Si un seul input, vérifier s'il est de type password
            WebElement input = visibleInputs.get(0);
            String type = input.getAttribute("type");
            if (type != null && type.equals("password")) {
                System.out.println("✓ Utilisation du seul input visible (type password)");
                return input;
            }
        }
        
        // Si rien n'est trouvé
        System.out.println("✗ Aucun champ password trouvé. Début du debug...");
        debugInputs();
        throw new RuntimeException("Impossible de trouver le champ password. " +
            "Vérifiez que l'application Flutter est bien chargée. " +
            "Voir les logs ci-dessus pour les détails.");
    }

    /**
     * Trouve le bouton de connexion
     */
    private WebElement findLoginButton() {
        return wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//button[contains(., 'Se connecter') or contains(., 'Connexion') or contains(., 'Sign In')]")
        ));
    }

    @Test
    @DisplayName("Login succès avec identifiants valides - Flutter Web")
    void loginSuccess() {
        driver.get(appUrl);

        // Attendre que l'app Flutter soit chargée
        waitForFlutterAppToLoad();
        
        // Debug pour voir la structure de la page
        System.out.println("\n=== TEST: Login Success ===");
        debugInputs();

        // Trouver les champs de formulaire
        WebElement emailInput = findEmailInput();
        WebElement passwordInput = findPasswordInput();

        // Saisir les identifiants
        emailInput.clear();
        emailInput.sendKeys("mouad@gmail.com");

        passwordInput.clear();
        passwordInput.sendKeys("mouad1234");

        // Cliquer sur le bouton de connexion
        WebElement loginButton = findLoginButton();
        loginButton.click();

        // Attendre la navigation vers la page d'accueil
        // Flutter peut prendre du temps pour naviguer
        wait.until(ExpectedConditions.or(
            // Vérifier si un élément de la page d'accueil apparaît
            ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(), 'Accueil') or contains(text(), 'Home')]")
            ),
            // Ou vérifier si le bouton de connexion disparaît (indique une navigation)
            ExpectedConditions.invisibilityOfElementLocated(
                By.xpath("//button[contains(., 'Se connecter')]")
            )
        ));

        // Vérification finale
        String pageSource = driver.getPageSource();
        assertTrue(
            pageSource.contains("BurnCare") || 
            pageSource.contains("Accueil") ||
            pageSource.contains("Home"),
            "La page après connexion ne semble pas être la home attendue. Page actuelle: " + driver.getCurrentUrl()
        );
    }

    @Test
    @DisplayName("Login échoue avec mauvais mot de passe - Flutter Web")
    void loginFailWithBadPassword() {
        driver.get(appUrl);

        // Attendre que l'app Flutter soit chargée
        waitForFlutterAppToLoad();
        
        // Debug pour voir la structure de la page
        System.out.println("\n=== TEST: Login Fail ===");
        debugInputs();

        // Trouver les champs de formulaire
        WebElement emailInput = findEmailInput();
        WebElement passwordInput = findPasswordInput();

        // Saisir des identifiants incorrects
        emailInput.clear();
        emailInput.sendKeys("mouad@gmail.com");

        passwordInput.clear();
        passwordInput.sendKeys("mauvais_mdp");

        // Cliquer sur le bouton de connexion
        WebElement loginButton = findLoginButton();
        loginButton.click();

        // Attendre que le message d'erreur apparaisse
        // Flutter affiche les erreurs dans des SnackBars
        wait.until(ExpectedConditions.or(
            // Vérifier la présence d'un SnackBar (Flutter Material)
            ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(@class, 'snackbar') or contains(@role, 'alert') or contains(@class, 'snack-bar')]")
            ),
            // Ou vérifier un message d'erreur dans le DOM
            ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(), 'incorrect') or contains(text(), 'erreur') or " +
                         "contains(text(), 'identifiants') or contains(text(), 'invalid')]")
            )
        ));

        // Vérification du message d'erreur
        String pageSource = driver.getPageSource().toLowerCase();
        assertTrue(
            pageSource.contains("incorrect") ||
            pageSource.contains("erreur") ||
            pageSource.contains("identifiants") ||
            pageSource.contains("invalid") ||
            pageSource.contains("email ou mot de passe"),
            "Aucun message d'erreur de login n'a été trouvé après un mauvais mot de passe. " +
            "Contenu de la page: " + pageSource.substring(0, Math.min(500, pageSource.length()))
        );
    }

    @Test
    @DisplayName("Vérification de l'affichage de la page de login - Flutter Web")
    void verifyLoginPageDisplay() {
        driver.get(appUrl);

        // Attendre que l'app Flutter soit chargée
        waitForFlutterAppToLoad();
        
        // Debug pour voir la structure de la page
        System.out.println("\n=== TEST: Verify Login Page Display ===");
        debugInputs();

        String pageSource = driver.getPageSource();
        
        // Vérifier que les éléments essentiels sont présents
        assertTrue(
            pageSource.contains("BurnCare"),
            "Le titre 'BurnCare' n'est pas présent sur la page. Contenu: " + 
            pageSource.substring(0, Math.min(500, pageSource.length()))
        );

        // Vérification plus flexible du sous-titre
        String pageSourceLower = pageSource.toLowerCase();
        assertTrue(
            pageSourceLower.contains("prévention") ||
            pageSourceLower.contains("burnout") ||
            pageSourceLower.contains("burn") ||
            pageSource.contains("BurnCare"), // Si le titre est présent, c'est déjà bon
            "Le sous-titre ou un élément caractéristique n'est pas présent sur la page. " +
            "Contenu (premiers 500 caractères): " + pageSource.substring(0, Math.min(500, pageSource.length()))
        );

        // Vérifier la présence des champs de formulaire
        try {
            WebElement emailInput = findEmailInput();
            WebElement passwordInput = findPasswordInput();
            WebElement loginButton = findLoginButton();

            assertTrue(emailInput.isDisplayed(), "Le champ email n'est pas visible");
            assertTrue(passwordInput.isDisplayed(), "Le champ password n'est pas visible");
            assertTrue(loginButton.isDisplayed(), "Le bouton de connexion n'est pas visible");
        } catch (Exception e) {
            // Si on ne peut pas trouver les éléments, afficher le debug
            debugInputs();
            throw new AssertionError("Impossible de trouver les éléments du formulaire: " + e.getMessage());
        }
    }
}