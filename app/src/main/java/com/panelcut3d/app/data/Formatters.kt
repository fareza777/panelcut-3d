package com.panelcut3d.app.data

import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Money values are stored in the app's rupiah base so existing projects and
 * workshop assumptions remain intact. Display/input is converted using the
 * offline reference rates below; no network call is involved.
 */
data class CurrencySpec(
    val code: String,
    val symbol: String,
    val label: String,
    val idrPerUnit: Double,
    val decimals: Int,
    val inputStep: Float,
)

object Fmt {

    val idLocale: Locale = Locale.US

    const val DEFAULT_CURRENCY = "USD"

    val currencies: List<CurrencySpec> = listOf(
        CurrencySpec("USD", "$", "US Dollar", 16_000.0, 2, 1f),
        CurrencySpec("IDR", "Rp", "Indonesian Rupiah", 1.0, 0, 1_000f),
        CurrencySpec("EUR", "€", "Euro", 17_500.0, 2, 1f),
        CurrencySpec("GBP", "£", "British Pound", 20_500.0, 2, 1f),
        CurrencySpec("AUD", "A$", "Australian Dollar", 10_500.0, 2, 1f),
        CurrencySpec("CAD", "C$", "Canadian Dollar", 11_700.0, 2, 1f),
        CurrencySpec("SGD", "S$", "Singapore Dollar", 12_000.0, 2, 1f),
        CurrencySpec("MYR", "RM", "Malaysian Ringgit", 3_600.0, 2, 1f),
        CurrencySpec("JPY", "¥", "Japanese Yen", 105.0, 0, 100f),
    )

    fun currency(code: String): CurrencySpec {
        val normalized = code.trim().uppercase(Locale.US)
        return currencies.firstOrNull { it.code == normalized } ?: currencies.first()
    }

    /** Picks a supported currency from the phone's locale; USD is the fallback. */
    fun deviceCurrency(): CurrencySpec {
        val code = runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull()
        return currencies.firstOrNull { it.code == code } ?: currencies.first()
    }

    fun fromIdr(valueIdr: Double, code: String): Double {
        val spec = currency(code)
        return if (spec.idrPerUnit <= 0.0) valueIdr else valueIdr / spec.idrPerUnit
    }

    fun toIdr(value: Double, code: String): Double {
        val spec = currency(code)
        return value * spec.idrPerUnit
    }

    fun number(value: Number, maxDecimals: Int = 0, minDecimals: Int = 0): String {
        val nf = NumberFormat.getNumberInstance(idLocale).apply {
            maximumFractionDigits = maxDecimals
            minimumFractionDigits = minDecimals
        }
        return nf.format(value.toDouble())
    }

    fun money(valueIdr: Double, code: String = DEFAULT_CURRENCY): String {
        val spec = currency(code)
        val safe = if (valueIdr.isFinite()) valueIdr else 0.0
        val value = fromIdr(safe, code)
        val nf = NumberFormat.getNumberInstance(idLocale).apply {
            maximumFractionDigits = spec.decimals
            minimumFractionDigits = spec.decimals
            roundingMode = RoundingMode.HALF_UP
        }
        val amount = nf.format(value)
        return if (spec.symbol.endsWith("$") || spec.symbol == "€" || spec.symbol == "£" || spec.symbol == "¥") {
            "${spec.symbol}$amount"
        } else {
            "${spec.symbol} $amount"
        }
    }

    fun compactMoney(valueIdr: Double, code: String = DEFAULT_CURRENCY): String {
        val safe = if (valueIdr.isFinite()) valueIdr else 0.0
        val spec = currency(code)
        val value = fromIdr(safe, code)
        val prefix = if (spec.symbol.endsWith("$") || spec.symbol == "€" || spec.symbol == "£" || spec.symbol == "¥") {
            spec.symbol
        } else {
            "${spec.symbol} "
        }
        return when {
            abs(value) >= 1_000_000_000 -> "$prefix${number(value / 1_000_000_000.0, 1)}B"
            abs(value) >= 1_000_000 -> "$prefix${number(value / 1_000_000.0, 1)}M"
            abs(value) >= 10_000 -> "$prefix${number(value / 1_000.0, 1)}K"
            else -> money(valueIdr, code)
        }
    }

    fun dim(mm: Float, unit: UnitSystem, withUnit: Boolean = true, decimals: Int = 1): String {
        val value = unit.fromMm(mm)
        val text = number(value, decimals)
        return if (withUnit) "$text ${unit.suffix}" else text
    }

    fun dimNoUnit(mm: Float, unit: UnitSystem): String = dim(mm, unit, withUnit = false)

    fun area(m2: Double): String = "${number(m2, 2)} m²"

    fun percent(value: Double, decimals: Int = 1): String = "${number(value, decimals)}%"

    fun shortDate(millis: Long): String =
        SimpleDateFormat("d MMM yyyy", idLocale).format(Date(millis))

    fun dateTime(millis: Long): String =
        SimpleDateFormat("d MMM yyyy • HH:mm", idLocale).format(Date(millis))

    fun relativeTime(millis: Long, now: Long = System.currentTimeMillis()): String {
        val diff = (now - millis).coerceAtLeast(0L)
        val minutes = diff / 60_000L
        val hours = diff / 3_600_000L
        val days = diff / 86_400_000L
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hr ago"
            days < 7 -> "$days d ago"
            else -> shortDate(millis)
        }
    }

    /** Parses both "1.200,5" and "1200.5" into a number. */
    fun parseNumber(text: String): Float? {
        var cleaned = text.trim().replace(" ", "")
        if (cleaned.isEmpty()) return null
        val hasComma = cleaned.contains(',')
        val hasDot = cleaned.contains('.')
        cleaned = when {
            hasComma && hasDot -> {
                if (cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.')) {
                    cleaned.replace(".", "").replace(',', '.')
                } else {
                    cleaned.replace(",", "")
                }
            }
            hasComma -> cleaned.replace(',', '.')
            else -> cleaned
        }
        return cleaned.toFloatOrNull()
    }

    fun furnitureLabel(type: FurnitureType): String = type.label

    fun fileStamp(millis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(millis))

    fun safeFileName(name: String): String =
        name.trim().replace(Regex("[^A-Za-z0-9 _-]"), "").replace(Regex("\\s+"), "_")
            .ifBlank { "PanelCut3D" }
}
