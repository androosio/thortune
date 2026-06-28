package com.androosio.thortune.utils

import android.content.Context
import android.content.Intent

object JdspUtils {
    const val JDSP_PACKAGE_NAME = "james.dsp"

    /**
     * Copy the bundled JamesDSP preset backup into the Downloads folder so the user can
     * import it from inside the JamesDSP Manager app.
     */
    fun copyBackupFile(context: Context) {
        val backupFile = "app/support/conf_files/jamesdsp_backup_o2ptweaks.backup"
        val outFile = FileUtils.getPathDownload("/jamesdsp_backup.tar.gz")
        FileUtils.copyAsset(context, backupFile, outFile)
    }

    /** Filename of the recommended preset once copied into the Downloads folder. */
    const val RECOMMENDED_PRESET_FILENAME = "Joeys_Retro_Handhelds.tar"

    /**
     * Copy Joey's Retro Handhelds recommended JamesDSP preset into the Downloads folder so the
     * user can load it from inside the JamesDSP Manager app. Returns true on success.
     */
    fun copyRecommendedPreset(context: Context): Boolean {
        val asset = "app/support/conf_files/joeys_retro_handhelds_preset.tar"
        val outFile = FileUtils.getPathDownload("/$RECOMMENDED_PRESET_FILENAME")
        return FileUtils.copyAsset(context, asset, outFile)
    }

    /**
     * Temporary-root path: mount the JamesDSP audio_effects config and start the engine.
     * Returns true if the root operation was dispatched (the PServer binder accepted it).
     */
    fun enableJdsp(context: Context): Boolean =
        RootUtils.runRootScript(context, "jdsp.enable.sh") != null

    fun disableJdsp(context: Context): Boolean =
        RootUtils.runRootScript(context, "jdsp.disable.sh") != null

    /**
     * True when the JamesDSP Manager is actually present **and usable** — i.e. it has a launchable
     * activity. We deliberately don't use a bare `getPackageInfo` here: a package record can exist
     * while the app is disabled or its launcher is hidden (seen in the wild with the rootless
     * JamesDSP build), in which case it reports "installed" yet can't be opened and the engine
     * can't be configured. Launchability is the honest signal, so the UI gates on this.
     */
    fun isManagerInstalled(context: Context): Boolean = launchIntent(context) != null

    /** Resolve the Manager's launch intent, or null if it isn't installed/enabled/launchable. */
    private fun launchIntent(context: Context): Intent? {
        val pm = context.packageManager
        pm.getLaunchIntentForPackage(JDSP_PACKAGE_NAME)?.let { return it }
        // Fallback: resolve the MAIN/LAUNCHER activity ourselves and target it explicitly.
        val info = pm.resolveActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(JDSP_PACKAGE_NAME),
            0,
        )?.activityInfo ?: return null
        return Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(info.packageName, info.name)
    }

    /** Launch the JamesDSP Manager UI. Returns false if it isn't installed/launchable. */
    fun openJdspManager(context: Context): Boolean {
        val intent = launchIntent(context) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }

    /** Install the bundled JamesDSP Manager APK (user confirms via the system installer). */
    fun installJdspManager(context: Context) {
        ApkUtils.installApkFromAssets(context, "JamesDSPManagerThePBone.apk", "app")
    }
}
