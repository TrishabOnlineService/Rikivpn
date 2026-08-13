package com.example.util

import android.net.TrafficStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object NetworkSpeedTracker {
    fun getSpeedFlow(): Flow<Pair<Long, Long>> = flow {
        var lastRx = TrafficStats.getTotalRxBytes()
        var lastTx = TrafficStats.getTotalTxBytes()
        
        while (true) {
            delay(1000)
            val currentRx = TrafficStats.getTotalRxBytes()
            val currentTx = TrafficStats.getTotalTxBytes()
            
            val rxSpeed = if (currentRx == TrafficStats.UNSUPPORTED.toLong()) 0L else currentRx - lastRx
            val txSpeed = if (currentTx == TrafficStats.UNSUPPORTED.toLong()) 0L else currentTx - lastTx
            
            lastRx = currentRx
            lastTx = currentTx
            
            emit(Pair(rxSpeed.coerceAtLeast(0), txSpeed.coerceAtLeast(0)))
        }
    }
    
    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec < 1024) return "$bytesPerSec B/s"
        val kb = bytesPerSec / 1024.0
        if (kb < 1024) return String.format("%.1f KB/s", kb)
        val mb = kb / 1024.0
        return String.format("%.2f MB/s", mb)
    }
}
