package com.panelcut3d.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panelcut3d.app.data.Defaults
import com.panelcut3d.app.data.DesignEngine
import com.panelcut3d.app.data.DesignParams
import com.panelcut3d.app.data.Fmt
import com.panelcut3d.app.data.FurnitureType
import com.panelcut3d.app.data.Grain
import com.panelcut3d.app.data.Material
import com.panelcut3d.app.data.Panel
import com.panelcut3d.app.data.ProjectDesign
import com.panelcut3d.app.data.UnitSystem
import com.panelcut3d.app.ui.components.DimStepper
import com.panelcut3d.app.ui.components.IntStepper
import com.panelcut3d.app.ui.components.MaterialDot
import com.panelcut3d.app.ui.components.MoneyStepper
import com.panelcut3d.app.ui.components.PcBottomSheet
import com.panelcut3d.app.ui.components.PcButton
import com.panelcut3d.app.ui.components.PcCard
import com.panelcut3d.app.ui.components.Pill
import com.panelcut3d.app.ui.components.Segmented
import com.panelcut3d.app.ui.components.SheetTitle
import com.panelcut3d.app.ui.components.SoftSwitchRow
import com.panelcut3d.app.ui.toColor

@Composable
fun ParamsSheet(
    initial: DesignParams,
    unit: UnitSystem,
    onDismiss: () -> Unit,
    onApply: (DesignParams) -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    PcBottomSheet(onDismiss = onDismiss) {
        SheetTitle(
            title = "Furniture parameters",
            subtitle = "Enter precise dimensions. Panels are rebuilt automatically.",
        )

        Text("FURNITURE TYPE", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            FurnitureType.entries.forEach { type ->
                Pill(
                    text = type.label,
                    selected = draft.type == type,
                    onClick = { draft = Defaults.paramsFor(type) },
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("OUTER DIMENSIONS", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            DimStepper(
                label = "Width (W)",
                valueMm = draft.width,
                unit = unit,
                onChangeMm = { draft = draft.copy(width = it) },
                modifier = Modifier.weight(1f),
                minMm = 300f,
                maxMm = 4000f,
                stepMm = if (unit == UnitSystem.CM) 10f else 50f,
            )
            DimStepper(
                label = "Height (H)",
                valueMm = draft.height,
                unit = unit,
                onChangeMm = { draft = draft.copy(height = it) },
                modifier = Modifier.weight(1f),
                minMm = 300f,
                maxMm = 3000f,
                stepMm = if (unit == UnitSystem.CM) 10f else 50f,
            )
        }
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            DimStepper(
                label = "Depth (D)",
                valueMm = draft.depth,
                unit = unit,
                onChangeMm = { draft = draft.copy(depth = it) },
                modifier = Modifier.weight(1f),
                minMm = 150f,
                maxMm = 1200f,
                stepMm = if (unit == UnitSystem.CM) 10f else 50f,
            )
            if (draft.type == FurnitureType.TABLE) {
                DimStepper(
                    label = "Leg size",
                    valueMm = draft.legSize,
                    unit = unit,
                    onChangeMm = { draft = draft.copy(legSize = it) },
                    modifier = Modifier.weight(1f),
                    minMm = 30f,
                    maxMm = 120f,
                    stepMm = 5f,
                )
            } else {
                DimStepper(
                    label = "Door gap",
                    valueMm = draft.doorGap,
                    unit = unit,
                    onChangeMm = { draft = draft.copy(doorGap = it) },
                    modifier = Modifier.weight(1f),
                    minMm = 0f,
                    maxMm = 8f,
                    stepMm = 1f,
                )
            }
        }

        if (draft.type != FurnitureType.TABLE) {
            Spacer(Modifier.height(18.dp))
            Text("INTERIOR & DOORS", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                IntStepper(
                    label = "Shelf count",
                    value = draft.shelfCount,
                    onChange = { draft = draft.copy(shelfCount = it) },
                    modifier = Modifier.weight(1f),
                    range = 0..8,
                )
                IntStepper(
                    label = "Door count",
                    value = draft.doorCount,
                    onChange = { draft = draft.copy(doorCount = it) },
                    modifier = Modifier.weight(1f),
                    range = 0..4,
                )
            }
            Spacer(Modifier.height(10.dp))
            SoftSwitchRow(
                title = "Back panel",
                subtitle = "Thin backing closes the rear",
                checked = draft.backPanel,
                onCheckedChange = { draft = draft.copy(backPanel = it) },
            )
            if (draft.type == FurnitureType.CABINET || draft.type == FurnitureType.CUSTOM) {
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    DimStepper(
                        label = "Toe kick",
                        valueMm = draft.toeKick,
                        unit = unit,
                        onChangeMm = { draft = draft.copy(toeKick = it) },
                        modifier = Modifier.weight(1f),
                        minMm = 0f,
                        maxMm = 250f,
                        stepMm = 10f,
                    )
                    Spacer(Modifier.weight(1f))
                }
            }
        } else {
            Spacer(Modifier.height(18.dp))
            Text("FRAME", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                DimStepper(
                    label = "Apron height",
                    valueMm = draft.apronHeight,
                    unit = unit,
                    onChangeMm = { draft = draft.copy(apronHeight = it) },
                    modifier = Modifier.weight(1f),
                    minMm = 40f,
                    maxMm = 200f,
                    stepMm = 10f,
                )
                Spacer(Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Changing parameters rebuilds the panels and resets manual positions. Use undo if needed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(18.dp))
        PcButton(
            text = "Apply parameters",
            onClick = { onApply(draft) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun MaterialsSheet(
    design: ProjectDesign,
    currency: String,
    unit: UnitSystem,
    onDismiss: () -> Unit,
    onApply: (List<Material>, String, String) -> Unit,
) {
    var materials by remember { mutableStateOf(design.materials) }
    var primaryId by remember { mutableStateOf(design.primaryMaterialId) }
    var backId by remember { mutableStateOf(design.backMaterialId) }

    PcBottomSheet(onDismiss = onDismiss) {
        SheetTitle(
            title = "Materials & sheets",
            subtitle = "Set thickness, sheet size, price, and grain direction.",
        )

        Text("PRIMARY MATERIAL", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            materials.forEach { material ->
                Pill(
                    text = material.name.substringBefore(" "),
                    selected = primaryId == material.id,
                    icon = null,
                    onClick = { primaryId = material.id },
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("BACKING / REAR PANEL", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            materials.forEach { material ->
                Pill(
                    text = "${material.name} • ${material.thicknessMm.toInt()} mm",
                    selected = backId == material.id,
                    onClick = { backId = material.id },
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        materials.forEachIndexed { index, material ->
            MaterialEditorCard(
                material = material,
                currency = currency,
                unit = unit,
                onChange = { updated ->
                    materials = materials.toMutableList().also { it[index] = updated }
                },
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(6.dp))
        PcButton(
            text = "Apply materials",
            onClick = { onApply(materials, primaryId, backId) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MaterialEditorCard(
    material: Material,
    currency: String,
    unit: UnitSystem,
    onChange: (Material) -> Unit,
) {
    PcCard(contentPadding = PaddingValues(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MaterialDot(material.colorHex, 12.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                material.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (material.hasGrain) "Grain direction" else "No grain",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = material.name,
            onValueChange = { onChange(material.copy(name = it.ifBlank { material.name })) },
            label = { Text("Material name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DimStepper(
                label = "Thickness",
                valueMm = material.thicknessMm,
                unit = UnitSystem.MM,
                onChangeMm = { onChange(material.copy(thicknessMm = it)) },
                modifier = Modifier.weight(1f),
                minMm = 3f,
                maxMm = 40f,
                stepMm = 1f,
            )
            DimStepper(
                label = "Sheet width",
                valueMm = material.sheetWidthMm,
                unit = unit,
                onChangeMm = { onChange(material.copy(sheetWidthMm = it)) },
                modifier = Modifier.weight(1f),
                minMm = 600f,
                maxMm = 4000f,
                stepMm = 10f,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DimStepper(
                label = "Sheet length",
                valueMm = material.sheetHeightMm,
                unit = unit,
                onChangeMm = { onChange(material.copy(sheetHeightMm = it)) },
                modifier = Modifier.weight(1f),
                minMm = 600f,
                maxMm = 4000f,
                stepMm = 10f,
            )
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(9.dp))
        MoneyStepper(
            label = "Price per sheet",
            valueIdr = material.pricePerSheet,
            currencyCode = currency,
            onValueIdrChange = { onChange(material.copy(pricePerSheet = it)) },
            maxIdr = 2_000_000.0,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        SoftSwitchRow(
            title = "Grain direction",
            subtitle = "Preserve cut orientation during nesting",
            checked = material.hasGrain,
            onCheckedChange = { onChange(material.copy(hasGrain = it)) },
        )
    }
}

@Composable
fun PanelEditSheet(
    panel: Panel,
    design: ProjectDesign,
    unit: UnitSystem,
    onDismiss: () -> Unit,
    onSave: (Panel) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(panel.id) { mutableStateOf(panel.name) }
    var cutW by remember(panel.id) { mutableStateOf(panel.cutW) }
    var cutH by remember(panel.id) { mutableStateOf(panel.cutH) }
    var materialId by remember(panel.id) { mutableStateOf(panel.materialId) }
    var grain by remember(panel.id) { mutableStateOf(panel.grain) }
    var banding by remember(panel.id) { mutableStateOf(panel.banding) }

    PcBottomSheet(onDismiss = onDismiss) {
        SheetTitle(
            title = "Edit panel",
            subtitle = "Precise cut dimensions and edge details.",
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Panel name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DimStepper(
                label = "Width (W)",
                valueMm = cutW,
                unit = unit,
                onChangeMm = { cutW = it },
                modifier = Modifier.weight(1f),
                minMm = 20f,
                maxMm = 4000f,
                stepMm = 10f,
            )
            DimStepper(
                label = "Height (H)",
                valueMm = cutH,
                unit = unit,
                onChangeMm = { cutH = it },
                modifier = Modifier.weight(1f),
                minMm = 20f,
                maxMm = 4000f,
                stepMm = 10f,
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("MATERIAL", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            design.materials.forEach { material ->
                Pill(
                    text = material.name,
                    selected = materialId == material.id,
                    onClick = { materialId = material.id },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("GRAIN DIRECTION", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Segmented(
            options = Grain.entries.map { it.label },
            selected = Grain.entries.indexOf(grain),
            onSelect = { grain = Grain.entries[it] },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        Text("EDGE BANDING", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Pill("Top", banding.top, onClick = { banding = banding.copy(top = !banding.top) }, modifier = Modifier.weight(1f))
            Pill("Bottom", banding.bottom, onClick = { banding = banding.copy(bottom = !banding.bottom) }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Pill("Left", banding.left, onClick = { banding = banding.copy(left = !banding.left) }, modifier = Modifier.weight(1f))
            Pill("Right", banding.right, onClick = { banding = banding.copy(right = !banding.right) }, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(18.dp))
        PcButton(
            text = "Save changes",
            onClick = {
                val resized = DesignEngine.withPanelSize(panel, cutW, cutH).copy(
                    name = name.ifBlank { panel.name },
                    materialId = materialId,
                    grain = grain,
                    banding = banding,
                )
                onSave(resized)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(2.dp))
        TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(6.dp))
            Text("Delete panel", color = MaterialTheme.colorScheme.error)
        }
    }
}
