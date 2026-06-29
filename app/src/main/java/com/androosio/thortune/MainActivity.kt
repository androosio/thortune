package com.androosio.thortune

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.androosio.thortune.secondary.CompanionPresentation
import com.androosio.thortune.ui.AppState
import com.androosio.thortune.ui.MainScreen
import com.androosio.thortune.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var appState: AppState
    private lateinit var displayManager: DisplayManager
    private var presentation: CompanionPresentation? = null

    // The Thor's lower screen can appear/disappear (e.g. it sleeps); keep the panel in sync.
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = refreshCompanionPanel()
        override fun onDisplayRemoved(displayId: Int) = refreshCompanionPanel()
        override fun onDisplayChanged(displayId: Int) = refreshCompanionPanel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appState = AppState(this)
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        setContent {
            AppTheme { MainScreen(appState) }
        }

        // Re-evaluate the panel whenever the user flips the Settings toggle.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                snapshotFlow { appState.secondaryPanelEnabled }.collect { refreshCompanionPanel() }
            }
        }

        // Keep our engine toggle in step with JamesDSP's own switch / Quick Settings tile.
        // JamesDSP emits no cross-process change event, so we poll its persisted state while a
        // ThorTune surface is on-screen — the main UI (STARTED) or the companion panel, which can
        // stay up on the lower screen while another app is foreground (STOPPED but panel showing).
        // lifecycleScope cancels this at onDestroy.
        lifecycleScope.launch {
            while (true) {
                val visible = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) ||
                    presentation?.isShowing == true
                if (visible) appState.syncJdspPowerFromManager()
                delay(JDSP_SYNC_INTERVAL_MS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appState.refreshInstallState()
        displayManager.registerDisplayListener(displayListener, null)
        refreshCompanionPanel()
    }

    override fun onPause() {
        super.onPause()
        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissCompanionPanel()
    }

    /** Show the quick-controls panel on the presentation display, or tear it down if unavailable. */
    private fun refreshCompanionPanel() {
        val display = displayManager
            .getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            .firstOrNull()

        if (appState.secondaryPanelEnabled && display != null) {
            val existing = presentation
            if (existing == null || existing.display?.displayId != display.displayId || !existing.isShowing) {
                dismissCompanionPanel()
                presentation = CompanionPresentation(this, display, appState).also { panel ->
                    runCatching { panel.show() }
                }
            }
        } else {
            dismissCompanionPanel()
        }
    }

    private fun dismissCompanionPanel() {
        presentation?.let { runCatching { it.dismiss() } }
        presentation = null
    }

    private companion object {
        /** How often to poll JamesDSP's power state while a ThorTune surface is visible. */
        const val JDSP_SYNC_INTERVAL_MS = 2000L
    }
}
