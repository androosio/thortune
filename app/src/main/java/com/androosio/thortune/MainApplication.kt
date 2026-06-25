package com.androosio.thortune

import android.app.Application
import com.androosio.thortune.utils.copyAssetFolderToFilesDir

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Stage bundled scripts, configs and the JamesDSP engine into private filesDir.
        copyAssetFolderToFilesDir(applicationContext, "app")
    }
}
