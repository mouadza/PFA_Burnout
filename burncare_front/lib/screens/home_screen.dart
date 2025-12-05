import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
// Vérifiez que ce chemin est correct selon votre structure de dossier
import 'auth/login_screen.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    // On récupère l'utilisateur depuis le Provider
    final user = context.watch<AuthProvider>().user;

    // 🔒 Sécurité : Si pas d'user, retour forcé au login
    if (user == null) {
      Future.microtask(() => Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (_) => const LoginScreen()),
      ));
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    // ✅ Gestion de l'affichage du nom (Nom + Prénom)
    String displayName = "Utilisateur"; // Valeur par défaut (pas d'email)

    // On vérifie si au moins l'un des deux est présent pour l'afficher
    if (user.lastName.isNotEmpty || user.firstName.isNotEmpty) {
      // .trim() enlève les espaces inutiles si l'un des deux est vide
      displayName = "${user.lastName} ${user.firstName}".trim();
    }

    // ✅ Calcul des initiales sécurisé (Nom Prénom)
    String initials = "?"; // Valeur par défaut

    if (user.lastName.isNotEmpty && user.firstName.isNotEmpty) {
      // 1ère lettre du Nom + 1ère lettre du Prénom
      initials = "${user.lastName[0]}${user.firstName[0]}".toUpperCase();
    } else if (user.lastName.isNotEmpty) {
      // Juste 1ère lettre du Nom
      initials = user.lastName[0].toUpperCase();
    } else if (user.firstName.isNotEmpty) {
      // Juste 1ère lettre du Prénom
      initials = user.firstName[0].toUpperCase();
    }

    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA), // Gris très clair
      appBar: AppBar(
        title: const Text('Tableau de bord', style: TextStyle(color: Colors.black)),
        backgroundColor: Colors.white,
        elevation: 0,
        iconTheme: const IconThemeData(color: Colors.black),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout, color: Colors.redAccent),
            onPressed: () async {
              await context.read<AuthProvider>().logout();
              if (context.mounted) {
                Navigator.pushAndRemoveUntil(
                  context,
                  MaterialPageRoute(builder: (_) => const LoginScreen()),
                      (_) => false,
                );
              }
            },
          )
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 👋 Section Bienvenue
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: Colors.blue.shade800,
                borderRadius: BorderRadius.circular(20),
                boxShadow: [
                  BoxShadow(
                    color: Colors.blue.withOpacity(0.3),
                    blurRadius: 10,
                    offset: const Offset(0, 5),
                  ),
                ],
              ),
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 30,
                    backgroundColor: Colors.white.withOpacity(0.2),
                    child: Text(
                      initials,
                      style: const TextStyle(fontSize: 20, color: Colors.white, fontWeight: FontWeight.bold),
                    ),
                  ),
                  const SizedBox(width: 15),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        "Bonjour, $displayName",
                        style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
                      ),
                      Text(
                        user.profession, // Affiche "MEDECIN" ou "INFIRMIER"
                        style: const TextStyle(color: Colors.white70, fontSize: 14),
                      ),
                    ],
                  ),
                ],
              ),
            ),

            const SizedBox(height: 30),

            // Titre de section
            Text(
              "Espace ${user.profession == 'MEDECIN' ? 'Médical' : 'Soins'}",
              style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colors.black87),
            ),

            const SizedBox(height: 15),

            // 🎛️ Grille de Menu Dynamique
            Expanded(
              child: user.profession == 'MEDECIN'
                  ? _buildDoctorMenu()
                  : _buildNurseMenu(),
            ),
          ],
        ),
      ),
    );
  }

  // --- MENU MÉDECIN ---
  Widget _buildDoctorMenu() {
    return GridView.count(
      crossAxisCount: 2,
      crossAxisSpacing: 15,
      mainAxisSpacing: 15,
      children: [
        _MenuCard(icon: Icons.people_alt, title: "Patients", color: Colors.blue),
        _MenuCard(icon: Icons.analytics_outlined, title: "Analyses Burnout", color: Colors.purple),
        _MenuCard(icon: Icons.calendar_month, title: "Rendez-vous", color: Colors.orange),
        _MenuCard(icon: Icons.settings, title: "Paramètres", color: Colors.grey),
      ],
    );
  }

  // --- MENU INFIRMIER ---
  Widget _buildNurseMenu() {
    return GridView.count(
      crossAxisCount: 2,
      crossAxisSpacing: 15,
      mainAxisSpacing: 15,
      children: [
        _MenuCard(icon: Icons.healing, title: "Soins du jour", color: Colors.teal),
        _MenuCard(icon: Icons.assignment_ind, title: "Suivi Patients", color: Colors.green),
        _MenuCard(icon: Icons.notifications_active, title: "Alertes", color: Colors.redAccent),
        _MenuCard(icon: Icons.chat, title: "Messages", color: Colors.indigo),
      ],
    );
  }
}

// Widget réutilisable pour les boutons du menu
class _MenuCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final Color color;

  const _MenuCard({required this.icon, required this.title, required this.color});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Ouverture de $title...")));
      },
      child: Container(
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(20),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.05),
              blurRadius: 10,
              offset: const Offset(0, 4),
            )
          ],
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(15),
              decoration: BoxDecoration(
                color: color.withOpacity(0.1),
                shape: BoxShape.circle,
              ),
              child: Icon(icon, size: 30, color: color),
            ),
            const SizedBox(height: 15),
            Text(title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          ],
        ),
      ),
    );
  }
}