package com.panelcut3d.app

import com.panelcut3d.app.data.Fmt
import org.junit.Assert.assertEquals
import org.junit.Test

class AppCurrencyTest {

    @Test
    fun `USD is the default and converts the rupiah base`() {
        assertEquals("$20.00", Fmt.money(320_000.0))
        assertEquals("$2.50", Fmt.money(40_000.0))
        assertEquals("$58.13", Fmt.money(930_000.0))
    }

    @Test
    fun `local rupiah keeps whole-number shop prices`() {
        assertEquals("Rp 320,000", Fmt.money(320_000.0, "IDR"))
        assertEquals("Rp 40,000", Fmt.money(40_000.0, "IDR"))
    }

    @Test
    fun `currency conversion round trips without changing stored value`() {
        val original = 320_000.0
        val displayed = Fmt.fromIdr(original, "USD")
        assertEquals(original, Fmt.toIdr(displayed, "USD"), 0.001)
        assertEquals("€18.29", Fmt.money(original, "EUR"))
    }
}
