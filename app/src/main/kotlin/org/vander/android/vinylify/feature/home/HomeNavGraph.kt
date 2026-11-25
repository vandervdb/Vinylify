package org.vander.android.vinylify.feature.home

import org.vander.android.vinylify.feature.ContentKind
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

fun NavGraphBuilder.homeNavGraph(
    onOpen: (ContentKind, String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    composable<HomeRoute> { HomeScreen(onOpen = onOpen, onOpenSettings = onOpenSettings, onOpenSearch = onOpenSearch) }
}

@Composable
fun HomeScreen(
    onOpen: (ContentKind, String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        modifier = Modifier.fillMaxSize(),
        onFilterSelect = viewModel::onFilterSelect,
        onSettingsClick = onOpenSettings,
        onSearchClick = onOpenSearch,
        onResumeListen = viewModel::onResumeListen,
        onRetry = viewModel::onRetry,
        onTileClick = { tile -> onOpen(tile.kind, tile.id) },
    )
}
