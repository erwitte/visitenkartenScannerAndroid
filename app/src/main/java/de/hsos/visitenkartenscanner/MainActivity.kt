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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import de.hsos.visitenkartenscanner.database.BusinessCard
import de.hsos.visitenkartenscanner.database.BusinessCardDatabase
import de.hsos.visitenkartenscanner.database.BusinessCardDao
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {
    private lateinit var database: BusinessCardDatabase
    private lateinit var businessCardDao: BusinessCardDao
    private val businessCards = mutableStateListOf<BusinessCard>()
    private var parsedString: Array<String> = Array(4){""}

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let { bitmap ->
                val rotatedBitmap = rotateBitmap(bitmap, -90f)
                val base64Image = encodeImageToBase64(rotatedBitmap)
                extractTextFromImage(rotatedBitmap)
                showEditableScreen(base64Image)
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
                    onBack = { selectedEntry = null }
                )
            }
        }
    }

    private fun showEditableScreen(imageBase64: String) {
        setContent {
            var name by remember { mutableStateOf(parsedString[0]) }
            var email by remember { mutableStateOf(parsedString[1]) }
            var phone by remember { mutableStateOf(parsedString[2]) }
            var address by remember { mutableStateOf(parsedString[3])}

            Column(modifier = Modifier.padding(16.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
                TextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") })
                TextField(value = address, onValueChange = { address = it}, label = { Text("Adress")})

                Button(onClick = {
                    saveBusinessCard(imageBase64, name, email, phone, address)
                    showMainScreen()
                }) {
                    Text("Save")
                }
            }
        }
    }

    private fun saveBusinessCard(
        imageBase64: String,
        name: String,
        email: String,
        phone: String,
        address: String
    ) {
        val coroutineScope = lifecycleScope
        coroutineScope.launch {
            val newCard = BusinessCard(
                name = name,
                phoneNumber = phone,
                email = email,
                imageBase64 = imageBase64,
                address = address
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

    private fun extractTextFromImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                Log.d("MLKit", "Extracted text: ${visionText.text}")
                parsedString = extractData(
                    """
                    Beyer
                    Dämmtechnik
                    Henning Beyer
                    Geschaftsfuhrer
                    Osloer Straße 21
                    49377 Vechta
                    GmbH
                    Tel. 0 44 41 / 889 93 40
                    Mobil 0172/431 49 24
                    info@beyer-daemmtechnik.de
                    www.bevem mtechnik.de
                    """.trimIndent()
                ).split(";").toTypedArray()
            }
            .addOnFailureListener { e ->
                Log.e("MLKit", "Text extraction failed", e)
            }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
