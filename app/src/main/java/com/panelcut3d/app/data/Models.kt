package com.panelcut3d.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

/** Shared JSON configuration used by Room persistence and the 3D bridge. */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
}

fun newPanelId(): String = "p-" + UUID.randomUUID().toString().take(8)

@Serializable
enum class FurnitureType(val label: String, val tagline: String) {
    WARDROBE("Wardrobe", "Clothes & storage"),
    TABLE("Table", "Work & dining"),
    SHELF("Shelf", "Books & display"),
    CABINET("Cabinet", "Kitchen & base"),
    CUSTOM("Custom", "Freeform design"),
}

@Serializable
enum class UnitSystem(val label: String, val suffix: String) {
    MM("Millimeter", "mm"),
    CM("Centimeter", "cm");

    fun fromMm(mm: Float): Float = if (this == CM) mm / 10f else mm
    fun toMm(value: Float): Float = if (this == CM) value * 10f else value
}

@Serializable
enum class ThemeMode(val label: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System"),
}

@Serializable
enum class Grain(val label: String) {
    NONE("Free"),
    VERTICAL("Vertical (H)"),
    HORIZONTAL("Horizontal (W)"),
}

@Serializable
data class Banding(
    val top: Boolean = false,
    val bottom: Boolean = false,
    val left: Boolean = false,
    val right: Boolean = false,
) {
    val count: Int get() = (if (top) 1 else 0) + (if (bottom) 1 else 0) +
        (if (left) 1 else 0) + (if (right) 1 else 0)

    val any: Boolean get() = count > 0

    fun lengthMm(cutW: Float, cutH: Float): Float {
        var length = 0f
        if (top) length += cutW
        if (bottom) length += cutW
        if (left) length += cutH
        if (right) length += cutH
        return length
    }

    fun shortLabel(): String = buildString {
        if (top) append("T ")
        if (bottom) append("B ")
        if (left) append("L ")
        if (right) append("R ")
    }.trim().ifEmpty { "—" }
}

@Serializable
data class Material(
    val id: String,
    val name: String,
    val thicknessMm: Float,
    val sheetWidthMm: Float = 1220f,
    val sheetHeightMm: Float = 2440f,
    val pricePerSheet: Double = 0.0,
    val hasGrain: Boolean = true,
    val colorHex: String = "#C08A5A",
    val kind: String = "PANEL",
)

@Serializable
data class Panel(
    val id: String = newPanelId(),
    val name: String,
    val materialId: String,
    /** Dimension on the flat sheet, horizontal axis. */
    val cutW: Float,
    /** Dimension on the flat sheet, vertical axis. */
    val cutH: Float,
    /** Rendered size on the X axis (mm). */
    val sx: Float,
    /** Rendered size on the Y axis (mm). */
    val sy: Float,
    /** Rendered size on the Z axis (mm). */
    val sz: Float,
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val grain: Grain = Grain.NONE,
    val banding: Banding = Banding(),
    val qty: Int = 1,
    val isDoor: Boolean = false,
) {
    val areaM2: Double get() = cutW.toDouble() * cutH.toDouble() / 1_000_000.0
}

@Serializable
data class DesignParams(
    val type: FurnitureType = FurnitureType.WARDROBE,
    val width: Float = 1200f,
    val height: Float = 2000f,
    val depth: Float = 600f,
    val shelfCount: Int = 3,
    val doorCount: Int = 2,
    val backPanel: Boolean = true,
    val toeKick: Float = 0f,
    val doorGap: Float = 3f,
    val legSize: Float = 60f,
    val apronHeight: Float = 80f,
)

@Serializable
data class CostAssumptions(
    val laborHours: Float = 4f,
    val laborRatePerHour: Double = 40_000.0,
    val hardwarePerDoor: Double = 28_000.0,
    val edgeBandingPricePerMeter: Double = 12_000.0,
    val marginPercent: Float = 25f,
    val taxPercent: Float = 0f,
)

@Serializable
data class QuoteDetails(
    val number: String = "",
    val customer: String = "",
    val validDays: Int = 14,
    val discountPercent: Float = 0f,
    val depositPercent: Float = 30f,
    val notes: String = "",
)

@Serializable
data class ProjectDesign(
    val params: DesignParams = DesignParams(),
    val materials: List<Material> = Defaults.materials,
    val primaryMaterialId: String = Defaults.PLY_ID,
    val backMaterialId: String = Defaults.BACK_ID,
    val panels: List<Panel> = emptyList(),
    val costs: CostAssumptions = CostAssumptions(),
    val notes: String = "",
    val quote: QuoteDetails = QuoteDetails(),
    val cutDone: List<String> = emptyList(),
) {
    fun material(id: String): Material =
        materials.firstOrNull { it.id == id }
            ?: materials.firstOrNull()
            ?: Defaults.materials.first()

    val primaryMaterial: Material get() = material(primaryMaterialId)
    val backMaterial: Material get() = material(backMaterialId)

    fun totalAreaM2(): Double = panels.sumOf { it.areaM2 * it.qty }

    fun doorCount(): Int = panels.count { it.isDoor }

    fun bandingMeters(): Double = panels.sumOf { panel ->
        panel.banding.lengthMm(panel.cutW, panel.cutH).toDouble() * panel.qty / 1000.0
    }
}

data class FurnitureProject(
    val id: Long = 0L,
    val name: String,
    val design: ProjectDesign,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

object Defaults {
    const val PLY_ID = "ply18"
    const val MDF_ID = "mdf18"
    const val BACK_ID = "back4"

    val materials: List<Material> = listOf(
        Material(
            id = PLY_ID,
            name = "Plywood 18 mm",
            thicknessMm = 18f,
            sheetWidthMm = 1220f,
            sheetHeightMm = 2440f,
            pricePerSheet = 320_000.0,
            hasGrain = true,
            colorHex = "#C9A176",
        ),
        Material(
            id = MDF_ID,
            name = "MDF 18 mm",
            thicknessMm = 18f,
            sheetWidthMm = 1220f,
            sheetHeightMm = 2440f,
            pricePerSheet = 245_000.0,
            hasGrain = false,
            colorHex = "#A8A29A",
        ),
        Material(
            id = BACK_ID,
            name = "Backing 4 mm",
            thicknessMm = 4f,
            sheetWidthMm = 1220f,
            sheetHeightMm = 2440f,
            pricePerSheet = 135_000.0,
            hasGrain = false,
            colorHex = "#8D8880",
        ),
    )

    fun paramsFor(type: FurnitureType): DesignParams = when (type) {
        FurnitureType.WARDROBE -> DesignParams(
            type = type, width = 1200f, height = 2000f, depth = 600f,
            shelfCount = 3, doorCount = 2, backPanel = true, toeKick = 0f,
        )
        FurnitureType.TABLE -> DesignParams(
            type = type, width = 1400f, height = 750f, depth = 700f,
            shelfCount = 0, doorCount = 0, backPanel = false, legSize = 60f,
            apronHeight = 80f,
        )
        FurnitureType.SHELF -> DesignParams(
            type = type, width = 800f, height = 1200f, depth = 300f,
            shelfCount = 4, doorCount = 0, backPanel = true,
        )
        FurnitureType.CABINET -> DesignParams(
            type = type, width = 800f, height = 850f, depth = 600f,
            shelfCount = 1, doorCount = 2, backPanel = true, toeKick = 100f,
        )
        FurnitureType.CUSTOM -> DesignParams(
            type = type, width = 900f, height = 1200f, depth = 450f,
            shelfCount = 2, doorCount = 1, backPanel = true,
        )
    }
}
