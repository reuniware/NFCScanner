package com.example.nfcscanner

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nfcscanner.data.NfcDevice
import com.example.nfcscanner.ui.theme.NFCScannerTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private val viewModel: MainViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device", Toast.LENGTH_LONG).show()
        }

        setContent {
            NFCScannerTheme {
                val navController = rememberNavController()
                val isScanning by viewModel.isScanning.collectAsState()

                // Effect to handle NFC Reader Mode based on ViewModel state
                LaunchedEffect(isScanning) {
                    if (isScanning) {
                        enableNfcReaderMode()
                    } else {
                        disableNfcReaderMode()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomNavigationBar(navController)
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomeScreen(viewModel)
                        }
                        composable("history") {
                            HistoryScreen(viewModel)
                        }
                    }
                }
            }
        }
    }

    private fun enableNfcReaderMode() {
        nfcAdapter?.let {
            val options = Bundle()
            // Standard flags for reading tags
            val flags = NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NFC_BARCODE
            
            it.enableReaderMode(this, this, flags, options)
        }
    }

    private fun disableNfcReaderMode() {
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag?) {
        tag?.let {
            val serialNumber = it.id.joinToString(":") { byte -> "%02X".format(byte) }
            val techList = it.techList.joinToString(", ") { tech -> tech.split(".").last() }
            
            // Collect any additional info if possible
            val extraInfo = "ID Length: ${it.id.size} bytes"
            
            viewModel.addDevice(serialNumber, techList, extraInfo)
            
            runOnUiThread {
                Toast.makeText(this, "Tag Detected: $serialNumber", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        disableNfcReaderMode()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isScanning.value) {
            enableNfcReaderMode()
        }
    }
}

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val isScanning by viewModel.isScanning.collectAsState()
    val lastDetectedTag by viewModel.lastDetectedTag.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "NFC Scanner",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.fillMaxSize(), strokeWidth = 8.dp)
                Text("Scanning...", fontWeight = FontWeight.Medium)
            } else {
                Text("Idle", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row {
            Button(
                onClick = { viewModel.setScanning(true) },
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Start Scanning")
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Button(
                onClick = { viewModel.setScanning(false) },
                enabled = isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
            ) {
                Text("Stop Scanning")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        lastDetectedTag?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Last Detected:", fontWeight = FontWeight.Bold)
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: MainViewModel) {
    val devices by viewModel.allDevices.collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Detected Devices",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            TextButton(onClick = { viewModel.clearHistory() }) {
                Text("Clear All")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (devices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No devices detected yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(devices) { device ->
                    DeviceItem(device, dateFormat)
                }
            }
        }
    }
}

@Composable
fun DeviceItem(device: NfcDevice, dateFormat: SimpleDateFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Serial: ${device.serialNumber}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = "Technologies: ${device.techList}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Extra: ${device.extraInfo}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Detected at: ${dateFormat.format(Date(device.timestamp))}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        NavigationItem("home", "Home", Icons.Default.Home),
        NavigationItem("history", "History", Icons.Default.History)
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

data class NavigationItem(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
