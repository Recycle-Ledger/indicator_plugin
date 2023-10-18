import 'dart:async';

import 'package:flutter/services.dart';

class IndicatorPlugin {
  final methodChannel = const MethodChannel('indicator_plugin');
  final eventChannel = const EventChannel('event_channel');

  StreamSubscription? _scanSubscription;

  void dispose() {
    methodChannel.invokeListMethod('dispose');
  }

  Stream<double> startScan() {
    methodChannel.invokeListMethod('connect_indicator');
    return eventChannel.receiveBroadcastStream().map((gram) {
      double kg = (gram as int) / 1000;
      return kg;
    });
  }
}
