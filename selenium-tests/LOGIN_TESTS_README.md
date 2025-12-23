# Tests Selenium pour la Connexion (Login)

Ce document décrit les tests Selenium pour la fonctionnalité de connexion (SL-01, SL-02, SL-03, SL-05).

## Prérequis

1. **Java 17+** installé
2. **Maven** installé
3. **Chrome** installé (pour le WebDriver)
4. **Application Angular** démarrée sur `http://localhost:4200` (par défaut)
5. **Backend Spring Boot** démarré sur `http://localhost:8080`

## Configuration

### URL de l'application

Par défaut, les tests pointent vers `http://localhost:4200` (application Angular).

Pour changer l'URL, utilisez la propriété système `app.url` :

```bash
mvn test -Dapp.url=http://localhost:4200
```

### Identifiants de test

Les tests utilisent des utilisateurs de test définis dans la classe `LoginTest.java` :

```java
private static final String USER_EMAIL = "mouad@gmail.com";
private static final String USER_PASSWORD = "mouad1234";
private static final String ADMIN_EMAIL = "admin@admin.com";
private static final String ADMIN_PASSWORD = "admin1234";
private static final String UNAPPROVED_USER_EMAIL = "simo@gmail.com";
private static final String UNAPPROVED_USER_PASSWORD = "simo1234";
```

**Important** : Assurez-vous que ces utilisateurs existent dans votre base de données avec les propriétés suivantes :

- **Utilisateur normal** (`USER_EMAIL`) : Doit être approuvé (`enabled: true`)
- **Administrateur** (`ADMIN_EMAIL`) : Doit être approuvé et avoir `role: 'ADMIN'` ou `profession: 'ADMIN'`
- **Utilisateur non approuvé** (`UNAPPROVED_USER_EMAIL`) : Doit avoir `enabled: false`

## Structure des tests

### SL-01 : Connexion utilisateur avec identifiants valides
- **Précondition** : Compte utilisateur approuvé, page Login chargée
- **Action** : Saisir un email utilisateur valide et un mot de passe correct puis cliquer sur "Se connecter"
- **Vérification** : 
  - Connexion réussie et redirection vers `/user-home`
  - Vérification de la présence d'éléments spécifiques à l'utilisateur (questionnaire, fatigue, etc.)

### SL-02 : Connexion administrateur avec identifiants valides
- **Précondition** : Compte administrateur actif, page Login chargée
- **Action** : Saisir un email administrateur valide et un mot de passe correct puis cliquer sur "Se connecter"
- **Vérification** : 
  - Connexion réussie et redirection vers `/admin-home`
  - Vérification de la présence d'éléments spécifiques à l'admin (gestion utilisateurs, statistiques, etc.)

### SL-03 : Connexion avec identifiants invalides
- **Précondition** : Page Login chargée
- **Action** : Saisir un email valide et un mot de passe incorrect puis cliquer sur "Se connecter"
- **Vérification** : 
  - Message d'erreur affiché indiquant des identifiants incorrects
  - L'utilisateur reste sur la page de login

### SL-05 : Connexion utilisateur non approuvé
- **Précondition** : Compte utilisateur non approuvé, page Login chargée
- **Action** : Saisir les identifiants d'un utilisateur non approuvé puis cliquer sur "Se connecter"
- **Vérification** : 
  - Accès refusé avec message indiquant que le compte n'est pas encore approuvé
  - L'utilisateur reste sur la page de login

## Exécution des tests

### Exécuter tous les tests de connexion

```bash
cd selenium-tests
mvn test -Dtest=LoginTest
```

### Exécuter un test spécifique

```bash
mvn test -Dtest=LoginTest#SL_01_userLoginSuccess
mvn test -Dtest=LoginTest#SL_02_adminLoginSuccess
mvn test -Dtest=LoginTest#SL_03_loginInvalidCredentials
mvn test -Dtest=LoginTest#SL_05_userNotApproved
```

### Exécuter avec un autre URL

```bash
mvn test -Dtest=LoginTest -Dapp.url=http://localhost:4200
```

### Exécuter avec logs détaillés

```bash
mvn test -Dtest=LoginTest -X
```

## Artéfacts de test

En cas d'échec, les tests sauvegardent automatiquement :

- **Screenshots** : `target/selenium-artifacts/SL_XX_fail.png`
- **HTML de la page** : `target/selenium-artifacts/SL_XX_fail.html`

Ces fichiers permettent de diagnostiquer les problèmes.

## Améliorations apportées

### Problème de clic résolu

Le problème de clic a été résolu en :

1. **Utilisation de JavaScript click** : Plus fiable pour Angular
   ```java
   ((JavascriptExecutor) driver).executeScript("arguments[0].click();", loginButton);
   ```

2. **Meilleurs sélecteurs** : Utilisation de sélecteurs CSS spécifiques à Angular
   ```java
   input[formcontrolname='email'], button[type='submit']
   ```

3. **Attente explicite** : Ajout de délais pour garantir que les éléments sont prêts
   ```java
   Thread.sleep(500); // Attendre que le scroll soit terminé
   ```

4. **Méthode de remplissage améliorée** : Utilisation de Actions pour taper dans les champs
   ```java
   new Actions(driver).sendKeys(input, value).perform();
   ```

5. **Vérification de l'état** : Vérification que les éléments sont visibles et activés avant interaction

### Gestion Angular

- `waitForAngular()` : Attend que Angular soit stable avant d'interagir
- `waitDomReady()` : Attend que le DOM soit complètement chargé
- Gestion des redirections Angular : Vérification de l'URL après connexion

## Sélecteurs utilisés

### Champs de formulaire
- Email : `input[type='email'], input[formcontrolname='email']`
- Password : `input[type='password'], input[formcontrolname='password']`
- Bouton Login : `button[type='submit'], button.btn-login`

### Messages d'erreur
- `.error-message, [class*='error']`

### Vérification de redirection
- User Home : URL contient `/user-home`
- Admin Home : URL contient `/admin-home`

## Dépannage

### Les tests ne trouvent pas les éléments

1. Vérifiez que l'application Angular est bien démarrée
2. Vérifiez que les identifiants de test sont corrects
3. Augmentez les timeouts dans la classe de test si nécessaire
4. Consultez les artéfacts de test (`target/selenium-artifacts/`)

### Le clic ne fonctionne pas

1. Le test utilise maintenant JavaScript click qui est plus fiable
2. Vérifiez que Chrome est à jour
3. Vérifiez que les éléments ne sont pas masqués par d'autres éléments (z-index)

### Erreur de connexion

1. Vérifiez que le backend Spring Boot est démarré
2. Vérifiez que les identifiants de test sont corrects
3. Vérifiez que les utilisateurs existent dans la base de données avec les bonnes propriétés

### Les tests sont trop lents

Les tests incluent des délais pour garantir la stabilité avec Angular. Si vous voulez accélérer :

1. Réduisez les délais `Thread.sleep()` dans le code
2. Notez que cela peut rendre les tests moins fiables

### ChromeDriver non trouvé

WebDriverManager devrait télécharger automatiquement ChromeDriver. Si ce n'est pas le cas :

```bash
mvn clean test
```

## Notes importantes

1. **Isolation** : Chaque test est indépendant
2. **Navigateur** : Les tests utilisent Chrome par défaut
3. **Timeouts** : Les timeouts sont configurés pour Angular (45 secondes par défaut)
4. **Angular** : Les tests gèrent spécifiquement Angular (attente de stabilité, sélecteurs appropriés)

## Prochaines étapes

- Ajouter des tests pour d'autres scénarios (mot de passe oublié, etc.)
- Ajouter des tests de performance
- Intégrer les tests dans un pipeline CI/CD

