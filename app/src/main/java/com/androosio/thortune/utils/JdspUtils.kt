package com.androosio.thortune.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

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
     * Hand the recommended preset straight to JamesDSP's importer, skipping the manual
     * menu → Presets → Add → Import dance. JamesDSP's MainActivity registers an ACTION_VIEW
     * filter for `content://…*.tar`, so we copy the preset out (also a Downloads fallback),
     * wrap it in a FileProvider URI, and fire it at the package. Returns true if JamesDSP
     * accepted the intent (it must be installed and launchable — i.e. the engine is on).
     */
    fun importPresetIntoManager(context: Context): Boolean {
        if (!copyRecommendedPreset(context)) return false
        val file = File(FileUtils.getPathDownload("/$RECOMMENDED_PRESET_FILENAME"))
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setData(uri)
            .setPackage(JDSP_PACKAGE_NAME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    /**
     * Temporary-root path: mount the JamesDSP audio_effects config and start the engine.
     * Returns true if the root operation was dispatched (the PServer binder accepted it).
     */
    fun enableJdsp(context: Context): Boolean =
        RootUtils.runRootScript(context, "jdsp.enable.sh") != null

    fun disableJdsp(context: Context): Boolean =
        RootUtils.runRootScript(context, "jdsp.disable.sh") != null

    /** True when the JamesDSP Manager package is installed (regardless of whether it's openable). */
    fun isManagerInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(JDSP_PACKAGE_NAME, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /**
     * Resolve the Manager's launch intent, or null if it can't currently be opened. The rootless
     * JamesDSP only exposes a launchable activity while its engine/service is running, so this is
     * null when the engine is off even though the package is installed — hence it gates the "Open"
     * action only, not install/engine state.
     */
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

    /** Launch the system uninstall dialog for the JamesDSP Manager (user confirms). */
    fun uninstallJdspManager(context: Context) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$JDSP_PACKAGE_NAME"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
