package com.example.classic_bluetooth

import android.bluetooth.*
import android.content.Context
import android.media.AudioManager
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.EventChannel
import android.os.Handler
import android.os.Looper
import java.util.*

class ClassicBluetoothPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {

    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private var context: Context? = null
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val sockets = mutableMapOf<String, BluetoothSocket>()
    private var eventSink: EventChannel.EventSink? = null
    private var audioManager: AudioManager? = null

    // 新增：A2DP/HEADSET Profile
    private var a2dpProfile: BluetoothProfile? = null
    private var headsetProfile: BluetoothProfile? = null

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            when (profile) {
                BluetoothProfile.A2DP -> a2dpProfile = proxy
                BluetoothProfile.HEADSET -> headsetProfile = proxy
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            when (profile) {
                BluetoothProfile.A2DP -> a2dpProfile = null
                BluetoothProfile.HEADSET -> headsetProfile = null
            }
        }
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager?

        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "classic_bluetooth/methods")
        channel.setMethodCallHandler(this)

        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "classic_bluetooth/events")
        eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                eventSink = events
            }

            override fun onCancel(arguments: Any?) {
                eventSink = null
            }
        })

        // 绑定系统的 A2DP 和 HEADSET
        bluetoothAdapter?.getProfileProxy(context, serviceListener, BluetoothProfile.A2DP)
        bluetoothAdapter?.getProfileProxy(context, serviceListener, BluetoothProfile.HEADSET)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
        when (call.method) {
            "getBondedDevices" -> {
                val pairedDevices = bluetoothAdapter?.bondedDevices?.map {
                    mapOf("name" to it.name, "address" to it.address)
                } ?: emptyList()
                result.success(pairedDevices)
            }

            "isClassicConnected" -> {
                val mac = call.argument<String>("mac")!!
                result.success(sockets[mac]?.isConnected == true)
            }

            "isBleConnected" -> {
                val mac = call.argument<String>("mac")!!
                val manager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val connectedDevices = manager.getConnectedDevices(BluetoothProfile.GATT)
                val isConnected = connectedDevices.any { it.address == mac }
                result.success(isConnected)
            }

            // 🔥 新增：检查音频状态
            "getAudioState" -> {
                val mac = call.argument<String>("mac")!!
                val device = bluetoothAdapter?.getRemoteDevice(mac)

                val a2dpState = a2dpProfile?.getConnectionState(device)
                val headsetState = headsetProfile?.getConnectionState(device)

                val isAudioConnected = (a2dpState == BluetoothProfile.STATE_CONNECTED) ||
                        (headsetState == BluetoothProfile.STATE_CONNECTED)

                val state = when {
                    isAudioConnected && audioManager?.isBluetoothScoOn == true ->
                        "audioActive"
                    isAudioConnected ->
                        "connected"
                    else ->
                        "disconnected"
                }

                result.success(state)
            }

            "connect" -> {
                val mac = call.argument<String>("mac")!!
                val device = bluetoothAdapter?.getRemoteDevice(mac)

                Thread {
                    try {
                        val uuidToUse = device?.uuids?.firstOrNull()?.uuid
                            ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

                        val socket = device?.createRfcommSocketToServiceRecord(uuidToUse)
                        socket?.connect()
                        sockets[mac] = socket!!

                        Handler(Looper.getMainLooper()).post {
                            eventSink?.success(mapOf("mac" to mac, "status" to "connected"))
                        }

                        result.success(true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Handler(Looper.getMainLooper()).post {
                            eventSink?.success(mapOf("mac" to mac, "status" to "failed"))
                        }
                        result.success(false)
                    }
                }.start()
            }

            "disconnect" -> {
                val mac = call.argument<String>("mac")!!
                try {
                    sockets[mac]?.close()
                    sockets.remove(mac)
                    Handler(Looper.getMainLooper()).post {
                        eventSink?.success(mapOf("mac" to mac, "status" to "disconnected"))
                    }
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
}