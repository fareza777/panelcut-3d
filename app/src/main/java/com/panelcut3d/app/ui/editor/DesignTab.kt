package com.panelcut3d.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panelcut3d.app.data.Fmt
import com.panelcut3d.app.data.Panel
import com.panelcut3d.app.ui.components.PcCard
import com.panelcut3d.app.ui.components.PcIconButton
import com.panelcut3d.app.ui.components.SectionHeader
import com.panelcut3d.app.ui.components.TypeBadge
import com.panelcut3d.app.ui.editor.viewer.Viewer3D
import com.panelcut3d.app.ui.toColor

@Composable
fun DesignTab(
    state: EditorState,
    onSelect: (String?) -> Unit,
    onToggle3d: (Boolean) -> Unit,
    onViewerMode: (ViewerMode) -> Unit,
    stageLight: Boolean = false,
    showDoors: Boolean = true,
    exploded: Boolean = false,
    onToggleDoors: () -> Unit = {},
    onToggleExplode: () -> Unit = {},
    onOpenFullscreen: () -> Unit = {},
    onPlanView: (PlanView) -> Unit,
    onResetCamera: () -> Unit,
    onBeginDrag: () -> Unit,
    onMove: (String, Float, Float, Float) -> Unit,
    onEndDrag: () -> Unit,
    onAddPanel: () -> Unit,
    onEditPanel: (Panel) -> Unit,
    onDeletePanel: (String) -> Unit,
    onDuplicatePanel: (String) -> Unit,
    onOpenParams: () -> Unit,
    onOpenMaterials: () -> Unit,
) {
    val design = state.design
    val unit = state.settings.units
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeBadge(design.params)
                Spacer(Modifier.width(9.dp))
                Text(
                    "${design.params.width.toInt()} × ${design.params.height.toInt()} × ${design.params.depth.toInt()} mm",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                PcIconButton(
                    icon = Icons.Rounded.Tune,
                    contentDescription = "Parameter",
                    onClick = onOpenParams,
                    filled = true,
                )
                Spacer(Modifier.width(7.dp))
                PcIconButton(
                    icon = Icons.Rounded.Inventory2,
                    contentDescription = "Material",
                    onClick = onOpenMaterials,
                    filled = true,
                )
            }
        }

        item {
            PcCard(
                contentPadding = PaddingValues(6.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(336.dp)
                        .clip(RoundedCornerShape(18.dp)),
                ) {
                    if (state.is3d) {
                        Viewer3D(
                            design = design,
                            selectedId = state.selectedPanelId,
                            mode = state.viewerMode,
                            stageLight = stageLight,
                            showDoors = showDoors,
                            exploded = exploded,
                            onSelect = onSelect,
                            onMoveStart = onBeginDrag,
                            onMove = onMove,
                            onMoveEnd = onEndDrag,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Plan2D(
                            design = design,
                            selectedId = state.selectedPanelId,
                            view = state.planView,
                            unit = unit,
                            onSelect = onSelect,
                            onDragStart = onBeginDrag,
                            onMove = onMove,
                            onDragEnd = onEndDrag,
                            // Keep the drawing clear of the floating 3D/2D switch
                            // and the plan-view chips.
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 38.dp, bottom = 36.dp),
                        )
                    }

                    ViewModeToggle3D(
                        is3d = state.is3d,
                        onChange = onToggle3d,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                    )

                    if (state.is3d) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            DarkChipButton(
                                label = "Expand",
                                onClick = onOpenFullscreen,
                            ) {
                                Icon(
                                    Icons.Rounded.Fullscreen,
                                    contentDescription = null,
                                    tint = Color(0xFFE8A33D),
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                            DarkChipButton(
                                label = "Reset",
                                onClick = onResetCamera,
                            ) {
                                Icon(
                                    Icons.Rounded.RotateRight,
                                    contentDescription = null,
                                    tint = Color(0xFFE8A33D),
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (state.is3d) {
                            ViewerMode.entries.forEach { mode ->
                                ModeChip(mode.label, state.viewerMode == mode) { onViewerMode(mode) }
                            }
                        } else {
                            PlanView.entries.forEach { view ->
                                ModeChip(view.label, state.planView == view) { onPlanView(view) }
                            }
                        }
                    }

                    if (state.is3d) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            DarkChipButton(
                                label = "Doors",
                                onClick = onToggleDoors,
                                selected = showDoors,
                            ) {
                                Icon(
                                    Icons.Rounded.Inventory2,
                                    contentDescription = null,
                                    tint = if (showDoors) Color(0xFFF3C27C) else Color(0xFFB8B2A8),
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                            DarkChipButton(
                                label = "Explode",
                                onClick = onToggleExplode,
                                selected = exploded,
                            ) {
                                Icon(
                                    Icons.Rounded.Fullscreen,
                                    contentDescription = null,
                                    tint = if (exploded) Color(0xFFF3C27C) else Color(0xFFB8B2A8),
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            val selected = state.selectedPanel
            if (selected != null) {
                val material = design.material(selected.materialId)
                PcCard(contentPadding = PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(material.colorHex.toColor().copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Straighten,
                                contentDescription = null,
                                tint = material.colorHex.toColor(),
                                modifier = Modifier.size(21.dp),
                            )
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                selected.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${Fmt.dimNoUnit(selected.cutW, unit)} × ${Fmt.dimNoUnit(selected.cutH, unit)} ${unit.suffix}  •  ${material.thicknessMm.toInt()} mm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "${material.name}  •  ${selected.grain.label}  •  EB ${selected.banding.count} ${if (selected.banding.count == 1) "edge" else "edges"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            )
                        }
                        PcIconButton(
                            icon = Icons.Rounded.Edit,
                            contentDescription = "Edit panel",
                            onClick = { onEditPanel(selected) },
                            filled = true,
                        )
                        PcIconButton(
                            icon = Icons.Rounded.Delete,
                            contentDescription = "Delete panel",
                            onClick = { onDeletePanel(selected.id) },
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            } else {
                PcCard(contentPadding = PaddingValues(12.dp)) {
                    Text(
                        "Tap a panel in ${if (state.is3d) "3D" else "2D"} to select it. Once selected, drag the panel to move it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                DesignAction(
                    label = "Add panel",
                    icon = Icons.Rounded.Add,
                    modifier = Modifier.weight(1f),
                    onClick = onAddPanel,
                )
                DesignAction(
                    label = "Parameter",
                    icon = Icons.Rounded.Tune,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenParams,
                )
                DesignAction(
                    label = "Material",
                    icon = Icons.Rounded.Inventory2,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenMaterials,
                )
            }
        }

        item {
            SectionHeader(
                title = "Panel list",
                subtitle = "${design.panels.size} panels • ${Fmt.area(design.totalAreaM2())}",
            )
        }

        items(design.panels, key = { it.id }) { panel ->
            val material = design.material(panel.materialId)
            PcCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSelect(panel.id) },
                color = if (panel.id == state.selectedPanelId) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                contentPadding = PaddingValues(11.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .height(42.dp)
                            .clip(CircleShape)
                            .background(material.colorHex.toColor()),
                    )
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            panel.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${Fmt.dimNoUnit(panel.cutW, unit)} × ${Fmt.dimNoUnit(panel.cutH, unit)} ${unit.suffix}  •  ${material.thicknessMm.toInt()} mm",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            if (panel.grain == com.panelcut3d.app.data.Grain.NONE) "Free grain" else panel.grain.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "EB ${panel.banding.count} ${if (panel.banding.count == 1) "edge" else "edges"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    PcIconButton(
                        icon = Icons.Rounded.Edit,
                        contentDescription = "Edit ${panel.name}",
                        onClick = { onEditPanel(panel) },
                        modifier = Modifier,
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            Text(
                "Change parameters to rebuild all panels • ${design.primaryMaterial.name}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun DesignAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    PcCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(vertical = 13.dp, horizontal = 8.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
            Spacer(Modifier.height(7.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ViewModeToggle3D(
    is3d: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xE614171B))
            .padding(3.dp),
    ) {
        listOf("3D", "2D").forEachIndexed { index, label ->
            val active = (index == 0) == is3d
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (active) Color(0xFFE8A33D) else Color.Transparent)
                    .minimumInteractiveComponentSize()
                    .clickable { onChange(index == 0) }
                    .padding(horizontal = 13.dp, vertical = 6.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 0.4.sp,
                    color = if (active) Color(0xFF231604) else Color(0xFFB8B2A8),
                )
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) Color(0x33E8A33D) else Color(0xCC111418))
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) Color(0xFFF3C27C) else Color(0xFFA7A198),
        )
    }
}

@Composable
private fun DarkChipButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Color(0x66E8A33D) else Color(0xCC111418))
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        icon?.invoke()
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color(0xFFF3C27C) else Color(0xFFB8B2A8),
        )
    }
}
