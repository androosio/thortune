package com.androosio.thortune.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * Host-side unit tests for the pure saturation helpers. These cover the exact logic behind two
 * fixed bugs: the slider committing 0.794 and the label then showing 79 instead of 80, and a
 * comma-decimal locale emitting "0,80" for SurfaceFlinger.
 */
class SaturationUtilsTest {

    @Test
    fun quantize_snapsToWholePercents() {
        // Below the half-percent rounds down, at/above rounds up — so an intended "80%" sticks.
        assertEquals(0.79f, SaturationUtils.quantize(0.794f), 1e-4f)
        assertEquals(0.80f, SaturationUtils.quantize(0.796f), 1e-4f)
        assertEquals(0.80f, SaturationUtils.quantize(0.795f), 1e-4f) // ties round up
        assertEquals(0.80f, SaturationUtils.quantize(0.8f), 1e-4f)
    }

    @Test
    fun quantize_isIdempotentOnExactPercents() {
        listOf(0.0f, 0.5f, 0.8f, 1.0f, 1.23f, 2.0f).forEach { v ->
            assertEquals(v, SaturationUtils.quantize(v), 1e-4f)
        }
    }

    @Test
    fun quantize_keepsLabelAndStoredValueInStep() {
        // The label is (value * 100).roundToInt(); after quantizing it must match the stored value.
        for (raw in generateSequence(0.0f) { it + 0.013f }.takeWhile { it <= 2.0f }) {
            val q = SaturationUtils.quantize(raw)
            val labelPercent = Math.round(q * 100)
            assertEquals(labelPercent.toFloat(), q * 100, 1e-3f)
        }
    }

    @Test
    fun format_usesDotDecimalRegardlessOfLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY) // uses comma as the decimal separator
            assertEquals("0.80", SaturationUtils.format(0.8f))
            assertEquals("1.00", SaturationUtils.format(1.0f))
            assertEquals("0.00", SaturationUtils.format(0.0f))
            assertEquals("2.00", SaturationUtils.format(2.0f))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun format_alwaysTwoDecimals() {
        assertEquals("0.80", SaturationUtils.format(SaturationUtils.quantize(0.8f)))
        assertEquals("1.20", SaturationUtils.format(1.2f))
    }
}
