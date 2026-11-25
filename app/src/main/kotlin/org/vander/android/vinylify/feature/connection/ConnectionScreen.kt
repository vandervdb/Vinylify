package org.vander.android.vinylify.feature.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import org.vander.android.vinylify.R
import org.vander.android.vinylify.designsystem.AndroidAppTheme
import org.vander.android.vinylify.designsystem.VinylPurple
import org.vander.android.vinylify.designsystem.VinylTextEmphasis
import org.vander.android.vinylify.designsystem.VinylTextMuted
import org.vander.android.vinylify.designsystem.VinylTextTertiary
import org.vander.android.vinylify.designsystem.VotDimens
import org.vander.android.vinylify.designsystem.component.VINYL_LOADER_CYCLE_MS
import org.vander.android.vinylify.designsystem.component.VinylLoader
import org.vander.android.vinylify.designsystem.loginHalo
import org.vander.android.vinylify.designsystem.modifier.drawGrainOverlay
import kotlinx.coroutines.delay
import org.vander.spotify.SpotifyError
import org.vander.spotify.auth.SessionState

/**
 * Screen 01 "Connexion Spotify" from the Vinyl O'Tech design — the app's start
 * destination ([org.vander.android.vinylify.feature.connection.ConnectionRoute]).
 *
 * Takes an intent lambda, not the SessionManagerB: the manager needs the
 * activity result launcher and the Activity, both of which live in AppRoot. A
 * screen holding it could not be previewed either.
 */
@Composable
fun ConnectionScreen(
    state: SessionState,
    onContinueWithSpotify: () -> Unit,
    spinning: Boolean,
    onConnected: () -> Unit = {},
) {
    val anchor = LocalLaunchAnchor.current
    val connected = state is SessionState.Authorized

    // Retour de Spotify : un cycle complet du loader avant de quitter l'écran, le texte passe à
    // « CONNECTÉ » à mi-cycle. Naviguer dès l'autorisation couperait l'animation net.
    var showConnected by remember { mutableStateOf(false) }
    LaunchedEffect(connected) {
        if (!connected) return@LaunchedEffect
        delay(VINYL_LOADER_CYCLE_MS / 2L)
        showConnected = true
        delay(VINYL_LOADER_CYCLE_MS / 2L)
        onConnected()
    }

    val grooveText =
        when {
            connected && showConnected -> stringResource(R.string.loader_connected)
            spinning || connected -> stringResource(R.string.loader_connecting)
            else -> stringResource(R.string.loader_brand)
        }

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .drawGrainOverlay(alpha = 0.82f),
    ) {
        val glowSize = maxWidth * (480f / 428f)
        val glowOffsetX = maxWidth * (-110f / 428f)
        val glowOffsetY = maxHeight * (-160f / 908f)

        val haloRadiusPx = with(LocalDensity.current) { glowSize.toPx() / 2f }
        Box(
            modifier =
                Modifier
                    .size(glowSize)
                    .offset(x = glowOffsetX, y = glowOffsetY)
                    .background(brush = loginHalo(haloRadiusPx)),
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = VotDimens.screenPadding,
                        end = VotDimens.screenPadding,
                        bottom = VotDimens.screenBottom,
                    ),
        ) {
            BoxWithConstraints(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                // key : au passage à « connecté » le cycle repart de son début, il dure donc un cycle entier.
                key(connected) {
                    VinylLoader(
                        modifier =
                            Modifier
                                // Avant le size() interne du loader : mesure donc sa taille finale.
                                .onGloballyPositioned { anchor?.loaderBounds = it.boundsInRoot() }
                                // Invisible pendant le splash, dont la platine vient se poser ici.
                                .graphicsLayer { alpha = if (anchor?.active == true) 0f else 1f },
                        size = min(maxWidth, maxHeight),
                        animated = spinning || connected,
                        grooveText = grooveText,
                    )
                }
            }

            Text(
                text = stringResource(R.string.home_headline),
                style = MaterialTheme.typography.headlineLarge,
                color = VinylTextEmphasis,
            )

            Text(
                text = stringResource(R.string.home_subtitle),
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        letterSpacing = 0.sp,
                    ),
                color = VinylTextMuted,
                modifier =
                    Modifier.padding(
                        top = VotDimens.space14,
                        bottom = VotDimens.space32,
                    ),
            )

            Button(
                enabled = !spinning && !connected,
                onClick = onContinueWithSpotify,
                shape = RoundedCornerShape(percent = 50),
                contentPadding = PaddingValues(vertical = VotDimens.buttonPadding),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = VotDimens.buttonElevation,
                            shape = RoundedCornerShape(percent = 50),
                            ambientColor = VinylPurple,
                            spotColor = VinylPurple,
                        ),
            ) {
                Text(stringResource(R.string.home_continue_button))
            }

            Text(
                text = stringResource(R.string.home_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = VinylTextTertiary,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = VotDimens.space20),
            )
        }
    }
}

private class SessionStates : PreviewParameterProvider<SessionState> {
    override val values =
        sequenceOf(
            SessionState.SignedOut,
            SessionState.Authorizing,
            SessionState.Authorized,
            SessionState.Failed(SpotifyError.Network(IllegalStateException("Hors ligne"))),
        )
}

@Preview
@Composable
private fun PreviewConnection(
    @PreviewParameter(SessionStates::class) state: SessionState,
) {
    AndroidAppTheme { ConnectionScreen(state, onContinueWithSpotify = {}, spinning = state is SessionState.Authorizing) }
}
