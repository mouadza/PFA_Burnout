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

  // ✅ Mise à jour du profil (avec mise à jour locale immédiate)
  Future<bool> updateProfile(String firstName, String lastName) async {
    if (_currentUser == null) return false;
    try {
      final updatedUser = await authService.updateProfile(
        _currentUser!.email,
        firstName,
        lastName,
      );
      // On garde le token actuel, mais on met à jour les infos
      _currentUser = AuthResponse(
        token: _currentUser!.token,
        firstName: updatedUser.firstName,
        lastName: updatedUser.lastName,
        email: _currentUser!.email,
        role: _currentUser!.role,
        profession: _currentUser!.profession,
      );
      notifyListeners();
      return true;
    } catch (e) {
      print("Erreur Update Profile: $e");
      return false;
    }
  }

  // ✅ Changement de mot de passe
  Future<bool> changePassword(String newPassword) async {
    if (_currentUser == null) return false;
    try {
      await authService.changePassword(_currentUser!.email, newPassword);
      return true;
    } catch (e) {
      print("Erreur Change Password: $e");
      return false;
    }
  }

  Future<void> logout() async {
    _currentUser = null;
    await _storage.delete(key: 'token');
    notifyListeners();
  }
}