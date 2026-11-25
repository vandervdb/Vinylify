package org.vander.android.vinylify.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import org.vander.android.vinylify.R

// Downloadable Fonts (Google Play Services) — no font files bundled in the app.
// Certificates in res/values/font_certs.xml are Google's own standard Fonts
// Provider certs (from the official Android vinylotech), not project-specific.
private val googleFontProvider =
    GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )

// Titres : Aldrich (années 70, façon cadrans de platine). Une seule graisse existe : on demande toujours
// Normal, sinon Compose fabriquerait un gras artificiel. Interlettrage neutre : l'interlettrage négatif
// des titres précédents ferait se toucher ses lettres.
// Corps de texte et labels : DM Sans.
private val Aldrich =
    FontFamily(
        Font(GoogleFont("Aldrich"), googleFontProvider, FontWeight.Normal),
    )

private val DMSans =
    FontFamily(
        Font(GoogleFont("DM Sans"), googleFontProvider, FontWeight.Normal),
        Font(GoogleFont("DM Sans"), googleFontProvider, FontWeight.Medium),
        Font(GoogleFont("DM Sans"), googleFontProvider, FontWeight.Bold),
    )

// Texte gravé sur les disques : Righteous (années 70), une seule graisse comme Aldrich.
val GrooveFontFamily =
    FontFamily(
        Font(GoogleFont("Righteous"), googleFontProvider, FontWeight.Normal),
    )

// Chiffres (durées, compteurs, années) : Aldrich, comme les titres.
val DurationTextStyle =
    TextStyle(
        fontFamily = Aldrich,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp,
    )

private val NumeralSpan = SpanStyle(fontFamily = Aldrich)
private val NumeralRun = Regex("""\d+(?::\d+)*""")

/** Le texte, avec chaque suite de chiffres (et « 4:30 ») composée en Aldrich ; le reste ne change pas. */
fun String.withNumerals(): AnnotatedString =
    buildAnnotatedString {
        append(this@withNumerals)
        NumeralRun.findAll(this@withNumerals).forEach { addStyle(NumeralSpan, it.range.first, it.range.last + 1) }
    }

// Labels (boutons, dock, filtres, surtitres) : Chakra Petch, d'allure « boutons de platine ».
private val ChakraPetch =
    FontFamily(
        Font(GoogleFont("Chakra Petch"), googleFontProvider, FontWeight.Medium),
        Font(GoogleFont("Chakra Petch"), googleFontProvider, FontWeight.SemiBold),
        Font(GoogleFont("Chakra Petch"), googleFontProvider, FontWeight.Bold),
    )

// Titres des listes (morceaux, albums, playlists) : Chakra Petch Medium, la police des libellés.
val ListTitleTextStyle =
    TextStyle(
        fontFamily = ChakraPetch,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp,
    )

// Set of Material typography styles to start with
val Typography =
    Typography(
        displayMedium =
            TextStyle(
                fontFamily = Aldrich,
                fontWeight = FontWeight.Normal,
                fontSize = 48.sp,
                lineHeight = 54.sp,
                letterSpacing = 0.sp,
            ),
        displaySmall =
            TextStyle(
                fontFamily = Aldrich,
                fontWeight = FontWeight.Normal,
                fontSize = 40.sp,
                lineHeight = 46.sp,
                letterSpacing = 0.sp,
            ),
        // Hero headline (e.g. "Ta discothèque, à ta façon.") — screen 01 "Connexion Spotify".
        headlineLarge =
            TextStyle(
                fontFamily = Aldrich,
                fontWeight = FontWeight.Normal,
                fontSize = 36.sp,
                lineHeight = 42.sp,
                letterSpacing = 0.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = Aldrich,
                fontWeight = FontWeight.Normal,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = Aldrich,
                fontWeight = FontWeight.Normal,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                letterSpacing = 0.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = Aldrich,
                fontWeight = FontWeight.Normal,
                fontSize = 17.sp,
                lineHeight = 23.sp,
                letterSpacing = 0.sp,
            ),
        // Snackbar title and action label (VinylSnackbar) — design kit: 15sp
        // Space Grotesk SemiBold. Material3's own default is Roboto, which would
        // break the brand typeface on every component reading this slot.
        titleSmall =
            TextStyle(
                fontFamily = Aldrich,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = DMSans,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = DMSans,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 23.sp,
                letterSpacing = 0.sp,
            ),
        // Disclaimer/caption text.
        bodySmall =
            TextStyle(
                fontFamily = DMSans,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.4.sp,
            ),
        // Button label text (Material3's Button() defaults its Text to labelLarge).
        labelLarge =
            TextStyle(
                fontFamily = ChakraPetch,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = ChakraPetch,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                letterSpacing = 0.sp,
            ),
        // Overline: never below 11sp, 0.18em tracking, uppercase.
        labelSmall =
            TextStyle(
                fontFamily = ChakraPetch,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 2.0.sp,
            ),
    )
