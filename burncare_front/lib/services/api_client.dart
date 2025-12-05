import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class ApiClient {
  final String baseUrl;
  final _storage = const FlutterSecureStorage();

  ApiClient({required this.baseUrl});

  // Méthode POST (Login, Register)
  Future<dynamic> post(String path, Map<String, dynamic> body) async {
    final url = Uri.parse('$baseUrl$path');
    final token = await _storage.read(key: 'token');

    // On n'envoie PAS le token pour login/register pour éviter les conflits 401
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

  // Méthode PUT (Mise à jour Profil & Password)
  Future<dynamic> put(String path, Map<String, dynamic> body) async {
    final url = Uri.parse('$baseUrl$path');
    final token = await _storage.read(key: 'token');

    final response = await http.put(
      url,
      headers: {
        'Content-Type': 'application/json',
        // Token OBLIGATOIRE pour les modifications
        if (token != null) 'Authorization': 'Bearer $token',
      },
      body: jsonEncode(body),
    );

    return _handleResponse(response);
  }

  // Méthode GET
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

  // Gestion des réponses
  dynamic _handleResponse(http.Response response) {
    if (response.statusCode >= 200 && response.statusCode < 300) {
      if (response.body.isEmpty) return {};
      return jsonDecode(utf8.decode(response.bodyBytes));
    } else if (response.statusCode == 401) {
      // Cas spécifique : Token expiré
      throw Exception('Session expirée (401). Veuillez vous reconnecter.');
    } else {
      print("⚠️ API ERROR ${response.statusCode} : ${response.body}");
      throw Exception('Erreur API (${response.statusCode}): ${response.body}');
    }
  }
}