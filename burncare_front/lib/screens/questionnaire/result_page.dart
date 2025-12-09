import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:provider/provider.dart';

import '../../providers/auth_provider.dart'; // 🔁 adapte le chemin

class ResultPage extends StatefulWidget {
  final double score;            // 0–100
  final String riskTitle;        // "Risque Modéré"
  final String riskLabel;        // "Faible / Moyen / Élevé"
  final String message;          // texte explicatif
  final String recommendation;   // texte de reco
  final List<int> answers;       // ✅ on ajoute les réponses brutes

  const ResultPage({
    super.key,
    required this.score,
    required this.riskTitle,
    required this.riskLabel,
    required this.message,
    required this.recommendation,
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

  @override
  void initState() {
    super.initState();
    // 🔁 on déclenche la sauvegarde dès l'affichage de la page
    Future.microtask(_saveResult);
  }

  Future<void> _saveResult() async {
    try {
      final auth = context.read<AuthProvider>();
      final String? token = auth.token; // ⚠️ adapte selon ton AuthProvider

      if (token == null) {
        print("⚠️ Aucun token disponible, résultat non sauvegardé.");
        return;
      }

      setState(() {
        _saving = true;
        _saveError = null;
      });

      const String springApiUrl = "http://10.0.2.2:8080/api/burnout-results";

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
          "recommendation": widget.recommendation,
          "answers": widget.answers, // ✅ on envoie le tableau de réponses
        }),
      );

      print("DEBUG save status = ${response.statusCode}");
      print("DEBUG save body   = ${response.body}");

      if (response.statusCode < 200 || response.statusCode >= 300) {
        setState(() {
          _saveError = "Erreur lors de l'enregistrement du résultat.";
        });
      }
    } catch (e) {
      print("❌ Exception pendant la sauvegarde: $e");
      if (mounted) {
        setState(() {
          _saveError = "Erreur de connexion lors de l'enregistrement.";
        });
      }
    } finally {
      if (mounted) {
        setState(() => _saving = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final Color mainColor = _colorForRisk();

    return Scaffold(
      appBar: AppBar(
        title: const Text("Résultats de l'évaluation"),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // 🔔 petit bandeau discrêt sur la sauvegarde
            if (_saving)
              const Padding(
                padding: EdgeInsets.only(bottom: 8.0),
                child: Row(
                  children: [
                    SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    ),
                    SizedBox(width: 8),
                    Text(
                      "Enregistrement de votre résultat...",
                      style: TextStyle(fontSize: 12),
                    ),
                  ],
                ),
              ),
            if (_saveError != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 8.0),
                child: Text(
                  _saveError!,
                  style: const TextStyle(color: Colors.red, fontSize: 12),
                ),
              ),

            // Carte score + cercle
            Card(
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(20),
              ),
              elevation: 3,
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      "Résultats de l'évaluation",
                      style: Theme.of(context)
                          .textTheme
                          .titleMedium
                          ?.copyWith(fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      "Analyse de votre risque de burnout",
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
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
                                valueColor:
                                AlwaysStoppedAnimation<Color>(mainColor),
                              ),
                            ),
                            Text(
                              widget.score.toStringAsFixed(0),
                              style: const TextStyle(
                                fontSize: 28,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        Icon(Icons.warning_amber_rounded,
                            color: mainColor, size: 28),
                        const SizedBox(width: 8),
                        Text(
                          widget.riskTitle,
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                            color: mainColor,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Text(
                      widget.message,
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),

            // Carte recommandations
            Card(
              color: Colors.blueAccent,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(20),
              ),
              elevation: 3,
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: const [
                        Icon(Icons.lightbulb_outline,
                            color: Colors.white, size: 24),
                        SizedBox(width: 8),
                        Text(
                          "Recommandations",
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Text(
                      widget.recommendation,
                      style: const TextStyle(color: Colors.white),
                    ),
                    const SizedBox(height: 12),
                    ElevatedButton(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.white,
                        foregroundColor: Colors.blueAccent,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(30),
                        ),
                      ),
                      onPressed: () {
                        // plus tard : afficher une page de conseils détaillés
                      },
                      child: const Text("Voir les conseils personnalisés"),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 12),

            // Bouton refaire
            OutlinedButton.icon(
              onPressed: () {
                Navigator.popUntil(context, (route) => route.isFirst);
              },
              icon: const Icon(Icons.replay),
              label: const Text("Refaire le test"),
            ),
            const SizedBox(height: 8),
            const Text(
              "Ce test est un outil de dépistage et ne remplace pas un diagnostic médical professionnel.",
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 11, color: Colors.grey),
            ),
          ],
        ),
      ),
    );
  }
}
