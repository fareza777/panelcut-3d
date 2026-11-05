package com.panelcut3d.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panelcut3d.app.PanelCutApplication
import com.panelcut3d.app.ads.BannerAd
import com.panelcut3d.app.data.AppSettings
import com.panelcut3d.app.data.Defaults
import com.panelcut3d.app.data.DesignEngine
import com.panelcut3d.app.data.Fmt
import com.panelcut3d.app.data.FurnitureProject
import com.panelcut3d.app.data.FurnitureType
import com.panelcut3d.app.data.NestingEngine
import com.panelcut3d.app.ui.components.BrandMark
import com.panelcut3d.app.ui.components.DividerSoft
import com.panelcut3d.app.ui.components.EmptyState
import com.panelcut3d.app.ui.components.GradientHero
import com.panelcut3d.app.ui.components.MiniPlan
import com.panelcut3d.app.ui.components.PcBottomSheet
import com.panelcut3d.app.ui.components.PcButton
import com.panelcut3d.app.ui.components.PcCard
import com.panelcut3d.app.ui.components.PcIconButton
import com.panelcut3d.app.ui.components.PlanStat
import com.panelcut3d.app.ui.components.SectionHeader
import com.panelcut3d.app.ui.components.SheetTitle
import com.panelcut3d.app.ui.findActivity
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    settings: AppSettings,
    onOpenProject: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: HomeViewModel = viewModel()
    val projects by vm.projects.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val app = context.applicationContext as PanelCutApplication
    val adFree = settings.isAdFree()

    var showNewSheet by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FurnitureProject?>(null) }
    var deleteTarget by remember { mutableStateOf<FurnitureProject?>(null) }

    val panelCount = projects.sumOf { it.design.panels.size }
    val sheetCount = projects.sumOf { NestingEngine.estimateSheets(it.design) }
    val totalArea = projects.sumOf { it.design.totalAreaM2() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp),
            ) {
                if (!adFree) {
                    BannerAd(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                }
                PcButton(
                    text = "New project",
                    onClick = { showNewSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Add,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandMark(size = 40.dp)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "PanelCut",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            " 3D",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                    Text(
                        "FURNITURE DESIGN STUDIO",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.4.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PcIconButton(
                    icon = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    onClick = onOpenSettings,
                    filled = true,
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Column {
                        Text(
                            "Workspace",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            "Design precisely, cut with less waste.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item {
                    GradientHero {
                        Row {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "FROM DESIGN TO CUT LIST",
                                    style = MaterialTheme.typography.labelSmall,
                                    letterSpacing = 1.8.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "One workflow,\nevery cut measured.",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "3D editor, nesting, cut lists, and cost estimates. Your designs stay on-device.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            BrandMark(size = 70.dp, framed = false)
                        }
                        Spacer(Modifier.height(18.dp))
                        DividerSoft()
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PlanStat("PROJECTS", projects.size.toString(), Modifier.weight(1f))
                            PlanStat("PANELS", panelCount.toString(), Modifier.weight(1f))
                            PlanStat("SHEETS", sheetCount.toString(), Modifier.weight(1f))
                            PlanStat("AREA", Fmt.area(totalArea), Modifier.weight(1.2f))
                        }
                    }
                }

                item {
                    SectionHeader(
                        title = "Recent projects",
                        subtitle = if (projects.isEmpty()) "No projects yet" else "${projects.size} saved ${if (projects.size == 1) "project" else "projects"} • newest first",
                    )
                }

                if (projects.isEmpty()) {
                    item {
                        PcCard {
                            EmptyState(
                                icon = Icons.Rounded.FolderOpen,
                                title = "No designs yet",
                                message = "Create your first wardrobe, table, shelf, or cabinet. All data stays on this device.",
                                actionLabel = "Create your first project",
                                onAction = { showNewSheet = true },
                            )
                        }
                    }
                } else {
                    items(projects, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            onClick = { onOpenProject(project.id) },
                            onRename = { renameTarget = project },
                            onDuplicate = {
                                vm.duplicate(project.id) { newId ->
                                    scope.launch { snackbar.showSnackbar("Project duplicated") }
                                    onOpenProject(newId)
                                }
                            },
                            onDelete = { deleteTarget = project },
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Projects are stored locally with Room and designs are never uploaded. Ads need an internet connection.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
        }
    }

    if (showNewSheet) {
        NewProjectSheet(
            onDismiss = { showNewSheet = false },
            onCreate = { type ->
                showNewSheet = false
                vm.create(type) { id ->
                    app.ads.showInterstitial(context.findActivity(), adFree) {
                        onOpenProject(id)
                    }
                }
            },
        )
    }

    renameTarget?.let { target ->
        RenameProjectDialog(
            project = target,
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                vm.rename(target.id, name)
                renameTarget = null
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete project?") },
            text = { Text("\"${target.name}\" will be permanently removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(target.id)
                    deleteTarget = null
                    scope.launch { snackbar.showSnackbar("Project deleted") }
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PresetCard(type: FurnitureType, onClick: () -> Unit) {
    val design = remember(type) { DesignEngine.newDesign(Defaults.paramsFor(type)) }
    PcCard(
        modifier = Modifier.width(150.dp),
        onClick = onClick,
        contentPadding = PaddingValues(11.dp),
    ) {
        MiniPlan(
            design = design,
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            type.label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "${design.params.width.toInt()}×${design.params.height.toInt()}×${design.params.depth.toInt()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProjectCard(
    project: FurnitureProject,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    PcCard(
        onClick = onClick,
        contentPadding = PaddingValues(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniPlan(
                design = project.design,
                modifier = Modifier
                    .size(width = 76.dp, height = 84.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    project.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${project.design.params.type.label} • ${project.design.panels.size} ${if (project.design.panels.size == 1) "panel" else "panels"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${project.design.params.width.toInt()} × ${project.design.params.height.toInt()} × ${project.design.params.depth.toInt()} mm • ${Fmt.area(project.design.totalAreaM2())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    Fmt.relativeTime(project.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "Project options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Open") },
                        leadingIcon = { Icon(Icons.Rounded.FolderOpen, null) },
                        onClick = {
                            menuOpen = false
                            onClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                        onClick = {
                            menuOpen = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                        onClick = {
                            menuOpen = false
                            onDuplicate()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NewProjectSheet(
    onDismiss: () -> Unit,
    onCreate: (FurnitureType) -> Unit,
) {
    PcBottomSheet(onDismiss = onDismiss) {
        SheetTitle(
            title = "New furniture",
            subtitle = "Choose a parametric foundation. Every dimension can be changed later.",
        )
        FurnitureType.entries.forEach { type ->
            val design = remember(type) { DesignEngine.newDesign(Defaults.paramsFor(type)) }
            PcCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                onClick = { onCreate(type) },
                contentPadding = PaddingValues(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MiniPlan(
                        design = design,
                        modifier = Modifier
                            .size(width = 62.dp, height = 68.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            type.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            type.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "${design.params.width.toInt()} × ${design.params.height.toInt()} × ${design.params.depth.toInt()} mm",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RenameProjectDialog(
    project: FurnitureProject,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(project.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename project") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Project name") },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
