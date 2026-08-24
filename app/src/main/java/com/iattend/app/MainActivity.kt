package com.iattend.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.AppLinks
import com.iattend.app.core.crash.CrashReporter
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

                    CrashReportPrompt()
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun CrashReportPrompt() {
    val context = LocalContext.current
    var log by remember { mutableStateOf(CrashReporter.pendingLog(context)) }
    val currentLog = log ?: return

    AlertDialog(
        onDismissRequest = { CrashReporter.clearPendingLog(context); log = null },
        title = { Text("iAttend crashed last time") },
        text = { Text("Send the crash log to the developer to help fix it?") },
        confirmButton = {
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "message/rfc822"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(AppLinks.SUPPORT_EMAIL))
                    putExtra(Intent.EXTRA_SUBJECT, "iAttend crash report")
                    putExtra(Intent.EXTRA_TEXT, currentLog)
                }
                context.startActivity(Intent.createChooser(intent, "Send crash report"))
                CrashReporter.clearPendingLog(context)
                log = null
            }) { Text("Send") }
        },
        dismissButton = {
            TextButton(onClick = { CrashReporter.clearPendingLog(context); log = null }) { Text("Dismiss") }
        }
    )
}
