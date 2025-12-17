  import 'dart:convert';
  import 'dart:io';

  import 'package:camera/camera.dart';
  import 'package:flutter/material.dart';
  import 'package:http/http.dart' as http;
  import 'package:http_parser/http_parser.dart';
  import 'package:mime/mime.dart';
  import 'package:path/path.dart' as p;

  import 'fatigue_result_page.dart';

  class FatigueCameraScreen extends StatefulWidget {
    const FatigueCameraScreen({super.key});

    @override
    State<FatigueCameraScreen> createState() => _FatigueCameraScreenState();
  }

  class _FatigueCameraScreenState extends State<FatigueCameraScreen>
      with WidgetsBindingObserver {
    CameraController? _controller;
    bool _loadingCamera = true;
    bool _sending = false;
    String? _error;

    // ✅ Endpoint fatigue (score par tranches + recos détaillées)
    final String apiUrl = "http://10.0.2.2:8000/fatigue/predict_personalized";

    bool _previewPaused = false;

    @override
    void initState() {
      super.initState();
      WidgetsBinding.instance.addObserver(this);
      _initCamera();
    }

    @override
    void dispose() {
      WidgetsBinding.instance.removeObserver(this);
      _controller?.dispose();
      super.dispose();
    }

    @override
    void didChangeAppLifecycleState(AppLifecycleState state) {
      final controller = _controller;
      if (controller == null || !controller.value.isInitialized) return;

      if (state == AppLifecycleState.inactive || state == AppLifecycleState.paused) {
        controller.pausePreview();
        _previewPaused = true;
      } else if (state == AppLifecycleState.resumed) {
        controller.resumePreview();
        _previewPaused = false;
      }
    }

    Future<void> _initCamera() async {
      try {
        final cameras = await availableCameras();
        final front = cameras.firstWhere(
              (c) => c.lensDirection == CameraLensDirection.front,
          orElse: () => cameras.first,
        );

        final controller = CameraController(
          front,
          ResolutionPreset.medium,
          enableAudio: false,
          imageFormatGroup: ImageFormatGroup.jpeg,
        );

        await controller.initialize();

        if (!mounted) return;
        setState(() {
          _controller = controller;
          _loadingCamera = false;
        });
      } catch (e) {
        setState(() {
          _error = "Camera error: $e";
          _loadingCamera = false;
        });
      }
    }

    Future<void> _captureAndSend() async {
      final controller = _controller;
      if (controller == null || !controller.value.isInitialized) return;

      setState(() {
        _sending = true;
        _error = null;
      });

      try {
        if (!_previewPaused) {
          await controller.pausePreview();
          _previewPaused = true;
        }

        final XFile photo = await controller.takePicture();
        final file = File(photo.path);

        if (!await file.exists() || await file.length() == 0) {
          throw Exception("Captured file is empty or missing.");
        }

        final request = http.MultipartRequest("POST", Uri.parse(apiUrl));

        // ✅ context est OPTIONNEL: l’API (version score-tranches) peut l’ignorer
        request.fields["context"] = jsonEncode({
          "role": "Infirmier",
          "department": "Urgences",
          "shift": "Nuit",
          "hours_slept": 5.5,
          "stress_level": 8,
          "had_breaks": false,
          "caffeine_cups": 3,
          "consecutive_shifts": 4,
        });

        final mimeType = lookupMimeType(file.path) ?? "image/jpeg";
        final parts = mimeType.split("/");
        final mediaType = MediaType(parts[0], parts.length > 1 ? parts[1] : "jpeg");

        request.files.add(
          await http.MultipartFile.fromPath(
            "file",
            file.path,
            filename: p.basename(file.path),
            contentType: mediaType,
          ),
        );

        request.headers["Accept"] = "application/json";

        final streamed = await request.send().timeout(const Duration(seconds: 30));
        final respStr = await streamed.stream.bytesToString();

        if (streamed.statusCode != 200) {
          throw Exception("Erreur API: ${streamed.statusCode}\n$respStr");
        }

        final data = jsonDecode(respStr) as Map<String, dynamic>;

        final String riskTitle = data["risk_title"]?.toString() ?? "Résultat";
        final String riskLabel = data["risk_label"]?.toString() ?? "Moyen";
        final String message = data["message"]?.toString() ?? "";

        final int score = (data["fatigue_score"] ?? 0) is int
            ? (data["fatigue_score"] ?? 0) as int
            : int.tryParse(data["fatigue_score"].toString()) ?? 0;

        final double confidence = (data["confidence"] is num)
            ? (data["confidence"] as num).toDouble()
            : double.tryParse(data["confidence"]?.toString() ?? "") ?? 0.0;

        final List<dynamic> recommendations =
        (data["personalized_recommendations"] is List)
            ? (data["personalized_recommendations"] as List)
            : <dynamic>[];


        if (!mounted) return;

        await Navigator.push(
          context,
          MaterialPageRoute(
            builder: (_) => FatigueResultPage(
              score: score.toDouble(),
              confidence: confidence,
              riskTitle: riskTitle,
              riskLabel: riskLabel,
              message: message,
              imagePath: file.path,
              recommendations: recommendations,
            ),
          ),
        );

        if (mounted) {
          if (_controller != null && _controller!.value.isInitialized) {
            await _controller!.resumePreview();
            _previewPaused = false;
          }
        }
      } catch (e) {
        setState(() => _error = "Erreur: $e");
        try {
          if (_controller != null && _controller!.value.isInitialized) {
            await _controller!.resumePreview();
            _previewPaused = false;
          }
        } catch (_) {}
      } finally {
        if (mounted) setState(() => _sending = false);
      }
    }

    @override
    Widget build(BuildContext context) {
      final controller = _controller;

      return Scaffold(
        appBar: AppBar(title: const Text("Détection Fatigue (Caméra)")),
        body: _loadingCamera
            ? const Center(child: CircularProgressIndicator())
            : _error != null
            ? Center(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Text(_error!, style: const TextStyle(color: Colors.red)),
          ),
        )
            : Column(
          children: [
            Expanded(
              child: Center(
                child: AspectRatio(
                  aspectRatio: 1,
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(18),
                    child: FittedBox(
                      fit: BoxFit.cover,
                      child: SizedBox(
                        width: controller!.value.previewSize!.height,
                        height: controller.value.previewSize!.width,
                        child: CameraPreview(controller),
                      ),
                    ),
                  ),
                ),
              ),
            ),
            if (_sending)
              const Padding(
                padding: EdgeInsets.all(12),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    ),
                    SizedBox(width: 10),
                    Text("Analyse en cours..."),
                  ],
                ),
              ),
            if (_error != null)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: Text(_error!, style: const TextStyle(color: Colors.red)),
              ),
            Padding(
              padding: const EdgeInsets.all(16),
              child: SizedBox(
                width: double.infinity,
                height: 52,
                child: ElevatedButton.icon(
                  onPressed: _sending ? null : _captureAndSend,
                  icon: const Icon(Icons.camera_alt),
                  label: const Text("Prendre une photo & analyser"),
                ),
              ),
            ),
          ],
        ),
      );
    }
  }
