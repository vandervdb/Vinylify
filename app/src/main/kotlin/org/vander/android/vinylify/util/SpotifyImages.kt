package org.vander.android.vinylify.util

private const val SPOTIFY_IMAGE_CDN = "https://i.scdn.co/image/"

/**
 * L'App Remote pousse une URI `spotify:image:<hash>` que Coil ne sait pas charger ; le CDN
 * attend le seul hash. Une URL déjà `http(s)` (celle de la Web API) passe telle quelle.
 */
fun spotifyImageUrl(raw: String?): String? =
    when {
        raw.isNullOrBlank() -> null
        raw.startsWith("http") -> raw
        else -> SPOTIFY_IMAGE_CDN + raw.substringAfterLast(':')
    }
