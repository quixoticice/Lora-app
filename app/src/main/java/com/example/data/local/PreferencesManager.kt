package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ConnectionMode
import com.example.model.RadioConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lora_prefs", Context.MODE_PRIVATE)

    private val _callsignFlow = MutableStateFlow(getCallsign())
    val callsignFlow: StateFlow<String> = _callsignFlow

    private val _modeFlow = MutableStateFlow(getConnectionMode())
    val modeFlow: StateFlow<ConnectionMode> = _modeFlow

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean("onboarding_completed", false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
    }

    fun getCallsign(): String {
        return prefs.getString("user_callsign", "")?.ifEmpty { "OPERATOR-01" } ?: "OPERATOR-01"
    }

    fun setCallsign(callsign: String) {
        val clean = callsign.trim().uppercase()
        prefs.edit().putString("user_callsign", clean).apply()
        _callsignFlow.value = clean
    }

    fun getConnectionMode(): ConnectionMode {
        val raw = prefs.getString("connection_mode", ConnectionMode.DEMO_SIMULATION.name)
        return try {
            ConnectionMode.valueOf(raw ?: ConnectionMode.DEMO_SIMULATION.name)
        } catch (e: Exception) {
            ConnectionMode.DEMO_SIMULATION
        }
    }

    fun setConnectionMode(mode: ConnectionMode) {
        prefs.edit().putString("connection_mode", mode.name).apply()
        _modeFlow.value = mode
    }

    fun getRadioConfig(): RadioConfig {
        return RadioConfig(
            frequencyMHz = prefs.getFloat("radio_freq", 433.0f).toDouble(),
            spreadingFactor = prefs.getInt("radio_sf", 7),
            bandwidthKHz = prefs.getFloat("radio_bw", 125.0f).toDouble(),
            codingRate = prefs.getString("radio_cr", "4/5") ?: "4/5",
            txPowerDbm = prefs.getInt("radio_power", 20),
            wsUrl = prefs.getString("ws_url", "ws://192.168.4.1/ws") ?: "ws://192.168.4.1/ws",
            btDeviceAddress = prefs.getString("bt_address", "") ?: "",
            btDeviceName = prefs.getString("bt_name", "") ?: "",
            autoReconnect = prefs.getBoolean("auto_reconnect", true)
        )
    }

    fun saveRadioConfig(config: RadioConfig) {
        prefs.edit()
            .putFloat("radio_freq", config.frequencyMHz.toFloat())
            .putInt("radio_sf", config.spreadingFactor)
            .putFloat("radio_bw", config.bandwidthKHz.toFloat())
            .putString("radio_cr", config.codingRate)
            .putInt("radio_power", config.txPowerDbm)
            .putString("ws_url", config.wsUrl)
            .putString("bt_address", config.btDeviceAddress)
            .putString("bt_name", config.btDeviceName)
            .putBoolean("auto_reconnect", config.autoReconnect)
            .apply()
    }
}
