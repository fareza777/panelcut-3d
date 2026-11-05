package com.panelcut3d.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.panelcut3d.app.PanelCutApplication
import com.panelcut3d.app.data.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PanelCutApplication

    val settings = app.settings.settings.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AppSettings(),
    )

    val projectCount = app.projects.observeAll()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { app.settings.update(transform) }
    }

    fun rate(stars: Int) {
        viewModelScope.launch { app.settings.setRating(stars) }
    }

    fun clearProjects(onDone: () -> Unit) {
        viewModelScope.launch {
            app.projects.clearAll()
            onDone()
        }
    }
}
