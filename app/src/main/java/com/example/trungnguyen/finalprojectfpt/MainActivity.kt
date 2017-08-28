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

import java.util.ArrayList
import android.widget.Toast
import android.bluetooth.BluetoothSocket
import android.os.*


/**
 * Author : Trung Nguyen
 * Create Date : 8/28/2017
 */

class MainActivity : AppCompatActivity() {
    private var btnRepeat: ImageView? = null
    private var btSkipNext: ImageView? = null
    private var btSkipPrevious: ImageView? = null
    var imgPlayPause: ImageView? = null
    private var mBound = false
    private val activityMessenger = Messenger(ActivityHandler(this))
    private var playerMessenger: Messenger? = null
    private var songUrl: String? = null
    private var songIndex: Int = 0
    private lateinit var mSongList: ArrayList<String>
    private var serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            mBound = true
            val startServiceIntent = Intent(this@MainActivity, PlayerService::class.java)
            startService(startServiceIntent)
            val songUrlIntent = Intent("SEND_SONG_URL")
            if (songUrl != null) {
                songUrlIntent.putExtra("SONG_URL", songUrl)
                sendBroadcast(songUrlIntent)
            }
            if (isNetworkAvailable) {
                playerMessenger = Messenger(iBinder)
                val message = Message.obtain()
                message.arg1 = 2
                message.arg2 = 2
                message.replyTo = activityMessenger
                try {
                    playerMessenger!!.send(message)
                } catch (e: RemoteException) {
                    e.printStackTrace()
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
                Bluetooth.Companion.SUCCESS_CONNECT -> {
                    Bluetooth.connectedThread = Bluetooth.ConnectedThread(msg.obj as BluetoothSocket)
                    Toast.makeText(applicationContext, "Connected!", Toast.LENGTH_SHORT).show()
                    Bluetooth.connectedThread!!.start()
                }
                Bluetooth.Companion.MESSAGE_READ -> {
                    val readBuf = msg.obj as ByteArray
                    val strIncom = String(readBuf)
                    val char = strIncom[0]
                    when (char) {
                        's' -> playOrPauseMedia()
                        'n' -> nextTrack()
                        'p' -> previousTrack()
                    }
                }
            }
        }

    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mSongList = ArrayList()
        mSongList.add("http://zmp3-mp3-s1-te-vnso-tn-8.zadn.vn/8739deea06aeeff0b6bf/1243431489925204133?authen=exp=1503947405~acl=/8739deea06aeeff0b6bf/*~hmac=508081dea8cb04e2c7ad44da3698e85a")
        mSongList.add("http://zmp3-mp3-s1-te-zmp3-fpthcm-1.zadn.vn/fc6afd732537cc699526/3124316109290043920?authen=exp=1503947878~acl=/fc6afd732537cc699526/*~hmac=fe923789774c9f2c0c6478cf5b6b6dd3")
        mSongList.add("http://zmp3-mp3-s1-te-zmp3-bdhcm-1.zadn.vn/c56bfc772433cd6d9422/6711005331051776182?authen=exp=1503946615~acl=/c56bfc772433cd6d9422/*~hmac=f23146a3f0c8aa2193d9bfcd278d7cf1")
        mSongList.add("http://zmp3-mp3-s1-te-zmp3-bdhcm-1.zadn.vn/d6ac9d8c44c8ad96f4d9/7646434173735821454?authen=exp=1503948189~acl=/d6ac9d8c44c8ad96f4d9/*~hmac=e13139b051743377c24c2d7edf298cc5")
        addControls()
        songUrl = mSongList[0]
        songIndex = 0
        Bluetooth.getHandler(mHandler)
    }


    private fun ButtonSkipPreviousEvent() {
        btSkipPrevious!!.setOnClickListener { view ->
            previousTrack()
        }
    }

    private fun previousTrack(){
        songIndex--
        if (songIndex < 0) songIndex = mSongList.size - 1
        play(songIndex)
    }

    private fun ButtonSkipNextEvent() {
        btSkipNext!!.setOnClickListener { view ->
           nextTrack()
        }
    }

    private fun nextTrack(){
        songIndex++
        if (songIndex >= mSongList.size) {
            songIndex = 0
        }
        play(songIndex)
    }

    private fun play(index: Int) {
        val songUrlIntent = Intent("SEND_SONG_URL")
        songUrl = mSongList[index]
        if (songUrl != null) {
            songUrlIntent.putExtra("SONG_URL", songUrl)
            sendBroadcast(songUrlIntent)
        }
        if (isNetworkAvailable) {
            val message = Message.obtain()
            message.arg1 = 2
            message.arg2 = 2
            message.replyTo = activityMessenger
            try {
                playerMessenger!!.send(message)
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

    private fun playOrPauseMedia(){
        if (isNetworkAvailable) {
                val message = Message.obtain()
                message.arg1 = 2
                message.arg2 = 1
                message.replyTo = activityMessenger
                try {
                    playerMessenger!!.send(message)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
    }


    private fun addControls() {
        btnRepeat = findViewById<View>(R.id.btRepeat) as ImageView
        btnRepeat!!.setImageResource(R.drawable.btn_playback_repeat)
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
            unbindService(serviceConnection)
            mBound = false
        }
    }

    public override fun onStart() {
        super.onStart()
        val intent = Intent(this, PlayerService::class.java)
        if (songUrl != null)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    public override fun onStop() {
        if (mBound) {
            unbindService(serviceConnection)
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
            R.id.bt_connect -> startActivity(Intent(this, Bluetooth::class.java))
            R.id.bt_disconnect -> {
                try {
                    Bluetooth.connectedThread!!.cancel()
                    Toast.makeText(applicationContext, "disconected !", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(applicationContext, "not connected !", Toast.LENGTH_LONG).show()
                }

            }
        }
        return true
    }
}

