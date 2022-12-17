package mx.erick.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class BroadCastBTB: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        var state= intent?.extras?.getInt(BluetoothAdapter.EXTRA_STATE)
        when(state){
            BluetoothAdapter.STATE_TURNING_OFF->{
                Toast.makeText(context , "En proceso de apagado", Toast.LENGTH_SHORT).show()
            }
            BluetoothAdapter.STATE_TURNING_ON->{
                Toast.makeText(context , "En proceso de encendido", Toast.LENGTH_SHORT).show()
            }
            BluetoothAdapter.STATE_OFF->{
                Toast.makeText(context , "Apagado", Toast.LENGTH_SHORT).show()
            }
            BluetoothAdapter.STATE_ON->{
                Toast.makeText(context , "Prendido", Toast.LENGTH_SHORT).show()
            }
        }
    }
}