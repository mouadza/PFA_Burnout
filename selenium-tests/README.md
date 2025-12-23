# Tests Selenium pour BurnCare Flutter Web

## Prérequis

1. **Java 17+** installé
2. **Maven** installé
3. **Chrome** installé
4. **Flutter** installé et configuré

## Instructions pour lancer les tests

### ⚠️ IMPORTANT : Mode de rendu Flutter

**✅ Configuration automatique** : Le fichier `web/index.html` a été configuré pour utiliser automatiquement le mode HTML. Vous n'avez plus besoin de passer le flag `--web-renderer html`.

### Étape 1 : Lancer l'application Flutter Web

Ouvrez un terminal et exécutez :

```bash
cd burncare_front
flutter run -d chrome --web-port=5000
```

**Important** : 
- L'application utilisera automatiquement le mode HTML (configuré dans `web/index.html`)
- Attendez que l'application soit complètement chargée dans le navigateur avant de passer à l'étape 2
- Vérifiez que vous voyez bien les champs de formulaire dans le navigateur

**Note** : Si vous voyez toujours "Inputs trouvés (0)" dans les logs, vérifiez que le fichier `web/index.html` contient bien la configuration `window.flutterConfiguration = { renderer: "html" };`

### Étape 2 : Lancer les tests Selenium

Ouvrez un **nouveau terminal** (gardez le premier ouvert avec Flutter) et exécutez :

#### Pour Windows PowerShell :
```powershell
cd selenium-tests
mvn test "-Dapp.url=http://localhost:5000"
```

**Note** : Les guillemets sont importants dans PowerShell pour éviter les problèmes de parsing.

#### Pour Windows CMD :
```cmd
cd selenium-tests
mvn test -Dapp.url=http://localhost:5000
```

#### Pour Linux/Mac :
```bash
cd selenium-tests
mvn test -Dapp.url=http://localhost:5000
```

## Résolution des problèmes

### Erreur : Aucun input trouvé (Inputs trouvés: 0)

**Cause** : Flutter utilise CanvasKit au lieu du mode HTML.

**Solution** : 
1. Arrêtez l'application Flutter
2. Relancez avec le flag `--web-renderer html` :
   ```bash
   flutter run -d chrome --web-port=5000 --web-renderer html
   ```
3. Vérifiez dans le navigateur que les champs de formulaire sont visibles
4. Relancez les tests Selenium

Voir `FLUTTER_WEB_SETUP.md` pour plus de détails.

### Erreur : `ERR_CONNECTION_REFUSED`

**Cause** : L'application Flutter n'est pas lancée ou n'est pas accessible sur le port 5000.

**Solution** :
1. Vérifiez que l'application Flutter est bien lancée avec `flutter run -d chrome --web-port=5000`
2. Vérifiez que l'application est accessible dans votre navigateur sur `http://localhost:5000`
3. Si vous utilisez un autre port, modifiez l'URL dans la commande Maven

### Erreur : `TimeoutException` - Éléments non trouvés

**Cause** : L'application Flutter n'est pas complètement chargée ou les sélecteurs ne correspondent pas.

**Solution** :
1. Attendez que l'application soit complètement chargée (vous devriez voir la page de login)
2. Vérifiez que le texte "BurnCare" est visible sur la page
3. Augmentez le timeout dans `LoginTest.java` si nécessaire (ligne 48)

### Erreur Maven : `Plugin not found`

**Cause** : Problème de parsing de la commande dans PowerShell.

**Solution** : Utilisez des guillemets autour du paramètre :
```powershell
mvn test "-Dapp.url=http://localhost:5000"
```

### Avertissement CDP

L'avertissement concernant CDP n'est pas critique. Si vous voulez le corriger, ajoutez la dépendance correspondante dans `pom.xml` :

```xml
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-devtools-v143</artifactId>
    <version>4.25.0</version>
</dependency>
```

(Remplacez `v143` par la version correspondant à votre Chrome)

## Tests disponibles

1. **loginSuccess** : Test de connexion avec des identifiants valides
2. **loginFailWithBadPassword** : Test de connexion avec un mauvais mot de passe
3. **verifyLoginPageDisplay** : Vérification de l'affichage de la page de login

## Configuration

### Changer l'URL de l'application

Modifiez la valeur par défaut dans `LoginTest.java` ligne 30 :
```java
appUrl = System.getProperty("app.url", "http://localhost:5000");
```

Ou passez l'URL en paramètre :
```bash
mvn test "-Dapp.url=http://localhost:8080"
```

### Changer les identifiants de test

Modifiez les valeurs dans les méthodes de test :
- Email : `mouad@gmail.com`
- Mot de passe : `mouad1234`

## Structure du projet

```
selenium-tests/
├── src/
│   └── test/
│       └── java/
│           └── com/
│               └── burncare/
│                   └── selenium/
│                       └── LoginTest.java
├── pom.xml
└── README.md
```

