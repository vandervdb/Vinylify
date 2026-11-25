package org.vander.android.vinylify.feature.home

import org.vander.android.vinylify.designsystem.ListTitleTextStyle
import org.vander.android.vinylify.feature.ContentKind
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import org.vander.android.vinylify.R
import org.vander.android.vinylify.component.SpotifyTrackCover
import org.vander.android.vinylify.designsystem.AndroidAppTheme
import org.vander.android.vinylify.designsystem.VinylTileLabel
import org.vander.android.vinylify.designsystem.VotDimens
import org.vander.android.vinylify.designsystem.component.SpinningDisc

/** Maquette écran 02 : `grid-template-columns: repeat(3, 1fr)`. */
private const val GRID_COLUMNS = 3

/** Maquette : titre de vignette en 12px / 500, interligne 1,2 — soit 14,4sp. */
private val TILE_TITLE_SIZE = 12.sp

private val TILE_TITLE_LINE_HEIGHT = 14.4.sp

/**
 * Les vignettes de « Ta bibliothèque », écran 02.
 *
 * À injecter dans le `LazyVerticalGrid` de l'écran, qui portera aussi le héros et l'en-tête
 * de section : un second grid imbriqué mettrait un défilement vertical dans un autre.
 * [CollectionGrid] est le tandem autonome, pour les previews et l'usage isolé.
 *
 * **Écart assumé avec la maquette** : elle fait tourner le disque au survol
 * (`--vt-spin: running` sur `:hover`), ce qui n'existe pas au doigt. Ici il tourne quand la
 * tile est celle en cours de lecture — l'animation devient un indicateur d'état plutôt
 * qu'un effet de survol.
 *
 * @param playingTileId identifiant de la tile en cours ; `null` quand rien ne joue,
 *   ou quand la source ne sait pas encore le dire.
 */
fun LazyGridScope.collectionGridItems(
    tiles: List<CollectionTile>,
    playingTileId: String?,
    onTileClick: (CollectionTile) -> Unit,
) {
    items(
        items = tiles,
        key = { it.id },
    ) { tile ->
        CollectionGridItem(
            tile = tile,
            isPlaying = tile.id == playingTileId,
            onClick = { onTileClick(tile) },
        )
    }
}

/**
 * [collectionGridItems] dans son propre `LazyVerticalGrid`.
 *
 * Le `contentPadding` est dissymétrique — 20 dp à gauche, 30 dp à droite — parce que le
 * disque de la dernière colonne déborde de sa pochette ; c'est ce que documente déjà
 * [VotDimens.listPaddingEnd].
 */
@Composable
fun CollectionGrid(
    tiles: List<CollectionTile>,
    playingTileId: String?,
    onTileClick: (CollectionTile) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = modifier,
        contentPadding =
            PaddingValues(
                start = VotDimens.listPaddingStart,
                end = VotDimens.listPaddingEnd,
            ),
        verticalArrangement = Arrangement.spacedBy(VotDimens.gridGutterV),
        horizontalArrangement = Arrangement.spacedBy(VotDimens.gridGutterH),
    ) {
        collectionGridItems(tiles, playingTileId, onTileClick)
    }
}

/**
 * Une vignette : le disque qui dépasse derrière, la pochette par-dessus, le titre dessous.
 *
 * La maquette pose une seconde ligne sous le titre (l'artiste, 11px `#857E92`) qu'on
 * n'affiche pas : [CollectionTile] ne porte que `id`, `name` et `coverUrl`, et une tile n'a
 * de toute façon pas d'artiste unique.
 */
@Composable
private fun CollectionGridItem(
    tile: CollectionTile,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val playingLabel = stringResource(R.string.content_desc_playlist_playing, tile.name)

    Column(
        modifier =
            Modifier
                .clickable(onClick = onClick)
                // Le titre est déjà lu par le lecteur d'écran sous la vignette ; ce qu'il
                // rate, c'est le disque qui tourne. La sémantique de la tuile porte donc
                // l'état de lecture, et le nom seul sinon.
                .clearAndSetSemantics {
                    contentDescription = if (isPlaying) playingLabel else tile.name
                },
        verticalArrangement = Arrangement.spacedBy(VotDimens.space7),
    ) {
        // BoxWithConstraints et non un offset fixe : le débord du disque est une fraction
        // de la largeur de la vignette, qui dépend elle-même de celle de l'écran.
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            val overhang = maxWidth * VotDimens.DISC_OVERHANG_RATIO

            SpinningDisc(
                spinning = isPlaying,
                modifier =
                    Modifier
                        .matchParentSize()
                        .offset(x = overhang),
            )

            SpotifyTrackCover(
                modifier = Modifier.matchParentSize(),
                model = tile.coverUrl,
            )
        }

        Text(
            text = tile.name,
            style =
                ListTitleTextStyle.copy(
                    fontSize = TILE_TITLE_SIZE,
                    lineHeight = TILE_TITLE_LINE_HEIGHT,
                ),
            color = VinylTileLabel,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val previewCollectionTiles =
    listOf(
        CollectionTile(id = "1", name = "Sillons", coverUrl = "", kind = ContentKind.Album),
        CollectionTile(id = "2", name = "Marée haute", coverUrl = "", kind = ContentKind.Album),
        CollectionTile(id = "3", name = "Braise", coverUrl = "", kind = ContentKind.Album),
        CollectionTile(id = "4", name = "Gris perle", coverUrl = "", kind = ContentKind.Album),
        CollectionTile(id = "5", name = "Or mat", coverUrl = "", kind = ContentKind.Album),
        CollectionTile(id = "6", name = "Onde longue", coverUrl = "", kind = ContentKind.Album),
    )

@Preview(showBackground = true, backgroundColor = 0xFF0C0A10)
@Composable
private fun CollectionGridPreview() {
    AndroidAppTheme {
        CollectionGrid(
            tiles = previewCollectionTiles,
            playingTileId = null,
            onTileClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0C0A10, name = "Une tile en lecture")
@Composable
private fun CollectionGridPlayingPreview() {
    AndroidAppTheme {
        CollectionGrid(
            tiles = previewCollectionTiles,
            playingTileId = "3",
            onTileClick = {},
        )
    }
}
