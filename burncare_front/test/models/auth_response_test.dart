import 'package:flutter_test/flutter_test.dart';
import 'package:burncare_front/models/auth_response.dart';

void main() {
  group('AuthResponse Model', () {

    // TEST 1 : Vérifie que le JSON est bien converti quand tout est rempli
    test('fromJson doit créer un objet valide avec toutes les données', () {
      // Données simulées
      final Map<String, dynamic> jsonFull = {
        'token': 'tok-123',
        'firstName': 'Jean',
        'lastName': 'Dupont',
        'email': 'jean@test.com',
        'role': 'ADMIN',
        'profession': 'Infirmier',
      };

      // Action
      final result = AuthResponse.fromJson(jsonFull);

      // Vérifications
      expect(result.token, 'tok-123');
      expect(result.firstName, 'Jean');
      expect(result.lastName, 'Dupont');
      expect(result.email, 'jean@test.com');
      expect(result.role, 'ADMIN');
      expect(result.profession, 'Infirmier');
    });

    // TEST 2 : Vérifie les valeurs par défaut (les '??') quand le JSON est vide
    test('fromJson doit utiliser les valeurs par défaut si le JSON est vide', () {
      // Données vides
      final Map<String, dynamic> jsonEmpty = {};

      // Action
      final result = AuthResponse.fromJson(jsonEmpty);

      // Vérifications des valeurs par défaut définies dans ton code
      expect(result.token, '');
      expect(result.firstName, '');
      expect(result.role, 'USER'); // C'est le plus important à tester ici
      expect(result.profession, '');
    });
  });
}