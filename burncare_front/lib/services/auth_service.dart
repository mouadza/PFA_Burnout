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

  // ✅ CORRECTION : On envoie firstName et lastName séparément
  Future<AuthResponse> register({
    required String firstName,
    required String lastName,
    required String email,
    required String password,
    required String profession,
  }) async {
    final json = await apiClient.post('/api/auth/register', {
      'firstName': firstName, // ✅ Envoi du prénom
      'lastName': lastName,   // ✅ Envoi du nom
      'email': email,
      'password': password,
      'profession': profession,
    });
    return AuthResponse.fromJson(json);
  }
}