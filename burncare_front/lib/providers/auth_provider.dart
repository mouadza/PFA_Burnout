import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../models/auth_response.dart';
import '../services/auth_service.dart';

class AuthProvider extends ChangeNotifier {
  final AuthService authService;
  final _storage = const FlutterSecureStorage();

  AuthResponse? _currentUser;

  AuthResponse? get currentUser => _currentUser;
  bool get isLoggedIn => _currentUser != null;

  AuthProvider({required this.authService});

  Future<void> login(String email, String password) async {
    final response = await authService.login(email, password);
    _currentUser = response;
    await _storage.write(key: 'token', value: response.token);
    notifyListeners();
  }

  Future<void> register(
      String fullName, String email, String password, String profession) async {
    final response = await authService.register(
      fullName: fullName,
      email: email,
      password: password,
      profession: profession,
    );
    _currentUser = response;
    await _storage.write(key: 'token', value: response.token);
    notifyListeners();
  }

  Future<void> logout() async {
    _currentUser = null;
    await _storage.delete(key: 'token');
    notifyListeners();
  }
}
