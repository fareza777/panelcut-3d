package com.panelcut3d.app.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.panelcut3d.app.PanelCutApplication
import com.panelcut3d.app.data.AppSettings
import com.panelcut3d.app.data.Banding
import com.panelcut3d.app.data.CostAssumptions
import com.panelcut3d.app.data.DesignEngine
import com.panelcut3d.app.data.DesignParams
import com.panelcut3d.app.data.FurnitureProject
import com.panelcut3d.app.data.FurnitureType
import com.panelcut3d.app.data.Grain
import com.panelcut3d.app.data.Material
import com.panelcut3d.app.data.NestResult
import com.panelcut3d.app.data.NestingEngine
import com.panelcut3d.app.data.Panel
import com.panelcut3d.app.data.ProjectDesign
import com.panelcut3d.app.data.QuoteDetails
import com.panelcut3d.app.data.newPanelId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

enum class EditorTab(val label: String) {
    DESIGN("Design"),
    CUT_LIST("Cut List"),
    NESTING("Nesting"),
    COST("Costs"),
}

enum class PlanView(val label: String) {
    FRONT("Front"),
    TOP("Top"),
}

enum class ViewerMode(val label: String) {
    ISO("Iso"),
    FRONT("Front"),
    TOP("Top"),
}

data class EditorState(
    val loading: Boolean = true,
    val projectId: Long = 0L,
    val name: String = "",
    val design: ProjectDesign = ProjectDesign(),
    val createdAt: Long = 0L,
    val selectedPanelId: String? = null,
    val tab: EditorTab = EditorTab.DESIGN,
    val is3d: Boolean = true,
    val viewerMode: ViewerMode = ViewerMode.ISO,
    val planView: PlanView = PlanView.FRONT,
    val nesting: NestResult? = null,
    val nestingBusy: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val savedAt: Long = 0L,
    val error: String? = null,
) {
    val selectedPanel: Panel? get() = design.panels.firstOrNull { it.id == selectedPanelId }
}

class EditorViewModel(
    application: Application,
    private val projectId: Long,
) : AndroidViewModel(application) {

    private val app = application as PanelCutApplication
    private val projects = app.projects
    private val settingsRepo = app.settings

    private val _state = MutableStateFlow(EditorState(projectId = projectId))
    val state = _state.asStateFlow()

    private val undoStack = ArrayDeque<DesignSnapshot>()
    private var nestingJob: Job? = null
    private var saveVersion = 0L
    private var writtenVersion = 0L
    private val saveMutex = Mutex()
    private var loaded = false
    private var dragUndoCaptured = false

    init {
        loadProject()
        viewModelScope.launch {
            settingsRepo.settings.collect { settings ->
                _state.update { it.copy(settings = settings) }
                if (loaded) scheduleNesting()
            }
        }
    }

    private fun loadProject() {
        viewModelScope.launch {
            val project = projects.get(projectId)
            if (project == null) {
                _state.update { it.copy(loading = false, error = "Project not found") }
                return@launch
            }
            _state.update {
                it.copy(
                    loading = false,
                    name = project.name,
                    design = project.design,
                    createdAt = project.createdAt,
                    savedAt = project.updatedAt,
                )
            }
            loaded = true
            scheduleNesting()
        }
    }

    // ---------------------------------------------------------------- editing

    private data class DesignSnapshot(
        val params: DesignParams,
        val panels: List<Panel>,
    )

    private fun pushUndo() {
        val current = _state.value.design
        undoStack.addLast(DesignSnapshot(current.params, current.panels))
        while (undoStack.size > 30) undoStack.removeFirst()
    }

    fun undo() {
        val snapshot = undoStack.removeLastOrNull() ?: return
        _state.update {
            it.copy(
                design = it.design.copy(params = snapshot.params, panels = snapshot.panels),
                selectedPanelId = null,
            )
        }
        scheduleNesting()
        persist()
    }

    private fun applyDesign(design: ProjectDesign, select: String? = null) {
        _state.update {
            it.copy(
                design = design,
                selectedPanelId = select ?: it.selectedPanelId?.takeIf { id ->
                    design.panels.any { panel -> panel.id == id }
                },
            )
        }
        scheduleNesting()
        persist()
    }

    fun updateParams(params: DesignParams) {
        pushUndo()
        applyDesign(DesignEngine.withParams(_state.value.design, params))
    }

    fun changeType(type: FurnitureType) {
        pushUndo()
        val design = _state.value.design
        applyDesign(
            DesignEngine.regenerate(
                design.copy(params = com.panelcut3d.app.data.Defaults.paramsFor(type)),
            ),
        )
    }

    fun rename(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        _state.update { it.copy(name = clean) }
        persist()
    }

    fun updateMaterials(materials: List<Material>, primaryId: String, backId: String) {
        pushUndo()
        val design = _state.value.design
        val primary = materials.firstOrNull { it.id == primaryId } ?: materials.first()
        val back = materials.firstOrNull { it.id == backId } ?: primary
        val updated = design.copy(
            materials = materials,
            primaryMaterialId = primary.id,
            backMaterialId = back.id,
        )
        applyDesign(DesignEngine.regenerate(updated))
    }

    fun addPanel() {
        pushUndo()
        val panel = DesignEngine.defaultPanel(_state.value.design)
        applyDesign(
            _state.value.design.copy(panels = _state.value.design.panels + panel),
            select = panel.id,
        )
    }

    fun duplicatePanel(id: String) {
        val panel = _state.value.design.panels.firstOrNull { it.id == id } ?: return
        pushUndo()
        val copy = panel.copy(
            id = newPanelId(),
            name = "${panel.name} (copy)",
            x = panel.x + 40f,
            z = panel.z + 40f,
        )
        applyDesign(
            _state.value.design.copy(panels = _state.value.design.panels + copy),
            select = copy.id,
        )
    }

    fun deletePanel(id: String) {
        pushUndo()
        _state.update {
            it.copy(
                design = it.design.copy(panels = it.design.panels.filterNot { panel -> panel.id == id }),
                selectedPanelId = if (it.selectedPanelId == id) null else it.selectedPanelId,
            )
        }
        scheduleNesting()
        persist()
    }

    fun updatePanel(panel: Panel) {
        pushUndo()
        _state.update {
            it.copy(
                design = it.design.copy(
                    panels = it.design.panels.map { current -> if (current.id == panel.id) panel else current },
                ),
                selectedPanelId = panel.id,
            )
        }
        scheduleNesting()
        persist()
    }

    fun resizePanel(id: String, cutW: Float, cutH: Float) {
        val panel = _state.value.design.panels.firstOrNull { it.id == id } ?: return
        updatePanel(DesignEngine.withPanelSize(panel, cutW, cutH))
    }

    fun selectPanel(id: String?) {
        _state.update { it.copy(selectedPanelId = id) }
    }

    fun beginPanelDrag() {
        if (dragUndoCaptured) return
        dragUndoCaptured = true
        pushUndo()
    }

    fun movePanel(id: String, x: Float, y: Float, z: Float) {
        _state.update {
            it.copy(
                design = it.design.copy(
                    panels = it.design.panels.map { panel ->
                        if (panel.id == id) {
                            panel.copy(
                                x = (x / 5f).roundToInt() * 5f,
                                y = (y / 5f).roundToInt() * 5f,
                                z = (z / 5f).roundToInt() * 5f,
                            )
                        } else panel
                    },
                ),
            )
        }
    }

    fun endPanelDrag() {
        dragUndoCaptured = false
        persist()
    }

    fun updateCosts(transform: (CostAssumptions) -> CostAssumptions) {
        _state.update { it.copy(design = it.design.copy(costs = transform(it.design.costs))) }
        persist()
    }

    fun updateNotes(notes: String) {
        _state.update { it.copy(design = it.design.copy(notes = notes)) }
        persist()
    }

    fun updateQuote(quote: QuoteDetails) {
        _state.update { it.copy(design = it.design.copy(quote = quote)) }
        persist()
    }

    fun toggleCutDone(key: String) {
        _state.update { snapshot ->
            val done = snapshot.design.cutDone
            val next = if (key in done) done - key else done + key
            snapshot.copy(design = snapshot.design.copy(cutDone = next))
        }
        persist()
    }

    // --------------------------------------------------------------- viewing

    fun setTab(tab: EditorTab) = _state.update { it.copy(tab = tab) }

    fun set3d(is3d: Boolean) = _state.update { it.copy(is3d = is3d) }

    fun setViewerMode(mode: ViewerMode) = _state.update { it.copy(viewerMode = mode) }

    fun setPlanView(view: PlanView) = _state.update { it.copy(planView = view) }

    fun resetCamera() {
        _state.update { it.copy(viewerMode = ViewerMode.ISO) }
    }

    fun updateNestingSettings(kerf: Float, trim: Float, respectGrain: Boolean) {
        viewModelScope.launch {
            settingsRepo.update {
                it.copy(kerfMm = kerf.coerceIn(0f, 12f), trimMm = trim.coerceIn(0f, 40f), respectGrain = respectGrain)
            }
        }
    }

    // --------------------------------------------------------------- nesting

    fun scheduleNesting() {
        nestingJob?.cancel()
        val snapshot = _state.value
        if (snapshot.design.panels.isEmpty()) {
            _state.update { it.copy(nesting = null, nestingBusy = false) }
            return
        }
        _state.update { it.copy(nestingBusy = true) }
        nestingJob = viewModelScope.launch {
            delay(140)
            val result = withContext(Dispatchers.Default) {
                NestingEngine.optimize(snapshot.design, snapshot.settings.nestSettings())
            }
            _state.update { current ->
                if (current.design.panels == snapshot.design.panels &&
                    current.design.materials == snapshot.design.materials &&
                    current.settings.nestSettings() == snapshot.settings.nestSettings()
                ) {
                    current.copy(nesting = result, nestingBusy = false)
                } else {
                    current.copy(nestingBusy = false)
                }
            }
        }
    }

    fun refreshNesting() = scheduleNesting()

    // ------------------------------------------------------------- persistence

    private fun persist() {
        val snapshot = _state.value
        if (snapshot.loading || snapshot.error != null) return
        val version = ++saveVersion
        val project = FurnitureProject(
            id = snapshot.projectId,
            name = snapshot.name,
            design = snapshot.design,
            createdAt = snapshot.createdAt,
            updatedAt = System.currentTimeMillis(),
        )
        viewModelScope.launch(Dispatchers.IO) {
            delay(220)
            saveMutex.withLock {
                if (version < writtenVersion) return@withLock
                writtenVersion = version
                runCatching { projects.save(project) }.onSuccess { now ->
                    if (version == writtenVersion) {
                        _state.update { it.copy(savedAt = System.currentTimeMillis()) }
                    }
                }
            }
        }
    }

    fun closeAnd(onBack: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                val snapshot = _state.value
                projects.save(
                    FurnitureProject(
                        id = snapshot.projectId,
                        name = snapshot.name,
                        design = snapshot.design,
                        createdAt = snapshot.createdAt,
                    ),
                )
            }
            onBack()
        }
    }

    fun duplicateProject(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = projects.duplicate(projectId)
            if (id != null) onCreated(id)
        }
    }

    fun deleteProject(onDeleted: () -> Unit) {
        viewModelScope.launch {
            projects.delete(projectId)
            onDeleted()
        }
    }

    fun toggleBanding(panel: Panel, top: Boolean? = null, bottom: Boolean? = null, left: Boolean? = null, right: Boolean? = null) {
        val current = panel.banding
        updatePanel(
            panel.copy(
                banding = current.copy(
                    top = top ?: current.top,
                    bottom = bottom ?: current.bottom,
                    left = left ?: current.left,
                    right = right ?: current.right,
                ),
            ),
        )
    }

    fun setGrain(panel: Panel, grain: Grain) = updatePanel(panel.copy(grain = grain))

    fun setPanelMaterial(panel: Panel, materialId: String) = updatePanel(panel.copy(materialId = materialId))

    companion object {
        fun factory(app: PanelCutApplication, projectId: Long): androidx.lifecycle.ViewModelProvider.Factory =
            viewModelFactory {
                initializer { EditorViewModel(app, projectId) }
            }
    }
}
