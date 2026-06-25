package com.androosio.thortune

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.androosio.thortune.ui.AppState
import com.androosio.thortune.ui.MainScreen
import com.androosio.thortune.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    private lateinit var appState: AppState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appState = AppState(this)
        setContent {
            AppTheme { MainScreen(appState) }
        }
    }

    override fun onResume() {
        super.onResume()
        appState.refreshInstallState()
    }
}
