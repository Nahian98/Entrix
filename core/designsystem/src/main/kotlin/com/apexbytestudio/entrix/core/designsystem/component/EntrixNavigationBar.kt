package com.apexbytestudio.entrix.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apexbytestudio.entrix.core.designsystem.icon.EntrixIcons
import com.apexbytestudio.entrix.core.designsystem.theme.EntrixSpacing
import com.apexbytestudio.entrix.core.designsystem.theme.EntrixTheme

/**
 * Colours for the floating navigation bar. Monochrome by design (white content on a dark pill).
 * Light theme uses the reference near-black. Dark theme lifts the pill to a charcoal *lighter* than
 * the page, because a black pill on a near-black page reads as a hole rather than a floating bar.
 * Resolved from the active colour scheme so a forced/in-app theme is respected too.
 */
@Immutable
data class EntrixNavigationBarColors(
    val container: Color,
    val indicator: Color,
    val content: Color,
    val border: Color,
)

object EntrixNavigationBarDefaults {
    private val Light = EntrixNavigationBarColors(
        container = Color(0xFF0B0B0C),
        indicator = Color(0xFF2A2A2C),
        content = Color.White,
        border = Color.Transparent,
    )
    private val Dark = EntrixNavigationBarColors(
        container = Color(0xFF242428),
        indicator = Color(0xFF3B3B41),
        content = Color.White,
        border = Color.White.copy(alpha = 0.08f),
    )

    @Composable
    @ReadOnlyComposable
    fun colors(): EntrixNavigationBarColors =
        if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Dark else Light
}

/**
 * A floating, fully rounded navigation bar. Place [EntrixNavigationBarItem]s inside [content].
 * Handles the system navigation-bar inset itself, so it can be used directly as a Scaffold
 * `bottomBar`.
 */
@Composable
fun EntrixNavigationBar(
    modifier: Modifier = Modifier,
    colors: EntrixNavigationBarColors = EntrixNavigationBarDefaults.colors(),
    content: @Composable RowScope.() -> Unit,
) {
    // The outer Box is transparent and only positions the pill, so whatever scrolls underneath
    // stays visible around it.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = EntrixSpacing.md, vertical = EntrixSpacing.sm + EntrixSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                // Keep the reference proportions on tablets / unfolded foldables.
                .widthIn(max = MaxBarWidth)
                .fillMaxWidth()
                .shadow(elevation = 12.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(colors.container)
                .border(1.dp, colors.border, CircleShape)
                .height(68.dp)
                .padding(horizontal = 10.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

private val MaxBarWidth = 440.dp

/**
 * One destination. Unselected: a bare outline icon. Selected: the item grows into a tinted pill
 * holding the filled icon and its [label] (label animates in/out horizontally).
 */
@Composable
fun EntrixNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    modifier: Modifier = Modifier,
    colors: EntrixNavigationBarColors = EntrixNavigationBarDefaults.colors(),
) {
    val indicator by animateColorAsState(
        targetValue = if (selected) colors.indicator else Color.Transparent,
        label = "navIndicator",
    )
    Row(
        modifier = modifier
            .height(48.dp)
            .widthIn(min = 48.dp)
            .clip(CircleShape)
            .background(indicator)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = null,
                indication = ripple(color = colors.content),
            )
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            // When selected the visible label already describes the item.
            contentDescription = if (selected) null else label,
            tint = colors.content,
            modifier = Modifier.size(24.dp),
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + expandHorizontally(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            ),
            exit = fadeOut() + shrinkHorizontally(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            ),
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(start = 10.dp, end = EntrixSpacing.xs),
                color = colors.content,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EntrixNavigationBarPreview() {
    EntrixTheme(dynamicColor = false) {
        EntrixNavigationBar {
            EntrixNavigationBarItem(true, {}, "Home", EntrixIcons.HomeFilled, EntrixIcons.HomeOutlined)
            EntrixNavigationBarItem(false, {}, "History", EntrixIcons.HistoryFilled, EntrixIcons.HistoryOutlined)
            EntrixNavigationBarItem(false, {}, "Reports", EntrixIcons.ReportsFilled, EntrixIcons.ReportsOutlined)
            EntrixNavigationBarItem(false, {}, "Settings", EntrixIcons.SettingsFilled, EntrixIcons.SettingsOutlined)
        }
    }
}
