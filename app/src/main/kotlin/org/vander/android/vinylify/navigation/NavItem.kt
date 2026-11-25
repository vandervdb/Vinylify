package org.vander.android.vinylify.navigation

import org.vander.android.vinylify.feature.search.SearchRoute
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph
import org.vander.android.vinylify.R
import org.vander.android.vinylify.feature.home.HomeRoute
import org.vander.android.vinylify.feature.library.LibraryRoute
import org.vander.android.vinylify.feature.profile.ProfileRoute

/**
 * Un onglet de la dock.
 *
 * Dans l'ordre de la maquette : Accueil, Recherche, Bibliothèque, Profil.
 *
 * @property route l'`object` `@Serializable` lui-même : `navigate` prend l'instance, `hasRoute`
 *   compare sa classe.
 */
sealed class NavItem(
    val route: Any,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    /** Parcourt la hiérarchie : un écran empilé depuis l'onglet l'allume encore. */
    fun isSelectedIn(destination: NavDestination?): Boolean =
        destination?.hierarchy?.any { it.hasRoute(route::class) } == true

    companion object {
        val all: List<NavItem> = listOf(Home, Search, Library, Profile)
    }

    object Home : NavItem(HomeRoute, R.string.nav_label_home, NavIcons.Turntable)

    object Search : NavItem(SearchRoute, R.string.nav_label_search, NavIcons.RecordSearch)

    object Library :
        NavItem(LibraryRoute, R.string.nav_label_library, NavIcons.RecordSleeves)

    object Profile : NavItem(ProfileRoute, R.string.nav_label_profile, NavIcons.Headphones)
}

/**
 * Bascule vers l'onglet [item] comme une barre du bas doit le faire : `popUpTo` le premier
 * onglet avec `saveState`, `launchSingleTop` pour ne pas empiler l'onglet courant, et
 * `restoreState` pour retrouver l'onglet là où on l'a laissé. À n'appeler que lorsque
 * [MainGraph] est sur le back stack, ce qui est toujours vrai quand la dock est visible.
 */
fun NavController.navigateToTab(item: NavItem) {
    val tabs = getBackStackEntry<MainGraph>().destination as NavGraph
    navigate(item.route) {
        popUpTo(tabs.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
