import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'services/api_client.dart';
import 'services/auth_service.dart';
import 'providers/auth_provider.dart';
import 'burncare_app.dart';
import 'utils/api_config.dart';

void main() {
  // Utilise une URL différente selon la plateforme (web / Android / desktop)
  final apiClient = ApiClient(baseUrl: getSpringApiBaseUrl());
  final authService = AuthService(apiClient);

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(
          // ✅ CORRECTION : On passe 'authService' directement, sans mettre 'authService:' devant.
          create: (_) => AuthProvider(authService),
        ),
      ],
      child: const BurnCareApp(),
    ),
  );
}