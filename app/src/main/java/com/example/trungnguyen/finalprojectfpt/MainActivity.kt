package com.example.trungnguyen.finalprojectfpt

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.ConnectivityManager
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView

import android.widget.Toast
import android.bluetooth.BluetoothSocket
import android.os.*
import java.util.*

/**
 * Author : Trung Nguyen
 * Create Date : 8/28/2017
 */

class MainActivity : AppCompatActivity() {
    private var btSkipNext: ImageView? = null
    private var btSkipPrevious: ImageView? = null
    var imgPlayPause: ImageView? = null
    private var mBound = false
    private val mActivityMessenger = Messenger(ActivityHandler(this))
    private var mPlayerMessenger: Messenger? = null
    private var mUrl: String? = null
    private var mIndex: Int = 0
    private lateinit var mListTrack: ArrayList<String>
    private var mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            mBound = true
            val serviceIntent = Intent(this@MainActivity, PlayerService::class.java)
            startService(serviceIntent)
            val intent = Intent(INTENT_SERVICE)
            if (mUrl != null) {
                intent.putExtra(URL, mUrl)
                sendBroadcast(intent)
            }
            if (isNetworkAvailable) {
                mPlayerMessenger = Messenger(iBinder)
                val message = Message.obtain()
                message.arg1 = 2
                message.arg2 = 2
                message.replyTo = mActivityMessenger
                try {
                    mPlayerMessenger!!.send(message)
                } catch (ignored: RemoteException) {
                }

            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBound = false
        }
    }

    private var mHandler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                BluetoothDevices.Companion.SUCCESS_CONNECT -> {
                    BluetoothDevices.mConnectedThread = BluetoothDevices.ConnectedThread(msg.obj as BluetoothSocket)
                    Toast.makeText(applicationContext, "Đã Kết Nối!", Toast.LENGTH_SHORT).show()
                    BluetoothDevices.mConnectedThread!!.start()
                }
                BluetoothDevices.Companion.MESSAGE_READ -> {
                    val readBuf = msg.obj as ByteArray
                    val strIncom = String(readBuf)
                    val char = strIncom[0]
                    when (char) {
                        's' -> playOrPauseMedia()
                        'n' -> nextTrack()
                        'p' -> previousTrack()
                        else -> Toast.makeText(applicationContext, "Lệnh điều khiển chưa hợp lệ", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mListTrack = ArrayList()
        mListTrack.add("http://zmp3-mp3-s1-te-zmp3-bdhcm-1.zadn.vn/ccdf4a779033796d2022/7340335391073080988?authen=exp=1503969897~acl=/ccdf4a779033796d2022/*~hmac=0db50abfc497ecbcf547eae0a2971739")
        mListTrack.add("http://zmp3-mp3-s1-te-vnso-tn-8.zadn.vn/0084a8e771a398fdc1b2/2434791795262812451?authen=exp=1503969290~acl=/0084a8e771a398fdc1b2/*~hmac=c9f8ef989dc4809752ad712935fbddf7")
        addControls()
        mUrl = mListTrack[0]
        mIndex = 0
        BluetoothDevices.getHandler(mHandler)
    }


    private fun ButtonSkipPreviousEvent() {
        btSkipPrevious!!.setOnClickListener { view ->
            previousTrack()
        }
    }

    private fun previousTrack() {
        mIndex--
        if (mIndex < 0) mIndex = mListTrack.size - 1
        play(mIndex)
    }

    private fun ButtonSkipNextEvent() {
        btSkipNext!!.setOnClickListener { view ->
            nextTrack()
        }
    }

    private fun nextTrack() {
        mIndex++
        if (mIndex >= mListTrack.size) {
            mIndex = 0
        }
        play(mIndex)
    }

    private fun play(index: Int) {
        val intent = Intent(INTENT_SERVICE)
        mUrl = mListTrack[index]
        if (mUrl != null) {
            intent.putExtra(URL, mUrl)
            sendBroadcast(intent)
        }
        if (isNetworkAvailable) {
            val message = Message.obtain()
            message.arg1 = 2
            message.arg2 = 2
            message.replyTo = mActivityMessenger
            try {
                mPlayerMessenger!!.send(message)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

        }
    }

    private fun ButtonPlayPauseEvent() {
        imgPlayPause?.setOnClickListener { view ->
            playOrPauseMedia()
        }
    }

    private fun playOrPauseMedia() {
        if (isNetworkAvailable) {
            val message = Message.obtain()
            message.arg1 = 2
            message.arg2 = 1
            message.replyTo = mActivityMessenger
            try {
                mPlayerMessenger!!.send(message)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }


    private fun addControls() {
        imgPlayPause = findViewById<View>(R.id.btPlayPause) as ImageView
        imgPlayPause?.setBackgroundResource(R.drawable.click_effet)
        ButtonPlayPauseEvent()
        btSkipNext = findViewById<View>(R.id.btSkipNext) as ImageView
        btSkipNext!!.setBackgroundResource(R.drawable.click_effet)
        btSkipPrevious = findViewById<View>(R.id.btSkipPrevious) as ImageView
        btSkipPrevious!!.setBackgroundResource(R.drawable.click_effet)
        ButtonSkipNextEvent()
        ButtonSkipPreviousEvent()
    }

    private val isNetworkAvailable: Boolean
        get() {
            val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = manager.activeNetworkInfo
            var isAvailable = false
            if (networkInfo != null && networkInfo.isConnected) {
                isAvailable = true
            }
            return isAvailable
        }


    public override fun onPause() {
        super.onPause()
        if (mBound) {
            unbindService(mServiceConnection)
            mBound = false
        }
    }

    public override fun onStart() {
        super.onStart()
        val intent = Intent(this, PlayerService::class.java)
        if (mUrl != null)
            bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    public override fun onStop() {
        if (mBound) {
            unbindService(mServiceConnection)
            mBound = false
        }
        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item?.itemId
        when (id) {
            R.id.bt_connect -> startActivity(Intent(this, BluetoothDevices::class.java))
            R.id.bt_disconnect -> {
                try {
                    BluetoothDevices.mConnectedThread!!.cancel()
                    Toast.makeText(applicationContext, "Đã ngắt kết nối !", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(applicationContext, "Chưa kết nối !", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return true
    }

    companion object {
        const val INTENT_SERVICE = "send_track_intent"
        const val URL = "URL"
    }
}

