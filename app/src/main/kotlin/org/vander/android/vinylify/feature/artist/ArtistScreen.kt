package org.vander.android.vinylify.feature.artist

import org.vander.android.vinylify.designsystem.withNumerals
import org.vander.android.vinylify.designsystem.component.MarqueeLazyColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import org.vander.spotify.model.Album
import org.vander.spotify.model.ArtistDetail
import org.vander.spotify.model.Track

@Serializable
data class ArtistRoute(
    val id: String,
) {
    companion object {
        const val ARG_ID = "id"
    }
}

fun NavGraphBuilder.artistNavGraph(
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
) {
    composable<ArtistRoute> {
        val viewModel: ArtistViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val isFollowing by viewModel.isFollowing.collectAsStateWithLifecycle()
        val followFailed by viewModel.followFailed.collectAsStateWithLifecycle()
        ArtistScreen(
            state = state,
            isFollowing = isFollowing,
            followFailed = followFailed,
            onToggleFollow = viewModel::toggleFollow,
            onBack = onBack,
            onRetry = viewModel::load,
            onPlayArtist = viewModel::playArtist,
            onTrackClick = viewModel::playTrack,
            onAlbumClick = { onOpenAlbum(it.id) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun ArtistScreen(
    state: DetailState<ArtistDetail>,
    isFollowing: Boolean?,
    followFailed: Boolean,
    onToggleFollow: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlayArtist: () -> Unit,
    onTrackClick: (Track) -> Unit,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.statusBarsPadding()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.content_desc_back), tint = VinylTextEmphasis)
        }
        when (state) {
            DetailState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.detail_loading), color = VinylTextMuted) }
            DetailState.Failed -> FailedMessage(onRetry)
            is DetailState.Loaded -> Loaded(state.value, isFollowing, followFailed, onToggleFollow, onPlayArtist, onTrackClick, onAlbumClick)
        }
    }
}

@Composable
private fun Loaded(
    detail: ArtistDetail,
    isFollowing: Boolean?,
    followFailed: Boolean,
    onToggleFollow: () -> Unit,
    onPlayArtist: () -> Unit,
    onTrackClick: (Track) -> Unit,
    onAlbumClick: (Album) -> Unit,
) {
    MarqueeLazyColumn(
        contentPadding = PaddingValues(horizontal = VotDimens.screenPadding, vertical = VotDimens.space12),
        verticalArrangement = Arrangement.spacedBy(VotDimens.space12),
    ) {
        item(key = "header") {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                SpotifyTrackCover(Modifier.size(160.dp).clip(CircleShape), model = spotifyImageUrl(detail.artist.cover?.url))
                Text(detail.artist.name, style = MaterialTheme.typography.headlineLarge, color = VinylTextEmphasis, modifier = Modifier.padding(top = VotDimens.space14))
                if (detail.albums.isNotEmpty()) {
                    Text(
                        pluralStringResource(R.plurals.artist_albums_count, detail.albums.size, detail.albums.size).withNumerals(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = VinylTextSecondary,
                    )
                }
            }
        }
        item(key = "play") {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(VotDimens.space12), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onPlayArtist) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Text(stringResource(R.string.action_listen))
                    }
                    if (isFollowing != null) {
                        OutlinedButton(onClick = onToggleFollow) {
                            Text(stringResource(if (isFollowing) R.string.artist_following else R.string.artist_follow), color = VinylTextEmphasis)
                        }
                    }
                }
                if (followFailed) {
                    Text(stringResource(R.string.artist_follow_failed), style = MaterialTheme.typography.bodySmall, color = VinylTextMuted)
                }
            }
        }
        if (detail.topTracks.isNotEmpty()) {
            item(key = "top-header") { Text(stringResource(R.string.artist_top_tracks), style = MaterialTheme.typography.titleMedium, color = VinylTextEmphasis) }
            itemsIndexed(detail.topTracks.take(TOP_TRACKS_SHOWN), key = { _, t -> "top:${t.id.value}" }) { index, track ->
                TrackLine(index + 1, track.name, track.album?.name, track.durationMs, { onTrackClick(track) })
            }
        }
        if (detail.albums.isNotEmpty()) {
            item(key = "albums-header") { Text(stringResource(R.string.artist_albums), style = MaterialTheme.typography.titleMedium, color = VinylTextEmphasis) }
            item(key = "albums") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(VotDimens.space14)) {
                    items(detail.albums, key = { it.id }) { album ->
                        Column(Modifier.width(120.dp).clickable { onAlbumClick(album) }) {
                            SpotifyTrackCover(Modifier.size(120.dp), model = spotifyImageUrl(album.cover?.url))
                            Text(album.name, style = MaterialTheme.typography.bodySmall, color = VinylTextEmphasis, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

private const val TOP_TRACKS_SHOWN = 5
