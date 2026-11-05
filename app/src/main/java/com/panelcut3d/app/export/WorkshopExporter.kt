package com.panelcut3d.app.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.panelcut3d.app.data.CostBreakdown
import com.panelcut3d.app.data.Fmt
import com.panelcut3d.app.data.NestResult
import com.panelcut3d.app.data.ProjectDesign
import java.io.File
import java.io.FileOutputStream

/** Workshop-oriented exports that are not part of the main illustrated PDF. */
object WorkshopExporter {
    private const val W = 595
    private const val H = 842

    fun labelsPdf(context: Context, projectName: String, nesting: NestResult): File {
        val doc = PdfDocument()
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(35, 33, 29)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val bold = Paint(text).apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val faint = Paint(text).apply { color = Color.rgb(110, 103, 93); textSize = 7.5f }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(205, 196, 181)
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
        }
        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
        var pageCanvas = page.canvas
        val margin = 30f
        val gap = 8f
        val cardW = (W - margin * 2 - gap) / 2f
        val cardH = 170f
        var col = 0
        var row = 0

        fun nextSlot() {
            col++
            if (col == 2) {
                col = 0
                row++
            }
            if (row == 4) {
                doc.finishPage(page)
                pageNo++
                page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
                pageCanvas = page.canvas
                row = 0
            }
        }

        nesting.sheets.forEach { sheet ->
            sheet.placements.forEachIndexed { index, piece ->
                val left = margin + col * (cardW + gap)
                val top = margin + row * (cardH + gap)
                pageCanvas.drawRoundRect(RectF(left, top, left + cardW, top + cardH), 7f, 7f, line)
                pageCanvas.drawText("PANELCUT 3D", left + 10f, top + 19f, faint)
                pageCanvas.drawText(piece.name.take(28), left + 10f, top + 37f, bold)
                pageCanvas.drawText("${piece.w.toInt()} x ${piece.h.toInt()} mm", left + 10f, top + 54f, text)
                pageCanvas.drawText("Material: ${sheet.materialName.take(25)}", left + 10f, top + 69f, text)
                pageCanvas.drawText("Sheet ${sheet.index}  •  piece ${index + 1}", left + 10f, top + 84f, text)
                pageCanvas.drawText("Grain: ${if (piece.rotated) "rotated" else "as laid"}  •  edge: ${piece.edgeSummary()}", left + 10f, top + 99f, faint)
                piece.edgeBandingSummary()?.let {
                    pageCanvas.drawText(it.take(52), left + 10f, top + 114f, faint)
                }
                val qr = qrBitmap("PANELCUT|$projectName|s${sheet.index}|${piece.pieceId}|${piece.name}|${piece.w.toInt()}x${piece.h.toInt()}")
                if (qr != null) {
                    pageCanvas.drawBitmap(qr, null, RectF(left + cardW - 78f, top + 38f, left + cardW - 12f, top + 104f), null)
                    qr.recycle()
                }
                pageCanvas.drawText("Scan to verify panel", left + 10f, top + cardH - 15f, faint)
                nextSlot()
            }
        }
        doc.finishPage(page)
        val file = outputFile(context, projectName, "panel-labels", "pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun quotePdf(context: Context, projectName: String, design: ProjectDesign, cost: CostBreakdown, currency: String): File {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, 1).create())
        val c = page.canvas
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(34, 31, 27)
            textSize = 10f
        }
        val small = Paint(ink).apply { textSize = 8f; color = Color.rgb(105, 97, 87) }
        val bold = Paint(ink).apply { textSize = 21f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val h = Paint(ink).apply { textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(209, 200, 185)
            strokeWidth = 0.8f
        }
        val q = design.quote
        val number = q.number.ifBlank { "Q-${design.hashCode().toUInt().toString().take(6)}" }
        c.drawText("PANELCUT 3D", 36f, 48f, small)
        c.drawText("QUOTATION", 36f, 78f, bold)
        c.drawText(projectName.take(60), 36f, 100f, h)
        c.drawLine(36f, 114f, W - 36f, 114f, line)
        c.drawText("Quote no.", 36f, 136f, small)
        c.drawText(number, 36f, 150f, ink)
        c.drawText("Customer", 260f, 136f, small)
        c.drawText(q.customer.ifBlank { "—" }.take(42), 260f, 150f, ink)
        c.drawText("Valid", 460f, 136f, small)
        c.drawText("${q.validDays} days", 460f, 150f, ink)
        var y = 188f
        fun row(label: String, amount: Double) {
            c.drawText(label, 36f, y, ink)
            c.drawText(Fmt.money(amount, currency), W - 36f - 80f, y, ink)
            y += 20f
        }
        c.drawText("DESCRIPTION", 36f, y, small)
        c.drawText("AMOUNT", W - 36f - 80f, y, small)
        y += 10f
        c.drawLine(36f, y, W - 36f, y, line)
        y += 24f
        row("Sheet material (${cost.sheetCount} sheets)", cost.materialTotal)
        row("Edge banding (${Fmt.number(cost.edgeBandingMeters, 2)} m)", cost.edgeBandingTotal)
        row("Hardware (${cost.doorCount} doors)", cost.hardwareTotal)
        row("Workshop labour (${Fmt.number(cost.laborHours.toDouble(), 1)} h)", cost.laborTotal)
        c.drawLine(36f, y, W - 36f, y, line)
        y += 24f
        row("Subtotal", cost.subtotal)
        row("Margin ${Fmt.number(cost.marginPercent.toDouble(), 0)}%", cost.marginAmount)
        if (cost.taxPercent > 0f) row("Tax ${Fmt.number(cost.taxPercent.toDouble(), 0)}%", cost.taxAmount)
        val beforeDiscount = cost.total
        val discount = beforeDiscount * q.discountPercent / 100.0
        val grand = beforeDiscount - discount
        val deposit = grand * q.depositPercent / 100.0
        if (discount > 0) row("Discount ${Fmt.number(q.discountPercent.toDouble(), 0)}%", -discount)
        y += 8f
        c.drawLine(36f, y, W - 36f, y, line)
        y += 27f
        c.drawText("TOTAL", 36f, y, h)
        c.drawText(Fmt.money(grand, currency), W - 36f - 120f, y, Paint(bold).apply { textSize = 17f; color = Color.rgb(133, 86, 27) })
        y += 22f
        c.drawText("Deposit ${Fmt.number(q.depositPercent.toDouble(), 0)}%: ${Fmt.money(deposit, currency)}", 36f, y, small)
        y += 38f
        c.drawText("Notes", 36f, y, small)
        y += 18f
        (q.notes.ifBlank { "Prices are valid for ${q.validDays} days. Production starts after deposit confirmation." })
            .chunked(86)
            .take(5)
            .forEach { part ->
                c.drawText(part, 36f, y, ink)
                y += 16f
            }
        c.drawText("Generated on-device by PanelCut 3D • ${Fmt.shortDate(System.currentTimeMillis())}", 36f, H - 40f, small)
        doc.finishPage(page)
        val file = outputFile(context, projectName, "quote", "pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun dxf(context: Context, projectName: String, nesting: NestResult): File {
        val sb = StringBuilder()
        sb.append("0\nSECTION\n2\nENTITIES\n")
        nesting.sheets.forEach { sheet ->
            fun rect(x: Float, y: Float, w: Float, h: Float) {
                sb.append("0\nLWPOLYLINE\n8\nCUT\n90\n4\n70\n1\n")
                sb.append("10\n").append(x).append("\n20\n").append(y).append("\n")
                sb.append("10\n").append(x + w).append("\n20\n").append(y).append("\n")
                sb.append("10\n").append(x + w).append("\n20\n").append(y + h).append("\n")
                sb.append("10\n").append(x).append("\n20\n").append(y + h).append("\n")
            }
            rect(0f, 0f, sheet.sheetW, sheet.sheetH)
            sheet.placements.forEach { p ->
                rect(p.x, p.y, p.w, p.h)
                sb.append("0\nTEXT\n8\nLABELS\n10\n").append(p.x + 20f)
                    .append("\n20\n").append(p.y + 50f)
                    .append("\n40\n50\n1\n").append(p.name.replace("\n", " ")).append("\n")
            }
        }
        sb.append("0\nENDSEC\n0\nEOF\n")
        val file = outputFile(context, projectName, "sheet-layout", "dxf")
        file.writeText(sb.toString())
        return file
    }

    fun svg(context: Context, projectName: String, nesting: NestResult): File {
        val palette = listOf("#e8a33d", "#b9cbbe", "#e4c896", "#c9bba5", "#a9c1b2", "#dcae7e")
        val maxW = nesting.sheets.maxOfOrNull { it.sheetW } ?: 2440f
        val totalH = nesting.sheets.sumOf { it.sheetH.toDouble() }.toFloat() + nesting.sheets.size * 160f + 120f
        val sb = StringBuilder()
        sb.append("""<svg xmlns="http://www.w3.org/2000/svg" width="${maxW.toInt()}mm" height="${totalH.toInt()}mm" viewBox="0 0 ${maxW.toInt()} ${totalH.toInt()}">""")
        sb.append("<title>${xml(projectName)} — PanelCut 3D</title>")
        var y = 80f
        nesting.sheets.forEach { sheet ->
            sb.append("<g transform=\"translate(40,${y})\">")
            sb.append("<text x=\"0\" y=\"-28\" font-family=\"sans-serif\" font-size=\"64\" fill=\"#22201c\">Sheet ${sheet.index} • ${xml(sheet.materialName)}</text>")
            sb.append("<rect x=\"0\" y=\"0\" width=\"${sheet.sheetW}\" height=\"${sheet.sheetH}\" fill=\"#f4efe5\" stroke=\"#5c5142\" stroke-width=\"6\"/>")
            sheet.placements.forEachIndexed { i, p ->
                sb.append("<rect x=\"${p.x}\" y=\"${p.y}\" width=\"${p.w}\" height=\"${p.h}\" fill=\"${palette[i % palette.size]}\" stroke=\"#493d2d\" stroke-width=\"4\"/>")
                sb.append("<text x=\"${p.x + 18}\" y=\"${p.y + 64}\" font-family=\"sans-serif\" font-size=\"52\" fill=\"#231607\">${xml(p.name)}</text>")
                sb.append("<text x=\"${p.x + 18}\" y=\"${p.y + 128}\" font-family=\"sans-serif\" font-size=\"44\" fill=\"#4b3f30\">${p.w.toInt()} x ${p.h.toInt()} mm</text>")
            }
            sb.append("</g>")
            y += sheet.sheetH + 160f
        }
        sb.append("</svg>")
        val file = outputFile(context, projectName, "sheet-layout", "svg")
        file.writeText(sb.toString())
        return file
    }

    private fun qrBitmap(content: String): Bitmap? = runCatching {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 240, 240)
        val bmp = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(matrix.width * matrix.height)
        for (yy in 0 until matrix.height) {
            for (xx in 0 until matrix.width) {
                pixels[yy * matrix.width + xx] = if (matrix.get(xx, yy)) Color.BLACK else Color.WHITE
            }
        }
        bmp.setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
        bmp
    }.getOrNull()

    private fun outputFile(context: Context, projectName: String, suffix: String, ext: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safe = projectName.trim().replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-').ifBlank { "project" }
        return File(dir, "${safe.take(40)}-$suffix-${System.currentTimeMillis()}.$ext")
    }

    private fun xml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}

private fun Any.edgeSummary(): String = "see cut list"
private fun com.panelcut3d.app.data.PlacedPiece.edgeBandingSummary(): String? = null
