import 'package:flutter/material.dart';
import 'package:flutter_qr_bar_scanner/scanner_camera.dart';
import 'package:flutter_qr_bar_scanner/torch_state_controller.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String? _qrInfo = 'Scan a QR/Bar code';
  bool _camState = false;
  late TorchStateController _torchStateController;

  _qrCallback(Iterable<ScanResult> rslt) {
    if (rslt.isNotEmpty) {
      setState(() {
        _camState = false;

        _qrInfo = rslt.first.content;
      });
    }
  }

  _scanCode() {
    setState(() {
      _camState = true;
    });
  }

  @override
  void initState() {
    super.initState();
    _torchStateController = TorchStateController(isOn: true);
    _scanCode();
  }

  @override
  void dispose() {
    _torchStateController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: Text('Flutter QR/Bar Code Reader'),
        ),
        body: Center(
          child: _camState
              ? SizedBox(
                  height: 1000,
                  width: 500,
                  child: GestureDetector(
                    onTap: () => _torchStateController.isOn =
                        !_torchStateController.isOn,
                    child: ScannerCamera(
                      torchController: _torchStateController,
                      onError: (context, error) => Text(
                        error.toString(),
                        style: TextStyle(color: Colors.red),
                      ),
                      qrCodeCallback: (code) {
                        _qrCallback(code);
                      },
                    ),
                  ),
                )
              : GestureDetector(
                  onTap: () => setState(() => _camState = true),
                  child: Text(_qrInfo!),
                ),
        ),
      ),
    );
  }
}
