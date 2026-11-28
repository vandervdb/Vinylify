package org.vander.android.vinylify.feature.home

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.vander.android.vinylify.R
import org.vander.android.vinylify.component.SpotifyTrackCover
import org.vander.android.vinylify.designsystem.AndroidAppTheme
import org.vander.android.vinylify.designsystem.ListTitleTextStyle
import org.vander.android.vinylify.designsystem.VinylPurpleSoft
import org.vander.android.vinylify.designsystem.VinylTextEmphasis
import org.vander.android.vinylify.designsystem.VinylTextSecondary
import org.vander.android.vinylify.designsystem.VinylTextTertiary
import org.vander.android.vinylify.designsystem.VotDimens
import org.vander.android.vinylify.designsystem.component.VinylBrandTopBar
import org.vander.android.vinylify.designsystem.component.VinylHeroBackdrop
import org.vander.android.vinylify.designsystem.component.VinylHeroFeature
import org.vander.android.vinylify.designsystem.component.VinylSectionHeader
import org.vander.android.vinylify.designsystem.modifier.fullBleed
import org.vander.android.vinylify.designsystem.withNumerals
import org.vander.android.vinylify.feature.ContentKind

/** Air between the top bar and the resume block, which scrolls under it. */
private val RESUME_TOP = VotDimens.space20

private val SIDE_PADDING = VotDimens.listPaddingStart

private val COVER_SIZE = 56.dp

/** Place du chevron, réservée aussi sur les écoutes seules pour aligner les heures. */
private val TOGGLE_SIZE = 40.dp

private const val MINUTES_PER_HOUR = 60

private const val KEY_RESUME = "home:resume"
private const val KEY_SECTION = "home:section"
private const val KEY_ERROR = "home:error"
private const val KEY_EMPTY = "home:empty"

/**
 * The whole Accueil screen, without a ViewModel: it draws [state] and reports what the user does.
 *
 * Stateless on purpose. It is previewable in every state, and `HomeScreen` only has to collect
 * the ViewModel and pass values and lambdas through. Every callback defaults to doing nothing.
 *
 * « Dernièrement écouté » est une liste et non une grille : chaque ligne porte une heure et un
 * nombre de titres, qui ne tiennent pas sous une vignette d'un tiers d'écran.
 */
@Composable
fun HomeContent(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onResumeListen: () -> Unit = {},
    onResumeToggleSave: () -> Unit = {},
    onEntryClick: (RecentEntry) -> Unit = {},
    onTrackClick: (trackId: String) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    Box(modifier) {
        // Fixed layer: the gradient never scrolls. The empty content makes it measure exactly
        // status bar + heroHeight.
        VinylHeroBackdrop(Modifier.align(Alignment.TopStart)) {}

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            VinylBrandTopBar(onSearchClick = onSearchClick, onSettingsClick = onSettingsClick)

            // Le héros s'insère au-dessus quand l'historique arrive : la liste garde sa première
            // ligne visible ancrée par clé et le laisserait hors écran.
            val listState = rememberLazyListState()
            LaunchedEffect(state.resume != null) { listState.scrollToItem(0) }

            // Une seule horloge par composition des données : toutes les lignes parlent du même « maintenant ».
            val nowMs = remember(state.sections) { System.currentTimeMillis() }

            // Une liste (sérialisable) plutôt qu'un Set : rememberSaveable la garde à la rotation.
            var expanded by rememberSaveable { mutableStateOf(listOf<String>()) }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = SIDE_PADDING, end = SIDE_PADDING, top = RESUME_TOP, bottom = VotDimens.space20),
                verticalArrangement = Arrangement.spacedBy(VotDimens.space14),
            ) {
                state.resume?.let { resume ->
                    item(key = KEY_RESUME, contentType = KEY_RESUME) {
                        VinylHeroFeature(
                            kicker = stringResource(R.string.home_resume_kicker),
                            title = resume.title,
                            subtitle = resume.subtitle,
                            isSaved = resume.isSaved,
                            onListen = onResumeListen,
                            onToggleSave = onResumeToggleSave,
                            modifier = Modifier.fullBleed(SIDE_PADDING, SIDE_PADDING),
                        )
                    }
                }

                item(key = KEY_SECTION, contentType = KEY_SECTION) {
                    VinylSectionHeader(
                        title = stringResource(R.string.home_recent_title),
                        trailing = null,
                        modifier = Modifier.padding(top = VotDimens.space7),
                    )
                }

                when {
                    state.loadFailed && state.sections.isEmpty() ->
                        item(key = KEY_ERROR, contentType = KEY_ERROR) { LoadFailed(onRetry) }
                    state.isLoaded && state.sections.isEmpty() ->
                        item(key = KEY_EMPTY, contentType = KEY_EMPTY) { EmptyHistory() }
                }

                state.sections.forEach { section ->
                    item(key = "day:${section.day}", contentType = "day") { DayHeader(stringResource(section.day.labelRes)) }
                    items(section.entries, key = { it.id }, contentType = { "entry" }) { entry ->
                        RecentRow(
                            entry = entry,
                            day = section.day,
                            nowMs = nowMs,
                            isPlaying = entry.id == state.playingEntryId,
                            isExpanded = entry.id in expanded,
                            onClick = { onEntryClick(entry) },
                            onToggle = { expanded = if (entry.id in expanded) expanded - entry.id else expanded + entry.id },
                            onTrackClick = onTrackClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = VinylTextSecondary,
        modifier = Modifier.padding(top = VotDimens.space7),
    )
}

@Composable
private fun RecentRow(
    entry: RecentEntry,
    day: RecentDay,
    nowMs: Long,
    isPlaying: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onTrackClick: (String) -> Unit,
) {
    val isGroup = entry.playCount > 1
    val subtitle =
        if (isGroup) {
            listOf(
                pluralStringResource(R.plurals.home_recent_track_count, entry.playCount, entry.playCount),
                durationLabel(entry.totalDurationMs),
                entry.detail,
            ).filter { it.isNotBlank() }.joinToString(" · ")
        } else {
            entry.detail
        }
    val time =
        when {
            isPlaying -> stringResource(R.string.home_recent_playing)
            isGroup -> playedRangeLabel(entry.firstPlayedAtMs, entry.playedAtMs, day).joinToString(" ")
            else -> playedAtLabel(entry.playedAtMs, day, nowMs)
        }
    val progress = entry.albumProgress?.let { stringResource(R.string.home_recent_album_progress, it.heard, it.total) }
    val description = listOfNotNull(entry.title, subtitle, time, progress).filter { it.isNotBlank() }.joinToString(", ")

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
                    .clearAndSetSemantics { contentDescription = description },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VotDimens.space14),
            ) {
                SpotifyTrackCover(
                    modifier =
                        Modifier
                            .size(COVER_SIZE)
                            .clip(if (entry.kind == ContentKind.Artist) CircleShape else RoundedCornerShape(VotDimens.radiusSleeve)),
                    model = entry.coverUrl,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.title,
                        style = ListTitleTextStyle,
                        color = if (isPlaying) VinylPurpleSoft else VinylTextEmphasis,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        subtitle.withNumerals(),
                        style = MaterialTheme.typography.bodySmall,
                        color = VinylTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    // Une plage sur une ligne mange un tiers de la largeur avec l'espacement du labelSmall.
                    val timeLines = if (isGroup && !isPlaying) playedRangeLabel(entry.firstPlayedAtMs, entry.playedAtMs, day) else listOf(time)
                    timeLines.forEach { line ->
                        Text(
                            line.withNumerals(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPlaying) VinylPurpleSoft else VinylTextTertiary,
                        )
                    }
                    progress?.let {
                        Text(it.withNumerals(), style = MaterialTheme.typography.labelSmall, color = VinylTextTertiary)
                    }
                }
            }
            if (isGroup) {
                val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "chevron")
                IconButton(onClick = onToggle, modifier = Modifier.size(TOGGLE_SIZE)) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription =
                            stringResource(if (isExpanded) R.string.home_recent_collapse else R.string.home_recent_expand),
                        tint = VinylTextSecondary,
                        modifier = Modifier.rotate(rotation),
                    )
                }
            } else {
                Spacer(Modifier.width(TOGGLE_SIZE))
            }
        }
        if (isGroup) {
            AnimatedVisibility(visible = isExpanded) {
                Column(Modifier.padding(start = COVER_SIZE + VotDimens.space14, top = VotDimens.space7)) {
                    entry.tracks.forEach { track -> PlayedTrackRow(track, onClick = { onTrackClick(track.id) }) }
                }
            }
        }
    }
}

@Composable
private fun PlayedTrackRow(
    track: RecentTrack,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val time = DateUtils.formatDateTime(context, track.playedAtMs, DateUtils.FORMAT_SHOW_TIME)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = VotDimens.space7),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VotDimens.space14),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = MaterialTheme.typography.bodyMedium,
                color = VinylTextEmphasis,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                track.artists,
                style = MaterialTheme.typography.bodySmall,
                color = VinylTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(time.withNumerals(), style = MaterialTheme.typography.labelSmall, color = VinylTextTertiary)
        Spacer(Modifier.width(TOGGLE_SIZE))
    }
}

/** Arrondi à la minute ; vide sous une minute, où l'estimation ne veut plus rien dire. */
@Composable
private fun durationLabel(durationMs: Long): String {
    val minutes = (durationMs / DateUtils.MINUTE_IN_MILLIS).toInt()
    return when {
        minutes < 1 -> ""
        minutes < MINUTES_PER_HOUR -> stringResource(R.string.home_recent_duration_minutes, minutes)
        else -> stringResource(R.string.home_recent_duration_hours, minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
    }
}

/**
 * Plage d'un groupe, en deux lignes « 17:30 – » / « 18:15 » ; au-delà d'une semaine, la date seule.
 * Le jour de la semaine ne précède que le début : la fin est le même jour, la coupure à minuit
 * étant faite au regroupement.
 */
@Composable
private fun playedRangeLabel(
    startMs: Long,
    endMs: Long,
    day: RecentDay,
): List<String> {
    val context = LocalContext.current
    val startFlags =
        when (day) {
            RecentDay.Today, RecentDay.Yesterday -> DateUtils.FORMAT_SHOW_TIME
            RecentDay.ThisWeek -> DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY
            RecentDay.Earlier -> return listOf(DateUtils.formatDateTime(context, endMs, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH))
        }
    return listOf(
        DateUtils.formatDateTime(context, startMs, startFlags) + " –",
        DateUtils.formatDateTime(context, endMs, DateUtils.FORMAT_SHOW_TIME),
    )
}

/**
 * Le jour est déjà dans l'en-tête de section : aujourd'hui une durée (« il y a 2 h »), hier l'heure,
 * cette semaine le jour et l'heure, au-delà la date.
 */
@Composable
private fun playedAtLabel(
    playedAtMs: Long,
    day: RecentDay,
    nowMs: Long,
): String {
    val context = LocalContext.current
    return when (day) {
        RecentDay.Today ->
            DateUtils
                .getRelativeTimeSpanString(playedAtMs, nowMs, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE)
                .toString()
        RecentDay.Yesterday -> DateUtils.formatDateTime(context, playedAtMs, DateUtils.FORMAT_SHOW_TIME)
        RecentDay.ThisWeek ->
            DateUtils.formatDateTime(context, playedAtMs, DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY or DateUtils.FORMAT_SHOW_TIME)
        RecentDay.Earlier -> DateUtils.formatDateTime(context, playedAtMs, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)
    }
}

@Composable
private fun EmptyHistory() {
    Text(
        stringResource(R.string.home_recent_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = VinylTextSecondary,
        modifier = Modifier.fillMaxWidth().padding(top = VotDimens.space30),
    )
}

@Composable
private fun LoadFailed(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = VotDimens.space30),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VotDimens.space14),
    ) {
        Text(
            stringResource(R.string.home_load_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = VinylTextSecondary,
        )
        Button(onClick = onRetry) { Text(stringResource(R.string.home_load_retry)) }
    }
}

private const val PREVIEW_NOW = 1_795_860_000_000L
private const val MINUTE_MS = 60_000L

private fun previewEntry(
    id: String,
    title: String,
    detail: String,
    minutesAgo: Long,
    playCount: Int = 1,
    kind: ContentKind = ContentKind.Album,
) = RecentEntry(
    id = id,
    title = title,
    detail = detail,
    coverUrl = null,
    playCount = playCount,
    playedAtMs = PREVIEW_NOW - minutesAgo * MINUTE_MS,
    kind = kind,
    targetId = id,
    albumId = id,
    firstPlayedAtMs = PREVIEW_NOW - (minutesAgo + playCount * 4) * MINUTE_MS,
    totalDurationMs = playCount * 4 * MINUTE_MS,
    albumProgress = if (kind == ContentKind.Album && playCount > 1) AlbumProgress(playCount, 10) else null,
    tracks =
        List(playCount) { i ->
            RecentTrack(id = "$id-$i", title = "Titre ${i + 1}", artists = detail, playedAtMs = PREVIEW_NOW - (minutesAgo + i * 4) * MINUTE_MS)
        },
)

private val previewState =
    HomeUiState(
        sections =
            listOf(
                RecentSection(
                    RecentDay.Today,
                    listOf(
                        previewEntry("a1", "Nuits blanches", "Elia Faure", minutesAgo = 4, playCount = 6),
                        previewEntry("p1", "Sillons", "Onde longue", minutesAgo = 52, playCount = 3, kind = ContentKind.Playlist),
                        previewEntry("t1", "Braise", "Cléo Wend · Or mat", minutesAgo = 130),
                    ),
                ),
                RecentSection(
                    RecentDay.Yesterday,
                    listOf(
                        previewEntry("r1", "Cléo Wend", "Gris perle", minutesAgo = 1_500, playCount = 4, kind = ContentKind.Artist),
                        previewEntry("a2", "Marée haute", "Elia Faure · Marée haute", minutesAgo = 1_620),
                    ),
                ),
            ),
        playingEntryId = "a1",
        resume = ResumeListening(title = "Nuits blanches", subtitle = "Elia Faure", isSaved = null),
        isLoaded = true,
    )

@Preview(showBackground = true, backgroundColor = 0xFF0C0A10, heightDp = 900)
@Composable
private fun HomeContentPreview() {
    AndroidAppTheme { HomeContent(state = previewState, modifier = Modifier.fillMaxSize()) }
}

@Preview(showBackground = true, backgroundColor = 0xFF0C0A10, heightDp = 900, name = "Nothing played yet")
@Composable
private fun HomeContentEmptyPreview() {
    AndroidAppTheme { HomeContent(state = HomeUiState(isLoaded = true), modifier = Modifier.fillMaxSize()) }
}

@Preview(showBackground = true, backgroundColor = 0xFF0C0A10, heightDp = 900, name = "Load failed")
@Composable
private fun HomeContentFailedPreview() {
    AndroidAppTheme { HomeContent(state = HomeUiState(loadFailed = true), modifier = Modifier.fillMaxSize()) }
}
