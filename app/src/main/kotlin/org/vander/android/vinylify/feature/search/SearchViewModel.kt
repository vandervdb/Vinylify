package org.vander.android.vinylify.feature.search

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vander.spotify.SpotifyClient
import org.vander.spotify.android.SpotifyPlayer
import org.vander.android.vinylify.R
import org.vander.spotify.model.SearchResults
import org.vander.spotify.model.SearchType
import org.vander.spotify.model.Track
import javax.inject.Inject

enum class SearchStatus { Idle, Loading, Done, Failed }

/** Genre de résultat à chercher : [types] part tel quel dans le paramètre `type` de Spotify. */
enum class SearchFilter(
    @StringRes val labelRes: Int,
    val types: Set<SearchType>,
) {
    All(R.string.home_filter_all, SearchType.entries.toSet()),
    Tracks(R.string.search_section_tracks, setOf(SearchType.Track)),
    Albums(R.string.home_filter_albums, setOf(SearchType.Album)),
    Artists(R.string.home_filter_artists, setOf(SearchType.Artist)),
    Playlists(R.string.home_filter_playlists, setOf(SearchType.Playlist)),
}

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.All,
    val results: SearchResults = SearchResults.EMPTY,
    val status: SearchStatus = SearchStatus.Idle,
)

/**
 * Recherche (maquette écran 03).
 *
 * Chaque frappe annule la recherche en attente et en relance une après [DEBOUNCE_MS] : on ne
 * cherche pas à chaque lettre, et une réponse tardive d'une ancienne requête ne peut pas
 * écraser celle de la requête courante (le `Job` est annulé avant d'écrire).
 */
@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val spotify: SpotifyClient,
        private val player: SpotifyPlayer,
    ) : ViewModel() {
        private val _state = MutableStateFlow(SearchUiState())
        val state: StateFlow<SearchUiState> = _state.asStateFlow()

        private var searchJob: Job? = null

        fun onQueryChange(query: String) {
            _state.update { it.copy(query = query) }
            search(debounce = true)
        }

        /** Changer de genre relance la recherche tout de suite : ce n'est pas une frappe. */
        fun onFilterSelect(filter: SearchFilter) {
            if (filter == _state.value.filter) return
            _state.update { it.copy(filter = filter) }
            search(debounce = false)
        }

        private fun search(debounce: Boolean) {
            searchJob?.cancel()
            val (query, filter) = _state.value.let { it.query to it.filter }
            if (query.isBlank()) {
                _state.update { it.copy(results = SearchResults.EMPTY, status = SearchStatus.Idle) }
                return
            }
            searchJob =
                viewModelScope.launch {
                    if (debounce) delay(DEBOUNCE_MS)
                    _state.update { it.copy(status = SearchStatus.Loading) }
                    spotify.search(query, types = filter.types).fold(
                        onSuccess = { results -> _state.update { it.copy(results = results, status = SearchStatus.Done) } },
                        onFailure = { _state.update { it.copy(results = SearchResults.EMPTY, status = SearchStatus.Failed) } },
                    )
                }
        }

        fun onTrackClick(track: Track) {
            viewModelScope.launch { player.play(track.id) }
        }

        companion object {
            const val DEBOUNCE_MS = 350L
        }
    }
