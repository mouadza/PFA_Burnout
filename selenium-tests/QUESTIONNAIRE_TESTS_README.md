# Tests Selenium pour le Questionnaire de Burnout

Ce document décrit les tests Selenium pour le questionnaire de burnout (SQ-01 à SQ-05).

## Prérequis

1. **Java 17+** installé
2. **Maven** installé
3. **Chrome** installé (pour le WebDriver)
4. **Application Angular** démarrée sur `http://localhost:4200` (par défaut)
5. **Backend Spring Boot** démarré sur `http://localhost:8080`
6. **Backend FastAPI** démarré (pour l'analyse du questionnaire)

## Configuration

### URL de l'application

Par défaut, les tests pointent vers `http://localhost:4200` (application Angular).

Pour changer l'URL, utilisez la propriété système `app.url` :

```bash
mvn test -Dapp.url=http://localhost:4200
```

### Identifiants de test

Les tests utilisent un utilisateur de test défini dans la classe `QuestionnaireTest.java` :

```java
private static final String TEST_EMAIL = "mouad@gmail.com";
private static final String TEST_PASSWORD = "mouad1234";
```

**Important** : Assurez-vous que cet utilisateur existe dans votre base de données et qu'il est **approuvé** (`enabled: true`).

## Structure des tests

### SQ-01 : Accès au questionnaire
- **Précondition** : Utilisateur connecté et approuvé
- **Action** : Accéder à la page Questionnaire depuis l'interface utilisateur
- **Vérification** : La page du questionnaire s'affiche avec les questions et boutons de réponse

### SQ-02 : Soumission du questionnaire incomplet
- **Précondition** : Questionnaire ouvert
- **Action** : Ne pas répondre à toutes les questions puis cliquer sur "Soumettre"
- **Vérification** : Soumission bloquée avec message d'erreur indiquant que toutes les questions doivent être complétées

### SQ-03 : Soumission du questionnaire avec réponses valides
- **Précondition** : Questionnaire ouvert
- **Action** : Répondre à toutes les questions puis cliquer sur "Soumettre"
- **Vérification** : Soumission réussie et redirection vers la page de résultat

### SQ-04 : Affichage du score de burnout
- **Précondition** : Questionnaire soumis
- **Action** : Consulter la page de résultat
- **Vérification** : Le score de burnout est affiché (nombre entre 0 et 100)

### SQ-05 : Empêchement de la double soumission
- **Précondition** : Questionnaire soumis une première fois
- **Action** : Cliquer plusieurs fois sur le bouton "Soumettre"
- **Vérification** : Une seule soumission est prise en compte (bouton désactivé ou un seul résultat)

## Exécution des tests

### Exécuter tous les tests du questionnaire

```bash
cd selenium-tests
mvn test -Dtest=QuestionnaireTest
```

### Exécuter un test spécifique

```bash
mvn test -Dtest=QuestionnaireTest#SQ_01_accessQuestionnaire
```

### Exécuter avec un autre navigateur

Pour changer l'URL et exécuter un test spécifique :

```bash
mvn test -Dtest=QuestionnaireTest#SQ_03_submitCompleteQuestionnaire -Dapp.url=http://localhost:4200
```

### Exécuter avec logs détaillés

```bash
mvn test -Dtest=QuestionnaireTest -X
```

## Artéfacts de test

En cas d'échec, les tests sauvegardent automatiquement :

- **Screenshots** : `target/selenium-artifacts/SQ_XX_fail.png`
- **HTML de la page** : `target/selenium-artifacts/SQ_XX_fail.html`

Ces fichiers permettent de diagnostiquer les problèmes.

## Structure du questionnaire

Le questionnaire contient **12 questions** avec une échelle Likert à **5 points** :

- Jamais (0)
- Rarement (1)
- Parfois (2)
- Souvent (3)
- Toujours (4)

## Sélecteurs utilisés

Les tests utilisent les sélecteurs suivants pour identifier les éléments Angular :

### Page de questionnaire
- Questions : `h2`, `.question-card h2`
- Boutons de réponse : `.option-btn`
- Bouton Suivant/Envoyer : `button` avec texte "Suivant" ou "Envoyer"
- Barre de progression : `.progress-bar`, `.progress`

### Page de résultat
- Carte de résultat : `.result-card`
- Score : `.score-text`
- Titre de risque : `.risk-title`

## Dépannage

### Les tests ne trouvent pas les éléments

1. Vérifiez que l'application Angular est bien démarrée
2. Vérifiez que l'utilisateur de test est bien approuvé
3. Augmentez les timeouts dans la classe de test si nécessaire
4. Consultez les artéfacts de test (`target/selenium-artifacts/`)

### Erreur de connexion

1. Vérifiez que le backend Spring Boot est démarré
2. Vérifiez que les identifiants de test sont corrects
3. Vérifiez que l'utilisateur existe dans la base de données

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

1. **Authentification** : Les tests se connectent automatiquement avant chaque test du questionnaire
2. **Isolation** : Chaque test est indépendant et se reconnecte
3. **Navigateur** : Les tests utilisent Chrome par défaut (configurable via `ChromeOptions`)
4. **Timeouts** : Les timeouts sont configurés pour Angular (45 secondes par défaut)

## Prochaines étapes

- Ajouter des tests pour différents types de réponses
- Ajouter des tests pour la navigation entre les questions
- Ajouter des tests pour la barre de progression
- Intégrer les tests dans un pipeline CI/CD

