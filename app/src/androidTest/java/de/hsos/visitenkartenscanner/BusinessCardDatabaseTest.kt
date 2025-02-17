package de.hsos.visitenkartenscanner

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.hsos.visitenkartenscanner.database.BusinessCard
import de.hsos.visitenkartenscanner.database.BusinessCardDao
import de.hsos.visitenkartenscanner.database.BusinessCardDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class BusinessCardDatabaseTest {

    private lateinit var database: BusinessCardDatabase
    private lateinit var dao: BusinessCardDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context, BusinessCardDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.businessCardDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetBusinessCard() = runBlocking {
        val businessCard = BusinessCard(name = "Max Mustermann", phoneNumber = "123456789", email = "max@example.com", imageBase64 = "", address = "Musterstraße 1")
        dao.insert(businessCard)

        val allCards = dao.getAll()
        assertEquals(1, allCards.size)
        assertEquals("Max Mustermann", allCards[0].name)
    }

    @Test
    fun updateBusinessCard() = runBlocking {
        val businessCard = BusinessCard(name = "Max Mustermann", phoneNumber = "123456789", email = "max@example.com", imageBase64 = "", address = "Musterstraße 1")
        dao.insert(businessCard)

        val insertedCard = dao.getAll().first()
        val updatedCard = insertedCard.copy(name = "Erika Musterfrau")
        dao.update(updatedCard)

        val allCards = dao.getAll()
        assertEquals(1, allCards.size)
        assertEquals("Erika Musterfrau", allCards[0].name)
    }

    @Test
    fun deleteBusinessCard() = runBlocking {
        val businessCard = BusinessCard(name = "Max Mustermann", phoneNumber = "123456789", email = "max@example.com", imageBase64 = "", address = "Musterstraße 1")
        dao.insert(businessCard)

        val insertedCard = dao.getAll().first()
        dao.delete(insertedCard)

        val allCards = dao.getAll()
        assertTrue(allCards.isEmpty())
    }
}
