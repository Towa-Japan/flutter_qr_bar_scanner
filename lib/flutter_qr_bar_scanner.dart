import 'dart:async';
import 'dart:developer' as developer;

import 'package:flutter/services.dart';
import 'package:flutter_qr_bar_scanner/barcode_formats.dart';
import 'package:flutter_qr_bar_scanner/camera_orientation.dart';
import 'package:flutter_qr_bar_scanner/scan_result.dart';

class PreviewDetails {
  final Size size;
  final int sensorOrientation;
  final int textureId;

  PreviewDetails(int width, int height, this.sensorOrientation, this.textureId)
      : size = Size(width.toDouble(), height.toDouble());
}

const _defaultBarcodeFormats = const [
  BarcodeFormats.ALL_FORMATS,
];

class FlutterQrReader {
  static const MethodChannel _channel =
      const MethodChannel('com.towagifu/flutter_qr_bar_scanner');
  static QrChannelReader channelReader = QrChannelReader(_channel);

  //Set target size before starting
  static Future<PreviewDetails> start({
    required int width,
    required int height,
    required Rect Function(Rect) Function() transformBuilder,
    required QRCodeHandler qrCodeHandler,
    List<BarcodeFormats>? formats,
    CameraOrientation cameraOrientation = CameraOrientation.awayFromUser,
  }) async {
    developer.log('$width x $height $cameraOrientation ($formats)', name: 'FlutterQrReader');
    final formatsResolved = formats ?? _defaultBarcodeFormats;
    assert(formatsResolved.length > 0);

    List<String> formatStrings = formatsResolved
        .map((format) => format.toString().split('.')[1])
        .toList(growable: false);

    var details = await _channel.invokeMethod('start', {
      'targetWidth': width,
      'targetHeight': height,
      'heartbeatTimeout': 0,
      'formats': formatStrings,
      'orientation': cameraOrientation.name,
    });

    // invokeMethod returns Map<dynamic,...> in dart 2.0
    assert(details is Map<dynamic, dynamic>);

    int textureId = details["textureId"];
    int surfaceOrientation = details["surfaceOrientation"];
    int surfaceHeight = details["surfaceHeight"];
    int surfaceWidth = details["surfaceWidth"];

    channelReader.setTransformBuilder(transformBuilder);
    channelReader.setQrCodeHandler(qrCodeHandler);

    return PreviewDetails(
        surfaceWidth, surfaceHeight, surfaceOrientation, textureId);
  }

  static Future setTorchState(bool isOn) {
    return _channel.invokeMethod(
      'setTorchState',
      {'isOn': isOn},
    ).catchError(print);
  }

  static Future stop() {
    channelReader.setQrCodeHandler(null);
    return _channel.invokeMethod('stop').catchError(print);
  }

  static Future heartbeat() {
    return _channel.invokeMethod('heartbeat').catchError(print);
  }
}

enum FrameRotation { none, ninetyCC, oneeighty, twoseventyCC }

typedef void QRCodeHandler(Iterable<ScanResult> qr);

class QrChannelReader {
  QrChannelReader(this.channel) {
    channel.setMethodCallHandler((MethodCall call) async {
      switch (call.method) {
        case 'qrRead':
          if (qrCodeHandler != null) {
            var args = (call.arguments as List)
                .map((e) => ScanResult.fromChannelArgs(Map.from(e)));
            if (this._transformBuilder != null) {
              final transform = this._transformBuilder!();
              args = args.map((e) => e.applyTransform(transform));
            }
            qrCodeHandler!(args);
          }
          break;
        default:
          print("QrChannelHandler: unknown method call received at "
              "${call.method}");
      }
    });
  }

  void setQrCodeHandler(QRCodeHandler? qrch) {
    this.qrCodeHandler = qrch;
  }

  void setTransformBuilder(Rect Function(Rect) Function() transformBuilder) {
    this._transformBuilder = transformBuilder;
  }

  MethodChannel channel;
  QRCodeHandler? qrCodeHandler;
  Rect Function(Rect) Function()? _transformBuilder;
}
