import Flutter
import UIKit
import AVFoundation

public class SwiftClassicBluetoothPlugin: NSObject, FlutterPlugin {
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "classic_bluetooth/methods", binaryMessenger: registrar.messenger())
        let instance = SwiftClassicBluetoothPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    enum BluetoothAudioState: String {
        case disconnected
        case connected
        case audioActive
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
            
        case "getAudioState":
            guard let args = call.arguments as? [String: Any],
                  let targetMac = args["mac"] as? String else {
                result(BluetoothAudioState.disconnected.rawValue)
                return
            }
            result(getAudioState(targetMac: targetMac).rawValue)
            
        default:
            result(FlutterMethodNotImplemented)
        }
    }
    
    private func getAudioState(targetMac: String) -> BluetoothAudioState {
        let session = AVAudioSession.sharedInstance()
        let outputs = session.currentRoute.outputs
        
        // iOS 无法用 MAC 地址识别蓝牙耳机，只能根据名称匹配
        for output in outputs {
            let portName = output.portName.lowercased()
            if portName.contains("h8") || portName.contains("h9") {
                // 音频已经路由到耳机
                if session.isOtherAudioPlaying {
                    return .audioActive
                } else {
                    return .connected
                }
            }
        }
        return .disconnected
    }
}