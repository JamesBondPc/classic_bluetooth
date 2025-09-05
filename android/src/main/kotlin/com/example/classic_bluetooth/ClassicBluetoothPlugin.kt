package com.example.classic_bluetooth

import android.bluetooth.*
import android.content.Context
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.EventChannel

class ClassicBluetoothPlugin: FlutterPlugin, MethodChannel.MethodCallHandler {

    private lateinit var channel : MethodChannel
    private lateinit var eventChannel: EventChannel
    private var context: Context? = null
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val sockets = mutableMapOf<String, BluetoothSocket>()

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "classic_bluetooth/methods")
        channel.setMethodCallHandler(this)

        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "classic_bluetooth/events")
        // 这里可实现 EventChannel.StreamHandler 监听
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
        when(call.method) {
            "getBondedDevices" -> {
                val pairedDevices = bluetoothAdapter?.bondedDevices?.map {
                    mapOf("name" to it.name, "address" to it.address)
                } ?: emptyList()
                result.success(pairedDevices)
            }
            "isClassicConnected" -> {
                val mac = call.argument<String>("mac")!!
                val isConnected = sockets[mac]?.isConnected == true
                result.success(isConnected)
            }
            "isBleConnected" -> {
                val mac = call.argument<String>("mac")!!
                val manager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val connectedDevices = manager.getConnectedDevices(BluetoothProfile.GATT)
                val isConnected = connectedDevices.any { it.address == mac }
                result.success(isConnected)
            }
            "connect" -> {
                val mac = call.argument<String>("mac")!!
                val device = bluetoothAdapter?.getRemoteDevice(mac)
                Thread {
                    try {
                        val socket = device?.createRfcommSocketToServiceRecord(
                            device.uuids.first().uuid
                        )
                        socket?.connect()
                        sockets[mac] = socket!!
                        result.success(true)
                        // TODO: 可以通过 EventChannel 通知状态
                    } catch (e: Exception) {
                        e.printStackTrace()
                        result.success(false)
                    }
                }.start()
            }
            "disconnect" -> {
                val mac = call.argument<String>("mac")!!
                sockets[mac]?.close()
                sockets.remove(mac)
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}