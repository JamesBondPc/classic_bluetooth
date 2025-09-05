import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'classic_bluetooth_platform_interface.dart';

/// An implementation of [ClassicBluetoothPlatform] that uses method channels.
class MethodChannelClassicBluetooth extends ClassicBluetoothPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('classic_bluetooth');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
