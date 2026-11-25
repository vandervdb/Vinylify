package org.vander.android.vinylify.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.vander.android.vinylify.R
import org.vander.android.vinylify.component.SpotifyTrackCover
import org.vander.android.vinylify.designsystem.AndroidAppTheme
import org.vander.android.vinylify.designsystem.VinylOutline
import org.vander.android.vinylify.designsystem.VinylPurple
import org.vander.android.vinylify.designsystem.VinylPurpleDim
import org.vander.android.vinylify.designsystem.VinylSurfaceCard
import org.vander.android.vinylify.designsystem.VinylTextEmphasis
import org.vander.android.vinylify.designsystem.VinylTextMuted
import org.vander.android.vinylify.designsystem.VinylTextSecondary
import org.vander.android.vinylify.designsystem.VotDimens

@Serializable
object ProfileRoute

fun NavGraphBuilder.profileNavGraph(onOpenSettings: () -> Unit) {
    composable<ProfileRoute> { ProfileScreen(onOpenSettings = onOpenSettings) }
}

@Composable
fun ProfileScreen(
    onOpenSettings: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfileContent(
        state = state,
        onOpenSettings = onOpenSettings,
        modifier = Modifier.fillMaxSize(),
    )
}

/**
 * Écran 10. Les réglages « qualité de streaming » et « écoute hors connexion » de la maquette
 * ne sont pas pilotables par l'API Spotify : ils sont volontairement absents plutôt que factices.
 */
@Composable
fun ProfileContent(
    state: ProfileUiState,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .statusBarsPadding()
            .padding(horizontal = VotDimens.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(VotDimens.space32))
        Avatar(state)
        Spacer(Modifier.height(VotDimens.space14))
        Text(state.name.orEmpty(), style = MaterialTheme.typography.headlineMedium, color = VinylTextEmphasis)
        Text(
            text =
                stringResource(
                    if (state.isPremium == true) R.string.profile_connected_premium else R.string.profile_connected,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = VinylTextSecondary,
        )

        Spacer(Modifier.height(VotDimens.space30))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stat(state.albumCount, R.string.profile_stat_albums)
            Stat(state.likedCount, R.string.profile_stat_liked)
            Stat(state.followingCount, R.string.profile_stat_following)
        }

        Spacer(Modifier.height(VotDimens.space32))
        Row(
            Modifier
                .fillMaxWidth()
                .background(VinylSurfaceCard, MaterialTheme.shapes.medium)
                .clickable(onClick = onOpenSettings)
                .padding(VotDimens.space14),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.profile_settings), color = VinylTextEmphasis)
            Text(stringResource(R.string.profile_account_manage), color = VinylTextMuted)
        }
    }
}

@Composable
private fun Avatar(state: ProfileUiState) {
    val size = 96.dp
    if (state.avatarUrl != null) {
        SpotifyTrackCover(Modifier.size(size).clip(CircleShape), model = state.avatarUrl)
        return
    }
    // Sans photo : l'initiale, comme Spotify le fait.
    Box(Modifier.size(size).background(VinylPurpleDim, CircleShape), contentAlignment = Alignment.Center) {
        Text(
            state.name?.firstOrNull()?.uppercase().orEmpty(),
            style = MaterialTheme.typography.headlineLarge,
            color = VinylPurple,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Stat(
    value: Int?,
    labelRes: Int,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value?.toString() ?: "–", style = MaterialTheme.typography.headlineSmall, color = VinylTextEmphasis)
        Text(stringResource(labelRes), style = MaterialTheme.typography.bodySmall, color = VinylTextSecondary)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0C0A10, heightDp = 700)
@Composable
private fun ProfileContentPreview() {
    AndroidAppTheme {
        ProfileContent(
            state = ProfileUiState("Théo", null, true, 243, 1284, 61),
            onOpenSettings = {},
        )
    }
}
