package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.data.analytics.AcademicTermTrendPoint

/** Compact accessible trend chart; null values create gaps rather than fabricated zeroes. */
@Composable
fun AcademicTrendChart(points: List<AcademicTermTrendPoint>, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val description = points.joinToString(", ") { point ->
        "${point.termName}: ${point.averagePercentage?.let { "%.1f percent".format(it) } ?: "no average"}"
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Text("Term average trend", style = MaterialTheme.typography.titleSmall)
        Canvas(
            Modifier.fillMaxWidth().height(132.dp).padding(top = 8.dp)
                .semantics { contentDescription = "Academic percentage trend. $description" }
        ) {
            val left = 8f
            val right = size.width - 8f
            val top = 8f
            val bottom = size.height - 8f
            val plotHeight = bottom - top
            val plotWidth = right - left
            for (step in 0..4) {
                val y = top + plotHeight * step / 4f
                drawLine(grid, Offset(left, y), Offset(right, y), strokeWidth = 1f)
            }
            val values = points.map { it.averagePercentage }
            val denominator = (values.size - 1).coerceAtLeast(1)
            var previous: Offset? = null
            values.forEachIndexed { index, value ->
                if (value == null || !value.isFinite()) {
                    previous = null
                } else {
                    val point = Offset(
                        x = left + plotWidth * index / denominator,
                        y = bottom - (value.coerceIn(0.0, 100.0).toFloat() / 100f) * plotHeight
                    )
                    previous?.let { drawLine(primary, it, point, strokeWidth = 3f, cap = StrokeCap.Round) }
                    drawCircle(primary, radius = 4f, center = point)
                    previous = point
                }
            }
        }
        Text("${points.first().termName}  →  ${points.last().termName} · percentage scale 0–100", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
