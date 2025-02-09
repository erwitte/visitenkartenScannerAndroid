package de.hsos.visitenkartenscanner

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.core.app.ActivityCompat.startActivityForResult


// Data class representing an entry
data class Entry(val name: String, val email: String, val phone: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EntryListScreen(::openCamera)
        }
    }
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(this, intent, 100, null)
    }
}

@Composable
fun EntryListScreen(openCamera: () -> Unit) {
    val entries = remember { mutableStateListOf<Entry>() } // Placeholder for database entries

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
fun EntryCard(entry: Entry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = entry.name, fontSize = 18.sp, color = Color.Black)
            Text(text = entry.email, fontSize = 14.sp, color = Color.DarkGray)
            Text(text = entry.phone, fontSize = 14.sp, color = Color.DarkGray)
        }
    }
}
