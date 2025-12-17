import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:provider/provider.dart';
import '../user_home_screen.dart';

import '../../../providers/auth_provider.dart';

class ResultPage extends StatefulWidget {
  final double score;            // 0–100
  final String riskTitle;
  final String riskLabel;
  final String message;

  // ✅ NEW
  final double confidence;       // 0..1
  final List<dynamic> recommendations; // recos détaillées (plan)

  final List<int> answers;

  const ResultPage({
    super.key,
    required this.score,
    required this.riskTitle,
    required this.riskLabel,
    required this.message,
    required this.confidence,
    required this.recommendations,
    required this.answers,
  });

  @override
  State<ResultPage> createState() => _ResultPageState();
}

class _ResultPageState extends State<ResultPage> {
  bool _saving = false;
  String? _saveError;

  Color _colorForRisk() {
    switch (widget.riskLabel) {
      case "Faible":
        return Colors.green;
      case "Moyen":
        return Colors.orange;
      case "Élevé":
      default:
        return Colors.red;
    }
  }

  Icon _iconForTag(String? tag) {
    switch (tag) {
      case "sécurité":
        return const Icon(Icons.warning, color: Colors.red);
      case "repos":
        return const Icon(Icons.hotel, color: Colors.blue);
      case "mental":
        return const Icon(Icons.psychology, color: Colors.orange);
      case "vigilance":
        return const Icon(Icons.visibility, color: Colors.purple);
      case "pause":
        return const Icon(Icons.pause_circle, color: Colors.teal);
      case "shift":
        return const Icon(Icons.nightlight_round, color: Colors.indigo);
      case "planning":
        return const Icon(Icons.calendar_month, color: Colors.brown);
      default:
        return const Icon(Icons.lightbulb_outline);
    }
  }

  @override
  void initState() {
    super.initState();
    Future.microtask(_saveResult);
  }

  Future<void> _saveResult() async {
    try {
      final auth = context.read<AuthProvider>();
      final String? token = auth.token;

      if (token == null) {
        print("⚠️ Aucun token disponible, résultat non sauvegardé.");
        return;
      }

      setState(() {
        _saving = true;
        _saveError = null;
      });

      const String springApiUrl = "http://10.0.2.2:8080/api/burnout-results";

      // ✅ Option: on stocke tout (reco détaillées) au lieu d’un string
      final response = await http.post(
        Uri.parse(springApiUrl),
        headers: {
          "Content-Type": "application/json",
          "Authorization": "Bearer $token",
        },
        body: jsonEncode({
          "burnoutScore": widget.score.toInt(),
          "riskLabel": widget.riskLabel,
          "riskTitle": widget.riskTitle,
          "message": widget.message,

          // ✅ NEW champs utiles
          "confidence": widget.confidence,
          "recommendations": widget.recommendations, // JSON array complet
          "answers": widget.answers,
        }),
      );

      print("DEBUG save status = ${response.statusCode}");
      print("DEBUG save body   = ${response.body}");

      if (response.statusCode < 200 || response.statusCode >= 300) {
        setState(() => _saveError = "Erreur lors de l'enregistrement du résultat.");
      }
    } catch (e) {
      print("❌ Exception pendant la sauvegarde: $e");
      if (mounted) {
        setState(() => _saveError = "Erreur de connexion lors de l'enregistrement.");
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  List<String> _listFrom(dynamic v) {
    if (v is List) return v.map((e) => e.toString()).toList();
    return const [];
  }

  Widget _bullets(String label, List<String> items) {
    if (items.isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: const TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 4),
          ...items.map((t) => Padding(
            padding: const EdgeInsets.only(bottom: 2),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text("• "),
                Expanded(child: Text(t)),
              ],
            ),
          )),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final Color mainColor = _colorForRisk();

    return Scaffold(
      appBar: AppBar(title: const Text("Résultats de l'évaluation")),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (_saving)
              const Padding(
                padding: EdgeInsets.only(bottom: 8.0),
                child: Row(
                  children: [
                    SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2)),
                    SizedBox(width: 8),
                    Text("Enregistrement de votre résultat...", style: TextStyle(fontSize: 12)),
                  ],
                ),
              ),
            if (_saveError != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 8.0),
                child: Text(_saveError!, style: const TextStyle(color: Colors.red, fontSize: 12)),
              ),

            Card(
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
              elevation: 3,
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text("Résultats de l'évaluation",
                        style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                    const SizedBox(height: 8),
                    Text("Analyse de votre risque de burnout", style: Theme.of(context).textTheme.bodySmall),

                    const SizedBox(height: 16),
                    Center(
                      child: SizedBox(
                        width: 140,
                        height: 140,
                        child: Stack(
                          alignment: Alignment.center,
                          children: [
                            SizedBox(
                              width: 120,
                              height: 120,
                              child: CircularProgressIndicator(
                                value: widget.score / 100.0,
                                strokeWidth: 10,
                                backgroundColor: Colors.grey.shade200,
                                valueColor: AlwaysStoppedAnimation<Color>(mainColor),
                              ),
                            ),
                            Text(widget.score.toStringAsFixed(0),
                                style: const TextStyle(fontSize: 28, fontWeight: FontWeight.bold)),
                          ],
                        ),
                      ),
                    ),

                    const SizedBox(height: 12),

                    // ✅ confiance
                    Text(
                      "Confiance modèle : ${(widget.confidence * 100).toStringAsFixed(0)}%",
                      style: Theme.of(context).textTheme.bodySmall,
                    ),

                    const SizedBox(height: 12),

                    Row(
                      children: [
                        Icon(Icons.warning_amber_rounded, color: mainColor, size: 28),
                        const SizedBox(width: 8),
                        Text(widget.riskTitle,
                            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: mainColor)),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Text(widget.message, style: Theme.of(context).textTheme.bodyMedium),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 16),

            Text("Recommandations personnalisées",
                style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),

            if (widget.recommendations.isEmpty)
              const Card(
                child: Padding(
                  padding: EdgeInsets.all(16),
                  child: Text("Aucune recommandation disponible."),
                ),
              ),

            ...widget.recommendations.map((rec) {
              final r = (rec as Map).cast<String, dynamic>();
              final String title = r["title"]?.toString() ?? "";
              final String tag = r["tag"]?.toString() ?? "";
              final String why = r["why"]?.toString() ?? "";
              final int severity = (r["severity"] ?? 1) is int
                  ? (r["severity"] ?? 1) as int
                  : int.tryParse(r["severity"].toString()) ?? 1;

              final Map<String, dynamic> plan =
              (r["plan"] ?? {}) is Map ? (r["plan"] as Map).cast<String, dynamic>() : <String, dynamic>{};

              final now = _listFrom(plan["now"]);
              final next30 = _listFrom(plan["next_30_min"]);
              final during = _listFrom(plan["during_shift"]);
              final after = _listFrom(plan["after_shift"]);
              final avoid = _listFrom(plan["avoid"]);

              return Card(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                elevation: 2,
                child: Padding(
                  padding: const EdgeInsets.all(14),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          _iconForTag(tag),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                            decoration: BoxDecoration(
                              color: Colors.black.withOpacity(0.06),
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Text("Sév. $severity/5", style: const TextStyle(fontSize: 12)),
                          ),
                        ],
                      ),
                      if (why.isNotEmpty) ...[
                        const SizedBox(height: 8),
                        Text("Pourquoi : $why"),
                      ],
                      _bullets("Maintenant", now),
                      _bullets("Dans 30 minutes", next30),
                      _bullets("Pendant le service", during),
                      _bullets("Après le service", after),
                      _bullets("À éviter", avoid),
                    ],
                  ),
                ),
              );
            }).toList(),

            const SizedBox(height: 12),

            OutlinedButton.icon(
              onPressed: () => Navigator.popUntil(context, (route) => route.isFirst),
              icon: const Icon(Icons.replay),
              label: const Text("Refaire le test"),
            ),

            const SizedBox(height: 12),

            ElevatedButton.icon(
              onPressed: () {
                Navigator.push(context, MaterialPageRoute(builder: (_) => const UserHomeScreen()));
              },
              icon: const Icon(Icons.home),
              label: const Text("Retour à l'accueil"),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.blueAccent,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
              ),
            ),

            const SizedBox(height: 8),
            const Text(
              "Ce test est un outil de dépistage et ne remplace pas un diagnostic médical professionnel.",
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 12, color: Colors.grey),
            ),
          ],
        ),
      ),
    );
  }
}
