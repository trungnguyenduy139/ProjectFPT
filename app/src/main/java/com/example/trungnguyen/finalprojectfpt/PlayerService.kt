package com.example.trungnguyen.finalprojectfpt

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
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
    private var mResId: Int? = null

    override fun onCreate() {
        registerReceiver(mReceiver, IntentFilter(
                MainActivity.Companion.INTENT_SERVICE))
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return Service.START_NOT_STICKY
    }

    private var mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val resId: Int? = intent.getIntExtra(MainActivity.Companion.URL, 0)
            if (mResId != resId) {
                if (mMediaPlayer != null) {
                    mMediaPlayer!!.release()
                    mMediaPlayer = null
                }
                mMediaPlayer = MediaPlayer.create(applicationContext, resId!!)
                try {
                    mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    mMediaPlayer!!.start()
                    mMediaPlayer!!.isLooping = true
                } catch (e: IOException) {
                    Log.d(TAG, e.message)
                }
                mResId = resId
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? = mMessenger.binder

    override fun onDestroy() {
        unregisterReceiver(mReceiver)
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
