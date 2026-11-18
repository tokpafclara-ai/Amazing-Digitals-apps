package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PresentationEngine
import com.example.data.SpreadsheetEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Amazing Office", appName)
    }

    @Test
    fun presentationAndSpreadsheetJsonSerialization_worksWithAndroidJson() {
        val deckJson = PresentationEngine.createDefaultPresentationJson("Amazing Pitch")
        val parsedDeck = PresentationEngine.parseDeck(deckJson)
        assertEquals("NAVY_GOLD", parsedDeck.themeId)
        assertTrue(parsedDeck.slides.isNotEmpty())
        assertEquals("Amazing Pitch", parsedDeck.slides.first().title)

        val sheetsJson = SpreadsheetEngine.createDefaultBudgetJson()
        val parsedSheets = SpreadsheetEngine.parseSheets(sheetsJson)
        assertTrue(parsedSheets.isNotEmpty())
        assertEquals("Q4 Financials", parsedSheets.first().name)
    }
}
