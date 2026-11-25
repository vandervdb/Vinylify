package org.vander.android.vinylify.component

import org.vander.android.vinylify.designsystem.ListTitleTextStyle
import org.vander.android.vinylify.designsystem.withNumerals
import org.vander.android.vinylify.designsystem.DurationTextStyle
import org.vander.android.vinylify.designsystem.component.MarqueeTextInfinite
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.vander.android.vinylify.designsystem.VinylTextEmphasis
import org.vander.android.vinylify.designsystem.VinylTextSecondary
import org.vander.android.vinylify.designsystem.VotDimens

/** Ligne « pochette + titre + sous-titre ». [round] pour un artiste, carré sinon. */
@Composable
fun ContentRow(
    title: String,
    subtitle: String,
    coverUrl: String?,
    round: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VotDimens.space14),
    ) {
        SpotifyTrackCover(
            modifier = Modifier.size(56.dp).clip(if (round) CircleShape else RoundedCornerShape(VotDimens.radiusSleeve)),
            model = coverUrl,
        )
        Column(Modifier.weight(1f)) {
            MarqueeTextInfinite(text = title, textStyle = ListTitleTextStyle, color = VinylTextEmphasis, fadeColor = MaterialTheme.colorScheme.background)
            Text(subtitle.withNumerals(), style = MaterialTheme.typography.bodySmall, color = VinylTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Ligne de titre d'un album ou d'un artiste : numéro, nom, artistes, durée. */
@Composable
fun TrackLine(
    number: Int?,
    title: String,
    subtitle: String?,
    durationMs: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VotDimens.space14),
    ) {
        if (number != null) {
            Text(number.toString().withNumerals(), style = MaterialTheme.typography.bodyMedium, color = VinylTextSecondary, modifier = Modifier.size(width = 24.dp, height = 24.dp))
        }
        Column(Modifier.weight(1f)) {
            MarqueeTextInfinite(text = title, textStyle = ListTitleTextStyle, color = VinylTextEmphasis, fadeColor = MaterialTheme.colorScheme.background)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle.withNumerals(), style = MaterialTheme.typography.bodySmall, color = VinylTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (durationMs > 0) {
            Text(formatDuration(durationMs), style = DurationTextStyle, color = VinylTextSecondary)
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
