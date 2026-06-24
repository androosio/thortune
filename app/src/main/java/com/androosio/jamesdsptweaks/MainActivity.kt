package com.androosio.jamesdsptweaks

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.androosio.jamesdsptweaks.components.SettingsRow
import com.androosio.jamesdsptweaks.ui.theme.AppTheme
import com.androosio.jamesdsptweaks.utils.JdspUtils
import com.androosio.jamesdsptweaks.utils.RootUtils

private val BUTTON_WIDTH = 260.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sharedPrefs = AppSettings.getSharedPrefs(context)
    val isRooted = RootUtils.isDeviceRooted

    var jdspEnabled by remember { mutableStateOf(AppSettings.getJdspEnabled(sharedPrefs)) }
    var managerInstalled by remember { mutableStateOf(JdspUtils.hasJdspPackage(context)) }
    var moduleInstalled by remember { mutableStateOf(false) }

    // Re-check install state whenever we return to the app (e.g. after the system installer
    // or a reboot), so the buttons always reflect what's actually on the device.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                managerInstalled = JdspUtils.hasJdspPackage(context)
                if (isRooted) moduleInstalled = JdspUtils.isMagiskModuleInstalled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(getApplicationName(context)) })
        }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
        ) {
            SettingsRow(
                label = "JamesDSP",
                detail = "System-wide DSP providing EQ and audio effects. Install the JamesDSP " +
                        "Manager app first, then enable the audio engine." +
                        (if (isRooted) "\n\nThe Magisk module requires a reboot to take effect."
                        else "\n\nThe engine is re-applied automatically on each boot."),
                notice = if (!RootUtils.hasPServer() && !isRooted)
                    "No root method detected on this device." else null,
            ) {
                if (isRooted) {
                    Button(
                        modifier = Modifier.width(BUTTON_WIDTH),
                        enabled = !moduleInstalled,
                        onClick = {
                            JdspUtils.installJdspMagiskModule(context)
                            moduleInstalled = true
                            AppSettings.setJdspEnabled(sharedPrefs, true)
                            jdspEnabled = true
                            Toast.makeText(context, "JamesDSP module installed — please reboot", Toast.LENGTH_LONG).show()
                        },
                    ) {
                        Text(
                            if (moduleInstalled) "JamesDSP Module Installed" else "Install JamesDSP Module",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                } else {
                    Switch(
                        checked = jdspEnabled,
                        // Enabling the engine is pointless until the Manager app is installed.
                        enabled = managerInstalled,
                        onCheckedChange = { checked ->
                            jdspEnabled = checked
                            AppSettings.setJdspEnabled(sharedPrefs, checked)
                            if (checked) JdspUtils.enableJdsp(context) else JdspUtils.disableJdsp(context)
                        }
                    )
                }

                Spacer(modifier = Modifier.padding(PaddingValues(0.dp, 8.dp, 0.dp, 0.dp)))

                Button(
                    modifier = Modifier.width(BUTTON_WIDTH),
                    onClick = {
                        JdspUtils.installJdspManager(context)
                        JdspUtils.copyBackupFile(context)
                    },
                ) {
                    Text(
                        if (managerInstalled) "Reinstall JamesDSP Manager" else "Install JamesDSP Manager",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

                Spacer(modifier = Modifier.padding(PaddingValues(0.dp, 8.dp, 0.dp, 0.dp)))

                Button(
                    modifier = Modifier.width(BUTTON_WIDTH),
                    // Only offer to open once the Manager app is actually installed.
                    enabled = managerInstalled,
                    onClick = { JdspUtils.openJdspManager(context) },
                ) {
                    Text(
                        if (managerInstalled) "Open JamesDSP" else "JamesDSP Not Installed",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }

            Spacer(modifier = Modifier.padding(16.dp))

            Text(
                modifier = Modifier.fillMaxWidth().padding(16.dp, 0.dp),
                text = "After installing, open JamesDSP Manager once and import the preset backup " +
                        "saved to your Downloads folder, then enable the engine above.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
