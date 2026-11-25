package org.vander.android.vinylify.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.vander.android.vinylify.BuildConfig
import org.vander.android.vinylify.R
import org.vander.android.vinylify.designsystem.AndroidAppTheme
import org.vander.android.vinylify.designsystem.VinylFeedbackErrorText
import org.vander.android.vinylify.designsystem.VinylSurfaceCard
import org.vander.android.vinylify.designsystem.VinylTextEmphasis
import org.vander.android.vinylify.designsystem.VinylTextMuted
import org.vander.android.vinylify.designsystem.VinylTextSecondary
import org.vander.android.vinylify.designsystem.VotDimens

/** Écran empilé dans `MainGraph` (registre push) : il garde la dock et se quitte par `‹`. */
@Serializable
object SettingsRoute

private const val SPOTIFY_ACCOUNT_URL = "https://www.spotify.com/account"

fun NavGraphBuilder.settingsNavGraph(onBack: () -> Unit) {
    composable<SettingsRoute> {
        val viewModel: SettingsViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val uriHandler = LocalUriHandler.current
        SettingsScreen(
            state = state,
            versionName = BuildConfig.VERSION_NAME,
            onBack = onBack,
            onManageAccount = { uriHandler.openUri(SPOTIFY_ACCOUNT_URL) },
            onSignOut = viewModel::signOut,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    versionName: String,
    onBack: () -> Unit,
    onManageAccount: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // `rememberSaveable` : la boîte de confirmation survit à une rotation.
    var confirmingSignOut by rememberSaveable { mutableStateOf(false) }

    Column(modifier.statusBarsPadding()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.content_desc_back), tint = VinylTextEmphasis)
        }
        Column(Modifier.padding(horizontal = VotDimens.screenPadding)) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge, color = VinylTextEmphasis)

            Spacer(Modifier.height(VotDimens.space30))
            SectionLabel(stringResource(R.string.settings_section_account))
            Card {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onManageAccount).padding(VotDimens.space14),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(stringResource(R.string.profile_account), color = VinylTextEmphasis)
                        state.userName?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = VinylTextSecondary) }
                    }
                    Text(stringResource(R.string.profile_account_manage), color = VinylTextMuted)
                }
            }

            Spacer(Modifier.height(VotDimens.space20))
            SectionLabel(stringResource(R.string.settings_section_about))
            Card {
                Row(Modifier.fillMaxWidth().padding(VotDimens.space14), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.settings_version), color = VinylTextEmphasis)
                    Text(versionName, color = VinylTextMuted)
                }
            }

            Spacer(Modifier.height(VotDimens.space32))
            OutlinedButton(onClick = { confirmingSignOut = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_sign_out), color = VinylFeedbackErrorText)
            }
        }
    }

    if (confirmingSignOut) {
        AlertDialog(
            onDismissRequest = { confirmingSignOut = false },
            title = { Text(stringResource(R.string.settings_sign_out_title)) },
            text = { Text(stringResource(R.string.settings_sign_out_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingSignOut = false
                        onSignOut()
                    },
                ) { Text(stringResource(R.string.profile_sign_out), color = VinylFeedbackErrorText) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingSignOut = false }) { Text(stringResource(R.string.settings_cancel)) }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = VinylTextSecondary, modifier = Modifier.padding(bottom = VotDimens.space8))
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().background(VinylSurfaceCard, MaterialTheme.shapes.medium)) { content() }
}

@Preview(showBackground = true, backgroundColor = 0xFF0C0A10, heightDp = 700)
@Composable
private fun SettingsScreenPreview() {
    AndroidAppTheme {
        SettingsScreen(SettingsUiState("Théo"), versionName = "0.1.0", onBack = {}, onManageAccount = {}, onSignOut = {})
    }
}
