package com.panelcut3d.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panelcut3d.app.BuildConfig
import com.panelcut3d.app.ui.components.BrandMark
import com.panelcut3d.app.ui.components.DividerSoft
import com.panelcut3d.app.ui.components.PcButton
import com.panelcut3d.app.ui.components.PcCard
import com.panelcut3d.app.ui.components.Pill
import com.panelcut3d.app.ui.components.SectionHeader
import com.panelcut3d.app.ui.components.SettingRow
import com.panelcut3d.app.ui.components.TopBar

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var showRate by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "About", subtitle = "PanelCut 3D", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BrandMark(size = 96.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "PanelCut 3D",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Furniture design and wood-cutting optimization, built for workshops everywhere.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Pill("Local-first", selected = true)
                        Pill("One-time upgrade")
                        Pill("Ads supported")
                    }
                }
            }

            item {
                PcCard(contentPadding = PaddingValues(18.dp)) {
                    Text("Why PanelCut 3D?", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Everything you need to turn a furniture idea into production-ready cut sheets, with your design data staying on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    FeatureRow("Parametric 3D design", "Wardrobes, tables, shelves, and cabinets with precise dimensions.")
                    FeatureRow("MaxRects nesting", "Three heuristics and three sort orders for minimal waste.")
                    FeatureRow("Cut list & costs", "Kerf, trim, grain, edge banding, labor, margin, and tax.")
                    FeatureRow("Workshop exports", "Quote PDFs, cut-list CSV, and sheet-layout CSV.")
                    FeatureRow("Local data", "Projects stay on your device and are never uploaded.")
                    FeatureRow("Optional ads", "Banner, interstitial, and rewarded ads use an internet connection. Remove Ads is a one-time purchase.")
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    AboutStat("Local", "Data", Modifier.weight(1f))
                    AboutStat("3", "Heuristics", Modifier.weight(1f))
                    AboutStat("4", "Furniture types", Modifier.weight(1f))
                }
            }

            item {
                SectionHeader(title = "Support", subtitle = "We read every message")
            }
            item {
                PcCard(contentPadding = PaddingValues(6.dp)) {
                    SettingRow(
                        title = "Rate the app",
                        subtitle = "Help other workshops find it",
                        icon = Icons.Rounded.Star,
                        onClick = { showRate = true },
                    )
                    DividerSoft()
                    SettingRow(
                        title = "Share the app",
                        subtitle = "Send it to a colleague or workshop group",
                        icon = Icons.Rounded.Share,
                        onClick = { shareApp(context) },
                    )
                    DividerSoft()
                    SettingRow(
                        title = "Email feedback",
                        subtitle = "halo@panelcut3d.app",
                        icon = Icons.Rounded.Email,
                        onClick = {
                            sendFeedback(
                                context,
                                BuildConfig.VERSION_NAME,
                                android.os.Build.MODEL ?: "Android",
                            )
                        },
                    )
                    DividerSoft()
                    SettingRow(
                        title = "Privacy policy",
                        subtitle = "Local data, ads, and purchases",
                        icon = Icons.Rounded.PrivacyTip,
                        onClick = { showPrivacy = true },
                    )
                    DividerSoft()
                    SettingRow(
                        title = "Licenses",
                        subtitle = "Open-source components",
                        icon = Icons.Rounded.Description,
                        onClick = { showLicenses = true },
                    )
                }
            }

            item {
                PcCard(contentPadding = PaddingValues(16.dp)) {
                    Text("BUILT WITH", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Pill("Kotlin")
                        Pill("Compose")
                        Pill("Three.js")
                        Pill("Room")
                    }
                    Spacer(Modifier.height(12.dp))
                    DividerSoft()
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "Made for woodworkers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PcButton(
                        text = "Rate now",
                        onClick = { showRate = true },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.Star,
                    )
                    Spacer(Modifier.height(9.dp))
                    PcButton(
                        text = "Share PanelCut 3D",
                        onClick = { shareApp(context) },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.Share,
                        filled = false,
                    )
                }
            }

            item {
                Text(
                    "© 2026 PanelCut 3D. All calculations are performed on-device. Nesting and cost results are estimates; verify before mass production.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }

    if (showRate) {
        RateDialog(
            initial = 0,
            onDismiss = { showRate = false },
            onSubmit = { stars ->
                showRate = false
                if (stars >= 4) openStore(context)
            },
        )
    }

    if (showPrivacy) {
        InfoDialog(
            title = "Privacy policy",
            body = "PanelCut 3D is local-first.\n\n" +
                "• No account is required. Projects, materials, and settings are stored in a local database on your device.\n" +
                "• Core design, nesting, cut-list, and export tools work without an internet connection.\n" +
                "• Ads are provided by Google AdMob and purchases are processed by Google Play. Both need an internet connection and may collect device, usage, and advertising data under Google's policies. Your project designs are not uploaded.\n" +
                "• PDF/CSV files are created on-device only when you request them, then shared through the app you choose.\n" +
                "• Uninstalling the app removes local data. A purchased Remove Ads entitlement can be restored through Google Play.",
            onDismiss = { showPrivacy = false },
        )
    }

    if (showLicenses) {
        InfoDialog(
            title = "Open-source licenses",
            body = "PanelCut 3D uses open-source components:\n\n" +
                "• Three.js — MIT License. WebGL 3D rendering.\n" +
                "• Kotlin & Kotlin Coroutines — Apache License 2.0.\n" +
                "• Jetpack Compose, Material 3 — Apache License 2.0.\n" +
                "• AndroidX Room & DataStore — Apache License 2.0.\n" +
                "• Kotlinx Serialization — Apache License 2.0.\n" +
                "• Plus Jakarta Sans — SIL Open Font License 1.1.\n" +
                "• Google Mobile Ads SDK & Google Play Billing — Google APIs Terms of Service.\n\n" +
                "The MaxRects nesting algorithm is implemented specifically for this app.",
            onDismiss = { showLicenses = false },
        )
    }
}

@Composable
private fun FeatureRow(title: String, subtitle: String) {
    Row(Modifier.padding(vertical = 5.dp)) {
        Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(17.dp),
        )
        Spacer(Modifier.width(9.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AboutStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
