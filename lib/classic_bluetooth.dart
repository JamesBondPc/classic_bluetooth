import 'dart:async';
import 'package:flutter/services.dart';

enum BluetoothConnectionState { disconnected, connecting, connected }

enum BluetoothAudioState { disconnected, connected, audioActive }

class BluetoothDevice {
  final String name;
  final String address;
  BluetoothDevice({required this.name, required this.address});
}

class ClassicBluetooth {
  static const MethodChannel _channel = MethodChannel(
    'classic_bluetooth/methods',
  );
  static const EventChannel _eventChannel = EventChannel(
    'classic_bluetooth/events',
  );

  // 每个设备单独监听流
  static final Map<String, Stream<BluetoothAudioState>> _streams = {};

  /// 获取已配对设备
  static Future<List<BluetoothDevice>> getBondedDevices() async {
    final List devices = await _channel.invokeMethod('getBondedDevices');
    return devices
        .map((d) => BluetoothDevice(name: d['name'], address: d['address']))
        .toList();
  }

  /// 获取设备的音频状态
  static Future<BluetoothAudioState> getAudioState(String mac) async {
    final status = await _channel.invokeMethod<String>('getAudioState', {
      'mac': mac,
    });
    switch (status) {
      case 'connected':
        return BluetoothAudioState.connected;
      case 'audioActive':
        return BluetoothAudioState.audioActive;
      default:
        return BluetoothAudioState.disconnected;
    }
  }

  /// 连接 Classic 蓝牙
  static Future<bool> connect(String mac, {bool checkBle = true}) async {
    final isClassicConnected = await _channel.invokeMethod(
      'isClassicConnected',
      {'mac': mac},
    );
    if (isClassicConnected) return true;

    if (checkBle) {
      final isBleConnected = await _channel.invokeMethod('isBleConnected', {
        'mac': mac,
      });
      if (isBleConnected) {
        print("BLE 已连接，继续连接 Classic 蓝牙");
      }
    }

    return await _channel.invokeMethod('connect', {'mac': mac});
  }

  /// 断开 Classic 蓝牙
  static Future<void> disconnect(String mac) async {
    await _channel.invokeMethod('disconnect', {'mac': mac});
  }

  /// 监听 Classic 蓝牙状态 + 音频路由状态
  static Stream<BluetoothAudioState> audioState(String mac) {
    if (!_streams.containsKey(mac)) {
      _streams[mac] =
          _eventChannel.receiveBroadcastStream({'mac': mac}).map((event) {
            final status = event['status'] as String?;
            switch (status) {
              case 'connected':
                return BluetoothAudioState.connected; // 只连接，没有音频
              case 'audioActive':
                return BluetoothAudioState.audioActive; // 连接且音频已路由
              default:
                return BluetoothAudioState.disconnected; // 未连接
            }
          }).asBroadcastStream();
    }
    return _streams[mac]!;
  }
}
