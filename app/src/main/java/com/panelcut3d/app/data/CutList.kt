package com.panelcut3d.app.data

data class CutRow(
    val key: String,
    val name: String,
    val material: Material,
    val cutW: Float,
    val cutH: Float,
    val qty: Int,
    val grain: Grain,
    val banding: Banding,
    val isDoor: Boolean,
    val panelIds: List<String> = emptyList(),
) {
    val areaM2: Double get() = cutW.toDouble() * cutH.toDouble() * qty / 1_000_000.0
    val bandingMeters: Double get() = banding.lengthMm(cutW, cutH).toDouble() * qty / 1000.0
}

fun ProjectDesign.cutRows(): List<CutRow> {
    val grouped = panels.groupBy { panel ->
        listOf(
            panel.name,
            panel.materialId,
            panel.cutW.toInt().toString(),
            panel.cutH.toInt().toString(),
            panel.grain.name,
            panel.banding.top,
            panel.banding.bottom,
            panel.banding.left,
            panel.banding.right,
        ).joinToString("|")
    }
    return grouped.map { (key, items) ->
        val first = items.first()
        CutRow(
            key = key,
            name = first.name,
            material = material(first.materialId),
            cutW = first.cutW,
            cutH = first.cutH,
            qty = items.sumOf { it.qty },
            grain = first.grain,
            banding = first.banding,
            isDoor = first.isDoor,
            panelIds = items.map { it.id }.distinct(),
        )
    }.sortedWith(compareBy({ it.material.name }, { -it.cutH }, { -it.cutW }))
}

fun ProjectDesign.cutRowsByMaterial(): List<Pair<Material, List<CutRow>>> =
    cutRows().groupBy { it.material.id }
        .map { (_, rows) -> rows.first().material to rows }
        .sortedBy { it.first.name }

fun ProjectDesign.effectiveSheetCount(nesting: NestResult?): Int =
    nesting?.sheetCount ?: NestingEngine.estimateSheets(this)
