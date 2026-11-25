package org.vander.android.vinylify.feature.playlist

import org.vander.android.vinylify.designsystem.withNumerals
import org.vander.android.vinylify.designsystem.component.MarqueeLazyColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.vander.android.vinylify.R
import org.vander.android.vinylify.component.SpotifyTrackCover
import org.vander.android.vinylify.component.TrackLine
import org.vander.android.vinylify.designsystem.VinylTextEmphasis
import org.vander.android.vinylify.designsystem.VinylTextMuted
import org.vander.android.vinylify.designsystem.VinylTextSecondary
import org.vander.android.vinylify.designsystem.VotDimens
import org.vander.android.vinylify.feature.DetailState
import org.vander.android.vinylify.feature.album.FailedMessage
import org.vander.android.vinylify.util.spotifyImageUrl
import org.vander.spotify.model.PlaylistDetail
import org.vander.spotify.model.Track

@Serializable
data class PlaylistRoute(
    val id: String,
) {
    companion object {
        const val ARG_ID = "id"
    }
}

fun NavGraphBuilder.playlistNavGraph(onBack: () -> Unit) {
    composable<PlaylistRoute> {
        val viewModel: PlaylistViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        PlaylistScreen(
            state = state,
            onBack = onBack,
            onRetry = viewModel::load,
            onPlay = viewModel::playPlaylist,
            onTrackClick = viewModel::playTrack,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun PlaylistScreen(
    state: DetailState<PlaylistDetail>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlay: () -> Unit,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.statusBarsPadding()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.content_desc_back), tint = VinylTextEmphasis)
        }
        when (state) {
            DetailState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.detail_loading), color = VinylTextMuted) }
            DetailState.Failed -> FailedMessage(onRetry)
            is DetailState.Loaded -> Loaded(state.value, onPlay, onTrackClick)
        }
    }
}

@Composable
private fun Loaded(
    detail: PlaylistDetail,
    onPlay: () -> Unit,
    onTrackClick: (Track) -> Unit,
) {
    MarqueeLazyColumn(
        contentPadding = PaddingValues(horizontal = VotDimens.screenPadding, vertical = VotDimens.space12),
        verticalArrangement = Arrangement.spacedBy(VotDimens.space12),
    ) {
        item(key = "cover") {
            SpotifyTrackCover(Modifier.fillMaxWidth().aspectRatio(1f), model = spotifyImageUrl(detail.playlist.cover?.url))
        }
        item(key = "title") {
            Column {
                Spacer(Modifier.height(VotDimens.space8))
                Text(stringResource(R.string.playlist_kind), style = MaterialTheme.typography.labelLarge, color = VinylTextSecondary)
                Text(detail.playlist.name, style = MaterialTheme.typography.headlineLarge, color = VinylTextEmphasis)
                detail.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = VinylTextSecondary) }
            }
        }
        item(key = "play") {
            Button(onClick = onPlay) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(stringResource(R.string.action_listen))
            }
        }
        item(key = "summary") {
            Text(
                pluralStringResource(R.plurals.playlist_summary, detail.totalTracks, detail.totalTracks).withNumerals(),
                style = MaterialTheme.typography.bodySmall,
                color = VinylTextSecondary,
            )
        }
        // Index dans la clé : un même titre peut figurer plusieurs fois dans une playlist.
        itemsIndexed(detail.tracks, key = { index, track -> "$index:${track.id.value}" }) { index, track ->
            TrackLine(index + 1, track.name, track.artistNames, track.durationMs, { onTrackClick(track) })
        }
    }
}
