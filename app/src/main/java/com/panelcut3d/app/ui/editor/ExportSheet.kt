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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.panelcut3d.app.export.ExportFormat
import com.panelcut3d.app.ui.components.PcBottomSheet
import com.panelcut3d.app.ui.components.PcButton
import com.panelcut3d.app.ui.components.PcCard
import com.panelcut3d.app.ui.components.SheetTitle

private data class ExportOption(
    val format: ExportFormat,
    val icon: ImageVector,
    val subtitle: String,
)

private val options = listOf(
    ExportOption(ExportFormat.PDF, Icons.Rounded.PictureAsPdf, "Summary, cut list, nesting, drawings, and costs"),
    ExportOption(ExportFormat.LABELS_PDF, Icons.Rounded.QrCode2, "Printable QR labels with size, material, and edge banding"),
    ExportOption(ExportFormat.QUOTE_PDF, Icons.Rounded.RequestQuote, "Customer quotation with discount, tax, and deposit"),
    ExportOption(ExportFormat.CSV_CUT, Icons.Rounded.TableChart, "Cut list per material for the workshop and Excel"),
    ExportOption(ExportFormat.CSV_NEST, Icons.Rounded.TableChart, "Coordinates of every piece on each sheet"),
    ExportOption(ExportFormat.DXF, Icons.Rounded.Architecture, "Sheet layout for CAD and CNC workflow"),
    ExportOption(ExportFormat.SVG, Icons.Rounded.Architecture, "Scalable nesting plan for print or laser software"),
)

@Composable
fun ExportSheet(
    onDismiss: () -> Unit,
    onExport: (ExportFormat, Boolean) -> Unit,
) {
    PcBottomSheet(onDismiss = onDismiss) {
        SheetTitle(
            title = "Export workshop files",
            subtitle = "Every document is created on-device, then shared or saved to Downloads.",
        )
        options.forEach { option ->
            ExportRow(
                option = option,
                onShare = { onExport(option.format, false) },
                onSave = { onExport(option.format, true) },
            )
            Spacer(Modifier.height(9.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Labels include a QR code that identifies the project and panel. Print at 100% on A4.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun ExportRow(
    option: ExportOption,
    onShare: () -> Unit,
    onSave: () -> Unit,
) {
    PcCard(contentPadding = PaddingValues(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    option.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(option.format.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    option.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PcButton(
                text = "Share",
                onClick = onShare,
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.IosShare,
                filled = false,
            )
            PcButton(
                text = "Save",
                onClick = onSave,
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Download,
            )
        }
    }
}
