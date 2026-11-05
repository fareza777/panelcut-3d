package com.panelcut3d.app.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panelcut3d.app.data.Fmt
import com.panelcut3d.app.data.Material
import com.panelcut3d.app.data.Panel
import com.panelcut3d.app.data.ProjectDesign
import com.panelcut3d.app.data.UnitSystem
import com.panelcut3d.app.ui.toColor
import com.panelcut3d.app.ui.components.PcIconButton
import kotlin.math.min

private data class PlanTransform(val scale: Float, val ox: Float, val oy: Float)

private fun planTransform(
    width: Float,
    height: Float,
    contentW: Float,
    contentH: Float,
    padding: Float,
    zoom: Float = 1f,
    pan: Offset = Offset.Zero,
): PlanTransform {
    if (contentW <= 0f || contentH <= 0f) return PlanTransform(1f, 0f, 0f)
    val scale = min(
        (width - padding * 2f) / contentW,
        (height - padding * 2f) / contentH,
    ).coerceAtLeast(0.005f) * zoom
    return PlanTransform(
        scale = scale,
        ox = (width - contentW * scale) / 2f + pan.x,
        oy = (height - contentH * scale) / 2f + pan.y,
    )
}

private fun hitPanel(
    panels: List<Panel>,
    view: PlanView,
    x: Float,
    y: Float,
    t: PlanTransform,
    contentH: Float,
): Panel? {
    val mx = (x - t.ox) / t.scale
    val myTop = (y - t.oy) / t.scale
    val sorted = if (view == PlanView.FRONT) {
        panels.sortedByDescending { it.z }
    } else {
        panels.sortedByDescending { it.y }
    }
    return sorted.firstOrNull { p ->
        if (view == PlanView.FRONT) {
            val my = contentH - myTop
            mx >= p.x && mx <= p.x + p.sx && my >= p.y && my <= p.y + p.sy
        } else {
            mx >= p.x && mx <= p.x + p.sx && myTop >= p.z && myTop <= p.z + p.sz
        }
    }
}

@Composable
fun Plan2D(
    design: ProjectDesign,
    selectedId: String?,
    view: PlanView,
    unit: UnitSystem,
    onSelect: (String?) -> Unit,
    onDragStart: () -> Unit,
    onMove: (String, Float, Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val paddingPx = with(density) { 30.dp.toPx() }
    val textMeasurer = rememberTextMeasurer()
    val panels = design.panels
    val contentW = design.params.width
    val contentH = if (view == PlanView.FRONT) design.params.height else design.params.depth

    val background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f)
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val dimColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedColor = MaterialTheme.colorScheme.primary
    val panelStroke = Color.Black.copy(alpha = 0.35f)
    val labelFont = MaterialTheme.typography.labelSmall.fontFamily
    val materials = design.materials.associateBy { it.id }

    val designState = rememberUpdatedState(design)
    val viewState = rememberUpdatedState(view)
    val selectState = rememberUpdatedState(onSelect)
    val moveState = rememberUpdatedState(onMove)
    val startState = rememberUpdatedState(onDragStart)
    val endState = rememberUpdatedState(onDragEnd)
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val zoomState = rememberUpdatedState(zoom)
    val panState = rememberUpdatedState(pan)

    val tapModifier = Modifier.pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = {
                zoom = 1f
                pan = Offset.Zero
            },
        ) { offset ->
            val d = designState.value
            val v = viewState.value
            val ch = if (v == PlanView.FRONT) d.params.height else d.params.depth
            val t = planTransform(size.width.toFloat(), size.height.toFloat(), d.params.width, ch, paddingPx, zoomState.value, panState.value)
            selectState.value(hitPanel(d.panels, v, offset.x, offset.y, t, ch)?.id)
        }
    }

    val dragModifier = Modifier.pointerInput(Unit) {
        var active: Panel? = null
        var scale = 1f
        var moved = false
        detectDragGestures(
            onDragStart = { offset ->
                val d = designState.value
                val v = viewState.value
                val ch = if (v == PlanView.FRONT) d.params.height else d.params.depth
                val t = planTransform(size.width.toFloat(), size.height.toFloat(), d.params.width, ch, paddingPx, zoomState.value, panState.value)
                val hit = hitPanel(d.panels, v, offset.x, offset.y, t, ch)
                selectState.value(hit?.id)
                active = hit
                scale = t.scale
                moved = false
            },
            onDrag = { change, dragAmount ->
                change.consume()
                val p = active
                if (p == null) {
                    pan += dragAmount
                    return@detectDragGestures
                }
                if (scale <= 0f) return@detectDragGestures
                if (!moved) {
                    moved = true
                    startState.value()
                }
                val v = viewState.value
                val d = designState.value
                val dx = dragAmount.x / scale
                val dy = dragAmount.y / scale
                if (v == PlanView.FRONT) {
                    val nx = (p.x + dx).coerceIn(-p.sx * 0.7f, d.params.width - p.sx * 0.3f)
                    val ny = (p.y - dy).coerceIn(-p.sy * 0.7f, d.params.height - p.sy * 0.3f)
                    val next = p.copy(x = nx, y = ny)
                    active = next
                    moveState.value(p.id, nx, ny, p.z)
                } else {
                    val nx = (p.x + dx).coerceIn(-p.sx * 0.7f, d.params.width - p.sx * 0.3f)
                    val nz = (p.z + dy).coerceIn(-p.sz * 0.7f, d.params.depth - p.sz * 0.3f)
                    val next = p.copy(x = nx, z = nz)
                    active = next
                    moveState.value(p.id, nx, p.y, nz)
                }
            },
            onDragEnd = {
                if (moved) endState.value()
                active = null
                moved = false
            },
            onDragCancel = {
                if (moved) endState.value()
                active = null
                moved = false
            },
        )
    }

    Box(modifier) {
    Canvas(modifier = Modifier.fillMaxSize().then(tapModifier).then(dragModifier)) {
        val t = planTransform(size.width, size.height, contentW, contentH, paddingPx, zoom, pan)
        val areaW = contentW * t.scale
        val areaH = contentH * t.scale

        drawRoundRect(color = background, cornerRadius = CornerRadius(14.dp.toPx()))
        drawRect(color = Color.Black.copy(alpha = 0.10f), topLeft = Offset(t.ox, t.oy), size = Size(areaW, areaH))

        // 100 mm construction grid
        val gridStep = 100f
        var gx = 0f
        while (gx <= contentW + 0.1f) {
            val x = t.ox + gx * t.scale
            drawLine(gridColor, Offset(x, t.oy), Offset(x, t.oy + areaH), strokeWidth = 0.6.dp.toPx())
            gx += gridStep
        }
        var gy = 0f
        while (gy <= contentH + 0.1f) {
            val y = t.oy + (contentH - gy) * t.scale
            drawLine(gridColor, Offset(t.ox, y), Offset(t.ox + areaW, y), strokeWidth = 0.6.dp.toPx())
            gy += gridStep
        }

        drawRect(
            color = borderColor,
            topLeft = Offset(t.ox, t.oy),
            size = Size(areaW, areaH),
            style = Stroke(1.dp.toPx()),
        )

        panels.sortedBy { it.z }.forEach { panel ->
            if (panel.sx <= 0f) return@forEach
            val w = if (view == PlanView.FRONT) panel.sx else panel.sx
            val h = if (view == PlanView.FRONT) panel.sy else panel.sz
            val left: Float
            val top: Float
            if (view == PlanView.FRONT) {
                left = t.ox + panel.x * t.scale
                top = t.oy + (contentH - panel.y - panel.sy) * t.scale
            } else {
                left = t.ox + panel.x * t.scale
                top = t.oy + panel.z * t.scale
            }
            val drawW = (w * t.scale).coerceAtLeast(1f)
            val drawH = (h * t.scale).coerceAtLeast(1f)
            val material: Material? = materials[panel.materialId]
            val base = (material?.colorHex ?: "#C9A176").toColor()
            drawRect(
                color = if (panel.materialId == design.backMaterialId) base.copy(alpha = 0.5f) else base.copy(alpha = 0.93f),
                topLeft = Offset(left, top),
                size = Size(drawW, drawH),
            )
            drawRect(
                color = panelStroke,
                topLeft = Offset(left, top),
                size = Size(drawW, drawH),
                style = Stroke(0.7.dp.toPx()),
            )

            if (panel.id == selectedId) {
                drawRect(
                    color = selectedColor.copy(alpha = 0.14f),
                    topLeft = Offset(left, top),
                    size = Size(drawW, drawH),
                )
                drawRect(
                    color = selectedColor,
                    topLeft = Offset(left, top),
                    size = Size(drawW, drawH),
                    style = Stroke(2.dp.toPx()),
                )
                val handle = 3.5.dp.toPx()
                listOf(
                    Offset(left, top),
                    Offset(left + drawW, top),
                    Offset(left, top + drawH),
                    Offset(left + drawW, top + drawH),
                ).forEach { point ->
                    drawRect(
                        color = selectedColor,
                        topLeft = Offset(point.x - handle / 2f, point.y - handle / 2f),
                        size = Size(handle, handle),
                    )
                }
            }

            if (drawW > 58.dp.toPx() && drawH > 26.dp.toPx()) {
                val text = if (drawH > 46.dp.toPx()) panel.name else ""
                if (text.isNotEmpty()) {
                    val layout = textMeasurer.measure(
                        AnnotatedString(text),
                        style = TextStyle(
                            color = Color(0xCC231607),
                            fontSize = 8.5.sp,
                            fontFamily = labelFont,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        maxLines = 1,
                    )
                    if (layout.size.width < drawW - 6.dp.toPx()) {
                        drawText(
                            layout,
                            topLeft = Offset(left + 4.dp.toPx(), top + 4.dp.toPx()),
                        )
                    }
                }
                val dims = "${w.toInt()}×${h.toInt()}"
                val dimLayout = textMeasurer.measure(
                    AnnotatedString(dims),
                    style = TextStyle(
                        color = Color(0x99231607),
                        fontSize = 7.5.sp,
                        fontFamily = labelFont,
                    ),
                    maxLines = 1,
                )
                if (dimLayout.size.width < drawW - 6.dp.toPx() && drawH > 38.dp.toPx()) {
                    drawText(
                        dimLayout,
                        topLeft = Offset(left + 4.dp.toPx(), top + drawH - dimLayout.size.height - 4.dp.toPx()),
                    )
                }
            }
        }

        drawDimensionLine(
            textMeasurer = textMeasurer,
            label = Fmt.dim(contentW, unit),
            start = Offset(t.ox, t.oy - 17.dp.toPx()),
            end = Offset(t.ox + areaW, t.oy - 17.dp.toPx()),
            color = dimColor,
            textColor = labelColor,
            fontFamily = labelFont,
            vertical = false,
        )
        drawDimensionLine(
            textMeasurer = textMeasurer,
            label = Fmt.dim(contentH, unit),
            start = Offset(t.ox - 17.dp.toPx(), t.oy),
            end = Offset(t.ox - 17.dp.toPx(), t.oy + areaH),
            color = dimColor,
            textColor = labelColor,
            fontFamily = labelFont,
            vertical = true,
        )

        val legend = "Grid 100 mm"
        val legendLayout = textMeasurer.measure(
            AnnotatedString(legend),
            style = TextStyle(
                color = labelColor.copy(alpha = 0.65f),
                fontSize = 8.sp,
                fontFamily = labelFont,
            ),
        )
        drawText(
            legendLayout,
            topLeft = Offset(
                size.width - legendLayout.size.width - 12.dp.toPx(),
                size.height - legendLayout.size.height - 10.dp.toPx(),
            ),
        )
    }
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PcIconButton(
                icon = Icons.Rounded.Remove,
                contentDescription = "Zoom out plan",
                onClick = {
                    zoom = (zoom / 1.25f).coerceAtLeast(1f)
                    if (zoom == 1f) pan = Offset.Zero
                },
                filled = true,
            )
            PcIconButton(
                icon = Icons.Rounded.CenterFocusStrong,
                contentDescription = "Fit plan",
                onClick = {
                    zoom = 1f
                    pan = Offset.Zero
                },
                filled = true,
            )
            PcIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Zoom in plan",
                onClick = { zoom = (zoom * 1.25f).coerceAtMost(3.5f) },
                filled = true,
            )
        }
    }
}

private fun DrawScope.drawDimensionLine(
    textMeasurer: TextMeasurer,
    label: String,
    start: Offset,
    end: Offset,
    color: Color,
    textColor: Color,
    fontFamily: FontFamily?,
    vertical: Boolean,
) {
    val tick = 4.dp.toPx()
    drawLine(color, start, end, strokeWidth = 1.dp.toPx(), cap = StrokeCap.Round)
    if (vertical) {
        drawLine(color, Offset(start.x - tick, start.y), Offset(start.x + tick, start.y), strokeWidth = 1.dp.toPx())
        drawLine(color, Offset(end.x - tick, end.y), Offset(end.x + tick, end.y), strokeWidth = 1.dp.toPx())
    } else {
        drawLine(color, Offset(start.x, start.y - tick), Offset(start.x, start.y + tick), strokeWidth = 1.dp.toPx())
        drawLine(color, Offset(end.x, end.y - tick), Offset(end.x, end.y + tick), strokeWidth = 1.dp.toPx())
    }
    val layout = textMeasurer.measure(
        AnnotatedString(label),
        style = TextStyle(
            color = textColor,
            fontSize = 9.sp,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
        ),
        maxLines = 1,
    )
    if (vertical) {
        val midY = (start.y + end.y) / 2f
        rotate(-90f, pivot = Offset(start.x - 9.dp.toPx(), midY)) {
            drawText(
                layout,
                topLeft = Offset(
                    start.x - 9.dp.toPx() - layout.size.width / 2f,
                    midY - layout.size.height / 2f,
                ),
            )
        }
    } else {
        val midX = (start.x + end.x) / 2f
        drawText(
            layout,
            topLeft = Offset(
                midX - layout.size.width / 2f,
                start.y - layout.size.height - 2.dp.toPx(),
            ),
        )
    }
}
