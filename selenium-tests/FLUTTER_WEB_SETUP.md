# Configuration Flutter Web pour Selenium

## Problème

Si Selenium ne trouve aucun input sur la page Flutter, c'est probablement parce que Flutter utilise **CanvasKit** (rendu canvas) au lieu du mode **HTML**.

Avec CanvasKit, les éléments sont rendus dans un canvas et ne sont pas accessibles via Selenium standard.

## Solution : Forcer le mode HTML

### Option 1 : Lancer en mode HTML (recommandé pour les tests)

```bash
cd burncare_front
flutter run -d chrome --web-port=5000 --web-renderer html
```

### Option 2 : Compiler en mode HTML

```bash
cd burncare_front
flutter build web --web-renderer html
```

Puis servez les fichiers avec un serveur web local :

```bash
# Avec Python
cd build/web
python -m http.server 5000

# Ou avec Node.js (http-server)
npx http-server -p 5000
```

### Option 3 : Configurer dans le code Flutter (✅ DÉJÀ FAIT)

La configuration a été ajoutée dans `web/index.html`. L'application utilisera automatiquement le mode HTML.

Si vous devez le faire manuellement, ajoutez dans `web/index.html` avant le script `flutter_bootstrap.js` :

```html
<script>
  window.flutterConfiguration = {
    renderer: "html"
  };
</script>
```

## Vérification

Pour vérifier le mode de rendu utilisé :

1. Ouvrez l'application dans Chrome
2. Ouvrez les DevTools (F12)
3. Regardez dans la console ou les éléments
4. Si vous voyez beaucoup de `<canvas>` et peu d'éléments HTML, c'est CanvasKit
5. Si vous voyez des vrais `<input>`, `<button>`, etc., c'est le mode HTML

## Modes de rendu Flutter Web

- **HTML** : Rendu avec des vrais éléments HTML (compatible Selenium) ✅
- **CanvasKit** : Rendu dans un canvas (non compatible Selenium) ❌
- **Auto** : Flutter choisit automatiquement (peut être CanvasKit)

## Note importante

Le mode HTML est généralement plus lent mais est nécessaire pour les tests automatisés avec Selenium.

