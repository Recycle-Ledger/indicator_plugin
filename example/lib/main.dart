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
  bool isConnect = false;

  @override
  void initState() {
    super.initState();
  }

  Future<void> getPermission() async {
    await Permission.bluetoothConnect.request();
    await Permission.bluetoothScan.request();
    await Permission.locationWhenInUse.request();
  }

  Future<void> connectIndicator() async {
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
          title: const Text('Indicator plugin example'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                'weight : $weight KG',
                style: Theme.of(context).textTheme.headlineLarge,
              ),
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: () async {
                  if (isConnect) {
                    setState(() {
                      isConnect = !isConnect;
                      weight = 0;
                    });
                    _indicatorPlugin.dispose();
                  } else {
                    setState(() {
                      isConnect = !isConnect;
                    });
                    await getPermission();
                    await connectIndicator();
                  }
                },
                child: Text(isConnect ? '해제' : '연결'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
