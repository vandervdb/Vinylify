package org.vander.android.vinylify.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.vander.android.vinylify.designsystem.AndroidAppTheme
import org.vander.android.vinylify.designsystem.VinylNavIconInactive
import org.vander.android.vinylify.designsystem.VinylOutline
import org.vander.android.vinylify.designsystem.VinylPurple
import org.vander.android.vinylify.designsystem.VinylPurpleSoft
import org.vander.android.vinylify.designsystem.VinylSurfaceHigh
import org.vander.android.vinylify.designsystem.VotDimens

/** `border-radius: 999px` in the design — a full pill, whatever the height. */
private val DockShape = RoundedCornerShape(percent = 50)

/**
 * The floating dock of screens 02 · 03 · 04 · 10, not a Material navigation bar.
 *
 * It holds no state: the caller derives [selected] from the back stack and navigates in
 * [onSelect], so the dock can only show where the app is.
 *
 * Not a restyled `ShortNavigationBar`: the design overrides its surface, its shape, its
 * height and its item layout, so nothing of Material's would remain except the name.
 * Labelling only the selected item is the part Material cannot express at all.
 *
 * Values read from the design file, dock of screen 02 Accueil:
 * container `#16121E`, `1px solid rgba(255,255,255,0.07)`, `border-radius: 999px`,
 * `padding: 12px 8px`, `box-shadow: 0 12px 40px rgba(0,0,0,0.5)`, `space-around`;
 * selected item `rgba(124,92,255,0.18)`, `border-radius: 999px`, `padding: 6px 14px`,
 * label 12px/600 `#C7B4FF` (sans icône, choix postérieur à la maquette); unselected icons `#7A7387`.
 */
@Composable
fun BottomBar(
    selected: NavItem?,
    onSelect: (NavItem) -> Unit,
    modifier: Modifier = Modifier,
    navItems: List<NavItem> = NavItem.all,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(
                    elevation = VotDimens.dockElevation,
                    shape = DockShape,
                    ambientColor = Color.Black,
                    spotColor = Color.Black,
                ).background(VinylSurfaceHigh, DockShape)
                .border(1.dp, VinylOutline, DockShape)
                .padding(horizontal = VotDimens.space8, vertical = VotDimens.space12),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navItems.forEach { item ->
            DockItem(
                item = item,
                selected = item == selected,
                onClick = { onSelect(item) },
            )
        }
    }
}

@Composable
private fun DockItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(item.labelRes)

    Row(
        modifier =
            Modifier
                .clip(DockShape)
                .background(if (selected) VinylPurple.copy(alpha = 0.18f) else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(
                    horizontal = if (selected) VotDimens.space14 else VotDimens.space12,
                    vertical = VotDimens.space6,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Sélectionné : le libellé seul, sans icône. Les autres restent des icônes seules.
        if (selected) {
            Text(
                text = label,
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = VinylPurpleSoft,
            )
        } else {
            Icon(
                imageVector = item.icon,
                contentDescription = label,
                tint = VinylNavIconInactive,
                modifier = Modifier.size(VotDimens.dockIcon),
            )
        }
    }
}

@Preview
@Composable
private fun BottomBarPreview() {
    AndroidAppTheme {
        BottomBar(selected = NavItem.Home, onSelect = {})
    }
}
