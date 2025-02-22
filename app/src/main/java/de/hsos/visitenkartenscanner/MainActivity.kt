package de.hsos.visitenkartenscanner

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import de.hsos.visitenkartenscanner.database.BusinessCard
import de.hsos.visitenkartenscanner.database.BusinessCardDatabase
import de.hsos.visitenkartenscanner.database.BusinessCardDao
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import okhttp3.*
import com.google.gson.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import extractData
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.IOException

class MainActivity : ComponentActivity() {
    private lateinit var database: BusinessCardDatabase
    private lateinit var businessCardDao: BusinessCardDao
    private val businessCards = mutableStateListOf<BusinessCard>()
    private var parsedString: String = ""
    private lateinit var splitParsedString: Array<String>

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let { bitmap ->
                val rotatedBitmap = rotateBitmap(bitmap, -90f)
                val base64Image = encodeImageToBase64(rotatedBitmap)
                lifecycleScope.launch {
                    parsedString = extractDataFromImage(rotatedBitmap)
                    val parts = parsedString.split(";")
                    if (parts.size == 4){
                        showEditableScreen(base64Image, parts)
                    } else {
                        showErrorScreen()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = BusinessCardDatabase.getDatabase(this)
        businessCardDao = database.businessCardDao()

        showMainScreen()
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private fun showErrorScreen() {
        setContent {
            ErrorScreen { showMainScreen() }
        }
    }

    private fun showMainScreen() {
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
                    onBack = { selectedEntry = null },
                    onSave = { updatedName, updatedEmail, updatedPhone, updatedAddress ->
                        saveBusinessCard(
                            selectedEntry!!.imageBase64,
                            updatedName,
                            updatedEmail,
                            updatedPhone,
                            updatedAddress,
                            selectedEntry
                        )
                        selectedEntry = null  // Nach dem Speichern zurück zur Liste
                    }
                )
            }
        }
    }


    private fun showEditableScreen(imageBase64: String, parts: List<String>) {
        val name = parts.getOrNull(0) ?: ""
        val email = parts.getOrNull(1) ?: ""
        val phone = parts.getOrNull(2) ?: ""
        val address = parts.getOrNull(3) ?: ""

        setContent {
            BusinessCardEditor(
                imageBase64 = imageBase64,
                initialName = name,
                initialEmail = email,
                initialPhone = phone,
                initialAddress = address,
                onSave = { updatedName, updatedEmail, updatedPhone, updatedAddress ->
                    saveBusinessCard(imageBase64, updatedName, updatedEmail, updatedPhone, updatedAddress)
                    showMainScreen()
                }
            )
        }
    }


    private fun saveBusinessCard(
        imageBase64: String,
        name: String,
        email: String,
        phone: String,
        address: String,
        existingCard: BusinessCard? = null
    ) {
        val coroutineScope = lifecycleScope
        coroutineScope.launch {
            val updatedCard = existingCard?.copy(
                name = name,
                phoneNumber = phone,
                email = email,
                imageBase64 = imageBase64,
                address = address
            ) ?: BusinessCard(
                name = name,
                phoneNumber = phone,
                email = email,
                imageBase64 = imageBase64,
                address = address
            )

            if (existingCard == null) {
                businessCardDao.insert(updatedCard) // Neuer Eintrag
            } else {
                businessCardDao.update(updatedCard) // Bestehenden Eintrag aktualisieren
            }

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

    private suspend fun extractDataFromImage(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    lifecycleScope.launch {
                        val extractedText = extractData(visionText.text)
                        continuation.resume(extractedText)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MLKit", "Text extraction failed", e)
                    continuation.resume("")
                }
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}