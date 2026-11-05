package com.panelcut3d.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.panelcut3d.app.data.ProjectDesign
import com.panelcut3d.app.ui.editor.viewer.Viewer3D

@Composable
fun FullscreenViewer(
    design: ProjectDesign,
    selectedId: String?,
    mode: ViewerMode,
    stageLight: Boolean,
    onToggleStage: () -> Unit,
    onSelect: (String?) -> Unit,
    onBeginDrag: () -> Unit,
    onMove: (String, Float, Float, Float) -> Unit,
    onEndDrag: () -> Unit,
    onDismiss: () -> Unit,
) {
    val stageColor = if (stageLight) Color(0xFFF2ECE2) else Color(0xFF0D0F12)
    val foreground = if (stageLight) Color(0xFF332A20) else Color(0xFFF5EDE1)
    val controlBackground = if (stageLight) Color(0xE6FFFCF7) else Color(0xE6111418)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(stageColor),
        ) {
            Viewer3D(
                design = design,
                selectedId = selectedId,
                mode = mode,
                stageLight = stageLight,
                cornerRadius = 0.dp,
                onSelect = onSelect,
                onMoveStart = onBeginDrag,
                onMove = onMove,
                onMoveEnd = onEndDrag,
                modifier = Modifier.fillMaxSize(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(controlBackground),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close fullscreen viewer", tint = foreground)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "3D viewer",
                        color = foreground,
                        fontWeight = FontWeight.SemiBold,
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "${design.panels.size} panels • fullscreen",
                        color = foreground.copy(alpha = 0.65f),
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(controlBackground)
                        .clickable(onClick = onToggleStage)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        if (stageLight) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                        contentDescription = null,
                        tint = foreground,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        if (stageLight) "Dark" else "Light",
                        color = foreground,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
