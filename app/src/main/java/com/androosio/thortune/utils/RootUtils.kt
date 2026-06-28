package com.androosio.thortune.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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
}
