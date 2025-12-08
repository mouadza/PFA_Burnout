import 'package:flutter/material.dart';
import '../services/api_client.dart';

class AdminUserDetailsScreen extends StatefulWidget {
  final Map<String, dynamic> user;

  const AdminUserDetailsScreen({super.key, required this.user});

  @override
  State<AdminUserDetailsScreen> createState() => _AdminUserDetailsScreenState();
}

class _AdminUserDetailsScreenState extends State<AdminUserDetailsScreen> {
  final apiClient = ApiClient(baseUrl: 'http://10.0.2.2:8080'); // ⚠️ Vérifiez votre IP
  late bool isApproved;
  bool isLoading = false;

  @override
  void initState() {
    super.initState();
    // On récupère le statut actuel (par défaut false si null)
    isApproved = widget.user['enabled'] ?? false;
  }

  Future<void> _toggleApproval(bool value) async {
    setState(() {
      isApproved = value;
      isLoading = true;
    });

    try {
      final int userId = widget.user['id'];

      // 🔄 Appel API pour changer le statut
      // Adaptez l'endpoint selon votre backend Keycloak/Spring
      // Exemple : PATCH /api/admin/users/{id} avec body { "enabled": true }
      await apiClient.put('/api/admin/users/$userId', {
        'enabled': value,
        // On renvoie les autres infos pour ne pas les écraser si l'API le demande
        'firstName': widget.user['firstName'],
        'lastName': widget.user['lastName'],
        'email': widget.user['email'],
        'role': widget.user['role'],
      });

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(value ? "Compte approuvé (Activé)" : "Compte désactivé"),
            backgroundColor: value ? Colors.green : Colors.orange,
          ),
        );
      }
    } catch (e) {
      // En cas d'erreur, on revient en arrière
      if (mounted) {
        setState(() {
          isApproved = !value;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text("Erreur : $e"), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = widget.user;
    final String initials = (user['firstName']?[0] ?? '') + (user['lastName']?[0] ?? '');

    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA),
      appBar: AppBar(
        title: const Text("Détails Utilisateur", style: TextStyle(color: Colors.black)),
        backgroundColor: Colors.white,
        elevation: 0,
        iconTheme: const IconThemeData(color: Colors.black),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            const SizedBox(height: 20),
            // Avatar Géant
            CircleAvatar(
              radius: 50,
              backgroundColor: Colors.indigo,
              child: Text(
                initials.toUpperCase(),
                style: const TextStyle(fontSize: 32, color: Colors.white, fontWeight: FontWeight.bold),
              ),
            ),
            const SizedBox(height: 15),
            Text(
              "${user['firstName']} ${user['lastName']}",
              style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
            ),
            Text(
              user['email'] ?? '',
              style: const TextStyle(fontSize: 16, color: Colors.grey),
            ),
            const SizedBox(height: 30),

            // Section Approbation
            Container(
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(15),
                boxShadow: [BoxShadow(color: Colors.black12, blurRadius: 10)],
              ),
              child: SwitchListTile(
                title: const Text("Approuver ce compte", style: TextStyle(fontWeight: FontWeight.bold)),
                subtitle: Text(
                  isApproved ? "L'utilisateur peut se connecter" : "L'utilisateur est bloqué",
                  style: TextStyle(color: isApproved ? Colors.green : Colors.red),
                ),
                value: isApproved,
                activeColor: Colors.green,
                onChanged: isLoading ? null : _toggleApproval,
                secondary: Icon(
                  isApproved ? Icons.check_circle : Icons.block,
                  color: isApproved ? Colors.green : Colors.red,
                  size: 30,
                ),
              ),
            ),

            const SizedBox(height: 20),

            // Section Informations
            _buildInfoSection(),
          ],
        ),
      ),
    );
  }

  Widget _buildInfoSection() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(15),
      ),
      child: Column(
        children: [
          _buildInfoRow(Icons.work, "Profession", widget.user['profession'] ?? 'N/A'),
          const Divider(),
          _buildInfoRow(Icons.security, "Rôle", widget.user['role'] ?? 'USER'),
          const Divider(),
          _buildInfoRow(Icons.key, "ID Système", "${widget.user['id']}"),
        ],
      ),
    );
  }

  Widget _buildInfoRow(IconData icon, String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Row(
        children: [
          Icon(icon, color: Colors.indigo, size: 20),
          const SizedBox(width: 15),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: const TextStyle(color: Colors.grey, fontSize: 12)),
                Text(value, style: const TextStyle(fontWeight: FontWeight.w500, fontSize: 16)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}