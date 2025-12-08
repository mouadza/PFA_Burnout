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

  // ✅ MODIFICATION : Ajout du paramètre optionnel 'role'
  Future<AuthResponse> register({
    required String firstName,
    required String lastName,
    required String email,
    required String password,
    required String profession,
    String? role, // <--- Nouveau paramètre (peut être null)
  }) async {

    // Construction du corps de la requête
    final Map<String, dynamic> body = {
      'firstName': firstName,
      'lastName': lastName,
      'email': email,
      'password': password,
      'profession': profession,
      // Si role est null (inscription publique), on envoie "USER" par défaut
      // Sinon (depuis l'admin), on envoie la valeur choisie ("ADMIN", etc.)
      'role': role ?? "USER",
    };

    final json = await apiClient.post('/api/auth/register', body);
    return AuthResponse.fromJson(json);
  }

  Future<AuthResponse> updateProfile(String email, String firstName, String lastName) async {
    final json = await apiClient.put('/api/user/profile', {
      'email': email,
      'firstName': firstName,
      'lastName': lastName,
    });
    return AuthResponse.fromJson(json);
  }

  Future<void> changePassword(String email, String newPassword) async {
    await apiClient.put('/api/user/password', {
      'email': email,
      'newPassword': newPassword,
    });
  }
}