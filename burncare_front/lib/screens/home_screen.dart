import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import 'auth/login_screen.dart';


import 'settings_screen.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final user = context.watch<AuthProvider>().user;

    // 🔒 Sécurité
    if (user == null) {
      Future.microtask(() => Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (_) => const LoginScreen()),
      ));
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    // ✅ Affichage Nom
    String displayName = "Utilisateur";
    if (user.lastName.isNotEmpty || user.firstName.isNotEmpty) {
      displayName = "${user.lastName} ${user.firstName}".trim();
    }

    // ✅ Initiales
    String initials = "?";
    if (user.lastName.isNotEmpty && user.firstName.isNotEmpty) {
      initials = "${user.lastName[0]}${user.firstName[0]}".toUpperCase();
    } else if (user.lastName.isNotEmpty) {
      initials = user.lastName[0].toUpperCase();
    } else if (user.firstName.isNotEmpty) {
      initials = user.firstName[0].toUpperCase();
    }

    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA),
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
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // 👋 EN-TÊTE UTILISATEUR
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
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          "Bonjour, $displayName",
                          style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
                        ),
                        Text(
                          user.profession,
                          style: const TextStyle(color: Colors.white70, fontSize: 14),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 30),

            const Text(
              "Que souhaitez-vous faire ?",
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colors.black87),
            ),

            const SizedBox(height: 20),

            // 1️⃣ GRAND BLOC : COMMENCER LE QUESTIONNAIRE
            _ActionCard(
              title: "Commencer le Questionnaire",
              subtitle: "Évaluez votre niveau de stress et de burnout",
              icon: Icons.play_circle_fill,
              color: Colors.blue,
              isLarge: true,
              onTap: () {
                // TODO: Naviguer vers la page du questionnaire
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text("Lancement du questionnaire...")),
                );
              },
            ),

            const SizedBox(height: 20),

            // 2️⃣ & 3️⃣ DEUX BLOCS : RÉSULTATS ET SETTINGS
            Row(
              children: [
                Expanded(
                  child: _ActionCard(
                    title: "Mes Résultats",
                    subtitle: "Historique",
                    icon: Icons.history, // ou Icons.bar_chart
                    color: Colors.purple,
                    onTap: () {
                      // TODO: Naviguer vers l'historique
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text("Ouverture des résultats...")),
                      );
                    },
                  ),
                ),
                const SizedBox(width: 15),
                Expanded(
                  child: _ActionCard(
                    title: "Paramètres",
                    subtitle: "Compte & App",
                    icon: Icons.settings,
                    color: Colors.grey,
                    onTap: () {
                      // ✅ NAVIGATION VERS LA PAGE SETTINGS
                      Navigator.push(
                        context,
                        MaterialPageRoute(builder: (_) => const SettingsScreen()),
                      );
                    },
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

// Widget personnalisé pour les cartes
class _ActionCard extends StatelessWidget {
  final String title;
  final String subtitle;
  final IconData icon;
  final Color color;
  final VoidCallback onTap;
  final bool isLarge;

  const _ActionCard({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.color,
    required this.onTap,
    this.isLarge = false,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(20),
      child: Container(
        height: isLarge ? 160 : 140, // Hauteur différente selon l'importance
        padding: const EdgeInsets.all(20),
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
          crossAxisAlignment: isLarge ? CrossAxisAlignment.start : CrossAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: color.withOpacity(0.1),
                shape: BoxShape.circle,
              ),
              child: Icon(icon, size: isLarge ? 40 : 30, color: color),
            ),
            const Spacer(),
            Text(
              title,
              textAlign: isLarge ? TextAlign.left : TextAlign.center,
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 16,
                color: Colors.black87,
              ),
            ),
            if (isLarge) ...[
              const SizedBox(height: 5),
              Text(
                subtitle,
                style: TextStyle(color: Colors.grey.shade600, fontSize: 13),
              ),
            ]
          ],
        ),
      ),
    );
  }
}