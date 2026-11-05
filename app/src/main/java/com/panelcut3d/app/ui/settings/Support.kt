package com.panelcut3d.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

private const val PACKAGE_NAME = "com.panelcut3d.app"
private const val PLAY_URL = "https://play.google.com/store/apps/details?id=$PACKAGE_NAME"

fun shareApp(context: Context) {
    val text = buildString {
        append("PanelCut 3D — 3D furniture design, automatic nesting, cut lists, and PDFs in one local-first app.\n\n")
        append("Built for woodworking shops and custom furniture makers. Your designs stay on your device.\n")
        append(PLAY_URL)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "PanelCut 3D")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share PanelCut 3D").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun openStore(context: Context) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PACKAGE_NAME"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_URL))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

fun sendFeedback(context: Context, version: String, device: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:halo@panelcut3d.app")
        putExtra(Intent.EXTRA_SUBJECT, "PanelCut 3D feedback — $version")
        putExtra(
            Intent.EXTRA_TEXT,
            "Hello PanelCut 3D team,\n\nI'd like to share some feedback:\n\n\n\n" +
                "—\nVersion $version\nDevice $device",
        )
    }
    runCatching { context.startActivity(intent) }
}
