import 'package:flutter/foundation.dart';

class TorchStateController {
  final ValueNotifier<bool> notifier;

  bool get isOn => notifier.value;
  void set isOn(bool isOn) => notifier.value = isOn;

  TorchStateController({bool? isOn})
      : notifier = new ValueNotifier(isOn ?? false);

  void dispose() => notifier.dispose();
}
