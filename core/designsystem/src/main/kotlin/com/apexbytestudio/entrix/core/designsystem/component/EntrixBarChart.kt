package com.apexbytestudio.entrix.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apexbytestudio.entrix.core.designsystem.theme.EntrixSpacing

/**
 * One bar in an [EntrixBarChart]. [value] is in any unit (the chart normalises to the tallest bar);
 * [valueLabel] is how that value reads to a person, e.g. "8h 46m".
 */
data class EntrixBarEntry(
    val label: String,
    val value: Float,
    val valueLabel: String = "",
)

/**
 * A dependency-free bar chart in the style of modern health/time apps:
 * - a caption row that reads out the selected bar ("Wed · 8h 46m"), defaulting to [initialSelection]
 *   (typically today) — tap any bar to inspect it;
 * - the selected bar in full accent, the others softened so the eye lands on one value;
 * - an optional dashed [average] line with its label in the caption row;
 * - empty slots drawn as a short stub so every day still has a place.
 *
 * Each bar is its own selectable element, so TalkBack can step through them with their values.
 */
@Composable
fun EntrixBarChart(
    entries: List<EntrixBarEntry>,
    modifier: Modifier = Modifier,
    initialSelection: Int? = null,
    average: Float? = null,
    averageLabel: String? = null,
    barColor: Color = MaterialTheme.colorScheme.primary,
    stubColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    chartHeight: Dp = 168.dp,
) {
    if (entries.isEmpty()) return
    val max = maxOf(entries.maxOf { it.value }, average ?: 0f).takeIf { it > 0f } ?: 1f
    // Re-seed the selection when the data set changes (another week/month).
    var selected by rememberSaveable(entries.map { it.label }) { mutableStateOf(initialSelection) }
    val guideColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    // Only soften the other bars when the selected one has a value to focus on.
    val focusIndex = selected?.takeIf { (entries.getOrNull(it)?.value ?: 0f) > 0f }

    Column(modifier = modifier.fillMaxWidth()) {
        CaptionRow(
            selectedEntry = selected?.let(entries::getOrNull),
            averageLabel = averageLabel,
            guideColor = guideColor,
        )
        Spacer(Modifier.height(EntrixSpacing.md))
        Box(
            Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .selectableGroup(),
                verticalAlignment = Alignment.Bottom,
            ) {
                entries.forEachIndexed { index, entry ->
                    val isSelected = selected == index
                    val fraction by animateFloatAsState(
                        targetValue = (entry.value / max).coerceIn(0f, 1f),
                        label = "barHeight",
                    )
                    val color = when {
                        focusIndex == null || focusIndex == index -> barColor
                        else -> barColor.copy(alpha = 0.4f)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(
                                selected = isSelected,
                                role = Role.Tab,
                                onClick = { selected = if (isSelected) null else index },
                            )
                            .semantics {
                                contentDescription = "${entry.label}, ${entry.valueLabel.ifEmpty { "none" }}"
                            },
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.56f)
                                .widthIn(max = 36.dp)
                                .fillMaxHeight(if (entry.value > 0f) fraction else 0f)
                                .clip(RoundedCornerShape(8.dp))
                                .drawBehind { drawRect(color) },
                        )
                        if (entry.value <= 0f) {
                            Box(
                                Modifier
                                    .fillMaxWidth(0.56f)
                                    .widthIn(max = 36.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .drawBehind { drawRect(stubColor) },
                            )
                        }
                    }
                }
            }
            if (average != null && average > 0f) {
                // Dashed average guide, drawn over the bars.
                Canvas(Modifier.fillMaxSize()) {
                    val y = size.height * (1f - average / max)
                    drawLine(
                        color = guideColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                    )
                }
            }
        }
        Spacer(Modifier.height(EntrixSpacing.sm))
        Row(Modifier.fillMaxWidth()) {
            entries.forEachIndexed { index, entry ->
                Text(
                    text = entry.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected == index) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CaptionRow(
    selectedEntry: EntrixBarEntry?,
    averageLabel: String?,
    guideColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = selectedEntry?.let { "${it.label} · ${it.valueLabel.ifEmpty { "—" }}" }
                ?: "Tap a bar for details",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selectedEntry != null) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selectedEntry != null) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        if (averageLabel != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.width(16.dp).height(2.dp)) {
                    drawLine(
                        color = guideColor,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                    )
                }
                Spacer(Modifier.width(EntrixSpacing.xs))
                Text(
                    text = averageLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
