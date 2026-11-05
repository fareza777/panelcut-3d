package com.panelcut3d.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.panelcut3d.app.data.NestResult
import com.panelcut3d.app.data.ProjectDesign
import com.panelcut3d.app.ui.components.PcIconButton
import com.panelcut3d.app.ui.components.PcCard
import com.panelcut3d.app.ui.components.Pill as PcPill

@Composable
fun CutDayDialog(
    projectName: String,
    design: ProjectDesign,
    nesting: NestResult,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var sheetIndex by remember { mutableStateOf(0) }
    val sheets = nesting.sheets
    val selected = sheets.getOrNull(sheetIndex)
    val all = sheets.flatMap { sheet -> sheet.placements.map { "${sheet.index}:${it.pieceId}" to it.name } }
    val done = design.cutDone.toSet()
    val complete = all.count { it.first in done }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.statusBarsPadding().navigationBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Cut day", style = MaterialTheme.typography.headlineSmall)
                        Text(projectName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    PcIconButton(Icons.Rounded.Close, "Close cut day", onDismiss, filled = true)
                }
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(10.dp))
                    Text("$complete of ${all.size} panels checked", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { if (all.isEmpty()) 1f else complete.toFloat() / all.size },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        sheets.indices.forEach { index ->
                            PcPill(
                                text = "Sheet ${sheets[index].index}",
                                selected = sheetIndex == index,
                                onClick = { sheetIndex = index },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (complete == all.size && all.isNotEmpty()) "All panels cut. Ready for assembly." else "Check every panel as it comes off the saw.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (complete == all.size) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (selected != null) {
                    LazyColumn(
                        Modifier.weight(1f).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        item { Spacer(Modifier.height(10.dp)) }
                        items(selected.placements, key = { it.pieceId }) { piece ->
                            val key = "${selected.index}:${piece.pieceId}"
                            PcCard(onClick = { onToggle(key) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = key in done, onCheckedChange = { onToggle(key) })
                                    Column(Modifier.weight(1f)) {
                                        Text(piece.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${piece.w.toInt()} × ${piece.h.toInt()} mm • ${selected.materialName}${if (piece.rotated) " • rotated" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(20.dp)) }
                    }
                } else {
                    Text("No sheets to cut yet.", Modifier.padding(16.dp))
                }
            }
        }
    }
}
