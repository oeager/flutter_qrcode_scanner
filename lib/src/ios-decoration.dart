import 'dart:async';
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import '../scanner.dart';

class CuperScanController extends ScannerController {
  final ValueNotifier<bool> modeNotifier;

  CuperScanController(this.modeNotifier, int id, onResultCallback)
      : super(id, onResultCallback);

  @override
  Future<bool> toggleQRMode(bool on) {
    modeNotifier.value = on;
    return Future.value(true);
  }
}

class QrScanBoxPainter extends CustomPainter {
  final double animationValue;
  final bool isForward;
  final Color borderColor;
  final Color cornerColor;
  final Color scanColor;
  final path = new Path();

  QrScanBoxPainter(
      {@required this.animationValue,
      @required this.isForward,
      this.cornerColor,
      this.scanColor,
      this.borderColor})
      : assert(animationValue != null),
        assert(isForward != null);

  @override
  void paint(Canvas canvas, Size size) {
    final borderRadius =
        RRect.fromLTRBR(0, 0, size.width, size.height, Radius.zero);
    canvas.drawRRect(
      borderRadius,
      Paint()
        ..color = borderColor
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1,
    );
    final borderPaint = Paint()
      ..color = cornerColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2;

    path.reset();
    // leftTop
    path.moveTo(0, 20);
    path.lineTo(0, 0);
    path.lineTo(20, 0);
    // rightTop
    path.moveTo(size.width - 20, 0);
    path.lineTo(size.width, 0);
    path.lineTo(size.width, 20);
    // rightBottom
    path.moveTo(size.width, size.height - 20);
    path.lineTo(size.width, size.height);
    path.lineTo(size.width - 20, size.height);
    // leftBottom
    path.moveTo(20, size.height);
    path.lineTo(0, size.height);
    path.lineTo(0, size.height - 20);
    canvas.drawPath(path, borderPaint);
    canvas.clipRRect(borderRadius);
    // 绘制横向网格
    final linePaint = Paint();
    final lineSize = size.height * 0.45;
    final leftPress = (size.height + lineSize) * animationValue - lineSize;
    linePaint.style = PaintingStyle.stroke;
    linePaint.shader = LinearGradient(
      colors: [Colors.transparent, scanColor],
      begin: isForward ? Alignment.topCenter : Alignment(0.0, 2.0),
      end: isForward ? Alignment(0.0, 0.5) : Alignment.topCenter,
    ).createShader(Rect.fromLTWH(0, leftPress, size.width, lineSize));
    for (int i = 0; i < size.height / 5; i++) {
      canvas.drawLine(
        Offset(
          i * 5.0,
          leftPress,
        ),
        Offset(i * 5.0, leftPress + lineSize),
        linePaint,
      );
    }
    for (int i = 0; i < lineSize / 5; i++) {
      canvas.drawLine(
        Offset(0, leftPress + i * 5.0),
        Offset(
          size.width,
          leftPress + i * 5.0,
        ),
        linePaint,
      );
    }
  }

  @override
  bool shouldRepaint(QrScanBoxPainter oldDelegate) =>
      animationValue != oldDelegate.animationValue;

  @override
  bool shouldRebuildSemantics(QrScanBoxPainter oldDelegate) =>
      animationValue != oldDelegate.animationValue;
}

class CupertioScanView extends StatelessWidget {
  final ValueChanged<ScanResult> onResultCallback;
  final Function(ScannerController controller) onCreatedCallback;
  final Color borderColor;
  final Color cornerColor;
  final Color scanColor;
  final double scanBoxSize;
  final double scanBoxHeightWhenBarCode;
  final double scanBoxVerticalBias;
  final ValueNotifier<bool> modeNotifier;

  CupertioScanView(
      {Key key,
      this.borderColor,
      this.cornerColor,
      this.scanColor,
      this.scanBoxSize,
      this.scanBoxHeightWhenBarCode,
      this.scanBoxVerticalBias = 0.4,
      bool defaultQRCodeMode: true,
      @required this.onResultCallback,
      this.onCreatedCallback})
      : modeNotifier = new ValueNotifier(defaultQRCodeMode),
        assert(defaultQRCodeMode != null),
        assert(scanBoxVerticalBias != null),
        assert(onResultCallback != null);

  @override
  Widget build(BuildContext context) {
    double w = window.physicalSize.width / window.devicePixelRatio;
    double h = window.physicalSize.height / window.devicePixelRatio;
    double scanBoxWidth = scanBoxSize ?? w / 2;
    double scanBoxHeight = scanBoxHeightWhenBarCode ?? 140;
    var topOffset = (h - scanBoxWidth) * scanBoxVerticalBias;
    return Stack(
      fit: StackFit.expand,
      children: [
        UiKitView(
          viewType: "com.qr.scanner.view",
          creationParams: {},
          creationParamsCodec: const StandardMessageCodec(),
          onPlatformViewCreated: _onPlatformViewCreated,
        ),
        Container(
          padding: EdgeInsets.only(top: topOffset),
          alignment: Alignment.topCenter,
          child: ScanBoxView(
              borderColor: borderColor,
              cornerColor: cornerColor,
              scanColor: scanColor,
              modeNotifier: modeNotifier,
              scanBoxWidth: scanBoxWidth,
              scanBoxHeight: scanBoxHeight),
        )
      ],
    );
  }

  void _onPlatformViewCreated(int id) {
    onCreatedCallback
        ?.call(CuperScanController(modeNotifier, id, onResultCallback));
  }
}

class ScanBoxView extends StatefulWidget {
  final Color borderColor;
  final Color cornerColor;
  final Color scanColor;
  final double scanBoxWidth;
  final double scanBoxHeight;
  final ValueNotifier<bool> modeNotifier;

  ScanBoxView(
      {@required this.borderColor,
      @required this.cornerColor,
      @required this.modeNotifier,
      @required this.scanColor,
      @required this.scanBoxWidth,
      @required this.scanBoxHeight})
      : assert(modeNotifier != null);

  @override
  _ScanBoxViewState createState() => _ScanBoxViewState();
}

class _ScanBoxViewState extends State<ScanBoxView>
    with SingleTickerProviderStateMixin {
  AnimationController _animationController;
  Timer _timer;

  @override
  void initState() {
    _animationController = AnimationController(
        vsync: this, duration: Duration(milliseconds: 1000));
    _animationController..addListener(_upState);
    _animationController.repeat(reverse: true);
    widget.modeNotifier.addListener(_upState);
    super.initState();
  }

  void _upState() {
    setState(() {});
  }

  @override
  void dispose() {
    widget.modeNotifier.removeListener(_upState);
    _clearAnimation();
    super.dispose();
  }

  void _clearAnimation() {
    _timer?.cancel();
    if (_animationController != null) {
      _animationController?.dispose();
      _animationController = null;
    }
  }

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      painter: QrScanBoxPainter(
          animationValue: _animationController?.value ?? 0,
          scanColor: widget.scanColor,
          borderColor: widget.borderColor,
          cornerColor: widget.cornerColor,
          isForward: _animationController?.status == AnimationStatus.forward),
      child: SizedBox(
        width: widget.scanBoxWidth,
        height: widget.modeNotifier.value
            ? widget.scanBoxWidth
            : widget.scanBoxHeight,
      ),
    );
  }
}
