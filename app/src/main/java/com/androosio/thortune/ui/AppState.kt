package com.androosio.thortune.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.androosio.thortune.AppSettings
import com.androosio.thortune.utils.JdspUtils
import com.androosio.thortune.utils.RootUtils
import com.androosio.thortune.utils.SaturationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Single source of truth for the UI. Created once by [com.androosio.thortune.MainActivity].
 * Holds Compose snapshot state so any change recomposes the screen automatically.
 */
class AppState(private val context: Context) {

    private val prefs = AppSettings.getSharedPrefs(context)

    // Privileged work (root scripts over the PServer binder) is blocking, so run it off the main
    // thread to keep the UI responsive. Lives for the activity's lifetime alongside this state.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Manufacturer PServer binder present — the privileged channel both features rely on. */
    val hasPServer: Boolean = RootUtils.hasPServer()

    val saturationSupported: Boolean = SaturationUtils.isSupported()

    var jdspEnabled by mutableStateOf(AppSettings.getJdspEnabled(prefs))
        private set
    var saturation by mutableFloatStateOf(AppSettings.getSaturation(prefs))
        private set
    var managerInstalled by mutableStateOf(JdspUtils.isManagerInstalled(context))
        private set

    /** Whether the live quick-controls panel is mirrored onto the Thor's lower screen. */
    var secondaryPanelEnabled by mutableStateOf(AppSettings.getSecondaryPanelEnabled(prefs))
        private set

    fun setPanelEnabled(enabled: Boolean) {
        secondaryPanelEnabled = enabled
        AppSettings.setSecondaryPanelEnabled(prefs, enabled)
    }

    /** Re-check install state — call when the app returns to the foreground. */
    fun refreshInstallState() {
        managerInstalled = JdspUtils.isManagerInstalled(context)
    }

    fun toggleJdsp(enabled: Boolean) {
        // Flip the switch and persist immediately so the UI feels instant; the actual engine
        // enable/disable (a blocking root operation) runs on a background thread.
        jdspEnabled = enabled
        AppSettings.setJdspEnabled(prefs, enabled)
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                if (enabled) JdspUtils.enableJdsp(context) else JdspUtils.disableJdsp(context)
            }
            // If the root operation didn't go through, snap the switch back to its real state.
            if (!ok) {
                jdspEnabled = !enabled
                AppSettings.setJdspEnabled(prefs, !enabled)
            }
        }
    }

    fun installManager() {
        JdspUtils.installJdspManager(context)
        JdspUtils.copyBackupFile(context)
    }

    fun openManager(): Boolean = JdspUtils.openJdspManager(context)

    /** Copy the recommended preset into Downloads; returns true on success. */
    fun copyRecommendedPreset(): Boolean = JdspUtils.copyRecommendedPreset(context)

    /** Hand the recommended preset to JamesDSP's importer; returns true if it opened. */
    fun importPreset(): Boolean = JdspUtils.importPresetIntoManager(context)

    /** Update the displayed value while dragging, without touching SurfaceFlinger. */
    fun previewSaturation(value: Float) {
        saturation = quantize(value)
    }

    /** Commit a saturation value: persist it and apply it to SurfaceFlinger now. */
    fun applySaturation(value: Float) {
        val v = quantize(value)
        saturation = v
        AppSettings.setSaturation(prefs, v)
        scope.launch(Dispatchers.IO) { SaturationUtils.apply(context, v) }
    }

    fun resetSaturation() = applySaturation(AppSettings.DEFAULT_SATURATION)

    /**
     * Snap to whole-percent steps. The slider is continuous, so without this a drag commits an
     * arbitrary float (e.g. 0.794) that the "%" label then rounds down to 79 — making a value the
     * user set as "80" reappear as 79. Quantizing keeps the stored value and the label in lockstep.
     */
    private fun quantize(value: Float): Float = (value * 100).roundToInt() / 100f
}
