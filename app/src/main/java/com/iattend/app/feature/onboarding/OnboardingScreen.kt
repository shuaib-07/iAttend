package com.iattend.app.feature.onboarding

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.ui.SpringAlertDialog
import com.iattend.app.core.ui.hapticClick
import com.iattend.app.ui.theme.Motion

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onFinishedBackdated: (Long) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val message by viewModel.message.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.finishEvents.collect { event ->
            when (event) {
                is OnboardingFinishEvent.ToHome -> onFinished()
                is OnboardingFinishEvent.ToBackdatedFill -> onFinishedBackdated(event.versionId)
            }
        }
    }
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }

    if (state.showTemplateNamePrompt) {
        SpringAlertDialog(
            onDismissRequest = {},
            title = { Text("Template Imported!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Your timetable and subjects have been imported. What should we call you?")
                    OutlinedTextField(
                        value = state.templateNameInput,
                        onValueChange = viewModel::onTemplateNameInputChange,
                        label = { Text("Your name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = viewModel::submitTemplateNameAndFinish) {
                    Text("Continue")
                }
            }
        )
    }

    if (!state.hasChosenStart) {
        StartChoiceScreen(
            onStartFresh = hapticClick(viewModel::chooseStartFresh),
            onImportBackup = viewModel::importBackup
        )
        return
    }

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            com.iattend.app.core.ui.OnboardingAmbientBackground(modifier = Modifier.fillMaxSize())
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Step ${state.step + 1} of $ONBOARDING_STEP_COUNT", style = MaterialTheme.typography.labelLarge)
            val animatedProgress by animateFloatAsState(
                targetValue = (state.step + 1f) / ONBOARDING_STEP_COUNT,
                animationSpec = Motion.gentle(),
                label = "onboardingProgress"
            )
            LinearWavyProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            AnimatedContent(
                targetState = state.step,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally(animationSpec = Motion.mediumTween()) { w -> if (forward) w else -w } + fadeIn(Motion.mediumTween())) togetherWith
                        (slideOutHorizontally(animationSpec = Motion.mediumTween()) { w -> if (forward) -w else w } + fadeOut(Motion.mediumTween()))
                },
                label = "onboardingStep"
            ) { step ->
                when (step) {
                    0 -> FeaturesStep()
                    1 -> NotificationPermissionStep(onGranted = viewModel::onNotificationsEnabled)
                    2 -> ThemeStep()
                    3 -> NameStep(state.name, viewModel::onNameChange, state.avatarUri, viewModel::onAvatarPicked, viewModel::onAvatarUrlSelected)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (state.step > 0) {
                    OutlinedButton(onClick = hapticClick(viewModel::previousStep)) { Text("Back") }
                } else {
                    Box {}
                }
                if (state.step < ONBOARDING_STEP_COUNT - 1) {
                    Button(onClick = hapticClick(viewModel::nextStep)) { Text("Next") }
                } else {
                    Button(onClick = hapticClick(viewModel::finish)) { Text("Finish setup") }
                }
            }
            }
        }
    }
}

@Composable
private fun StartChoiceScreen(onStartFresh: () -> Unit, onImportBackup: (android.net.Uri) -> Unit) {
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImportBackup)
    }
    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        com.iattend.app.core.ui.OnboardingAmbientBackground(modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            com.iattend.app.core.ui.PulsingLogo(logoRes = com.iattend.app.R.drawable.img_logo_icon)
            Text(
                "iAttend",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Text(
                "Start fresh, or restore a backup you exported from another device.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Button(
                onClick = onStartFresh,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Start fresh") }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Import existing backup") }
        }
        }
    }
}

@Composable
private fun NameStep(
    name: String,
    onNameChange: (String) -> Unit,
    avatarUri: String?,
    onAvatarPicked: (android.net.Uri) -> Unit,
    onAvatarUrlSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("What's your name?", style = MaterialTheme.typography.titleLarge)
        Text(
            "Optional - you can skip this and set it later in Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        com.iattend.app.core.ui.AvatarPicker(
            pictureUri = avatarUri,
            onPictureSelected = onAvatarPicked,
            onAvatarUrlSelected = onAvatarUrlSelected,
            avatarSize = 80.dp,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}

