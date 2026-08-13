package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.ServerEntry
import com.example.config.ServerListRepository
import com.example.vpnservice.ProtocolSelector
import com.example.vpnservice.VpnState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.util.NetworkSpeedTracker

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ServerListRepository(application)
    
    val servers: StateFlow<List<ServerEntry>> = repository.servers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val vpnState: StateFlow<VpnState> = ProtocolSelector.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VpnState.DISCONNECTED)

    private val _rxSpeed = MutableStateFlow("0 B/s")
    val rxSpeed: StateFlow<String> = _rxSpeed
    
    private val _txSpeed = MutableStateFlow("0 B/s")
    val txSpeed: StateFlow<String> = _txSpeed

    init {
        viewModelScope.launch {
            repository.loadServers()
        }
        viewModelScope.launch {
            NetworkSpeedTracker.getSpeedFlow().collect { (rx, tx) ->
                _rxSpeed.value = NetworkSpeedTracker.formatSpeed(rx)
                _txSpeed.value = NetworkSpeedTracker.formatSpeed(tx)
            }
        }
    }
    
    fun disconnect() {
        ProtocolSelector.disconnect()
    }
    
    fun addServer(server: ServerEntry) {
        viewModelScope.launch {
            repository.addServer(server)
        }
    }
}
