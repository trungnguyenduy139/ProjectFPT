package com.example.trungnguyen.finalprojectfpt

/**
 * Author : Trung Nguyen
 * Create Date : 8/28/2017
 */
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

class BluetoothDevices : Activity(), OnItemClickListener {
    internal lateinit var mAdapter: ArrayAdapter<String>
    private lateinit var mListView: ListView
    private lateinit var mDevicesArray: Set<BluetoothDevice>
    internal lateinit var mPairedDevices: ArrayList<String>
    internal lateinit var mDevices: ArrayList<BluetoothDevice>
    private lateinit var mFilter: IntentFilter
    private lateinit var mReceiver: BroadcastReceiver

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bluetooth_activity)
        init()
        if (mBlueToothAdapter == null) {
            Toast.makeText(applicationContext, "Không tìm thấy bluetooth", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            if (!mBlueToothAdapter!!.isEnabled) {
                turnOnBT()
            }
            getPairedDevices()
            startDiscovery()
        }

    }

    private fun startDiscovery() {
        mBlueToothAdapter!!.cancelDiscovery()
        mBlueToothAdapter!!.startDiscovery()
    }

    private fun turnOnBT() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(intent, 3)
    }

    private fun getPairedDevices() {
        mDevicesArray = mBlueToothAdapter!!.bondedDevices
        if (mDevicesArray.isNotEmpty()) {
            for (device in mDevicesArray) {
                mPairedDevices.add(device.name)
            }
        }
    }

    private fun init() {
        mListView = findViewById(R.id.listView)
        mListView.onItemClickListener = this
        mAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, 0)
        mListView.adapter = mAdapter
        mBlueToothAdapter = BluetoothAdapter.getDefaultAdapter()
        mPairedDevices = ArrayList()
        mFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        mDevices = ArrayList()
        mReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                when (action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        mDevices.add(device)
                        val s = if (mPairedDevices.indices.any { device.name == mPairedDevices[it] }) "(Paired)" else ""
                        mAdapter.add(device.name + " " + s + " " + "\n" + device.address)
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    }
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        if (mBlueToothAdapter!!.state == BluetoothAdapter.STATE_OFF)
                            turnOnBT()
                    }
                }
            }
        }

        registerReceiver(mReceiver, mFilter)
        var filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        registerReceiver(mReceiver, filter)
        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(mReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(mReceiver)
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
        if (mBlueToothAdapter!!.isDiscovering) {
            mBlueToothAdapter!!.cancelDiscovery()
        }
        if (mAdapter.getItem(arg2)!!.contains("(Paired)")) {

            val selectedDevice = mDevices[arg2]
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
            mBlueToothAdapter!!.cancelDiscovery()
            try {
                mmSocket!!.connect()
            } catch (connectException: IOException) {
                try {
                    mmSocket!!.close()
                } catch (closeException: IOException) {
                }

                return
            }
            mHandler?.obtainMessage(SUCCESS_CONNECT, mmSocket)?.sendToTarget()
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
                    mHandler?.obtainMessage(MESSAGE_READ, bytes, -1, buffer)?.sendToTarget()
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

        internal var mHandler: Handler? = null
        internal var mConnectedThread: ConnectedThread? = null
        val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")!!
        const val SUCCESS_CONNECT = 0
        const val MESSAGE_READ = 1
        internal var mBlueToothAdapter: BluetoothAdapter? = null
    }

}