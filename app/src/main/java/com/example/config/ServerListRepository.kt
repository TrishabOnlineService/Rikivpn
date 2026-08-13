package com.example.config

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ServerListRepository(private val context: Context) {
    private val _servers = MutableStateFlow<List<ServerEntry>>(emptyList())
    val servers: StateFlow<List<ServerEntry>> = _servers.asStateFlow()
    
    private val prefs = context.getSharedPreferences("vpn_servers", Context.MODE_PRIVATE)

    suspend fun loadServers() {
        withContext(Dispatchers.IO) {
            try {
                // 1. Load manually added servers from SharedPreferences
                val jsonString = prefs.getString("server_list", "[]") ?: "[]"
                val manualList = Json { ignoreUnknownKeys = true }.decodeFromString<List<ServerEntry>>(jsonString)
                
                // 2. Load and parse .ovpn files from the assets/ovpn folder
                val assetList = mutableListOf<ServerEntry>()
                try {
                    val assetFiles = context.assets.list("ovpn") ?: emptyArray()
                    for (fileName in assetFiles) {
                        if (fileName.endsWith(".ovpn", ignoreCase = true)) {
                            val inputStream = context.assets.open("ovpn/$fileName")
                            val content = inputStream.bufferedReader().use { it.readText() }
                            
                            // Parse remote host and port
                            var host = "unknown"
                            var port = 443
                            val remoteRegex = Regex("""^remote\s+([^\s]+)\s+(\d+)""", RegexOption.MULTILINE)
                            val match = remoteRegex.find(content)
                            if (match != null) {
                                host = match.groupValues[1]
                                port = match.groupValues[2].toIntOrNull() ?: 443
                            }
                            
                            // Format name from filename
                            val name = fileName.removeSuffix(".ovpn")
                                .replace(Regex("[^a-zA-Z0-9]"), " ")
                                .split(" ")
                                .filter { it.isNotBlank() }
                                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                                
                            assetList.add(
                                ServerEntry(
                                    id = "asset_$fileName",
                                    name = if (name.isNotBlank()) name else fileName,
                                    protocol = "openvpn",
                                    host = host,
                                    port = port
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // Combine and update
                _servers.value = assetList + manualList
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    suspend fun addServer(server: ServerEntry) {
        withContext(Dispatchers.IO) {
            val currentList = _servers.value.toMutableList()
            currentList.add(server)
            _servers.value = currentList
            
            val jsonString = Json.encodeToString(currentList)
            prefs.edit().putString("server_list", jsonString).apply()
        }
    }
}
