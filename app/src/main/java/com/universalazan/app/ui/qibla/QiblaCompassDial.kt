package com.universalazan.app.ui.qibla

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.universalazan.app.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Compass dial. The whole dial is rotated by -[heading] so its "N" points to true north;
 * the gold Kaaba marker sits at [qiblaBearing] on the dial. The green pointer at the top is
 * fixed and shows where the phone points. When the marker is under the pointer, the phone faces
 * the Qibla.
 */
@Composable
fun QiblaCompassDial(
    heading: Double,
    qiblaBearing: Double,
    facing: Boolean,
    description: String,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
    val labels = listOf(
        stringResource(R.string.compass_north),
        stringResource(R.string.compass_east),
        stringResource(R.string.compass_south),
        stringResource(R.string.compass_west),
    )
    val ringColor = if (facing) colors.primary else colors.outline
    val qiblaColor = colors.secondary
    val tickColor = colors.onSurfaceVariant
    val pointerColor = colors.primary
    val northColor = colors.error

    Canvas(
        modifier = modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics { contentDescription = description },
    ) {
        val radius = min(size.width, size.height) / 2f * 0.86f
        val center = Offset(size.width / 2f, size.height / 2f)

        fun pointAt(angleDegrees: Double, distance: Float): Offset {
            val rad = Math.toRadians(angleDegrees)
            return Offset(center.x + distance * sin(rad).toFloat(), center.y - distance * cos(rad).toFloat())
        }

        // Fixed pointer at the top: where the phone is pointing.
        val pointer = Path().apply {
            moveTo(center.x, center.y - radius - 4.dp.toPx())
            lineTo(center.x - 10.dp.toPx(), center.y - radius - 22.dp.toPx())
            lineTo(center.x + 10.dp.toPx(), center.y - radius - 22.dp.toPx())
            close()
        }
        drawPath(pointer, pointerColor)

        rotate(degrees = (-heading).toFloat(), pivot = center) {
            drawCircle(ringColor, radius = radius, center = center, style = Stroke(width = if (facing) 6.dp.toPx() else 3.dp.toPx()))

            // Tick marks every 10°, longer every 30°.
            for (angle in 0 until 360 step 10) {
                val long = angle % 30 == 0
                val inner = radius - (if (long) 14.dp.toPx() else 7.dp.toPx())
                drawLine(
                    color = tickColor,
                    start = pointAt(angle.toDouble(), inner),
                    end = pointAt(angle.toDouble(), radius),
                    strokeWidth = if (long) 2.dp.toPx() else 1.dp.toPx(),
                )
            }

            // N, E, S, W (N in red). Letters stay upright relative to the dial.
            labels.forEachIndexed { index, label ->
                val angle = index * 90.0
                val layout = textMeasurer.measure(label, TextStyle(color = if (index == 0) northColor else tickColor).merge(labelStyle))
                val at = pointAt(angle, radius - 32.dp.toPx())
                rotate(degrees = angle.toFloat(), pivot = at) {
                    drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
                }
            }

            // Qibla line and Kaaba marker.
            val markerCentre = pointAt(qiblaBearing, radius - 4.dp.toPx())
            drawLine(qiblaColor, start = center, end = pointAt(qiblaBearing, radius - 16.dp.toPx()), strokeWidth = 4.dp.toPx())
            val half = 9.dp.toPx()
            rotate(degrees = qiblaBearing.toFloat(), pivot = markerCentre) {
                drawRect(qiblaColor, topLeft = Offset(markerCentre.x - half, markerCentre.y - half), size = Size(half * 2, half * 2))
            }
        }
        drawCircle(pointerColor, radius = 5.dp.toPx(), center = center)
    }
}
