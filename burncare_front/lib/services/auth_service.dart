import '../models/auth_response.dart';
import 'api_client.dart';

class AuthService {
  final ApiClient apiClient;

  AuthService(this.apiClient);

  Future<AuthResponse> login(String email, String password) async {
    final json = await apiClient.post('/api/auth/login', {
      'email': email,
      'password': password,
    });
    return AuthResponse.fromJson(json);
  }

  Future<AuthResponse> register({
    required String firstName,
    required String lastName,
    required String email,
    required String password,
    required String profession,
  }) async {
    final json = await apiClient.post('/api/auth/register', {
      'firstName': firstName,
      'lastName': lastName,
      'email': email,
      'password': password,
      'profession': profession,
    });
    return AuthResponse.fromJson(json);
  }

  // ✅ CORRECTION : Route vers UserController (/api/user/profile)
  Future<AuthResponse> updateProfile(String email, String firstName, String lastName) async {
    // On change '/api/auth/update-profile' par '/api/user/profile'
    final json = await apiClient.put('/api/user/profile', {
      'email': email,
      'firstName': firstName,
      'lastName': lastName,
    });
    return AuthResponse.fromJson(json);
  }

  // ✅ CORRECTION : Route vers UserController (/api/user/password)
  Future<void> changePassword(String email, String newPassword) async {
    // On change '/api/auth/change-password' par '/api/user/password'
    await apiClient.put('/api/user/password', {
      'email': email,
      'newPassword': newPassword,
    });
  }
}