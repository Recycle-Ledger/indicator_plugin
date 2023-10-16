import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_reactive_ble/flutter_reactive_ble.dart';

class IndicatorPlugin {
  final methodChannel = const MethodChannel('indicator_plugin');
  final scanChannel = const EventChannel('scan_channel');

  StreamSubscription? _scanSubscription;

  void dispose() {
    _scanSubscription?.cancel();
  }

  Stream<double> startScan() {
    methodChannel.invokeListMethod('startScan');
    return scanChannel.receiveBroadcastStream().map((gram) {
      double kg = (gram as int) / 1000;
      return kg;
    });
  }
}
