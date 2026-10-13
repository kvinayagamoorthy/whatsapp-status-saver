package com.moorthy.statussaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.moorthy.statussaver.ui.onboarding.OnboardingScreen
import com.moorthy.statussaver.ui.theme.KeepsakeTheme

/** Single activity that hosts all Compose screens. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the system bars; status bar icon color follows the system theme.
        enableEdgeToEdge()
        setContent {
            KeepsakeTheme {
                // Onboarding is the only screen until navigation arrives in T1.6.
                OnboardingScreen()
            }
        }
    }
}
