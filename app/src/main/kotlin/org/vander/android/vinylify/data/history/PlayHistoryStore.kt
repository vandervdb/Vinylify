package org.vander.android.vinylify.data.history

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.vander.spotify.model.Album
import org.vander.spotify.model.Artist
import org.vander.spotify.model.Image
import org.vander.spotify.model.PlayContext
import org.vander.spotify.model.PlayContextType
import org.vander.spotify.model.PlayHistory
import org.vander.spotify.model.Track
import org.vander.spotify.model.TrackId
import javax.inject.Inject

/**
 * Historique d'écoute accumulé sur l'appareil, du plus récent au plus ancien.
 *
 * Spotify ne rend que ~50 écoutes et ne pagine pas au-delà : sans ce stockage, l'accueil ne
 * remonterait jamais plus loin que la veille.
 */
interface PlayHistoryStore {
    val history: Flow<List<PlayHistory>>

    /** Ajoute les écoutes inconnues et oublie celles antérieures à [keepAfterMs]. */
    suspend fun merge(
        plays: List<PlayHistory>,
        keepAfterMs: Long,
    )
}

class RoomPlayHistoryStore
    @Inject
    constructor(
        private val dao: PlayHistoryDao,
    ) : PlayHistoryStore {
        override val history: Flow<List<PlayHistory>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

        override suspend fun merge(
            plays: List<PlayHistory>,
            keepAfterMs: Long,
        ) {
            dao.insertAll(plays.filter { it.playedAtMs >= keepAfterMs }.map { it.toEntity() })
            dao.deleteOlderThan(keepAfterMs)
        }
    }

internal fun PlayHistory.toEntity() =
    PlayHistoryEntity(
        playedAtMs = playedAtMs,
        trackId = track.id.value,
        trackName = track.name,
        durationMs = track.durationMs,
        artists = track.artists.encode(),
        albumId = track.album?.id,
        albumName = track.album?.name,
        albumArtists = track.album?.artists?.encode(),
        albumCoverUrl = track.album?.cover?.url,
        albumTrackCount = track.album?.trackCount ?: 0,
        contextType = context?.type?.name,
        contextId = context?.id,
    )

internal fun PlayHistoryEntity.toDomain(): PlayHistory {
    val album =
        albumId?.let {
            Album(
                id = it,
                name = albumName.orEmpty(),
                images = listOfNotNull(albumCoverUrl?.let(::Image)),
                artists = decodeArtists(albumArtists),
                trackCount = albumTrackCount,
            )
        }
    // Un type inconnu (renommé dans une version future) perd son contexte plutôt que l'écoute.
    val context =
        contextType
            ?.let { type -> PlayContextType.entries.firstOrNull { it.name == type } }
            ?.let { type -> contextId?.let { PlayContext(type, it) } }
    return PlayHistory(
        track = Track(id = TrackId(trackId), name = trackName, artists = decodeArtists(artists), album = album, durationMs = durationMs),
        playedAtMs = playedAtMs,
        context = context,
    )
}

private fun List<Artist>.encode(): String = joinToString(FIELD_SEPARATOR.toString()) { "${it.id}$FIELD_SEPARATOR${it.name}" }

private fun decodeArtists(encoded: String?): List<Artist> =
    encoded
        ?.takeIf { it.isNotEmpty() }
        ?.split(FIELD_SEPARATOR)
        ?.chunked(2)
        ?.filter { it.size == 2 }
        ?.map { (id, name) -> Artist(id = id, name = name) }
        .orEmpty()
