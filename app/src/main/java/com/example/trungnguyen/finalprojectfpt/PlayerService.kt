package com.example.trungnguyen.finalprojectfpt

import android.annotation.TargetApi
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Messenger
import android.util.Log

import java.io.IOException

/**
 * Author : Trung Nguyen
 * Create Date : 8/28/2017
 */

class PlayerService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    var messenger = Messenger(PlayerHandler(this))
    private var isRepeatAll = false
    private var isRepeatOne = false
    internal var tempUrl: String? = null

    override fun onCreate() {
        Log.d(TAG, "onCreate")
        mediaPlayer = MediaPlayer()
        tempUrl = null
        registerReceiver(repeatReceiver, IntentFilter(
                "REPEAT"))
        registerReceiver(songUrlReceiver, IntentFilter(
                "SEND_SONG_URL"))
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        mediaPlayer!!.setOnCompletionListener { mediaPlayer ->
            Log.d(TAG, "CALL COMPLETION MEDIA")
            if (!isRepeatOne && !isRepeatAll) {
                mediaPlayer.isLooping = false
                stopSelf()
                stopForeground(true)
            }
        }
        return Service.START_NOT_STICKY
    }

    private var songUrlReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "Call onReceive send Url")
            val mp3Url: String? = intent.getStringExtra("SONG_URL")
            if (tempUrl == null || tempUrl != mp3Url) {
                val uri = Uri.parse(mp3Url)
                try {
                    mediaPlayer!!.seekTo(0)
                    mediaPlayer!!.reset()
                    mediaPlayer!!.setDataSource(applicationContext, uri)
                    mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    mediaPlayer!!.prepare()
                    mediaPlayer!!.isLooping = false
                } catch (e: IOException) {
                    Log.d(TAG, "Error do not found resource")
                }

                tempUrl = mp3Url
            }
        }
    }
    private val repeatReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            isRepeatAll = intent.getBooleanExtra("REPEAT_ALL", false)
            isRepeatOne = intent.getBooleanExtra("REPEAT_ONE", false)
            if (isRepeatOne && !isRepeatAll)
                mediaPlayer!!.isLooping = true
            else if (!isRepeatOne)
                mediaPlayer!!.isLooping = false
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind")
        return messenger.binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "onUnbind")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        unregisterReceiver(repeatReceiver)
        unregisterReceiver(songUrlReceiver)
        mediaPlayer!!.release()
    }

    // Client Methods
    val isMediaPlaying: Boolean
        get() = mediaPlayer!!.isPlaying

    fun playMedia() {
        if (!mediaPlayer!!.isPlaying) {
            mediaPlayer!!.start()
        }
    }

    fun pauseMedia() {
        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
        }
    }

    companion object {
        private val TAG = PlayerService::class.java.simpleName
    }
}
