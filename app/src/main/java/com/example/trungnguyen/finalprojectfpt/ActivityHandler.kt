package com.example.trungnguyen.finalprojectfpt

import android.os.Handler
import android.os.Message
import android.os.RemoteException

/**
 * Author : Trung Nguyen
 * Create Date : 8/28/2017
 */
class ActivityHandler(private val mMainActivity: MainActivity) : Handler() {
    override fun handleMessage(msg: Message) {
        if (msg.arg1 == 0) {
            //Music is not playing
            val message = Message.obtain()
            message.arg1 = 0
            try {
                msg.replyTo.send(message)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

            //Change button to "Pause"
            mMainActivity.imgPlayPause?.setImageResource(R.drawable.pause_circle)
        } else if (msg.arg1 == 1) {
            //Music is playing
            val message = Message.obtain()
            message.arg1 = 1
            try {
                msg.replyTo.send(message)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

            //Change the button to "Play"
            mMainActivity.imgPlayPause?.setImageResource(R.drawable.play_circle)
        }
    }
}
