package com.iattend.app.feature.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.R
import com.iattend.app.core.AppLinks
import com.iattend.app.core.ui.ListItem
import com.iattend.app.core.ui.ListItemPosition
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.core.ui.hapticClick
import com.iattend.app.core.ui.toShape
import com.iattend.app.core.updates.UpdateAvailableModal
import com.iattend.app.core.updates.WhatsNewModal
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onLicenses: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isChecking by viewModel.isCheckingForUpdates.collectAsState()
    val showWhatsNew by viewModel.showWhatsNew.collectAsState()
    val updateAvailableInfo by viewModel.updateAvailableInfo.collectAsState()
    val autoCheckUpdates by viewModel.autoCheckUpdates.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is AboutUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    SquircleIconButton(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // App Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_logo_icon),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Text(
                    "iAttend",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (viewModel.currentVersionName.isNotBlank()) {
                    Text(
                        "Version ${viewModel.currentVersionName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Developer Item
            ListItem(
                headline = { Text("Developed by") },
                supporting = { Text("shuaib-07") },
                leading = { Icon(Icons.Filled.Person, contentDescription = null) },
                trailing = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                shape = ListItemPosition.Single.toShape(),
                selected = true,
                onClick = hapticClick { openUrl(AppLinks.GITHUB_PROFILE_URL) }
            )

            // Releases & Updates Section
            Column {
                ListItem(
                    headline = { Text("What's New") },
                    supporting = { Text("See latest features and changes in v${viewModel.currentVersionName}") },
                    leading = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailing = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                    shape = ListItemPosition.Top.toShape(),
                    onClick = hapticClick { viewModel.showWhatsNew() }
                )
                ListItem(
                    headline = { Text("Check for updates") },
                    supporting = {
                        Text(if (isChecking) "Checking GitHub Releases..." else "Search for the latest iAttend release")
                    },
                    leading = {
                        if (isChecking) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                        } else {
                            Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        }
                    },
                    trailing = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                    shape = ListItemPosition.Middle.toShape(),
                    onClick = hapticClick {
                        if (!isChecking) {
                            viewModel.checkForUpdates()
                        }
                    }
                )
                ListItem(
                    headline = { Text("Automatic update checks") },
                    supporting = { Text("Check on app launch and daily in background") },
                    leading = { Icon(Icons.Filled.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                    trailing = {
                        Switch(
                            checked = autoCheckUpdates,
                            onCheckedChange = { viewModel.setAutoCheckUpdates(it) }
                        )
                    },
                    shape = ListItemPosition.Bottom.toShape(),
                    onClick = hapticClick { viewModel.setAutoCheckUpdates(!autoCheckUpdates) }
                )
            }

            // General Links Section
            Column {
                ListItem(
                    headline = { Text("Source code") },
                    supporting = { Text(AppLinks.GITHUB_REPO_URL.removePrefix("https://")) },
                    leading = { Icon(Icons.Filled.Code, contentDescription = null) },
                    trailing = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                    shape = ListItemPosition.Top.toShape(),
                    onClick = hapticClick { openUrl(AppLinks.GITHUB_REPO_URL) }
                )
                ListItem(
                    headline = { Text("Report a bug") },
                    supporting = { Text("Open an issue on GitHub") },
                    leading = { Icon(Icons.Filled.BugReport, contentDescription = null) },
                    trailing = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                    shape = ListItemPosition.Middle.toShape(),
                    onClick = hapticClick { openUrl(AppLinks.REPORT_BUG_URL) }
                )
                ListItem(
                    headline = { Text("Licenses") },
                    supporting = { Text("Open-source software used in this app") },
                    leading = { Icon(Icons.Filled.Description, contentDescription = null) },
                    trailing = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                    shape = ListItemPosition.Bottom.toShape(),
                    onClick = hapticClick(onLicenses)
                )
            }
        }
    }

    if (showWhatsNew) {
        WhatsNewModal(
            versionName = viewModel.currentVersionName,
            highlights = viewModel.builtInHighlights,
            onDismiss = { viewModel.dismissWhatsNew() }
        )
    }

    updateAvailableInfo?.let { releaseInfo ->
        UpdateAvailableModal(
            releaseInfo = releaseInfo,
            onDismiss = { viewModel.dismissUpdateAvailable() }
        )
    }
}
