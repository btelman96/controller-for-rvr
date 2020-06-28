package org.btelman.controller.rvr.utils

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import org.btelman.controller.rvr.drivers.bluetooth.BluetoothBuilder
import org.btelman.controller.rvr.drivers.bluetooth.Connection
import org.btelman.controller.rvr.drivers.bluetooth.le.BluetoothGattInterface
import org.btelman.logutil.kotlin.LogUtil
import java.util.*

/**
 * Created by Brendon on 12/7/2019.
 */
class RVRManager(context: Context, device : BluetoothDevice){
    val log = LogUtil("RVRManager")
    private val RVR_MAIN_SERVICE = UUID.fromString("000000f3-0000-1000-8000-00805f9b34fb")
    private val RVR_COMMS_CHAR = UUID.fromString("0000fff5-0000-1000-8000-00805f9b34fb")

    val bluetooth = BluetoothBuilder(context, device, BluetoothBuilder.TYPE_GATT_LE).build() as BluetoothGattInterface

    private val onCommCharacteristicUpdate =  { characteristic: BluetoothGattCharacteristic ->

    }

    init {
        bluetooth.subscribe(RVR_MAIN_SERVICE, RVR_COMMS_CHAR, onCommCharacteristicUpdate)
    }

    fun connect(){
        bluetooth.connect()
    }

    fun disconnect(){
        bluetooth.disconnect()
    }

    fun send(packet : ByteArray) {
        // Are we connected?
        if (bluetooth.getStatus() != Connection.STATE_CONNECTED)
            return
        bluetooth.writeBytes(RVR_MAIN_SERVICE, RVR_COMMS_CHAR, packet)
    }
}