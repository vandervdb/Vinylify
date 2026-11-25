package org.vander.android.vinylify.navigation

import org.vander.android.vinylify.feature.settings.settingsNavGraph
import org.vander.android.vinylify.feature.settings.SettingsRoute
import org.vander.android.vinylify.feature.playlist.playlistNavGraph
import org.vander.android.vinylify.feature.playlist.PlaylistRoute
import org.vander.android.vinylify.feature.ContentKind
import org.vander.android.vinylify.feature.search.searchNavGraph
import org.vander.android.vinylify.feature.artist.artistNavGraph
import org.vander.android.vinylify.feature.artist.ArtistRoute
import org.vander.android.vinylify.feature.album.albumNavGraph
import org.vander.android.vinylify.feature.album.AlbumRoute
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import org.vander.android.vinylify.feature.connection.ConnectionRoute
import org.vander.android.vinylify.feature.connection.SessionViewModel
import org.vander.android.vinylify.feature.connection.connectionNavGraph
import org.vander.android.vinylify.feature.home.HomeRoute
import org.vander.android.vinylify.feature.home.homeNavGraph
import org.vander.android.vinylify.feature.library.libraryNavGraph
import org.vander.android.vinylify.feature.lyrics.LyricsRoute
import org.vander.android.vinylify.feature.lyrics.LyricsViewModel
import org.vander.android.vinylify.feature.lyrics.lyricsNavGraph
import org.vander.android.vinylify.feature.player.playerNavGraph
import org.vander.android.vinylify.feature.profile.profileNavGraph
import org.vander.android.vinylify.feature.queue.QueueRoute
import org.vander.android.vinylify.feature.queue.queueNavGraph
import kotlin.reflect.KClass

/**
 * Un seul `NavHost`, donc un seul back stack. Deux registres s'y expriment par la structure
 * du graphe : le **push** (onglets et écrans empilés, dans [MainGraph], avec dock) et la
 * **modale** (Lecteur, à la racine, sans dock). Voir `ScreenChrome`.
 *
 * `PlayerViewModel` est lié à l'entrée de [MainGraph], qui survit aux changements d'onglet et
 * à l'ouverture de la modale : une instance par destination rouvrirait la liaison App Remote.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    session: SessionViewModel,
    contentPadding: PaddingValues,
    startDestination: KClass<*>,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.padding(contentPadding),
    ) {
        connectionNavGraph(
            session = session,
            // `inclusive = true` : retour depuis l'Accueil quitte l'app au lieu de ramener à
            // un écran de connexion que la session a déjà franchi.
            onSessionReady = {
                navController.navigate(MainGraph) {
                    popUpTo(ConnectionRoute) { inclusive = true }
                    launchSingleTop = true
                }
            },
        )

        navigation<MainGraph>(startDestination = HomeRoute::class) {
            // Un tap sur n'importe quel contenu ouvre son détail : le genre décide de la destination.
            val open = { kind: ContentKind, id: String ->
                navController.navigate(
                    when (kind) {
                        ContentKind.Album -> AlbumRoute(id)
                        ContentKind.Artist -> ArtistRoute(id)
                        ContentKind.Playlist -> PlaylistRoute(id)
                    },
                )
            }

            val openSettings = { navController.navigate(SettingsRoute) }

            homeNavGraph(
                onOpen = open,
                onOpenSettings = openSettings,
                // La loupe de l'Accueil mène à l'onglet Recherche, comme un tap sur la dock.
                onOpenSearch = { navController.navigateToTab(NavItem.Search) },
            )
            searchNavGraph(onOpen = open)
            libraryNavGraph(onOpen = open)
            profileNavGraph(onOpenSettings = openSettings)
            settingsNavGraph(onBack = { navController.popBackStack() })
            // Écrans empilés : dans MainGraph, donc avec dock et MiniPlayer (registre push).
            albumNavGraph(onBack = { navController.popBackStack() })
            artistNavGraph(onBack = { navController.popBackStack() }, onOpenAlbum = { id -> open(ContentKind.Album, id) })
            playlistNavGraph(onBack = { navController.popBackStack() })
        }

        // Lié à l'entrée de MainGraph, comme PlayerViewModel : le Lecteur (texte gravé sur le disque) et
        // l'écran Paroles partagent ainsi une seule recherche par titre.
        val lyricsOwner: @Composable (NavBackStackEntry) -> LyricsViewModel = { entry ->
            val parent = remember(entry) { navController.getBackStackEntry<MainGraph>() }
            hiltViewModel(parent)
        }

        playerNavGraph(
            playerOwner = { entry ->
                val parent = remember(entry) { navController.getBackStackEntry<MainGraph>() }
                hiltViewModel(parent)
            },
            lyricsOwner = lyricsOwner,
            onClose = { navController.popBackStack() },
            onOpenQueue = { navController.navigate(QueueRoute) },
            onOpenLyrics = { navController.navigate(LyricsRoute) },
        )

        queueNavGraph(onClose = { navController.popBackStack() })
        lyricsNavGraph(lyricsOwner = lyricsOwner, onClose = { navController.popBackStack() })
    }
}
