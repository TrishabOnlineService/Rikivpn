package com.example

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.config.ServerEntry
import com.example.ui.theme.MyApplicationTheme
import com.example.vpnservice.ProtocolSelector
import com.example.vpnservice.TunnelVpnService
import com.example.vpnservice.VpnState
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.Firebase

class MainActivity : ComponentActivity() {
    private var firebaseAnalytics: FirebaseAnalytics? = null

    private var pendingServerToConnect: ServerEntry? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Permission granted, start VPN
            pendingServerToConnect?.let { server ->
                startVpn(server)
            }
        } else {
            Toast.makeText(this, "VPN Permission denied", Toast.LENGTH_SHORT).show()
        }
        pendingServerToConnect = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        try {
            firebaseAnalytics = Firebase.analytics
        } catch (e: Exception) {
            e.printStackTrace()
            // Firebase might not be fully configured without google-services.json
        }

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                
                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") {
                        com.example.ui.screens.SplashScreen(onSplashFinished = {
                            val prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                            val hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false)
                            if (hasSeenOnboarding) {
                                navController.navigate("main") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            } else {
                                navController.navigate("onboarding") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        })
                    }
                    
                    composable("onboarding") {
                        com.example.ui.screens.OnboardingScreen(onFinish = {
                            val prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                            navController.navigate("main") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        })
                    }
                    
                    composable("main") {
                        val viewModel: MainViewModel = viewModel()
                        val servers by viewModel.servers.collectAsStateWithLifecycle()
                        val vpnState by viewModel.vpnState.collectAsStateWithLifecycle()
                        
                        val rxSpeed by viewModel.rxSpeed.collectAsStateWithLifecycle()
                        val txSpeed by viewModel.txSpeed.collectAsStateWithLifecycle()
                        
                        MainScreen(
                            servers = servers,
                            vpnState = vpnState,
                            rxSpeed = rxSpeed,
                            txSpeed = txSpeed,
                            onConnect = { server -> prepareAndConnectVpn(server) },
                            onDisconnect = { 
                                viewModel.disconnect() 
                            },
                            onAddServer = { newServer ->
                                viewModel.addServer(newServer)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun prepareAndConnectVpn(server: ServerEntry) {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            pendingServerToConnect = server
            vpnPermissionLauncher.launch(vpnIntent)
        } else {
            // Already prepared
            startVpn(server)
        }
    }

    private fun startVpn(server: ServerEntry) {
        // Log event to Firebase Analytics
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, server.id)
            putString(FirebaseAnalytics.Param.ITEM_NAME, server.name)
            putString("protocol", server.protocol)
        }
        firebaseAnalytics?.logEvent("vpn_connect_attempt", bundle)

        val intent = Intent(this, TunnelVpnService::class.java)
        startService(intent)
        val builder = TunnelVpnService().Builder() 
        ProtocolSelector.connect(this, server, builder)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    servers: List<ServerEntry>,
    vpnState: VpnState,
    rxSpeed: String,
    txSpeed: String,
    onConnect: (ServerEntry) -> Unit,
    onDisconnect: () -> Unit,
    onAddServer: (ServerEntry) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { VpnTopAppBar() },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Server")
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            ConnectionStatusHeader(vpnState, rxSpeed, txSpeed, onDisconnect = onDisconnect)
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Select Server",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (servers.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No servers added. Tap + to add one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(servers, key = { it.id }) { server ->
                        ServerItem(
                            server = server,
                            isConnected = (vpnState == VpnState.CONNECTED || vpnState == VpnState.CONNECTING),
                            onConnectClick = { onConnect(server) }
                        )
                    }
                }
            }
        }
        
        if (showAddDialog) {
            AddServerDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { 
                    onAddServer(it)
                    showAddDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerDialog(onDismiss: () -> Unit, onAdd: (ServerEntry) -> Unit) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var protocol by remember { mutableStateOf("OpenVPN") }
    val protocols = listOf("OpenVPN", "Xray", "Hysteria")

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Server") },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Server Name (e.g. US East)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host / IP") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Simple protocol selection
                Text("Protocol", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    protocols.forEach { p ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.RadioButton(
                                selected = (protocol == p),
                                onClick = { protocol = p }
                            )
                            Text(p, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && host.isNotBlank()) {
                        onAdd(
                            ServerEntry(
                                id = java.util.UUID.randomUUID().toString(),
                                name = name,
                                protocol = protocol.lowercase(),
                                host = host,
                                port = 443
                            )
                        )
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnTopAppBar() {
    val context = LocalContext.current
    TopAppBar(
        title = { Text("Riki Vpn", fontWeight = FontWeight.Bold) },
        actions = {
            androidx.compose.material3.IconButton(onClick = {
                Toast.makeText(context, "Support: nitai.grp00@gmail.com", Toast.LENGTH_LONG).show()
            }) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Info,
                    contentDescription = "Support Email"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun ConnectionStatusHeader(vpnState: VpnState, rxSpeed: String, txSpeed: String, onDisconnect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val (color, text, icon) = when (vpnState) {
                VpnState.DISCONNECTED -> Triple(MaterialTheme.colorScheme.outline, "Not Connected", Icons.Default.Shield)
                VpnState.CONNECTING -> Triple(Color(0xFFFFA000), "Connecting...", Icons.Default.Shield)
                VpnState.CONNECTED -> Triple(Color(0xFF4CAF50), "Connected", Icons.Default.CheckCircle)
                VpnState.ERROR -> Triple(Color.Red, "Connection Error", Icons.Default.Warning)
            }
            
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
            
            if (vpnState == VpnState.CONNECTED) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("↓ $rxSpeed", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Download", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("↑ $txSpeed", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Upload", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Disconnect", style = MaterialTheme.typography.titleMedium)
                }
            } else if (vpnState == VpnState.CONNECTING) {
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(color = color)
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Select a server below to connect.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ServerItem(
    server: ServerEntry,
    isConnected: Boolean,
    onConnectClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnected) { onConnectClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${server.protocol.uppercase()} • ${server.host}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onConnectClick,
                enabled = !isConnected,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Connect")
            }
        }
    }
}
