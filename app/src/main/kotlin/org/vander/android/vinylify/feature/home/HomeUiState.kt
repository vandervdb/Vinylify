package org.vander.android.vinylify.feature.home

import org.vander.android.vinylify.feature.ContentKind
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import org.vander.android.vinylify.R

/**
 * Ce que l'écran Accueil dessine. Chaque champ a une valeur par défaut : un champ non fourni
 * rend son état vide.
 *
 * @property sections écoutes récentes par jour, du plus récent au plus ancien.
 * @property playingEntryId entrée dont le contenu est en cours de lecture, `null` sinon.
 * @property resume ce que le héros met en avant ; `null` masque le bloc.
 * @property isLoaded l'historique a été lu au moins une fois : distingue « vide » de « pas encore là ».
 * @property loadFailed le chargement a échoué malgré les nouvelles tentatives : l'écran propose de
 *   réessayer plutôt que d'afficher une liste vide sans explication.
 */
data class HomeUiState(
    val sections: List<RecentSection> = emptyList(),
    val playingEntryId: String? = null,
    val resume: ResumeListening? = null,
    val isLoaded: Boolean = false,
    val loadFailed: Boolean = false,
)

data class RecentSection(
    val day: RecentDay,
    val entries: List<RecentEntry>,
)

enum class RecentDay(
    @StringRes val labelRes: Int,
) {
    Today(R.string.home_recent_today),
    Yesterday(R.string.home_recent_yesterday),
    ThisWeek(R.string.home_recent_this_week),
    Earlier(R.string.home_recent_earlier),
}

/**
 * Une ligne de « Dernièrement écouté » : une écoute, ou plusieurs écoutes consécutives du même
 * album, de la même playlist ou du même artiste, réunies pour ne pas répéter 12 fois un album.
 *
 * @property detail seconde ligne sans le nombre de titres, que l'écran préfixe quand [playCount] > 1.
 * @property playedAtMs fin de l'écoute la plus récente du groupe.
 * @property firstPlayedAtMs écoute la plus ancienne du groupe : début de la plage horaire.
 * @property totalDurationMs somme des durées des titres. Estimation haute : un titre entre dans
 *   l'historique après ~30 s d'écoute, l'API ne dit pas s'il a été zappé.
 * @property albumProgress titres distincts écoutés sur le total de l'album, `null` hors album ou
 *   pour un album d'un seul titre.
 * @property tracks écoutes du groupe, de la plus récente à la plus ancienne.
 * @property kind et [targetId] : l'écran qu'un tap ouvre, et ce que « Écouter » relance.
 * @property albumId album du titre le plus récent, pour repérer la lecture en cours.
 */
data class RecentEntry(
    val id: String,
    val title: String,
    val detail: String,
    val coverUrl: String?,
    val playCount: Int,
    val playedAtMs: Long,
    val kind: ContentKind,
    val targetId: String,
    val albumId: String?,
    val firstPlayedAtMs: Long = playedAtMs,
    val totalDurationMs: Long = 0,
    val albumProgress: AlbumProgress? = null,
    val tracks: List<RecentTrack> = emptyList(),
)

data class AlbumProgress(
    val heard: Int,
    val total: Int,
)

data class RecentTrack(
    val id: String,
    val title: String,
    val artists: String,
    val playedAtMs: Long,
)

/**
 * @property subtitle ligne de contexte déjà composée, ex. « Elia Faure · Nuits blanches » :
 *   l'écran ne décide pas comment elle se construit.
 * @property isSaved `null` masque le cœur (inconnu, ou non sauvegardable).
 */
data class ResumeListening(
    val title: String,
    val subtitle: String,
    val isSaved: Boolean?,
)

/** Les puces de l'écran Bibliothèque, dans l'ordre de la maquette. */
enum class HomeFilter(
    @StringRes val labelRes: Int,
    @PluralsRes val countRes: Int,
) {
    All(R.string.home_filter_all, R.plurals.home_count_all),
    Albums(R.string.home_filter_albums, R.plurals.home_count_albums),
    Playlists(R.string.home_filter_playlists, R.plurals.home_count_playlists),
    Artists(R.string.home_filter_artists, R.plurals.home_count_artists),
}
