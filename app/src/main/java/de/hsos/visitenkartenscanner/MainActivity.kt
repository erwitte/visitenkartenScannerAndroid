package de.hsos.visitenkartenscanner

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import de.hsos.visitenkartenscanner.database.BusinessCard
import de.hsos.visitenkartenscanner.database.BusinessCardDatabase
import de.hsos.visitenkartenscanner.database.BusinessCardDao
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {
    private lateinit var database: BusinessCardDatabase
    private lateinit var businessCardDao: BusinessCardDao
    private val businessCards = mutableStateListOf<BusinessCard>()

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let { bitmap ->
                val base64Image = encodeImageToBase64(bitmap)
                saveBusinessCard(base64Image)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = BusinessCardDatabase.getDatabase(this)
        businessCardDao = database.businessCardDao()

        setContent {
            val coroutineScope = rememberCoroutineScope()
            var selectedEntry by remember { mutableStateOf<BusinessCard?>(null) }

            LaunchedEffect(Unit) {
                coroutineScope.launch {
                    val storedCards = businessCardDao.getAll()
                    businessCards.clear()
                    businessCards.addAll(storedCards)
                }
            }

            if (selectedEntry == null) {
                EntryListScreen(
                    entries = businessCards,
                    onEntryClick = { entry -> selectedEntry = entry },
                    openCamera = { openCamera() },
                    deleteEntry = { entry -> deleteBusinessCard(entry) }
                )
            } else {
                EntryDetailsScreen(
                    entry = selectedEntry!!,
                    onBack = { selectedEntry = null }
                )
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private fun saveBusinessCard(imageBase64: String) {
        val coroutineScope = lifecycleScope
        coroutineScope.launch {
            val newCard = BusinessCard(
                name = "w",
                phoneNumber = "w",
                email = "w",
                imageBase64 = imageBase64
            )
            businessCardDao.insert(newCard)

            val updatedCards = businessCardDao.getAll()
            businessCards.clear()
            businessCards.addAll(updatedCards)
        }
    }

    private fun deleteBusinessCard(card: BusinessCard) {
        val coroutineScope = lifecycleScope
        coroutineScope.launch {
            businessCardDao.delete(card)
            val updatedCards = businessCardDao.getAll()
            businessCards.clear()
            businessCards.addAll(updatedCards)
        }
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}
