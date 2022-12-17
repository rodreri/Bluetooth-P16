package mx.erick.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import mx.erick.bluetooth.databinding.ActivityMainBinding
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var responseLauncher: ActivityResultLauncher<Intent>
    private var discoverDevicesReceiver: BroadcastReceiver? = null
    private var discoveryFinishedReceiver: BroadcastReceiver? = null
    private var discoveryDevices = java.util.ArrayList<BluetoothDevice>()
    private var nameDevices = java.util.ArrayList<String>()
    private lateinit var serverThread: ServerThread
    private var connectToServerThread: ConnectToServerThread? = null

    companion object {
        const val ENABLE_BLUETOOTH = 100
        const val BLUETOOTH_ACTIVAR = "BLUETOOTH_ACTIVAR"
        const val UUID = "00001101-0000-1000-8000-00805F9B34FB"
        lateinit var iUpdate: Handler
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MainActivity.iUpdate = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                var bytesReceived = msg.arg1
                val buffer: ByteArray = msg.obj as ByteArray
                var cadena = String(buffer).substring(0, bytesReceived)
                binding.recivedData.text = binding.recivedData.text.toString() + " " + cadena
            }
        }
        bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        responseLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
                val name = activityResult.data?.getStringExtra(BLUETOOTH_ACTIVAR).orEmpty()
                if (activityResult.resultCode == RESULT_OK) {
                    //Toast.makeText(this, "Activado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No Activarlo", Toast.LENGTH_SHORT).show()
                }
            }
        if (isBluetooth()) {
            if (bluetoothAdapter.isEnabled == false) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(this, "Listo para activar", Toast.LENGTH_SHORT).show()
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                        putExtra(BLUETOOTH_ACTIVAR, ENABLE_BLUETOOTH)
                    }
                    responseLauncher.launch(enableBtIntent)
                }
            } else {
                Toast.makeText(this, "Esta listo para usar", Toast.LENGTH_SHORT).show()
            }
        }
        //ser visible
        binding.makeVisible.setOnClickListener {
            var intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
            startActivity(intent)
        }
        //buscar dispositivos
        binding.searchDevices.setOnClickListener {
            search()
        }
        //mandar el mensaje

        binding.sendMessages.setOnClickListener {
            if (connectToServerThread != null) {
                runBlocking {
                    var data = async {
                        connectToServerThread?.commsThread?.write(
                            binding.message.text.toString()
                        )
                    }
                }
            } else {
                Toast.makeText(this, "Selecciona un cliente", Toast.LENGTH_SHORT).show()
            }
        }
        binding.devicesList.setOnItemClickListener { parent, view, position, id ->
            print("Evento......")
            connectToServerThread =
                ConnectToServerThread(discoveryDevices.get(position), bluetoothAdapter, this)
            connectToServerThread?.start()
        }
    }

    private fun isBluetooth(): Boolean {
        return bluetoothAdapter != null
    }

    private fun search() {
        if (discoverDevicesReceiver == null) {
            println("entra....1")
            discoverDevicesReceiver = MyBroadCastReceiver()
            // discoverDevicesReceiver=object:BroadcastReceiver(){
            // @SuppressLint("MissingPermission")
            // override fun onReceive(context: Context, intent: Intent) {
            // println("entra....2")
            // val action: String? = intent.action
            // println("entra....2 "+action)
            // when(action) {
            // BluetoothDevice.ACTION_NAME_CHANGED -> {
            // println("entra....3")
            // // Discovery has found a device. Get the BluetoothDevice
            // // object and its info from the Intent.
            // val device: BluetoothDevice? =
            // intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            // val deviceName = device!!.name
            // val deviceHardwareAddress = device.address // MAC address
            // println(deviceName)
            // }
            // }
            // }
            //
            // }
        }
        if (discoveryFinishedReceiver == null) {
            discoveryFinishedReceiver = MyBroadFinishReceiver()
        }
        //registar los receptores de difusion,si no detecta cambiar ACTION_FOUND
        var filterUno = IntentFilter(BluetoothDevice.ACTION_NAME_CHANGED)
        // filterUno.addAction(BluetoothDevice.ACTION_NAME_CHANGED)
        var filterDos = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(discoverDevicesReceiver, filterUno)
        registerReceiver(discoveryFinishedReceiver, filterDos)
        binding.devicesList.isEnabled = false
        //Toast.makeText(binding.root.context, "Buscando en progreso..", Toast.LENGTH_SHORT).show()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(binding.root.context, "Buscando en progreso..", Toast.LENGTH_SHORT)
                .show()
            bluetoothAdapter.cancelDiscovery()
            bluetoothAdapter.startDiscovery()
        }
    }

    inner class MyBroadCastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var action = intent?.action
            if (BluetoothDevice.ACTION_NAME_CHANGED.equals(action)) {
                var device =
                    intent?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (!discoveryDevices.contains(device)) {
                    discoveryDevices.add(device!!)
                    if (ActivityCompat.checkSelfPermission(
                            binding.root.context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        nameDevices.add(device.name)
                        binding.devicesList.adapter =
                            ArrayAdapter<String>(
                                binding.root.context,
                                android.R.layout.simple_list_item_1, nameDevices
                            )
                    }
                }
            }
        }
    }

    inner class MyBroadFinishReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            binding.devicesList.isEnabled = true
            Toast.makeText(binding.root.context, "Selecciona un dispositivo", Toast.LENGTH_SHORT)
                .show()
            unregisterReceiver(this)
        }
    }

    override fun onPause() {
        super.onPause()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothAdapter.cancelDiscovery()
        }
        if (discoverDevicesReceiver != null) {
            try {
                unregisterReceiver(discoverDevicesReceiver)
            } catch (e: Exception) {
                println(e.localizedMessage)
            }
        }
        if (connectToServerThread != null) {
            try {
                connectToServerThread?.bluetoothSocket?.close()
            } catch (e: Exception) {
                println(e.localizedMessage)
            }
        }
        if (serverThread != null) {
            serverThread.cancel()
        }
    }

    override fun onResume() {
        super.onResume()
        serverThread = ServerThread(bluetoothAdapter, binding.root.context)
        serverThread.start()
    }
}