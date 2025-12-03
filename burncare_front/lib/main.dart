import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'services/api_client.dart';
import 'services/auth_service.dart';
import 'providers/auth_provider.dart';
import 'burncare_app.dart';


void main() {
  final apiClient = ApiClient(baseUrl: 'http://10.0.2.2:8080');
  final authService = AuthService(apiClient);

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(
          create: (_) => AuthProvider(authService: authService),
        ),
      ],
      child: const BurnCareApp(),
    ),
  );
}
