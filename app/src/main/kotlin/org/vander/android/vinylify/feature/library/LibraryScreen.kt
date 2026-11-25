package org.vander.android.vinylify.feature.library

import org.vander.android.vinylify.designsystem.ListTitleTextStyle
import org.vander.android.vinylify.designsystem.withNumerals
import org.vander.android.vinylify.designsystem.component.MarqueeLazyColumn
import org.vander.android.vinylify.designsystem.component.MarqueeTextInfinite
import org.vander.android.vinylify.feature.ContentKind
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.vander.android.vinylify.R
import org.vander.android.vinylify.component.SpotifyTrackCover
import org.vander.android.vinylify.designsystem.AndroidAppTheme
import org.vander.android.vinylify.designsystem.VinylPurple
import org.vander.android.vinylify.designsystem.VinylPurpleDim
import org.vander.android.vinylify.designsystem.VinylTextEmphasis
import org.vander.android.vinylify.designsystem.VinylTextMuted
import org.vander.android.vinylify.designsystem.VinylTextSecondary
import org.vander.android.vinylify.designsystem.VotDimens
import org.vander.android.vinylify.designsystem.component.VinylFilterChipRow
import org.vander.android.vinylify.feature.home.HomeFilter

@Serializable
object LibraryRoute

fun NavGraphBuilder.libraryNavGraph(onOpen: (ContentKind, String) -> Unit) {
    composable<LibraryRoute> { LibraryScreen(onOpen = onOpen) }
}

@Composable
fun LibraryScreen(
    onOpen: (ContentKind, String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LibraryContent(
        state = state,
        onFilterSelect = viewModel::onFilterSelect,
        onRowClick = { row -> onOpen(row.kind, row.id) },
        modifier = Modifier.fillMaxSize(),
    )
}

/** Sans ViewModel : dessine [state], signale les actions. Prévisualisable dans chaque état. */
@Composable
fun LibraryContent(
    state: LibraryUiState,
    onFilterSelect: (HomeFilter) -> Unit,
    onRowClick: (LibraryRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.statusBarsPadding()) {
        Text(
            text = stringResource(R.string.library_title),
            style = MaterialTheme.typography.headlineLarge,
            color = VinylTextEmphasis,
            modifier = Modifier.padding(start = VotDimens.screenPadding, top = VotDimens.space20, bottom = VotDimens.space12),
        )
        VinylFilterChipRow(
            options = HomeFilter.entries,
            selected = state.selectedFilter,
            label = { stringResource(it.labelRes) },
            onSelect = onFilterSelect,
        )

        // `LazyColumn` garde la première ligne visible ancrée par sa clé : la ligne « Titres likés »,
        // insérée au-dessus une fois son total chargé, resterait hors écran sans ce retour en haut.
        val listState = rememberLazyListState()
        val likedVisible =
            state.likedCount != null &&
                (state.selectedFilter == HomeFilter.All || state.selectedFilter == HomeFilter.Playlists)
        LaunchedEffect(likedVisible) { if (likedVisible) listState.scrollToItem(0) }

        MarqueeLazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = VotDimens.screenPadding, vertical = VotDimens.space14),
            verticalArrangement = Arrangement.spacedBy(VotDimens.space12),
        ) {
            // Les titres likés ne sont pas une playlist dans l'API : ligne épinglée, sans lecture
            // (l'App Remote n'a pas d'URI documentée pour la collection).
            val showLiked = state.selectedFilter == HomeFilter.All || state.selectedFilter == HomeFilter.Playlists
            val liked = state.likedCount
            if (showLiked && liked != null) {
                item(key = "liked") { LikedRow(liked) }
            }
            items(state.rows, key = { "${it.kind}:${it.id}" }) { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onRowClick(row) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VotDimens.space14),
                ) {
                    SpotifyTrackCover(
                        modifier =
                            Modifier
                                .size(COVER_SIZE)
                                .clip(if (row.kind == ContentKind.Artist) CircleShape else RoundedCornerShape(VotDimens.radiusSleeve)),
                        model = row.coverUrl,
                    )
                    RowTexts(row.title, subtitleOf(row))
                }
            }
        }
    }
}

@Composable
private fun subtitleOf(row: LibraryRow): String =
    when (row.kind) {
        ContentKind.Album ->
            row.detail?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.library_sub_album, it) }
                ?: stringResource(R.string.library_sub_album_plain)
        ContentKind.Playlist -> pluralStringResource(R.plurals.library_sub_playlist, row.trackCount, row.trackCount)
        ContentKind.Artist -> stringResource(R.string.library_sub_artist)
    }

@Composable
private fun LikedRow(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VotDimens.space14)) {
        Box(
            Modifier
                .size(COVER_SIZE)
                .background(VinylPurpleDim, RoundedCornerShape(VotDimens.radiusSleeve)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = VinylPurple)
        }
        RowTexts(
            stringResource(R.string.library_liked_title),
            pluralStringResource(R.plurals.library_sub_liked, count, count),
        )
    }
}

@Composable
private fun RowTexts(
    title: String,
    subtitle: String,
) {
    Column(Modifier.fillMaxWidth()) {
        MarqueeTextInfinite(text = title, textStyle = ListTitleTextStyle, color = VinylTextEmphasis, fadeColor = MaterialTheme.colorScheme.background)
        Text(subtitle.withNumerals(), style = MaterialTheme.typography.bodySmall, color = VinylTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private val COVER_SIZE = 56.dp

@Preview(showBackground = true, backgroundColor = 0xFF0C0A10, heightDp = 700)
@Composable
private fun LibraryContentPreview() {
    AndroidAppTheme {
        LibraryContent(
            state =
                LibraryUiState(
                    rows =
                        listOf(
                            LibraryRow("a1", ContentKind.Album, "Nuits blanches", null, detail = "Elia Faure"),
                            LibraryRow("p1", ContentKind.Playlist, "Le soir tard", null, trackCount = 38),
                            LibraryRow("r1", ContentKind.Artist, "Cléo Wend", null),
                        ),
                    likedCount = 1284,
                ),
            onFilterSelect = {},
            onRowClick = {},
        )
    }
}
