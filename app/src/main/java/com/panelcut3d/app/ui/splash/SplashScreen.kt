package com.panelcut3d.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import com.panelcut3d.app.ui.components.BrandMark

@Composable
fun SplashScreen() {
    val scale = remember { Animatable(0.82f) }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, animationSpec = tween(600))
    }

    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(scheme.background, scheme.surfaceVariant.copy(alpha = 0.55f)),
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawGlow(scheme.primary, scheme.outlineVariant)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(reveal.value),
            ) {
                BrandMark(size = 96.dp, framed = true)
            }
            Spacer(Modifier.height(26.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "PanelCut",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onBackground,
                    letterSpacing = (-0.8).sp,
                    modifier = Modifier.alpha(reveal.value),
                )
                Text(
                    text = " 3D",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.primary,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .alpha(reveal.value),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "DESIGN  •  NESTING  •  CUT LIST",
                fontSize = 11.5.sp,
                letterSpacing = 2.2.sp,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(reveal.value),
            )
        }
        Text(
            text = "LOCAL-FIRST — YOUR DESIGNS STAY ON THIS DEVICE",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 30.dp),
        )
    }
}

private fun DrawScope.drawGlow(primary: Color, line: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(primary.copy(alpha = 0.16f), primary.copy(alpha = 0f)),
            center = Offset(size.width * 0.5f, size.height * 0.48f),
            radius = size.minDimension * 0.62f,
        ),
        radius = size.minDimension * 0.62f,
        center = Offset(size.width * 0.5f, size.height * 0.48f),
    )
    drawLine(
        color = line.copy(alpha = 0.65f),
        start = Offset(size.width * 0.3f, size.height * 0.86f),
        end = Offset(size.width * 0.7f, size.height * 0.86f),
        strokeWidth = 1f,
    )
}
