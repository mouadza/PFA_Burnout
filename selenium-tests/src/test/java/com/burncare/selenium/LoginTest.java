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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests Selenium pour la page de login BurnCare (Flutter Web).
 *
 * Pré-requis :
 *  - Backend Spring Boot sur http://localhost:8080
 *  - FastAPI sur http://localhost:8000
 *  - Frontend Flutter Web lancé, par ex. :
 *        flutter run -d chrome --web-port 5000
 *  - Un utilisateur de test existe (email/mot de passe ci-dessous).
 *
 * Par défaut, l'URL de l'app est http://localhost:5000
 * Vous pouvez la surcharger :
 *      mvn test -Dapp.url=http://localhost:62482
 */
public class LoginTest {

    private static String appUrl;

    private WebDriver driver;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
        appUrl = System.getProperty("app.url", "http://localhost:5000");
    }

    @BeforeEach
    void setupTest() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @DisplayName("Login succès avec identifiants valides")
    void loginSuccess() throws InterruptedException {
        driver.get(appUrl);

        // Laisser le temps à Flutter de rendre la page
        Thread.sleep(4000);

        WebElement emailInput = driver.findElement(By.cssSelector("input[type='email'], input[name='email']"));
        WebElement passwordInput = driver.findElement(By.cssSelector("input[type='password'], input[name='password']"));

        emailInput.clear();
        emailInput.sendKeys("mouad@gmail.com");  // adapte si besoin

        passwordInput.clear();
        passwordInput.sendKeys("mouad1234");     // adapte si besoin

        WebElement loginButton = driver.findElement(
                By.xpath("//button[contains(., 'Se connecter') or contains(., 'Connexion')]"));
        loginButton.click();

        Thread.sleep(5000);

        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("BurnCare") || pageSource.contains("Accueil"),
                "La page après connexion ne semble pas être la home attendue.");
    }

    @Test
    @DisplayName("Login échoue avec mauvais mot de passe")
    void loginFailWithBadPassword() throws InterruptedException {
        driver.get(appUrl);

        Thread.sleep(4000);

        WebElement emailInput = driver.findElement(By.cssSelector("input[type='email'], input[name='email']"));
        WebElement passwordInput = driver.findElement(By.cssSelector("input[type='password'], input[name='password']"));

        emailInput.clear();
        emailInput.sendKeys("mouad@gmail.com");

        passwordInput.clear();
        passwordInput.sendKeys("mauvais_mdp");

        WebElement loginButton = driver.findElement(
                By.xpath("//button contains(., 'Se connecter') or contains(., 'Connexion')]"));
        loginButton.click();

        Thread.sleep(4000);

        String pageSource = driver.getPageSource();
        assertTrue(
                pageSource.toLowerCase().contains("incorrect")
                        || pageSource.toLowerCase().contains("erreur")
                        || pageSource.toLowerCase().contains("identifiants"),
                "Aucun message d'erreur de login n'a été trouvé après un mauvais mot de passe.");
    }
}


