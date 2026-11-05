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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PictureAsPdf
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
import com.panelcut3d.app.ui.components.PcCard
import com.panelcut3d.app.ui.components.PcIconButton
import com.panelcut3d.app.ui.components.SheetTitle

@Composable
fun ExportSheet(
    onDismiss: () -> Unit,
    onExport: (ExportFormat, Boolean) -> Unit,
) {
    PcBottomSheet(onDismiss = onDismiss) {
        SheetTitle(
            title = "Export document",
            subtitle = "Choose a format, then share or save it to Downloads.",
        )
        ExportRow(
            icon = Icons.Rounded.PictureAsPdf,
            title = "Full PDF",
            subtitle = "Summary, cut list, nesting layouts, shop drawings, and costs",
            onShare = { onExport(ExportFormat.PDF, false) },
            onSave = { onExport(ExportFormat.PDF, true) },
        )
        Spacer(Modifier.height(9.dp))
        ExportRow(
            icon = Icons.Rounded.TableChart,
            title = "CSV cut list",
            subtitle = "Cut list per material for the workshop and Excel",
            onShare = { onExport(ExportFormat.CSV_CUT, false) },
            onSave = { onExport(ExportFormat.CSV_CUT, true) },
        )
        Spacer(Modifier.height(9.dp))
        ExportRow(
            icon = Icons.Rounded.PictureAsPdf,
            title = "CSV nesting",
            subtitle = "Coordinates of every piece on each sheet",
            onShare = { onExport(ExportFormat.CSV_NEST, false) },
            onSave = { onExport(ExportFormat.CSV_NEST, true) },
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "All files are created on-device. Nothing is uploaded to a server.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        )
    }
}

@Composable
private fun ExportRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onShare: () -> Unit,
    onSave: () -> Unit,
) {
    PcCard(contentPadding = PaddingValues(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .then(Modifier),
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PcIconButton(
                icon = Icons.Rounded.IosShare,
                contentDescription = "Share",
                onClick = onShare,
                filled = true,
                modifier = Modifier.weight(1f),
            )
            PcIconButton(
                icon = Icons.Rounded.Download,
                contentDescription = "Save to Downloads",
                onClick = onSave,
                filled = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
