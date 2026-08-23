package com.iattend.app.feature.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.iattend.app.core.ui.SquircleIconButton
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer

/** Auto-generated from Gradle dependency metadata by the aboutlibraries plugin - no manual upkeep
 * as dependencies change (see AppLinks.kt / AboutScreen.kt doc for why this replaces a hand-rolled list). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Licenses") },
                navigationIcon = { SquircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack) }
            )
        }
    ) { padding ->
        LibrariesContainer(modifier = Modifier.fillMaxSize().padding(padding))
    }
}
