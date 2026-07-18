package com.personal.freelancingdocument.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.personal.freelancingdocument.data.model.MediaItem
import com.personal.freelancingdocument.data.model.MediaType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentEditScreen(
    isNew: Boolean,
    initialSubject: String,
    initialDescription: String,
    photos: List<MediaItem>,
    videos: List<MediaItem>,
    onSave: (subject: String, description: String) -> Unit,
    onAddPhoto: (Uri) -> Unit,
    onAddVideo: (Uri) -> Unit,
    onDeleteMedia: (MediaItem) -> Unit,
    onDeleteDocument: (() -> Unit)?,
    onBack: () -> Unit
) {
    var subject by remember { mutableStateOf(initialSubject) }
    var description by remember { mutableStateOf(initialDescription) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let(onAddPhoto) }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let(onAddVideo) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New Document" else "Edit Document") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isNew && onDeleteDocument != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    IconButton(onClick = { onSave(subject, description); onBack() }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )

            Spacer(Modifier.height(20.dp))
            Text("Photos", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            MediaRow(
                items = photos,
                addLabel = "Add photo",
                onAddClick = { photoPicker.launch("image/*") },
                onDeleteClick = onDeleteMedia
            )

            Spacer(Modifier.height(20.dp))
            Text("Videos", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            MediaRow(
                items = videos,
                addLabel = "Add video",
                onAddClick = { videoPicker.launch("video/*") },
                onDeleteClick = onDeleteMedia
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete document?") },
            text = { Text("This will remove it and its attached media from this device and your Drive.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteDocument?.invoke()
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MediaRow(
    items: List<MediaItem>,
    addLabel: String,
    onAddClick: () -> Unit,
    onDeleteClick: (MediaItem) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedCard(
                onClick = onAddClick,
                modifier = Modifier.size(96.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = addLabel)
                    }
                }
            }
        }
        items(items, key = { it.id }) { media ->
            Box(modifier = Modifier.size(96.dp)) {
                val model = media.localUri ?: media.driveDownloadUrl
                AsyncImage(
                    model = model,
                    contentDescription = media.fileName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                if (media.type == MediaType.VIDEO) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                IconButton(
                    onClick = { onDeleteClick(media) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = "Remove",
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
    }
}
