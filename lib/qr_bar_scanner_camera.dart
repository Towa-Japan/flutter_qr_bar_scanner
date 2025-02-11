import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_qr_bar_scanner/scan_result.dart';
import 'package:native_device_orientation/native_device_orientation.dart';
import 'package:flutter_qr_bar_scanner/flutter_qr_bar_scanner.dart';

final WidgetBuilder _defaultNotStartedBuilder =
    (context) => Text("Camera Loading ...");
final WidgetBuilder _defaultOffscreenBuilder =
    (context) => Text("Camera Paused.");
final ErrorCallback _defaultOnError = (BuildContext context, Object? error) {
  print("Error reading from camera: $error");
  return Text("Error reading from camera...");
};

typedef Widget ErrorCallback(BuildContext context, Object? error);

class QRBarScannerCamera extends StatefulWidget {
  QRBarScannerCamera({
    Key? key,
    required this.qrCodeCallback,
    this.child,
    this.fit = BoxFit.cover,
    WidgetBuilder? notStartedBuilder,
    WidgetBuilder? offscreenBuilder,
    ErrorCallback? onError,
    this.formats,
  })  : notStartedBuilder = notStartedBuilder ?? _defaultNotStartedBuilder,
        offscreenBuilder =
            offscreenBuilder ?? notStartedBuilder ?? _defaultOffscreenBuilder,
        onError = onError ?? _defaultOnError,
        super(key: key);

  final BoxFit fit;
  final ValueChanged<Iterable<ScanResult>> qrCodeCallback;
  final Widget? child;
  final WidgetBuilder notStartedBuilder;
  final WidgetBuilder offscreenBuilder;
  final ErrorCallback onError;
  final List<BarcodeFormats>? formats;

  @override
  QRBarScannerCameraState createState() => QRBarScannerCameraState();
}

class QRBarScannerCameraState extends State<QRBarScannerCamera>
    with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      setState(() => onScreen = true);
    } else {
      if (_asyncInitOnce != null && onScreen) {
        FlutterQrReader.stop();
      }
      setState(() {
        onScreen = false;
        _asyncInitOnce = null;
      });
    }
  }

  bool onScreen = true;
  Future<PreviewDetails>? _asyncInitOnce;
  Orientation? _deviceOrientation;
  PreviewDetails? _previewDetails;

  Future<PreviewDetails> _asyncInit(num width, num height) async {
    final devicePixelRatio = MediaQuery.of(context).devicePixelRatio;
    assert(widget.fit == BoxFit.cover);

    return await FlutterQrReader.start(
      width: (devicePixelRatio * width.toInt()).ceil(),
      height: (devicePixelRatio * height.toInt()).ceil(),
      qrCodeHandler: widget.qrCodeCallback,
      transformBuilder: _transformBuilder,
      formats: widget.formats,
    );
  }

  Rect Function(Rect) _transformBuilder() {
    assert(widget.fit == BoxFit.cover);

    if (_previewDetails == null) {
      return (r) => r;
    }
    final preview = _previewDetails!;
    final devicePixelRatio = MediaQuery.of(context).devicePixelRatio;
    final rotationCompensation = _getRotationCompensation(
      (_deviceOrientation == null)
          ? NativeDeviceOrientation.unknown
          : (_deviceOrientation == Orientation.portrait
              ? NativeDeviceOrientation.portraitUp
              : NativeDeviceOrientation.landscapeLeft),
      preview.sensorOrientation,
    );
    final widgetSize = (context.findRenderObject() as RenderBox).size;
    final displaySize = Size(
      widgetSize.width * devicePixelRatio,
      widgetSize.height * devicePixelRatio,
    );
    final previewSizeResolved =
        (rotationCompensation % 2 == 0) ? preview.size.flipped : preview.size;

    switch (widget.fit) {
      case BoxFit.cover:
        final scale =
            (displaySize.aspectRatio >= previewSizeResolved.aspectRatio)
                ? displaySize.width / previewSizeResolved.width
                : displaySize.height / previewSizeResolved.height;
        final offset = Offset(
          (previewSizeResolved.width * scale - displaySize.width) / 2,
          (previewSizeResolved.height * scale - displaySize.height) / 2,
        );

        return (bounds) {
          return Rect.fromLTWH(
            (bounds.left * scale - offset.dx) / devicePixelRatio,
            (bounds.top * scale - offset.dy) / devicePixelRatio,
            bounds.width * scale / devicePixelRatio,
            bounds.height * scale / devicePixelRatio,
          );
        };
      case BoxFit.fill:
      case BoxFit.contain:
      case BoxFit.fitWidth:
      case BoxFit.fitHeight:
      case BoxFit.none:
      case BoxFit.scaleDown:
        throw Error();
    }
  }

  /// This method can be used to restart scanning
  ///  the event that it was paused.
  void restart() {
    (() async {
      await FlutterQrReader.stop();
      setState(() {
        _asyncInitOnce = null;
      });
    })();
  }

  /// This method can be used to manually stop the
  /// camera.
  void stop() {
    (() async {
      await FlutterQrReader.stop();
    })();
  }

  @override
  deactivate() {
    super.deactivate();
    FlutterQrReader.stop();
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (BuildContext context, BoxConstraints constraints) {
        if (_asyncInitOnce == null && onScreen) {
          _asyncInitOnce =
              _asyncInit(constraints.maxWidth, constraints.maxHeight);
        } else if (!onScreen) {
          return widget.offscreenBuilder(context);
        }

        return FutureBuilder(
          future: _asyncInitOnce,
          builder: (
            BuildContext context,
            AsyncSnapshot<PreviewDetails> details,
          ) {
            _previewDetails = details.data;
            switch (details.connectionState) {
              case ConnectionState.none:
              case ConnectionState.waiting:
                return widget.notStartedBuilder(context);
              case ConnectionState.done:
                if (details.hasError) {
                  debugPrint(details.error.toString());
                  return widget.onError(context, details.error);
                }

                return OrientationBuilder(
                  builder: (context, orientation) {
                    _deviceOrientation = orientation;

                    Widget preview = SizedBox(
                      width: constraints.maxWidth,
                      height: constraints.maxHeight,
                      child: Preview(
                        previewDetails: details.data!,
                        targetWidth: constraints.maxWidth,
                        targetHeight: constraints.maxHeight,
                        fit: widget.fit,
                      ),
                    );

                    if (widget.child != null) {
                      return Stack(
                        children: [
                          preview,
                          widget.child!,
                        ],
                      );
                    }
                    return preview;
                  },
                );

              default:
                throw AssertionError(
                    "${details.connectionState} not supported.");
            }
          },
        );
      },
    );
  }
}

int _getRotationCompensation(
  final NativeDeviceOrientation deviceOrientation,
  final int cameraOrientation,
) {
  int nativeRotation = 0;
  switch (deviceOrientation) {
    case NativeDeviceOrientation.portraitUp:
      nativeRotation = 0;
      break;
    case NativeDeviceOrientation.landscapeRight:
      nativeRotation = 90;
      break;
    case NativeDeviceOrientation.portraitDown:
      nativeRotation = 180;
      break;
    case NativeDeviceOrientation.landscapeLeft:
      nativeRotation = 270;
      break;
    case NativeDeviceOrientation.unknown:
    default:
      break;
  }

  return ((nativeRotation - cameraOrientation + 450) % 360) ~/ 90;
}

class Preview extends StatelessWidget {
  final Size size;
  final double targetWidth, targetHeight;
  final int textureId;
  final int sensorOrientation;
  final BoxFit fit;

  Preview({
    required PreviewDetails previewDetails,
    required this.targetWidth,
    required this.targetHeight,
    required this.fit,
  })  : textureId = previewDetails.textureId,
        size = previewDetails.size.flipped,
        sensorOrientation = previewDetails.sensorOrientation;

  @override
  Widget build(BuildContext context) {
    return NativeDeviceOrientationReader(
      builder: (context) {
        final rotationCompensation = _getRotationCompensation(
          NativeDeviceOrientationReader.orientation(context),
          sensorOrientation,
        );

        return ClipRect(
          child: FittedBox(
            fit: fit,
            child: RotatedBox(
              quarterTurns: rotationCompensation,
              child: SizedBox(
                width: size.width,
                height: size.height,
                child: Texture(textureId: textureId),
              ),
            ),
          ),
        );
      },
    );
  }
}
