import 'dart:typed_data';

import 'package:collection/collection.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_qr_bar_scanner/barcode_formats.dart';

class ScanResult {
  final String? content;
  final Rect bounds;
  final BarcodeFormats? format;

  final Uint8List bytes;

  const ScanResult(this.content, this.bytes, this.bounds, this.format);

  ScanResult applyTransform(Rect Function(Rect) transform) {
    return ScanResult(
      this.content,
      this.bytes,
      transform(this.bounds),
      this.format,
    );
  }

  static ScanResult fromChannelArgs(Map<String, dynamic> args) {
    final bb = Map.from(args["bounds"]);
    final bounds = Rect.fromLTRB(
      (bb["left"] as int).toDouble(),
      (bb["top"] as int).toDouble(),
      (bb["right"] as int).toDouble(),
      (bb["bottom"] as int).toDouble(),
    );

    final format = args["format"] as String;

    return ScanResult(
      args["content"] as String?,
      args["bytes"] as Uint8List,
      bounds,
      BarcodeFormats.values.firstWhereOrNull((e) => e.name == format),
    );
  }
}
