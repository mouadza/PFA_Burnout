import 'package:flutter/material.dart';
import '../../services/api_client.dart';
import '../../utils/api_config.dart';
import 'admin_user_details_screen.dart';

class AdminUsersScreen extends StatefulWidget {
  const AdminUsersScreen({super.key});

  @override
  State<AdminUsersScreen> createState() => _AdminUsersScreenState();
}

class _AdminUsersScreenState extends State<AdminUsersScreen> {
  // ✅ URL adaptée automatiquement selon la plateforme
  final apiClient = ApiClient(baseUrl: getSpringApiBaseUrl());

  List<dynamic> users = [];
  bool isLoading = true;
  String? error;

  @override
  void initState() {
    super.initState();
    _fetchUsers();
  }

  // --- RÉCUPÉRATION DES UTILISATEURS ---
  Future<void> _fetchUsers() async {
    setState(() {
      isLoading = true;
      error = null;
    });
    try {
      final fetchedUsers = await apiClient.get('/api/admin/users');
      setState(() {
        users = fetchedUsers;
        isLoading = false;
      });
    } catch (e) {
      setState(() {
        error = e.toString();
        isLoading = false;
      });
    }
  }

  // --- SUPPRESSION D'UN UTILISATEUR ---
  Future<void> _deleteUser(int id) async {
    bool confirm = await showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text("Confirmer"),
        content: const Text("Voulez-vous vraiment supprimer cet utilisateur ?"),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text("Annuler")),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text("Supprimer", style: TextStyle(color: Colors.red))),
        ],
      ),
    ) ?? false;

    if (!confirm) return;

    try {
      await apiClient.delete('/api/admin/users/$id');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Utilisateur supprimé"), backgroundColor: Colors.green));
      }
      _fetchUsers(); // Rafraîchir la liste
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Erreur: $e"), backgroundColor: Colors.red));
      }
    }
  }

  // --- AJOUT D'UN UTILISATEUR ---
  Future<void> _addUser(Map<String, dynamic> userData) async {
    try {
      // On suppose que votre ApiClient a une méthode post, sinon utilisez http.post
      await apiClient.post('/api/auth/register', userData);
      // Note: J'utilise /api/auth/register car c'est souvent là qu'on crée les users,
      // mais si vous avez une route admin spécifique (ex: /api/admin/users), changez l'URL ci-dessus.

      if (mounted) {
        Navigator.pop(context); // Fermer le dialogue
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Utilisateur ajouté avec succès"), backgroundColor: Colors.green));
        _fetchUsers(); // Rafraîchir la liste
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Erreur lors de l'ajout: $e"), backgroundColor: Colors.red));
      }
    }
  }

  // --- FORMULAIRE D'AJOUT ---
  void _showAddUserDialog() {
    final formKey = GlobalKey<FormState>();

    // Variables pour stocker les saisies
    String firstName = '';
    String lastName = '';
    String email = '';
    String password = '';
    String role = 'USER'; // Valeur par défaut
    String profession = 'MEDECIN'; // Valeur par défaut
    // Par défaut, on active le compte à la création
    bool enabled = true;

    // Liste des rôles disponibles
    final List<String> roles = ['USER', 'ADMIN'];
    // ✅ Liste des professions mise à jour
    final List<String> professions = ['MEDECIN', 'INFIRMIER', 'ADMIN'];

    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          title: const Text("Ajouter un utilisateur"),
          content: SingleChildScrollView(
            child: Form(
              key: formKey,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextFormField(
                    decoration: const InputDecoration(labelText: "Prénom", prefixIcon: Icon(Icons.person)),
                    validator: (v) => v == null || v.isEmpty ? 'Requis' : null,
                    onSaved: (v) => firstName = v!,
                  ),
                  TextFormField(
                    decoration: const InputDecoration(labelText: "Nom", prefixIcon: Icon(Icons.person_outline)),
                    validator: (v) => v == null || v.isEmpty ? 'Requis' : null,
                    onSaved: (v) => lastName = v!,
                  ),
                  TextFormField(
                    decoration: const InputDecoration(labelText: "Email", prefixIcon: Icon(Icons.email)),
                    keyboardType: TextInputType.emailAddress,
                    validator: (v) => v == null || v.isEmpty ? 'Requis' : null,
                    onSaved: (v) => email = v!,
                  ),
                  TextFormField(
                    decoration: const InputDecoration(labelText: "Mot de passe", prefixIcon: Icon(Icons.lock)),
                    obscureText: true,
                    validator: (v) => v == null || v.length < 4 ? 'Min 4 caractères' : null,
                    onSaved: (v) => password = v!,
                  ),
                  const SizedBox(height: 15),
                  DropdownButtonFormField<String>(
                    value: role,
                    decoration: const InputDecoration(labelText: "Rôle", border: OutlineInputBorder()),
                    items: roles.map((r) => DropdownMenuItem(value: r, child: Text(r))).toList(),
                    onChanged: (val) => role = val!,
                  ),
                  const SizedBox(height: 15),
                  DropdownButtonFormField<String>(
                    value: profession,
                    decoration: const InputDecoration(labelText: "Profession", border: OutlineInputBorder()),
                    items: professions.map((p) => DropdownMenuItem(value: p, child: Text(p))).toList(),
                    onChanged: (val) => profession = val!,
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text("Annuler"),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: Colors.indigo, foregroundColor: Colors.white),
              onPressed: () {
                if (formKey.currentState!.validate()) {
                  formKey.currentState!.save();
                  // Construction de l'objet JSON à envoyer
                  _addUser({
                    "firstName": firstName,
                    "lastName": lastName,
                    "email": email,
                    "password": password,
                    "role": role,
                    "profession": profession,
                    "enabled": enabled // ✅ Envoi du statut 'enabled' au backend
                  });
                }
              },
              child: const Text("Ajouter"),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA),
      appBar: AppBar(
        title: const Text("Gestion Utilisateurs", style: TextStyle(color: Colors.black)),
        backgroundColor: Colors.white,
        iconTheme: const IconThemeData(color: Colors.black),
        elevation: 0,
      ),
      // ✅ BOUTON AJOUTER ICI
      floatingActionButton: FloatingActionButton(
        onPressed: _showAddUserDialog,
        backgroundColor: Colors.indigo,
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: isLoading
          ? const Center(child: CircularProgressIndicator())
          : error != null
          ? Center(child: Text("Erreur : $error", style: const TextStyle(color: Colors.red)))
          : ListView.builder(
        padding: const EdgeInsets.all(15),
        itemCount: users.length,
        itemBuilder: (ctx, index) {
          final user = users[index];
          final String name = "${user['firstName'] ?? ''} ${user['lastName'] ?? ''}".trim();
          final String role = user['role'] ?? 'USER';
          final String email = user['email'] ?? '';
          final int id = user['id'];
          // ✅ Récupération du statut (par défaut true si absent)
          final bool isEnabled = user['enabled'] ?? true;

          return Card(
            elevation: 2,
            margin: const EdgeInsets.only(bottom: 15),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
            child: ListTile(
              // ✅ AJOUT : Navigation vers les détails au clic
              onTap: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) => AdminUserDetailsScreen(user: user),
                  ),
                ).then((_) {
                  // On rafraîchit la liste au retour au cas où des modifications ont été faites
                  _fetchUsers();
                });
              },
              // ✅ Modification de l'avatar pour inclure un indicateur de statut
              leading: Stack(
                children: [
                  CircleAvatar(
                    backgroundColor: isEnabled
                        ? (role == 'ADMIN' ? Colors.black87 : Colors.blue)
                        : Colors.grey, // Grisé si désactivé
                    child: Text(name.isNotEmpty ? name[0].toUpperCase() : '?', style: const TextStyle(color: Colors.white)),
                  ),
                  Positioned(
                    bottom: 0,
                    right: 0,
                    child: Container(
                      width: 12,
                      height: 12,
                      decoration: BoxDecoration(
                        color: isEnabled ? Colors.green : Colors.red,
                        shape: BoxShape.circle,
                        border: Border.all(color: Colors.white, width: 2),
                      ),
                    ),
                  ),
                ],
              ),
              title: Text(
                  name.isEmpty ? "Utilisateur sans nom" : name,
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    color: isEnabled ? Colors.black : Colors.grey, // Texte grisé si désactivé
                  )
              ),
              subtitle: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(email, style: TextStyle(color: isEnabled ? Colors.black54 : Colors.grey)),
                  const SizedBox(height: 4),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                    decoration: BoxDecoration(
                      color: isEnabled
                          ? (role == 'ADMIN' ? Colors.red.withOpacity(0.1) : Colors.green.withOpacity(0.1))
                          : Colors.grey.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(5),
                    ),
                    child: Text(
                        role,
                        style: TextStyle(
                            fontSize: 12,
                            color: isEnabled
                                ? (role == 'ADMIN' ? Colors.red : Colors.green)
                                : Colors.grey
                        )
                    ),
                  )
                ],
              ),
              trailing: IconButton(
                icon: const Icon(Icons.delete, color: Colors.red),
                onPressed: () => _deleteUser(id),
              ),
            ),
          );
        },
      ),
    );
  }
}