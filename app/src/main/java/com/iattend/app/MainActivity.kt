package com.iattend.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.navigation.AppNavHost
import com.iattend.app.core.navigation.AppViewModel
import com.iattend.app.core.navigation.HomeRoute
import com.iattend.app.core.navigation.OnboardingRoute
import com.iattend.app.ui.theme.IAttendTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val themeMode by appViewModel.themeMode.collectAsState()
            val dynamicColorEnabled by appViewModel.dynamicColorEnabled.collectAsState()
            val accentColor by appViewModel.accentColor.collectAsState()

            IAttendTheme(themeMode = themeMode, dynamicColorEnabled = dynamicColorEnabled, accentColor = accentColor) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val onboardingComplete by appViewModel.onboardingComplete.collectAsState()

                    when (onboardingComplete) {
                        null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        else -> AppNavHost(startDestination = if (onboardingComplete == true) HomeRoute else OnboardingRoute)
                    }
                }
            }
        }
    }
}
