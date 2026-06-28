package com.androosio.thortune.secondary

import android.app.Presentation
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.androosio.thortune.ui.AppState
import com.androosio.thortune.ui.CompanionPanel
import com.androosio.thortune.ui.theme.AppTheme

/**
 * Hosts the [CompanionPanel] Compose UI on the Thor's lower (presentation) screen. The ComposeView
 * borrows the host [activity] as its lifecycle / saved-state / view-model owner — a Presentation is
 * a Dialog on another display and has no owners of its own, so Compose needs these wired manually.
 */
class CompanionPresentation(
    private val activity: ComponentActivity,
    display: Display,
    private val appState: AppState,
) : Presentation(activity, display) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(activity)
            setViewTreeViewModelStoreOwner(activity)
            setViewTreeSavedStateRegistryOwner(activity)
            setContent {
                AppTheme { CompanionPanel(appState) }
            }
        }
        setContentView(composeView)
    }
}
