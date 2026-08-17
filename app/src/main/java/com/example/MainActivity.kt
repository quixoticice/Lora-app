package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ConnectionModal
import com.example.ui.FirmwareModal
import com.example.ui.MainChatScreen
import com.example.ui.NodeListModal
import com.example.ui.OnboardingScreen
import com.example.ui.PacketLogModal
import com.example.ui.theme.LoRaDarkBg
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                RequestRuntimePermissions()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LoRaDarkBg
                ) {
                    LoRaApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun RequestRuntimePermissions() {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        launcher.launch(permissions.toTypedArray())
    }
}

@Composable
fun LoRaApp(viewModel: MainViewModel) {
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    val initialCallsign by viewModel.callsign.collectAsStateWithLifecycle()
    val initialMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val radioConfig by viewModel.radioConfig.collectAsStateWithLifecycle()
    val discoveredNodes by viewModel.discoveredNodes.collectAsStateWithLifecycle()
    val packetLogs by viewModel.recentPacketLogs.collectAsStateWithLifecycle()

    var showNodesModal by remember { mutableStateOf(false) }
    var showFirmwareModal by remember { mutableStateOf(false) }
    var showLogsModal by remember { mutableStateOf(false) }
    var showSettingsModal by remember { mutableStateOf(false) }

    if (!isOnboardingCompleted) {
        OnboardingScreen(
            initialCallsign = initialCallsign,
            initialMode = initialMode,
            onComplete = { callsign, mode ->
                viewModel.completeOnboarding(callsign, mode)
            }
        )
    } else {
        MainChatScreen(
            viewModel = viewModel,
            onOpenNodesModal = { showNodesModal = true },
            onOpenFirmwareModal = { showFirmwareModal = true },
            onOpenLogsModal = { showLogsModal = true },
            onOpenSettingsModal = { showSettingsModal = true }
        )

        if (showNodesModal) {
            NodeListModal(
                discoveredNodes = discoveredNodes,
                onSelectNode = { callsign -> viewModel.setRecipient(callsign) },
                onDismiss = { showNodesModal = false }
            )
        }

        if (showFirmwareModal) {
            FirmwareModal(
                firmwareCode = viewModel.getEspFirmwareSnippet(),
                onDismiss = { showFirmwareModal = false }
            )
        }

        if (showLogsModal) {
            PacketLogModal(
                packetLogs = packetLogs,
                onDismiss = { showLogsModal = false }
            )
        }

        if (showSettingsModal) {
            ConnectionModal(
                currentCallsign = initialCallsign,
                currentMode = initialMode,
                currentConfig = radioConfig,
                connectionManager = viewModel.connectionManager,
                onSaveCallsign = { viewModel.updateCallsign(it) },
                onSaveMode = { viewModel.updateMode(it) },
                onSaveConfig = { viewModel.updateRadioConfig(it) },
                onClearChatHistory = { viewModel.clearChatHistory() },
                onDismiss = { showSettingsModal = false }
            )
        }
    }
}
