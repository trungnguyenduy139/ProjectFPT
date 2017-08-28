package com.example.trungnguyen.finalprojectfpt

/**
 * Author : Trung Nguyen
 * Create Date : 8/28/2017
 */
import android.annotation.SuppressLint
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.ArrayList
import java.util.UUID

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
@SuppressLint("MissingPermission")
class BluetoothDevices : Activity(), OnItemClickListener {
    internal lateinit var listAdapter: ArrayAdapter<String>
    private lateinit var listView: ListView
    private lateinit var devicesArray: Set<BluetoothDevice>
    internal lateinit var pairedDevices: ArrayList<String>
    internal lateinit var devices: ArrayList<BluetoothDevice>
    private lateinit var filter: IntentFilter
    private lateinit var receiver: BroadcastReceiver

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bluetooth_activity)
        init()
        if (btAdapter == null) {
            Toast.makeText(applicationContext, "Không tìm thấy bluetooth", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            if (!btAdapter!!.isEnabled) {
                turnOnBT()
            }
            getPairedDevices()
            startDiscovery()
        }

    }


    private fun startDiscovery() {
        btAdapter!!.cancelDiscovery()
        btAdapter!!.startDiscovery()
    }

    private fun turnOnBT() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(intent, 3)
    }

    private fun getPairedDevices() {
        devicesArray = btAdapter!!.bondedDevices
        if (devicesArray.isNotEmpty()) {
            for (device in devicesArray) {
                pairedDevices.add(device.name)
            }
        }
    }

    private fun init() {
        listView = findViewById(R.id.listView)
        listView.onItemClickListener = this
        listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, 0)
        listView.adapter = listAdapter
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        pairedDevices = ArrayList()
        filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        devices = ArrayList()
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (BluetoothDevice.ACTION_FOUND == action) {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    devices.add(device)
                    val s = if (pairedDevices.indices.any { device.name == pairedDevices[it] }) "(Paired)" else ""

//                    for (a in pairedDevices.indices) {
//                        if (device.name == pairedDevices[a]) {
//                            //append
//                            s = "(Paired)"
//                            break
//                        }
//                    }

                    listAdapter.add(device.name + " " + s + " " + "\n" + device.address)

                } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED == action) {

                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {

                } else if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                    if (btAdapter!!.state == btAdapter!!.state) {
                        turnOnBT()
                    }
                }
            }

        }

        registerReceiver(receiver, filter)
        var filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        registerReceiver(receiver, filter)
        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(receiver, filter)
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            Log.d("Exception: ", e.message)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(applicationContext, "Cần bật bluetooth để tiếp tục", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onItemClick(arg0: AdapterView<*>, arg1: View, arg2: Int, arg3: Long) {
        if (btAdapter!!.isDiscovering) {
            btAdapter!!.cancelDiscovery()
        }
        if (listAdapter.getItem(arg2)!!.contains("(Paired)")) {

            val selectedDevice = devices[arg2]
            val connect = ConnectThread(selectedDevice)
            connect.start()
        } else {
            Toast.makeText(applicationContext, "Thiết bị chưa được pair", Toast.LENGTH_SHORT).show()
        }
    }

    private inner class ConnectThread(mmDevice: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket?

        init {
            var tmp: BluetoothSocket? = null
            try {
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID)
            } catch (e: IOException) {
            }

            mmSocket = tmp
        }

        override fun run() {
            btAdapter!!.cancelDiscovery()
            try {
                mmSocket!!.connect()
            } catch (connectException: IOException) {
                try {
                    mmSocket!!.close()
                } catch (closeException: IOException) {
                }

                return
            }
            mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket).sendToTarget()
        }
    }

    internal class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
                tmpIn = mmSocket.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
            }

            mmInStream = tmpIn
            mmOutStream = tmpOut
        }

        override fun run() {

            var buffer: ByteArray
            var bytes: Int

            while (true) {
                try {
                    try {
                        Thread.sleep(30)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                    buffer = ByteArray(1024)
                    bytes = mmInStream!!.read(buffer)
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget()
                } catch (ignored: IOException) {
                }

            }
        }
        fun cancel() {
            try {
                mmSocket.close()
            } catch (ignored: IOException) {
            }

        }
    }

    companion object {

        fun getHandler(handler: Handler) {
            mHandler = handler
        }

        internal var mHandler = Handler()
        internal var connectedThread: ConnectedThread? = null
        val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")!!
        val SUCCESS_CONNECT = 0
        val MESSAGE_READ = 1
        internal var btAdapter: BluetoothAdapter? = null
    }

}