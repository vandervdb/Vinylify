package org.vander.android.vinylify.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.vander.android.vinylify.designsystem.ListTitleTextStyle
import org.vander.android.vinylify.designsystem.DurationTextStyle
import org.vander.android.vinylify.R
import org.vander.android.vinylify.component.SpotifyTrackCover
import org.vander.android.vinylify.designsystem.VinylPurple
import org.vander.android.vinylify.designsystem.VinylPurpleSoft
import org.vander.android.vinylify.designsystem.VinylTextEmphasis
import org.vander.android.vinylify.designsystem.VinylTextMuted
import org.vander.android.vinylify.designsystem.VinylTextSecondary
import org.vander.android.vinylify.designsystem.VotDimens
import org.vander.android.vinylify.designsystem.component.SpinningDisc
import org.vander.android.vinylify.designsystem.component.coverHiddenArc
import org.vander.android.vinylify.feature.lyrics.LyricsState
import org.vander.android.vinylify.feature.lyrics.LyricsViewModel
import org.vander.android.vinylify.feature.lyrics.lineAt
import org.vander.android.vinylify.designsystem.component.Tonearm
import org.vander.android.vinylify.designsystem.modifier.drawGrainOverlay
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import org.vander.android.vinylify.navigation.modalEnter
import org.vander.android.vinylify.navigation.modalExit
import org.vander.android.vinylify.util.spotifyImageUrl
import org.vander.spotify.android.NowPlaying
import org.vander.spotify.android.RepeatMode

/**
 * Écran 06, **modale** : il vit à la racine du graphe, hors de `MainGraph`, donc `ScreenChrome`
 * ne lui donne ni dock ni MiniPlayer. Le `⌄` en haut à gauche encode ce registre.
 */
@Serializable
object PlayerRoute

fun NavGraphBuilder.playerNavGraph(
    playerOwner: @Composable (NavBackStackEntry) -> PlayerViewModel,
    lyricsOwner: @Composable (NavBackStackEntry) -> LyricsViewModel,
    onClose: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
) {
    composable<PlayerRoute>(
        enterTransition = { modalEnter() },
        popExitTransition = { modalExit() },
        // La File d'attente se superpose au Lecteur : il reste en place dessous, sans fondu.
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
    ) { entry ->
        val viewModel = playerOwner(entry)
        val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
        val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
        val lyricsState by lyricsOwner(entry).state.collectAsStateWithLifecycle()
        PlayerScreen(
            nowPlaying = nowPlaying,
            isSaved = isSaved,
            lyricsState = lyricsState,
            onClose = onClose,
            onOpenQueue = onOpenQueue,
            onOpenLyrics = onOpenLyrics,
            onTogglePlayPause = viewModel::togglePlayPause,
            onSkipNext = viewModel::skipNext,
            onSkipPrevious = viewModel::skipPrevious,
            onPreviousTrack = viewModel::previousTrack,
            onSeek = viewModel::seekTo,
            onToggleShuffle = viewModel::toggleShuffle,
            onCycleRepeat = viewModel::cycleRepeat,
            onToggleSave = viewModel::toggleSave,
        )
    }
}

@Composable
fun PlayerScreen(
    nowPlaying: NowPlaying?,
    isSaved: Boolean?,
    lyricsState: LyricsState,
    onClose: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onPreviousTrack: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleSave: () -> Unit,
) {
    // Dit à la platine de quel côté sortir la pochette quand un bouton change de titre.
    val skipAnimation = rememberSkipAnimationState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalSwipe(onSwipeDown = onClose, onSwipeUp = onOpenQueue)
            .background(MaterialTheme.colorScheme.background)
            .drawGrainOverlay(alpha = 0.82f)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = VotDimens.screenPadding),
    ) {
        IconButton(onClick = onClose) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.content_desc_close_player),
                tint = VinylTextEmphasis,
            )
        }

        if (nowPlaying == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.player_nothing_playing), color = VinylTextMuted)
            }
            return@Column
        }

        // Deux blocs : platine + titre en haut, progression + boutons en bas. Le vide restant se répartit
        // au-dessus de la platine, entre les deux blocs et sous les boutons.
        Spacer(Modifier.height(VotDimens.space12))
        Spacer(Modifier.weight(0.4f))
        // Balayage de la pochette : gauche = suivant, droite = précédent. Les boutons du bas gardent
        // leur sens habituel (le précédent y redémarre le titre passé 3 s).
        PlayerTurntable(
            nowPlaying = nowPlaying,
            lyricsState = lyricsState,
            onNext = onSkipNext,
            onPrevious = onPreviousTrack,
            skipAnimation = skipAnimation,
        )
        Spacer(Modifier.height(VotDimens.space12))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(nowPlaying.title, style = MaterialTheme.typography.headlineSmall, color = VinylTextEmphasis, maxLines = 2)
                Text(nowPlaying.artist, style = ListTitleTextStyle, color = VinylTextSecondary, maxLines = 1)
            }
            // Toujours affiché : tant que l'état est inconnu, le cœur est vide et un tap le relit.
            val saved = isSaved == true
            IconButton(onClick = onToggleSave) {
                Icon(
                    if (saved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription =
                        stringResource(if (saved) R.string.content_desc_remove_from_library else R.string.content_desc_save_to_library),
                    tint = if (saved) VinylPurple else VinylTextSecondary,
                )
            }
        }

        Spacer(Modifier.height(VotDimens.space20))
        Spacer(Modifier.weight(0.7f))
        ProgressBar(nowPlaying, onSeek)
        Spacer(Modifier.height(VotDimens.space12))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = stringResource(R.string.content_desc_shuffle),
                    tint = if (nowPlaying.shuffle) VinylPurpleSoft else VinylTextMuted,
                )
            }
            IconButton(onClick = {
                skipAnimation.expectPrevious()
                onSkipPrevious()
            }) {
                Icon(Icons.Filled.SkipPrevious, stringResource(R.string.content_desc_previous), tint = VinylTextEmphasis, modifier = Modifier.size(36.dp))
            }
            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier.size(72.dp).background(VinylPurple, CircleShape),
            ) {
                Icon(
                    if (nowPlaying.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription =
                        stringResource(if (nowPlaying.isPaused) R.string.content_desc_play else R.string.content_desc_pause),
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
            IconButton(onClick = {
                skipAnimation.expectNext()
                onSkipNext()
            }) {
                Icon(Icons.Filled.SkipNext, stringResource(R.string.content_desc_next), tint = VinylTextEmphasis, modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = onCycleRepeat) {
                Icon(
                    if (nowPlaying.repeat == RepeatMode.Track) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = stringResource(R.string.content_desc_repeat),
                    tint = if (nowPlaying.repeat == RepeatMode.Off) VinylTextMuted else VinylPurpleSoft,
                )
            }
        }

        Spacer(Modifier.weight(1.0f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = onOpenLyrics) {
                Text(stringResource(R.string.player_open_lyrics), color = VinylTextSecondary)
            }
            TextButton(onClick = onOpenQueue) {
                Text(stringResource(R.string.player_open_queue), color = VinylTextSecondary)
            }
        }
    }
}

/**
 * Platine du Lecteur : le disque reste en place, seule la pochette glisse au balayage de [SwipeToSkip]
 * (sa zone de geste couvre pochette + disque). Le bras, par-dessus, reste en place lui aussi, comme
 * la base d'une vraie platine.
 *
 * La pochette est réduite pour que pochette + dépassement du disque occupent exactement la largeur
 * disponible ; au-dessus, [VotDimens.TONEARM_HEADROOM_RATIO] de sa taille est réservé au pivot.
 */
@Composable
private fun PlayerTurntable(
    nowPlaying: NowPlaying,
    lyricsState: LyricsState,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    skipAnimation: SkipAnimationState,
) {
    val position = rememberLivePosition(nowPlaying)
    val progress = if (nowPlaying.durationMs > 0) position.toFloat() / nowPlaying.durationMs else 0f
    // Course de la pochette (0 = au repos, 1 = sortie) : l'ombre qu'elle porte sur le disque s'efface avec elle.
    val slide = remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cover = maxWidth / (1f + VotDimens.PLAYER_DISC_OVERHANG_RATIO)
        Box(Modifier.padding(top = cover * VotDimens.TONEARM_HEADROOM_RATIO).fillMaxWidth().height(cover)) {
            TurntableDisc(
                cover = cover,
                albumName = nowPlaying.album,
                track = nowPlaying.track,
                spinning = !nowPlaying.isPaused,
                // Lue au moment du swap : la position est celle de cet instant, pas celle de la dernière image.
                currentLyric = { lyricsState.lineAt(interpolatePosition(nowPlaying)) },
                shadowAlpha = { 1f - slide.floatValue },
            )
            SwipeToSkip(
                trackKey = nowPlaying.track,
                item = spotifyImageUrl(nowPlaying.coverImageUri),
                onNext = onNext,
                onPrevious = onPrevious,
                modifier = Modifier.fillMaxSize(),
                skipAnimation = skipAnimation,
                slideFraction = slide,
            ) { coverUrl ->
                TurntableCover(cover = cover, coverUrl = coverUrl)
            }
        }
        Tonearm(
            progress = progress,
            playing = !nowPlaying.isPaused,
            discOverhangRatio = VotDimens.PLAYER_DISC_OVERHANG_RATIO,
            modifier = Modifier.matchParentSize(),
        )
    }
}

private const val GROOVE_RING_RADIUS = 0.83f

private val CoverShape = RoundedCornerShape(4.dp)
private val COVER_ELEVATION = 18.dp

/** Ombre de la pochette sur le disque : opacité au bord, et portée en part du diamètre du disque. */
private const val COVER_SHADOW_ALPHA = 0.9f
private const val COVER_SHADOW_REACH_RATIO = 0.24f

/** Arc maximal du texte gravé ; en dessous de 150° il peut tenir entièrement derrière la pochette. */
private const val GROOVE_TEXT_MAX_ARC_DEGREES = 150f

private val HIDDEN_ARC =
    coverHiddenArc(VotDimens.PLAYER_DISC_OVERHANG_RATIO, GROOVE_RING_RADIUS, GROOVE_TEXT_MAX_ARC_DEGREES)

/**
 * Le disque, immobile dans son cadre : il a le diamètre de la pochette ([cover]), passe derrière elle
 * et dépasse à droite de [VotDimens.PLAYER_DISC_OVERHANG_RATIO]. Le nom de l'album, ou la ligne de
 * paroles en cours, est gravé sur un sillon et tourne avec lui.
 *
 * @param shadowAlpha opacité de l'ombre de la pochette sur le disque, lue au dessin : elle baisse
 *   quand la pochette s'éloigne.
 */
@Composable
private fun BoxScope.TurntableDisc(
    cover: Dp,
    albumName: String,
    track: Any?,
    spinning: Boolean,
    currentLyric: () -> String?,
    shadowAlpha: () -> Float,
) {
    SpinningDisc(
        spinning = spinning,
        grooveText = albumName,
        // Bande lisse extérieure, centrée à droite : c'est la seule partie du disque hors de la pochette.
        grooveTextRadius = GROOVE_RING_RADIUS,
        grooveTextAngleDegrees = 0f,
        // La ligne de paroles ne remplace le nom de l'album que lorsque le texte est derrière la pochette.
        grooveTextMaxArcDegrees = GROOVE_TEXT_MAX_ARC_DEGREES,
        hiddenArc = HIDDEN_ARC,
        swappedGrooveText = currentLyric,
        // Nouveau titre : le nom de l'album revient tout de suite, tant que les paroles n'ont pas commencé.
        resetKey = track,
        // Calé à droite : il dépasse de la pochette de PLAYER_DISC_OVERHANG_RATIO, sans décalage en plus.
        modifier = Modifier.size(cover).align(Alignment.CenterEnd),
    )
    // Ombre de la pochette sur le disque. À part, donc fixe : le disque tourne, pas l'ombre. Limitée
    // au disque, car au-delà de la pochette il n'y a rien sur quoi la porter. Lumière en haut à gauche.
    Canvas(Modifier.size(cover).align(Alignment.CenterEnd).graphicsLayer { alpha = shadowAlpha() }) {
        val edge = size.width * (1f - VotDimens.PLAYER_DISC_OVERHANG_RATIO)
        clipPath(Path().apply { addOval(Rect(Offset.Zero, size)) }) {
            drawRect(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = COVER_SHADOW_ALPHA),
                    1f to Color.Transparent,
                    startX = edge,
                    endX = edge + size.width * COVER_SHADOW_REACH_RATIO,
                ),
            )
        }
    }
}

/** La pochette, posée au-dessus du disque : ombre portée, puis arête éclairée par le haut à gauche. */
@Composable
private fun TurntableCover(
    cover: Dp,
    coverUrl: String?,
) {
    SpotifyTrackCover(
        modifier =
            Modifier
                .size(cover)
                .shadow(COVER_ELEVATION, CoverShape, ambientColor = Color.Black, spotColor = Color.Black)
                .drawWithContent {
                    drawContent()
                    drawRoundRect(
                        brush =
                            Brush.linearGradient(
                                0f to Color.White.copy(alpha = 0.30f),
                                0.5f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.35f),
                            ),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                },
        model = coverUrl,
    )
}

/**
 * Barre de progression interpolée.
 *
 * Le SDK ne pousse la position qu'aux changements d'état ; elle ne s'écoule pas toute seule.
 * On repart donc de `positionMs` au moment `emittedAtElapsedMs` et on ajoute le temps écoulé
 * tant que la lecture n'est pas en pause. Pendant un glissement, la position affichée est celle
 * du doigt, et le `seekTo` n'est envoyé qu'au relâchement.
 */
@Composable
private fun ProgressBar(
    nowPlaying: NowPlaying,
    onSeek: (Long) -> Unit,
) {
    var position by remember(nowPlaying) { mutableLongStateOf(interpolatePosition(nowPlaying)) }
    var dragging by remember { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(nowPlaying) {
        while (true) {
            if (!dragging) position = interpolatePosition(nowPlaying)
            delay(TICK_MS)
        }
    }

    Column {
        Slider(
            value = position.toFloat(),
            onValueChange = {
                dragging = true
                position = it.toLong()
            },
            onValueChangeFinished = {
                onSeek(position)
                dragging = false
            },
            valueRange = 0f..nowPlaying.durationMs.coerceAtLeast(1).toFloat(),
            colors =
                SliderDefaults.colors(
                    thumbColor = VinylPurple,
                    activeTrackColor = VinylPurple,
                    inactiveTrackColor = VinylTextMuted.copy(alpha = 0.3f),
                ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(position), style = DurationTextStyle, color = VinylTextSecondary)
            Text(formatTime(nowPlaying.durationMs), style = DurationTextStyle, color = VinylTextSecondary)
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

