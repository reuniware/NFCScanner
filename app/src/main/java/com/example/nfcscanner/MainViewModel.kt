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

    private val _lastDetectedTag = MutableStateFlow<String?>(null)
    val lastDetectedTag: StateFlow<String?> = _lastDetectedTag.asStateFlow()

    fun setScanning(scanning: Boolean) {
        _isScanning.value = scanning
    }

    fun addDevice(serialNumber: String, techList: String, extraInfo: String) {
        viewModelScope.launch {
            nfcDeviceDao.insert(
                NfcDevice(
                    serialNumber = serialNumber,
                    techList = techList,
                    extraInfo = extraInfo
                )
            )
            _lastDetectedTag.value = serialNumber
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            nfcDeviceDao.deleteAll()
        }
    }
}
