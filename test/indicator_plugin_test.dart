import 'package:flutter_test/flutter_test.dart';
import 'package:indicator_plugin/indicator_plugin.dart';
import 'package:indicator_plugin/indicator_plugin_platform_interface.dart';
import 'package:indicator_plugin/indicator_plugin_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockIndicatorPluginPlatform
    with MockPlatformInterfaceMixin
    implements IndicatorPluginPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final IndicatorPluginPlatform initialPlatform = IndicatorPluginPlatform.instance;

  test('$MethodChannelIndicatorPlugin is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelIndicatorPlugin>());
  });

  test('getPlatformVersion', () async {
    IndicatorPlugin indicatorPlugin = IndicatorPlugin();
    MockIndicatorPluginPlatform fakePlatform = MockIndicatorPluginPlatform();
    IndicatorPluginPlatform.instance = fakePlatform;

    expect(await indicatorPlugin.getPlatformVersion(), '42');
  });
}
