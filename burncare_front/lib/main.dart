import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:provider/provider.dart';

import 'services/api_client.dart';
import 'services/auth_service.dart';
import 'providers/auth_provider.dart';
import 'burncare_app.dart';
import 'utils/api_config.dart';

void main() {
  // 2. AJOUTER CES DEUX LIGNES AU DÉBUT
  WidgetsFlutterBinding.ensureInitialized();
  // Force Flutter à générer l'arbre HTML pour que Selenium puisse voir les champs
  RendererBinding.instance.ensureSemantics();

  // Utilise une URL différente selon la plateforme (web / Android / desktop)
  final apiClient = ApiClient(baseUrl: getSpringApiBaseUrl());
  final authService = AuthService(apiClient);

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(
          create: (_) => AuthProvider(authService),
        ),
      ],
      child: const BurnCareApp(),
    ),
  );
}