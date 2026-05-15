package com.example.nfcscanner

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.Ndef
import android.os.Bundle
import android.os.Environment
import android.util.Log
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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private val viewModel: MainViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null
    private var mifareKeys: List<ByteArray> = emptyList()
    private val foundKeysCache = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device", Toast.LENGTH_LONG).show()
        }

        mifareKeys = loadKeysFromAssets()

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
            
            var content = readNdefContent(it)
            
            // Si pas de NDEF ou vide, et que c'est du Mifare Classic, on tente les clés
            if ((content == "Pas de données NDEF" || content == "NDEF Vide") && it.techList.contains(MifareClassic::class.java.name)) {
                val mifareContent = readMifareClassicContent(it)
                if (mifareContent.isNotEmpty()) {
                    content = mifareContent
                }
            }

            val extraInfo = "ID Length: ${it.id.size} bytes"
            
            viewModel.addDevice(serialNumber, techList, extraInfo, content)
            saveScanToDownload(serialNumber, content)
            
            runOnUiThread {
                Toast.makeText(this, "Tag Detected: $serialNumber", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveScanToDownload(serialNumber: String, content: String) {
        try {
            val fileName = "NFC_Scan_${serialNumber.replace(":", "")}_${System.currentTimeMillis()}.txt"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            
            FileOutputStream(file).use { fos ->
                val data = "Serial: $serialNumber\n\n$content"
                fos.write(data.toByteArray())
            }
            Log.d("NFCScanner", "File saved: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("NFCScanner", "Error saving file", e)
        }
    }

    private fun loadKeysFromAssets(): List<ByteArray> {
        val keys = mutableSetOf<String>()
        val files = listOf("std.keys", "hotel-std.keys", "extended-std.keys")
        
        files.forEach { fileName ->
            try {
                assets.open(fileName).bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val cleanLine = line.trim()
                        if (cleanLine.isNotEmpty() && !cleanLine.startsWith("#") && cleanLine.length == 12) {
                            keys.add(cleanLine.uppercase())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("NFCScanner", "Error loading $fileName", e)
            }
        }
        
        return keys.map { hexToByteArray(it) }
    }

    private fun hexToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
        }
        return data
    }

    private fun readMifareClassicContent(tag: Tag): String {
        val mifare = MifareClassic.get(tag) ?: return ""
        val sb = StringBuilder()
        
        // On vide le cache des clés trouvées pour un nouveau tag physique
        // ou on le garde si on veut accélérer les scans de badges identiques à la suite
        // Restons sur un cache par session d'application pour maximiser la vitesse.
        
        try {
            mifare.connect()
            mifare.timeout = 5000 
            val sectorCount = mifare.sectorCount
            sb.append("Mifare Classic (${mifare.size} bytes)\n")
            
            for (i in 0 until sectorCount) {
                if (!mifare.isConnected) {
                    try { mifare.connect() } catch (e: Exception) {
                        sb.append("Connection lost at Sector $i\n")
                        break
                    }
                }
                var authenticated = false
                var authKeyType = ""
                var usedKey = ""

                // 1. On prépare la liste des clés à tester
                // On met en priorité les clés qui ont déjà fonctionné (Cache)
                val keysToTest = mutableListOf<ByteArray>()
                foundKeysCache.forEach { keysToTest.add(hexToByteArray(it)) }
                
                // On ajoute les clés du dictionnaire (en évitant les doublons avec le cache)
                mifareKeys.forEach { key ->
                    val hex = key.joinToString("") { "%02X".format(it) }
                    if (!foundKeysCache.contains(hex)) {
                        keysToTest.add(key)
                    }
                }

                // 2. On essaie de trouver une clé qui permet la LECTURE
                for (key in keysToTest) {
                    val keyHex = key.joinToString("") { "%02X".format(it) }
                    
                    // Test Clé A
                    if (mifare.authenticateSectorWithKeyA(i, key)) {
                        try {
                            mifare.readBlock(mifare.sectorToBlock(i))
                            authenticated = true
                            authKeyType = "A"
                            usedKey = keyHex
                            foundKeysCache.add(keyHex) // On mémorise la clé gagnante
                            break 
                        } catch (e: Exception) {
                        }
                    }
                    
                    // Test Clé B
                    if (mifare.authenticateSectorWithKeyB(i, key)) {
                        try {
                            mifare.readBlock(mifare.sectorToBlock(i))
                            authenticated = true
                            authKeyType = "B"
                            usedKey = keyHex
                            foundKeysCache.add(keyHex) // On mémorise la clé gagnante
                            break
                        } catch (e: Exception) {
                        }
                    }
                }
                
                if (authenticated) {
                    sb.append("Sector $i: Authenticated (Key $authKeyType: $usedKey)\n")
                    val blockCount = mifare.getBlockCountInSector(i)
                    val firstBlock = mifare.sectorToBlock(i)
                    for (j in 0 until blockCount) {
                        try {
                            val data = mifare.readBlock(firstBlock + j)
                            val hexData = data.joinToString("") { "%02X".format(it) }
                            val asciiData = String(data).map { if (it in ' '..'~') it else '.' }.joinToString("")
                            sb.append("  Block ${firstBlock + j}: $hexData [$asciiData]\n")
                        } catch (e: Exception) {
                            sb.append("  Block ${firstBlock + j}: Error reading (Permissions restricted)\n")
                        }
                    }
                } else {
                    sb.append("Sector $i: Authentication failed or Read access denied\n")
                }
            }
            mifare.close()
        } catch (e: Exception) {
            sb.append("Mifare Error: ${e.localizedMessage}")
        } finally {
            try { mifare.close() } catch (e: Exception) {}
        }
        
        return sb.toString()
    }

    private fun readNdefContent(tag: Tag): String {
        val ndef = Ndef.get(tag) ?: return "Pas de données NDEF"
        return try {
            ndef.connect()
            val ndefMessage = ndef.ndefMessage
            ndef.close()
            ndefMessage?.records?.joinToString("\n") { record ->
                val payload = record.payload
                if (payload.isNotEmpty()) {
                    // Pour les records TEXT, le premier octet contient le statut (longueur du code langue)
                    // On simplifie ici pour afficher le contenu lisible
                    val textEncoding = if ((payload[0].toInt() and 128) == 0) "UTF-8" else "UTF-16"
                    val languageCodeLength = payload[0].toInt() and 63
                    String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, charset(textEncoding))
                } else ""
            } ?: "NDEF Vide"
        } catch (e: Exception) {
            "Erreur de lecture : ${e.localizedMessage}"
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

        lastDetectedTag?.let { device ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Dernière détection :", fontWeight = FontWeight.Bold)
                    Text("ID: ${device.serialNumber}", style = MaterialTheme.typography.bodyLarge)
                    if (device.content.isNotEmpty()) {
                        Text("Contenu: ${device.content}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
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
            if (device.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Contenu:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = device.content,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
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
