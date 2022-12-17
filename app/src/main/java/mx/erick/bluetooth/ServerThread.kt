package mx.erick.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.*

class ServerThread(var bluetoothAdapter: BluetoothAdapter, var contex: Context) : Thread() {
    private lateinit var bluetoothServerSocket: BluetoothServerSocket

    init {
        try {
            if (ActivityCompat.checkSelfPermission(
                    contex,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothServerSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                    "BluetoothAp", UUID.fromString(MainActivity.UUID)
                )
            }
        } catch (e: IOException) {
            println(e.localizedMessage)
        }
    }

    override fun run() {
        var socket: BluetoothSocket
        while (true) {
            try {
                socket = bluetoothServerSocket.accept()
            } catch (e: IOException) {
                println(e.localizedMessage)
                break
            }
            if (socket != null) {
                var cmTh = CommsThread(socket)
                cmTh.run()
            }
        }
    }

    fun cancel() {
        try {
            bluetoothServerSocket.close()
        } catch (e: IOException) {
            println(e.localizedMessage)
        }
    }
}