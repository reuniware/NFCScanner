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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CompareArrows
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private val viewModel: MainViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null
    private var mifareKeys: List<ByteArray> = emptyList()
    private val foundKeysCache = mutableSetOf<String>()

    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { importScanFromFile(it) }
    }

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
                            HistoryScreen(viewModel) {
                                importLauncher.launch("text/plain")
                            }
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
            
            // Mode RESTAURATION
            val restoreData = viewModel.pendingRestore.value
            if (restoreData != null) {
                runOnUiThread { Toast.makeText(this, "Restauration en cours...", Toast.LENGTH_SHORT).show() }
                val results = mutableListOf<String>()
                val mifare = MifareClassic.get(it)
                try {
                    mifare?.connect()
                    mifare?.timeout = 5000
                    restoreData.split(";").forEach { blockInfo ->
                        results.add(writeMifareBlock(it, blockInfo))
                    }
                } catch (e: Exception) {
                    results.add("Erreur connection: ${e.localizedMessage}")
                } finally {
                    try { mifare?.close() } catch (e: Exception) {}
                }

                viewModel.setPendingRestore(null) // Reset après tentative
                runOnUiThread { 
                    Toast.makeText(this, "Résultat : ${results.last()}", Toast.LENGTH_LONG).show()
                }
                return@let // On s'arrête là pour ne pas ré-enregistrer le scan
            }

            var content = readNdefContent(it)
            var rawData: String? = null
            
            // Si pas de NDEF ou vide, et que c'est du Mifare Classic, on tente les clés
            if ((content == "Pas de données NDEF" || content == "NDEF Vide") && it.techList.contains(MifareClassic::class.java.name)) {
                val result = readMifareClassicWithRawData(it)
                if (result.first.isNotEmpty()) {
                    content = result.first
                    rawData = result.second
                }
            }

            val extraInfo = "ID Length: ${it.id.size} bytes"
            
            viewModel.addDevice(serialNumber, techList, extraInfo, content, rawData)
            saveScanToDownload(serialNumber, content, rawData)
            
            runOnUiThread {
                Toast.makeText(this, "Tag Detected: $serialNumber", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveScanToDownload(serialNumber: String, content: String, rawData: String?) {
        try {
            val fileName = "NFC_Scan_${serialNumber.replace(":", "")}_${System.currentTimeMillis()}.txt"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            
            FileOutputStream(file).use { fos ->
                val data = StringBuilder()
                data.append("Serial: $serialNumber\n\n")
                data.append(content)
                if (rawData != null) {
                    data.append("\n\n--- INTERNAL RAW DATA (DO NOT MODIFY) ---\n")
                    data.append(rawData)
                }
                fos.write(data.toString().toByteArray())
            }
            Log.d("NFCScanner", "File saved: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("NFCScanner", "Error saving file", e)
        }
    }

    private fun importScanFromFile(uri: android.net.Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val text = inputStream.bufferedReader().readText()
                val serialMatch = Regex("Serial: ([0-9A-F:]+)").find(text)
                val serial = serialMatch?.groupValues?.get(1) ?: "Unknown"
                
                val rawDataSection = text.split("--- INTERNAL RAW DATA (DO NOT MODIFY) ---")
                if (rawDataSection.size > 1) {
                    val rawData = rawDataSection[1].trim()
                    val content = rawDataSection[0].replace("Serial: $serial", "").trim()
                    
                    viewModel.addDevice(serial, "Imported Mifare", "Imported from file", content, rawData)
                    Toast.makeText(this, "Scan importé avec succès", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Fichier invalide : Données brutes manquantes", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur import : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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

    private fun readMifareClassicWithRawData(tag: Tag): Pair<String, String?> {
        val mifare = MifareClassic.get(tag) ?: return Pair("", null)
        val sb = StringBuilder()
        val rawDataList = mutableListOf<String>()
        
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

                val keysToTest = mutableListOf<ByteArray>()
                foundKeysCache.forEach { keysToTest.add(hexToByteArray(it)) }
                mifareKeys.forEach { key ->
                    val hex = key.joinToString("") { "%02X".format(it) }
                    if (!foundKeysCache.contains(hex)) {
                        keysToTest.add(key)
                    }
                }

                for (key in keysToTest) {
                    val keyHex = key.joinToString("") { "%02X".format(it) }
                    if (mifare.authenticateSectorWithKeyA(i, key)) {
                        try {
                            mifare.readBlock(mifare.sectorToBlock(i))
                            authenticated = true
                            authKeyType = "A"
                            usedKey = keyHex
                            foundKeysCache.add(keyHex)
                            break 
                        } catch (e: Exception) {}
                    }
                    if (mifare.authenticateSectorWithKeyB(i, key)) {
                        try {
                            mifare.readBlock(mifare.sectorToBlock(i))
                            authenticated = true
                            authKeyType = "B"
                            usedKey = keyHex
                            foundKeysCache.add(keyHex)
                            break
                        } catch (e: Exception) {}
                    }
                }
                
                if (authenticated) {
                    sb.append("Sector $i: Authenticated (Key $authKeyType: $usedKey)\n")
                    val blockCount = mifare.getBlockCountInSector(i)
                    val firstBlock = mifare.sectorToBlock(i)
                    for (j in 0 until blockCount) {
                        val blockIndex = firstBlock + j
                        try {
                            val data = mifare.readBlock(blockIndex)
                            val hexData = data.joinToString("") { "%02X".format(it) }
                            val asciiData = String(data).map { if (it in ' '..'~') it else '.' }.joinToString("")
                            sb.append("  Block $blockIndex: $hexData [$asciiData]\n")
                            
                            // On ne sauvegarde QUE les blocs de données (pas le bloc 0, pas les trailers)
                            val isTrailer = (blockIndex + 1) % 4 == 0
                            val isManufacturer = blockIndex == 0
                            if (!isTrailer && !isManufacturer) {
                                rawDataList.add("$blockIndex:$hexData:$authKeyType:$usedKey")
                            }
                            
                            // Mise en évidence du Bloc 37 (Candidat Solde)
                            val blockPrefix = if (blockIndex == 37) "⭐ Block" else "  Block"
                            sb.append("$blockPrefix $blockIndex: $hexData [$asciiData]\n")
                        } catch (e: Exception) {
                            sb.append("  Block $blockIndex: Error reading (Permissions restricted)\n")
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
        
        return Pair(sb.toString(), if (rawDataList.isEmpty()) null else rawDataList.joinToString(";"))
    }

    private fun writeMifareBlock(tag: Tag, blockData: String): String {
        // blockData format: "index:hexData:keyType:keyHex"
        val parts = blockData.split(":")
        if (parts.size < 4) return "Format invalide"
        
        val blockIndex = parts[0].toInt()
        val data = hexToByteArray(parts[1])
        val keyType = parts[2]
        val key = hexToByteArray(parts[3])
        val sectorIndex = blockIndex / 4

        val mifare = MifareClassic.get(tag) ?: return "Incompatible"
        try {
            if (!mifare.isConnected) {
                mifare.connect()
                mifare.timeout = 5000
            }
            
            val auth = if (keyType == "A") {
                mifare.authenticateSectorWithKeyA(sectorIndex, key)
            } else {
                mifare.authenticateSectorWithKeyB(sectorIndex, key)
            }

            return if (auth) {
                mifare.writeBlock(blockIndex, data)
                Log.d("NFCScanner", "Write success: Block $blockIndex")
                "Succès Bloc $blockIndex"
            } else {
                "Auth échouée Secteur $sectorIndex"
            }
        } catch (e: Exception) {
            Log.e("NFCScanner", "Write error: Block $blockIndex", e)
            return "Erreur B$blockIndex: ${e.localizedMessage}"
        }
        // Note: On ne ferme pas la connexion ici car writeMifareBlock est souvent appelé dans une boucle
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
fun HistoryScreen(viewModel: MainViewModel, onImportClick: () -> Unit) {
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
                text = "History",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row {
                TextButton(onClick = onImportClick) {
                    Text("Import")
                }
                TextButton(onClick = { viewModel.clearHistory() }) {
                    Text("Clear")
                }
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
                    DeviceItem(device, dateFormat, viewModel)
                }
            }
        }
    }
}

@Composable
fun DeviceItem(device: NfcDevice, dateFormat: SimpleDateFormat, viewModel: MainViewModel) {
    val selectedForCompare by viewModel.selectedForCompare.collectAsState()
    val isSelectedForCompare = selectedForCompare?.id == device.id
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(if (isSelectedForCompare) 8.dp else 2.dp),
        border = if (isSelectedForCompare) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Serial: ${device.serialNumber}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = dateFormat.format(Date(device.timestamp)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                
                Row {
                    // Bouton Comparer
                    if (device.rawData != null) {
                        IconButton(
                            onClick = {
                                if (selectedForCompare == null) {
                                    viewModel.setSelectedForCompare(device)
                                } else if (selectedForCompare?.id == device.id) {
                                    viewModel.setSelectedForCompare(null)
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.CompareArrows,
                                contentDescription = "Compare",
                                tint = if (isSelectedForCompare) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Bouton Restaurer si des données brutes existent
                    if (device.rawData != null) {
                        val isPending = viewModel.pendingRestore.collectAsState().value == device.rawData
                        Button(
                            onClick = {
                                if (isPending) {
                                    viewModel.setPendingRestore(null)
                                } else {
                                    viewModel.setPendingRestore(device.rawData)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPending) 
                                    Color(0xFFFFA500) else MaterialTheme.colorScheme.secondary
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(if (isPending) "Cancel" else "Restore", fontSize = 12.sp)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded || isSelectedForCompare) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Technologies: ${device.techList}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Extra: ${device.extraInfo}", style = MaterialTheme.typography.bodySmall)
                    
                    // Affichage de la comparaison si un autre badge est sélectionné
                    if (selectedForCompare != null && selectedForCompare?.id != device.id && device.rawData != null && selectedForCompare?.rawData != null) {
                        ComparisonView(selectedForCompare!!, device)
                    } else if (device.content.isNotEmpty()) {
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
                    
                    if (viewModel.pendingRestore.collectAsState().value == device.rawData) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "⚠️ Mode Restauration activé. Activez le scan et présentez le badge pour réécrire les données.",
                            color = Color.Red,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonView(oldDevice: NfcDevice, newDevice: NfcDevice) {
    val oldBlocks = oldDevice.rawData?.split(";") ?: emptyList()
    val newBlocks = newDevice.rawData?.split(";") ?: emptyList()
    
    val diffs = mutableListOf<String>()
    
    // Comparaison bloc par bloc
    newBlocks.forEach { newBlockStr ->
        val parts = newBlockStr.split(":")
        if (parts.size >= 2) {
            val index = parts[0]
            val newData = parts[1]
            
            val oldBlockStr = oldBlocks.find { it.startsWith("$index:") }
            val oldData = oldBlockStr?.split(":")?.getOrNull(1)
            
            if (oldData != null && oldData != newData) {
                val prefix = if (index == "37") "💰 " else ""
                diffs.add("$prefix Block $index: $oldData -> $newData")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(Color(0xFFFFEBEE), MaterialTheme.shapes.small)
            .padding(8.dp)
    ) {
        Text("Différences détectées :", fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 12.sp)
        if (diffs.isEmpty()) {
            Text("Aucune différence dans les données de secteurs.", fontSize = 11.sp)
        } else {
            diffs.forEach { diff ->
                Text(diff, fontSize = 11.sp, color = Color.DarkGray)
            }
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
