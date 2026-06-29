package com.androosio.thortune.ui

import android.content.Context
import android.os.SystemClock
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

    // When the user toggles here, suppress the JamesDSP-state poller briefly: the engine takes a
    // moment to apply and persist the new `powered_on`, and a poll landing in that window would
    // read the stale value and visibly flip the switch back.
    private var lastLocalToggleAt = 0L

    fun toggleJdsp(enabled: Boolean) {
        // Flip the switch and persist immediately so the UI feels instant; the actual engine
        // enable/disable (a blocking root operation) runs on a background thread.
        jdspEnabled = enabled
        AppSettings.setJdspEnabled(prefs, enabled)
        lastLocalToggleAt = SystemClock.elapsedRealtime()
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

    /**
     * Reconcile our toggle with JamesDSP's actual power state, so changes made through JamesDSP's
     * own switch or its Quick Settings tile show up here too. Driven by a poll while a ThorTune
     * surface is on-screen (there is no change event to subscribe to). No-op without root or the
     * Manager; skipped right after a local toggle so we don't read a not-yet-persisted value.
     */
    suspend fun syncJdspPowerFromManager() {
        if (!hasPServer || !managerInstalled) return
        if (SystemClock.elapsedRealtime() - lastLocalToggleAt < LOCAL_TOGGLE_GRACE_MS) return
        val actual = withContext(Dispatchers.IO) { JdspUtils.readManagerPowerState(context) } ?: return
        if (actual != jdspEnabled) {
            // Mirror the external change without re-driving the engine (that would echo back).
            jdspEnabled = actual
            AppSettings.setJdspEnabled(prefs, actual)
        }
    }

    fun installManager() {
        JdspUtils.installJdspManager(context)
        JdspUtils.copyBackupFile(context)
    }

    fun openManager(): Boolean = JdspUtils.openJdspManager(context)

    fun uninstallManager() = JdspUtils.uninstallJdspManager(context)

    /** Copy the recommended preset into Downloads; returns true on success. */
    fun copyRecommendedPreset(): Boolean = JdspUtils.copyRecommendedPreset(context)

    /**
     * Hand the recommended preset to JamesDSP's importer. Runs off the main thread (it primes the
     * Manager's first run and waits for it), reporting success on the main thread via [onResult].
     */
    fun importPreset(onResult: (Boolean) -> Unit) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) { JdspUtils.importPresetIntoManager(context) }
            onResult(ok)
        }
    }

    /** Update the displayed value while dragging, without touching SurfaceFlinger. */
    fun previewSaturation(value: Float) {
        saturation = SaturationUtils.quantize(value)
    }

    /** Commit a saturation value: persist it and apply it to SurfaceFlinger now. */
    fun applySaturation(value: Float) {
        val v = SaturationUtils.quantize(value)
        saturation = v
        AppSettings.setSaturation(prefs, v)
        scope.launch(Dispatchers.IO) { SaturationUtils.apply(context, v) }
    }

    fun resetSaturation() = applySaturation(AppSettings.DEFAULT_SATURATION)

    private companion object {
        /** Ignore polled JamesDSP state for this long after a local toggle (see [toggleJdsp]). */
        const val LOCAL_TOGGLE_GRACE_MS = 2500L
    }
}
