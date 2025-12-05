import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../models/auth_response.dart';
import '../services/auth_service.dart';

class AuthProvider extends ChangeNotifier {
  final AuthService authService;
  final _storage = const FlutterSecureStorage();

  AuthResponse? _currentUser;

  AuthResponse? get user => _currentUser;
  bool get isAuthenticated => _currentUser != null;

  AuthProvider(this.authService);

  Future<bool> login(String email, String password) async {
    try {
      final response = await authService.login(email, password);
      _currentUser = response;
      await _storage.write(key: 'token', value: response.token);
      notifyListeners();
      return true;
    } catch (e) {
      print("Erreur Login Provider: $e");
      return false;
    }
  }

  // ✅ CORRECTION : On utilise firstName et lastName au lieu de fullName
  Future<bool> register(String firstName, String lastName, String email, String password, String profession) async {
    try {
      await authService.register(
        firstName: firstName,
        lastName: lastName,
        email: email,
        password: password,
        profession: profession,
      );
      return true;
    } catch (e) {
      print("Erreur Register Provider: $e");
      return false;
    }
  }

  Future<void> logout() async {
    _currentUser = null;
    await _storage.delete(key: 'token');
    notifyListeners();
  }
}