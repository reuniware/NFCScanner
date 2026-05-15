package com.example.nfcscanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfcscanner.data.AppDatabase
import com.example.nfcscanner.data.NfcDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val nfcDeviceDao = db.nfcDeviceDao()

    val allDevices = nfcDeviceDao.getAllDevices()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _lastDetectedTag = MutableStateFlow<NfcDevice?>(null)
    val lastDetectedTag: StateFlow<NfcDevice?> = _lastDetectedTag.asStateFlow()

    private val _pendingRestore = MutableStateFlow<String?>(null)
    val pendingRestore: StateFlow<String?> = _pendingRestore.asStateFlow()

    private val _selectedForCompare = MutableStateFlow<NfcDevice?>(null)
    val selectedForCompare: StateFlow<NfcDevice?> = _selectedForCompare.asStateFlow()

    fun setScanning(scanning: Boolean) {
        _isScanning.value = scanning
    }

    fun setSelectedForCompare(device: NfcDevice?) {
        _selectedForCompare.value = device
    }

    fun setPendingRestore(rawData: String?) {
        _pendingRestore.value = rawData
    }

    fun addDevice(serialNumber: String, techList: String, extraInfo: String, content: String, rawData: String? = null) {
        viewModelScope.launch {
            val device = NfcDevice(
                serialNumber = serialNumber,
                techList = techList,
                extraInfo = extraInfo,
                content = content,
                rawData = rawData
            )
            nfcDeviceDao.insert(device)
            _lastDetectedTag.value = device
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            nfcDeviceDao.deleteAll()
        }
    }
}
