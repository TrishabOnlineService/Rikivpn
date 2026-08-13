package com.example.vpnservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.widget.Toast

class MainReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // Check if user has auto-reconnect enabled in preferences.
                // For now, we prompt or auto-connect to the best server.
                Toast.makeText(context, "MultiVPN: Ready to connect", Toast.LENGTH_SHORT).show()
            }
            ConnectivityManager.CONNECTIVITY_ACTION -> {
                // Handle network change for active connections
                if (ProtocolSelector.connectionState.value == VpnState.CONNECTED) {
                    // Signal protocol engines to re-establish connection
                }
            }
        }
    }
}
