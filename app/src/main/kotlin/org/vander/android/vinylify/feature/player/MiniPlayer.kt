package org.vander.android.vinylify.feature.player

import org.vander.android.vinylify.designsystem.ListTitleTextStyle
import org.vander.android.vinylify.designsystem.component.MarqueeTextInfinite
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.vander.android.vinylify.R
import org.vander.android.vinylify.component.SpotifyTrackCover
import org.vander.android.vinylify.designsystem.AndroidAppTheme
import org.vander.android.vinylify.designsystem.VinylOutline
import org.vander.android.vinylify.designsystem.VinylPurple
import org.vander.android.vinylify.designsystem.VinylSurfaceCard
import org.vander.android.vinylify.designsystem.VinylTextEmphasis
import org.vander.android.vinylify.designsystem.VinylTextSecondary
import org.vander.android.vinylify.designsystem.VotDimens
import org.vander.android.vinylify.designsystem.component.VinylMiniTurntable
import org.vander.android.vinylify.util.spotifyImageUrl
import org.vander.spotify.android.NowPlaying
import org.vander.spotify.android.PlayerConnection
import org.vander.spotify.android.RepeatMode

private val CardShape = RoundedCornerShape(VotDimens.radiusCard)

/**
 * Carte MiniPlayer, sans ViewModel : elle dessine [nowPlaying] et signale les actions.
 *
 * Elle reste affichée même sans lecture — l'encadrement est une donnée de l'écran, pas de
 * l'état de lecture (voir `ScreenChrome`) — et dit alors pourquoi il n'y a rien à lire.
 */
@Composable
fun MiniPlayer(
    nowPlaying: NowPlaying?,
    connection: PlayerConnection,
    isSaved: Boolean?,
    onOpen: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleSave: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onPreviousTrack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(VinylSurfaceCard, CardShape)
            .border(1.dp, VinylOutline, CardShape),
    ) {
        // La barre est un frère de la ligne cliquable, pas un enfant : un tap dessus cherche dans le
        // titre au lieu d'ouvrir le Lecteur.
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = nowPlaying != null, onClick = onOpen)
                    .padding(start = VotDimens.space10, end = VotDimens.space10, top = VotDimens.space10),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VotDimens.space12),
        ) {
        // Disque et titre se balaient ensemble : glisser vers la gauche passe au titre suivant,
        // vers la droite au précédent. Les boutons, eux, restent fixes.
        SwipeToSkip(
            trackKey = nowPlaying?.track,
            onNext = onSkipNext,
            onPrevious = onPreviousTrack,
            modifier = Modifier.weight(1f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VotDimens.space12),
            ) {
                VinylMiniTurntable(spinning = nowPlaying?.isPaused == false) {
                    SpotifyTrackCover(
                        modifier = Modifier.fillMaxSize(),
                        model = spotifyImageUrl(nowPlaying?.coverImageUri),
                    )
                }

                Column(Modifier.weight(1f)) {
                    MarqueeTextInfinite(
                        text = nowPlaying?.title ?: idleTitle(connection),
                        textStyle = ListTitleTextStyle.copy(fontSize = 15.sp, lineHeight = 22.sp),
                        color = VinylTextEmphasis,
                        fadeColor = VinylSurfaceCard,
                        // Le MiniPlayer n'est pas dans une liste : il défile même quand la page derrière défile.
                        enabled = true,
                    )
                    nowPlaying?.let {
                        MarqueeTextInfinite(
                            text = it.artist,
                            textStyle = ListTitleTextStyle.copy(fontSize = 12.sp, lineHeight = 18.sp),
                            color = VinylTextSecondary,
                            fadeColor = VinylSurfaceCard,
                            enabled = true,
                        )
                    }
                }
            }
        }

        if (nowPlaying != null) {
            // Toujours affiché : tant que l'état est inconnu, le cœur est vide et un tap le relit.
            val saved = isSaved == true
            IconButton(onClick = onToggleSave) {
                Icon(
                    imageVector = if (saved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription =
                        stringResource(if (saved) R.string.content_desc_remove_from_library else R.string.content_desc_save_to_library),
                    tint = if (saved) VinylPurple else VinylTextSecondary,
                )
            }
            IconButton(onClick = onTogglePlayPause) {
                Icon(
                    imageVector = if (nowPlaying.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription =
                        stringResource(if (nowPlaying.isPaused) R.string.content_desc_play else R.string.content_desc_pause),
                    tint = VinylTextEmphasis,
                    modifier = Modifier.size(VotDimens.touchMin / 2),
                )
            }
        }
        }

        if (nowPlaying != null) {
            SeekBar(
                positionMs = rememberLivePosition(nowPlaying),
                durationMs = nowPlaying.durationMs,
                onSeek = onSeek,
                modifier = Modifier.padding(horizontal = VotDimens.space10),
            )
        } else {
            Spacer(Modifier.height(VotDimens.space10))
        }
    }
}

@Composable
private fun idleTitle(connection: PlayerConnection): String =
    stringResource(
        when (connection) {
            PlayerConnection.Connected -> R.string.mini_idle_connected
            PlayerConnection.Connecting -> R.string.mini_idle_connecting
            PlayerConnection.NotConnected -> R.string.mini_idle_connecting
            is PlayerConnection.Failed -> R.string.mini_idle_failed
        },
    )

private val previewNowPlaying =
    NowPlaying(
        track = null,
        title = "Sous la neige",
        artist = "Elia Faure",
        album = "Nuits blanches",
        coverImageUri = null,
        durationMs = 268_000,
        positionMs = 102_000,
        isPaused = false,
        shuffle = false,
        repeat = RepeatMode.Off,
        emittedAtElapsedMs = 0,
    )

@Preview(showBackground = true, backgroundColor = 0xFF0C0A10)
@Composable
private fun MiniPlayerPreview() {
    AndroidAppTheme {
        Box(Modifier.padding(VotDimens.space14)) {
            MiniPlayer(previewNowPlaying, PlayerConnection.Connected, isSaved = true, {}, {}, {}, {}, {}, {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0C0A10)
@Composable
private fun MiniPlayerIdlePreview() {
    AndroidAppTheme {
        Box(Modifier.padding(VotDimens.space14)) {
            MiniPlayer(null, PlayerConnection.Connected, isSaved = null, {}, {}, {}, {}, {}, {})
        }
    }
}
