import 'package:flutter/foundation.dart';
import 'package:flutter_qr_bar_scanner/camera_orientation.dart';

class CameraStateController {
  final ValueNotifier<bool> torchNotifier;
  final ValueNotifier<CameraOrientation> cameraOrientationNotifier;

  bool get isTorchOn => torchNotifier.value;
  void set isTorchOn(bool isTorchOn) => torchNotifier.value = isTorchOn;

  CameraOrientation get orientation => cameraOrientationNotifier.value;
  void set orientation(CameraOrientation orientation) =>
      cameraOrientationNotifier.value = orientation;

  CameraStateController({bool? isTorchOn, CameraOrientation? orientation})
      : torchNotifier = new ValueNotifier(isTorchOn ?? false),
        cameraOrientationNotifier =
            new ValueNotifier(orientation ?? CameraOrientation.awayFromUser);

  void dispose() {
    torchNotifier.dispose();
    cameraOrientationNotifier.dispose();
  }
}
