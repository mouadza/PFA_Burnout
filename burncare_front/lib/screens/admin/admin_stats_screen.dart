import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:provider/provider.dart';

import '../../providers/auth_provider.dart';


class AdminStatsScreen extends StatefulWidget {

  const AdminStatsScreen({super.key});

  @override
  State<AdminStatsScreen> createState() => _AdminStatsScreenState();
}

class _AdminStatsScreenState extends State<AdminStatsScreen> {
  bool loading = true;
  Map<String, dynamic>? data;

  @override
  void initState() {
    super.initState();
    fetchStats();
  }

  Future<void> fetchStats() async {
    final token = context.read<AuthProvider>().token;
    final res = await http.get(
      Uri.parse("http://10.0.2.2:8080/api/admin/stats"),
      headers: {"Authorization": "Bearer $token"},
    );

    setState(() {
      data = jsonDecode(res.body);
      loading = false;
    });
  }

  Widget statCard(String title, String value, Color color) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        children: [
          Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Text(value, style: TextStyle(fontSize: 22, color: color)),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (loading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    return Scaffold(
      appBar: AppBar(title: const Text("Statistiques globales")),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: GridView.count(
          crossAxisCount: 2,
          crossAxisSpacing: 12,
          mainAxisSpacing: 12,
          children: [
            statCard("Utilisateurs", data!["totalUsers"].toString(), Colors.indigo),

            statCard("Burnout total", data!["burnoutTotal"].toString(), Colors.red),
            statCard("Burnout élevé", data!["burnoutHigh"].toString(), Colors.redAccent),

            statCard("Fatigue total", data!["fatigueTotal"].toString(), Colors.orange),
            statCard("Fatigue élevée", data!["fatigueTired"].toString(), Colors.deepOrange),

            statCard(
              "Fatigue moyenne",
              data!["avgFatigueScore"].toStringAsFixed(1),
              Colors.purple,
            ),
          ],
        ),
      ),
    );
  }
}
