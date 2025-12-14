import 'dart:io';
import 'package:flutter/material.dart';

class FatigueResultPage extends StatelessWidget {
  final double score;
  final String riskTitle;
  final String riskLabel;
  final String message;
  final String imagePath;

  final Map<String, dynamic> fatigueProfile;
  final List<dynamic> recommendations;

  const FatigueResultPage({
    super.key,
    required this.score,
    required this.riskTitle,
    required this.riskLabel,
    required this.message,
    required this.imagePath,
    required this.fatigueProfile,
    required this.recommendations,
  });

  Color _colorForRisk() {
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

  Widget _fatigueTile({
    required String title,
    required int value,
    required IconData icon,
    required Color color,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: color.withOpacity(0.12),
        borderRadius: BorderRadius.circular(16),
      ),
      padding: const EdgeInsets.all(14),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, size: 30, color: color),
          const SizedBox(height: 8),
          Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 6),
          Text(
            value.toString(),
            style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: color),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final mainColor = _colorForRisk();

    final int physical = (fatigueProfile["physical"] ?? 0) as int;
    final int mental = (fatigueProfile["mental"] ?? 0) as int;
    final int vigilance = (fatigueProfile["vigilance"] ?? 0) as int;

    return Scaffold(
      appBar: AppBar(title: const Text("Résultat Fatigue")),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // ✅ Square photo preview
            Center(
              child: ClipRRect(
                borderRadius: BorderRadius.circular(18),
                child: AspectRatio(
                  aspectRatio: 1,
                  child: Image.file(
                    File(imagePath),
                    fit: BoxFit.cover,
                  ),
                ),
              ),
            ),

            const SizedBox(height: 16),

            // main score card
            Card(
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
              elevation: 3,
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      "Résultats de l'analyse",
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
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
                                value: score / 100.0,
                                strokeWidth: 10,
                                backgroundColor: Colors.grey.shade200,
                                valueColor: AlwaysStoppedAnimation<Color>(mainColor),
                              ),
                            ),
                            Text(
                              score.toStringAsFixed(0),
                              style: const TextStyle(fontSize: 28, fontWeight: FontWeight.bold),
                            ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        Icon(Icons.warning_amber_rounded, color: mainColor, size: 28),
                        const SizedBox(width: 8),
                        Text(
                          riskTitle,
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                            color: mainColor,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Text(message),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 16),

            // ✅ tiles profile (carreaux en face)
            Text(
              "Profil de fatigue",
              style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),

            GridView.count(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              crossAxisCount: 3,
              crossAxisSpacing: 12,
              mainAxisSpacing: 12,
              children: [
                _fatigueTile(
                  title: "Physique",
                  value: physical,
                  icon: Icons.fitness_center,
                  color: Colors.blue,
                ),
                _fatigueTile(
                  title: "Mentale",
                  value: mental,
                  icon: Icons.psychology,
                  color: Colors.orange,
                ),
                _fatigueTile(
                  title: "Vigilance",
                  value: vigilance,
                  icon: Icons.visibility,
                  color: Colors.purple,
                ),
              ],
            ),

            const SizedBox(height: 16),

            Text(
              "Recommandations personnalisées",
              style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),

            if (recommendations.isEmpty)
              const Card(
                child: Padding(
                  padding: EdgeInsets.all(16),
                  child: Text("Aucune recommandation disponible."),
                ),
              ),

            ...recommendations.map((rec) {
              final String title = rec["title"]?.toString() ?? "";
              final String action = rec["action"]?.toString() ?? "";
              final String why = rec["why"]?.toString() ?? "";
              final String tag = rec["tag"]?.toString() ?? "";

              return Card(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                elevation: 2,
                child: ListTile(
                  leading: _iconForTag(tag),
                  title: Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
                  subtitle: Padding(
                    padding: const EdgeInsets.only(top: 6),
                    child: Text("$action\n\nPourquoi : $why"),
                  ),
                ),
              );
            }).toList(),

            const SizedBox(height: 12),

            OutlinedButton.icon(
              onPressed: () => Navigator.pop(context),
              icon: const Icon(Icons.replay),
              label: const Text("Refaire une photo"),
            ),
          ],
        ),
      ),
    );
  }
}
