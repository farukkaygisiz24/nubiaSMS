package com.aramalr.messages

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aramalr.messages.ui.navigation.AramalrNav
import com.aramalr.messages.ui.theme.AramalrTheme

class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        // Drop splash immediately — black window → message list, no logo flash.
        splash.setKeepOnScreenCondition { false }
        splash.setOnExitAnimationListener { provider -> provider.remove() }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AramalrTheme {
                AramalrNav(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                )
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
}
