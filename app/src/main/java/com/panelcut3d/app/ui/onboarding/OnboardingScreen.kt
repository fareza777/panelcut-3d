package com.panelcut3d.app.ui.onboarding

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panelcut3d.app.ui.components.BrandMark
import com.panelcut3d.app.ui.components.PcButton
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val eyebrow: String,
    val title: String,
    val body: String,
    val bullets: List<String>,
    val illustration: Int,
)

private val pages = listOf(
    OnboardingPage(
        eyebrow = "PARAMETRIC EDITOR",
        title = "Design furniture\nright in 3D",
        body = "Wardrobes, tables, shelves, and cabinets are built automatically from the dimensions you enter.",
        bullets = listOf("Millimeter precision", "Camera orbit", "Move panels"),
        illustration = 0,
    ),
    OnboardingPage(
        eyebrow = "2D NESTING",
        title = "Lay out cuts,\nminimize waste",
        body = "The MaxRects algorithm places panels on plywood or MDF sheets with saw kerf and grain direction.",
        bullets = listOf("Saw kerf", "Grain direction", "Measured efficiency"),
        illustration = 1,
    ),
    OnboardingPage(
        eyebrow = "WORKSHOP READY",
        title = "Cut list, costs,\nand PDF in one tap",
        body = "Sheet needs, edge banding, selling price, shop drawings, and CSV — with your designs stored locally.",
        bullets = listOf("PDF & CSV", "Cost estimate", "Stored locally"),
        illustration = 2,
    ),
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandMark(size = 38.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "PanelCut 3D",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "FURNITURE STUDIO",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.6.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            TextButton(onClick = onDone) {
                Text("Skip", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            OnboardingPageContent(pages[page])
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pages.indices.forEach { index ->
                    val active = index == pagerState.currentPage
                    Box(
                        Modifier
                            .height(4.dp)
                            .width(if (active) 26.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                            ),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            val isLast = pagerState.currentPage == pages.lastIndex
            PcButton(
                text = if (isLast) "Start designing" else "Continue",
                onClick = {
                    if (isLast) {
                        onDone()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                pagerState.currentPage + 1,
                                animationSpec = tween(420),
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.ArrowForward,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "No account  •  Local projects  •  One-time upgrade",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    RoundedCornerShape(28.dp),
                ),
        ) {
            when (page.illustration) {
                0 -> DesignIllustration()
                1 -> NestingIllustration()
                else -> ExportIllustration()
            }
            Text(
                text = "PanelCut 3D",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(18.dp),
            )
        }
        Spacer(Modifier.height(26.dp))
        Text(
            text = page.eyebrow,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 35.sp),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        page.bullets.forEach { bullet ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 3.dp),
            ) {
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    bullet,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DesignIllustration() {
    val amber = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val ink = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.fillMaxSize().padding(28.dp)) {
        val w = size.width
        val h = size.height
        // floor ellipse
        drawOval(
            color = ink.copy(alpha = 0.10f),
            topLeft = Offset(w * 0.16f, h * 0.73f),
            size = Size(w * 0.68f, h * 0.14f),
        )
        // cabinet body
        val bodyLeft = w * 0.3f
        val bodyTop = h * 0.16f
        val bodyW = w * 0.4f
        val bodyH = h * 0.56f
        drawRect(
            brush = Brush.verticalGradient(
                listOf(amber.copy(alpha = 0.24f), amber.copy(alpha = 0.08f)),
            ),
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyW, bodyH),
        )
        drawRect(
            color = amber.copy(alpha = 0.65f),
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyW, bodyH),
            style = Stroke(2.dp.toPx()),
        )
        // shelves
        for (i in 1..3) {
            val y = bodyTop + bodyH * i / 4f
            drawLine(
                color = ink.copy(alpha = 0.16f),
                start = Offset(bodyLeft, y),
                end = Offset(bodyLeft + bodyW, y),
                strokeWidth = 1.2.dp.toPx(),
            )
        }
        // doors
        drawLine(
            color = ink.copy(alpha = 0.18f),
            start = Offset(bodyLeft + bodyW / 2f, bodyTop),
            end = Offset(bodyLeft + bodyW / 2f, bodyTop + bodyH),
            strokeWidth = 1.4.dp.toPx(),
        )
        drawCircle(amber, radius = 2.4.dp.toPx(), center = Offset(bodyLeft + bodyW * 0.56f, bodyTop + bodyH * 0.5f))
        drawCircle(amber, radius = 2.4.dp.toPx(), center = Offset(bodyLeft + bodyW * 0.44f, bodyTop + bodyH * 0.5f))
        // dimension line
        val dimY = bodyTop - 14.dp.toPx()
        drawLine(amber.copy(alpha = 0.8f), Offset(bodyLeft, dimY), Offset(bodyLeft + bodyW, dimY), strokeWidth = 1.4f)
        drawLine(amber.copy(alpha = 0.8f), Offset(bodyLeft, dimY - 4f), Offset(bodyLeft, dimY + 4f), strokeWidth = 1.4f)
        drawLine(amber.copy(alpha = 0.8f), Offset(bodyLeft + bodyW, dimY - 4f), Offset(bodyLeft + bodyW, dimY + 4f), strokeWidth = 1.4f)
        // orbit ring
        drawArc(
            color = amber.copy(alpha = 0.2f),
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(w * 0.08f, h * 0.08f),
            size = Size(w * 0.84f, h * 0.6f),
            style = Stroke(1.dp.toPx()),
        )
        // cursor
        val cursor = Path().apply {
            moveTo(w * 0.68f, h * 0.68f)
            lineTo(w * 0.68f, h * 0.82f)
            lineTo(w * 0.72f, h * 0.77f)
            lineTo(w * 0.77f, h * 0.81f)
            lineTo(w * 0.79f, h * 0.78f)
            lineTo(w * 0.74f, h * 0.74f)
            lineTo(w * 0.79f, h * 0.72f)
            close()
        }
        drawPath(cursor, surface)
        drawPath(cursor, amber, style = Stroke(1.4.dp.toPx()))
    }
}

@Composable
private fun NestingIllustration() {
    val amber = MaterialTheme.colorScheme.primary
    val sage = MaterialTheme.colorScheme.secondary
    val cream = Color(0xFFF0E3CB)
    Canvas(Modifier.fillMaxSize().padding(30.dp)) {
        val w = size.width
        val h = size.height
        val sheetLeft = w * 0.08f
        val sheetTop = h * 0.12f
        val sheetW = w * 0.84f
        val sheetH = h * 0.7f
        drawRoundRect(
            color = Color(0xFF1C2024),
            topLeft = Offset(sheetLeft, sheetTop),
            size = Size(sheetW, sheetH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
        )
        drawRoundRect(
            color = amber.copy(alpha = 0.4f),
            topLeft = Offset(sheetLeft, sheetTop),
            size = Size(sheetW, sheetH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
            style = Stroke(1.4.dp.toPx()),
        )
        val pieces = listOf(
            floatArrayOf(0.04f, 0.05f, 0.52f, 0.36f),
            floatArrayOf(0.58f, 0.05f, 0.38f, 0.36f),
            floatArrayOf(0.04f, 0.44f, 0.32f, 0.5f),
            floatArrayOf(0.38f, 0.44f, 0.3f, 0.5f),
            floatArrayOf(0.7f, 0.44f, 0.26f, 0.23f),
            floatArrayOf(0.7f, 0.7f, 0.26f, 0.24f),
        )
        pieces.forEachIndexed { i, p ->
            val left = sheetLeft + p[0] * sheetW
            val top = sheetTop + p[1] * sheetH
            val pw = p[2] * sheetW
            val ph = p[3] * sheetH
            val color = when (i % 3) {
                0 -> amber.copy(alpha = 0.8f)
                1 -> cream.copy(alpha = 0.75f)
                else -> sage.copy(alpha = 0.6f)
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(pw, ph),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
            )
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.25f),
                topLeft = Offset(left, top),
                size = Size(pw, ph),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                style = Stroke(0.8.dp.toPx()),
            )
        }
        // utilisation badge
        drawRoundRect(
            color = Color(0xFF14201A),
            topLeft = Offset(w * 0.54f, h * 0.84f),
            size = Size(w * 0.4f, h * 0.12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(50f),
        )
        drawCircle(sage, radius = 3.dp.toPx(), center = Offset(w * 0.6f, h * 0.9f))
    }
}

@Composable
private fun ExportIllustration() {
    val amber = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxSize().padding(30.dp)) {
        val w = size.width
        val h = size.height
        // back paper
        drawRoundRect(
            color = Color(0xFF23272C),
            topLeft = Offset(w * 0.22f, h * 0.12f),
            size = Size(w * 0.56f, h * 0.66f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
        )
        // front paper
        drawRoundRect(
            color = Color(0xFFF6F0E4),
            topLeft = Offset(w * 0.14f, h * 0.2f),
            size = Size(w * 0.56f, h * 0.66f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
        )
        // header lines
        drawRoundRect(
            color = amber,
            topLeft = Offset(w * 0.2f, h * 0.28f),
            size = Size(w * 0.2f, h * 0.035f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(50f),
        )
        for (i in 0..5) {
            drawRoundRect(
                color = Color(0xFF2A2A26).copy(alpha = 0.22f),
                topLeft = Offset(w * 0.2f, h * 0.37f + i * h * 0.075f),
                size = Size(w * (0.4f - i * 0.02f), h * 0.014f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(50f),
            )
        }
        // cut layout mini on right
        drawRoundRect(
            color = Color(0xFF1A1D21),
            topLeft = Offset(w * 0.58f, h * 0.42f),
            size = Size(w * 0.32f, h * 0.4f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
        )
        drawRect(
            color = amber.copy(alpha = 0.85f),
            topLeft = Offset(w * 0.62f, h * 0.46f),
            size = Size(w * 0.12f, h * 0.14f),
        )
        drawRect(
            color = amber.copy(alpha = 0.5f),
            topLeft = Offset(w * 0.75f, h * 0.46f),
            size = Size(w * 0.11f, h * 0.2f),
        )
        drawRect(
            color = amber.copy(alpha = 0.35f),
            topLeft = Offset(w * 0.62f, h * 0.63f),
            size = Size(w * 0.24f, h * 0.12f),
        )
        // coin
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFF6C87E), Color(0xFFD08A29)),
                center = Offset(w * 0.78f, h * 0.85f),
                radius = 26.dp.toPx(),
            ),
            radius = 22.dp.toPx(),
            center = Offset(w * 0.78f, h * 0.85f),
        )
        drawCircle(
            color = Color(0xFF7A4A0B),
            radius = 12.dp.toPx(),
            center = Offset(w * 0.78f, h * 0.85f),
            style = Stroke(2.dp.toPx()),
        )
    }
}
