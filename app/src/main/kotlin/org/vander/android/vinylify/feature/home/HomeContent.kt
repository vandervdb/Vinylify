package org.vander.android.vinylify.feature.home

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import org.vander.android.vinylify.feature.ContentKind
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import org.vander.android.vinylify.designsystem.VinylTextSecondary
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.vander.android.vinylify.R
import org.vander.android.vinylify.designsystem.AndroidAppTheme
import org.vander.android.vinylify.designsystem.VotDimens
import org.vander.android.vinylify.designsystem.component.VinylBrandTopBar
import org.vander.android.vinylify.designsystem.component.VinylFilterChipRow
import org.vander.android.vinylify.designsystem.component.VinylHeroBackdrop
import org.vander.android.vinylify.designsystem.component.VinylHeroFeature
import org.vander.android.vinylify.designsystem.component.VinylSectionHeader
import org.vander.android.vinylify.designsystem.modifier.fullBleed

/** Air between the fixed chip row and the resume block, which now scrolls under it. */
private val RESUME_TOP = VotDimens.space20

/** Titre de section : `padding: 20px 20px 12px`, moins l'écart vertical que la grille ajoute déjà. */
private val SECTION_TOP = VotDimens.space20 - VotDimens.gridGutterV

private const val GRID_COLUMNS = 3

private const val KEY_RESUME = "home:resume"

private const val KEY_SECTION = "home:section"
private const val KEY_ERROR = "home:error"

/**
 * The whole Accueil screen (maquette écran 02), without a ViewModel: it draws [state] and
 * reports what the user does.
 *
 * Stateless on purpose. It is previewable in every state, and `HomeScreen` only has to collect
 * the ViewModel and pass values and lambdas through. Every callback defaults to doing nothing,
 * so they can be wired one at a time.
 *
 * One `LazyVerticalGrid` holds everything — hero, section header, tiles — instead of a column
 * wrapping a grid, which would nest two vertical scrolls. The hero and the header span the three
 * columns and escape the grid's side padding through [fullBleed]; the tiles keep it.
 */
@Composable
fun HomeContent(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onFilterSelect: (HomeFilter) -> Unit = {},
    onResumeListen: () -> Unit = {},
    onResumeToggleSave: () -> Unit = {},
    onTileClick: (CollectionTile) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    Box(modifier) {
        // Fixed layer: the gradient never scrolls. The empty content makes it measure exactly
        // status bar + heroHeight.
        VinylHeroBackdrop(Modifier.align(Alignment.TopStart)) {}

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            VinylBrandTopBar(onSearchClick = onSearchClick, onSettingsClick = onSettingsClick)
            VinylFilterChipRow(
                options = HomeFilter.entries,
                selected = state.selectedFilter,
                label = { stringResource(it.labelRes) },
                onSelect = onFilterSelect,
            )

            // Le héros s'insère au-dessus quand les albums arrivent (ou au changement de filtre) :
            // la grille garde sa première ligne visible ancrée par clé et le laisserait hors écran.
            val gridState = rememberLazyGridState()
            LaunchedEffect(state.resume != null) { gridState.scrollToItem(0) }

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(GRID_COLUMNS),
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = VotDimens.listPaddingStart,
                        end = VotDimens.listPaddingEnd,
                        top = RESUME_TOP,
                    ),
                verticalArrangement = Arrangement.spacedBy(VotDimens.gridGutterV),
                horizontalArrangement = Arrangement.spacedBy(VotDimens.gridGutterH),
            ) {
                state.resume?.let { resume ->
                    item(key = KEY_RESUME, span = { GridItemSpan(maxLineSpan) }, contentType = KEY_RESUME) {
                        VinylHeroFeature(
                            kicker = stringResource(R.string.home_resume_kicker),
                            title = resume.title,
                            subtitle = resume.subtitle,
                            isSaved = resume.isSaved,
                            onListen = onResumeListen,
                            onToggleSave = onResumeToggleSave,
                            modifier = Modifier.fullBleed(VotDimens.listPaddingStart, VotDimens.listPaddingEnd),
                        )
                    }
                }

                item(key = KEY_SECTION, span = { GridItemSpan(maxLineSpan) }, contentType = KEY_SECTION) {
                    val count = state.libraryCount
                    VinylSectionHeader(
                        title = stringResource(R.string.home_library_title),
                        trailing = pluralStringResource(state.selectedFilter.countRes, count, count),
                        modifier =
                            Modifier
                                .fullBleed(VotDimens.listPaddingStart, VotDimens.listPaddingEnd)
                                .padding(start = VotDimens.space20, end = VotDimens.space20, top = SECTION_TOP),
                    )
                }

                if (state.loadFailed && state.tiles.isEmpty()) {
                    item(key = KEY_ERROR, span = { GridItemSpan(maxLineSpan) }, contentType = KEY_ERROR) {
                        LoadFailed(onRetry)
                    }
                }

                collectionGridItems(
                    tiles = state.tiles,
                    playingTileId = state.playingTileId,
                    onTileClick = onTileClick,
                )
            }
        }
    }
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

private val previewState =
    HomeUiState(
        tiles =
            listOf("Sillons", "Marée haute", "Braise", "Gris perle", "Or mat", "Onde longue", "Souffle")
                .mapIndexed { index, name -> CollectionTile(id = "p$index", name = name, coverUrl = null, kind = ContentKind.Album) },
        libraryCount = 243,
        playingTileId = "p2",
        resume = ResumeListening(title = "Nuits blanches", subtitle = "Elia Faure · 2023 · 11 titres", isSaved = false),
    )

@Preview(showBackground = true, backgroundColor = 0xFF0C0A10, heightDp = 900)
@Composable
private fun HomeContentPreview() {
    AndroidAppTheme { HomeContent(state = previewState, modifier = Modifier.fillMaxSize()) }
}

@Preview(showBackground = true, backgroundColor = 0xFF0C0A10, heightDp = 900, name = "Nothing to resume")
@Composable
private fun HomeContentNoResumePreview() {
    AndroidAppTheme { HomeContent(state = previewState.copy(resume = null), modifier = Modifier.fillMaxSize()) }
}

@Preview(showBackground = true, backgroundColor = 0xFF0C0A10, heightDp = 900, name = "Empty library")
@Composable
private fun HomeContentEmptyPreview() {
    AndroidAppTheme { HomeContent(state = HomeUiState(), modifier = Modifier.fillMaxSize()) }
}
