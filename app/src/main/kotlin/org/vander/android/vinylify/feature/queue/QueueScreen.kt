package org.vander.android.vinylify.feature.queue

import org.vander.android.vinylify.designsystem.ListTitleTextStyle
import org.vander.android.vinylify.designsystem.DurationTextStyle
import org.vander.android.vinylify.designsystem.component.MarqueeLazyColumn
import org.vander.android.vinylify.designsystem.component.MarqueeTextInfinite
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
import org.vander.android.vinylify.designsystem.VinylPurpleSoft
import org.vander.android.vinylify.designsystem.VinylTextEmphasis
import org.vander.android.vinylify.designsystem.VinylTextMuted
import org.vander.android.vinylify.designsystem.VinylTextSecondary
import org.vander.android.vinylify.designsystem.VotDimens
import org.vander.android.vinylify.designsystem.modifier.drawGrainOverlay
import org.vander.android.vinylify.feature.player.pullDownToDismiss
import org.vander.android.vinylify.feature.player.verticalSwipe
import org.vander.android.vinylify.navigation.modalEnter
import org.vander.android.vinylify.navigation.modalExit
import org.vander.android.vinylify.util.spotifyImageUrl
import org.vander.spotify.model.Album
import org.vander.spotify.model.Artist
import org.vander.spotify.model.Queue
import org.vander.spotify.model.Track
import org.vander.spotify.model.TrackId

/** Écran 07, **modale** comme le Lecteur : à la racine du graphe, donc sans dock ni MiniPlayer. */
@Serializable
object QueueRoute

fun NavGraphBuilder.queueNavGraph(onClose: () -> Unit) {
    composable<QueueRoute>(
        enterTransition = { modalEnter() },
        popExitTransition = { modalExit() },
    ) {
        val viewModel: QueueViewModel = hiltViewModel()
        val queue by viewModel.queue.collectAsStateWithLifecycle()
        QueueScreen(queue = queue, onClose = onClose)
    }
}

@Composable
fun QueueScreen(
    queue: Queue,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawGrainOverlay(alpha = 0.82f)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Le swipe de fermeture est limité à l'en-tête : sur la liste, il entrerait en conflit avec le défilement.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().verticalSwipe(onSwipeDown = onClose).padding(horizontal = VotDimens.space8),
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.content_desc_close_queue), tint = VinylTextEmphasis)
            }
            Text(stringResource(R.string.queue_title), style = MaterialTheme.typography.titleLarge, color = VinylTextEmphasis)
        }

        if (queue.currentlyPlaying == null && queue.upcoming.isEmpty()) {
            Box(Modifier.fillMaxSize().verticalSwipe(onSwipeDown = onClose), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.queue_empty), color = VinylTextMuted)
            }
            return@Column
        }

        MarqueeLazyColumn(
            modifier = Modifier.pullDownToDismiss(onClose),
            contentPadding = PaddingValues(horizontal = VotDimens.screenPadding, vertical = VotDimens.space12),
            verticalArrangement = Arrangement.spacedBy(VotDimens.space12),
        ) {
            queue.currentlyPlaying?.let { current ->
                item(key = "now-header") { SectionLabel(stringResource(R.string.queue_now)) }
                item(key = "now") { TrackRow(current, highlighted = true) }
            }
            if (queue.upcoming.isNotEmpty()) {
                item(key = "next-header") { SectionLabel(stringResource(R.string.queue_next)) }
                // Index dans la clé : une même piste peut apparaître plusieurs fois dans la file.
                itemsIndexed(queue.upcoming, key = { index, track -> "$index:${track.id}" }) { _, track ->
                    TrackRow(track, highlighted = false)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = VinylTextSecondary)
}

@Composable
private fun TrackRow(
    track: Track,
    highlighted: Boolean,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VotDimens.space14),
    ) {
        SpotifyTrackCover(Modifier.size(48.dp), model = spotifyImageUrl(track.album?.cover?.url))
        Column(Modifier.weight(1f)) {
            MarqueeTextInfinite(
                text = track.name,
                textStyle = ListTitleTextStyle,
                color = if (highlighted) VinylPurpleSoft else VinylTextEmphasis,
                fadeColor = MaterialTheme.colorScheme.background,
            )
            Text(track.artistNames, style = MaterialTheme.typography.bodySmall, color = VinylTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (track.durationMs > 0) {
            Text(formatDuration(track.durationMs), style = DurationTextStyle, color = VinylTextMuted)
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Preview(showBackground = true, backgroundColor = 0xFF0C0A10)
@Composable
private fun QueueScreenPreview() {
    fun track(
        id: String,
        name: String,
        ms: Long,
    ) = Track(TrackId(id), name, listOf(Artist("r1", "Elia Faure")), Album("a1", "Nuits blanches"), ms)
    AndroidAppTheme {
        QueueScreen(
            queue =
                Queue(
                    currentlyPlaying = track("t0", "Sous la neige", 268_000),
                    upcoming = listOf(track("t1", "Réverbère", 199_000), track("t2", "Quatre heures", 302_000)),
                ),
            onClose = {},
        )
    }
}
