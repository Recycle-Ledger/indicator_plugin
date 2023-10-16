package com.example.indicator_plugin

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
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
class IndicatorPlugin: FlutterPlugin, MethodCallHandler {
  val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID

  private lateinit var methodChannel : MethodChannel
  private lateinit var eventChannel : EventChannel

  private lateinit var inputStream: InputStream

  private lateinit var applicationContext: Context

  private var scanSink: EventSink? = null

  private lateinit var bluetoothManager: BluetoothManager
  private lateinit var bluetoothAdapter: BluetoothAdapter

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    applicationContext = flutterPluginBinding.applicationContext
    bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    bluetoothAdapter = bluetoothManager.adapter

    methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "indicator_plugin")
    methodChannel.setMethodCallHandler(this)

    eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "scan_channel")

    eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
      override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        scanSink = events
      }

      override fun onCancel(arguments: Any?) {
      }
    })
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    when (call.method) {
      "startScan" -> startScan()
    }
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    methodChannel.setMethodCallHandler(null)
  }

  private fun startScan() {
    val device: BluetoothDevice? = getPairedDevice()
    if (device != null) {
      startBluetoothSocket(device)
    } else {
      val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
      applicationContext.registerReceiver(discoveryReceiver, filter)
      bluetoothAdapter.startDiscovery()
    }
  }

  private fun getPairedDevice(): BluetoothDevice? {
    val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices

    pairedDevices.forEach { device ->
      val deviceName = device.name
      val deviceHardwareAddress = device.address

      if (device.name.startsWith("WCS")) {
        return device
      }
    }
    return null
  }

  private val discoveryReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      val action: String? = intent?.action
      if (BluetoothDevice.ACTION_FOUND == action) {
        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
          intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

        if (device?.name?.startsWith("WCS") == true) {
          connectDevice(device)
        }
      }
    }
  }

  private fun connectDevice(device: BluetoothDevice) {
    when (device.bondState) {
      BluetoothDevice.BOND_NONE -> {
        device.setPin("1234".toByteArray())
        val connectMethod: Method = device.javaClass.getMethod("createBond")
        connectMethod.invoke(device)
      }
      BluetoothDevice.BOND_BONDED -> {
        startBluetoothSocket(device)
      }
    }
  }

  private fun startBluetoothSocket(device: BluetoothDevice) {
    val bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
    bluetoothSocket?.connect()
    inputStream = bluetoothSocket!!.inputStream
    if (bluetoothSocket.isConnected) {
      startReadingData()
    } else {
      Log.d("블루투스 통신","연결 실패")
    }
  }

  private fun startReadingData() {
    Thread {
      val regex = "^(ST|US),NT,[-+]?\\s\\d{6}(kg|g|t)\$"

      val sb = StringBuilder()
      while (true) {
        try {

          val availableLength:Int = inputStream.available()
          if(0 < availableLength)
          {
            val receivedData:Int = inputStream.read()
            sb.append(receivedData.toChar())
            if(receivedData == 0x0a)//LF 검출
            {
              Log.d("디버깅",sb.toString())


              if(sb.toString().replace("\r", "").replace("\n", "").matches(Regex(regex)))
              {
                val regex = Regex("[-+]?\\s\\d+")//무게값만 가져오기 위한 정규식
                val matchResult = regex.find(sb.toString())//무게값만 가져옴
                Log.d(matchResult?.value, "@_@");
                val weight = matchResult?.value?.replace(" ", "")?.let { Integer.parseInt(it) }//가져온 무게값 변환 (공백 없앰)
                Handler(Looper.getMainLooper()).post {
                  scanSink?.success(weight?.toInt())
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
