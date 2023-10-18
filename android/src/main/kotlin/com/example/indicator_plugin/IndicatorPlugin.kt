package com.example.indicator_plugin

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Method
import java.util.UUID


/** IndicatorPlugin */
class IndicatorPlugin : FlutterPlugin, MethodCallHandler {
    val TAG: String = "@@@ IndicatorPlugin"
    val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID

    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: EventChannel

    private lateinit var inputStream: InputStream

    private lateinit var applicationContext: Context

    private var eventSink: EventSink? = null

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothSocket: BluetoothSocket? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = flutterPluginBinding.applicationContext
        bluetoothManager =
            applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "indicator_plugin")
        methodChannel.setMethodCallHandler(this)

        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "event_channel")

        eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                eventSink = events
            }

            override fun onCancel(arguments: Any?) {
                eventSink = null
            }
        })
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "connect_indicator" -> connectIndicator()
            "dispose" -> dispose()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
    }

    private fun dispose() {
        eventSink = null
        bluetoothSocket?.close()
        if (discoveryReceiver.isOrderedBroadcast) {
            applicationContext.unregisterReceiver(discoveryReceiver)
        }
        if (pairingReceiver.isOrderedBroadcast) {
            applicationContext.unregisterReceiver(pairingReceiver)
        }
    }

    private fun connectIndicator() {
        var filter: IntentFilter = IntentFilter()
        // 기기를 찾았을때
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        // 찾은 기기의 연결 상태가 변했을때 수신을 받는다
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)

        val device: BluetoothDevice? = getPairedDevice()

        // 이미 기기 연결이 되었거나 페어링을 끝내고 기기 연결을 완료했을때
        if (device != null && device.bondState == BluetoothDevice.BOND_BONDED) {
            Log.d(TAG, "device exist")
            // 소켓통신 시작
            connectBluetoothSocket(device)
            // 기존에 기기 탐색 리시버 해제
            if (discoveryReceiver.isOrderedBroadcast) {
                applicationContext.unregisterReceiver(discoveryReceiver)
            }
            if (pairingReceiver.isOrderedBroadcast) {
                applicationContext.unregisterReceiver(pairingReceiver)
            }
        }
        // 등록된 기기가 없어서 새롭게 스캔해야할때
        else {
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            applicationContext.registerReceiver(discoveryReceiver, filter)
            bluetoothAdapter.startDiscovery()
        }
    }

    private fun getPairedDevice(): BluetoothDevice? {
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices

        pairedDevices.forEach { device ->
            if (device.name.startsWith("WCS")) {
                return device
            }
        }
        return null
    }

    private val pairingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "onReceive")
            val device = getDevice(intent)
            // 현재 상태가 연결 상태가 됐을 경우
            if (device?.bondState == BluetoothDevice.BOND_BONDED) {
                connectIndicator()
            }
        }
    }

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "onReceive")
            val action: String? = intent?.action
            val device = getDevice(intent)

            Log.d(TAG, "device : ${device?.name}")
            if (device?.name?.startsWith("WCS") != true) {
                return
            }

            if (BluetoothDevice.ACTION_FOUND == action) {
                Log.d(TAG, "device found : ${device.name}")
                connectDevice(device)
            }
        }
    }

    private fun getDevice(intent: Intent?): BluetoothDevice? {
        val device: BluetoothDevice? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(
                    BluetoothDevice.EXTRA_DEVICE,
                    BluetoothDevice::class.java
                )
            } else {
                intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }
        return device
    }

    private fun connectDevice(device: BluetoothDevice) {
        Log.d(TAG, "connectDevice()")
        when (device.bondState) {
            BluetoothDevice.BOND_NONE -> {
                device.setPin("1234".toByteArray())
                val connectMethod: Method = device.javaClass.getMethod("createBond")
                connectMethod.invoke(device)
                applicationContext.registerReceiver(
                    pairingReceiver,
                    IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                )
            }

            BluetoothDevice.BOND_BONDED -> {
                connectBluetoothSocket(device)
            }
        }
    }

    private fun connectBluetoothSocket(device: BluetoothDevice) {
        Log.d(TAG, "connectBluetoothSocket()")
        bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
        bluetoothSocket?.connect()
        inputStream = bluetoothSocket!!.inputStream
        if (bluetoothSocket!!.isConnected) {
            startReadingData()
        }
    }

    private fun startReadingData() {
        Log.d(TAG, "startReadingData()")
        Thread {
            val regex = "^(ST|US),NT,[-+]?\\s\\d{6}(kg|g|t)\$"
            val sb = StringBuilder()

            while (true) {
                try {
                    val availableLength: Int = inputStream.available()
                    if (0 < availableLength) {
                        val receivedData: Int = inputStream.read()
                        sb.append(receivedData.toChar())
                        if (receivedData == 0x0a)//LF 검출
                        {
                            if (sb.toString().replace("\r", "").replace("\n", "").matches(Regex(regex))
                            ) {
                                val regex = Regex("[-+]?\\s\\d+")//무게값만 가져오기 위한 정규식
                                val matchResult = regex.find(sb.toString())//무게값만 가져옴
                                val weight = matchResult?.value?.replace(" ", "")
                                    ?.let { Integer.parseInt(it) }//가져온 무게값 변환 (공백 없앰)
                                Handler(Looper.getMainLooper()).post {
                                    eventSink?.success(weight)
                                }
                            }
                            sb.clear()//패킷의 끝을 수신 했기에 버퍼 초기화
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    // Error occurred while reading data
                    break
                }
            }
        }.start()
    }
}
