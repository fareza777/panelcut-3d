package com.panelcut3d.app.data

import kotlin.math.ceil

data class MaterialCostLine(
    val name: String,
    val sheets: Int,
    val pricePerSheet: Double,
    val subtotal: Double,
)

data class CostBreakdown(
    val materialLines: List<MaterialCostLine>,
    val materialTotal: Double,
    val sheetCount: Int,
    val edgeBandingMeters: Double,
    val edgeBandingRate: Double,
    val edgeBandingTotal: Double,
    val doorCount: Int,
    val hardwarePerDoor: Double,
    val hardwareTotal: Double,
    val laborHours: Float,
    val laborRate: Double,
    val laborTotal: Double,
    val subtotal: Double,
    val marginPercent: Float,
    val marginAmount: Double,
    val taxPercent: Float,
    val taxAmount: Double,
    val total: Double,
    val areaM2: Double,
    val wastePercent: Double,
) {
    val markupValue: Double get() = marginAmount
    val pricePerM2: Double get() = if (areaM2 <= 0.0) 0.0 else materialTotal / areaM2
    val directCost: Double get() = subtotal
}

object CostEngine {

    fun calculate(design: ProjectDesign, nesting: NestResult?): CostBreakdown {
        val assumptions = design.costs
        val materials = design.materials.associateBy { it.id }

        val lines = mutableListOf<MaterialCostLine>()
        var materialTotal = 0.0
        var sheets = 0
        if (nesting != null && nesting.materialUsages.isNotEmpty()) {
            for (usage in nesting.materialUsages) {
                val subtotal = usage.sheets * usage.pricePerSheet
                materialTotal += subtotal
                sheets += usage.sheets
                lines += MaterialCostLine(usage.name, usage.sheets, usage.pricePerSheet, subtotal)
            }
        } else {
            design.panels.groupBy { it.materialId }.forEach { (id, panels) ->
                val material = materials[id] ?: design.materials.first()
                val sheetArea = material.sheetWidthMm * material.sheetHeightMm / 1_000_000.0
                val area = panels.sumOf { it.areaM2 * it.qty }
                val count = if (sheetArea <= 0.0) 0 else ceil(area * 1.18 / sheetArea).toInt().coerceAtLeast(1)
                val subtotal = count * material.pricePerSheet
                materialTotal += subtotal
                sheets += count
                lines += MaterialCostLine(material.name, count, material.pricePerSheet, subtotal)
            }
        }

        val bandingMeters = design.bandingMeters()
        val bandingTotal = bandingMeters * assumptions.edgeBandingPricePerMeter
        val doors = design.panels.count { it.isDoor }
        val hardwareTotal = doors * assumptions.hardwarePerDoor
        val laborTotal = assumptions.laborHours * assumptions.laborRatePerHour

        val subtotal = materialTotal + bandingTotal + hardwareTotal + laborTotal
        val marginAmount = subtotal * assumptions.marginPercent / 100.0
        val taxable = subtotal + marginAmount
        val taxAmount = taxable * assumptions.taxPercent / 100.0
        val total = taxable + taxAmount

        return CostBreakdown(
            materialLines = lines,
            materialTotal = materialTotal,
            sheetCount = sheets,
            edgeBandingMeters = bandingMeters,
            edgeBandingRate = assumptions.edgeBandingPricePerMeter,
            edgeBandingTotal = bandingTotal,
            doorCount = doors,
            hardwarePerDoor = assumptions.hardwarePerDoor,
            hardwareTotal = hardwareTotal,
            laborHours = assumptions.laborHours,
            laborRate = assumptions.laborRatePerHour,
            laborTotal = laborTotal,
            subtotal = subtotal,
            marginPercent = assumptions.marginPercent,
            marginAmount = marginAmount,
            taxPercent = assumptions.taxPercent,
            taxAmount = taxAmount,
            total = total,
            areaM2 = design.totalAreaM2(),
            wastePercent = nesting?.wastePercent ?: 0.0,
        )
    }
}
