import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class ApiClient {
  final String baseUrl;
  // Stockage pour récupérer le token automatiquement
  final _storage = const FlutterSecureStorage();

  ApiClient({required this.baseUrl});

  // 1️⃣ Méthode POST (Login, Register)
  Future<dynamic> post(String path, Map<String, dynamic> body) async {
    final url = Uri.parse('$baseUrl$path');
    final token = await _storage.read(key: 'token');

    // ✅ FIX 401 : Pas de token pour l'auth
    final isAuthEndpoint = path.contains('/auth/login') || path.contains('/auth/register');

    final response = await http.post(
      url,
      headers: {
        'Content-Type': 'application/json',
        if (token != null && !isAuthEndpoint) 'Authorization': 'Bearer $token',
      },
      body: jsonEncode(body),
    );

    return _handleResponse(response);
  }

  // 2️⃣ Méthode PUT (Update Profil & Password)
  Future<dynamic> put(String path, Map<String, dynamic> body) async {
    final url = Uri.parse('$baseUrl$path');
    final token = await _storage.read(key: 'token');

    final response = await http.put(
      url,
      headers: {
        'Content-Type': 'application/json',
        if (token != null) 'Authorization': 'Bearer $token',
      },
      body: jsonEncode(body),
    );

    return _handleResponse(response);
  }

  // 3️⃣ Méthode GET (Récupérer infos, listes)
  Future<dynamic> get(String path) async {
    final url = Uri.parse('$baseUrl$path');
    final token = await _storage.read(key: 'token');

    final response = await http.get(
      url,
      headers: {
        'Content-Type': 'application/json',
        if (token != null) 'Authorization': 'Bearer $token',
      },
    );

    return _handleResponse(response);
  }

  // 4️⃣ Méthode DELETE (Pour l'Admin Dashboard)
  Future<dynamic> delete(String path) async {
    final url = Uri.parse('$baseUrl$path');
    final token = await _storage.read(key: 'token');

    final response = await http.delete(
      url,
      headers: {
        'Content-Type': 'application/json',
        if (token != null) 'Authorization': 'Bearer $token',
      },
    );

    if (response.statusCode == 204 || response.statusCode == 200) {
      return null;
    } else {
      return _handleResponse(response);
    }
  }

  // 🔄 Gestion centralisée des réponses CORRIGÉE
  dynamic _handleResponse(http.Response response) {
    // Debug log pour voir exactement ce que le serveur envoie
    if (response.statusCode >= 400) {
      print("⚠️ API ERROR ${response.statusCode} | Body Length: ${response.body.length} | Content: '${response.body}'");
    }

    if (response.statusCode >= 200 && response.statusCode < 300) {
      if (response.body.isEmpty) return {};
      return jsonDecode(utf8.decode(response.bodyBytes));
    } else {
      String errorMessage;
      try {
        // Tente de décoder le JSON d'erreur (ex: {"message": "User is disabled", ...})
        final Map<String, dynamic> body = jsonDecode(utf8.decode(response.bodyBytes));

        // Spring boot met souvent le message dans 'message' ou 'error'
        errorMessage = body['message'] ?? body['error'] ?? "Erreur ${response.statusCode}";
      } catch (e) {
        // Si le serveur n'a pas renvoyé de JSON ou si le body est vide
        if (response.statusCode == 401 && response.body.isEmpty) {
          // On force un message "propre" pour éviter "Erreur serveur"
          // NOTE: Sans le fix Backend (Java), on ne peut pas savoir si c'est "Bloqué" ici.
          errorMessage = "Identifiants incorrects";
        } else {
          errorMessage = response.body.isNotEmpty
              ? response.body
              : "Erreur serveur (${response.statusCode})";
        }
      }

      // On propage le message
      throw Exception(errorMessage);
    }
  }
}