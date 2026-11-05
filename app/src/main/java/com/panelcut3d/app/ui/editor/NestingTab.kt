package com.panelcut3d.app.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panelcut3d.app.data.Fmt
import com.panelcut3d.app.data.NestResult
import com.panelcut3d.app.data.NestSheet
import com.panelcut3d.app.data.NestingEngine
import com.panelcut3d.app.ui.components.DimStepper
import com.panelcut3d.app.ui.components.PcButton
import com.panelcut3d.app.ui.components.PcCard
import com.panelcut3d.app.ui.components.PcIconButton
import com.panelcut3d.app.ui.components.Pill
import com.panelcut3d.app.ui.components.PlanStat
import com.panelcut3d.app.ui.components.SectionHeader
import com.panelcut3d.app.ui.components.SoftSwitchRow
import kotlin.math.min

@Composable
fun NestingTab(
    state: EditorState,
    onRefresh: () -> Unit,
    onSettings: (Float, Float, Boolean) -> Unit,
    onOpenExport: () -> Unit,
) {
    val nesting = state.nesting
    var showSettings by remember { mutableStateOf(false) }
    val units = state.settings.units
    val wasteArea = nesting?.let { NestingEngine.totalWasteArea(it) } ?: 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PcCard(contentPadding = PaddingValues(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val utilization = nesting?.let { NestingEngine.utilizationPercent(it) } ?: 0f
                    UtilizationDonut(utilization)
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Sheet efficiency", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            when {
                                nesting == null -> "Calculating…"
                                nesting.sheets.isEmpty() -> "No panels to arrange yet"
                                else -> "${nesting.sheetCount} sheets • waste ${Fmt.percent(nesting.wastePercent, 1)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            PlanStat("USED", Fmt.area(nesting?.totalPieceAreaM2 ?: 0.0), Modifier.weight(1f))
                            PlanStat("WASTE", Fmt.area(wasteArea), Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Pill("Kerf ${Fmt.dimNoUnit(state.settings.kerfMm, units)} ${units.suffix}")
                    Pill("Trim ${Fmt.dimNoUnit(state.settings.trimMm, units)} ${units.suffix}")
                    Pill(if (state.settings.respectGrain) "Grain locked" else "Free grain")
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    PcButton(
                        text = "Edit settings",
                        onClick = { showSettings = true },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Tune,
                        filled = false,
                    )
                    PcButton(
                        text = "Recalculate",
                        onClick = onRefresh,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Refresh,
                    )
                }
            }
        }

        if (nesting != null && nesting.unplaced.isNotEmpty()) {
            item {
                PcCard(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f),
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(
                            "${nesting.unplaced.size} panels don't fit",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    nesting.unplaced.forEach { piece ->
                        Text(
                            "• ${piece.name} (${piece.w.toInt()} × ${piece.h.toInt()} mm) — ${piece.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (nesting != null && nesting.materialUsages.isNotEmpty()) {
            item {
                PcCard(contentPadding = PaddingValues(15.dp)) {
                    Text("KEBUTUHAN MATERIAL", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    nesting.materialUsages.forEach { usage ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(usage.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${usage.sheets} sheets ${usage.sheetW.toInt()}×${usage.sheetH.toInt()} mm",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                Fmt.money(usage.cost, state.settings.currencyCode),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Sheet layouts",
                subtitle = "Each sheet is rotated to fit the screen.",
            )
        }

        if (nesting == null || nesting.sheets.isEmpty()) {
            item {
                PcCard {
                    Text(
                        "No layout yet. Add panels or recalculate nesting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            nesting.sheets.forEach { sheet ->
                item(key = "sheet-${sheet.index}") {
                    SheetCard(sheet = sheet, units = units)
                }
            }
        }

        item {
            PcButton(
                text = "Share nesting result",
                onClick = onOpenExport,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Download,
            )
        }
    }

    if (showSettings) {
        NestingSettingsDialog(
            kerf = state.settings.kerfMm,
            trim = state.settings.trimMm,
            respectGrain = state.settings.respectGrain,
            unit = units,
            onDismiss = { showSettings = false },
            onApply = { kerf, trim, grain ->
                onSettings(kerf, trim, grain)
                showSettings = false
            },
        )
    }
}

@Composable
private fun UtilizationDonut(percent: Float) {
    Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = Color(0xFFE8A33D),
                startAngle = -90f,
                sweepAngle = 360f * (percent / 100f).coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${percent.toInt()}%",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("efficient", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SheetCard(sheet: NestSheet, units: com.panelcut3d.app.data.UnitSystem) {
    val used = 100.0 - sheet.wastePercent
    PcCard(contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    sheet.index.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("Sheet ${sheet.index}", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${sheet.materialName} • ${sheet.sheetW.toInt()} × ${sheet.sheetH.toInt()} mm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                Fmt.percent(used, 1),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(10.dp))
        SheetDiagram(
            sheet = sheet,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(sheet.sheetH / sheet.sheetW)
                .clip(RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.height(10.dp))
        sheet.placements.forEachIndexed { index, piece ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${index + 1}.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(20.dp),
                )
                Text(
                    piece.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Text(
                    "${piece.w.toInt()} × ${piece.h.toInt()} mm${if (piece.rotated) " ↻" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SheetDiagram(sheet: NestSheet, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val palette = listOf(
        Color(0xFFE8A33D),
        Color(0xFFB9CBBE),
        Color(0xFFE4C896),
        Color(0xFFC9BBA5),
        Color(0xFFA9C1B2),
        Color(0xFFDCAE7E),
    )
    val labelColor = Color(0xCC231607)
    val lineColor = Color(0xAA4A3A25)
    Canvas(modifier = modifier.background(Color(0xFF121417))) {
        val pad = 6.dp.toPx()
        val scale = min(
            (size.width - pad * 2) / sheet.sheetH,
            (size.height - pad * 2) / sheet.sheetW,
        ).coerceAtLeast(0.01f)
        val originX = (size.width - sheet.sheetH * scale) / 2f
        val originY = (size.height - sheet.sheetW * scale) / 2f
        drawRoundRect(
            color = Color(0xFFF2EEE6),
            topLeft = Offset(originX, originY),
            size = Size(sheet.sheetH * scale, sheet.sheetW * scale),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )
        drawRect(
            color = Color(0x667A6A55),
            topLeft = Offset(originX + sheet.trim * scale, originY + sheet.trim * scale),
            size = Size(
                (sheet.sheetH - sheet.trim * 2) * scale,
                (sheet.sheetW - sheet.trim * 2) * scale,
            ),
            style = Stroke(0.6.dp.toPx()),
        )
        sheet.placements.forEachIndexed { index, p ->
            val left = originX + p.y * scale
            val top = originY + p.x * scale
            val w = p.h * scale
            val h = p.w * scale
            if (w <= 0.3f || h <= 0.3f) return@forEachIndexed
            drawRect(
                color = palette[index % palette.size],
                topLeft = Offset(left, top),
                size = Size(w, h),
            )
            drawRect(
                color = lineColor,
                topLeft = Offset(left, top),
                size = Size(w, h),
                style = Stroke(0.5.dp.toPx()),
            )
            if (w > 22.dp.toPx() && h > 13.dp.toPx()) {
                val layout = textMeasurer.measure(
                    AnnotatedString("${index + 1}"),
                    style = TextStyle(
                        color = labelColor,
                        fontSize = 6.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                if (layout.size.width < w - 2.dp.toPx()) {
                    drawText(
                        layout,
                        topLeft = Offset(
                            left + w / 2f - layout.size.width / 2f,
                            top + h / 2f - layout.size.height / 2f,
                        ),
                    )
                }
            }
        }
        // grain direction marker
        val arrowX = originX + sheet.sheetH * scale + 2.dp.toPx()
        drawLine(
            color = Color(0x99E8A33D),
            start = Offset(arrowX, originY + sheet.sheetW * scale * 0.35f),
            end = Offset(arrowX, originY + sheet.sheetW * scale * 0.65f),
            strokeWidth = 1.4f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun NestingSettingsDialog(
    kerf: Float,
    trim: Float,
    respectGrain: Boolean,
    unit: com.panelcut3d.app.data.UnitSystem,
    onDismiss: () -> Unit,
    onApply: (Float, Float, Boolean) -> Unit,
) {
    var kerfMm by remember { mutableStateOf(kerf) }
    var trimMm by remember { mutableStateOf(trim) }
    var grain by remember { mutableStateOf(respectGrain) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nesting settings") },
        text = {
            Column {
                DimStepper(
                    label = "Saw kerf",
                    valueMm = kerfMm,
                    unit = unit,
                    onChangeMm = { kerfMm = it },
                    minMm = 0f,
                    maxMm = 12f,
                    stepMm = 1f,
                )
                Spacer(Modifier.height(10.dp))
                DimStepper(
                    label = "Sheet edge trim",
                    valueMm = trimMm,
                    unit = unit,
                    onChangeMm = { trimMm = it },
                    minMm = 0f,
                    maxMm = 40f,
                    stepMm = 2f,
                )
                Spacer(Modifier.height(12.dp))
                SoftSwitchRow(
                    title = "Preserve grain direction",
                    subtitle = "Pieces won't rotate against the grain",
                    checked = grain,
                    onCheckedChange = { grain = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(kerfMm, trimMm, grain) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
