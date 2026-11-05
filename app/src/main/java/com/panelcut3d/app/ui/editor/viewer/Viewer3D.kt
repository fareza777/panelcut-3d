package com.panelcut3d.app.ui.editor.viewer

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.panelcut3d.app.data.AppJson
import com.panelcut3d.app.data.ProjectDesign
import com.panelcut3d.app.ui.editor.ViewerMode
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
private data class ViewerPanel(
    val id: String,
    val name: String,
    val sx: Float,
    val sy: Float,
    val sz: Float,
    val x: Float,
    val y: Float,
    val z: Float,
    val color: String,
    val door: Boolean,
)

@Serializable
private data class ViewerPayload(
    val panels: List<ViewerPanel>,
    val width: Float,
    val height: Float,
    val depth: Float,
    val mode: String,
)

class ViewerBridge(
    private val onReady: () -> Unit,
    private val onSelect: (String) -> Unit,
    private val onMoveStart: (String) -> Unit,
    private val onMove: (String, Float, Float, Float) -> Unit,
    private val onMoveEnd: (String, Float, Float, Float) -> Unit,
    private val onError: (String) -> Unit,
) {
    @JavascriptInterface
    fun ready() = onReady()

    @JavascriptInterface
    fun select(id: String) = onSelect(id)

    @JavascriptInterface
    fun moveStart(id: String) = onMoveStart(id)

    @JavascriptInterface
    fun move(id: String, x: Float, y: Float, z: Float) = onMove(id, x, y, z)

    @JavascriptInterface
    fun moveEnd(id: String, x: Float, y: Float, z: Float) = onMoveEnd(id, x, y, z)

    @JavascriptInterface
    fun error(message: String) = onError(message)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Viewer3D(
    design: ProjectDesign,
    selectedId: String?,
    mode: ViewerMode,
    stageLight: Boolean = false,
    showDoors: Boolean = true,
    exploded: Boolean = false,
    cornerRadius: Dp = 18.dp,
    onSelect: (String?) -> Unit,
    onMoveStart: () -> Unit,
    onMove: (String, Float, Float, Float) -> Unit,
    onMoveEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val handler = remember { Handler(Looper.getMainLooper()) }
    var ready by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val stageColor = if (stageLight) Color(0xFFF2ECE2) else Color(0xFF0D0F12)
    val stageColorHex = if (stageLight) "#F2ECE2" else "#0D0F12"
    val stageTheme = if (stageLight) "light" else "dark"

    val selectState = rememberUpdatedState(onSelect)
    val moveStartState = rememberUpdatedState(onMoveStart)
    val moveState = rememberUpdatedState(onMove)
    val moveEndState = rememberUpdatedState(onMoveEnd)

    val bridge = remember {
        ViewerBridge(
            onReady = { handler.post { ready = true } },
            onSelect = { id ->
                handler.post { selectState.value(if (id.isBlank()) null else id) }
            },
            onMoveStart = { id ->
                handler.post { if (id.isNotBlank()) moveStartState.value() }
            },
            onMove = { id, x, y, z ->
                handler.post { moveState.value(id, x, y, z) }
            },
            onMoveEnd = { _, _, _, _ ->
                handler.post { moveEndState.value() }
            },
            onError = { message ->
                handler.post { error = message }
            },
        )
    }

    val structuralKey = remember(design) {
        buildString {
            append(design.params.width).append(':')
            append(design.params.height).append(':')
            append(design.params.depth).append(':')
            design.materials.forEach { append(it.id).append(it.colorHex).append(';') }
            design.panels.forEach {
                append(it.id).append(it.name).append(it.sx).append(it.sy).append(it.sz)
                    .append(it.materialId).append(it.isDoor).append(';')
            }
        }
    }

    val payload = remember(structuralKey) {
        val materials = design.materials.associateBy { it.id }
        val panels = design.panels.map { panel ->
            ViewerPanel(
                id = panel.id,
                name = panel.name,
                sx = panel.sx,
                sy = panel.sy,
                sz = panel.sz,
                x = panel.x,
                y = panel.y,
                z = panel.z,
                color = materials[panel.materialId]?.colorHex ?: "#C9A176",
                door = panel.isDoor,
            )
        }
        AppJson.encodeToString(
            ViewerPayload(
                panels = panels,
                width = design.params.width,
                height = design.params.height,
                depth = design.params.depth,
                mode = "iso",
            ),
        )
    }

    val webView = remember {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.parseColor(stageColorHex))
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            settings.domStorageEnabled = false
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.mediaPlaybackRequiresUserGesture = true
            settings.loadWithOverviewMode = false
            settings.useWideViewPort = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = WebView.OVER_SCROLL_NEVER
            isLongClickable = false
            setOnLongClickListener { true }
            addJavascriptInterface(bridge, "PanelBridge")
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    Log.d("PanelCutViewer", "${message.message()} (${message.sourceId()}:${message.lineNumber()})")
                    return true
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d("PanelCutViewer", "page finished: $url")
                    view?.evaluateJavascript(
                        "(typeof window.PanelBridge) + ':' + (typeof window.PanelCutViewer)",
                    ) { result -> Log.d("PanelCutViewer", "bridge/viewer = $result") }
                    view?.postDelayed({
                        view.evaluateJavascript(
                            "window.PanelCutViewer && PanelCutViewer.resize && PanelCutViewer.resize()",
                            null,
                        )
                    }, 250)
                }
            }
            loadUrl("file:///android_asset/viewer.html?v=4&theme=$stageTheme")
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.removeJavascriptInterface("PanelBridge")
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    }

    LaunchedEffect(Unit) {
        delay(8_000)
        if (!ready && error == null) {
            error = "The 3D viewer did not start. Close and reopen the project."
        }
    }

    LaunchedEffect(ready, payload) {
        if (ready) {
            Log.d("PanelCutViewer", "sending payload (${payload.length} chars)")
            webView.evaluateJavascript(
                "window.PanelCutViewer && PanelCutViewer.load($payload)",
                null,
            )
            webView.postDelayed({
                webView.evaluateJavascript(
                    "window.PanelCutViewer && PanelCutViewer.resize && PanelCutViewer.resize()",
                    null,
                )
            }, 300)
        }
    }

    LaunchedEffect(ready, payload, selectedId) {
        if (ready) {
            val id = selectedId?.let { "\"$it\"" } ?: "null"
            webView.evaluateJavascript("window.PanelCutViewer && PanelCutViewer.select($id)", null)
        }
    }

    LaunchedEffect(ready, stageLight) {
        if (ready) {
            webView.setBackgroundColor(android.graphics.Color.parseColor(stageColorHex))
            webView.evaluateJavascript(
                "window.PanelCutViewer && PanelCutViewer.setTheme && PanelCutViewer.setTheme(\"$stageTheme\")",
                null,
            )
        }
    }

    LaunchedEffect(ready, mode) {
        if (ready) {
            val jsMode = when (mode) {
                ViewerMode.ISO -> "iso"
                ViewerMode.FRONT -> "front"
                ViewerMode.TOP -> "top"
            }
            webView.evaluateJavascript(
                "window.PanelCutViewer && PanelCutViewer.setMode(\"$jsMode\")",
                null,
            )
        }
    }

    LaunchedEffect(ready, payload, showDoors) {
        if (ready) {
            webView.evaluateJavascript(
                "window.PanelCutViewer && PanelCutViewer.setDoorsVisible($showDoors)",
                null,
            )
        }
    }

    LaunchedEffect(ready, payload, exploded) {
        if (ready) {
            webView.evaluateJavascript(
                "window.PanelCutViewer && PanelCutViewer.setExploded($exploded)",
                null,
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(stageColor),
    ) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.setBackgroundColor(android.graphics.Color.parseColor(stageColorHex))
            },
        )
        error?.let { message ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                    .padding(22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "3D view unavailable.\n$message",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    androidx.compose.material3.TextButton(
                        onClick = {
                            error = null
                            ready = false
                            webView.reload()
                        },
                    ) {
                        Text("Try again")
                    }
                }
            }
        }
    }
}
