package dev.arkbuilders.drop.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DropLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    showBackground: Boolean = false,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    Canvas(
        modifier = modifier.size(size)
    ) {
        val canvasSize = this.size
        val center = canvasSize.center
        val radius = minOf(canvasSize.width, canvasSize.height) / 2f * 0.8f

        // Background circle if requested
        if (showBackground) {
            drawCircle(
                color = backgroundColor,
                radius = radius * 1.2f,
                center = center
            )
        }

        // Draw the main drop shape with gradient
        val gradient = Brush.radialGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.9f),
                primaryColor,
                primaryColor.copy(alpha = 0.8f)
            ),
            center = center,
            radius = radius
        )

        drawDropShape(
            center = center,
            radius = radius * 0.7f,
            brush = gradient
        )

        // Draw connection lines representing file sharing
        val connectionColor = secondaryColor.copy(alpha = 0.7f)
        val strokeWidth = radius * 0.08f

        // Draw three curved connection lines
        repeat(3) { index ->
            rotate(degrees = index * 120f, pivot = center) {
                drawConnectionLine(
                    center = center,
                    radius = radius * 0.9f,
                    color = connectionColor,
                    strokeWidth = strokeWidth
                )
            }
        }

        // Draw small dots at connection points
        repeat(6) { index ->
            val angle = (index * 60f) * (Math.PI / 180f)
            val dotRadius = radius * 0.12f
            val connectionRadius = radius * 0.9f
            
            val dotX = center.x + connectionRadius * cos(angle).toFloat()
            val dotY = center.y + connectionRadius * sin(angle).toFloat()
            
            drawCircle(
                color = secondaryColor,
                radius = dotRadius,
                center = Offset(dotX, dotY)
            )
        }

        // Inner highlight for depth
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = radius * 0.3f,
            center = Offset(center.x - radius * 0.1f, center.y - radius * 0.1f)
        )
    }
}

private fun DrawScope.drawDropShape(
    center: Offset,
    radius: Float,
    brush: Brush
) {
    val path = Path().apply {
        // Create a drop/teardrop shape
        val topY = center.y - radius * 1.2f
        val bottomY = center.y + radius * 0.8f
        val leftX = center.x - radius * 0.8f
        val rightX = center.x + radius * 0.8f

        // Start at the top point
        moveTo(center.x, topY)
        
        // Right curve
        cubicTo(
            rightX, topY + radius * 0.3f,
            rightX, bottomY - radius * 0.3f,
            center.x, bottomY
        )
        
        // Left curve
        cubicTo(
            leftX, bottomY - radius * 0.3f,
            leftX, topY + radius * 0.3f,
            center.x, topY
        )
        
        close()
    }
    
    drawPath(
        path = path,
        brush = brush
    )
}

private fun DrawScope.drawConnectionLine(
    center: Offset,
    radius: Float,
    color: Color,
    strokeWidth: Float
) {
    val path = Path().apply {
        val startX = center.x - radius * 0.3f
        val startY = center.y
        val endX = center.x + radius * 0.3f
        val endY = center.y
        val controlY = center.y - radius * 0.2f

        moveTo(startX, startY)
        quadraticBezierTo(center.x, controlY, endX, endY)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round
        )
    )
}

@Composable
fun DropLogoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    DropLogo(
        modifier = modifier,
        size = size,
        primaryColor = tint,
        secondaryColor = tint.copy(alpha = 0.7f),
        showBackground = false
    )
}

@Composable
fun DropLogoWithBackground(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp
) {
    DropLogo(
        modifier = modifier,
        size = size,
        showBackground = true
    )
}
