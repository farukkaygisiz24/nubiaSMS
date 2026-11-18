package com.nubiasms.messages

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nubiasms.messages.ui.navigation.NubiaSmsNav
import com.nubiasms.messages.ui.theme.NubiaSmsTheme
import com.nubiasms.messages.ui.update.UpdatePromptHost

class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    var updateCheckTick by mutableIntStateOf(0)
        private set

    fun requestUpdateCheck() {
        updateCheckTick++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        // Drop splash immediately — black window → message list, no logo flash.
        splash.setKeepOnScreenCondition { false }
        splash.setOnExitAnimationListener { provider -> provider.remove() }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestHighestRefreshRate()
        setContent {
            NubiaSmsTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    NubiaSmsNav(modifier = Modifier.fillMaxSize())
                    UpdatePromptHost(manualCheckTick = updateCheckTick)
                }
            }
        }
        // Ask after first frame so the list paints before the permission sheet.
        window.decorView.post {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.POST_NOTIFICATIONS,
                ),
            )
        }
    }

    /**
     * MyOS keeps apps at 60Hz unless the window asks for more. Pick the fastest mode at the
     * current resolution (144Hz on NX741J) so scrolling matches the system UI; the OS can
     * still drop it for battery saver or thermal limits.
     */
    private fun requestHighestRefreshRate() {
        @Suppress("DEPRECATION")
        val display = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else windowManager.defaultDisplay)
            ?: return
        val current = display.mode
        val best =
            display.supportedModes
                .filter {
                    it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight
                }.maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.apply { preferredDisplayModeId = best.modeId }
    }
}
