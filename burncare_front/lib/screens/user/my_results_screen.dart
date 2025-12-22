import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:provider/provider.dart';

import '../../../providers/auth_provider.dart';
import '../../../utils/api_config.dart';

class MyResultsScreen extends StatefulWidget {
  const MyResultsScreen({super.key});

  @override
  State<MyResultsScreen> createState() => _MyResultsScreenState();
}

class _MyResultsScreenState extends State<MyResultsScreen>
    with SingleTickerProviderStateMixin {
  late final TabController _tabController;

  bool _loadingBurnout = true;
  bool _loadingFatigue = true;

  String? _errorBurnout;
  String? _errorFatigue;

  List<BurnoutResultItem> _burnoutResults = [];
  List<FatigueResultItem> _fatigueResults = [];

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _fetchBurnoutResults();
    _fetchFatigueResults();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
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

  IconData _iconForType(ResultType type) {
    return type == ResultType.burnout ? Icons.assignment_turned_in : Icons.camera_alt;
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

  Future<String?> _getToken() async {
    final auth = context.read<AuthProvider>();
    return auth.token;
  }

  Future<void> _fetchBurnoutResults() async {
    setState(() {
      _loadingBurnout = true;
      _errorBurnout = null;
    });

    try {
      final token = await _getToken();
      if (token == null) {
        setState(() {
          _loadingBurnout = false;
          _errorBurnout = "Utilisateur non authentifié.";
        });
        return;
      }

      final url = "${getSpringApiBaseUrl()}/api/burnout-results/me";
      final response = await http.get(
        Uri.parse(url),
        headers: {"Authorization": "Bearer $token"},
      );

      if (response.statusCode == 200) {
        final List<dynamic> list = jsonDecode(response.body);
        final items = list
            .map((e) => BurnoutResultItem.fromJson(e as Map<String, dynamic>))
            .toList();

        setState(() {
          _burnoutResults = items;
          _loadingBurnout = false;
        });
      } else {
        setState(() {
          _loadingBurnout = false;
          _errorBurnout = "Erreur API burnout : ${response.statusCode}";
        });
      }
    } catch (e) {
      setState(() {
        _loadingBurnout = false;
        _errorBurnout = "Erreur de connexion burnout : $e";
      });
    }
  }

  Future<void> _fetchFatigueResults() async {
    setState(() {
      _loadingFatigue = true;
      _errorFatigue = null;
    });

    try {
      final token = await _getToken();
      if (token == null) {
        setState(() {
          _loadingFatigue = false;
          _errorFatigue = "Utilisateur non authentifié.";
        });
        return;
      }

      final url = "${getSpringApiBaseUrl()}/api/fatigue-results/me";
      final response = await http.get(
        Uri.parse(url),
        headers: {"Authorization": "Bearer $token"},
      );

      if (response.statusCode == 200) {
        final List<dynamic> list = jsonDecode(response.body);
        final items = list
            .map((e) => FatigueResultItem.fromJson(e as Map<String, dynamic>))
            .toList();

        setState(() {
          _fatigueResults = items;
          _loadingFatigue = false;
        });
      } else {
        setState(() {
          _loadingFatigue = false;
          _errorFatigue = "Erreur API fatigue : ${response.statusCode}";
        });
      }
    } catch (e) {
      setState(() {
        _loadingFatigue = false;
        _errorFatigue = "Erreur de connexion fatigue : $e";
      });
    }
  }

  void _openDetails({
    required ResultType type,
    required String title,
    required String riskLabel,
    required int score,
    required DateTime createdAt,
    required String? message,
    String? recommendationText,
    double? confidence,
  }) {
    final color = _colorForRisk(riskLabel);

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(22)),
      ),
      builder: (_) {
        return DraggableScrollableSheet(
          expand: false,
          initialChildSize: 0.75,
          minChildSize: 0.35,
          maxChildSize: 0.95,
          builder: (context, scroll) {
            return Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
              child: ListView(
                controller: scroll,
                children: [
                  Row(
                    children: [
                      CircleAvatar(
                        backgroundColor: color.withOpacity(0.12),
                        child: Icon(_iconForType(type), color: color),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          title,
                          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),

                  Row(
                    children: [
                      _pill("Score", "$score", color),
                      const SizedBox(width: 8),
                      _pill("Risque", riskLabel, color),
                      const SizedBox(width: 8),
                      _pill("Date", _formatDate(createdAt), Colors.grey),
                    ],
                  ),

                  if (confidence != null) ...[
                    const SizedBox(height: 10),
                    Text(
                      "Confiance modèle : ${(confidence * 100).toStringAsFixed(0)}%",
                      style: TextStyle(color: Colors.grey.shade700),
                    ),
                  ],

                  if (message != null && message.trim().isNotEmpty) ...[
                    const SizedBox(height: 14),
                    const Text("Message", style: TextStyle(fontWeight: FontWeight.bold)),
                    const SizedBox(height: 6),
                    Text(message),
                  ],

                  if (recommendationText != null && recommendationText.trim().isNotEmpty) ...[
                    const SizedBox(height: 14),
                    const Text("Recommandations", style: TextStyle(fontWeight: FontWeight.bold)),
                    const SizedBox(height: 6),
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.grey.withOpacity(0.08),
                        borderRadius: BorderRadius.circular(14),
                      ),
                      child: Text(recommendationText),
                    ),
                  ],

                  const SizedBox(height: 18),
                  ElevatedButton.icon(
                    onPressed: () => Navigator.pop(context),
                    icon: const Icon(Icons.close),
                    label: const Text("Fermer"),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  Widget _pill(String label, String value, Color color) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
        decoration: BoxDecoration(
          color: color.withOpacity(0.10),
          borderRadius: BorderRadius.circular(14),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(label, style: TextStyle(fontSize: 11, color: Colors.grey.shade700)),
            const SizedBox(height: 4),
            Text(
              value,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(fontWeight: FontWeight.bold, color: color),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeaderSummary({
    required String title,
    required String subtitle,
    required IconData icon,
  }) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Theme.of(context).cardColor,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: Colors.grey.withOpacity(0.15)),
      ),
      child: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              color: Colors.blue.withOpacity(0.10),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Icon(icon),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                const SizedBox(height: 2),
                Text(subtitle, style: TextStyle(color: Colors.grey.shade700)),
              ],
            ),
          )
        ],
      ),
    );
  }

  Widget _buildEmpty(String text, VoidCallback onRetry) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 22),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.inbox_outlined, size: 42),
            const SizedBox(height: 12),
            Text(text, textAlign: TextAlign.center),
            const SizedBox(height: 12),
            ElevatedButton(onPressed: onRetry, child: const Text("Rafraîchir")),
          ],
        ),
      ),
    );
  }

  Widget _buildError(String text, VoidCallback onRetry) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 22),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(text, textAlign: TextAlign.center, style: const TextStyle(color: Colors.red)),
            const SizedBox(height: 12),
            ElevatedButton(onPressed: onRetry, child: const Text("Réessayer")),
          ],
        ),
      ),
    );
  }

  Widget _resultCard({
    required ResultType type,
    required String riskTitle,
    required String riskLabel,
    required int score,
    required DateTime createdAt,
    required String? message,
    String? recommendationText,
    double? confidence,
  }) {
    final color = _colorForRisk(riskLabel);

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Score bubble
            Container(
              width: 54,
              height: 54,
              decoration: BoxDecoration(
                color: color.withOpacity(0.10),
                shape: BoxShape.circle,
              ),
              child: Center(
                child: Text(
                  "$score",
                  style: TextStyle(fontWeight: FontWeight.bold, color: color, fontSize: 16),
                ),
              ),
            ),
            const SizedBox(width: 12),

            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // title + badge
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          riskTitle,
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 16,
                            color: color,
                          ),
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                        decoration: BoxDecoration(
                          color: color.withOpacity(0.12),
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: Text(
                          riskLabel,
                          style: TextStyle(color: color, fontSize: 12, fontWeight: FontWeight.w600),
                        ),
                      ),
                    ],
                  ),

                  const SizedBox(height: 6),

                  // date + type
                  Row(
                    children: [
                      Icon(_iconForType(type), size: 16, color: Colors.grey.shade700),
                      const SizedBox(width: 6),
                      Text(
                        _formatDate(createdAt),
                        style: TextStyle(fontSize: 12, color: Colors.grey.shade700),
                      ),
                      if (confidence != null) ...[
                        const SizedBox(width: 10),
                        Text(
                          "• ${(confidence * 100).toStringAsFixed(0)}%",
                          style: TextStyle(fontSize: 12, color: Colors.grey.shade700),
                        ),
                      ]
                    ],
                  ),

                  if (message != null && message.trim().isNotEmpty) ...[
                    const SizedBox(height: 10),
                    Text(
                      message,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontSize: 13),
                    ),
                  ],

                  const SizedBox(height: 10),

                  Align(
                    alignment: Alignment.centerRight,
                    child: TextButton.icon(
                      onPressed: () {
                        _openDetails(
                          type: type,
                          title: type == ResultType.burnout ? "Détails Burnout" : "Détails Fatigue",
                          riskLabel: riskLabel,
                          score: score,
                          createdAt: createdAt,
                          message: message,
                          recommendationText: recommendationText,
                          confidence: confidence,
                        );
                      },
                      icon: const Icon(Icons.visibility),
                      label: const Text("Voir détails"),
                    ),
                  )
                ],
              ),
            )
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final burnoutCount = _burnoutResults.length;
    final fatigueCount = _fatigueResults.length;

    return Scaffold(
      appBar: AppBar(
        title: const Text("Mes résultats"),
        bottom: TabBar(
          controller: _tabController,
          tabs: [
            Tab(text: "Burnout ($burnoutCount)", icon: Icon(_iconForType(ResultType.burnout))),
            Tab(text: "Fatigue ($fatigueCount)", icon: Icon(_iconForType(ResultType.fatigue))),
          ],
        ),
        actions: [
          IconButton(
            onPressed: () {
              _fetchBurnoutResults();
              _fetchFatigueResults();
            },
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          // ================== Burnout ==================
          Padding(
            padding: const EdgeInsets.all(16),
            child: _loadingBurnout
                ? const Center(child: CircularProgressIndicator())
                : _errorBurnout != null
                ? _buildError(_errorBurnout!, _fetchBurnoutResults)
                : _burnoutResults.isEmpty
                ? _buildEmpty("Aucun résultat burnout enregistré.", _fetchBurnoutResults)
                : Column(
              children: [
                _buildHeaderSummary(
                  title: "Burnout - Questionnaire",
                  subtitle: "Historique de vos évaluations burnout",
                  icon: Icons.assignment_turned_in,
                ),
                const SizedBox(height: 12),
                Expanded(
                  child: ListView.builder(
                    itemCount: _burnoutResults.length,
                    itemBuilder: (context, index) {
                      final item = _burnoutResults[index];
                      return _resultCard(
                        type: ResultType.burnout,
                        riskTitle: item.riskTitle,
                        riskLabel: item.riskLabel,
                        score: item.burnoutScore,
                        createdAt: item.createdAt,
                        message: item.message,
                        recommendationText: item.recommendation, // si ton API le renvoie
                      );
                    },
                  ),
                ),
              ],
            ),
          ),

          // ================== Fatigue ==================
          Padding(
            padding: const EdgeInsets.all(16),
            child: _loadingFatigue
                ? const Center(child: CircularProgressIndicator())
                : _errorFatigue != null
                ? _buildError(_errorFatigue!, _fetchFatigueResults)
                : _fatigueResults.isEmpty
                ? _buildEmpty("Aucun résultat fatigue enregistré.", _fetchFatigueResults)
                : Column(
              children: [
                _buildHeaderSummary(
                  title: "Fatigue - Caméra",
                  subtitle: "Historique des analyses fatigue par image",
                  icon: Icons.camera_alt,
                ),
                const SizedBox(height: 12),
                Expanded(
                  child: ListView.builder(
                    itemCount: _fatigueResults.length,
                    itemBuilder: (context, index) {
                      final item = _fatigueResults[index];
                      return _resultCard(
                        type: ResultType.fatigue,
                        riskTitle: item.riskTitle,
                        riskLabel: item.riskLabel,
                        score: item.fatigueScore,
                        createdAt: item.createdAt,
                        message: item.message,
                        recommendationText: item.recommendationText,
                        confidence: item.confidence,
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

enum ResultType { burnout, fatigue }

// ======================= MODELS =======================

class BurnoutResultItem {
  final int id;
  final int burnoutScore;
  final String riskLabel;
  final String riskTitle;
  final DateTime createdAt;
  final String? message;

  // ✅ optionnel si tu l’ajoutes dans BurnoutResultResponse
  final String? recommendation;

  BurnoutResultItem({
    required this.id,
    required this.burnoutScore,
    required this.riskLabel,
    required this.riskTitle,
    required this.createdAt,
    this.message,
    this.recommendation,
  });

  factory BurnoutResultItem.fromJson(Map<String, dynamic> json) {
    return BurnoutResultItem(
      id: (json['id'] as num).toInt(),
      burnoutScore: (json['burnoutScore'] as num).toInt(),
      riskLabel: json['riskLabel']?.toString() ?? "Moyen",
      riskTitle: json['riskTitle']?.toString() ?? "Risque",
      createdAt: DateTime.parse(json['createdAt'] as String),
      message: json['message']?.toString(),
      recommendation: json['recommendation']?.toString(),
    );
  }
}

class FatigueResultItem {
  final int id;
  final int fatigueScore;
  final String riskLabel;
  final String riskTitle;
  final DateTime createdAt;
  final String? message;
  final double? confidence;

  final String? recommendationText;

  FatigueResultItem({
    required this.id,
    required this.fatigueScore,
    required this.riskLabel,
    required this.riskTitle,
    required this.createdAt,
    this.message,
    this.confidence,
    this.recommendationText,
  });

  factory FatigueResultItem.fromJson(Map<String, dynamic> json) {
    double? conf;
    final rawConf = json['confidence'];
    if (rawConf is num) conf = rawConf.toDouble();
    if (rawConf is String) conf = double.tryParse(rawConf);

    return FatigueResultItem(
      id: (json['id'] as num).toInt(),
      fatigueScore: (json['fatigueScore'] as num).toInt(),
      riskLabel: json['riskLabel']?.toString() ?? "Moyen",
      riskTitle: json['riskTitle']?.toString() ?? "Risque",
      createdAt: DateTime.parse(json['createdAt'] as String),
      message: json['message']?.toString(),
      confidence: conf,
      recommendationText: json['recommendationText']?.toString(),
    );
  }
}
