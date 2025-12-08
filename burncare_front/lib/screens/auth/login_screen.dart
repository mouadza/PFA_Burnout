import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import '../home_screen.dart';
import 'register_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  bool loading = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA),
      body: Center(
        child: SingleChildScrollView(
          child: Container(
            width: 350,
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(20),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.1),
                  blurRadius: 10,
                  offset: const Offset(0, 5),
                )
              ],
            ),
            child: Column(
              children: [
                const Icon(Icons.favorite, color: Colors.blue, size: 60),
                const SizedBox(height: 10),
                const Text(
                  "BurnCare",
                  style: TextStyle(fontSize: 28, fontWeight: FontWeight.bold),
                ),
                const Text("Prévention du Burnout"),
                const SizedBox(height: 25),

                // Onglets Connexion / Inscription
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    _tab("Connexion", true, () {}),
                    _tab("Inscription", false, () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(builder: (_) => const RegisterScreen()),
                      );
                    }),
                  ],
                ),

                const SizedBox(height: 25),

                // Email
                _inputField(
                  controller: _emailController,
                  icon: Icons.email_outlined,
                  label: "Email",
                ),

                // Mot de passe
                const SizedBox(height: 15),
                _inputField(
                  controller: _passwordController,
                  icon: Icons.lock_outline,
                  label: "Mot de passe",
                  isPassword: true,
                ),

                const SizedBox(height: 25),

                // Btn Se connecter
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: loading ? null : _login,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.blue,
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(10),
                      ),
                    ),
                    child: loading
                        ? const CircularProgressIndicator(color: Colors.white)
                        : const Text(
                      "Se connecter",
                      style: TextStyle(fontSize: 16, color: Colors.white),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _tab(String text, bool active, VoidCallback onTap) {
    return Expanded(
      child: InkWell(
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 12),
          decoration: BoxDecoration(
            color: active ? Colors.blue : Colors.transparent,
            borderRadius: BorderRadius.circular(50),
          ),
          child: Text(
            text,
            textAlign: TextAlign.center,
            style: TextStyle(
              color: active ? Colors.white : Colors.black54,
            ),
          ),
        ),
      ),
    );
  }

  Widget _inputField({
    required TextEditingController controller,
    required IconData icon,
    required String label,
    bool isPassword = false,
  }) {
    return TextField(
      controller: controller,
      obscureText: isPassword,
      decoration: InputDecoration(
        prefixIcon: Icon(icon),
        labelText: label,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
      ),
    );
  }

  Future<void> _login() async {
    setState(() => loading = true);

    // On récupère le provider
    final authProvider = context.read<AuthProvider>();

    // On tente le login
    bool success = await authProvider.login(
      _emailController.text.trim(),
      _passwordController.text.trim(),
    );

    setState(() => loading = false);

    if (!mounted) return;

    if (success) {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (_) => const HomeScreen()),
      );
    } else {
      // ✅ CORRECTION : On récupère le message dynamique depuis le Provider
      // Assurez-vous que votre AuthProvider a un champ 'errorMessage' ou 'error'
      // qui contient le message reçu du JSON Spring Boot.
      String message = authProvider.errorMessage ?? "Une erreur est survenue";

      // Si le message est vide ou null, on met un message par défaut,
      // mais on n'écrase pas le message du serveur s'il existe.
      if (message.isEmpty) message = "Email ou mot de passe incorrect";

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(message), // Affichage dynamique
          backgroundColor: Colors.red,
        ),
      );
    }
  }
}