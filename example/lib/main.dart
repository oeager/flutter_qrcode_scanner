import 'dart:ui';

import 'package:Scanner/scanner.dart';
import 'package:flutter/material.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

final GlobalKey<NavigatorState> navigatorKey = new GlobalKey<NavigatorState>();

class _MyAppState extends State<MyApp> {
  String _text;

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      navigatorKey: navigatorKey,
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
          actions: [
            IconButton(
                icon: Icon(Icons.scanner),
                onPressed: () async {
                  ScanResult result = await navigatorKey.currentState
                      .push<ScanResult>(
                      MaterialPageRoute(builder: (context) => ScanPage()));
                  setState(() {
                    if (result?.isSuccess() ?? false) {
                      _text = result.result;
                    } else {
                      _text = "error:" + (result?.message ?? "no result");
                    }
                  });

                })
          ],
        ),
        body: Center(
          child: Text(_text ?? "No Result"),
        ),
      ),
    );
  }
}

class ScanPage extends StatelessWidget {
  ScannerController controller;
  bool isOn = false;
  bool qrCodeMode = true;

  @override
  Widget build(BuildContext context) {
    double w = window.physicalSize.width / window.devicePixelRatio;
    double h = window.physicalSize.height / window.devicePixelRatio;
    double square = w / 2;
    var topOffset = (h - square) * 0.4;
    double bottomOffset =
        (h - (topOffset + square)) / 2 / window.devicePixelRatio;
    double buttonOffset = (w - 80) / 2;
    return Scaffold(
      backgroundColor: Colors.transparent,
      body: Stack(
        children: [
          QRScannerView(
            onCreatedCallback: (controller) {
              this.controller = controller;
              controller.startScan();
            },
            onResultCallback: (result) {
              navigatorKey.currentState.pop(result);
            },
            borderColor: Colors.white,
            cornerColor: Colors.deepPurpleAccent,
            scanColor: Colors.deepOrangeAccent,
            scanBoxSize: square,
            scanBoxVerticalBias: 0.4,
            scanBoxHeightWhenBarCode: 120,
            defaultQRCodeMode: true,
          ),
          Positioned(
            child: Center(
              child: Container(
                decoration: BoxDecoration(
                    color: Colors.white.withAlpha(128),
                    borderRadius: BorderRadius.circular(8)),
                padding: EdgeInsets.all(8),
                child: Text(
                  "请将二维码/条形码对准框中",
                  style: TextStyle(color: Colors.white, fontSize: 12),
                ),
              ),
            ),
            top: topOffset - 40,
            left: 0,
            right: 0,
          ),
          AppBar(
            backgroundColor: Colors.transparent,
            automaticallyImplyLeading: true,
            elevation: 0,
            titleSpacing: 0,
            title: Text(
              "Scan",
              style: TextStyle(color: Colors.white),
            ),
            actions: [
              IconButton(
                  icon: Icon(Icons.switch_camera),
                  onPressed: () {
                    controller?.toggleQRMode(!qrCodeMode);
                    qrCodeMode = !qrCodeMode;
                  })
            ],
          ),
          Positioned(
            child: InkWell(
              child: Container(
                width: 80,
                height: 80,
                decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(40)),
                alignment: Alignment.center,
                child: Icon(Icons.flash_on),
              ),
              onTap: () {
                controller?.toggleFlashlight(!isOn);
                isOn = !isOn;
              },
            ),
            bottom: bottomOffset,
            left: buttonOffset,
            right: buttonOffset,
          ),
        ],
      ),
    );
  }
}
