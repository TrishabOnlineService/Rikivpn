package com.example.vpnservice

import android.content.Intent
import android.net.VpnService

class TunnelVpnService : VpnService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_DISCONNECT) {
            ProtocolSelector.disconnect()
            stopSelf()
            return START_NOT_STICKY
        }
        
        // Return START_STICKY so service is restarted if killed by system
        return START_STICKY
    }

    override fun onDestroy() {
        ProtocolSelector.disconnect()
        super.onDestroy()
    }

    companion object {
        const val ACTION_DISCONNECT = "com.example.vpnservice.DISCONNECT"
    }
}
