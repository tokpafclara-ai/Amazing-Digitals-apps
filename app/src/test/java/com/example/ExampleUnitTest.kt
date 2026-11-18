package com.example

import com.example.data.SpreadsheetEngine
import com.example.data.SubscriptionDefaults
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun spreadsheetFormulas_evaluateCorrectly() {
        val cells = mapOf(
            "A1" to "Product",
            "B1" to "Revenue",
            "C1" to "Cost",
            "A2" to "Word Suite",
            "B2" to "1000",
            "C2" to "400",
            "D2" to "=B2-C2",
            "A3" to "Sheets Pro",
            "B3" to "2000",
            "C3" to "500",
            "D3" to "=B3-C3",
            "D4" to "=SUM(D2:D3)",
            "D5" to "=AVERAGE(D2:D3)",
            "E2" to "=IF(D2>500,\"Profitable\",\"BreakEven\")",
            "E3" to "=MAX(B2:B3)",
            "E4" to "=MIN(C2:C3)"
        )

        assertEquals("600", SpreadsheetEngine.evaluateCell("D2", cells))
        assertEquals("1500", SpreadsheetEngine.evaluateCell("D3", cells))
        assertEquals("2100", SpreadsheetEngine.evaluateCell("D4", cells))
        assertEquals("1050", SpreadsheetEngine.evaluateCell("D5", cells))
        assertEquals("Profitable", SpreadsheetEngine.evaluateCell("E2", cells))
        assertEquals("2000", SpreadsheetEngine.evaluateCell("E3", cells))
        assertEquals("400", SpreadsheetEngine.evaluateCell("E4", cells))
    }

    @Test
    fun subscriptionDefaults_returnsExpectedQuotas() {
        assertEquals(52_428_800L, SubscriptionDefaults.quotaBytesForTier("FREE"))
        assertEquals(1_073_741_824L, SubscriptionDefaults.quotaBytesForTier("BASIC"))
        assertEquals(5_368_709_120L, SubscriptionDefaults.quotaBytesForTier("PRO"))
        assertEquals(26_843_545_600L, SubscriptionDefaults.quotaBytesForTier("PREMIUM"))
    }
}
