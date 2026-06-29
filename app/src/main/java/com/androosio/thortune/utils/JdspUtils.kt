package com.androosio.thortune.utils

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object JdspUtils {
    const val JDSP_PACKAGE_NAME = "james.dsp"

    // JamesDSP's own master power control. Its in-app switch, its Quick Settings tile, and this
    // broadcast all funnel through the same `toggleEnginePower` path and persist the same
    // `powered_on` preference — so driving the engine through this broadcast keeps ThorTune's
    // toggle in lockstep with JamesDSP's own controls (instead of fighting them). The receiver is
    // exported and unprotected; the boolean extra is applied verbatim (true = on, false = off).
    private const val POWER_ACTION = "me.timschneeberger.rootlessjamesdsp.SET_POWER_STATE"
    private const val POWER_EXTRA = "rootlessjamesdsp.enabled"
    private const val POWER_RECEIVER = "me.timschneeberger.rootlessjamesdsp.receiver.PowerStateReceiver"

    /** File holding JamesDSP's persisted app preferences, including `powered_on`. */
    private val PREFS_PATH = "/data/data/$JDSP_PACKAGE_NAME/shared_prefs/application.xml"
    private val POWERED_ON_REGEX = Regex("""name="powered_on"\s+value="(true|false)"""")

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
        val filesDir = "/storage/emulated/0/Android/data/$JDSP_PACKAGE_NAME/files"

        // Each root command is issued separately: the PServer channel runs them directly, so
        // ';'-chained compound commands (and pm grant inside them) don't reliably take.
        // 1. Pre-grant notifications so the Manager's first-run prompt never blocks/interrupts us.
        RootUtils.runRootCommand(context, "pm grant $JDSP_PACKAGE_NAME android.permission.POST_NOTIFICATIONS")
        // 2. The Manager copies the imported .tar into its own Presets folder but doesn't create
        //    that folder, so a fresh import fails with ENOENT. Create it and make it writable by
        //    the Manager's uid (root creates it root-owned, and the uid changes on reinstall).
        //    We deliberately do NOT create the Liveprog folder: the Manager's preset loader writes
        //    the embedded liveprog there itself, and a root-owned Liveprog folder makes that write
        //    throw — which it then reports via a dialog on a background thread and crashes.
        RootUtils.runRootCommand(context, "mkdir -p $filesDir/Presets")
        RootUtils.runRootCommand(context, "chmod 0777 $filesDir $filesDir/Presets")

        // Prime first run: launching MainActivity makes the Manager initialise and extract its
        // built-in resources (incl. liveprog scripts) so the import isn't racing first-run. With
        // notifications already granted this runs silently. ~1s in practice; wait a safe margin.
        val launch = launchIntent(context) ?: return false
        context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        Thread.sleep(2500)

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
     * Turn the engine on. The root script ensures the effect is loaded (mounting the
     * audio_effects config + restarting audioserver once per boot, on its heavy path), launches
     * the Manager's engine activity to attach the effect on the global session, and then powers
     * it on via JamesDSP's own SET_POWER_STATE broadcast. It must stay in the root script (not an
     * app-side intent): when toggled from the backgrounded companion panel, only the root `am`
     * can legally start the engine activity, and starting it that way sidesteps the
     * background-foreground-service-start restriction. Returns true if the binder accepted it.
     */
    fun enableJdsp(context: Context): Boolean =
        RootUtils.runRootScript(context, "jdsp.enable.sh") != null

    /**
     * Turn the engine off by asking JamesDSP to power down via its own mechanism — the same one
     * its switch and QS tile use. This is a plain broadcast (no root, no background restriction):
     * it bypasses the attached effect and leaves the Manager process running, exactly like
     * flipping JamesDSP's own switch, so the two never disagree. (Contrast the old force-stop,
     * which killed the process but left JamesDSP's own `powered_on` stuck at true.)
     */
    fun disableJdsp(context: Context): Boolean {
        setManagerPower(context, false)
        return true
    }

    /** Broadcast a power on/off request to JamesDSP's exported PowerStateReceiver. */
    fun setManagerPower(context: Context, on: Boolean) {
        val intent = Intent(POWER_ACTION)
            .setComponent(ComponentName(JDSP_PACKAGE_NAME, POWER_RECEIVER))
            .putExtra(POWER_EXTRA, on)
        context.sendBroadcast(intent)
    }

    /**
     * Read JamesDSP's persisted `powered_on` state (root `cat` of its prefs), so ThorTune can
     * reflect changes made through JamesDSP's own switch or Quick Settings tile — there is no
     * cross-process change event to subscribe to. Returns null if the state can't be determined
     * (no root, prefs not written yet, Manager not installed).
     */
    fun readManagerPowerState(context: Context): Boolean? {
        // grep (not cat): the PServer channel only returns the command's first output line, so we
        // filter down to the single `powered_on` line rather than the whole multi-line prefs file.
        val line = RootUtils.runRootCommand(context, "grep powered_on $PREFS_PATH") ?: return null
        return POWERED_ON_REGEX.find(line)?.groupValues?.get(1)?.toBoolean()
    }

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
