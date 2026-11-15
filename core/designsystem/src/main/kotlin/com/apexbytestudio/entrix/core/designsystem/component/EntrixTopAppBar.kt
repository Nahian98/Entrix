package com.apexbytestudio.entrix.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.apexbytestudio.entrix.core.designsystem.icon.EntrixIcons
import kotlin.math.roundToInt

/**
 * The app's header. Expanded, it shows a large left-aligned [title] (plus optional [subtitle])
 * directly under the status bar, with the back button and [actions] on the same line as the title,
 * so there is no empty toolbar row above it. As content scrolls (via [scrollBehavior], typically
 * `exitUntilCollapsed`) it shrinks to a compact 64dp bar with a small title and a tonal container.
 *
 * Pass [onNavigateUp] on pushed screens to show a back button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrixTopAppBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onNavigateUp: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val state = scrollBehavior.state
    val expandedColor = MaterialTheme.colorScheme.background
    val collapsedColor = MaterialTheme.colorScheme.surfaceContainer
    // Every collapse-dependent value below is read in the layout/draw phase only, so scrolling
    // re-lays-out and redraws the header without recomposing it.
    val largeAlpha = { (1f - state.collapsedFraction * 2f).coerceIn(0f, 1f) }
    val smallAlpha = { ((state.collapsedFraction - 0.5f) * 2f).coerceIn(0f, 1f) }

    Layout(
        modifier = modifier
            .drawBehind { drawRect(lerp(expandedColor, collapsedColor, state.collapsedFraction)) }
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
            .clipToBounds(),
        content = {
            if (onNavigateUp != null) {
                IconButton(onClick = onNavigateUp, modifier = Modifier.layoutId(NavId)) {
                    Icon(EntrixIcons.Back, contentDescription = "Navigate up")
                }
            }
            Row(modifier = Modifier.layoutId(ActionsId), content = actions)
            Text(
                text = title,
                modifier = Modifier
                    .layoutId(LargeTitleId)
                    .graphicsLayer { alpha = largeAlpha() }
                    .semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    modifier = Modifier
                        .layoutId(SubtitleId)
                        .graphicsLayer { alpha = largeAlpha() },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Duplicate of the title for the collapsed state; hidden from accessibility so the
            // title is announced once.
            Text(
                text = title,
                modifier = Modifier
                    .layoutId(SmallTitleId)
                    .graphicsLayer { alpha = smallAlpha() }
                    .clearAndSetSemantics { },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val loose = Constraints(maxWidth = width)
        val collapsedHeight = CollapsedHeight.roundToPx()
        val edge = EdgePadding.roundToPx()
        val iconEdge = IconEdgePadding.roundToPx()

        fun measureOptional(id: String, c: Constraints): Placeable? =
            measurables.firstOrNull { it.layoutId == id }?.measure(c)

        fun measure(id: String, c: Constraints): Placeable =
            measurables.first { it.layoutId == id }.measure(c)

        val nav = measureOptional(NavId, loose)
        val actions = measure(ActionsId, loose)
        val titleStart = if (nav != null) iconEdge + nav.width + TitleGap.roundToPx() else edge
        val titleEnd = if (actions.width > 0) iconEdge + actions.width else edge
        val titleWidth = (width - titleStart - titleEnd).coerceAtLeast(0)
        val textConstraints = Constraints(maxWidth = titleWidth)
        val largeTitle = measure(LargeTitleId, textConstraints)
        val subtitle = measureOptional(SubtitleId, textConstraints)
        val smallTitle = measure(SmallTitleId, textConstraints)

        // Expanded height is driven by the actual text size (font scale aware), never less than
        // the collapsed bar. Report the scroll range to the behavior when it changes.
        val topPad = ExpandedTopPadding.roundToPx()
        val contentHeight = topPad + largeTitle.height + (subtitle?.height ?: 0) +
            ExpandedBottomPadding.roundToPx()
        val expandedHeight = maxOf(collapsedHeight, contentHeight)
        val offsetLimit = (collapsedHeight - expandedHeight).toFloat()
        if (state.heightOffsetLimit != offsetLimit) state.heightOffsetLimit = offsetLimit

        val height = (expandedHeight + state.heightOffset).roundToInt()
            .coerceIn(collapsedHeight, expandedHeight)
        val fraction = state.collapsedFraction
        // The large title scrolls up with the content as the header collapses.
        val scrolledBy = expandedHeight - height

        layout(width, height) {
            // Icons sit on the large title's line when expanded, and centre in the bar when
            // collapsed.
            val expandedLine = topPad + largeTitle.height / 2f
            val iconLine = expandedLine + (collapsedHeight / 2f - expandedLine) * fraction
            nav?.place(iconEdge, (iconLine - nav.height / 2f).roundToInt())
            actions.place(width - iconEdge - actions.width, (iconLine - actions.height / 2f).roundToInt())

            val largeY = topPad - scrolledBy
            largeTitle.place(titleStart, largeY)
            subtitle?.place(titleStart, largeY + largeTitle.height)
            smallTitle.place(titleStart, (collapsedHeight - smallTitle.height) / 2)
        }
    }
}

private const val NavId = "nav"
private const val ActionsId = "actions"
private const val LargeTitleId = "largeTitle"
private const val SubtitleId = "subtitle"
private const val SmallTitleId = "smallTitle"

private val CollapsedHeight = 64.dp
private val EdgePadding = 16.dp
private val IconEdgePadding = 4.dp
private val TitleGap = 4.dp
private val ExpandedTopPadding = 12.dp
private val ExpandedBottomPadding = 20.dp
