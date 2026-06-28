package com.androosio.thortune.utils

import android.content.Context
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Applies the display colour saturation through SurfaceFlinger, routed over the manufacturer's
 * PServer binder via [RootUtils] (the same privileged channel JamesDSP uses — there is no `su`).
 *
 * Two mechanisms are used together:
 *  1. The persisted system property [PROP_SATURATION], which SurfaceFlinger reads itself at boot,
 *     so the setting survives reboots without us re-issuing anything.
 *  2. An immediate runtime SurfaceFlinger transaction so the change is visible right away.
 *
 * The float is always formatted with [Locale.US] so a comma-decimal device locale can't turn
 * "0.80" into "0,80" and feed SurfaceFlinger an unparseable argument.
 *
 * Adapted from ThorSaturation (which derived its SurfaceFlinger handling from OdinTools, MIT).
 */
object SaturationUtils {
    const val PROP_SATURATION = "persist.sys.sf.color_saturation"

    /** SurfaceFlinger transaction code for setting colour saturation on AYN firmware. */
    const val SATURATION_TRANSACTION = "1022"

    const val MIN_SATURATION = 0.0f
    const val MAX_SATURATION = 2.0f

    /** True when the privileged PServer binder is available (saturation can be changed). */
    fun isSupported(): Boolean = RootUtils.hasPServer()

    /** Persist the value and apply it immediately. Use from the UI while the system is fully up. */
    fun apply(context: Context, value: Float) {
        val formatted = format(value)
        RootUtils.runRootCommand(context, "setprop $PROP_SATURATION $formatted")
        RootUtils.runRootCommand(context, "service call SurfaceFlinger $SATURATION_TRANSACTION f $formatted")
    }

    /** Persist only. SurfaceFlinger picks this up on the next boot. */
    fun persist(context: Context, value: Float) {
        RootUtils.runRootCommand(context, "setprop $PROP_SATURATION ${format(value)}")
    }

    /**
     * Snap a saturation value to whole-percent steps. The UI slider is continuous, so without this
     * a drag commits an arbitrary float (e.g. 0.794) that the "%" label rounds down to 79 — making
     * a value the user set as "80" reappear as 79. Quantizing keeps the stored value and label in
     * step. `internal` for unit testing.
     */
    internal fun quantize(value: Float): Float = (value * 100).roundToInt() / 100f

    /** Format with [Locale.US] so a comma-decimal locale can't emit "0,80". `internal` for tests. */
    internal fun format(value: Float) = String.format(Locale.US, "%.2f", value)
}
