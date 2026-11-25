package org.vander.android.vinylify.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Dit aux [MarqueeTextInfinite] descendants s'ils ont le droit de défiler.
 *
 * Dynamique (`compositionLocalOf`) et non statique : au début et à la fin d'un défilement seuls
 * les marquees qui lisent cette valeur se recomposent, pas toute la liste.
 */
val LocalMarqueeEnabled = compositionLocalOf { true }

/**
 * `LazyColumn` dont les textes en marquee sont figés tant qu'elle défile.
 *
 * C'est le point d'entrée à utiliser pour toute liste de lignes à titre sur une seule ligne :
 * le marquee y est actif au repos et suspendu pendant le défilement, sans que chaque ligne ait à
 * connaître l'état de la liste.
 */
@Composable
fun MarqueeLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit,
) {
    CompositionLocalProvider(LocalMarqueeEnabled provides !state.isScrollInProgress) {
        LazyColumn(
            modifier = modifier,
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}
