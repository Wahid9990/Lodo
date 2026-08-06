package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class Particle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    val rotation: Float
)

@Composable
fun ConfettiEffect(modifier: Modifier = Modifier) {
    val animationProgress = remember { Animatable(0f) }

    val particles = remember {
        val colors = listOf(
            Color(0xFFE53935), Color(0xFF43A047), Color(0xFFFDD835),
            Color(0xFF1E88E5), Color(0xFFFF9800), Color(0xFFE91E63)
        )
        List(80) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.3f,
                vx = (Random.nextFloat() - 0.5f) * 0.02f,
                vy = Random.nextFloat() * 0.03f + 0.01f,
                size = Random.nextFloat() * 16f + 10f,
                color = colors.random(),
                rotation = Random.nextFloat() * 360f
            )
        }
    }

    LaunchedEffect(Unit) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing)
            )
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            val currY = ((p.y + animationProgress.value * p.vy * 30) % 1.2f) * h
            val currX = ((p.x + p.vx * animationProgress.value * 30) % 1.0f) * w

            drawRect(
                color = p.color,
                topLeft = Offset(currX, currY),
                size = Size(p.size, p.size)
            )
        }
    }
}
