package com.androosio.jamesdsptweaks.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader

/** Recursively copy a bundled assets folder into the app's private filesDir. */
fun copyAssetFolderToFilesDir(context: Context, assetFolderPath: String) {
    try {
        val assetManager = context.assets
        val assetFiles = assetManager.list(assetFolderPath) ?: return

        if (assetFiles.isEmpty()) {
            // Empty folder: just create it.
            val targetDir = File(context.filesDir, assetFolderPath)
            targetDir.mkdirs()
            return
        }

        for (assetFileName in assetFiles) {
            val fullAssetPath = if (assetFolderPath.isEmpty()) assetFileName else "$assetFolderPath/$assetFileName"
            try {
                // If this opens, it's a file; otherwise it's a subfolder and throws.
                assetManager.open(fullAssetPath).use {
                    val outFile = File(context.filesDir, fullAssetPath)
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { output -> it.copyTo(output) }
                    outFile.setReadable(true)
                    outFile.setExecutable(true)
                }
            } catch (e: IOException) {
                copyAssetFolderToFilesDir(context, fullAssetPath)
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

object RootUtils {

    private const val TAG = "RootUtils"

    /** Subfolder under filesDir where bundled assets are staged. */
    const val subfolder = "app"

    /** True when the device has a permanent (Magisk/su) root. */
    val isDeviceRooted: Boolean
        get() = checkRootMethod1() || checkRootMethod2() || checkRootMethod3()

    private fun checkRootMethod1(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkRootMethod2(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/product/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    private fun checkRootMethod3(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine() != null
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }

    /** True when the manufacturer's "run script as root" PServer binder is available. */
    fun hasPServer(): Boolean {
        return RootExec().pServerAvailable
    }

    fun runRootCommand(context: Context, command: String): String? {
        Log.d(TAG, "running root command= $command")
        val result = RootExec().executeAsRoot(command)
        Log.d(TAG, "command finished with result: $result")
        return result.getOrNull()
    }

    /** Run one of the bundled support scripts (in assets/app/support/subscripts) as root. */
    fun runRootScript(context: Context, script: String): String? {
        val filespath = File(context.filesDir, subfolder).absolutePath
        val logpath = getLogFile(context)
        val cmd = "sh $filespath/support/subscripts/$script $filespath > $logpath"
        Log.d(TAG, "running root script with cmd= $cmd")
        val result = RootExec().executeAsRoot(cmd)
        Log.d(TAG, "$script finished with result: $result")
        return result.getOrNull()
    }

    fun reboot(context: Context) {
        runRootCommand(context, "reboot")
    }

    fun isPackageInstalled(context: Context, packageName: String?): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName!!, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
