package com.apexbytestudio.entrix.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Entrix's own navigation icon set: a minimal, rounded line style (1.5px stroke on a 24px grid)
 * with a solid "filled" twin for the selected state. Filled variants keep their inner details as
 * even-odd holes so they stay single-colour and tint correctly through `Icon`.
 */
object EntrixIcons {

    val HomeOutlined: ImageVector by lazy {
        icon("HomeOutlined") {
            strokePath { house() }
            strokePath {
                moveTo(12f, 17f)
                verticalLineTo(18.5f)
            }
        }
    }

    val HomeFilled: ImageVector by lazy {
        icon("HomeFilled") {
            fillPath(PathFillType.EvenOdd) {
                house()
                // Door notch, cut out of the solid body.
                moveTo(11.25f, 16.25f)
                arcToRelative(0.75f, 0.75f, 0f, false, true, 1.5f, 0f)
                verticalLineTo(19f)
                horizontalLineToRelative(-1.5f)
                close()
            }
            strokePath { house() }
        }
    }

    val HistoryOutlined: ImageVector by lazy {
        icon("HistoryOutlined") {
            strokePath { circle(12f, 12f, 8.5f) }
            strokePath {
                moveTo(12f, 7.75f)
                verticalLineTo(12f)
                lineTo(15f, 14f)
            }
        }
    }

    val HistoryFilled: ImageVector by lazy {
        icon("HistoryFilled") {
            fillPath(PathFillType.EvenOdd) {
                circle(12f, 12f, 9.25f)
                // Clock hands, cut out as a single rounded L-shaped hole.
                moveTo(11.25f, 8f)
                arcTo(0.75f, 0.75f, 0f, false, true, 12.75f, 8f)
                lineTo(12.75f, 11.6f)
                lineTo(15.42f, 13.38f)
                arcTo(0.75f, 0.75f, 0f, false, true, 14.58f, 14.62f)
                lineTo(11.58f, 12.62f)
                arcTo(0.75f, 0.75f, 0f, false, true, 11.25f, 12f)
                close()
            }
        }
    }

    val ReportsOutlined: ImageVector by lazy {
        icon("ReportsOutlined") {
            strokePath { bars() }
        }
    }

    val ReportsFilled: ImageVector by lazy {
        icon("ReportsFilled") {
            fillPath { bars() }
            strokePath { bars() }
        }
    }

    val SettingsOutlined: ImageVector by lazy {
        icon("SettingsOutlined") {
            strokePath {
                moveTo(4f, 8f); horizontalLineTo(12.75f)
                moveTo(17.25f, 8f); horizontalLineTo(20f)
                moveTo(4f, 16f); horizontalLineTo(6.75f)
                moveTo(11.25f, 16f); horizontalLineTo(20f)
                circle(15f, 8f, 2.25f)
                circle(9f, 16f, 2.25f)
            }
        }
    }

    val SettingsFilled: ImageVector by lazy {
        icon("SettingsFilled") {
            strokePath {
                moveTo(4f, 8f); horizontalLineTo(20f)
                moveTo(4f, 16f); horizontalLineTo(20f)
            }
            fillPath {
                circle(15f, 8f, 3f)
                circle(9f, 16f, 3f)
            }
        }
    }

    /** Chevron pointing "forward"; the mirror of [Back]. Auto-mirrored for RTL. */
    val Forward: ImageVector by lazy {
        icon("Forward", autoMirror = true) {
            strokePath {
                moveTo(9.5f, 5.5f)
                lineTo(16f, 12f)
                lineTo(9.5f, 18.5f)
            }
        }
    }

    /** Arrow rising out of a tray: export / share a file. */
    val Export: ImageVector by lazy {
        icon("Export") {
            strokePath {
                moveTo(12f, 14.5f); verticalLineTo(4f)
                moveTo(8f, 8f); lineTo(12f, 4f); lineTo(16f, 8f)
                moveTo(5f, 13f); verticalLineTo(17.5f)
                arcToRelative(2.5f, 2.5f, 0f, false, false, 2.5f, 2.5f)
                horizontalLineToRelative(9f)
                arcToRelative(2.5f, 2.5f, 0f, false, false, 2.5f, -2.5f)
                verticalLineTo(13f)
            }
        }
    }

    /** Solid circle with a check cut out: "done / all good". */
    val CheckCircle: ImageVector by lazy {
        icon("CheckCircle") {
            fillPath(PathFillType.EvenOdd) {
                circle(12f, 12f, 9.25f)
                // Check mark cut out as the outline of a 1.5-wide stroke along
                // (7.75,12.25) → (10.5,15) → (16.25,9.25), with round end caps.
                moveTo(7.22f, 12.78f)
                lineTo(10.5f, 16.06f)
                lineTo(16.78f, 9.78f)
                arcTo(0.75f, 0.75f, 0f, false, false, 15.72f, 8.72f)
                lineTo(10.5f, 13.94f)
                lineTo(8.28f, 11.72f)
                arcTo(0.75f, 0.75f, 0f, false, false, 7.22f, 12.78f)
                close()
            }
        }
    }

    /** Circle outline with an exclamation mark: "needs your attention". */
    val Alert: ImageVector by lazy {
        icon("Alert") {
            strokePath {
                circle(12f, 12f, 8.5f)
                moveTo(12f, 7.75f); verticalLineTo(12.75f)
            }
            fillPath { circle(12f, 16.1f, 1.05f) }
        }
    }

    /** Vertical ellipsis, for an overflow menu of secondary actions. */
    val More: ImageVector by lazy {
        icon("More") {
            fillPath {
                circle(12f, 5.5f, 1.6f)
                circle(12f, 12f, 1.6f)
                circle(12f, 18.5f, 1.6f)
            }
        }
    }

    /** Trash can, for destructive "clear/delete" actions. */
    val Delete: ImageVector by lazy {
        icon("Delete") {
            strokePath {
                // Lid and handle.
                moveTo(4f, 6.5f); horizontalLineTo(20f)
                moveTo(9.5f, 6.5f); verticalLineTo(5f)
                arcToRelative(1f, 1f, 0f, false, true, 1f, -1f)
                horizontalLineToRelative(3f)
                arcToRelative(1f, 1f, 0f, false, true, 1f, 1f)
                verticalLineTo(6.5f)
                // Can body.
                moveTo(6f, 6.5f)
                lineTo(6.9f, 18.6f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, 1.9f)
                horizontalLineToRelative(6.2f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, -1.9f)
                lineTo(18f, 6.5f)
                // Ribs.
                moveTo(10f, 10.5f); verticalLineTo(16.5f)
                moveTo(14f, 10.5f); verticalLineTo(16.5f)
            }
        }
    }

    /** Same line style as the nav icons. Auto-mirrored so it points "back" in RTL layouts. */
    val Back: ImageVector by lazy {
        icon("Back", autoMirror = true) {
            strokePath {
                moveTo(14.5f, 5.5f)
                lineTo(8f, 12f)
                lineTo(14.5f, 18.5f)
            }
        }
    }
}

private inline fun icon(
    name: String,
    autoMirror: Boolean = false,
    block: ImageVector.Builder.() -> Unit,
): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = autoMirror,
    ).apply(block).build()

private const val STROKE_WIDTH = 1.5f

private inline fun ImageVector.Builder.strokePath(block: PathBuilder.() -> Unit) = path(
    fill = null,
    stroke = SolidColor(Color.Black),
    strokeLineWidth = STROKE_WIDTH,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
    pathBuilder = block,
)

private inline fun ImageVector.Builder.fillPath(
    fillType: PathFillType = PathFillType.NonZero,
    block: PathBuilder.() -> Unit,
) = path(
    fill = SolidColor(Color.Black),
    pathFillType = fillType,
    pathBuilder = block,
)

/** House body with a softly rounded roof peak and rounded base. */
private fun PathBuilder.house() {
    moveTo(4f, 10f)
    lineTo(10.4f, 4.4f)
    curveTo(11.3f, 3.6f, 12.7f, 3.6f, 13.6f, 4.4f)
    lineTo(20f, 10f)
    verticalLineTo(18f)
    curveTo(20f, 19.66f, 18.66f, 21f, 17f, 21f)
    horizontalLineTo(7f)
    curveTo(5.34f, 21f, 4f, 19.66f, 4f, 18f)
    close()
}

/** Three rounded bars of a bar chart. */
private fun PathBuilder.bars() {
    roundRect(3.75f, 12f, 7.25f, 20.25f, 1.25f)
    roundRect(10.25f, 3.75f, 13.75f, 20.25f, 1.25f)
    roundRect(16.75f, 8.5f, 20.25f, 20.25f, 1.25f)
}

private fun PathBuilder.roundRect(l: Float, t: Float, r: Float, b: Float, rad: Float) {
    moveTo(l + rad, t)
    horizontalLineTo(r - rad)
    arcToRelative(rad, rad, 0f, false, true, rad, rad)
    verticalLineTo(b - rad)
    arcToRelative(rad, rad, 0f, false, true, -rad, rad)
    horizontalLineTo(l + rad)
    arcToRelative(rad, rad, 0f, false, true, -rad, -rad)
    verticalLineTo(t + rad)
    arcToRelative(rad, rad, 0f, false, true, rad, -rad)
    close()
}

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcToRelative(r, r, 0f, true, true, 2 * r, 0f)
    arcToRelative(r, r, 0f, true, true, -2 * r, 0f)
    close()
}
