package com.panelcut3d.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.graphics.Color

/** Finds the Activity hosting a Compose context, if there is one. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun String.toColor(fallback: Color = Color(0xFFC9A176)): Color = runCatching {
    Color(android.graphics.Color.parseColor(this))
}.getOrDefault(fallback)

fun Int.alphaColor(alpha: Float): Color = Color(this).copy(alpha = alpha)
