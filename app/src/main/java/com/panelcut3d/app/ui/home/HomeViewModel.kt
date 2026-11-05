package com.panelcut3d.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.panelcut3d.app.PanelCutApplication
import com.panelcut3d.app.data.Defaults
import com.panelcut3d.app.data.DesignEngine
import com.panelcut3d.app.data.Fmt
import com.panelcut3d.app.data.FurnitureProject
import com.panelcut3d.app.data.FurnitureType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PanelCutApplication

    val projects = app.projects.observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    fun create(type: FurnitureType, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val settings = app.settings.current()
            val design = DesignEngine.newDesign(
                params = Defaults.paramsFor(type),
                materials = Defaults.materials,
                primaryId = Defaults.PLY_ID,
                backId = Defaults.BACK_ID,
                costs = settings.costAssumptions(),
            )
            val name = type.label
            val id = app.projects.save(FurnitureProject(name = name, design = design))
            onCreated(id)
        }
    }

    fun duplicate(id: Long, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            app.projects.duplicate(id)?.let(onCreated)
        }
    }

    fun rename(id: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val project = app.projects.get(id) ?: return@launch
            app.projects.save(project.copy(name = name.trim()))
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { app.projects.delete(id) }
    }
}
