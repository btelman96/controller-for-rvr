package org.btelman.controller.rvr.activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar

import kotlinx.android.synthetic.main.activity_main.*
import org.btelman.controller.rvr.R
import org.btelman.controller.rvr.views.BLEScanSnackBarThing
import org.btelman.logutil.kotlin.LogUtil
import android.net.Uri.fromParts
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.view.*
import android.widget.SeekBar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.content_main.*
import org.btelman.controller.rvr.RVRViewModel
import org.btelman.controller.rvr.drivers.bluetooth.Connection
import org.btelman.controller.rvr.utils.DriveUtil
import org.btelman.controller.rvr.utils.RemoReceiver
import org.btelman.controller.rvr.utils.SpheroMotors
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), RemoReceiver.RemoListener {
    private var right = 0.0f
    private var left = 0.0f
    private var viewModelRVR: RVRViewModel? = null
    private var handler : Handler? = null
    private var allowPermissionClickedTime = 0L
    private var bleLayout: BLEScanSnackBarThing? = null
    private lateinit var remoInterface : RemoReceiver

    private lateinit var sharedPrefs : SharedPreferences

    private var keepScreenAwake : Boolean
        get() {
            return sharedPrefs.getBoolean("keepScreenAwake", false)
        }
        set(value) {
            sharedPrefs.edit().putBoolean("keepScreenAwake", value).apply()
        }

    private var _maxSpeed : Float? = null
    private var maxSpeed : Float
        get() {
            _maxSpeed?: run {
                _maxSpeed = sharedPrefs.getFloat("maxSpeed", .7f)
            }
            return _maxSpeed!!
        }
        set(value) {
            _maxSpeed = value
            sharedPrefs.edit().putFloat("maxSpeed", value).apply()
        }

    private var _maxTurnSpeed : Float? = null
    private var maxTurnSpeed : Float
        get() {
            _maxTurnSpeed?: run {
                _maxTurnSpeed = sharedPrefs.getFloat("maxTurnSpeed", .7f)
            }
            return _maxTurnSpeed!!
        }
        set(value) {
            _maxTurnSpeed = value
            sharedPrefs.edit().putFloat("maxTurnSpeed", value).apply()
        }

    val log = LogUtil("MainActivity")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        sharedPrefs = getSharedPreferences("RVR", Context.MODE_PRIVATE)
        if(!packageManager.hasSystemFeature(FEATURE_BLUETOOTH_LE)){
            connectionStatusView.text = "Device does not support required bluetooth mode"
            log.e { "Device does not support Bluetooth LE" }
            return
        }
        linearSpeedMaxValue.progress = (maxSpeed*100.0f).roundToInt()
        rotationSpeedMaxValue.progress = (maxTurnSpeed*100.0f).roundToInt()
        handler = Handler()
        viewModelRVR = ViewModelProviders.of(this)[RVRViewModel::class.java]
        viewModelRVR!!.connectionState.observe(this, Observer<Int> {
            connectionStatusView.text = when(it){
                Connection.STATE_ERROR -> "error"
                Connection.STATE_CONNECTING -> "connecting..."
                Connection.STATE_CONNECTED -> "connected"
                Connection.STATE_DISCONNECTED -> "disconnected"
                else -> "waiting for setup"
            }
            mainCoordinatorLayout.keepScreenOn = if(it == Connection.STATE_CONNECTED) keepScreenAwake else false
        })
        linearSpeedMaxValue.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                maxSpeed = progress/100.0f
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        rotationSpeedMaxValue.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                maxTurnSpeed = progress/100.0f
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        remoInterface = RemoReceiver(this, this)
        remoInterface.register()

        connectionStatusView.setOnClickListener {
            disconnectFromDevice()
        }
        fab.setOnClickListener { view ->
            if(bleLayout?.isShown != true){
                disconnectFromDevice()
                val ready = checkPerms()
                if(ready)
                    showScanLayout()
                else
                    showPermissionsRationale()
            }
            else{
                hideScanLayout()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        right = 0.0f
        left = 0.0f
        scheduleNewMotorLooper()
    }

    override fun onPause() {
        super.onPause()
        hideScanLayout()
        handler?.removeCallbacks(motorLooper)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu).also {
            menu?.findItem(R.id.action_keep_screen_on)?.isChecked = keepScreenAwake
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.action_privacy -> {
                startActivity(Intent(Intent.ACTION_VIEW).also {
                    it.data = Uri.parse("https://btelman.org/privacy/controller-for-rvr.html")
                })
            }
            R.id.action_keep_screen_on -> {
                val checked = !item.isChecked
                item.isChecked = checked
                keepScreenAwake = checked
                if(viewModelRVR?.connectionState?.value == Connection.STATE_CONNECTED)
                    mainCoordinatorLayout.keepScreenOn = checked
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        remoInterface.unregister()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH && resultCode == RESULT_OK) {
            showScanLayout()
        }
    }

    private fun showPermissionsRationale() {
        Snackbar.make(mainCoordinatorLayout, R.string.btPermRequestText, Snackbar.LENGTH_INDEFINITE).also {
            it.setAction("Allow"){
                allowPermissionClickedTime = System.currentTimeMillis()
                requestPerms()
            }
        }.show()
    }

    private fun checkPerms() : Boolean{
        if (Build.VERSION.SDK_INT >= 23) {
            return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED)
        }
        else return true
    }

    fun requestPerms(){
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            PERM_REQUEST_LOCATION
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERM_REQUEST_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    showScanLayout()
                } else {
                    if(System.currentTimeMillis() - allowPermissionClickedTime < 500){
                        val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                        Toast.makeText(this,
                            "Please enable the location permission in Permissions section", Toast.LENGTH_LONG).show()
                    }
                    else{
                        Toast.makeText(this,
                            "Location permission denied! Unable to scan for RVR. " +
                                    "Location permission only is used for bluetooth and data is not shared.", Toast.LENGTH_LONG).show()
                    }
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    fun showScanLayout() {
        bleLayout ?: let {
            bleLayout = BLEScanSnackBarThing.make(mainCoordinatorLayout)
        }
        if(bleLayout?.isShown != true) {
            bleLayout?.onItemClickedListener = {
                log.d { it.toString() }
                connectToDevice(it.device)
                hideScanLayout()
            }
            if(!BluetoothAdapter.getDefaultAdapter().isEnabled) {
                Snackbar.make(mainCoordinatorLayout, "Bluetooth needs to be enabled", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Turn on"){
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
                    }.show()
                return
            }
            fab.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            if(bleLayout?.isShown != true && !isFinishing)
                bleLayout?.show()
        }
    }

    private fun disconnectFromDevice(){
        viewModelRVR?.disconnect()
    }

    private fun connectToDevice(device: BluetoothDevice) {
        log.d { "connectToDevice" }
        if(viewModelRVR?.connectionState?.value == Connection.STATE_CONNECTED)
            disconnectFromDevice()
        viewModelRVR?.connect(device)
    }

    private val motorLooper = Runnable {
        sendMotorCommandFrame()
        scheduleNewMotorLooper()
    }

    private fun sendMotorCommandFrame() {
        viewModelRVR?.let { viewModel->
            if(viewModel.connectionState.value == Connection.STATE_CONNECTED){
                val axes = joystickSurfaceView.joystickAxes
                var command : ByteArray? = null
                if(axes[0] != 0.0f || axes[1] != 0.0f){
                    linearSpeed = -axes[1]*maxSpeed
                    angularSpeed = -axes[0]*maxTurnSpeed
                } else{
                    command = SpheroMotors.drive(left, right)
                }
                DriveUtil.rcDrive(linearSpeed, angularSpeed, true).also {
                    val left = it.first
                    val right = it.second
                    command = SpheroMotors.drive(left, right)
                }
                val forward =  byteArrayOf(0x31, 0x32, 0x44, 0x30, 0x53, 0x2d, 0x31)
                val backward = byteArrayOf(0x31, 0x32, 0x44, 0x31, 0x53, 0x2d, 0x31)
                val left =     byteArrayOf(0x31, 0x32, 0x44, 0x32, 0x53, 0x2d, 0x31)
                val right =    byteArrayOf(0x31, 0x32, 0x44, 0x33, 0x53, 0x2d, 0x31)
                when {
                    linearSpeed > 0f -> {
                        command = forward
                    }
                    linearSpeed < 0f -> {
                        command = backward
                    }
                    angularSpeed > 0f -> {
                        command = left
                    }
                    angularSpeed < 0f -> {
                        command = right
                    }
                }
                linearSpeed = 0f
                angularSpeed = 0f
                command?.let {
                    viewModel.sendCommand(it)
                }
            }
        }
    }

    private fun scheduleNewMotorLooper() {
        handler?.postDelayed(motorLooper, 45)
    }

    fun hideScanLayout(){
        fab.setImageResource(android.R.drawable.stat_sys_data_bluetooth)
        if(bleLayout?.isShown == true) {
            bleLayout?.dismiss()
            bleLayout?.onItemClickedListener = null
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        // Check that the event came from a game controller
        if (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK && event.action == MotionEvent.ACTION_MOVE) {
            processJoystickInput(event, -1)
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    private fun getCenteredAxis(
        event: MotionEvent,
        device: InputDevice, axis: Int, historyPos: Int
    ): Float {
        val range = device.getMotionRange(axis, event.source)

        // A joystick at rest does not always report an absolute position of
        // (0,0). Use the getFlat() method to determine the range of values
        // bounding the joystick axis center.
        if (range != null) {
            val flat = range.flat
            val value = if (historyPos < 0)
                event.getAxisValue(axis)
            else
                event.getHistoricalAxisValue(axis, historyPos)

            // Ignore axis values that are within the 'flat' region of the
            // joystick axis center.
            if (Math.abs(value) > flat) {
                return value
            }
        }
        return 0f
    }

    private fun processJoystickInput(
        event: MotionEvent,
        historyPos: Int
    ) {

        val mInputDevice = event.device

        // Calculate the vertical distance to move by
        // using the input value from one of these physical controls:
        // the left control stick, hat switch, or the right control stick.
        val linearSpeed = -getCenteredAxis(
            event, mInputDevice,
            MotionEvent.AXIS_Y, historyPos
        )
        val rotateSpeed = -getCenteredAxis(
            event, mInputDevice,
            MotionEvent.AXIS_Z, historyPos
        )
        DriveUtil.rcDrive(linearSpeed*maxSpeed, rotateSpeed*maxTurnSpeed,true).also {
            left = it.first
            right = it.second
        }
    }

    var linearSpeed : Float = 0f
    var angularSpeed : Float = 0f

    override fun onCommand(command: String) {
        when (command.replace("\r\n", "")) {
            "f" -> {
                linearSpeed = 1f
                angularSpeed = 0f
            }
            "b" -> {
                linearSpeed = -1f
                angularSpeed = 0f
            }
            "r" -> {
                linearSpeed = 0f
                angularSpeed = -1f
            }
            "l" -> {
                linearSpeed = 0f
                angularSpeed = 1f
            }
            else -> {
                linearSpeed = 0f
                angularSpeed = 0f
            }
        }
    }

    companion object {
        private const val PERM_REQUEST_LOCATION = 234
        private const val REQUEST_ENABLE_BLUETOOTH = 2221
    }
}
