package com.panelcut3d.app.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panelcut3d.app.PanelCutApplication
import com.panelcut3d.app.data.CostEngine
import com.panelcut3d.app.data.NestingEngine
import com.panelcut3d.app.data.Panel
import com.panelcut3d.app.export.ExportFormat
import com.panelcut3d.app.export.Exporter
import com.panelcut3d.app.export.WorkshopExporter
import com.panelcut3d.app.ui.components.PcIconButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EditorScreen(
    projectId: Long,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onOpenProject: (Long) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as PanelCutApplication
    val vm: EditorViewModel = viewModel(
        key = "editor-$projectId",
        factory = EditorViewModel.factory(app, projectId),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showParams by remember { mutableStateOf(false) }
    var showMaterials by remember { mutableStateOf(false) }
    var editingPanel by remember { mutableStateOf<Panel?>(null) }
    var showExport by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var busyExport by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(false) }
    var showQuote by remember { mutableStateOf(false) }
    var showCutDay by remember { mutableStateOf(false) }
    var stageLight by remember { mutableStateOf(false) }

    BackHandler { vm.closeAnd(onBack) }

    fun doExport(format: ExportFormat, saveToDownloads: Boolean) {
        if (busyExport) return
        busyExport = true
        scope.launch {
            val result = runCatching {
                val nesting = state.nesting ?: withContext(Dispatchers.Default) {
                    NestingEngine.optimize(state.design, state.settings.nestSettings())
                }
                val cost = CostEngine.calculate(state.design, nesting)
                withContext(Dispatchers.IO) {
                    val file = when (format) {
                        ExportFormat.PDF -> Exporter.createPdf(
                            context = context,
                            projectName = state.name,
                            design = state.design,
                            nesting = nesting,
                            cost = cost,
                            units = state.settings.units,
                            currency = state.settings.currencyCode,
                        )
                        ExportFormat.CSV_CUT -> Exporter.createCutCsv(
                            context = context,
                            projectName = state.name,
                            design = state.design,
                            units = state.settings.units,
                        )
                        ExportFormat.CSV_NEST -> Exporter.createNestingCsv(
                            context = context,
                            projectName = state.name,
                            design = state.design,
                            nesting = nesting,
                            units = state.settings.units,
                        )
                        ExportFormat.LABELS_PDF -> WorkshopExporter.labelsPdf(
                            context = context,
                            projectName = state.name,
                            nesting = nesting,
                        )
                        ExportFormat.QUOTE_PDF -> WorkshopExporter.quotePdf(
                            context = context,
                            projectName = state.name,
                            design = state.design,
                            cost = cost,
                            currency = state.settings.currencyCode,
                        )
                        ExportFormat.DXF -> WorkshopExporter.dxf(context, state.name, nesting)
                        ExportFormat.SVG -> WorkshopExporter.svg(context, state.name, nesting)
                    }
                    if (saveToDownloads) {
                        Exporter.saveToDownloads(context, file, format.mime)
                        null
                    } else {
                        file
                    }
                }
            }
            busyExport = false
            result.onSuccess { file ->
                if (file != null) {
                    Exporter.share(context, file, format.mime, state.name)
                } else {
                    snackbar.showSnackbar("Document saved to Download/PanelCut 3D")
                }
            }.onFailure { error ->
                snackbar.showSnackbar("Export failed: ${error.message ?: "unknown error"}")
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading project…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (state.error != null) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(30.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.error ?: "Something went wrong", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = onBack) { Text("Back") }
                }
            } else {
                EditorHeader(
                    state = state,
                    menuOpen = menuOpen,
                    onMenuOpenChange = { menuOpen = it },
                    onBack = { vm.closeAnd(onBack) },
                    onUndo = { vm.undo() },
                    onRename = { showRename = true },
                    onDuplicate = {
                        vm.duplicateProject { newId ->
                            scope.launch { snackbar.showSnackbar("Project duplicated") }
                            onOpenProject(newId)
                        }
                    },
                    onExport = { showExport = true },
                    onDelete = { showDelete = true },
                )
                EditorTabs(selected = state.tab, onSelect = vm::setTab)
                Box(
                    Modifier
                        .weight(1f)
                        .navigationBarsPadding(),
                ) {
                    when (state.tab) {
                        EditorTab.DESIGN -> DesignTab(
                            state = state,
                            onSelect = vm::selectPanel,
                            onToggle3d = vm::set3d,
                            onViewerMode = vm::setViewerMode,
                            stageLight = stageLight,
                            onOpenFullscreen = { fullscreen = true },
                            onPlanView = vm::setPlanView,
                            onResetCamera = vm::resetCamera,
                            onBeginDrag = vm::beginPanelDrag,
                            onMove = vm::movePanel,
                            onEndDrag = vm::endPanelDrag,
                            onAddPanel = vm::addPanel,
                            onEditPanel = { editingPanel = it },
                            onDeletePanel = vm::deletePanel,
                            onDuplicatePanel = vm::duplicatePanel,
                            onOpenParams = { showParams = true },
                            onOpenMaterials = { showMaterials = true },
                        )
                        EditorTab.CUT_LIST -> CutListTab(
                            design = state.design,
                            nesting = state.nesting,
                            units = state.settings.units,
                            onOpenExport = { showExport = true },
                        )
                        EditorTab.NESTING -> NestingTab(
                            state = state,
                            onRefresh = vm::refreshNesting,
                            onSettings = { kerf, trim, grain ->
                                vm.updateNestingSettings(kerf, trim, grain)
                            },
                            onOpenExport = { showExport = true },
                            onOpenCutDay = { showCutDay = true },
                        )
                        EditorTab.COST -> CostTab(
                            state = state,
                            onUpdateCosts = vm::updateCosts,
                            onOpenExport = { showExport = true },
                            onOpenQuote = { showQuote = true },
                        )
                    }
                }
            }
        }
    }

    if (fullscreen) {
        FullscreenViewer(
            design = state.design,
            selectedId = state.selectedPanelId,
            mode = state.viewerMode,
            stageLight = stageLight,
            onToggleStage = { stageLight = !stageLight },
            onSelect = vm::selectPanel,
            onBeginDrag = vm::beginPanelDrag,
            onMove = vm::movePanel,
            onEndDrag = vm::endPanelDrag,
            onDismiss = { fullscreen = false },
        )
    }

    if (showParams) {
        ParamsSheet(
            initial = state.design.params,
            unit = state.settings.units,
            onDismiss = { showParams = false },
            onApply = {
                vm.updateParams(it)
                showParams = false
            },
        )
    }

    if (showMaterials) {
        MaterialsSheet(
            design = state.design,
            currency = state.settings.currencyCode,
            unit = state.settings.units,
            onDismiss = { showMaterials = false },
            onApply = { materials, primaryId, backId ->
                vm.updateMaterials(materials, primaryId, backId)
                showMaterials = false
            },
        )
    }

    editingPanel?.let { panel ->
        PanelEditSheet(
            panel = state.design.panels.firstOrNull { it.id == panel.id } ?: panel,
            design = state.design,
            unit = state.settings.units,
            onDismiss = { editingPanel = null },
            onSave = {
                vm.updatePanel(it)
                editingPanel = null
            },
            onDelete = {
                vm.deletePanel(panel.id)
                editingPanel = null
            },
        )
    }

    if (showExport) {
        ExportSheet(
            onDismiss = { showExport = false },
            onExport = { format, save ->
                showExport = false
                doExport(format, save)
            },
        )
    }

    if (showQuote) {
        val cost = CostEngine.calculate(state.design, state.nesting)
        QuoteSheet(
            projectId = state.projectId,
            projectName = state.name,
            design = state.design,
            currency = state.settings.currencyCode,
            total = cost.total,
            onDismiss = { showQuote = false },
            onSave = {
                vm.updateQuote(it)
                showQuote = false
            },
        )
    }

    if (showCutDay) {
        state.nesting?.let { nesting ->
            CutDayDialog(
                projectName = state.name,
                design = state.design,
                nesting = nesting,
                onToggle = vm::toggleCutDone,
                onDismiss = { showCutDay = false },
            )
        }
    }

    if (showRename) {
        RenameDialog(
            current = state.name,
            onDismiss = { showRename = false },
            onConfirm = {
                vm.rename(it)
                showRename = false
            },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete project?") },
            text = { Text("The entire design and its calculations will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    vm.deleteProject(onDeleted)
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun EditorHeader(
    state: EditorState,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 6.dp, end = 8.dp, top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
        }
        Column(Modifier.weight(1f)) {
            Text(
                state.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .padding(end = 5.dp)
                        .width(5.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary),
                )
                Text(
                    if (state.savedAt > 0) "Saved automatically" else "Saving…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "  •  ${state.design.panels.size} panels",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
        PcIconButton(
            icon = Icons.Rounded.Undo,
            contentDescription = "Undo",
            onClick = onUndo,
        )
        Box {
            IconButton(onClick = { onMenuOpenChange(true) }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Project menu", tint = MaterialTheme.colorScheme.onBackground)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenuOpenChange(false) }) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                    onClick = {
                        onMenuOpenChange(false)
                        onRename()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Duplicate project") },
                    leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                    onClick = {
                        onMenuOpenChange(false)
                        onDuplicate()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Export document") },
                    leadingIcon = { Icon(Icons.Rounded.IosShare, null) },
                    onClick = {
                        onMenuOpenChange(false)
                        onExport()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Delete project", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        onMenuOpenChange(false)
                        onDelete()
                    },
                )
            }
        }
    }
}

@Composable
private fun EditorTabs(selected: EditorTab, onSelect: (EditorTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EditorTab.entries.forEach { tab ->
            val active = tab == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    )
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RenameDialog(current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename project") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Project name") },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

