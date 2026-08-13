package com.example.guitartuner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val MAX_CENTS = 50f

/**
 * 调音表盘：半圆弧量程 −50 ~ +50 音分，指针随 [cents] 摆动，|cents| < 5 变绿（文档 §8）。
 */
@Composable
fun TunerGauge(
    note: String?,
    cents: Float,
    inTune: Boolean,
    modifier: Modifier = Modifier,
) {
    val arcGray = Color(0xFFBDBDBD)
    val green = Color(0xFF2E7D32)
    val red = Color(0xFFD32F2F)
    val needleColor = if (inTune) green else red
    val noteColor = if (note == null) arcGray else needleColor
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height * 0.88f
        val radius = min(size.width, size.height) * 0.42f
        val arcTopLeft = Offset(cx - radius, cy - radius)
        val arcSize = Size(radius * 2f, radius * 2f)

        // 背景半圆弧（−50 ~ +50 音分）
        drawArc(
            color = arcGray,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
        )

        // 绿区（|cents| < 5）
        drawArc(
            color = green,
            startAngle = 90f - (IN_TUNE_CENTS * 1.8f),
            sweepAngle = IN_TUNE_CENTS * 1.8f * 2f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
        )

        // 刻度（每 10 音分）
        for (c in -50..50 step 10) {
            val rad = (90.0 - c * (90.0 / MAX_CENTS)) * PI / 180.0
            val co = cos(rad).toFloat()
            val si = sin(rad).toFloat()
            val inner = radius * 0.80f
            val outer = radius * 0.92f
            drawLine(
                color = if (c == 0) needleColor else arcGray,
                start = Offset(cx + inner * co, cy - inner * si),
                end = Offset(cx + outer * co, cy - outer * si),
                strokeWidth = if (c == 0) 4.dp.toPx() else 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // 指针
        val clamped = cents.coerceIn(-MAX_CENTS, MAX_CENTS)
        val rad = (90.0 - clamped * (90.0 / MAX_CENTS)) * PI / 180.0
        val co = cos(rad).toFloat()
        val si = sin(rad).toFloat()
        val pivot = Offset(cx, cy)
        drawLine(
            color = needleColor,
            start = pivot,
            end = Offset(cx + radius * co, cy - radius * si),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(color = needleColor, radius = 9.dp.toPx(), center = pivot)

        // 中央音名（如 "E2"）
        val noteStr = note ?: "--"
        val noteLayout = textMeasurer.measure(
            noteStr,
            TextStyle(color = noteColor, fontSize = 56.sp, fontWeight = FontWeight.Bold),
        )
        val noteTop = cy - radius * 0.62f
        drawText(noteLayout, topLeft = Offset(cx - noteLayout.size.width / 2f, noteTop))

        // 音分数值
        if (note != null) {
            val centsLayout = textMeasurer.measure(
                formatCents(cents),
                TextStyle(color = needleColor, fontSize = 18.sp),
            )
            drawText(
                centsLayout,
                topLeft = Offset(
                    cx - centsLayout.size.width / 2f,
                    noteTop + noteLayout.size.height + 6.dp.toPx(),
                ),
            )
        }
    }
}

private const val IN_TUNE_CENTS = 5f

private fun formatCents(cents: Float): String {
    val v = cents.roundToInt()
    return if (v >= 0) "+$v ¢" else "$v ¢"
}
