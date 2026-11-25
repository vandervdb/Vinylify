package org.vander.android.vinylify.feature.home

import org.vander.android.vinylify.feature.ContentKind
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import org.vander.android.vinylify.R

/** Une vignette de la grille : un album, une playlist ou un artiste, réduit à ce que la grille dessine. */
data class CollectionTile(
    val id: String,
    val name: String,
    val coverUrl: String?,
    val kind: ContentKind,
)

/**
 * Ce que l'écran Accueil dessine. Chaque champ a une valeur par défaut : un champ non fourni
 * rend son état vide.
 *
 * @property libraryCount total côté Spotify de ce que l'onglet [selectedFilter] liste ; il peut
 *   dépasser `tiles.size` (pagination).
 * @property playingTileId vignette dont le contenu est en cours de lecture, `null` sinon.
 * @property resume ce que le héros met en avant ; `null` masque le bloc.
 * @property loadFailed le chargement de la bibliothèque a échoué malgré les nouvelles tentatives : l'écran
 *   propose de réessayer plutôt que d'afficher une liste vide sans explication.
 */
data class HomeUiState(
    val tiles: List<CollectionTile> = emptyList(),
    val libraryCount: Int = 0,
    val playingTileId: String? = null,
    val resume: ResumeListening? = null,
    val selectedFilter: HomeFilter = HomeFilter.All,
    val loadFailed: Boolean = false,
)

/**
 * @property subtitle ligne de contexte déjà composée, ex. « Elia Faure · 2023 · 11 titres » :
 *   l'écran ne décide pas comment elle se construit.
 * @property isSaved `null` masque le cœur (inconnu, ou non sauvegardable).
 */
data class ResumeListening(
    val title: String,
    val subtitle: String,
    val isSaved: Boolean?,
)

/** Les puces sous la barre du haut, dans l'ordre de la maquette. */
enum class HomeFilter(
    @StringRes val labelRes: Int,
    @PluralsRes val countRes: Int,
) {
    All(R.string.home_filter_all, R.plurals.home_count_all),
    Albums(R.string.home_filter_albums, R.plurals.home_count_albums),
    Playlists(R.string.home_filter_playlists, R.plurals.home_count_playlists),
    Artists(R.string.home_filter_artists, R.plurals.home_count_artists),
}
