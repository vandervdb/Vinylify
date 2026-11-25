package org.vander.android.vinylify.feature

/** Ce qu'une vignette ou une ligne représente : elle décide de l'écran qu'un tap ouvre. */
enum class ContentKind { Album, Playlist, Artist }

/**
 * État d'un écran de détail (Album, Artiste) qui charge une ressource au démarrage.
 *
 * Trois états explicites plutôt qu'un `null` ambigu : « pas encore chargé » et « a échoué » ne
 * s'affichent pas pareil, et le second doit proposer de réessayer.
 */
sealed interface DetailState<out T> {
    data object Loading : DetailState<Nothing>

    data object Failed : DetailState<Nothing>

    data class Loaded<T>(
        val value: T,
    ) : DetailState<T>
}
