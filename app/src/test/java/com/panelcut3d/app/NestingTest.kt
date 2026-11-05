package com.panelcut3d.app

import com.panelcut3d.app.data.CostEngine
import com.panelcut3d.app.data.DesignEngine
import com.panelcut3d.app.data.FurnitureType
import com.panelcut3d.app.data.Grain
import com.panelcut3d.app.data.NestSettings
import com.panelcut3d.app.data.NestingEngine
import com.panelcut3d.app.data.Panel
import com.panelcut3d.app.data.ProjectDesign
import com.panelcut3d.app.data.Defaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class NestingTest {

    private val settings = NestSettings(kerfMm = 3f, trimMm = 8f, respectGrain = true)

    @Test
    fun wardrobePacksAllPanelsInsideSheets() {
        val design = DesignEngine.newDesign(Defaults.paramsFor(FurnitureType.WARDROBE))
        val result = NestingEngine.optimize(design, settings)

        assertTrue("expected at least one sheet", result.sheets.isNotEmpty())
        assertTrue("all pieces should fit", result.unplaced.isEmpty())
        assertEquals(design.panels.size, result.sheets.sumOf { it.placements.size })
        assertTrue("waste should be plausible", result.wastePercent in 0.0..80.0)
    }

    @Test
    fun placementsRespectTrimAndKerf() {
        val design = DesignEngine.newDesign(Defaults.paramsFor(FurnitureType.CABINET))
        val result = NestingEngine.optimize(design, settings)
        val kerf = settings.kerfMm

        for (sheet in result.sheets) {
            for (p in sheet.placements) {
                assertTrue(p.x >= sheet.trim - 0.02f)
                assertTrue(p.y >= sheet.trim - 0.02f)
                assertTrue(p.x + p.w <= sheet.sheetW - sheet.trim + 0.02f)
                assertTrue(p.y + p.h <= sheet.sheetH - sheet.trim + 0.02f)
            }
            for (i in sheet.placements.indices) {
                for (j in i + 1 until sheet.placements.size) {
                    val a = sheet.placements[i]
                    val b = sheet.placements[j]
                    val apartX = a.x + a.w + kerf <= b.x + 0.05f || b.x + b.w + kerf <= a.x + 0.05f
                    val apartY = a.y + a.h + kerf <= b.y + 0.05f || b.y + b.h + kerf <= a.y + 0.05f
                    assertTrue("pieces ${a.name} and ${b.name} overlap or ignore kerf", apartX || apartY)
                }
            }
        }
    }

    @Test
    fun verticalGrainPiecesStayUnrotated() {
        val design = DesignEngine.newDesign(Defaults.paramsFor(FurnitureType.WARDROBE))
        val result = NestingEngine.optimize(design, settings)
        val panels = design.panels.associateBy { it.id }
        for (sheet in result.sheets) {
            for (placement in sheet.placements) {
                val panel = panels[placement.panelId] ?: continue
                if (panel.grain == Grain.VERTICAL && design.material(panel.materialId).hasGrain) {
                    assertFalse("grain rotated on ${panel.name}", placement.rotated)
                    assertEquals(panel.cutW, placement.w, 0.01f)
                    assertEquals(panel.cutH, placement.h, 0.01f)
                }
            }
        }
    }

    @Test
    fun oversizedPanelIsReported() {
        val base = DesignEngine.newDesign(Defaults.paramsFor(FurnitureType.SHELF))
        val giant = Panel(
            name = "Panel raksasa",
            materialId = base.primaryMaterialId,
            cutW = 3000f,
            cutH = 2900f,
            sx = 18f,
            sy = 2900f,
            sz = 3000f,
        )
        val design = base.copy(panels = base.panels + giant)
        val result = NestingEngine.optimize(design, settings)
        assertTrue(result.unplaced.isNotEmpty())
        assertTrue(result.unplaced.first().reason.isNotBlank())
    }

    @Test
    fun costsFollowSheetUsage() {
        val design = DesignEngine.newDesign(Defaults.paramsFor(FurnitureType.TABLE))
        val result = NestingEngine.optimize(design, settings)
        val cost = CostEngine.calculate(design, result)
        assertTrue(cost.sheetCount > 0)
        assertTrue(cost.materialTotal > 0.0)
        assertTrue(cost.total > cost.subtotal)
        assertNotNull(cost.materialLines.firstOrNull())
        assertTrue(cost.edgeBandingMeters > 0.0)
    }

    @Test
    fun designDimensionsAreMillimetreAccurate() {
        val design = DesignEngine.newDesign(Defaults.paramsFor(FurnitureType.WARDROBE))
        val side = design.panels.first { it.name == "Left side" }
        assertEquals(600f, side.cutW, 0.01f)
        assertEquals(2000f, side.cutH, 0.01f)
        assertEquals(18f, side.sx, 0.01f)
        assertEquals(1200f, design.panels.first { it.name == "Top" }.cutW + 36f, 0.01f)
        assertEquals(2, design.panels.count { it.isDoor })
        assertTrue(design.panels.all { it.cutW > 0f && it.cutH > 0f && it.sx > 0f && it.sy > 0f && it.sz > 0f })
        assertTrue(design.panels.none { abs(it.x) > design.params.width + 50f })
    }

    @Test
    fun mdfPiecesMayRotate() {
        val design = DesignEngine.newDesign(Defaults.paramsFor(FurnitureType.SHELF))
        val mdf = design.copy(
            primaryMaterialId = Defaults.MDF_ID,
            panels = listOf(
                Panel(
                    name = "Rak MDF",
                    materialId = Defaults.MDF_ID,
                    cutW = 900f,
                    cutH = 250f,
                    sx = 900f,
                    sy = 18f,
                    sz = 250f,
                    grain = Grain.HORIZONTAL,
                ),
            ),
        )
        val result = NestingEngine.optimize(mdf, settings)
        assertTrue(result.sheets.isNotEmpty())
        assertTrue(result.sheets.first().placements.first().w == 900f || result.sheets.first().placements.first().w == 250f)
    }

    private fun validate(design: ProjectDesign): Boolean {
        val result = NestingEngine.optimize(design, settings)
        return result.unplaced.isEmpty()
    }

    @Test
    fun everyPresetProducesValidLayouts() {
        listOf(FurnitureType.WARDROBE, FurnitureType.TABLE, FurnitureType.SHELF, FurnitureType.CABINET)
            .forEach { type ->
                val design = DesignEngine.newDesign(Defaults.paramsFor(type))
                assertTrue("layout invalid for $type", validate(design))
            }
    }
}
