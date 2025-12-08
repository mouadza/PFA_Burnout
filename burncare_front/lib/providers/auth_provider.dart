import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../models/auth_response.dart';
import '../services/auth_service.dart';

class AuthProvider extends ChangeNotifier {
  final AuthService authService;
  final _storage = const FlutterSecureStorage();

  AuthResponse? _currentUser;

  String? errorMessage;

  AuthResponse? get user => _currentUser;
  bool get isAuthenticated => _currentUser != null;

  AuthProvider(this.authService);

  Future<bool> login(String email, String password) async {
    errorMessage = null;
    notifyListeners();

    try {
      final response = await authService.login(email, password);
      _currentUser = response;
      await _storage.write(key: 'token', value: response.token);
      notifyListeners();
      return true;
    } catch (e) {
      String rawError = e.toString().replaceAll("Exception: ", "");
      String lowerError = rawError.toLowerCase();

      if (lowerError.contains("disabl") ||
          lowerError.contains("lock") ||
          lowerError.contains("bloqu") ||
          lowerError.contains("desactiv") ||
          lowerError.contains("banni") ||
          lowerError.contains("suspend")) {
        errorMessage = "Votre compte est bloqué ou désactivé. Contactez l'administrateur.";
      }
      else if (lowerError.contains("401") ||
          lowerError.contains("bad credentials") ||
          lowerError.contains("session") ||
          lowerError.contains("unauthorized") ||
          lowerError.contains("incorrect")) {
        errorMessage = "Email ou mot de passe incorrect";
      }
      else {
        errorMessage = rawError;
      }

      notifyListeners();
      return false;
    }
  }

  // ✅ MODIFICATION : Ajout du paramètre nommé optionnel {role}
  Future<bool> register(
      String firstName,
      String lastName,
      String email,
      String password,
      String profession,
      {String? role} // <--- Ici
      ) async {
    errorMessage = null;
    try {
      await authService.register(
        firstName: firstName,
        lastName: lastName,
        email: email,
        password: password,
        profession: profession,
        role: role, // <--- Transmission au service
      );
      return true;
    } catch (e) {
      errorMessage = e.toString().replaceAll("Exception: ", "");
      print("Erreur Register Provider: $e");
      notifyListeners();
      return false;
    }
  }

  Future<bool> updateProfile(String firstName, String lastName) async {
    if (_currentUser == null) return false;
    errorMessage = null;

    try {
      final updatedUser = await authService.updateProfile(
        _currentUser!.email,
        firstName,
        lastName,
      );
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
      errorMessage = e.toString().replaceAll("Exception: ", "");
      print("Erreur Update Profile: $e");
      notifyListeners();
      return false;
    }
  }

  Future<bool> changePassword(String newPassword) async {
    if (_currentUser == null) return false;
    errorMessage = null;

    try {
      await authService.changePassword(_currentUser!.email, newPassword);
      return true;
    } catch (e) {
      errorMessage = e.toString().replaceAll("Exception: ", "");
      print("Erreur Change Password: $e");
      notifyListeners();
      return false;
    }
  }

  Future<void> logout() async {
    _currentUser = null;
    errorMessage = null;
    await _storage.delete(key: 'token');
    notifyListeners();
  }
}