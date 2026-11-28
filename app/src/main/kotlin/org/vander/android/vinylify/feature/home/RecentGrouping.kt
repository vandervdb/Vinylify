package org.vander.android.vinylify.feature.home

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import org.vander.android.vinylify.feature.ContentKind
import org.vander.android.vinylify.util.spotifyImageUrl
import org.vander.spotify.model.PlayContextType
import org.vander.spotify.model.PlayHistory
import org.vander.spotify.model.Playlist

private const val DAYS_IN_WEEK = 7

/**
 * Range l'historique [history] (du plus récent au plus ancien) en sections par jour, en réunissant
 * les écoutes consécutives d'une même source.
 *
 * Fonction pure, sans horloge ni fuseau implicites, pour être testée sans ViewModel.
 *
 * @param playlists playlists connues de l'utilisateur : l'historique ne donne que l'identifiant
 *   d'une playlist, pas son nom ni sa pochette.
 */
internal fun groupRecent(
    history: List<PlayHistory>,
    playlists: List<Playlist>,
    nowMs: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): List<RecentSection> {
    val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()

    fun dayOf(ms: Long): RecentDay {
        val days = ChronoUnit.DAYS.between(Instant.ofEpochMilli(ms).atZone(zone).toLocalDate(), today)
        return when {
            days <= 0 -> RecentDay.Today
            days == 1L -> RecentDay.Yesterday
            days < DAYS_IN_WEEK -> RecentDay.ThisWeek
            else -> RecentDay.Earlier
        }
    }

    val runs = mutableListOf<MutableList<PlayHistory>>()
    for (play in history) {
        val run = runs.lastOrNull()
        val sameRun = run != null && run.first().sourceUri == play.sourceUri && dayOf(run.first().playedAtMs) == dayOf(play.playedAtMs)
        if (sameRun) run.add(play) else runs += mutableListOf(play)
    }

    val playlistsById = playlists.associateBy { it.id.value }
    return runs
        .mapNotNull { it.toEntry(playlistsById) }
        .groupBy { dayOf(it.playedAtMs) }
        .map { (day, entries) -> RecentSection(day, entries) }
}

/** Sans contexte (titres likés, file d'attente…), on regroupe par album. */
private val PlayHistory.sourceUri: String
    get() = context?.uri ?: track.album?.uri ?: track.id.uri

private fun List<PlayHistory>.toEntry(playlists: Map<String, Playlist>): RecentEntry? {
    val latest = first()
    val track = latest.track
    val album = track.album
    val context = latest.context
    val playlist = context?.takeIf { it.type == PlayContextType.Playlist }?.let { playlists[it.id] }
    val artist = context?.takeIf { it.type == PlayContextType.Artist }?.let { c -> track.artists.firstOrNull { it.id == c.id } }

    val (kind, targetId) =
        when (context?.type) {
            PlayContextType.Album -> ContentKind.Album to context.id
            PlayContextType.Playlist -> ContentKind.Playlist to context.id
            PlayContextType.Artist -> ContentKind.Artist to context.id
            // Rien à ouvrir pour un titre sans album : on ne l'affiche pas.
            null -> ContentKind.Album to (album?.id?.takeIf { it.isNotBlank() } ?: return null)
        }

    // Une écoute seule se lit comme un titre ; un groupe comme sa source.
    val (title, detail) =
        when {
            size == 1 -> track.name to listOfNotNull(track.artistNames, playlist?.name ?: album?.name).filter { it.isNotBlank() }.joinToString(" · ")
            kind == ContentKind.Album && album != null -> album.name to album.artistNames.ifBlank { track.artistNames }
            playlist != null -> playlist.name to track.name
            artist != null -> artist.name to track.name
            else -> track.name to track.artistNames
        }

    val progress =
        album
            ?.takeIf { kind == ContentKind.Album && size > 1 && it.trackCount > 1 }
            ?.let { a -> AlbumProgress(heard = map { it.track.id }.distinct().size.coerceAtMost(a.trackCount), total = a.trackCount) }

    return RecentEntry(
        id = "${latest.playedAtMs}:${track.id.value}",
        title = title,
        detail = detail,
        coverUrl = spotifyImageUrl((playlist?.cover ?: album?.cover)?.url),
        playCount = size,
        playedAtMs = latest.playedAtMs,
        kind = kind,
        targetId = targetId,
        albumId = album?.id,
        firstPlayedAtMs = last().playedAtMs,
        totalDurationMs = sumOf { it.track.durationMs },
        albumProgress = progress,
        tracks = map { RecentTrack(id = it.track.id.value, title = it.track.name, artists = it.track.artistNames, playedAtMs = it.playedAtMs) },
    )
}
