import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'src/ios-decoration.dart';

class ScanResult {
  static const status_recognize_failed = 0;
  static const status_success = 1;
  static const status_file_not_exist = 2;
  static const status_platform_exception = 3;
  static const status_permission_denied = 4;
  final int status;
  final String result;
  final String message;

  const ScanResult({@required this.status, this.result, this.message});

  bool isSuccess() {
    return status == status_success;
  }
}

class Scanner {
  static const MethodChannel _channel = const MethodChannel('com.qr.scanner');

  static Future<ScanResult> fromImage(File file) async {
    if (file?.existsSync() == false) {
      return ScanResult(
          status: ScanResult.status_file_not_exist, message: "file not exist");
    }
    try {
      final map =
          await _channel.invokeMethod<Map>("fromImage", {"file": file.path});
      int status = map['status'] as int ?? 0;
      String message = map['message'];
      String result = map['result'];
      return ScanResult(status: status, message: message, result: result);
    } catch (e) {
      return ScanResult(
          status: ScanResult.status_platform_exception, message: e.toString());
    }
  }
}

class ScannerController {
  final MethodChannel _channel;
  final ValueChanged<ScanResult> onResultCallback;

  ScannerController(int id, this.onResultCallback)
      : _channel = MethodChannel('com.qr.scanner.view_$id') {
    _channel.setMethodCallHandler(_handleMessages);
  }

  Future _handleMessages(MethodCall call) async {
    switch (call.method) {
      case "onResult":
        String result = call.arguments["text"];
        int status = call.arguments['status'];
        String message = call.arguments['message'];
        onResultCallback?.call(
            ScanResult(status: status, result: result, message: message));
        return result;
    }
    return false;
  }

  Future<bool> startPreView() {
    return _channel.invokeMethod<bool>("startPreView");
  }

  Future<bool> stopPreView() {
    return _channel.invokeMethod<bool>("stopPreView");
  }

  Future<bool> startScan() {
    return _channel.invokeMethod<bool>("startScan");
  }

  Future<bool> stopScan() {
    return _channel.invokeMethod<bool>("stopScan");
  }

  Future<bool> toggleFlashlight(bool on) {
    final params = {"status": on};
    return _channel.invokeMethod<bool>("toggleFlashlight", params);
  }

  Future<bool> toggleQRMode(bool on) {
    final params = {"status": on};
    return _channel.invokeMethod<bool>("toggleQRMode", params);
  }
}

class QRScannerView extends StatefulWidget {
  final ValueChanged<ScanResult> onResultCallback;
  final Function(ScannerController controller) onCreatedCallback;
  final Color borderColor;
  final Color cornerColor;
  final Color scanColor;
  final double scanBoxSize;
  final double scanBoxHeightWhenBarCode;
  final double scanBoxVerticalBias;
  final bool defaultQRCodeMode;

  const QRScannerView(
      {Key key,
      this.borderColor: Colors.white,
      this.cornerColor: Colors.orange,
      this.scanColor: Colors.deepOrangeAccent,
      this.scanBoxSize,
      this.scanBoxHeightWhenBarCode,
      this.scanBoxVerticalBias = 0.4,
      this.defaultQRCodeMode: true,
      @required this.onResultCallback,
      this.onCreatedCallback})
      : assert(defaultQRCodeMode != null),
        assert(scanBoxVerticalBias != null),
        assert(onResultCallback != null),
        assert(borderColor != null),
        assert(scanColor != null),
        assert(cornerColor != null);

  @override
  _QRScannerViewState createState() => _QRScannerViewState();
}

class _QRScannerViewState extends State<QRScannerView> {
  @override
  Widget build(BuildContext context) {
    if (Platform.isAndroid) {
      final params = <String, dynamic>{
        "defaultQRCodeMode": widget.defaultQRCodeMode,
        "scanBoxVerticalBias": widget.scanBoxVerticalBias,
        "borderColor": widget.borderColor.value,
        "cornerColor": widget.cornerColor.value,
        "scanColor": widget.scanColor.value
      };
      if (widget.borderColor != null) {
        params['borderColor'] = widget.borderColor.value;
      }
      if (widget.cornerColor != null) {
        params['cornerColor'] = widget.cornerColor.value;
      }
      if (widget.scanColor != null) {
        params['scanColor'] = widget.scanColor.value;
      }
      if (widget.scanBoxSize != null) {
        params['scanBoxSize'] = widget.scanBoxSize;
      }
      if (widget.scanBoxHeightWhenBarCode != null) {
        params['scanBoxHeightWhenBarCode'] = widget.scanBoxHeightWhenBarCode;
      }
      return AndroidView(
        viewType: "com.qr.scanner.view",
        creationParams: params,
        creationParamsCodec: const StandardMessageCodec(),
        onPlatformViewCreated: _onPlatformViewCreated,
      );
    } else if (Platform.isIOS) {
      return CupertioScanView(
        onResultCallback: widget.onResultCallback,
        borderColor: widget.borderColor,
        cornerColor: widget.cornerColor,
        onCreatedCallback: widget.onCreatedCallback,
        scanColor: widget.scanColor,
        scanBoxSize: widget.scanBoxSize,
        scanBoxHeightWhenBarCode: widget.scanBoxHeightWhenBarCode,
        scanBoxVerticalBias: widget.scanBoxVerticalBias,
        defaultQRCodeMode: widget.defaultQRCodeMode,
      );
    } else {
      throw Exception("not supported");
    }
  }

  void _onPlatformViewCreated(int id) {
    widget.onCreatedCallback
        ?.call(ScannerController(id, widget.onResultCallback));
  }
}
