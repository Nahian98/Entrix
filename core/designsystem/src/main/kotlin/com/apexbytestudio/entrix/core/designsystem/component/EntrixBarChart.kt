package com.apexbytestudio.entrix.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apexbytestudio.entrix.core.designsystem.theme.EntrixSpacing

/** One bar in an [EntrixBarChart]. [value] is in any unit; the chart normalises to the tallest bar. */
data class EntrixBarEntry(
    val label: String,
    val value: Float,
)

/**
 * A minimal, dependency-free bar chart built from layout primitives (no charting library). Bars
 * grow from the baseline and are scaled to the largest value; a zero-value bar renders as an empty
 * column. Colours come from the theme so it works in light and dark.
 */
@Composable
fun EntrixBarChart(
    entries: List<EntrixBarEntry>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    chartHeight: Dp = 160.dp,
) {
    if (entries.isEmpty()) return
    val max = entries.maxOf { it.value }.takeIf { it > 0f } ?: 1f

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(EntrixSpacing.sm),
        verticalAlignment = Alignment.Bottom,
    ) {
        entries.forEach { entry ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(trackColor),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    val fraction = (entry.value / max).coerceIn(0f, 1f)
                    if (fraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barColor),
                        )
                    }
                }
                Spacer(Modifier.height(EntrixSpacing.xs))
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}
