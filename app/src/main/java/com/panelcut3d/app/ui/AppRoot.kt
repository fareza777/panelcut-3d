package com.panelcut3d.app.ui

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panelcut3d.app.PanelCutApplication
import com.panelcut3d.app.data.AppSettings
import com.panelcut3d.app.data.Defaults
import com.panelcut3d.app.data.DesignEngine
import com.panelcut3d.app.data.FurnitureProject
import com.panelcut3d.app.data.FurnitureType
import com.panelcut3d.app.ui.editor.EditorScreen
import com.panelcut3d.app.ui.home.HomeScreen
import com.panelcut3d.app.ui.onboarding.OnboardingScreen
import com.panelcut3d.app.ui.settings.AboutScreen
import com.panelcut3d.app.ui.settings.SettingsScreen
import com.panelcut3d.app.ui.splash.SplashScreen
import com.panelcut3d.app.ui.theme.PanelCutTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Boot : Screen
    data object Welcome : Screen
    data object Home : Screen
    data object Settings : Screen
    data object About : Screen
    data class Editor(val projectId: Long) : Screen
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PanelCutApplication

    val settings = app.settings.settings.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AppSettings(),
    )

    var screen by mutableStateOf<Screen>(Screen.Boot)
        private set

    private val backStack = mutableStateListOf<Screen>()

    init {
        viewModelScope.launch {
            val current = app.settings.current()
            delay(700)
            screen = if (current.onboardingDone) Screen.Home else Screen.Welcome
        }
    }

    fun navigate(target: Screen) {
        if (target == screen) return
        backStack.add(screen)
        screen = target
    }

    fun back(): Boolean {
        if (backStack.isEmpty()) return false
        screen = backStack.removeAt(backStack.lastIndex)
        return true
    }

    fun goHome() {
        backStack.clear()
        screen = Screen.Home
    }

    fun finishOnboarding() {
        viewModelScope.launch { app.settings.completeOnboarding() }
        backStack.clear()
        screen = Screen.Home
    }

    fun openEditor(projectId: Long) = navigate(Screen.Editor(projectId))

    fun startSample() {
        viewModelScope.launch {
            val current = app.settings.current()
            app.settings.completeOnboarding()
            val design = DesignEngine.newDesign(
                params = Defaults.paramsFor(FurnitureType.WARDROBE),
                materials = Defaults.materials,
                primaryId = Defaults.PLY_ID,
                backId = Defaults.BACK_ID,
                costs = current.costAssumptions(),
            )
            val id = app.projects.save(FurnitureProject(name = "Sample wardrobe", design = design))
            backStack.clear()
            screen = Screen.Editor(id)
        }
    }
}

@Composable
fun PanelCutRoot(app: PanelCutApplication) {
    val vm: AppViewModel = viewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val screen = vm.screen

    PanelCutTheme(mode = settings.theme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            if (screen is Screen.Boot) {
                SplashScreen()
            } else {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        (fadeIn(tween(260)) ).togetherWith(fadeOut(tween(150)))
                    },
                    label = "screen",
                ) { target ->
                    when (target) {
                        is Screen.Welcome -> OnboardingScreen(
                            onDone = vm::finishOnboarding,
                            onTrySample = vm::startSample,
                        )
                        is Screen.Home -> HomeScreen(
                            settings = settings,
                            onOpenProject = vm::openEditor,
                            onOpenSettings = { vm.navigate(Screen.Settings) },
                        )
                        is Screen.Settings -> SettingsScreen(
                            onBack = { vm.back() },
                            onAbout = { vm.navigate(Screen.About) },
                        )
                        is Screen.About -> AboutScreen(
                            onBack = { vm.back() },
                        )
                        is Screen.Editor -> EditorScreen(
                            projectId = target.projectId,
                            onBack = { vm.back() },
                            onDeleted = vm::goHome,
                            onOpenProject = vm::openEditor,
                        )
                        is Screen.Boot -> SplashScreen()
                    }
                }
            }
        }
    }

    BackHandler(
        enabled = screen is Screen.Settings || screen is Screen.About,
    ) {
        vm.back()
    }
}
