import 'package:flutter_test/flutter_test.dart';
import 'package:classic_bluetooth/classic_bluetooth.dart';
import 'package:classic_bluetooth/classic_bluetooth_platform_interface.dart';
import 'package:classic_bluetooth/classic_bluetooth_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockClassicBluetoothPlatform
    with MockPlatformInterfaceMixin
    implements ClassicBluetoothPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final ClassicBluetoothPlatform initialPlatform = ClassicBluetoothPlatform.instance;

  test('$MethodChannelClassicBluetooth is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelClassicBluetooth>());
  });

  test('getPlatformVersion', () async {
    ClassicBluetooth classicBluetoothPlugin = ClassicBluetooth();
    MockClassicBluetoothPlatform fakePlatform = MockClassicBluetoothPlatform();
    ClassicBluetoothPlatform.instance = fakePlatform;

    expect(await classicBluetoothPlugin.getPlatformVersion(), '42');
  });
}
