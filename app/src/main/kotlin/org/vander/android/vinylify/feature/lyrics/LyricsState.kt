package org.vander.android.vinylify.feature.lyrics

import org.vander.spotify.model.LyricLine
import org.vander.spotify.model.Lyrics

sealed interface LyricsState {
    data object Loading : LyricsState

    /** La source n'a rien pour ce titre (ou aucun titre n'est en lecture). */
    data object NotFound : LyricsState

    /** L'appel a échoué (réseau, service) : on ne sait pas s'il existe des paroles. */
    data object Failed : LyricsState

    data object Instrumental : LyricsState

    /** Texte sans horodatage : pas de ligne courante à suivre. */
    data class Plain(
        val text: String,
    ) : LyricsState

    data class Synced(
        val lines: List<LyricLine>,
    ) : LyricsState
}

/** Du résultat du SDK à l'état d'écran : les paroles synchronisées priment sur le texte brut. */
fun Lyrics?.toState(): LyricsState =
    when {
        this == null -> LyricsState.NotFound
        instrumental -> LyricsState.Instrumental
        isSynced -> LyricsState.Synced(lines)
        plain != null -> LyricsState.Plain(plain!!)
        else -> LyricsState.NotFound
    }

/**
 * Indice de la ligne en cours à [positionMs] : la dernière dont l'horodatage est atteint, ou
 * `-1` avant la première. [lines] est trié par [LyricLine.startMs] (le SDK le garantit).
 */
fun currentLineIndex(
    lines: List<LyricLine>,
    positionMs: Long,
): Int {
    var low = 0
    var high = lines.lastIndex
    var found = -1
    while (low <= high) {
        val mid = (low + high) ushr 1
        if (lines[mid].startMs <= positionMs) {
            found = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return found
}

/**
 * Texte de la ligne en cours à [positionMs], pour des paroles synchronisées ; `null` sinon, et aussi
 * avant la première ligne ou sur une ligne vide (pause instrumentale) : l'appelant garde alors son
 * texte par défaut.
 */
fun LyricsState.lineAt(positionMs: Long): String? {
    if (this !is LyricsState.Synced) return null
    return lines.getOrNull(currentLineIndex(lines, positionMs))?.text?.takeIf { it.isNotBlank() }
}
