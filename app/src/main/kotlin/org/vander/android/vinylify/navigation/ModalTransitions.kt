package org.vander.android.vinylify.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

private const val MODAL_SLIDE_MS = 300

/** Une modale (Lecteur, File d'attente) monte depuis le bas de l'écran. */
fun modalEnter(): EnterTransition = slideInVertically(tween(MODAL_SLIDE_MS)) { it }

/** ... et redescend par le bas, que ce soit par `⌄`, le retour système ou un swipe vers le bas. */
fun modalExit(): ExitTransition = slideOutVertically(tween(MODAL_SLIDE_MS)) { it }
