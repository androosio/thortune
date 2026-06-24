package com.androosio.jamesdsptweaks.utils

import android.content.Context
import android.content.Intent

object JdspUtils {
    const val JDSP_PACKAGE_NAME = "james.dsp"

    /** Magisk module id of the bundled jdsp_v6.4-trimmed.zip (see its module.prop). */
    const val MAGISK_MODULE_ID = "ainur_jamesdsp"

    /**
     * Copy the bundled JamesDSP preset backup into the Downloads folder so the user can
     * import it from inside the JamesDSP Manager app.
     */
    fun copyBackupFile(context: Context) {
        val backupFile = "app/support/conf_files/jamesdsp_backup_o2ptweaks.backup"
        val outFile = FileUtils.getPathDownload("/jamesdsp_backup.tar.gz")
        FileUtils.copyAsset(context, backupFile, outFile)
    }

    /** Temporary-root path: mount the JamesDSP audio_effects config and start the engine. */
    fun enableJdsp(context: Context) {
        RootUtils.runRootScript(context, "jdsp.enable.sh")
    }

    fun disableJdsp(context: Context) {
        RootUtils.runRootScript(context, "jdsp.disable.sh")
    }

    fun hasJdspPackage(context: Context): Boolean {
        return RootUtils.isPackageInstalled(context, JDSP_PACKAGE_NAME)
    }

    /**
     * True if the JamesDSP Magisk module is already present. Requires root, so this issues a
     * root command; returns false on non-rooted devices or if the check fails.
     */
    fun isMagiskModuleInstalled(context: Context): Boolean {
        val result = RootUtils.runRootCommand(
            context,
            "[ -d /data/adb/modules/$MAGISK_MODULE_ID ] && echo 1 || echo 0"
        )
        return result?.trim() == "1"
    }

    /** Launch the installed JamesDSP Manager UI. Returns false if it isn't installed. */
    fun openJdspManager(context: Context): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(JDSP_PACKAGE_NAME)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }

    /** Install the bundled JamesDSP Manager APK (user confirms via the system installer). */
    fun installJdspManager(context: Context) {
        ApkUtils.installApkFromAssets(context, "JamesDSPManagerThePBone.apk", "app")
    }

    /** Permanent-root path: install the JamesDSP Magisk module (requires reboot). */
    fun installJdspMagiskModule(context: Context): Boolean {
        RootUtils.runRootScript(context, "jdsp.magisk.sh")
        return true
    }
}
