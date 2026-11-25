package org.vander.android.vinylify.feature.lyrics

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.vander.android.vinylify.R
import org.vander.android.vinylify.designsystem.AndroidAppTheme
import org.vander.android.vinylify.designsystem.VinylPurpleSoft
import org.vander.android.vinylify.designsystem.VinylTextEmphasis
import org.vander.android.vinylify.designsystem.VinylTextMuted
import org.vander.android.vinylify.designsystem.VinylTextSecondary
import org.vander.android.vinylify.designsystem.VotDimens
import org.vander.android.vinylify.designsystem.modifier.drawGrainOverlay
import org.vander.android.vinylify.feature.player.pullDownToDismiss
import org.vander.android.vinylify.feature.player.rememberLivePosition
import org.vander.android.vinylify.feature.player.verticalSwipe
import org.vander.android.vinylify.navigation.modalEnter
import org.vander.android.vinylify.navigation.modalExit
import org.vander.spotify.android.NowPlaying
import org.vander.spotify.model.LyricLine

/** Paroles du titre en lecture : **modale**, comme le Lecteur et la File d'attente. */
@Serializable
object LyricsRoute

fun NavGraphBuilder.lyricsNavGraph(
    lyricsOwner: @Composable (NavBackStackEntry) -> LyricsViewModel,
    onClose: () -> Unit,
) {
    composable<LyricsRoute>(
        enterTransition = { modalEnter() },
        popExitTransition = { modalExit() },
    ) { entry ->
        val viewModel = lyricsOwner(entry)
        val state by viewModel.state.collectAsStateWithLifecycle()
        val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
        LyricsScreen(state = state, nowPlaying = nowPlaying, onClose = onClose)
    }
}

@Composable
fun LyricsScreen(
    state: LyricsState,
    nowPlaying: NowPlaying?,
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
        // Le swipe de fermeture est limité à l'en-tête : sur la liste, c'est `pullDownToDismiss`.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().verticalSwipe(onSwipeDown = onClose).padding(horizontal = VotDimens.space8),
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.content_desc_close_lyrics), tint = VinylTextEmphasis)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.lyrics_title), style = MaterialTheme.typography.titleLarge, color = VinylTextEmphasis)
                if (nowPlaying != null) {
                    Text(
                        "${nowPlaying.title} · ${nowPlaying.artist}",
                        style = MaterialTheme.typography.bodySmall,
                        color = VinylTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        when (state) {
            LyricsState.Loading -> Message(stringResource(R.string.lyrics_loading), onClose)
            LyricsState.NotFound -> Message(stringResource(R.string.lyrics_not_found), onClose)
            LyricsState.Failed -> Message(stringResource(R.string.lyrics_failed), onClose)
            LyricsState.Instrumental -> Message(stringResource(R.string.lyrics_instrumental), onClose)
            is LyricsState.Plain -> PlainLyrics(state.text, onClose)
            is LyricsState.Synced -> SyncedLyrics(state.lines, nowPlaying, onClose)
        }
    }
}

@Composable
private fun Message(
    text: String,
    onClose: () -> Unit,
) {
    Box(Modifier.fillMaxSize().verticalSwipe(onSwipeDown = onClose), contentAlignment = Alignment.Center) {
        Text(text, color = VinylTextMuted, modifier = Modifier.padding(horizontal = VotDimens.screenPadding))
    }
}

private val LyricsPadding = PaddingValues(horizontal = VotDimens.screenPadding, vertical = VotDimens.space20)

@Composable
private fun PlainLyrics(
    text: String,
    onClose: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().pullDownToDismiss(onClose), contentPadding = LyricsPadding) {
        item { Text(text, style = MaterialTheme.typography.bodyLarge, color = VinylTextEmphasis) }
    }
}

@Composable
private fun SyncedLyrics(
    lines: List<LyricLine>,
    nowPlaying: NowPlaying?,
    onClose: () -> Unit,
) {
    val position = if (nowPlaying != null) rememberLivePosition(nowPlaying) else 0L
    val current = currentLineIndex(lines, position)
    val listState = rememberLazyListState()

    // On suit la ligne en cours sauf si l'utilisateur est en train de faire défiler lui-même.
    LaunchedEffect(current) {
        if (current >= 0 && !listState.isScrollInProgress) {
            listState.animateScrollToItem((current - LINES_ABOVE_CURRENT).coerceAtLeast(0))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().pullDownToDismiss(onClose),
        state = listState,
        contentPadding = LyricsPadding,
        verticalArrangement = Arrangement.spacedBy(VotDimens.space12),
    ) {
        itemsIndexed(lines) { index, line ->
            Text(
                // Une ligne vide est une pause instrumentale : un repère plutôt qu'un trou.
                text = line.text.ifBlank { "♪" },
                style = MaterialTheme.typography.headlineSmall,
                color = if (index == current) VinylPurpleSoft else VinylTextMuted,
            )
        }
    }
}

private const val LINES_ABOVE_CURRENT = 2

@Preview(showBackground = true, backgroundColor = 0xFF0C0A10)
@Composable
private fun LyricsScreenPreview() {
    AndroidAppTheme {
        LyricsScreen(
            state = LyricsState.Plain("Sous la neige\nje te retrouve\nRéverbère"),
            nowPlaying = null,
            onClose = {},
        )
    }
}
