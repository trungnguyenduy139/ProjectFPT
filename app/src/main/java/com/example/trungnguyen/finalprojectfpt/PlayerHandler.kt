package com.example.trungnguyen.finalprojectfpt

import android.os.Handler
import android.os.Message
import android.os.RemoteException

/**
 * Author : Trung Nguyen
 * Create Date : 8/28/2017
 */

class PlayerHandler(private val mPlayerService: PlayerService) : Handler() {
    override fun handleMessage(msg: Message) {
        when (msg.arg1) {
            0//Play media
            -> mPlayerService.playMedia()
            1 //Pause media
            -> mPlayerService.pauseMedia()
            2 // isPlaying
            -> {
                val isPlaying = if (mPlayerService.isMediaPlaying) 1 else 0
                val message = Message.obtain()
                if (isPlaying == 1 && msg.arg2 == 1)
                // Music is playing and user click play button
                    message.arg1 = 1
                else if (isPlaying == 1 && msg.arg2 == 2)
                //Music is playing and user change track
                    message.arg1 = 0
                else
                    message.arg1 = isPlaying //Music is not playing and user start new track
                message.replyTo = mPlayerService.messenger
                try {
                    msg.replyTo.send(message)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
        }
    }
}