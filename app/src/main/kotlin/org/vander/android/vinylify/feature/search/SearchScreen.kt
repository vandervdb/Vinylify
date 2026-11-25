package org.vander.android.vinylify.feature.search

import org.vander.android.vinylify.designsystem.component.MarqueeLazyColumn
import org.vander.android.vinylify.feature.ContentKind
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.vander.android.vinylify.R
import org.vander.android.vinylify.component.ContentRow
import org.vander.android.vinylify.component.TrackLine
import org.vander.android.vinylify.designsystem.AndroidAppTheme
import org.vander.android.vinylify.designsystem.VinylTextEmphasis
import org.vander.android.vinylify.designsystem.component.VinylFilterChipRow
import org.vander.android.vinylify.designsystem.VinylTextMuted
import org.vander.android.vinylify.designsystem.VinylTextSecondary
import org.vander.android.vinylify.designsystem.VotDimens
import org.vander.android.vinylify.util.spotifyImageUrl
import org.vander.spotify.model.Album
import org.vander.spotify.model.Artist
import org.vander.spotify.model.Playlist
import org.vander.spotify.model.PlaylistId
import org.vander.spotify.model.SearchResults

@Serializable
object SearchRoute

fun NavGraphBuilder.searchNavGraph(onOpen: (ContentKind, String) -> Unit) {
    composable<SearchRoute> {
        val viewModel: SearchViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        SearchContent(
            state = state,
            onQueryChange = viewModel::onQueryChange,
            onFilterSelect = viewModel::onFilterSelect,
            onAlbumClick = { onOpen(ContentKind.Album, it.id) },
            onArtistClick = { onOpen(ContentKind.Artist, it.id) },
            onPlaylistClick = { onOpen(ContentKind.Playlist, it.id.value) },
            onTrackClick = viewModel::onTrackClick,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun SearchContent(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onFilterSelect: (SearchFilter) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onTrackClick: (org.vander.spotify.model.Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.statusBarsPadding()) {
        Text(
            stringResource(R.string.search_title),
            style = MaterialTheme.typography.headlineLarge,
            color = VinylTextEmphasis,
            modifier = Modifier.padding(start = VotDimens.screenPadding, top = VotDimens.space20, bottom = VotDimens.space12),
        )
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.search_hint)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.fillMaxWidth().padding(horizontal = VotDimens.screenPadding),
        )
        // Les mêmes pastilles que l'Accueil : le genre choisi part dans le paramètre `type` de Spotify.
        VinylFilterChipRow(
            options = SearchFilter.entries,
            selected = state.filter,
            label = { stringResource(it.labelRes) },
            onSelect = onFilterSelect,
            modifier = Modifier.padding(top = VotDimens.space12),
        )

        when {
            state.status == SearchStatus.Failed -> Message(stringResource(R.string.search_error))
            state.status == SearchStatus.Idle -> Message(stringResource(R.string.search_empty_hint))
            state.status == SearchStatus.Done && state.results.isEmpty ->
                Message(stringResource(R.string.search_no_results, state.query))
            else -> Results(state.results, state.filter == SearchFilter.All, onAlbumClick, onArtistClick, onPlaylistClick, onTrackClick)
        }
    }
}

@Composable
private fun Message(text: String) {
    Box(Modifier.fillMaxSize().padding(VotDimens.screenPadding), contentAlignment = Alignment.TopCenter) {
        Text(text, color = VinylTextMuted, modifier = Modifier.padding(top = VotDimens.space32))
    }
}

@Composable
private fun Results(
    results: SearchResults,
    showHeaders: Boolean,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onTrackClick: (org.vander.spotify.model.Track) -> Unit,
) {
    MarqueeLazyColumn(
        contentPadding = PaddingValues(horizontal = VotDimens.screenPadding, vertical = VotDimens.space14),
        verticalArrangement = Arrangement.spacedBy(VotDimens.space12),
    ) {
        section(R.string.search_section_albums, show = showHeaders && results.albums.isNotEmpty())
        items(results.albums, key = { "album:${it.id}" }) { album ->
            ContentRow(
                title = album.name,
                subtitle = stringResource(R.string.library_sub_album, album.artistNames),
                coverUrl = spotifyImageUrl(album.cover?.url),
                round = false,
                onClick = { onAlbumClick(album) },
            )
        }
        section(R.string.search_section_artists, show = showHeaders && results.artists.isNotEmpty())
        items(results.artists, key = { "artist:${it.id}" }) { artist ->
            ContentRow(artist.name, stringResource(R.string.search_artist_sub), spotifyImageUrl(artist.cover?.url), true, { onArtistClick(artist) })
        }
        section(R.string.search_section_playlists, show = showHeaders && results.playlists.isNotEmpty())
        items(results.playlists, key = { "playlist:${it.id.value}" }) { playlist ->
            ContentRow(
                playlist.name,
                androidx.compose.ui.res.pluralStringResource(R.plurals.library_sub_playlist, playlist.trackCount, playlist.trackCount),
                spotifyImageUrl(playlist.cover?.url),
                false,
                { onPlaylistClick(playlist) },
            )
        }
        section(R.string.search_section_tracks, show = showHeaders && results.tracks.isNotEmpty())
        items(results.tracks, key = { "track:${it.id.value}" }) { track ->
            TrackLine(null, track.name, track.artistNames, track.durationMs, { onTrackClick(track) })
        }
    }
}

/** En-tête de section, omis quand la section est vide. */
private fun androidx.compose.foundation.lazy.LazyListScope.section(
    titleRes: Int,
    show: Boolean = true,
) {
    if (!show) return
    item(key = "header:$titleRes") {
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium, color = VinylTextSecondary)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0C0A10, heightDp = 700)
@Composable
private fun SearchContentPreview() {
    AndroidAppTheme {
        SearchContent(
            state =
                SearchUiState(
                    query = "elia",
                    status = SearchStatus.Done,
                    results =
                        SearchResults(
                            albums = listOf(Album("a1", "Nuits blanches", artists = listOf(Artist("r1", "Elia Faure")))),
                            artists = listOf(Artist("r1", "Elia Faure")),
                            playlists = listOf(Playlist(PlaylistId("p1"), "Sillons", trackCount = 38)),
                        ),
                ),
            onQueryChange = {},
            onFilterSelect = {},
            onAlbumClick = {},
            onArtistClick = {},
            onPlaylistClick = {},
            onTrackClick = {},
        )
    }
}
