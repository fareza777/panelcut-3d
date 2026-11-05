package com.panelcut3d.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panelcut3d.app.data.CostAssumptions
import com.panelcut3d.app.data.CostEngine
import com.panelcut3d.app.data.Fmt
import com.panelcut3d.app.data.ProjectDesign
import com.panelcut3d.app.ui.components.AnimatedProgress
import com.panelcut3d.app.ui.components.DividerSoft
import com.panelcut3d.app.ui.components.GradientHero
import com.panelcut3d.app.ui.components.MoneyStepper
import com.panelcut3d.app.ui.components.NumberStepper
import com.panelcut3d.app.ui.components.PcButton
import com.panelcut3d.app.ui.components.PcCard
import com.panelcut3d.app.ui.components.PlanStat
import com.panelcut3d.app.ui.components.SectionHeader

@Composable
fun CostTab(
    state: EditorState,
    onUpdateCosts: ((CostAssumptions) -> CostAssumptions) -> Unit,
    onOpenExport: () -> Unit,
) {
    val design = state.design
    val currency = state.settings.currencyCode
    val cost = remember(design, state.nesting) { CostEngine.calculate(design, state.nesting) }
    val assumptions = design.costs

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            GradientHero {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "RECOMMENDED SELLING PRICE",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.7.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            Fmt.money(cost.total, currency),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Margin ${Fmt.number(cost.marginPercent.toDouble(), 0)}%  •  tax ${Fmt.number(cost.taxPercent.toDouble(), 0)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                DividerSoft()
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlanStat("MATERIAL", Fmt.money(cost.materialTotal, currency), Modifier.weight(1.1f))
                    PlanStat("LABOR", Fmt.money(cost.laborTotal + cost.hardwareTotal, currency), Modifier.weight(1.1f))
                    PlanStat("WASTE", Fmt.percent(cost.wastePercent, 1), Modifier.weight(1f))
                }
            }
        }

        item {
            PcCard(contentPadding = PaddingValues(16.dp)) {
                Text("COST BREAKDOWN", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                CostLine("Material (${cost.sheetCount} sheets)", cost.materialTotal, cost.subtotal, currency)
                CostLine("Edge banding ${Fmt.number(cost.edgeBandingMeters, 2)} m", cost.edgeBandingTotal, cost.subtotal, currency)
                CostLine("Hardware (${cost.doorCount} doors)", cost.hardwareTotal, cost.subtotal, currency)
                CostLine("Labor (${Fmt.number(cost.laborHours.toDouble(), 1)} h)", cost.laborTotal, cost.subtotal, currency)
                Spacer(Modifier.height(8.dp))
                DividerSoft()
                Spacer(Modifier.height(10.dp))
                CostTotalRow("Subtotal", cost.subtotal, currency)
                CostTotalRow("Margin ${Fmt.number(cost.marginPercent.toDouble(), 0)}%", cost.marginAmount, currency)
                if (cost.taxPercent > 0f) {
                    CostTotalRow("Tax ${Fmt.number(cost.taxPercent.toDouble(), 0)}%", cost.taxAmount, currency)
                }
                Spacer(Modifier.height(8.dp))
                DividerSoft()
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("TOTAL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text(
                        Fmt.money(cost.total, currency),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        item {
            PcCard(contentPadding = PaddingValues(16.dp)) {
                Text("COST ASSUMPTIONS", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    NumberStepper(
                        label = "Labor hours",
                        value = assumptions.laborHours,
                        suffix = "h",
                        onChange = { value -> onUpdateCosts { it.copy(laborHours = value) } },
                        modifier = Modifier.weight(1f),
                        min = 0f,
                        max = 80f,
                        step = 0.5f,
                        decimals = 1,
                    )
                    MoneyStepper(
                        label = "Rate / hour",
                        valueIdr = assumptions.laborRatePerHour,
                        currencyCode = currency,
                        onValueIdrChange = { value -> onUpdateCosts { it.copy(laborRatePerHour = value) } },
                        modifier = Modifier.weight(1f),
                        maxIdr = 2_000_000.0,
                    )
                }
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    MoneyStepper(
                        label = "Per door",
                        valueIdr = assumptions.hardwarePerDoor,
                        currencyCode = currency,
                        onValueIdrChange = { value -> onUpdateCosts { it.copy(hardwarePerDoor = value) } },
                        modifier = Modifier.weight(1f),
                        maxIdr = 2_000_000.0,
                    )
                    MoneyStepper(
                        label = "Edge / m",
                        valueIdr = assumptions.edgeBandingPricePerMeter,
                        currencyCode = currency,
                        onValueIdrChange = { value -> onUpdateCosts { it.copy(edgeBandingPricePerMeter = value) } },
                        modifier = Modifier.weight(1f),
                        maxIdr = 500_000.0,
                    )
                }
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    NumberStepper(
                        label = "Margin",
                        value = assumptions.marginPercent,
                        suffix = "%",
                        onChange = { value -> onUpdateCosts { it.copy(marginPercent = value) } },
                        modifier = Modifier.weight(1f),
                        min = 0f,
                        max = 200f,
                        step = 1f,
                    )
                    NumberStepper(
                        label = "Tax",
                        value = assumptions.taxPercent,
                        suffix = "%",
                        onChange = { value -> onUpdateCosts { it.copy(taxPercent = value) } },
                        modifier = Modifier.weight(1f),
                        min = 0f,
                        max = 50f,
                        step = 1f,
                    )
                }
            }
        }

        item {
            PcCard(contentPadding = PaddingValues(16.dp)) {
                Text("MATERIAL", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                cost.materialLines.forEach { line ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(line.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${line.sheets} sheets × ${Fmt.money(line.pricePerSheet, currency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            Fmt.money(line.subtotal, currency),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Material area ${Fmt.area(cost.areaM2)} • ${Fmt.money(cost.pricePerM2, currency)}/m²",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            PcButton(
                text = "Export quote PDF",
                onClick = onOpenExport,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Description,
            )
        }

        item {
            Text(
                "Prices are estimates and can be adjusted for your workshop. All calculations happen on-device.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun CostLine(label: String, amount: Double, subtotal: Double, currency: String) {
    Column(Modifier.padding(vertical = 5.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(Fmt.money(amount, currency), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(5.dp))
        AnimatedProgress(
            fraction = if (subtotal <= 0.0) 0f else (amount / subtotal).toFloat(),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun CostTotalRow(label: String, amount: Double, currency: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(Fmt.money(amount, currency), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
