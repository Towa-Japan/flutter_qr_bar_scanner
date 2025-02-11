import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_qr_bar_scanner/scan_result.dart';

class PreviewDetails {
  final Size size;
  final int sensorOrientation;
  final int textureId;

  PreviewDetails(int width, int height, this.sensorOrientation, this.textureId)
      : size = Size(width.toDouble(), height.toDouble());
}

enum BarcodeFormats {
  ALL_FORMATS,
  AZTEC,
  CODE_128,
  CODE_39,
  CODE_93,
  CODABAR,
  DATA_MATRIX,
  EAN_13,
  EAN_8,
  ITF,
  PDF417,
  QR_CODE,
  UPC_A,
  UPC_E,
}

const _defaultBarcodeFormats = const [
  BarcodeFormats.ALL_FORMATS,
];

class FlutterQrReader {
  static const MethodChannel _channel = const MethodChannel(
      'com.towagifu/flutter_qr_bar_scanner');
  static QrChannelReader channelReader = QrChannelReader(_channel);

  //Set target size before starting
  static Future<PreviewDetails> start({
    required int width,
    required int height,
    required Rect Function(Rect) Function() transformBuilder,
    required QRCodeHandler qrCodeHandler,
    List<BarcodeFormats>? formats = _defaultBarcodeFormats,
  }) async {
    final _formats = formats ?? _defaultBarcodeFormats;
    assert(_formats.length > 0);

    List<String> formatStrings = _formats
        .map((format) => format.toString().split('.')[1])
        .toList(growable: false);

    var details = await _channel.invokeMethod('start', {
      'targetWidth': width,
      'targetHeight': height,
      'heartbeatTimeout': 0,
      'formats': formatStrings,
    });

    // invokeMethod returns Map<dynamic,...> in dart 2.0
    assert(details is Map<dynamic, dynamic>);

    int textureId = details["textureId"];
    int orientation = details["surfaceOrientation"];
    int surfaceHeight = details["surfaceHeight"];
    int surfaceWidth = details["surfaceWidth"];

    channelReader.setTransformBuilder(transformBuilder);
    channelReader.setQrCodeHandler(qrCodeHandler);

    return PreviewDetails(surfaceWidth, surfaceHeight, orientation, textureId);
  }

  static Future stop() {
    channelReader.setQrCodeHandler(null);
    return _channel.invokeMethod('stop').catchError(print);
  }

  static Future heartbeat() {
    return _channel.invokeMethod('heartbeat').catchError(print);
  }

  static Future<List<List<int>>?> getSupportedSizes() {
    return _channel.invokeMethod('getSupportedSizes').catchError(print)
        as Future<List<List<int>>?>;
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
