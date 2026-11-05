package com.panelcut3d.app.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.panelcut3d.app.data.Banding
import com.panelcut3d.app.data.CostBreakdown
import com.panelcut3d.app.data.CutRow
import com.panelcut3d.app.data.Fmt
import com.panelcut3d.app.data.Grain
import com.panelcut3d.app.data.Material
import com.panelcut3d.app.data.NestResult
import com.panelcut3d.app.data.NestSheet
import com.panelcut3d.app.data.NestingEngine
import com.panelcut3d.app.data.ProjectDesign
import com.panelcut3d.app.data.UnitSystem
import com.panelcut3d.app.data.cutRows
import java.io.File
import java.io.FileOutputStream

enum class ExportFormat(val label: String, val mime: String) {
    PDF("Full PDF", "application/pdf"),
    CSV_CUT("CSV cut list", "text/csv"),
    CSV_NEST("CSV nesting", "text/csv"),
}

object Exporter {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 36f
    private const val CONTENT_W = PAGE_W - MARGIN * 2
    private const val BOTTOM = 792f

    private val INK = Color.rgb(26, 24, 21)
    private val MUTED = Color.rgb(112, 106, 96)
    private val AMBER = Color.rgb(196, 129, 39)
    private val AMBER_SOFT = Color.rgb(247, 235, 214)
    private val LINE = Color.rgb(224, 217, 206)
    private val PAPER = Color.rgb(252, 250, 246)
    private val DARK = Color.rgb(26, 27, 30)

    fun createPdf(
        context: Context,
        projectName: String,
        design: ProjectDesign,
        nesting: NestResult,
        cost: CostBreakdown,
        units: UnitSystem,
        currency: String,
    ): File {
        val rows = design.cutRows()
        val writer = PdfWriter(projectName)
        drawCover(writer, design, nesting, cost, units, currency)
        drawCutList(writer, design, rows, units)
        drawNesting(writer, design, nesting, units)
        drawDrawings(writer, design, units)
        drawCost(writer, cost, currency, design)
        val file = newFile(context, projectName, "pdf")
        writer.save(file)
        return file
    }

    fun createCutCsv(
        context: Context,
        projectName: String,
        design: ProjectDesign,
        units: UnitSystem,
    ): File {
        val rows = design.cutRows()
        val sb = StringBuilder("\uFEFF")
        sb.append("Panel;Material;Thickness mm;Width mm;Height mm;Qty;Top;Bottom;Left;Right;Grain;Area m2;Edge banding m\n")
        rows.forEach { row ->
            sb.append(csv(row.name)).append(';')
                .append(csv(row.material.name)).append(';')
                .append(num(row.material.thicknessMm)).append(';')
                .append(num(row.cutW)).append(';')
                .append(num(row.cutH)).append(';')
                .append(row.qty).append(';')
                .append(if (row.banding.top) "Yes" else "-").append(';')
                .append(if (row.banding.bottom) "Yes" else "-").append(';')
                .append(if (row.banding.left) "Yes" else "-").append(';')
                .append(if (row.banding.right) "Yes" else "-").append(';')
                .append(row.grain.label).append(';')
                .append(num(row.areaM2.toFloat(), 3)).append(';')
                .append(num(row.bandingMeters.toFloat(), 3)).append('\n')
        }
        sb.append("TOTAL;").append(rows.size).append(" types;").append(rows.sumOf { it.qty }).append(" panels;;;;;;;")
            .append(num(design.totalAreaM2().toFloat(), 3)).append(';')
            .append(num(design.bandingMeters().toFloat(), 3)).append('\n')
        val file = newFile(context, projectName, "csv")
        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    fun createNestingCsv(
        context: Context,
        projectName: String,
        design: ProjectDesign,
        nesting: NestResult,
        units: UnitSystem,
    ): File {
        val sb = StringBuilder("\uFEFF")
        sb.append("Sheet;Material;No;Panel;X mm;Y mm;Width mm;Height mm;Rotated\n")
        nesting.sheets.forEach { sheet ->
            sheet.placements.forEachIndexed { index, p ->
                sb.append(sheet.index).append(';')
                    .append(csv(sheet.materialName)).append(';')
                    .append(index + 1).append(';')
                    .append(csv(p.name)).append(';')
                    .append(num(p.x)).append(';')
                    .append(num(p.y)).append(';')
                    .append(num(p.w)).append(';')
                    .append(num(p.h)).append(';')
                    .append(if (p.rotated) "Yes" else "No").append('\n')
            }
        }
        val file = newFile(context, projectName, "nesting.csv")
        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    fun share(context: Context, file: File, mime: String, projectName: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${file.nameWithoutExtension} — PanelCut 3D")
            putExtra(Intent.EXTRA_TEXT, "PanelCut 3D document for project \"$projectName\".")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share document").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun saveToDownloads(context: Context, file: File, mime: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/PanelCut 3D")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri: Uri? = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { output ->
                    file.inputStream().use { input -> input.copyTo(output) }
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            "Download/PanelCut 3D/${file.name}"
        } else {
            val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val directory = File(base, "PanelCut 3D")
            directory.mkdirs()
            val target = File(directory, file.name)
            file.copyTo(target, overwrite = true)
            target.absolutePath
        }
    }

    private fun newFile(context: Context, projectName: String, extension: String): File {
        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = Fmt.fileStamp()
        val base = Fmt.safeFileName(projectName)
        return File(directory, "PanelCut3D_${base}_$stamp.$extension")
    }

    private fun num(value: Float, decimals: Int = 1): String {
        val text = String.format(java.util.Locale.US, "%.${decimals}f", value)
        return text.replace(Regex("\\.?0+$"), "").ifBlank { "0" }
    }

    private fun csv(value: String): String {
        val clean = value.replace("\n", " ")
        return if (clean.contains(';') || clean.contains('"')) {
            "\"" + clean.replace("\"", "\"\"") + "\""
        } else clean
    }

    // ------------------------------------------------------------------ pdf

    private class PdfWriter(val projectName: String) {
        val document = PdfDocument()
        private var page: PdfDocument.Page? = null
        private var pageNumber = 0

        lateinit var canvas: Canvas
            private set
        var y: Float = 0f

        private val baseText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        private val boldText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }

        fun addPage(section: String? = null) {
            finishPage()
            pageNumber++
            val newPage = document.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
            page = newPage
            canvas = newPage.canvas
            canvas.drawColor(Color.WHITE)
            if (section != null) {
                drawChrome(section)
                y = 64f
            } else {
                y = MARGIN
            }
        }

        fun ensure(height: Float, section: String) {
            if (!::canvas.isInitialized || y + height > BOTTOM) addPage(section)
        }

        fun finishPage() {
            page?.let { document.finishPage(it) }
            page = null
        }

        fun save(file: File) {
            finishPage()
            FileOutputStream(file).use { document.writeTo(it) }
            document.close()
        }

        fun text(value: String, x: Float, y: Float, size: Float, color: Int = INK, bold: Boolean = false) {
            val paint = if (bold) boldText else baseText
            paint.textSize = size
            paint.color = color
            canvas.drawText(value, x, y, paint)
        }

        fun textWidth(value: String, size: Float, bold: Boolean = false): Float {
            val paint = if (bold) boldText else baseText
            paint.textSize = size
            return paint.measureText(value)
        }

        fun textRight(value: String, right: Float, y: Float, size: Float, color: Int = INK, bold: Boolean = false) {
            text(value, right - textWidth(value, size, bold), y, size, color, bold)
        }

        fun textEllipsized(value: String, x: Float, y: Float, maxWidth: Float, size: Float, color: Int = INK, bold: Boolean = false) {
            val paint = if (bold) boldText else baseText
            paint.textSize = size
            paint.color = color
            var text = value
            if (paint.measureText(text) > maxWidth) {
                while (text.length > 1 && paint.measureText("$text…") > maxWidth) {
                    text = text.dropLast(1)
                }
                text = "$text…"
            }
            canvas.drawText(text, x, y, paint)
        }

        fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int = LINE, width: Float = 0.7f) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                strokeWidth = width
                style = Paint.Style.STROKE
            }
            canvas.drawLine(x1, y1, x2, y2, paint)
        }

        fun rect(x: Float, y: Float, w: Float, h: Float, fill: Int, stroke: Int? = null, radius: Float = 0f) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fill
                style = Paint.Style.FILL
            }
            if (radius > 0f) {
                canvas.drawRoundRect(RectF(x, y, x + w, y + h), radius, radius, paint)
            } else {
                canvas.drawRect(x, y, x + w, y + h, paint)
            }
            if (stroke != null) {
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = stroke
                    style = Paint.Style.STROKE
                    strokeWidth = 0.7f
                }
                if (radius > 0f) {
                    canvas.drawRoundRect(RectF(x, y, x + w, y + h), radius, radius, strokePaint)
                } else {
                    canvas.drawRect(x, y, x + w, y + h, strokePaint)
                }
            }
        }

        fun paragraph(value: String, x: Float, width: Float, size: Float = 8.5f, color: Int = MUTED): Float {
            val paint = TextPaint(baseText).apply {
                this.textSize = size
                this.color = color
            }
            val layout = StaticLayout.Builder.obtain(value, 0, value.length, paint, width.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.15f)
                .setIncludePad(false)
                .build()
            canvas.save()
            canvas.translate(x, y)
            layout.draw(canvas)
            canvas.restore()
            return layout.height.toFloat()
        }

        fun sectionTitle(number: String, title: String, subtitle: String? = null) {
            text(number, MARGIN, y, 8f, AMBER, bold = true)
            text(title, MARGIN + 20f, y, 15f, INK, bold = true)
            y += 7f
            if (subtitle != null) {
                text(subtitle, MARGIN + 20f, y + 8f, 8.5f, MUTED)
                y += 17f
            } else {
                y += 6f
            }
            line(MARGIN, y, PAGE_W - MARGIN, y, LINE, 0.8f)
            y += 16f
        }

        private fun drawChrome(section: String) {
            canvas.drawRect(0f, 0f, PAGE_W.toFloat(), 44f, Paint().apply { color = Color.WHITE })
            text("PanelCut 3D", MARGIN, 27f, 9f, AMBER, bold = true)
            textRight(section.uppercase(), PAGE_W - MARGIN, 27f, 7.5f, MUTED)
            line(MARGIN, 38f, PAGE_W - MARGIN, 38f, LINE, 0.8f)
            line(MARGIN, 800f, PAGE_W - MARGIN, 800f, LINE, 0.5f)
            text(projectName, MARGIN, 812f, 7f, MUTED)
            textRight("page $pageNumber", PAGE_W - MARGIN, 812f, 7f, MUTED)
        }
    }

    private fun drawCover(
        writer: PdfWriter,
        design: ProjectDesign,
        nesting: NestResult,
        cost: CostBreakdown,
        units: UnitSystem,
        currency: String,
    ) {
        writer.addPage(null)
        val c = writer.canvas
        c.drawRoundRect(RectF(MARGIN, MARGIN, (PAGE_W - MARGIN).toFloat(), 288f), 14f, 14f, Paint().apply { color = DARK })

        writer.text("PANELCUT 3D", MARGIN + 24f, 76f, 9f, AMBER, bold = true)
        writer.text("PRODUCTION DOCUMENT", MARGIN + 24f, 90f, 7f, Color.rgb(160, 152, 140))

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textSize = 27f
        }
        val nameLayout = StaticLayout.Builder
            .obtain(writer.projectName, 0, writer.projectName.length, TextPaint(namePaint), (CONTENT_W - 48f).toInt())
            .setLineSpacing(0f, 1.05f)
            .setIncludePad(false)
            .build()
        c.save()
        c.translate(MARGIN + 24f, 118f)
        nameLayout.draw(c)
        c.restore()

        val typeY = 118f + nameLayout.height + 16f
        writer.text(
            "${design.params.type.label.uppercase()}  •  ${design.params.width.toInt()} × ${design.params.height.toInt()} × ${design.params.depth.toInt()} mm",
            MARGIN + 24f,
            typeY,
            9f,
            Color.rgb(221, 211, 196),
        )
        writer.text(
            "${design.panels.size} panels  •  ${Fmt.area(design.totalAreaM2())} material  •  ${Fmt.shortDate(System.currentTimeMillis())}",
            MARGIN + 24f,
            typeY + 16f,
            8f,
            Color.rgb(160, 152, 140),
        )
        c.drawRect(MARGIN + 24f, 240f, MARGIN + 24f + 48f, 243f, Paint().apply { color = AMBER })

        writer.y = 312f
        writer.text("SUMMARY", MARGIN, writer.y, 8f, AMBER, bold = true)
        writer.y += 10f
        writer.text("Production readiness", MARGIN, writer.y + 12f, 16f, INK, bold = true)
        writer.y += 30f
        val stats = listOf(
            "PANELS" to "${design.panels.size}",
            "SHEETS" to "${nesting.sheetCount}",
            "EFFICIENCY" to Fmt.percent(NestingEngine.utilizationPercent(nesting).toDouble(), 0),
            "WASTE" to Fmt.percent(nesting.wastePercent, 0),
        )
        val statW = (CONTENT_W - 3 * 8f) / 4f
        stats.forEachIndexed { index, stat ->
            val x = MARGIN + index * (statW + 8f)
            writer.rect(x, writer.y, statW, 58f, PAPER, LINE, 8f)
            writer.text(stat.first, x + 10f, writer.y + 17f, 6.5f, MUTED)
            writer.text(stat.second, x + 10f, writer.y + 39f, 15f, INK, bold = true)
        }
        writer.y += 78f

        writer.rect(MARGIN, writer.y, CONTENT_W, 76f, AMBER_SOFT, null, 9f)
        writer.text("ESTIMATED SELLING PRICE", MARGIN + 14f, writer.y + 20f, 7f, Color.rgb(128, 84, 25), bold = true)
        writer.text(Fmt.money(cost.total, currency), MARGIN + 14f, writer.y + 44f, 19f, INK, bold = true)
        writer.text(
            "Material cost ${Fmt.money(cost.materialTotal, currency)}  •  ${nesting.sheetCount} sheets",
            MARGIN + 14f,
            writer.y + 61f,
            7.5f,
            Color.rgb(110, 84, 48),
        )
        writer.textRight(
            "Margin ${Fmt.number(cost.marginPercent.toDouble(), 0)}%",
            PAGE_W - MARGIN - 14f,
            writer.y + 44f,
            9f,
            Color.rgb(128, 84, 25),
            bold = true,
        )
        writer.y += 98f

        writer.text("DOCUMENT CONTENTS", MARGIN, writer.y, 8f, AMBER, bold = true)
        writer.y += 14f
        val contents = listOf(
            "Complete cut list by material, thickness, and edge banding",
            "Nesting layouts for ${nesting.sheetCount} sheets with ${Fmt.dimNoUnit(nesting.settings.kerfMm, units)} ${units.suffix} kerf",
            "Front and top shop drawings",
            "Cost estimate and selling price breakdown",
        )
        contents.forEach { item ->
            c.drawCircle(MARGIN + 3f, writer.y - 2.5f, 1.8f, Paint().apply { color = AMBER })
            writer.text(item, MARGIN + 12f, writer.y, 8.5f, INK)
            writer.y += 16f
        }

        writer.y = BOTTOM - 90f
        writer.rect(MARGIN, writer.y, CONTENT_W, 62f, Color.rgb(246, 243, 237), null, 8f)
        writer.text("PRODUCTION NOTES", MARGIN + 12f, writer.y + 16f, 7f, MUTED, bold = true)
        val notes = design.notes.ifBlank {
            "Check grain direction before cutting. All dimensions are in millimeters and already account for kerf."
        }
        writer.paragraph(notes, MARGIN + 12f, CONTENT_W - 24f, 8f, INK)
    }

    private fun drawCutList(writer: PdfWriter, design: ProjectDesign, rows: List<CutRow>, units: UnitSystem) {
        writer.addPage("CUT LIST")
        writer.sectionTitle("01", "Cut list", "Cut list for ${design.materials.size} materials • unit ${units.suffix}")

        val groups = rows.groupBy { it.material.id }
            .map { (_, list) -> list.first().material to list }
            .sortedBy { it.first.name }

        for ((material, materialRows) in groups) {
            writer.ensure(70f, "CUT LIST")
            writer.text(material.name, MARGIN, writer.y, 10.5f, INK, bold = true)
            val totalArea = materialRows.sumOf { it.areaM2 }
            writer.textRight(
                "${materialRows.sumOf { it.qty }} panels  •  ${Fmt.area(totalArea)}  •  ${material.thicknessMm.toInt()} mm",
                PAGE_W - MARGIN,
                writer.y,
                8f,
                MUTED,
            )
            writer.y += 8f
            writer.line(MARGIN, writer.y, PAGE_W - MARGIN, writer.y, LINE, 1f)
            writer.y += 13f
            drawTableHeader(writer)
            materialRows.forEach { row ->
                if (writer.y + 22f > BOTTOM) {
                    writer.addPage("CUT LIST")
                    writer.sectionTitle("01", "Cut list (continued)", material.name)
                    drawTableHeader(writer)
                }
                drawCutRow(writer, row, units)
            }
            writer.y += 14f
        }

        writer.ensure(70f, "CUT LIST")
        writer.rect(MARGIN, writer.y, CONTENT_W, 58f, AMBER_SOFT, null, 8f)
        writer.text("TOTAL", MARGIN + 12f, writer.y + 18f, 7f, MUTED, bold = true)
        writer.text(
            "${rows.sumOf { it.qty }} panels  •  ${Fmt.area(design.totalAreaM2())}  •  edge banding ${Fmt.number(design.bandingMeters(), 2)} m",
            MARGIN + 12f,
            writer.y + 38f,
            11f,
            INK,
            bold = true,
        )
        writer.y += 76f
        writer.text(
            "Dimensions are final cut sizes. Kerf and sheet trim are handled separately in the nesting layout.",
            MARGIN,
            writer.y,
            7.5f,
            MUTED,
        )
    }

    private fun drawTableHeader(writer: PdfWriter) {
        writer.rect(MARGIN, writer.y, CONTENT_W, 20f, Color.rgb(242, 239, 233))
        val baseline = writer.y + 13f
        writer.text("PANEL", MARGIN + 6f, baseline, 6.5f, MUTED, bold = true)
        writer.text("MATERIAL", MARGIN + 154f, baseline, 6.5f, MUTED, bold = true)
        writer.text("W", MARGIN + 262f, baseline, 6.5f, MUTED, bold = true)
        writer.text("H", MARGIN + 308f, baseline, 6.5f, MUTED, bold = true)
        writer.text("T", MARGIN + 354f, baseline, 6.5f, MUTED, bold = true)
        writer.text("QTY", MARGIN + 386f, baseline, 6.5f, MUTED, bold = true)
        writer.text("EDGE", MARGIN + 422f, baseline, 6.5f, MUTED, bold = true)
        writer.text("AREA", MARGIN + 486f, baseline, 6.5f, MUTED, bold = true)
        writer.y += 20f
    }

    private fun drawCutRow(writer: PdfWriter, row: CutRow, units: UnitSystem) {
        val h = 20f
        writer.line(MARGIN, writer.y + h, PAGE_W - MARGIN, writer.y + h, Color.rgb(238, 234, 227), 0.5f)
        val baseline = writer.y + 13.5f
        writer.textEllipsized(row.name, MARGIN + 6f, baseline, 142f, 8f, INK, bold = true)
        writer.textEllipsized(row.material.name, MARGIN + 154f, baseline, 102f, 7.5f, MUTED)
        writer.textRight(Fmt.dimNoUnit(row.cutW, units), MARGIN + 300f, baseline, 8f, INK)
        writer.textRight(Fmt.dimNoUnit(row.cutH, units), MARGIN + 346f, baseline, 8f, INK)
        writer.textRight(row.material.thicknessMm.toInt().toString(), MARGIN + 372f, baseline, 8f, MUTED)
        writer.textRight(row.qty.toString(), MARGIN + 412f, baseline, 8f, INK)
        writer.text(row.banding.shortLabel(), MARGIN + 422f, baseline, 6.8f, MUTED)
        writer.textRight(Fmt.number(row.areaM2, 2), PAGE_W - MARGIN - 6f, baseline, 7.5f, MUTED)
        writer.y += h
    }

    private fun drawNesting(writer: PdfWriter, design: ProjectDesign, nesting: NestResult, units: UnitSystem) {
        writer.addPage("NESTING")
        writer.sectionTitle(
            "02",
            "Sheet layouts",
            "Kerf ${Fmt.dimNoUnit(nesting.settings.kerfMm, units)} ${units.suffix} • trim ${Fmt.dimNoUnit(nesting.settings.trimMm, units)} ${units.suffix} • ${nesting.sheetCount} sheets",
        )

        if (nesting.sheets.isEmpty()) {
            writer.text("No panels to nest.", MARGIN, writer.y, 9f, MUTED)
            return
        }

        nesting.sheets.forEach { sheet ->
            writer.ensure(340f, "NESTING")
            drawSheetBlock(writer, sheet, units)
        }

        if (nesting.unplaced.isNotEmpty()) {
            writer.ensure(80f, "NESTING")
            writer.text("ATTENTION", MARGIN, writer.y, 8f, Color.rgb(180, 60, 30), bold = true)
            writer.y += 13f
            nesting.unplaced.forEach { piece ->
                writer.text(
                    "• ${piece.name} (${piece.w.toInt()} × ${piece.h.toInt()} mm) — ${piece.reason}",
                    MARGIN,
                    writer.y,
                    8f,
                    INK,
                )
                writer.y += 13f
            }
        }
    }

    private fun drawSheetBlock(writer: PdfWriter, sheet: NestSheet, units: UnitSystem) {
        val availableW = CONTENT_W
        val displayH = availableW * (sheet.sheetW / sheet.sheetH)
        writer.ensure(displayH + 108f, "NESTING")
        writer.text("Sheet ${sheet.index}  •  ${sheet.materialName}", MARGIN, writer.y, 10f, INK, bold = true)
        writer.textRight(
            "${Fmt.percent(100.0 - sheet.wastePercent.toDouble(), 0)} used  •  ${Fmt.percent(sheet.wastePercent.toDouble(), 0)} waste",
            PAGE_W - MARGIN,
            writer.y,
            7.5f,
            MUTED,
        )
        writer.y += 9f
        val sheetTop = writer.y
        writer.rect(MARGIN, sheetTop, availableW, displayH, Color.rgb(247, 244, 238), Color.rgb(120, 110, 96), 2f)
        val scale = availableW / sheet.sheetH
        val palette = intArrayOf(
            Color.rgb(213, 170, 112),
            Color.rgb(196, 205, 188),
            Color.rgb(232, 204, 160),
            Color.rgb(185, 172, 155),
        )
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(48, 37, 24)
            textSize = 5.5f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(90, 78, 62)
            style = Paint.Style.STROKE
            strokeWidth = 0.45f
        }
        sheet.placements.forEachIndexed { index, p ->
            val left = MARGIN + p.y * scale
            val top = sheetTop + p.x * scale
            val width = p.h * scale
            val height = p.w * scale
            val rect = RectF(left, top, left + width, top + height)
            writer.canvas.drawRect(rect, Paint().apply { color = palette[index % palette.size] })
            writer.canvas.drawRect(rect, borderPaint)
            if (width > 18f && height > 9f) {
                writer.canvas.drawText(
                    "${index + 1}",
                    rect.centerX(),
                    rect.centerY() + 2f,
                    labelPaint,
                )
            }
        }
        writer.y = sheetTop + displayH + 12f

        val used = sheet.placements
        val twoCols = used.size > 8
        val colW = if (twoCols) CONTENT_W / 2f else CONTENT_W
        val rows = if (twoCols) (used.size + 1) / 2 else used.size
        val needed = rows * 11f + 24f
        writer.ensure(needed, "NESTING")
        used.forEachIndexed { index, p ->
            val col = if (twoCols) index % 2 else 0
            val row = if (twoCols) index / 2 else index
            val x = MARGIN + col * colW
            val lineY = writer.y + row * 11f
            writer.text("${index + 1}.", x, lineY, 7f, AMBER, bold = true)
            writer.textEllipsized(p.name, x + 14f, lineY, colW - 104f, 7.5f, INK)
            writer.text(
                "${p.w.toInt()} × ${p.h.toInt()} mm${if (p.rotated) "  ↻" else ""}",
                x + colW - 86f,
                lineY,
                7f,
                MUTED,
            )
        }
        writer.y += rows * 11f + 20f
        writer.line(MARGIN, writer.y - 8f, PAGE_W - MARGIN, writer.y - 8f, LINE, 0.5f)
    }

    private fun drawDrawings(writer: PdfWriter, design: ProjectDesign, units: UnitSystem) {
        writer.addPage("SHOP DRAWINGS")
        writer.sectionTitle("03", "Shop drawings", "Front and top views • scale fits the page")

        val w = design.params.width
        val h = design.params.height
        val d = design.params.depth
        val availableW = CONTENT_W
        val frontH = availableW * (h / w)
        val maxH = 390f
        val scale = minOf(availableW / w, maxH / h)
        val drawW = w * scale
        val drawH = h * scale
        val originX = MARGIN + (availableW - drawW) / 2f
        val originY = writer.y + 14f

        writer.text("FRONT VIEW", MARGIN, writer.y, 7f, MUTED, bold = true)
        val materials = design.materials.associateBy { it.id }
        design.panels.sortedBy { it.z }.forEach { panel ->
            if (panel.sx <= 0f || panel.sy <= 0f) return@forEach
            val left = originX + panel.x * scale
            val top = originY + (h - panel.y - panel.sy) * scale
            val fill = try {
                Color.parseColor(materials[panel.materialId]?.colorHex ?: "#C9A176")
            } catch (_: Exception) {
                Color.rgb(201, 161, 118)
            }
            writer.rect(left, top, panel.sx * scale, panel.sy * scale, fill, Color.rgb(72, 59, 44))
        }
        writer.rect(originX, originY, drawW, drawH, Color.TRANSPARENT, Color.rgb(52, 45, 36))
        writer.line(originX, originY - 12f, originX + drawW, originY - 12f, AMBER, 0.7f)
        writer.text(
            Fmt.dim(w, units),
            originX + drawW / 2f - writer.textWidth(Fmt.dim(w, units), 7f) / 2f,
            originY - 16f,
            7f,
            MUTED,
        )
        writer.y = originY + drawH + 34f

        val topScale = minOf(availableW / w, 160f / d)
        val topW = w * topScale
        val topD = d * topScale
        val topX = MARGIN + (availableW - topW) / 2f
        writer.ensure(topD + 80f, "SHOP DRAWINGS")
        writer.text("TOP VIEW", MARGIN, writer.y, 7f, MUTED, bold = true)
        val topY = writer.y + 12f
        design.panels.sortedBy { it.y }.forEach { panel ->
            if (panel.sz <= 0f) return@forEach
            val left = topX + panel.x * topScale
            val top = topY + panel.z * topScale
            val fill = try {
                Color.parseColor(materials[panel.materialId]?.colorHex ?: "#C9A176")
            } catch (_: Exception) {
                Color.rgb(201, 161, 118)
            }
            writer.rect(left, top, panel.sx * topScale, panel.sz * topScale, fill, Color.rgb(72, 59, 44))
        }
        writer.rect(topX, topY, topW, topD, Color.TRANSPARENT, Color.rgb(52, 45, 36))
        writer.text("Front view scale 1:${(1f / scale).toInt().coerceAtLeast(1)}", MARGIN, topY + topD + 20f, 7f, MUTED)
        writer.textRight("PanelCut 3D • ${Fmt.shortDate(System.currentTimeMillis())}", PAGE_W - MARGIN, topY + topD + 20f, 7f, MUTED)
        writer.y = topY + topD + 36f
    }

    private fun drawCost(writer: PdfWriter, cost: CostBreakdown, currency: String, design: ProjectDesign) {
        writer.addPage("COSTS")
        writer.sectionTitle("04", "Cost estimate", "Material, labor, and selling price")

        val rows = mutableListOf<Triple<String, String, Boolean>>()
        cost.materialLines.forEach { line ->
            rows += Triple(
                "${line.name} — ${line.sheets} sheets",
                Fmt.money(line.subtotal, currency),
                false,
            )
        }
        rows += Triple("Edge banding — ${Fmt.number(cost.edgeBandingMeters, 2)} m", Fmt.money(cost.edgeBandingTotal, currency), false)
        rows += Triple("Hardware — ${cost.doorCount} doors", Fmt.money(cost.hardwareTotal, currency), false)
        rows += Triple("Labor — ${Fmt.number(cost.laborHours.toDouble(), 1)} h", Fmt.money(cost.laborTotal, currency), false)

        rows.forEach { row ->
            writer.ensure(24f, "COSTS")
            writer.text(row.first, MARGIN, writer.y, 9f, INK)
            writer.textRight(row.second, PAGE_W - MARGIN, writer.y, 9f, INK, bold = true)
            writer.y += 8f
            writer.line(MARGIN, writer.y, PAGE_W - MARGIN, writer.y, Color.rgb(240, 236, 230), 0.5f)
            writer.y += 12f
        }

        writer.y += 4f
        writer.ensure(110f, "COSTS")
        val totals = listOf(
            "Subtotal" to cost.subtotal,
            "Margin ${Fmt.number(cost.marginPercent.toDouble(), 0)}%" to cost.marginAmount,
            "Tax ${Fmt.number(cost.taxPercent.toDouble(), 0)}%" to cost.taxAmount,
        )
        totals.forEach { (label, value) ->
            writer.text(label, MARGIN, writer.y, 8.5f, MUTED)
            writer.textRight(Fmt.money(value, currency), PAGE_W - MARGIN, writer.y, 8.5f, INK)
            writer.y += 17f
        }
        writer.rect(MARGIN, writer.y, CONTENT_W, 64f, AMBER_SOFT, null, 9f)
        writer.text("RECOMMENDED SELLING PRICE", MARGIN + 14f, writer.y + 19f, 7f, Color.rgb(128, 84, 25), bold = true)
        writer.text(Fmt.money(cost.total, currency), MARGIN + 14f, writer.y + 46f, 20f, INK, bold = true)
        writer.textRight(
            "${design.params.type.label}  •  ${design.panels.size} panels",
            PAGE_W - MARGIN - 14f,
            writer.y + 46f,
            8f,
            MUTED,
        )
        writer.y += 82f

        writer.ensure(80f, "COSTS")
        writer.text("NOTES", MARGIN, writer.y, 7f, MUTED, bold = true)
        writer.y += 12f
        val assumptions = "Assumptions: labor ${Fmt.money(cost.laborRate, currency)}/h for ${Fmt.number(cost.laborHours.toDouble(), 1)} h, " +
            "edge banding ${Fmt.money(cost.edgeBandingRate, currency)}/m, hardware ${Fmt.money(cost.hardwarePerDoor, currency)}/door. " +
            "Material prices follow the per-sheet price in material settings. Delivery and installation are not included."
        val used = writer.paragraph(assumptions, MARGIN, CONTENT_W, 8f, MUTED)
        writer.y += used + 12f
    }
}
