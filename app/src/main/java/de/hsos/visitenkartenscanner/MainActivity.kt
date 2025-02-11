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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // **📌 Register camera launcher BEFORE onCreate() is called**
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let { bitmap ->
                val base64Image = encodeImageToBase64(bitmap)
                saveBusinessCard(base64Image) // Save and update UI
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Room database
        database = BusinessCardDatabase.getDatabase(this)
        businessCardDao = database.businessCardDao()

        setContent {
            val coroutineScope = rememberCoroutineScope()

            // Load database entries when the screen is displayed
            LaunchedEffect(Unit) {
                coroutineScope.launch {
                    val storedCards = businessCardDao.getAll()
                    businessCards.clear()
                    businessCards.addAll(storedCards)
                }
            }

            EntryListScreen(
                entries = businessCards,
                openCamera = { openCamera() }
            )
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

            // Refresh list from database
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

@Composable
fun EntryListScreen(entries: List<BusinessCard>, openCamera: () -> Unit) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openCamera() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("+", fontSize = 24.sp, color = Color.White)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (entries.isEmpty()) {
                Text("No entries", fontSize = 20.sp, color = Color.Gray)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(entries) { entry ->
                        EntryCard(entry)
                    }
                }
            }
        }
    }
}

@Composable
fun EntryCard(entry: BusinessCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Name: ${entry.name}", fontSize = 18.sp, color = Color.Black)
            Text(text = "Email: ${entry.email}", fontSize = 14.sp, color = Color.DarkGray)
            Text(text = "Phone: ${entry.phoneNumber}", fontSize = 14.sp, color = Color.DarkGray)
        }
    }
}
