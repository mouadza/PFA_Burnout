import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:provider/provider.dart';

import '../../providers/auth_provider.dart';

class MyResultsScreen extends StatefulWidget {
  const MyResultsScreen({super.key});

  @override
  State<MyResultsScreen> createState() => _MyResultsScreenState();
}

class _MyResultsScreenState extends State<MyResultsScreen> {
  bool _loading = true;
  String? _error;
  List<BurnoutResultItem> _results = [];

  @override
  void initState() {
    super.initState();
    _fetchResults();
  }

  Color _colorForRisk(String riskLabel) {
    switch (riskLabel) {
      case "Faible":
        return Colors.green;
      case "Moyen":
        return Colors.orange;
      case "Élevé":
      default:
        return Colors.red;
    }
  }

  Future<void> _fetchResults() async {
    try {
      final auth = context.read<AuthProvider>();
      final String? token = auth.token; // getter ajouté dans AuthProvider

      if (token == null) {
        setState(() {
          _loading = false;
          _error = "Utilisateur non authentifié.";
        });
        return;
      }

      const String apiUrl = "http://10.0.2.2:8080/api/burnout-results/me";

      final response = await http.get(
        Uri.parse(apiUrl),
        headers: {
          "Authorization": "Bearer $token",
        },
      );

      if (response.statusCode == 200) {
        final List<dynamic> jsonList = jsonDecode(response.body);
        final items = jsonList
            .map((e) => BurnoutResultItem.fromJson(e as Map<String, dynamic>))
            .toList();

        setState(() {
          _results = items;
          _loading = false;
          _error = null;
        });
      } else {
        setState(() {
          _loading = false;
          _error = "Erreur API : ${response.statusCode}";
        });
      }
    } catch (e) {
      setState(() {
        _loading = false;
        _error = "Erreur de connexion : $e";
      });
    }
  }

  String _formatDate(DateTime dt) {
    final local = dt.toLocal();
    final dd = local.day.toString().padLeft(2, '0');
    final mm = local.month.toString().padLeft(2, '0');
    final yyyy = local.year.toString();
    final hh = local.hour.toString().padLeft(2, '0');
    final min = local.minute.toString().padLeft(2, '0');
    return "$dd/$mm/$yyyy $hh:$min";
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Mes résultats"),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: _loading
            ? const Center(child: CircularProgressIndicator())
            : _error != null
            ? Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                _error!,
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.red),
              ),
              const SizedBox(height: 12),
              ElevatedButton(
                onPressed: () {
                  setState(() {
                    _loading = true;
                    _error = null;
                  });
                  _fetchResults();
                },
                child: const Text("Réessayer"),
              ),
            ],
          ),
        )
            : _results.isEmpty
            ? const Center(
          child: Text(
            "Vous n'avez pas encore de résultats enregistrés.",
            textAlign: TextAlign.center,
          ),
        )
            : ListView.builder(
          itemCount: _results.length,
          itemBuilder: (context, index) {
            final item = _results[index];
            final color = _colorForRisk(item.riskLabel);

            return Card(
              margin: const EdgeInsets.only(bottom: 12),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
              child: Padding(
                padding: const EdgeInsets.all(12.0),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Badge cercle avec score
                    Container(
                      width: 52,
                      height: 52,
                      decoration: BoxDecoration(
                        color: color.withOpacity(0.1),
                        shape: BoxShape.circle,
                      ),
                      child: Center(
                        child: Text(
                          item.burnoutScore.toString(),
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            color: color,
                            fontSize: 16,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment:
                        CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Text(
                                item.riskTitle,
                                style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 16,
                                  color: color,
                                ),
                              ),
                              const SizedBox(width: 8),
                              Container(
                                padding:
                                const EdgeInsets.symmetric(
                                  horizontal: 8,
                                  vertical: 4,
                                ),
                                decoration: BoxDecoration(
                                  color: color.withOpacity(0.12),
                                  borderRadius:
                                  BorderRadius.circular(12),
                                ),
                                child: Text(
                                  item.riskLabel,
                                  style: TextStyle(
                                    color: color,
                                    fontSize: 12,
                                    fontWeight: FontWeight.w500,
                                  ),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 4),
                          Text(
                            _formatDate(item.createdAt),
                            style: TextStyle(
                              fontSize: 12,
                              color: Colors.grey.shade600,
                            ),
                          ),
                          if (item.message != null &&
                              item.message!.isNotEmpty) ...[
                            const SizedBox(height: 8),
                            Text(
                              item.message!,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                fontSize: 13,
                              ),
                            ),
                          ],
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}

class BurnoutResultItem {
  final int id;
  final int burnoutScore;   // 0–100
  final String riskLabel;   // "Faible" / "Moyen" / "Élevé"
  final String riskTitle;   // "Risque Élevé"
  final DateTime createdAt;
  final String? message;

  BurnoutResultItem({
    required this.id,
    required this.burnoutScore,
    required this.riskLabel,
    required this.riskTitle,
    required this.createdAt,
    this.message,
  });

  factory BurnoutResultItem.fromJson(Map<String, dynamic> json) {
    return BurnoutResultItem(
      id: json['id'] as int,
      burnoutScore: json['burnoutScore'] as int,
      riskLabel: json['riskLabel'] as String,
      riskTitle: json['riskTitle'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
      message: json['message'] as String?, // si tu la renvoies dans ta response
    );
  }
}