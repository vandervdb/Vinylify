package org.vander.android.vinylify.feature.album

import org.vander.android.vinylify.designsystem.ListTitleTextStyle
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import org.vander.android.vinylify.feature.DetailState
import org.vander.android.vinylify.designsystem.VinylTextEmphasis
import org.vander.android.vinylify.designsystem.VinylTextMuted
import org.vander.android.vinylify.designsystem.VinylTextSecondary
import org.vander.android.vinylify.designsystem.VotDimens
import org.vander.android.vinylify.util.spotifyImageUrl
import org.vander.spotify.model.AlbumDetail

/** `id` est lu par [AlbumViewModel] dans le `SavedStateHandle` : le nom de la propriété fait la clé. */
@Serializable
data class AlbumRoute(
    val id: String,
) {
    companion object {
        const val ARG_ID = "id"
    }
}

fun NavGraphBuilder.albumNavGraph(onBack: () -> Unit) {
    composable<AlbumRoute> {
        val viewModel: AlbumViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        AlbumScreen(
            state = state,
            onBack = onBack,
            onRetry = viewModel::load,
            onPlayAlbum = viewModel::playAlbum,
            onTrackClick = viewModel::playTrack,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun AlbumScreen(
    state: DetailState<AlbumDetail>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlayAlbum: () -> Unit,
    onTrackClick: (org.vander.spotify.model.Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.statusBarsPadding()) {
        // `‹` : registre push (on dépile), pas le `⌄` d'une modale.
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.content_desc_back), tint = VinylTextEmphasis)
        }
        when (state) {
            DetailState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.detail_loading), color = VinylTextMuted) }
            DetailState.Failed -> FailedMessage(onRetry)
            is DetailState.Loaded -> Loaded(state.value, onPlayAlbum, onTrackClick)
        }
    }
}

@Composable
private fun Loaded(
    detail: AlbumDetail,
    onPlayAlbum: () -> Unit,
    onTrackClick: (org.vander.spotify.model.Track) -> Unit,
) {
    MarqueeLazyColumn(
        contentPadding = PaddingValues(horizontal = VotDimens.screenPadding, vertical = VotDimens.space12),
        verticalArrangement = Arrangement.spacedBy(VotDimens.space12),
    ) {
        item(key = "cover") {
            SpotifyTrackCover(Modifier.fillMaxWidth().aspectRatio(1f), model = spotifyImageUrl(detail.album.cover?.url))
        }
        item(key = "title") {
            Column {
                Spacer(Modifier.height(VotDimens.space8))
                val kicker = listOfNotNull(stringResource(R.string.album_kind), detail.album.releaseYear?.toString()).joinToString(" · ")
                Text(kicker.withNumerals(), style = MaterialTheme.typography.labelLarge, color = VinylTextSecondary)
                Text(detail.album.name, style = MaterialTheme.typography.headlineLarge, color = VinylTextEmphasis)
                Text(detail.album.artistNames, style = ListTitleTextStyle, color = VinylTextSecondary)
            }
        }
        item(key = "play") {
            Button(onClick = onPlayAlbum) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(stringResource(R.string.action_listen))
            }
        }
        item(key = "summary") {
            val minutes = (detail.durationMs / 60_000).toInt()
            Text(
                pluralStringResource(R.plurals.album_summary, detail.tracks.size, detail.tracks.size, minutes).withNumerals(),
                style = MaterialTheme.typography.bodySmall,
                color = VinylTextSecondary,
            )
        }
        items(detail.tracks, key = { it.id.value }) { track ->
            TrackLine(track.trackNumber, track.name, track.artistNames.takeIf { it != detail.album.artistNames }, track.durationMs, { onTrackClick(track) })
        }
    }
}

@Composable
internal fun FailedMessage(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(VotDimens.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.detail_error), color = VinylTextMuted)
        Spacer(Modifier.height(VotDimens.space12))
        Button(onClick = onRetry) { Text(stringResource(R.string.snack_retry)) }
    }
}
