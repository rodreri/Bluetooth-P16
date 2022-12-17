package mx.erick.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.*

class ConnectToServerThread(
    var bluetoothDevice: BluetoothDevice, var bluetoothAdapter: BluetoothAdapter,
    var context: Context
) : Thread() {
    lateinit var bluetoothSocket: BluetoothSocket
    lateinit var commsThread: CommsThread

    init {
        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(
                    UUID.fromString(MainActivity.UUID)
                )
            }
        } catch (e: IOException) {
            println(e.localizedMessage)
        }
    }

    override fun run() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothAdapter.cancelDiscovery()
        }
        try {
            println("se Inicia bien")
            bluetoothSocket.connect()
            commsThread = CommsThread(bluetoothSocket)
            commsThread.start()
        } catch (e: IOException) {
            try {
                bluetoothSocket.close()
            } catch (e: IOException) {
                println(e.localizedMessage)
            }
            return
        }
    }

    fun cancel() {
        try {
            bluetoothSocket.close()
            if (commsThread != null) {
                commsThread.cancel()
            }
        } catch (e: IOException) {
            println(e.localizedMessage)
        }
    }
}