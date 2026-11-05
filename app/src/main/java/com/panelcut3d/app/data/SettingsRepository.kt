package com.panelcut3d.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "panelcut_settings")

@Serializable
data class AppSettings(
    val units: UnitSystem = UnitSystem.MM,
    val theme: ThemeMode = ThemeMode.LIGHT,
    /** False for pre-light-mode settings; the first load maps those to Light. */
    val uiThemeChosen: Boolean = false,
    val kerfMm: Float = 3f,
    val trimMm: Float = 8f,
    val currencyCode: String = "USD",
    val laborRatePerHour: Double = 40_000.0,
    val defaultLaborHours: Float = 4f,
    val edgeBandingPricePerMeter: Double = 12_000.0,
    val hardwarePerDoor: Double = 28_000.0,
    val marginPercent: Float = 25f,
    val taxPercent: Float = 0f,
    val respectGrain: Boolean = true,
    val onboardingDone: Boolean = false,
    val rating: Int = 0,
    /** Lifetime entitlement for the one-time Remove Ads product. */
    val adsRemoved: Boolean = false,
    /** Epoch millis until which a rewarded-ad session removes ads. */
    val rewardedAdFreeUntil: Long = 0L,
) {
    fun isAdFree(now: Long = System.currentTimeMillis()): Boolean =
        adsRemoved || rewardedAdFreeUntil > now

    fun costAssumptions(): CostAssumptions = CostAssumptions(
        laborHours = defaultLaborHours,
        laborRatePerHour = laborRatePerHour,
        hardwarePerDoor = hardwarePerDoor,
        edgeBandingPricePerMeter = edgeBandingPricePerMeter,
        marginPercent = marginPercent,
        taxPercent = taxPercent,
    )

    fun nestSettings(): NestSettings = NestSettings(
        kerfMm = kerfMm,
        trimMm = trimMm,
        respectGrain = respectGrain,
    )
}

class SettingsRepository(private val context: Context) {

    private val key = stringPreferencesKey("settings_json")

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { prefs ->
            val stored = prefs[key]?.let { raw ->
                runCatching { AppJson.decodeFromString<AppSettings>(raw) }.getOrNull()
            } ?: AppSettings()
            // Version 1 shipped dark-only. Treat that old default as Light once,
            // while preserving a Dark choice made after this migration.
            if (!stored.uiThemeChosen && stored.theme == ThemeMode.DARK) {
                stored.copy(theme = ThemeMode.LIGHT)
            } else {
                stored
            }
        }

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        val current = current()
        val next = transform(current)
        context.settingsDataStore.edit { prefs ->
            prefs[key] = AppJson.encodeToString(next)
        }
    }

    suspend fun completeOnboarding() = update { it.copy(onboardingDone = true) }

    suspend fun setRating(stars: Int) = update { it.copy(rating = stars.coerceIn(0, 5)) }

    suspend fun grantRewardedAdFree(now: Long = System.currentTimeMillis()) = update {
        it.copy(rewardedAdFreeUntil = now + REWARDED_AD_FREE_MS)
    }

    companion object {
        const val REWARDED_AD_FREE_MS = 24L * 60L * 60L * 1000L
    }
}
