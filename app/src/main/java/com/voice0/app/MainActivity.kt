package com.voice0.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.voice0.app.ui.HomeScreen
import com.voice0.app.ui.theme.Voice0Theme
import com.voice0.app.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var resultSender: ActivityResultSender

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Required for MWA — must be created before setContent so result hooks register.
        resultSender = ActivityResultSender(this)

        // Mic permission upfront. If denied, recording will fail with a clear message.
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1001)

        setContent {
            Voice0Theme {
                val state by viewModel.state.collectAsState()
                HomeScreen(
                    state = state,
                    onCluster = viewModel::setCluster,
                    onConnectWallet = { viewModel.connectWallet(resultSender) },
                    onPressIn = viewModel::startRecording,
                    onPressOut = { viewModel.stopRecordingAndProcess(resultSender) },
                    onSubmitText = { viewModel.submitTextIntent(resultSender, it) },
                    onConfirm = { viewModel.confirmAndSign(resultSender) },
                    onReset = viewModel::resetToIdle,
                )
            }
        }
    }
}
