package de.hsos.visitenkartenscanner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.hsos.visitenkartenscanner.database.BusinessCard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntryListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun entryListScreenshowsEntries() {
        val sampleEntries = listOf(
            BusinessCard(name = "Alice", phoneNumber = "123", email = "alice@gmail.com", imageBase64 = "", address = "Strasse 1"),
            BusinessCard(name = "Bob", phoneNumber = "456", email = "bob@email.com", imageBase64 = "", address = "Strasse 2")
        )

        composeTestRule.setContent {
            EntryListScreen(
                entries = sampleEntries,
                onEntryClick = {},
                openCamera = {},
                deleteEntry = {}
            )
        }

        composeTestRule.onNodeWithText("Name: Alice").assertExists()
        composeTestRule.onNodeWithText("Name: Bob").assertExists()
    }

    @Test
    fun entryListScreenshowsNoEntriesTextWhenEmpty() {
        composeTestRule.setContent {
            EntryListScreen(
                entries = emptyList(),
                onEntryClick = {},
                openCamera = {},
                deleteEntry = {}
            )
        }

        composeTestRule.onNodeWithText("No entries").assertExists()
    }

    @Test
    fun entryCardclickable() {
        val sampleEntry = BusinessCard(name = "Charlie", phoneNumber = "789", email = "charlie@yahoo.com", imageBase64 = "", address = "Strasse 3")
        var clickedEntry: BusinessCard? = null

        composeTestRule.setContent {
            EntryCard(entry = sampleEntry, onEntryClick = { clickedEntry = it }, deleteEntry = {})
        }

        composeTestRule.onNodeWithText("Name: Charlie", useUnmergedTree = true).performClick()
        assert(clickedEntry == sampleEntry)
    }

    @Test
    fun entryCard_navigatesToDetailScreen() {
        val sampleEntry = BusinessCard(name = "David", phoneNumber = "999", email = "david@kray.de", imageBase64 = "", address = "Strasse 4")

        var selectedEntry: BusinessCard? = null

        composeTestRule.setContent {
            var currentEntry by remember { mutableStateOf<BusinessCard?>(null) }

            if (currentEntry == null) {
                EntryListScreen(
                    entries = listOf(sampleEntry),
                    onEntryClick = { currentEntry = it },
                    openCamera = {},
                    deleteEntry = {}
                )
            } else {
                EntryDetailsScreen(entry = currentEntry!!, onBack = { currentEntry = null })
            }
        }

        // Klick auf die EntryCard
        composeTestRule.onNodeWithText("Name: David", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        // Überprüfung, ob die Detailansicht sichtbar ist
        composeTestRule.onNodeWithText("Email: david@kray.de").assertExists()
        composeTestRule.onNodeWithText("Phone: 999").assertExists()
        composeTestRule.onNodeWithText("Address: Strasse 4").assertExists()
    }
}
