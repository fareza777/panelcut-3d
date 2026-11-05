package com.panelcut3d.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panelcut3d.app.data.CutRow
import com.panelcut3d.app.data.Fmt
import com.panelcut3d.app.data.Material
import com.panelcut3d.app.data.NestResult
import com.panelcut3d.app.data.ProjectDesign
import com.panelcut3d.app.data.UnitSystem
import com.panelcut3d.app.data.cutRowsByMaterial
import com.panelcut3d.app.ui.components.DividerSoft
import com.panelcut3d.app.ui.components.MaterialDot
import com.panelcut3d.app.ui.components.PcButton
import com.panelcut3d.app.ui.components.PcCard
import com.panelcut3d.app.ui.components.Pill
import com.panelcut3d.app.ui.components.PlanStat
import com.panelcut3d.app.ui.components.SectionHeader

@Composable
fun CutListTab(
    design: ProjectDesign,
    nesting: NestResult?,
    units: UnitSystem,
    onOpenExport: () -> Unit,
    selectedPanelId: String? = null,
    onSelectPanel: (String?) -> Unit = {},
) {
    val groups = remember(design) { design.cutRowsByMaterial() }
    var filter by remember { mutableStateOf<String?>(null) }
    val filtered = groups.filter { filter == null || it.first.id == filter }
    val totalPanelCount = design.panels.size
    val totalSheets = nesting?.sheetCount ?: 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PcCard(contentPadding = PaddingValues(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(13.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(11.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Straighten,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Production-ready cut list", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Cut dimensions are final. Kerf is handled in nesting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                DividerSoft()
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlanStat("PANELS", totalPanelCount.toString(), Modifier.weight(1f))
                    PlanStat("MATERIAL", Fmt.area(design.totalAreaM2()), Modifier.weight(1.15f))
                    PlanStat("SHEETS", if (totalSheets > 0) totalSheets.toString() else "—", Modifier.weight(1f))
                    PlanStat("WASTE", if (nesting != null) Fmt.percent(nesting.wastePercent, 0) else "—", Modifier.weight(1f))
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                item {
                    Pill(
                        text = "All (${groups.size})",
                        selected = filter == null,
                        onClick = { filter = null },
                    )
                }
                items(groups, key = { it.first.id }) { group ->
                    Pill(
                        text = group.first.name,
                        selected = filter == group.first.id,
                        onClick = { filter = group.first.id },
                    )
                }
            }
        }

        items(filtered, key = { it.first.id }) { (material, rows) ->
            CutListGroup(
                material = material,
                rows = rows,
                units = units,
                selectedPanelId = selectedPanelId,
                onSelectPanel = onSelectPanel,
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                PcButton(
                    text = "Export PDF",
                    onClick = onOpenExport,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Download,
                )
                PcButton(
                    text = "CSV",
                    onClick = onOpenExport,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Download,
                    filled = false,
                )
            }
        }

        item {
            Text(
                "Edge banding: T = top, B = bottom, L = left, R = right. Total ${Fmt.number(design.bandingMeters(), 2)} meters.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun CutListGroup(
    material: Material,
    rows: List<CutRow>,
    units: UnitSystem,
    selectedPanelId: String?,
    onSelectPanel: (String?) -> Unit,
) {
    val panelCount = rows.sumOf { it.qty }
    PcCard(contentPadding = PaddingValues(0.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MaterialDot(material.colorHex, 10.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    material.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "$panelCount ${if (panelCount == 1) "panel" else "panels"} • ${material.thicknessMm.toInt()} mm • ${Fmt.area(rows.sumOf { it.areaM2 })}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${rows.size} ${if (rows.size == 1) "type" else "types"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
            rows.forEachIndexed { index, row ->
                if (index > 0) DividerSoft()
                CutListRow(
                    number = index + 1,
                    row = row,
                    units = units,
                    selected = selectedPanelId != null && selectedPanelId in row.panelIds,
                    onClick = { onSelectPanel(row.panelIds.firstOrNull()) },
                )
            }
        }
    }
}

@Composable
private fun CutListRow(
    number: Int,
    row: CutRow,
    units: UnitSystem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
        ) {
            Text(
                number.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                row.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${Fmt.dimNoUnit(row.cutW, units)} × ${Fmt.dimNoUnit(row.cutH, units)} ${units.suffix}  •  ${row.grain.label}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (row.banding.any) {
                Text(
                    "Edge: ${row.banding.shortLabel()} (${row.banding.count} ${if (row.banding.count == 1) "edge" else "edges"})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${row.qty}×",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                Fmt.number(row.areaM2, 2) + " m²",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (selected) {
                Text(
                    "IN 3D",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}
