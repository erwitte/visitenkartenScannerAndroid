package de.hsos.visitenkartenscanner

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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

@Composable
fun EntryListScreen(
    entries: List<BusinessCard>,
    onEntryClick: (BusinessCard) -> Unit,
    openCamera: () -> Unit,
    deleteEntry: (BusinessCard) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openCamera() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("+")
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
                Text("No entries", fontSize = 20.sp)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(entries) { entry ->
                        EntryCard(entry, onEntryClick, deleteEntry)
                    }
                }
            }
        }
    }
}

@Composable
fun EntryCard(entry: BusinessCard, onEntryClick: (BusinessCard) -> Unit, deleteEntry: (BusinessCard) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onEntryClick(entry) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Name: ${entry.name}", fontSize = 18.sp)
                Text(text = "Email: ${entry.email}", fontSize = 14.sp)
                Text(text = "Phone: ${entry.phoneNumber}", fontSize = 14.sp)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Red, shape = CircleShape)
                    .clickable { deleteEntry(entry) },
                contentAlignment = Alignment.Center
            ) {
                Text("X", color = Color.White, fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun EntryDetailsScreen(entry: BusinessCard, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Name: ${entry.name}", fontSize = 24.sp)
        Text(text = "Email: ${entry.email}", fontSize = 18.sp)
        Text(text = "Phone: ${entry.phoneNumber}", fontSize = 18.sp)

        Spacer(modifier = Modifier.height(16.dp))

        val bitmap = remember(entry.imageBase64) { decodeBase64ToBitmap(entry.imageBase64) }
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Business Card Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onBack) {
            Text("Back to List")
        }
    }
}

fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
