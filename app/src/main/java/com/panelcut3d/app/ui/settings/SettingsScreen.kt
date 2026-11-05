package com.panelcut3d.app.ui.settings

import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panelcut3d.app.BuildConfig
import com.panelcut3d.app.PanelCutApplication
import com.panelcut3d.app.data.Fmt
import com.panelcut3d.app.data.SettingsRepository
import com.panelcut3d.app.data.ThemeMode
import com.panelcut3d.app.data.UnitSystem
import com.panelcut3d.app.ui.components.BrandMark
import com.panelcut3d.app.ui.components.DimStepper
import com.panelcut3d.app.ui.components.DividerSoft
import com.panelcut3d.app.ui.components.MoneyStepper
import com.panelcut3d.app.ui.components.NumberStepper
import com.panelcut3d.app.ui.components.PcButton
import com.panelcut3d.app.ui.components.PcCard
import com.panelcut3d.app.ui.components.Pill
import com.panelcut3d.app.ui.components.SectionHeader
import com.panelcut3d.app.ui.components.Segmented
import com.panelcut3d.app.ui.components.SettingRow
import com.panelcut3d.app.ui.components.SoftSwitchRow
import com.panelcut3d.app.ui.components.TopBar
import com.panelcut3d.app.ui.findActivity

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAbout: () -> Unit,
) {
    val vm: SettingsViewModel = viewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val projectCount by vm.projectCount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val app = context.applicationContext as PanelCutApplication
    val activity = context.findActivity()
    val playPrice by app.billing.price.collectAsStateWithLifecycle()
    val rewardedReady by app.ads.rewardedReady.collectAsStateWithLifecycle()
    val adFree = settings.isAdFree()

    var showRate by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    var clearedMessage by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = "Settings",
            subtitle = "PanelCut 3D ${BuildConfig.VERSION_NAME} • local-first",
            onBack = onBack,
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                PcCard(contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark(size = 54.dp)
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text("PanelCut 3D", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Furniture design studio",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Pill("Local projects", selected = true)
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Appearance & units", subtitle = "Interface preferences and measurement input")
            }
            item {
                PcCard(contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(9.dp))
                        Text("App theme", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(9.dp))
                    Segmented(
                        options = ThemeMode.entries.map { it.label },
                        selected = ThemeMode.entries.indexOf(settings.theme),
                        onSelect = { index ->
                            vm.update { s -> s.copy(theme = ThemeMode.entries[index], uiThemeChosen = true) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Straighten, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(9.dp))
                        Text("Measurement units", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(9.dp))
                    Segmented(
                        options = UnitSystem.entries.map { it.label },
                        selected = UnitSystem.entries.indexOf(settings.units),
                        onSelect = { index -> vm.update { it.copy(units = UnitSystem.entries[index]) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                SectionHeader(title = "Production", subtitle = "Nesting, kerf, and work pricing")
            }
            item {
                PcCard(contentPadding = PaddingValues(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        DimStepper(
                            label = "Saw kerf",
                            valueMm = settings.kerfMm,
                            unit = settings.units,
                            onChangeMm = { vm.update { s -> s.copy(kerfMm = it) } },
                            modifier = Modifier.weight(1f),
                            minMm = 0f,
                            maxMm = 12f,
                            stepMm = 1f,
                        )
                        DimStepper(
                            label = "Edge trim",
                            valueMm = settings.trimMm,
                            unit = settings.units,
                            onChangeMm = { vm.update { s -> s.copy(trimMm = it) } },
                            modifier = Modifier.weight(1f),
                            minMm = 0f,
                            maxMm = 40f,
                            stepMm = 2f,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    SoftSwitchRow(
                        title = "Preserve grain direction",
                        subtitle = "Panels won't rotate against the sheet grain",
                        checked = settings.respectGrain,
                        onCheckedChange = { vm.update { s -> s.copy(respectGrain = it) } },
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        MoneyStepper(
                            label = "Edge / m",
                            valueIdr = settings.edgeBandingPricePerMeter,
                            currencyCode = settings.currencyCode,
                            onValueIdrChange = { value ->
                                vm.update { s -> s.copy(edgeBandingPricePerMeter = value) }
                            },
                            modifier = Modifier.weight(1f),
                            maxIdr = 500_000.0,
                        )
                        MoneyStepper(
                            label = "Per door",
                            valueIdr = settings.hardwarePerDoor,
                            currencyCode = settings.currencyCode,
                            onValueIdrChange = { value ->
                                vm.update { s -> s.copy(hardwarePerDoor = value) }
                            },
                            modifier = Modifier.weight(1f),
                            maxIdr = 2_000_000.0,
                        )
                    }
                }
            }

            item {
                SectionHeader(title = "Business", subtitle = "Default cost estimates for new projects")
            }
            item {
                PcCard(contentPadding = PaddingValues(16.dp)) {
                    Text("CURRENCY", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    val deviceCurrency = remember { Fmt.deviceCurrency() }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(Fmt.currencies) { spec ->
                            Pill(
                                text = "${spec.code} ${spec.symbol}",
                                selected = settings.currencyCode == spec.code,
                                onClick = { vm.update { s -> s.copy(currencyCode = spec.code) } },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "USD is the default. Conversions use offline reference rates.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f),
                        )
                        if (deviceCurrency.code != settings.currencyCode) {
                            TextButton(
                                onClick = { vm.update { s -> s.copy(currencyCode = deviceCurrency.code) } },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                            ) {
                                Text("Use ${deviceCurrency.code}", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        MoneyStepper(
                            label = "Rate / hour",
                            valueIdr = settings.laborRatePerHour,
                            currencyCode = settings.currencyCode,
                            onValueIdrChange = { value -> vm.update { s -> s.copy(laborRatePerHour = value) } },
                            modifier = Modifier.weight(1f),
                            maxIdr = 2_000_000.0,
                        )
                        NumberStepper(
                            label = "Labor hours",
                            value = settings.defaultLaborHours,
                            suffix = "h",
                            onChange = { value -> vm.update { s -> s.copy(defaultLaborHours = value) } },
                            modifier = Modifier.weight(1f),
                            max = 80f,
                            step = 0.5f,
                            decimals = 1,
                        )
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        NumberStepper(
                            label = "Margin",
                            value = settings.marginPercent,
                            suffix = "%",
                            onChange = { value -> vm.update { s -> s.copy(marginPercent = value) } },
                            modifier = Modifier.weight(1f),
                            max = 200f,
                            step = 1f,
                        )
                        NumberStepper(
                            label = "Tax",
                            value = settings.taxPercent,
                            suffix = "%",
                            onChange = { value -> vm.update { s -> s.copy(taxPercent = value) } },
                            modifier = Modifier.weight(1f),
                            max = 50f,
                            step = 1f,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "These settings are defaults for new projects. Per-project values can be changed on the Costs tab.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    )
                }
            }

            item {
                SectionHeader(title = "Upgrade", subtitle = "Remove ads or support the workshop")
            }
            item {
                PcCard(contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.WorkspacePremium,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (settings.adsRemoved) "Ads removed" else "Remove ads",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                if (settings.adsRemoved) {
                                    "Lifetime access is active on this account."
                                } else {
                                    "One-time purchase • restores through Google Play"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (settings.adsRemoved) {
                            Pill("OWNED", selected = true)
                        } else {
                            Text(
                                playPrice ?: "\$4.99",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    if (!settings.adsRemoved) {
                        Spacer(Modifier.height(14.dp))
                        PcButton(
                            text = "Remove ads — ${playPrice ?: "\$4.99"}",
                            onClick = {
                                if (activity == null) {
                                    Toast.makeText(context, "Purchase is unavailable right now.", Toast.LENGTH_LONG).show()
                                } else {
                                    app.billing.purchase(activity) { message ->
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Rounded.WorkspacePremium,
                        )
                    }

                    if (settings.rewardedAdFreeUntil > System.currentTimeMillis()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Rewarded ad-free until ${Fmt.shortDate(settings.rewardedAdFreeUntil)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    if (!settings.adsRemoved && !adFree) {
                        Spacer(Modifier.height(15.dp))
                        DividerSoft()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "TRY AD-FREE FOR 24 HOURS",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Watch a short rewarded ad. The timer starts after the reward is confirmed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        PcButton(
                            text = if (rewardedReady) "Watch rewarded ad" else "Loading rewarded ad…",
                            onClick = {
                                if (activity == null) {
                                    Toast.makeText(context, "Ads are unavailable right now.", Toast.LENGTH_LONG).show()
                                } else {
                                    app.ads.showRewarded(
                                        activity = activity,
                                        onReward = {
                                            vm.update {
                                                it.copy(
                                                    rewardedAdFreeUntil = System.currentTimeMillis() +
                                                        SettingsRepository.REWARDED_AD_FREE_MS,
                                                )
                                            }
                                            Toast.makeText(context, "Ad-free for 24 hours unlocked.", Toast.LENGTH_LONG).show()
                                        },
                                        onUnavailable = {
                                            Toast.makeText(context, "No rewarded ad is available. Try again shortly.", Toast.LENGTH_LONG).show()
                                        },
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Rounded.PlayCircleOutline,
                            enabled = rewardedReady,
                            filled = false,
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Ads and purchases use Google services. Core design tools still work without a connection.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    )
                }
            }

            item {
                SectionHeader(title = "Data", subtitle = "Local device storage")
            }
            item {
                PcCard(contentPadding = PaddingValues(14.dp)) {
                    SettingRow(
                        title = "Saved projects",
                        subtitle = "Room database, no cloud sync",
                        icon = Icons.Rounded.Inventory2,
                        value = "$projectCount projects",
                    )
                    DividerSoft()
                    SettingRow(
                        title = "Delete all projects",
                        subtitle = if (clearedMessage) "All projects deleted" else "This action cannot be undone",
                        icon = Icons.Rounded.Delete,
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = { showClear = true },
                    )
                }
            }

            item {
                SectionHeader(title = "About", subtitle = "Information, support, and legal")
            }
            item {
                PcCard(contentPadding = PaddingValues(6.dp)) {
                    SettingRow(
                        title = "About PanelCut 3D",
                        subtitle = "Features, credits, and contact",
                        icon = Icons.Rounded.Info,
                        onClick = onAbout,
                    )
                    DividerSoft()
                    SettingRow(
                        title = "Rate the app",
                        subtitle = if (settings.rating > 0) "Your rating: ${settings.rating}/5" else "Help us improve",
                        icon = Icons.Rounded.Star,
                        onClick = { showRate = true },
                    )
                    DividerSoft()
                    SettingRow(
                        title = "Share the app",
                        subtitle = "Recommend it to fellow makers",
                        icon = Icons.Rounded.Share,
                        onClick = { shareApp(context) },
                    )
                    DividerSoft()
                    SettingRow(
                        title = "Send feedback",
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
                        title = "Open-source licenses",
                        subtitle = "Three.js, Kotlin, Jetpack Compose, Room",
                        icon = Icons.Rounded.Description,
                        onClick = { showLicenses = true },
                    )
                    DividerSoft()
                    SettingRow(
                        title = "App version",
                        subtitle = "Stable release",
                        icon = Icons.Rounded.ReceiptLong,
                        value = BuildConfig.VERSION_NAME,
                    )
                }
            }

            item {
                Text(
                    "PanelCut 3D is built for woodworkers. Projects stay local; ads and purchases use Google services.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }

    if (showRate) {
        RateDialog(
            initial = settings.rating,
            onDismiss = { showRate = false },
            onSubmit = { stars ->
                vm.rate(stars)
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

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("Delete all projects?") },
            text = { Text("All projects, material settings, and calculation history will be permanently removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    showClear = false
                    vm.clearProjects { clearedMessage = true }
                }) {
                    Text("Delete all", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClear = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
fun RateDialog(
    initial: Int,
    onDismiss: () -> Unit,
    onSubmit: (Int) -> Unit,
) {
    var stars by remember { mutableStateOf(initial.coerceIn(0, 5)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate PanelCut 3D") },
        text = {
            Column {
                Text(
                    "How helpful is the app for your work?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { index ->
                        IconButton(onClick = { stars = index }) {
                            Icon(
                                if (index <= stars) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = "$index stars",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        stars == 0 -> "Choose a star rating."
                        stars >= 4 -> "Thank you! You'll be taken to the Play Store."
                        else -> "Thanks. Your feedback helps us improve."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(stars) }, enabled = stars > 0) {
                Text(if (stars >= 4) "Rate" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        },
    )
}

@Composable
internal fun InfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier
                    .height(320.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

