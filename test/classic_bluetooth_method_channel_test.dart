import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:classic_bluetooth/classic_bluetooth_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelClassicBluetooth platform = MethodChannelClassicBluetooth();
  const MethodChannel channel = MethodChannel('classic_bluetooth');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        return '42';
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
