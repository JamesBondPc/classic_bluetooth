import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'classic_bluetooth_method_channel.dart';

abstract class ClassicBluetoothPlatform extends PlatformInterface {
  /// Constructs a ClassicBluetoothPlatform.
  ClassicBluetoothPlatform() : super(token: _token);

  static final Object _token = Object();

  static ClassicBluetoothPlatform _instance = MethodChannelClassicBluetooth();

  /// The default instance of [ClassicBluetoothPlatform] to use.
  ///
  /// Defaults to [MethodChannelClassicBluetooth].
  static ClassicBluetoothPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [ClassicBluetoothPlatform] when
  /// they register themselves.
  static set instance(ClassicBluetoothPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
