import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';

// Imports de vos classes
import 'package:burncare_front/burncare_app.dart'; // ✅ Import de votre vraie classe principale
import 'package:burncare_front/services/api_client.dart';
import 'package:burncare_front/services/auth_service.dart';
import 'package:burncare_front/providers/auth_provider.dart';

void main() {
  testWidgets('Lancement de l\'application BurnCare', (WidgetTester tester) async {
    // 1. Initialisation des dépendances nécessaires (comme dans le main.dart)
    // On met une URL fictive car le test ne fait pas de vrais appels réseau
    final apiClient = ApiClient(baseUrl: 'http://test-url');
    final authService = AuthService(apiClient);

    // 2. Construction de l'interface avec le Provider
    await tester.pumpWidget(
      MultiProvider(
        providers: [
          ChangeNotifierProvider(
            create: (_) => AuthProvider(authService),
          ),
        ],
        // ✅ On utilise BurnCareApp au lieu de MyApp
        child: const BurnCareApp(),
      ),
    );

    // 3. Vérification simple
    // On vérifie simplement que l'application a affiché un MaterialApp (donc qu'elle a démarré)
    expect(find.byType(MaterialApp), findsOneWidget);
  });
}