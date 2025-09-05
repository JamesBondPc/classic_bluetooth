
import 'classic_bluetooth_platform_interface.dart';

class ClassicBluetooth {
  Future<String?> getPlatformVersion() {
    return ClassicBluetoothPlatform.instance.getPlatformVersion();
  }
}
