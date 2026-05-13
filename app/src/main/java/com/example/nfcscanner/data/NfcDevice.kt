package com.example.nfcscanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nfc_devices")
data class NfcDevice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serialNumber: String,
    val techList: String,
    val timestamp: Long = System.currentTimeMillis(),
    val extraInfo: String = ""
)
