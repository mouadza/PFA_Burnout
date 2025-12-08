import 'api_client.dart';

class AdminService {
  final ApiClient apiClient;

  AdminService(this.apiClient);

  // Récupérer la liste des utilisateurs
  Future<List<dynamic>> getAllUsers() async {
    final response = await apiClient.get('/api/admin/users');
    return response as List<dynamic>;
  }

  // Supprimer un utilisateur
  Future<void> deleteUser(int id) async {
    // Note: delete n'est pas encore dans ApiClient, on va utiliser une astuce ou l'ajouter
    // Pour l'instant, assurez-vous d'avoir ajouté la méthode delete dans ApiClient
    // ou utilisez apiClient.post avec une méthode spéciale si nécessaire.
    // L'idéal est d'ajouter delete() à ApiClient.

    // On suppose que ApiClient a une méthode delete, sinon voir ci-dessous
    // await apiClient.delete('/api/admin/users/$id');
  }
}