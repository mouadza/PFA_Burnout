import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

import 'result_page.dart';

class QuestionnaireScreen extends StatefulWidget {
  const QuestionnaireScreen({super.key});

  @override
  State<QuestionnaireScreen> createState() => _QuestionnaireScreenState();
}

class _QuestionnaireScreenState extends State<QuestionnaireScreen> {
  final List<String> questions = [
    "Je me suis senti mentalement épuisé durant ce service.",
    "Je me suis senti physiquement fatigué à cause de la charge de travail.",
    "J’ai eu du mal à gérer les situations stressantes ou critiques.",
    "Je me suis senti débordé par le nombre de patients ou de tâches.",
    "J’ai eu des difficultés à me concentrer ou à penser clairement.",
    "J’ai commis ou failli commettre une erreur à cause de la fatigue.",
    "Le rythme de travail était trop rapide pour être géré sereinement.",
    "Je n’ai pas eu assez de temps pour accomplir correctement mes tâches.",
    "Je me suis senti émotionnellement détaché des patients.",
    "Je me suis senti moins motivé ou impliqué que d’habitude.",
    "Je me suis senti frustré ou insatisfait de ma performance.",
    "Ce service a eu un impact négatif sur mon bien-être général.",
  ];

  final Map<int, String> likertLabels = const {
    0: "Jamais",
    1: "Rarement",
    2: "Parfois",
    3: "Souvent",
    4: "Toujours",
  };

  int currentIndex = 0;
  late List<int?> answers;
  bool isLoading = false;

  // ✅ endpoint FastAPI burnout personnalisé (par score)
  final String apiUrl = "http://10.0.2.2:8000/predict_personalized";

  @override
  void initState() {
    super.initState();
    answers = List<int?>.filled(questions.length, null);
  }

  Future<void> _submit() async {
    print("DEBUG answers = $answers");

    if (answers.any((a) => a == null)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Merci de répondre à toutes les questions")),
      );
      return;
    }

    setState(() => isLoading = true);

    try {
      final response = await http
          .post(
        Uri.parse(apiUrl),
        headers: {
          "Content-Type": "application/json",
          "Accept": "application/json",
        },
        body: jsonEncode({
          "answers": answers.map((e) => e ?? 0).toList(),

          // ✅ optionnel : tu peux l’enlever côté backend si tu veux.
          // si ton backend ignore "context", ça ne pose aucun souci.
          "context": {
            "role": "Infirmier",
            "department": "Urgences",
            "shift": "Nuit",
            "hours_slept": 5.5,
            "stress_level": 8,
            "had_breaks": false,
            "caffeine_cups": 3,
            "consecutive_shifts": 4,
          }
        }),
      )
          .timeout(const Duration(seconds: 10));

      print("DEBUG status = ${response.statusCode}");
      print("DEBUG body   = ${response.body}");

      if (response.statusCode != 200) {
        throw Exception("Erreur API: ${response.statusCode} ${response.body}");
      }

      final data = jsonDecode(response.body) as Map<String, dynamic>;

      final String riskTitle = data["risk_title"]?.toString() ?? "Résultat";
      final String riskLabel = data["risk_label"]?.toString() ?? "Moyen";
      final String message = data["message"]?.toString() ?? "";

      // ✅ score burnout
      final int score = (data["burnout_score"] ?? 0) is int
          ? (data["burnout_score"] ?? 0) as int
          : int.tryParse(data["burnout_score"].toString()) ?? 0;

      // ✅ liste de recommandations détaillées (plan)
      final List<dynamic> recommendations =
      (data["personalized_recommendations"] ?? []) as List<dynamic>;

      // ✅ confiance (optionnelle)
      final double confidence = (data["confidence"] is num)
          ? (data["confidence"] as num).toDouble()
          : double.tryParse(data["confidence"]?.toString() ?? "") ?? 0.0;

      if (!mounted) return;
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => ResultPage(
            score: score.toDouble(),
            riskTitle: riskTitle,
            riskLabel: riskLabel,
            message: message,
            confidence: confidence,
            recommendations: recommendations,
            answers: answers.map((e) => e ?? 0).toList(),
          ),
        ),
      );
    } catch (e) {
      if (!mounted) return;
      print("❌ Erreur HTTP : $e");
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text("Erreur de connexion : $e")),
      );
    } finally {
      if (mounted) setState(() => isLoading = false);
    }
  }

  void _next() {
    // ✅ Vérifier que la question actuelle est répondue
    if (answers[currentIndex] == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Merci de choisir une réponse avant de continuer")),
      );
      return;
    }

    if (currentIndex < questions.length - 1) {
      setState(() => currentIndex++);
    } else {
      _submit();
    }
  }


  void _previous() {
    if (currentIndex > 0) {
      setState(() => currentIndex--);
    }
  }

  @override
  Widget build(BuildContext context) {
    final int qNumber = currentIndex + 1;
    final double progress = qNumber / questions.length;

    return Scaffold(
      appBar: AppBar(title: const Text("Questionnaire Burnout")),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text("Question $qNumber sur ${questions.length}",
                style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 4),
            LinearProgressIndicator(value: progress),
            const SizedBox(height: 24),

            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              decoration: BoxDecoration(
                color: Colors.blue.withOpacity(0.1),
                borderRadius: BorderRadius.circular(20),
              ),
              child: const Text("Stress"),
            ),
            const SizedBox(height: 16),

            Text(
              questions[currentIndex],
              style: Theme.of(context)
                  .textTheme
                  .titleMedium
                  ?.copyWith(fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 16),

            Expanded(
              child: ListView(
                children: likertLabels.entries.map((entry) {
                  final value = entry.key;
                  final label = entry.value;
                  final selected = answers[currentIndex] == value;

                  return Card(
                    margin: const EdgeInsets.symmetric(vertical: 4),
                    child: ListTile(
                      title: Text(label),
                      trailing: selected
                          ? const Icon(Icons.radio_button_checked, color: Colors.blue)
                          : const Icon(Icons.radio_button_off),
                      onTap: () {
                        setState(() => answers[currentIndex] = value);
                      },
                    ),
                  );
                }).toList(),
              ),
            ),

            const SizedBox(height: 8),

            Row(
              children: [
                TextButton(
                  onPressed: currentIndex == 0 || isLoading ? null : _previous,
                  child: const Text("Précédent"),
                ),
                const Spacer(),
                ElevatedButton(
                  onPressed: isLoading ? null : (answers[currentIndex] == null ? null : _next),
                  child: isLoading
                      ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                      : Text(currentIndex == questions.length - 1 ? "Envoyer" : "Suivant"),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
