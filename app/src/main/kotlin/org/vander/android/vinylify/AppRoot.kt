package org.vander.android.vinylify

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.vander.android.vinylify.designsystem.VinylInk
import org.vander.android.vinylify.designsystem.VotDimens
import org.vander.android.vinylify.designsystem.component.VinylSnackTone
import org.vander.android.vinylify.designsystem.component.VinylSnackbarData
import org.vander.android.vinylify.designsystem.component.VinylSnackbarHost
import org.vander.android.vinylify.designsystem.component.VinylSnackbarHostState
import org.vander.android.vinylify.designsystem.component.rememberVinylSnackbarHostState
import org.vander.android.vinylify.designsystem.modifier.drawGrainOverlay
import org.vander.android.vinylify.feature.connection.ConnectionRoute
import org.vander.android.vinylify.feature.connection.SessionViewModel
import org.vander.android.vinylify.feature.player.MiniPlayer
import org.vander.android.vinylify.feature.player.PlayerRoute
import org.vander.android.vinylify.feature.player.PlayerViewModel
import org.vander.android.vinylify.navigation.AppNavHost
import org.vander.android.vinylify.navigation.BottomBar
import org.vander.android.vinylify.navigation.MainGraph
import org.vander.android.vinylify.navigation.NavItem
import org.vander.android.vinylify.navigation.ScreenChrome
import org.vander.android.vinylify.navigation.navigateToTab
import org.vander.android.vinylify.navigation.screenChrome
import org.vander.spotify.auth.SessionState

/**
 * Racine de composition : possède le `NavController` et le snackbar, et passe à [MainShell]
 * ce qu'il faut pour la mise en page.
 *
 * Appelée une fois la session restaurée (le splash tient jusque-là, voir `MainActivity`) : la
 * destination de départ dépend de sa réponse et ne doit pas changer ensuite.
 */
@Composable
fun AppRoot(session: SessionViewModel) {
    val navController = rememberNavController()
    val backEntry by navController.currentBackStackEntryAsState()
    val chrome = backEntry?.destination.screenChrome()

    // Le padding de la bande du bas s'applique au NavHost entier : dès que la navigation démarre, il
    // s'appliquerait aussi à l'écran de connexion encore visible pendant son fondu, qui remonterait
    // d'un coup. On le retient jusqu'à ce que cet écran ait quitté la composition ; l'Accueil, ancré
    // en haut, ne bouge pas quand son viewport rétrécit ensuite.
    val visibleEntries by navController.visibleEntries.collectAsStateWithLifecycle()
    val connectionStillVisible = visibleEntries.any { it.destination.hasRoute(ConnectionRoute::class) }

    val sessionState by session.session.collectAsStateWithLifecycle()
    val startDestination =
        remember { if (session.session.value is SessionState.Authorized) MainGraph::class else ConnectionRoute::class }

    // Déconnexion depuis le Profil : la session repasse à SignedOut alors qu'on est dans MainGraph.
    // Hors de MainGraph (démarrage sur la connexion) ce n'est pas une déconnexion, donc rien à faire.
    LaunchedEffect(sessionState) {
        val inMainGraph = backEntry?.destination?.hierarchy?.any { it.hasRoute(MainGraph::class) } == true
        if (sessionState is SessionState.SignedOut && inMainGraph) {
            navController.navigate(ConnectionRoute) {
                popUpTo(MainGraph) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val snack = rememberVinylSnackbarHostState()
    AuthFailureSnackbarEffect(sessionState, snack, onRetry = session::signIn)

    // Hors du slot MiniPlayer : la reconnexion doit aussi jouer sur les écrans qui ne l'affichent pas.
    if (backEntry?.destination?.hierarchy?.any { it.hasRoute(MainGraph::class) } == true) {
        val parent = remember(backEntry) { navController.getBackStackEntry<MainGraph>() }
        ReconnectPlayerEffect(hiltViewModel<PlayerViewModel>(parent))
    }

    MainShell(
        chrome = chrome,
        holdBottomPadding = connectionStillVisible,
        snack = snack,
        selectedTab = NavItem.all.firstOrNull { it.isSelectedIn(backEntry?.destination) },
        onTabSelected = navController::navigateToTab,
        // Un slot et non un ViewModel en paramètre : le corps ne s'exécute que lorsque le shell
        // dessine le MiniPlayer, donc dans `MainGraph`. `getBackStackEntry` lève hors du graphe.
        miniPlayer = {
            val parent = remember(backEntry) { navController.getBackStackEntry<MainGraph>() }
            // Lié à l'entrée du graphe, pas à la destination : sinon un changement d'onglet
            // le viderait et rouvrirait la liaison App Remote.
            val player = hiltViewModel<PlayerViewModel>(parent)
            val nowPlaying by player.nowPlaying.collectAsStateWithLifecycle()
            val connection by player.connection.collectAsStateWithLifecycle()
            val isSaved by player.isSaved.collectAsStateWithLifecycle()
            MiniPlayer(
                nowPlaying = nowPlaying,
                connection = connection,
                isSaved = isSaved,
                onOpen = { navController.navigate(PlayerRoute) },
                onTogglePlayPause = player::togglePlayPause,
                onToggleSave = player::toggleSave,
                onSeek = player::seekTo,
                onSkipNext = player::skipNext,
                onPreviousTrack = player::previousTrack,
                onRetryConnect = player::retryConnect,
            )
        },
    ) { padding ->
        AppNavHost(
            navController = navController,
            session = session,
            contentPadding = padding,
            startDestination = startDestination,
        )
    }
}

/** Une tentative de reconnexion à chaque passage au premier plan. */
@Composable
private fun ReconnectPlayerEffect(player: PlayerViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(player, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { player.reconnectWhenLost() }
    }
}

/**
 * L'encadrement autour du contenu : grain, bande du bas, snackbar.
 *
 * La bande est une **sœur** du contenu, pas un parent : c'est ce qui laisse le MiniPlayer
 * survivre à un changement de destination. D'où la hauteur mesurée à la main et renvoyée en
 * [PaddingValues] — aucune relation de layout ne le fait ici, contrairement à `Scaffold`.
 */
@Composable
private fun MainShell(
    chrome: ScreenChrome,
    holdBottomPadding: Boolean,
    snack: VinylSnackbarHostState,
    selectedTab: NavItem?,
    onTabSelected: (NavItem) -> Unit,
    miniPlayer: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    var bandHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .drawGrainOverlay(alpha = 0.82f),
        ) {
            content(PaddingValues(bottom = if (chrome.bottomBar && !holdBottomPadding) bandHeight else 0.dp))

            if (chrome.bottomBar) {
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { bandHeight = with(density) { it.height.toDp() } }
                        // Fond AVANT les insets : il doit couvrir la zone de navigation système.
                        .background(VinylInk)
                        .navigationBarsPadding()
                        .padding(
                            start = VotDimens.dockMargin,
                            end = VotDimens.dockMargin,
                            top = VotDimens.space10,
                            bottom = VotDimens.dockMargin,
                        ),
                    verticalArrangement = Arrangement.spacedBy(VotDimens.space10),
                ) {
                    if (chrome.miniPlayer) miniPlayer()
                    BottomBar(selected = selectedTab, onSelect = onTabSelected)
                }
            }
        }

        VinylSnackbarHost(snack, Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun AuthFailureSnackbarEffect(
    sessionState: SessionState,
    snack: VinylSnackbarHostState,
    onRetry: () -> Unit,
) {
    val title = stringResource(R.string.snack_auth_failed_title)
    val message = stringResource(R.string.snack_auth_failed_message)
    val retryLabel = stringResource(R.string.snack_retry)

    LaunchedEffect(sessionState) {
        if (sessionState !is SessionState.Failed) return@LaunchedEffect
        snack.show(
            VinylSnackbarData(
                message = message,
                title = title,
                tone = VinylSnackTone.Error,
                actionLabel = retryLabel,
                onAction = onRetry,
            ),
        )
    }
}
