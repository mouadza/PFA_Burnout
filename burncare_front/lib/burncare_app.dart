import 'package:flutter/material.dart';
import 'screens/auth/login_screen.dart';

class BurnCareApp extends StatelessWidget {
  const BurnCareApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'BurnCare App',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const LoginScreen(), // OR HomeScreen()
    );
  }
}
