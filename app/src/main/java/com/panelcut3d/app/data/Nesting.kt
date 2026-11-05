package com.panelcut3d.app.data

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class NestSettings(
    val kerfMm: Float = 3f,
    val trimMm: Float = 8f,
    val respectGrain: Boolean = true,
)

data class NestPiece(
    val id: String,
    val name: String,
    val materialId: String,
    val materialName: String,
    val w: Float,
    val h: Float,
    val thickness: Float,
    val sourcePanelId: String,
    val forceRotated: Boolean = false,
)

data class PlacedPiece(
    val pieceId: String,
    val panelId: String,
    val name: String,
    val materialName: String,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val rotated: Boolean,
)

data class UnplacedPiece(
    val name: String,
    val w: Float,
    val h: Float,
    val reason: String,
)

data class NestSheet(
    val index: Int,
    val materialId: String,
    val materialName: String,
    val sheetW: Float,
    val sheetH: Float,
    val trim: Float,
    val placements: List<PlacedPiece>,
) {
    val usedArea: Float get() = placements.sumOf { it.w.toDouble() * it.h.toDouble() }.toFloat()
    val sheetArea: Float get() = sheetW * sheetH
    val wastePercent: Float get() = if (sheetArea <= 0f) 0f else ((sheetArea - usedArea) / sheetArea * 100f)
}

data class MaterialUsage(
    val materialId: String,
    val name: String,
    val sheets: Int,
    val sheetW: Float,
    val sheetH: Float,
    val pricePerSheet: Double,
    val areaM2: Double,
    val wastePercent: Float,
) {
    val cost: Double get() = sheets * pricePerSheet
}

data class NestResult(
    val settings: NestSettings,
    val sheets: List<NestSheet>,
    val unplaced: List<UnplacedPiece>,
    val totalPieceAreaM2: Double,
    val totalSheetAreaM2: Double,
    val wastePercent: Double,
    val materialUsages: List<MaterialUsage>,
    val totalMaterialCost: Double,
) {
    val sheetCount: Int get() = sheets.size
    val isEmpty: Boolean get() = sheets.isEmpty()
    val utilizedAreaM2: Double get() = totalPieceAreaM2
    val materialLabel: String
        get() = if (materialUsages.size == 1) materialUsages.first().name else "${materialUsages.size} material"
}

/**
 * Guillotine-friendly MaxRects packer. Tries several heuristics and sort orders
 * per material, then keeps the layout with the fewest sheets and least waste.
 */
object NestingEngine {

    private const val HEURISTIC_BSSF = 0
    private const val HEURISTIC_BAF = 1
    private const val HEURISTIC_BL = 2
    private val heuristics = intArrayOf(HEURISTIC_BSSF, HEURISTIC_BAF, HEURISTIC_BL)

    fun optimize(design: ProjectDesign, settings: NestSettings): NestResult {
        val kerf = settings.kerfMm.coerceIn(0f, 20f)
        val trim = settings.trimMm.coerceIn(0f, 60f)
        val materials = design.materials.associateBy { it.id }
        val sheets = mutableListOf<NestSheet>()
        val usages = mutableListOf<MaterialUsage>()
        val unplacedAll = mutableListOf<UnplacedPiece>()
        var pieceArea = 0.0
        var sheetArea = 0.0

        val groups = design.panels
            .groupBy { it.materialId }
            .toSortedMap(compareBy { id -> materials[id]?.name ?: id })

        for ((materialId, panels) in groups) {
            val material = materials[materialId] ?: design.materials.first()
            val instances = panels.flatMap { panel ->
                List(panel.qty.coerceAtLeast(1)) {
                    buildPiece(panel, material, settings.respectGrain)
                }
            }
            if (instances.isEmpty()) continue
            val usableW = material.sheetWidthMm - 2f * trim
            val usableH = material.sheetHeightMm - 2f * trim
            if (usableW <= 50f || usableH <= 50f) {
                instances.forEach { unplacedAll += UnplacedPiece(it.name, it.w, it.h, "Invalid sheet size") }
                continue
            }

            val best = bestPack(instances, usableW, usableH, kerf)
            best.bins.forEach { bin ->
                if (bin.placements.isEmpty()) return@forEach
                val placed = bin.placements.map { p ->
                    PlacedPiece(
                        pieceId = p.piece.id,
                        panelId = p.piece.sourcePanelId,
                        name = p.piece.name,
                        materialName = material.name,
                        x = p.x + trim,
                        y = p.y + trim,
                        w = p.w,
                        h = p.h,
                        rotated = p.rotated,
                    )
                }
                val sheet = NestSheet(
                    index = sheets.size + 1,
                    materialId = material.id,
                    materialName = material.name,
                    sheetW = material.sheetWidthMm,
                    sheetH = material.sheetHeightMm,
                    trim = trim,
                    placements = placed,
                )
                sheets += sheet
                sheetArea += sheet.sheetArea.toDouble() / 1_000_000.0
            }
            usages += MaterialUsage(
                materialId = material.id,
                name = material.name,
                sheets = best.bins.count { it.placements.isNotEmpty() },
                sheetW = material.sheetWidthMm,
                sheetH = material.sheetHeightMm,
                pricePerSheet = material.pricePerSheet,
                areaM2 = instances.sumOf { it.w.toDouble() * it.h.toDouble() } / 1_000_000.0,
                wastePercent = best.wastePercent(usableW, usableH, material),
            )
            unplacedAll += best.unplaced.map { piece ->
                val requiredW = if (piece.forceRotated) piece.h else piece.w
                val requiredH = if (piece.forceRotated) piece.w else piece.h
                val reason = when {
                    requiredW > usableW || requiredH > usableH -> "Exceeds sheet area"
                    piece.forceRotated -> "Grain direction does not match the sheet"
                    else -> "Does not fit on the remaining sheet"
                }
                UnplacedPiece(piece.name, piece.w, piece.h, reason)
            }
            pieceArea += instances.sumOf { it.w.toDouble() * it.h.toDouble() } / 1_000_000.0
        }

        val waste = if (sheetArea > 0.0) ((sheetArea - pieceArea) / sheetArea * 100.0).coerceIn(0.0, 100.0) else 0.0
        return NestResult(
            settings = settings,
            sheets = sheets,
            unplaced = unplacedAll,
            totalPieceAreaM2 = pieceArea,
            totalSheetAreaM2 = sheetArea,
            wastePercent = waste,
            materialUsages = usages,
            totalMaterialCost = usages.sumOf { it.cost },
        )
    }

    private fun buildPiece(panel: Panel, material: Material, respectGrain: Boolean): NestPiece {
        val forceRotated = respectGrain && material.hasGrain && panel.grain == Grain.HORIZONTAL
        return NestPiece(
            id = "n-${panel.id}-${System.nanoTime()}",
            name = panel.name,
            materialId = material.id,
            materialName = material.name,
            w = panel.cutW,
            h = panel.cutH,
            thickness = material.thicknessMm,
            sourcePanelId = panel.id,
            forceRotated = forceRotated,
        )
    }

    private data class Span(val x: Float, val y: Float, val w: Float, val h: Float) {
        val right get() = x + w
        val bottom get() = y + h
        fun intersects(o: Span): Boolean =
            o.x < right - 0.001f && o.right > x + 0.001f && o.y < bottom - 0.001f && o.bottom > y + 0.001f

        fun contains(o: Span): Boolean =
            o.x >= x - 0.001f && o.right <= right + 0.001f && o.y >= y - 0.001f && o.bottom <= bottom + 0.001f
    }

    private class Placement(
        val piece: NestPiece,
        val x: Float,
        val y: Float,
        val w: Float,
        val h: Float,
        val rotated: Boolean,
    )

    private class Bin(val width: Float, val height: Float, private val kerf: Float) {
        var free: MutableList<Span> = mutableListOf(Span(0f, 0f, width, height))
        val placements = mutableListOf<Placement>()
        var usedArea = 0.0

        fun findBest(piece: NestPiece, heuristic: Int): Candidate? {
            var best: Candidate? = null
            val rotations = buildList {
                if (piece.forceRotated) {
                    add(true)
                } else {
                    add(false)
                    if (!piece.forceRotated && piece.w != piece.h) add(true)
                }
            }
            for (fr in free) {
                for (rotated in rotations) {
                    val pw = if (rotated) piece.h else piece.w
                    val ph = if (rotated) piece.w else piece.h
                    if (pw > fr.w + 0.001f || ph > fr.h + 0.001f) continue
                    val leftoverW = fr.w - pw
                    val leftoverH = fr.h - ph
                    val shortSide = min(leftoverW, leftoverH)
                    val longSide = max(leftoverW, leftoverH)
                    val score = when (heuristic) {
                        HEURISTIC_BSSF -> shortSide * 1000f + longSide
                        HEURISTIC_BAF -> fr.w * fr.h - pw * ph
                        else -> (fr.y * 10_000f + fr.x * 10f + shortSide * 0.01f)
                    }
                    if (best == null || score < best!!.score) {
                        best = Candidate(fr.x, fr.y, pw, ph, rotated, score)
                    }
                }
            }
            return best
        }

        fun place(piece: NestPiece, candidate: Candidate) {
            placements += Placement(piece, candidate.x, candidate.y, candidate.w, candidate.h, candidate.rotated)
            usedArea += candidate.w.toDouble() * candidate.h.toDouble()
            occupy(candidate.x, candidate.y, candidate.w + kerf, candidate.h + kerf)
        }

        private fun occupy(x: Float, y: Float, w: Float, h: Float) {
            val used = Span(x, y, min(w, width - x), min(h, height - y))
            val next = mutableListOf<Span>()
            for (fr in free) {
                if (!fr.intersects(used)) {
                    next += fr
                    continue
                }
                if (used.x > fr.x) next += Span(fr.x, fr.y, used.x - fr.x, fr.h)
                if (used.right < fr.right) next += Span(used.right, fr.y, fr.right - used.right, fr.h)
                if (used.y > fr.y) next += Span(fr.x, fr.y, fr.w, used.y - fr.y)
                if (used.bottom < fr.bottom) next += Span(fr.x, used.bottom, fr.w, fr.bottom - used.bottom)
            }
            free = prune(next)
        }

        private fun prune(rects: List<Span>): MutableList<Span> {
            val cleaned = rects.filter { it.w > 0.5f && it.h > 0.5f }.toMutableList()
            val result = mutableListOf<Span>()
            for (i in cleaned.indices.reversed()) {
                var contained = false
                for (j in cleaned.indices) {
                    if (i == j) continue
                    if (cleaned[j].contains(cleaned[i])) {
                        contained = true
                        break
                    }
                }
                if (!contained) result += cleaned[i]
            }
            return result
        }
    }

    private data class Candidate(
        val x: Float,
        val y: Float,
        val w: Float,
        val h: Float,
        val rotated: Boolean,
        val score: Float,
    )

    private class PackResult(
        val bins: List<Bin>,
        val unplaced: List<NestPiece>,
        val areaW: Float,
        val areaH: Float,
    ) {
        fun wastePercent(w: Float, h: Float, material: Material): Float {
            val used = bins.sumOf { it.usedArea }
            val total = bins.count { it.placements.isNotEmpty() } * w.toDouble() * h.toDouble()
            return if (total <= 0.0) 0f else ((total - used) / total * 100.0).toFloat().coerceIn(0f, 100f)
        }
    }

    private fun bestPack(
        pieces: List<NestPiece>,
        binW: Float,
        binH: Float,
        kerf: Float,
    ): PackResult {
        var best: PackResult? = null
        val sorters = listOf<(NestPiece) -> Float>(
            { it.w * it.h },
            { max(it.w, it.h) },
            { it.h },
        )
        for (heuristic in heuristics) {
            for (sort in sorters) {
                val result = pack(pieces.sortedByDescending(sort), binW, binH, kerf, heuristic)
                if (best == null || better(result, best!!)) best = result
            }
        }
        return best ?: PackResult(emptyList(), pieces, binW, binH)
    }

    private fun better(a: PackResult, b: PackResult): Boolean {
        if (a.unplaced.size != b.unplaced.size) return a.unplaced.size < b.unplaced.size
        val sheetsA = a.bins.count { it.placements.isNotEmpty() }
        val sheetsB = b.bins.count { it.placements.isNotEmpty() }
        if (sheetsA != sheetsB) return sheetsA < sheetsB
        return a.bins.sumOf { it.usedArea } > b.bins.sumOf { it.usedArea }
    }

    private fun pack(
        pieces: List<NestPiece>,
        binW: Float,
        binH: Float,
        kerf: Float,
        heuristic: Int,
    ): PackResult {
        val bins = mutableListOf<Bin>()
        val unplaced = mutableListOf<NestPiece>()
        for (piece in pieces) {
            var bestBin: Bin? = null
            var bestCandidate: Candidate? = null
            for (bin in bins) {
                val candidate = bin.findBest(piece, heuristic) ?: continue
                if (bestCandidate == null || candidate.score < bestCandidate!!.score) {
                    bestCandidate = candidate
                    bestBin = bin
                }
            }
            if (bestBin != null && bestCandidate != null) {
                bestBin.place(piece, bestCandidate)
            } else {
                val fresh = Bin(binW, binH, kerf)
                val candidate = fresh.findBest(piece, heuristic)
                if (candidate == null) {
                    unplaced += piece
                } else {
                    fresh.place(piece, candidate)
                    bins += fresh
                }
            }
        }
        return PackResult(bins, unplaced, binW, binH)
    }

    /** Approximate sheet need when a full nesting pass is not required. */
    fun estimateSheets(design: ProjectDesign): Int {
        var sheets = 0
        design.panels.groupBy { it.materialId }.forEach { (id, panels) ->
            val material = design.material(id)
            val sheetArea = material.sheetWidthMm * material.sheetHeightMm
            if (sheetArea <= 0f) return@forEach
            val area = panels.sumOf { it.areaM2 * it.qty }
            sheets += ceil(area * 1.18 / (sheetArea / 1_000_000.0)).toInt().coerceAtLeast(1)
        }
        return sheets
    }

    fun utilizationPercent(result: NestResult): Float {
        val area = result.totalSheetAreaM2
        return if (area <= 0.0) 0f else (result.totalPieceAreaM2 / area * 100.0).toFloat()
    }

    fun totalWasteArea(result: NestResult): Double =
        (result.totalSheetAreaM2 - result.totalPieceAreaM2).coerceAtLeast(0.0)

    fun largestPanel(design: ProjectDesign): Panel? = design.panels.maxByOrNull { it.cutW * it.cutH }

    fun sheetLabel(width: Float, height: Float): String = "${width.toInt()} × ${height.toInt()} mm"
}
