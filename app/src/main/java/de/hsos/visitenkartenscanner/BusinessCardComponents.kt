package de.hsos.visitenkartenscanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.hsos.visitenkartenscanner.database.BusinessCard

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
fun ErrorScreen(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Error: Unable to extract all required fields!", color = Color.Red)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onDismiss) {
            Text("Okay")
        }
    }
}

@Composable
fun BusinessCardEditor(
    imageBase64: String,
    initialName: String,
    initialEmail: String,
    initialPhone: String,
    initialAddress: String,
    onSave: (String, String, String, String) -> Unit
) {
    var nameState by remember { mutableStateOf(initialName) }
    var emailState by remember { mutableStateOf(initialEmail) }
    var phoneState by remember { mutableStateOf(initialPhone) }
    var addressState by remember { mutableStateOf(initialAddress) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = nameState,
                onValueChange = { nameState = it },
                label = { Text("Name") }
            )
            TextField(
                value = emailState,
                onValueChange = { emailState = it },
                label = { Text("Email") }
            )
            TextField(
                value = phoneState,
                onValueChange = { phoneState = it },
                label = { Text("Phone") }
            )
            TextField(
                value = addressState,
                onValueChange = { addressState = it },
                label = { Text("Address") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { onSave(nameState, emailState, phoneState, addressState) }) {
                Text("Save")
            }
        }
    }
}


@Composable
fun EntryCard(entry: BusinessCard, onEntryClick: (BusinessCard) -> Unit, deleteEntry: (BusinessCard) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

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
                Text(text = "Address: ${entry.address}", fontSize = 14.sp)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Red, shape = CircleShape)
                    .clickable { showDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Text("X", color = Color.White, fontSize = 20.sp)
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this entry?") },
            confirmButton = {
                Button(onClick = {
                    deleteEntry(entry)
                    showDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
        Text(text = "Address: ${entry.address}", fontSize = 18.sp )

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
