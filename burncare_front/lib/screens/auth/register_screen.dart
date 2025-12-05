import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import 'login_screen.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  // ✅ REMPLACEMENT : _nameController devient _firstNameController et _lastNameController
  final _firstNameController = TextEditingController();
  final _lastNameController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();

  String profession = "MEDECIN";
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
                const Text("BurnCare",
                    style: TextStyle(fontSize: 28, fontWeight: FontWeight.bold)),
                const Text("Prévention du Burnout"),
                const SizedBox(height: 25),

                // Onglets Connexion / Inscription
                Row(
                  children: [
                    _tab("Connexion", false, () {
                      Navigator.push(context,
                          MaterialPageRoute(builder: (_) => const LoginScreen()));
                    }),
                    _tab("Inscription", true, () {}),
                  ],
                ),

                const SizedBox(height: 25),

                // ✅ NOUVEAUX CHAMPS : Prénom et Nom séparés
                _input(_firstNameController, Icons.person, "Prénom"),
                const SizedBox(height: 15),
                _input(_lastNameController, Icons.person_outline, "Nom"),
                const SizedBox(height: 15),

                _input(_emailController, Icons.email_outlined, "Email"),
                const SizedBox(height: 15),
                _input(_passwordController, Icons.lock_outline, "Mot de passe", true),

                const SizedBox(height: 20),
                const Align(
                  alignment: Alignment.centerLeft,
                  child: Text("Profession",
                      style: TextStyle(fontWeight: FontWeight.bold)),
                ),

                RadioListTile(
                  title: const Text("Médecin"),
                  value: "MEDECIN",
                  groupValue: profession,
                  onChanged: (value) {
                    setState(() => profession = value.toString());
                  },
                ),
                RadioListTile(
                  title: const Text("Infirmier(ère)"),
                  value: "INFIRMIER",
                  groupValue: profession,
                  onChanged: (value) {
                    setState(() => profession = value.toString());
                  },
                ),

                const SizedBox(height: 20),

                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: loading ? null : _register,
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
                      "S'inscrire",
                      style: TextStyle(color: Colors.white, fontSize: 16),
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

  Widget _input(TextEditingController controller, IconData icon, String label,
      [bool isPass = false]) {
    return TextField(
      controller: controller,
      obscureText: isPass,
      decoration: InputDecoration(
        prefixIcon: Icon(icon),
        labelText: label,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
      ),
    );
  }

  Future<void> _register() async {
    // 1. Validation : On vérifie que Prénom et Nom sont remplis
    if (_firstNameController.text.isEmpty ||
        _lastNameController.text.isEmpty ||
        _emailController.text.isEmpty ||
        _passwordController.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Veuillez remplir tous les champs")),
      );
      return;
    }

    setState(() => loading = true);

    // 2. Appel au Provider avec les 5 arguments (firstName, lastName, email, password, profession)
    bool success = await context.read<AuthProvider>().register(
      _firstNameController.text.trim(),
      _lastNameController.text.trim(),
      _emailController.text.trim(),
      _passwordController.text.trim(),
      profession,
    );

    setState(() => loading = false);

    if (!mounted) return;

    if (success) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text("Compte créé avec succès ! Connectez-vous."),
          backgroundColor: Colors.green,
        ),
      );
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (_) => const LoginScreen()),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text("Erreur : Email déjà utilisé ou problème technique."),
          backgroundColor: Colors.red,
        ),
      );
    }
  }
}