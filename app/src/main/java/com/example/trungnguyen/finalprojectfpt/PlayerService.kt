package com.example.trungnguyen.finalprojectfpt

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.os.Messenger
import android.util.Log

import java.io.IOException

/**
 * Author : Trung Nguyen
 * Create Date : 8/28/2017
 */

class PlayerService : Service() {
    private var mMediaPlayer: MediaPlayer? = null
    internal var mMessenger = Messenger(PlayerHandler(this))
    private var mUrl: String? = null

    override fun onCreate() {
        mMediaPlayer = MediaPlayer()
        mUrl = null
        registerReceiver(songUrlReceiver, IntentFilter(
                MainActivity.Companion.INTENT_SERVICE))
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        mMediaPlayer!!.setOnCompletionListener { mediaPlayer ->
            mediaPlayer.isLooping = false
            stopSelf()
            stopForeground(true)
        }
        return Service.START_NOT_STICKY
    }

    private var songUrlReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val url: String? = intent.getStringExtra(MainActivity.Companion.URL)
            if (mUrl == null || mUrl != url) {
                val uri = Uri.parse(url)
                try {
                    mMediaPlayer!!.seekTo(0)
                    mMediaPlayer!!.reset()
                    mMediaPlayer!!.setDataSource(applicationContext, uri)
                    mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    mMediaPlayer!!.prepare()
                    mMediaPlayer!!.isLooping = false
                } catch (e: IOException) {
                    Log.d(TAG, e.message)
                }
                mUrl = url
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return mMessenger.binder
    }

    override fun onDestroy() {
        unregisterReceiver(songUrlReceiver)
        mMediaPlayer!!.release()
    }

    // Client Methods
    val isMediaPlaying: Boolean
        get() = mMediaPlayer!!.isPlaying

    fun playMedia() {
        if (!mMediaPlayer!!.isPlaying) {
            mMediaPlayer!!.start()
        }
    }

    fun pauseMedia() {
        if (mMediaPlayer!!.isPlaying) {
            mMediaPlayer!!.pause()
        }
    }

    companion object {
        private val TAG = PlayerService::class.java.simpleName
    }
}
