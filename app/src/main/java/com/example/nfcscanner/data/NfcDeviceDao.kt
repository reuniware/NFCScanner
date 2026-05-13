package com.example.nfcscanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NfcDeviceDao {
    @Query("SELECT * FROM nfc_devices ORDER BY timestamp DESC")
    fun getAllDevices(): Flow<List<NfcDevice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: NfcDevice)

    @Query("DELETE FROM nfc_devices")
    suspend fun deleteAll()
}
