package com.iattend.app.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.iattend.app.R
import com.iattend.app.core.AppLinks
import com.iattend.app.core.ui.ListItem
import com.iattend.app.core.ui.ListItemPosition
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.core.ui.hapticClick
import com.iattend.app.core.ui.toShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit, onLicenses: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = { SquircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier.size(90.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_logo_icon),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Text("iAttend", style = MaterialTheme.typography.headlineMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                if (versionName.isNotBlank()) {
                    Text("Version $versionName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            ListItem(
                headline = { Text("Developed by") },
                supporting = { Text("shuaib-07") },
                leading = { Icon(Icons.Filled.Person, contentDescription = null) },
                trailing = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                shape = ListItemPosition.Single.toShape(),
                selected = true,
                onClick = hapticClick { openUrl(AppLinks.GITHUB_PROFILE_URL) }
            )

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
}
