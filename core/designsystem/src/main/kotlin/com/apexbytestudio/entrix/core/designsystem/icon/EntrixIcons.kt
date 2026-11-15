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
}

private inline fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
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
