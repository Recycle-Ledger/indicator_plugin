import 'dart:async';

import 'package:flutter/material.dart';
import 'package:indicator_plugin/indicator_plugin.dart';
import 'package:permission_handler/permission_handler.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final _indicatorPlugin = IndicatorPlugin();
  double weight = 0;

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  Future<void> getPermission() async {
    await Permission.bluetoothConnect.request();
    await Permission.bluetoothScan.request();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    if (!mounted) return;

    await getPermission();

    _indicatorPlugin.startScan().listen((event) {
      setState(() {
        weight = event;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Text(
            'weight : $weight KG',
            style: Theme.of(context).textTheme.headlineLarge,
          ),
        ),
      ),
    );
  }
}
