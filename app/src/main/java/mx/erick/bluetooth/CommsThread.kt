package mx.erick.bluetooth

import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class CommsThread(var socket: BluetoothSocket) : Thread() {
    var inputStream: InputStream? = null
    var outputStram: OutputStream? = null
    init {
        inputStream = socket.inputStream
        outputStram = socket.outputStream
    }
    override fun run() {
        var buffer: ByteArray = ByteArray(1024)
        var bytes=0
        while(true){
            try{
                bytes= inputStream!!.read(buffer)
                MainActivity.iUpdate.obtainMessage(0,bytes,-1,buffer).sendToTarget()
            }catch (e:IOException){
                break
            }
        }
    }
    fun write(str:String){
        try{
            outputStram!!.write(str.toByteArray())
        }catch(e:IOException){
            println(e.localizedMessage)
        }
    }
    fun cancel(){
        try{
            socket.close()
        }catch(e:IOException){
            println(e.localizedMessage)
        }
    }
}