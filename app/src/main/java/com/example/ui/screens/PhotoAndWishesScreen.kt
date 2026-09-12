package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.SeedData
import com.example.data.SharedPhotoEntity
import com.example.data.WishEntity
import com.example.ui.EventViewModel
import com.example.ui.components.WishTickerBanner
import com.example.ui.theme.LaurelGold
import com.example.ui.theme.LaurelGoldDark
import com.example.ui.theme.NeuroDarkNavy
import com.example.ui.theme.NeuroPrimary
import com.example.ui.theme.SynapseCyan
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoAndWishesScreen(
    viewModel: EventViewModel,
    modifier: Modifier = Modifier
) {
    val wishes by viewModel.wishes.collectAsState()
    val tickerIndex by viewModel.tickerIndex.collectAsState()
    val photos by viewModel.photos.collectAsState()

    var selectedSubTab by remember { mutableStateOf(0) } // 0 = Auguri, 1 = Galleria Foto
    var showPostWishDialog by remember { mutableStateOf(false) }
    var showUploadPhotoDialog by remember { mutableStateOf(false) }
    var selectedPhotoForPreview by remember { mutableStateOf<SharedPhotoEntity?>(null) }

    // Android Photo Picker contract (compliance zero-permission)
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedImageUri = uri
            showUploadPhotoDialog = true
        }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedSubTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showPostWishDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Celebration, contentDescription = null) },
                    text = { Text("Scrivi un Augurio", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("post_wish_fab")
                )
            } else {
                ExtendedFloatingActionButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null) },
                    text = { Text("Carica Foto", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("upload_photo_fab")
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Bacheca Auguri & Momenti",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Condividi congratulazioni e scatti indimenticabili con tutti gli invitati",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Sub Tab Selector
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(
                    selectedTabIndex = selectedSubTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Auguri (${wishes.size})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Foto (${photos.size})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedSubTab == 0) {
                // Wishes view with live ticker spotlight and full feed
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        WishTickerBanner(
                            wishes = wishes,
                            currentIndex = tickerIndex,
                            onHeartWish = { viewModel.heartWish(it) },
                            onClickBanner = { }
                        )
                    }

                    item {
                        Text(
                            text = "Tutti i Messaggi di Auguri",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    items(wishes, key = { it.id }) { wish ->
                        WishCard(
                            wish = wish,
                            onHeart = { viewModel.heartWish(wish.id) }
                        )
                    }
                }
            } else {
                // Photos View
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(photos, key = { it.id }) { photo ->
                        PhotoGridItem(
                            photo = photo,
                            onLike = { viewModel.likePhoto(photo.id) },
                            onClick = { selectedPhotoForPreview = photo }
                        )
                    }
                }
            }
        }
    }

    if (showPostWishDialog) {
        PostWishDialog(
            onDismiss = { showPostWishDialog = false },
            onPost = { author, target, text, emoji ->
                viewModel.postWish(author, target, text, emoji)
                showPostWishDialog = false
            }
        )
    }

    if (showUploadPhotoDialog && pickedImageUri != null) {
        UploadPhotoDialog(
            imageUri = pickedImageUri.toString(),
            onDismiss = {
                showUploadPhotoDialog = false
                pickedImageUri = null
            },
            onUpload = { author, caption ->
                viewModel.uploadPhoto(author, caption, pickedImageUri.toString())
                showUploadPhotoDialog = false
                pickedImageUri = null
            }
        )
    }

    if (selectedPhotoForPreview != null) {
        PhotoPreviewDialog(
            photo = selectedPhotoForPreview!!,
            onDismiss = { selectedPhotoForPreview = null },
            onLike = {
                viewModel.likePhoto(selectedPhotoForPreview!!.id)
            }
        )
    }
}

@Composable
fun WishCard(
    wish: WishEntity,
    onHeart: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wish_card_${wish.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = wish.emojiBadge,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = wish.authorName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Per: ${wish.targetGraduate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = LaurelGoldDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF43F5E).copy(alpha = 0.1f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onHeart() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Cuore",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${wish.heartCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF43F5E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = wish.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PhotoGridItem(
    photo: SharedPhotoEntity,
    onLike: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("photo_item_${photo.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (photo.imageUri.isNotBlank()) {
                    AsyncImage(
                        model = photo.imageUri,
                        contentDescription = photo.caption,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Stylized celebration placeholder gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = when (photo.id % 3) {
                                        0L -> listOf(NeuroPrimary, SynapseCyan)
                                        1L -> listOf(LaurelGold, LaurelGoldDark)
                                        else -> listOf(Color(0xFF8B5CF6), NeuroPrimary)
                                    }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = when (photo.id % 3) {
                                    0L -> Icons.Default.School
                                    1L -> Icons.Default.Celebration
                                    else -> Icons.Default.AutoAwesome
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Neurologia 2026",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Like counter badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clickable { onLike() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${photo.likesCount}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = photo.authorName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = photo.caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostWishDialog(
    onDismiss: () -> Unit,
    onPost: (author: String, target: String, text: String, emoji: String) -> Unit
) {
    var author by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("Tutti i Laureandi") }
    var text by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🎓") }

    val graduates = listOf("Tutti i Laureandi") + SeedData.graduates
    val emojis = listOf("🎓", "🧠", "🥂", "❤️", "⚡", "⭐", "🎉")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Invia un Augurio ai Laureandi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Il tuo Nome *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wish_author_input")
                )

                Text("Destinatario:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(graduates) { grad ->
                        FilterChip(
                            selected = target == grad,
                            onClick = { target = grad },
                            label = { Text(grad, fontSize = 11.sp) }
                        )
                    }
                }

                Text("Scegli un'emoji celebrativa:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(emojis) { em ->
                        Surface(
                            shape = CircleShape,
                            color = if (selectedEmoji == em) LaurelGold.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedEmoji = em }
                        ) {
                            Text(
                                text = em,
                                fontSize = 20.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Messaggio di congratulazioni *") },
                    minLines = 3,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wish_message_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onPost(author, target, text, selectedEmoji)
                    }
                },
                enabled = text.isNotBlank(),
                modifier = Modifier.testTag("submit_wish_button")
            ) {
                Text("Pubblica Augurio")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
fun UploadPhotoDialog(
    imageUri: String,
    onDismiss: () -> Unit,
    onUpload: (author: String, caption: String) -> Unit
) {
    var author by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Condividi uno Scatto",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Nome di chi pubblica") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Didascalia / Momento della festa") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpload(author, caption) },
                modifier = Modifier.testTag("confirm_upload_photo_button")
            ) {
                Text("Condividi in Galleria")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
fun PhotoPreviewDialog(
    photo: SharedPhotoEntity,
    onDismiss: () -> Unit,
    onLike: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(Color.Black)
                ) {
                    if (photo.imageUri.isNotBlank()) {
                        AsyncImage(
                            model = photo.imageUri,
                            contentDescription = photo.caption,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(NeuroDarkNavy),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Celebration,
                                contentDescription = null,
                                tint = LaurelGold,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Color.White)
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Scatto di ${photo.authorName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = onLike,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${photo.likesCount}")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = photo.caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
