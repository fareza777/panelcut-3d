package com.panelcut3d.app.data

import kotlin.math.abs
import kotlin.math.min

/**
 * Parametric furniture generator. Produces millimetre-accurate panels for the
 * 3D assembly, the 2D elevation and the cut list.
 */
object DesignEngine {

    fun generate(params: DesignParams, primary: Material, back: Material): List<Panel> {
        val p = params.normalized()
        return when (p.type) {
            FurnitureType.TABLE -> table(p, primary)
            FurnitureType.WARDROBE -> case(p, primary, back, withDoors = true, plinth = 0f)
            FurnitureType.SHELF -> case(p, primary, back, withDoors = p.doorCount > 0, plinth = 0f)
            FurnitureType.CABINET -> case(p, primary, back, withDoors = true, plinth = p.toeKick)
            FurnitureType.CUSTOM -> case(p, primary, back, withDoors = p.doorCount > 0, plinth = p.toeKick)
        }
    }

    fun newDesign(
        params: DesignParams = DesignParams(),
        materials: List<Material> = Defaults.materials,
        primaryId: String = Defaults.PLY_ID,
        backId: String = Defaults.BACK_ID,
        costs: CostAssumptions = CostAssumptions(),
    ): ProjectDesign {
        val primary = materials.firstOrNull { it.id == primaryId } ?: materials.first()
        val back = materials.firstOrNull { it.id == backId } ?: primary
        val normalized = params.normalized()
        return ProjectDesign(
            params = normalized,
            materials = materials,
            primaryMaterialId = primary.id,
            backMaterialId = back.id,
            panels = generate(normalized, primary, back),
            costs = costs,
        )
    }

    fun regenerate(design: ProjectDesign): ProjectDesign {
        val generated = generate(design.params, design.primaryMaterial, design.backMaterial)
        return design.copy(panels = generated)
    }

    /** Rebuilds only the params and keeps an existing, manually edited layout. */
    fun withParams(design: ProjectDesign, params: DesignParams): ProjectDesign =
        regenerate(design.copy(params = params.normalized()))

    private fun DesignParams.normalized(): DesignParams = copy(
        width = width.coerceIn(300f, 4000f),
        height = height.coerceIn(300f, 3000f),
        depth = depth.coerceIn(150f, 1200f),
        shelfCount = shelfCount.coerceIn(0, 10),
        doorCount = doorCount.coerceIn(0, 6),
        toeKick = toeKick.coerceIn(0f, 250f),
        legSize = legSize.coerceIn(30f, 120f),
        apronHeight = apronHeight.coerceIn(40f, 200f),
        doorGap = doorGap.coerceIn(0f, 8f),
    )

    private fun case(
        p: DesignParams,
        primary: Material,
        back: Material,
        withDoors: Boolean,
        plinth: Float,
    ): List<Panel> {
        val t = primary.thicknessMm
        val b = back.thicknessMm
        val w = p.width
        val h = p.height
        val d = p.depth
        val innerW = (w - 2f * t).coerceAtLeast(50f)
        val base = plinth.coerceIn(0f, h * 0.3f)
        val panels = mutableListOf<Panel>()

        // Vertical gables, cut depth x height.
        panels += Panel(
            name = "Left side",
            materialId = primary.id,
            cutW = d, cutH = h,
            sx = t, sy = h, sz = d,
            x = 0f, y = 0f, z = 0f,
            grain = Grain.VERTICAL,
            banding = Banding(left = true),
        )
        panels += Panel(
            name = "Right side",
            materialId = primary.id,
            cutW = d, cutH = h,
            sx = t, sy = h, sz = d,
            x = w - t, y = 0f, z = 0f,
            grain = Grain.VERTICAL,
            banding = Banding(right = true),
        )

        // Horizontal decks.
        panels += Panel(
            name = "Bottom",
            materialId = primary.id,
            cutW = innerW, cutH = d,
            sx = innerW, sy = t, sz = d,
            x = t, y = base, z = 0f,
            grain = Grain.HORIZONTAL,
            banding = Banding(bottom = true),
        )
        panels += Panel(
            name = "Top",
            materialId = primary.id,
            cutW = innerW, cutH = d,
            sx = innerW, sy = t, sz = d,
            x = t, y = h - t, z = 0f,
            grain = Grain.HORIZONTAL,
            banding = Banding(top = true),
        )

        if (p.backPanel && b > 0f) {
            panels += Panel(
                name = "Backing",
                materialId = back.id,
                cutW = w, cutH = h,
                sx = w, sy = h, sz = b,
                x = 0f, y = 0f, z = -b,
                grain = Grain.VERTICAL,
            )
        }

        val shelfBottom = base + t
        val shelfTop = h - t
        val room = shelfTop - shelfBottom
        if (p.shelfCount > 0 && room > t * (p.shelfCount + 1)) {
            val step = room / (p.shelfCount + 1)
            for (i in 1..p.shelfCount) {
                val centerY = shelfBottom + step * i
                val y = (centerY - t / 2f).coerceIn(shelfBottom, shelfTop - t)
                panels += Panel(
                    name = "Shelf $i",
                    materialId = primary.id,
                    cutW = innerW, cutH = d,
                    sx = innerW, sy = t, sz = d,
                    x = t, y = y, z = 0f,
                    grain = Grain.HORIZONTAL,
                    banding = Banding(top = true),
                )
            }
        }

        if (withDoors && p.doorCount > 0) {
            val n = p.doorCount
            val gap = p.doorGap
            val doorY = base + gap
            val doorH = (h - doorY - gap).coerceAtLeast(50f)
            val doorW = ((w - (n + 1) * gap) / n).coerceAtLeast(30f)
            for (i in 0 until n) {
                panels += Panel(
                    name = if (n == 1) "Door" else "Door ${i + 1}",
                    materialId = primary.id,
                    cutW = doorW, cutH = doorH,
                    sx = doorW, sy = doorH, sz = t,
                    x = gap + i * (doorW + gap),
                    y = doorY,
                    z = d,
                    grain = Grain.VERTICAL,
                    banding = Banding(top = true, bottom = true, left = true, right = true),
                    isDoor = true,
                )
            }
        }

        if (plinth > 0f) {
            panels += Panel(
                name = "Toe kick",
                materialId = primary.id,
                cutW = w, cutH = plinth,
                sx = w, sy = plinth, sz = t,
                x = 0f, y = 0f, z = d - t,
                grain = Grain.HORIZONTAL,
                banding = Banding(top = true),
            )
        }

        return panels
    }

    private fun table(p: DesignParams, primary: Material): List<Panel> {
        val t = primary.thicknessMm
        val w = p.width
        val h = p.height
        val d = p.depth
        val leg = p.legSize
        val legH = (h - t).coerceAtLeast(50f)
        val inset = min(min(60f, w * 0.08f), d * 0.08f)
        val panels = mutableListOf<Panel>()

        panels += Panel(
            name = "Table top",
            materialId = primary.id,
            cutW = w, cutH = d,
            sx = w, sy = t, sz = d,
            x = 0f, y = h - t, z = 0f,
            grain = Grain.HORIZONTAL,
            banding = Banding(top = true, bottom = true, left = true, right = true),
        )

        val legXs = listOf(inset, w - inset - leg)
        val legZs = listOf(inset, d - inset - leg)
        var index = 1
        for (lx in legXs) {
            for (lz in legZs) {
                panels += Panel(
                    name = "Leg $index",
                    materialId = primary.id,
                    cutW = leg, cutH = legH,
                    sx = leg, sy = legH, sz = leg,
                    x = lx, y = 0f, z = lz,
                    grain = Grain.VERTICAL,
                )
                index++
            }
        }

        val longLen = (w - 2f * (inset + leg)).coerceAtLeast(120f)
        val shortLen = (d - 2f * (inset + leg)).coerceAtLeast(120f)
        val apronY = (h - t - p.apronHeight).coerceAtLeast(60f)
        panels += Panel(
            name = "Front apron",
            materialId = primary.id,
            cutW = longLen, cutH = p.apronHeight,
            sx = longLen, sy = p.apronHeight, sz = t,
            x = inset + leg, y = apronY, z = inset,
            grain = Grain.HORIZONTAL,
        )
        panels += Panel(
            name = "Rear apron",
            materialId = primary.id,
            cutW = longLen, cutH = p.apronHeight,
            sx = longLen, sy = p.apronHeight, sz = t,
            x = inset + leg, y = apronY, z = d - inset - t,
            grain = Grain.HORIZONTAL,
        )
        panels += Panel(
            name = "Left apron",
            materialId = primary.id,
            cutW = shortLen, cutH = p.apronHeight,
            sx = t, sy = p.apronHeight, sz = shortLen,
            x = inset, y = apronY, z = inset + leg,
            grain = Grain.HORIZONTAL,
        )
        panels += Panel(
            name = "Right apron",
            materialId = primary.id,
            cutW = shortLen, cutH = p.apronHeight,
            sx = t, sy = p.apronHeight, sz = shortLen,
            x = w - inset - t, y = apronY, z = inset + leg,
            grain = Grain.HORIZONTAL,
        )

        return panels
    }

    /** Updates flat dimensions while keeping the 3D box coherent. */
    fun withPanelSize(panel: Panel, cutW: Float, cutH: Float): Panel {
        val newW = cutW.coerceIn(20f, 4000f)
        val newH = cutH.coerceIn(20f, 4000f)
        var sx = panel.sx
        var sy = panel.sy
        var sz = panel.sz
        val epsilon = 0.6f
        if (abs(panel.cutW - panel.sx) < epsilon) sx = newW
        if (abs(panel.cutH - panel.sy) < epsilon) sy = newH
        if (abs(panel.cutW - panel.sz) < epsilon) sz = newW
        if (abs(panel.cutH - panel.sz) < epsilon) sz = newH
        if (abs(panel.cutW - panel.sy) < epsilon) sy = newW
        if (abs(panel.cutH - panel.sx) < epsilon) sx = newH
        return panel.copy(cutW = newW, cutH = newH, sx = sx, sy = sy, sz = sz)
    }

    fun defaultPanel(design: ProjectDesign): Panel {
        val primary = design.primaryMaterial
        val p = design.params
        return Panel(
            name = "New panel",
            materialId = primary.id,
            cutW = (p.width * 0.4f).coerceIn(100f, 600f),
            cutH = (p.height * 0.5f).coerceIn(100f, 800f),
            sx = t(design), sy = p.height * 0.5f, sz = (p.depth * 0.9f).coerceAtLeast(50f),
            x = p.width * 0.3f,
            y = p.height * 0.25f,
            z = p.depth * 0.3f,
            grain = Grain.NONE,
        )
    }

    private fun t(design: ProjectDesign): Float = design.primaryMaterial.thicknessMm.coerceAtLeast(1f)
}
