package com.parentcontrol.app

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseReceiver : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val action = remoteMessage.data["action"]

        val intent = Intent(this, MonitoringService::class.java)
        intent.putExtra("action", action)
        startForegroundService(intent)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token server pe save karenge baad mein
    }
}
