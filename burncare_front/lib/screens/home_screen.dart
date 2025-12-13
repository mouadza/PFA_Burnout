import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import 'auth/login_screen.dart';
import 'admin_home_screen.dart';
import 'user_home_screen.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final user = context.watch<AuthProvider>().user;

    // 1. Sécurité
    if (user == null) {
      Future.microtask(() => Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (_) => const LoginScreen()),
      ));
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    // 2. Récupération Rôle et Profession
    final String role = user.role.trim().toUpperCase();
    final String profession = user.profession.trim().toUpperCase();

    // 3. Aiguillage
    if (role == 'ADMIN' || profession == 'ADMIN') {
      // ✅ CORRECTION 2 : On retourne le Dashboard (AdminHomeScreen)
      // Avant c'était : return const AdminUsersScreen();
      return const AdminHomeScreen();
    } else {
      return const UserHomeScreen();
    }
  }
}