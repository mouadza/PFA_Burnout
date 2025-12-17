import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/mockito.dart';
import 'package:mockito/annotations.dart';

// Assure-toi que ces imports correspondent bien à tes dossiers
import 'package:burncare_front/providers/auth_provider.dart';
import 'package:burncare_front/services/auth_service.dart';
import 'package:burncare_front/models/auth_response.dart';

// Cette ligne permet de générer le Mock.
// Le fichier .mocks.dart sera créé par la commande build_runner.
@GenerateMocks([AuthService])
import 'auth_provider_test.mocks.dart';

void main() {
  late MockAuthService mockAuthService;
  late AuthProvider authProvider;

  // Configuration globale avant tous les tests
  setUpAll(() {
    TestWidgetsFlutterBinding.ensureInitialized();

    // MOCK du FlutterSecureStorage
    // On intercepte les appels natifs pour éviter l'erreur "MissingPluginException"
    const MethodChannel('plugins.it_nomads.com/flutter_secure_storage')
        .setMockMethodCallHandler((MethodCall methodCall) async {
      return 'dummy_token'; // Simule une écriture/lecture réussie
    });
  });

  // Réinitialisation avant chaque test individuel
  setUp(() {
    mockAuthService = MockAuthService();
    authProvider = AuthProvider(mockAuthService);
  });

  group('AuthProvider Tests', () {

    // --- TEST 1 : Login Succès ---
    test('login success doit mettre à jour le user et retourner true', () async {
      // 1. Préparer la fausse réponse
      final mockUser = AuthResponse(
          token: 'fake-token',
          email: 'test@test.com',
          firstName: 'John',
          lastName: 'Doe',
          role: 'USER',
          profession: 'Dev'
      );

      // 2. Configurer le Mock : Quand on appelle login, renvoyer mockUser
      when(mockAuthService.login(any, any))
          .thenAnswer((_) async => mockUser);

      // 3. Exécuter l'action
      final success = await authProvider.login('test@test.com', '123456');

      // 4. Vérifier les résultats
      expect(success, true);
      expect(authProvider.isAuthenticated, true);
      expect(authProvider.user?.email, 'test@test.com'); // Corrigé ici (user au lieu de currentUser)
      expect(authProvider.errorMessage, isNull);
    });

    // --- TEST 2 : Login Échec (Mauvais mot de passe) ---
    test('login fail avec 401 doit mettre un message d\'erreur spécifique', () async {
      // Préparer une exception simulée
      when(mockAuthService.login(any, any))
          .thenThrow(Exception('Erreur 401 Unauthorized'));

      final success = await authProvider.login('test@test.com', 'badpass');

      expect(success, false);
      expect(authProvider.isAuthenticated, false);
      expect(authProvider.errorMessage, "Email ou mot de passe incorrect");
    });

    // --- TEST 3 : Login Échec (Compte bloqué) ---
    test('login fail avec compte bloqué doit avertir l\'utilisateur', () async {
      // Préparer une exception simulée contenant le mot "locked"
      when(mockAuthService.login(any, any))
          .thenThrow(Exception('User account is locked'));

      final success = await authProvider.login('banni@test.com', '123456');

      expect(success, false);
      expect(authProvider.errorMessage, contains("Votre compte est bloqué"));
    });

    // --- TEST 4 : Logout ---
    test('logout doit nettoyer l\'utilisateur', () async {
      // Action
      await authProvider.logout();

      // Vérification
      expect(authProvider.user, null); // Corrigé ici
      expect(authProvider.isAuthenticated, false);
      expect(authProvider.token, null);
    });

    // --- TEST 5 : Register ---
    test('register success doit retourner true', () async {
      // On crée un faux objet de réponse (même s'il n'est pas utilisé, il est obligatoire pour le type)
      final mockRegisterResponse = AuthResponse(
          token: 'new-token',
          firstName: 'Jean',
          lastName: 'Bon',
          email: 'j@j.com',
          role: 'USER',
          profession: 'Pro'
      );

      // Configuration du Mock : On renvoie l'objet AuthResponse, pas {}
      when(mockAuthService.register(
        firstName: anyNamed('firstName'),
        lastName: anyNamed('lastName'),
        email: anyNamed('email'),
        password: anyNamed('password'),
        profession: anyNamed('profession'),
        role: anyNamed('role'),
      )).thenAnswer((_) async => mockRegisterResponse); // <--- CORRECTION ICI

      // Action
      final success = await authProvider.register(
          'Jean', 'Bon', 'j@j.com', '123', 'Pro', role: 'USER'
      );

      // Vérification
      expect(success, true);
      expect(authProvider.errorMessage, isNull);
    });
  });
}