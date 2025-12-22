import 'dart:io';

import 'package:flutter/foundation.dart';

/// Retourne l'URL de base de l'API en fonction de la plateforme.
/// - Web (Chrome/Edge) : http://localhost:<port>
/// - Émulateur Android : http://10.0.2.2:<port> (accès au localhost de la machine)
/// - iOS / Desktop : http://localhost:<port>
String getApiBaseUrl({int port = 8080}) {
  if (kIsWeb) {
    return 'http://localhost:$port';
  }

  if (Platform.isAndroid) {
    return 'http://10.0.2.2:$port';
  }

  return 'http://localhost:$port';
}

/// Base URL pour le backend FastAPI (port 8000)
String getFastApiBaseUrl() => getApiBaseUrl(port: 8000);

/// Base URL pour le backend Spring Boot (port 8080)
String getSpringApiBaseUrl() => getApiBaseUrl(port: 8080);


