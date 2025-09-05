package com.example.classic_bluetooth

import android.bluetooth.*
import android.content.Context
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.EventChannel

class ClassicBluetoothPlugin: FlutterPlugin, MethodChannel.MethodCallHandler {

    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private var context: Context? = null
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val sockets = mutableMapOf<String, BluetoothSocket>()
    private var eventSink: EventChannel.EventSink? = null
    private var audioManager: AudioManager? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "classic_bluetooth/methods")
        channel.setMethodCallHandler(this)

        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "classic_bluetooth/events")
        eventChannel.setStreamHandler(object: EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                eventSink = events
            }

            override fun onCancel(arguments: Any?) {
                eventSink = null
            }
        })
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
                        val uuidToUse = device?.uuids?.firstOrNull()?.uuid
                            ?: java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

                        val socket = device?.createRfcommSocketToServiceRecord(uuidToUse)
                        socket?.connect()
                        sockets[mac] = socket!!
                        
                        // 开启 SCO 音频
                        startScoAudio()

                        // 发送连接状态到 Flutter
                        eventSink?.success(mapOf("mac" to mac, "status" to "connected"))

                        result.success(true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        eventSink?.success(mapOf("mac" to mac, "status" to "failed"))
                        result.success(false)
                    }
                }.start()
            }

            "disconnect" -> {
                val mac = call.argument<String>("mac")!!
                try {
                    sockets[mac]?.close()
                    sockets.remove(mac)
                    eventSink?.success(mapOf("mac" to mac, "status" to "disconnected"))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                result.success(null)
            }

            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        eventSink = null
    }

    private fun startScoAudio() {
    audioManager?.apply {
        if (!isBluetoothScoOn) {
            startBluetoothSco()
            isBluetoothScoOn = true
        }
        mode = AudioManager.MODE_IN_COMMUNICATION
    }
}

private fun stopScoAudio() {
    audioManager?.apply {
        if (isBluetoothScoOn) {
            stopBluetoothSco()
            isBluetoothScoOn = false
        }
        mode = AudioManager.MODE_NORMAL
    }
}
}