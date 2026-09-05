package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GuestEntity
import com.example.data.RsvpStatus
import com.example.ui.EventViewModel
import com.example.ui.theme.LaurelGold
import com.example.ui.theme.NeuroDarkNavy
import com.example.ui.theme.NeuroPrimary
import com.example.ui.theme.SynapseCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestsRsvpScreen(
    viewModel: EventViewModel,
    modifier: Modifier = Modifier
) {
    val allGuests by viewModel.allGuests.collectAsState()
    val filteredGuests by viewModel.filteredGuests.collectAsState()
    val activeFilter by viewModel.guestFilter.collectAsState()
    val searchQuery by viewModel.guestSearch.collectAsState()

    var showAddGuestDialog by remember { mutableStateOf(false) }

    // Calculated metrics
    val confirmedGuests = allGuests.filter { it.rsvpStatus == RsvpStatus.CONFIRMED }
    val totalConfirmedPeople = confirmedGuests.sumOf { it.guestsCount }
    val pendingGuests = allGuests.filter { it.rsvpStatus == RsvpStatus.PENDING }
    val declinedGuests = allGuests.filter { it.rsvpStatus == RsvpStatus.DECLINED }
    val specialDietCount = confirmedGuests.count { it.dietaryNotes.isNotBlank() && !it.dietaryNotes.contains("Nessuna", ignoreCase = true) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddGuestDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_guest_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Aggiungi Invitato")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            // Header stats cards
            item {
                Column {
                    Text(
                        text = "Gestione Invitati & RSVP",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Monitoraggio in tempo reale delle presenze e coperti per la festa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Real-time summary grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RsvpSummaryCard(
                        title = "Coperti Totali",
                        value = "$totalConfirmedPeople",
                        subtitle = "${confirmedGuests.size} invitati",
                        accentColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    RsvpSummaryCard(
                        title = "In Attesa",
                        value = "${pendingGuests.size}",
                        subtitle = "da confermare",
                        accentColor = LaurelGold,
                        modifier = Modifier.weight(1f)
                    )
                    RsvpSummaryCard(
                        title = "Menu Speciali",
                        value = "$specialDietCount",
                        subtitle = "intolleranze/diete",
                        accentColor = SynapseCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setGuestSearch(it) },
                    placeholder = { Text("Cerca invitato o categoria...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setGuestSearch("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Cancella")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("guest_search_input")
                )
            }

            // Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = activeFilter == null,
                            onClick = { viewModel.setGuestFilter(null) },
                            label = { Text("Tutti (${allGuests.size})") },
                            leadingIcon = if (activeFilter == null) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                    item {
                        FilterChip(
                            selected = activeFilter == RsvpStatus.CONFIRMED,
                            onClick = { viewModel.setGuestFilter(RsvpStatus.CONFIRMED) },
                            label = { Text("Confermati (${confirmedGuests.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF047857)
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = activeFilter == RsvpStatus.PENDING,
                            onClick = { viewModel.setGuestFilter(RsvpStatus.PENDING) },
                            label = { Text("In Attesa (${pendingGuests.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = activeFilter == RsvpStatus.DECLINED,
                            onClick = { viewModel.setGuestFilter(RsvpStatus.DECLINED) },
                            label = { Text("Declinati (${declinedGuests.size})") }
                        )
                    }
                }
            }

            // Guest items
            if (filteredGuests.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PersonSearch,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Nessun invitato corrisponde ai filtri",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(filteredGuests, key = { it.id }) { guest ->
                    GuestItemCard(
                        guest = guest,
                        onStatusChange = { newStatus ->
                            viewModel.updateGuestStatus(guest, newStatus)
                        },
                        onDelete = {
                            viewModel.deleteGuest(guest)
                        }
                    )
                }
            }
        }
    }

    if (showAddGuestDialog) {
        AddGuestDialog(
            onDismiss = { showAddGuestDialog = false },
            onAdd = { name, cat, status, count, dietary, contact ->
                viewModel.addGuest(name, cat, status, count, dietary, contact)
                showAddGuestDialog = false
            }
        )
    }
}

@Composable
fun RsvpSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun GuestItemCard(
    guest: GuestEntity,
    onStatusChange: (RsvpStatus) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("guest_card_${guest.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = guest.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeuroPrimary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = guest.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuroPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (guest.guestsCount > 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = LaurelGold.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${guest.guestsCount} Persone",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF92400E),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Delete guest icon
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Rimuovi invitato",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (guest.dietaryNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = SynapseCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Dieta/Note: ${guest.dietaryNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (guest.contactInfo.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ContactPhone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = guest.contactInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // RSVP Status Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusButton(
                    label = "Confermato",
                    isSelected = guest.rsvpStatus == RsvpStatus.CONFIRMED,
                    selectedColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChange(RsvpStatus.CONFIRMED) }
                )
                StatusButton(
                    label = "In Attesa",
                    isSelected = guest.rsvpStatus == RsvpStatus.PENDING,
                    selectedColor = LaurelGold,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChange(RsvpStatus.PENDING) }
                )
                StatusButton(
                    label = "Declinato",
                    isSelected = guest.rsvpStatus == RsvpStatus.DECLINED,
                    selectedColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChange(RsvpStatus.DECLINED) }
                )
            }
        }
    }
}

@Composable
fun StatusButton(
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) selectedColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (isSelected) BorderStroke(1.dp, selectedColor.copy(alpha = 0.5f)) else null,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGuestDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, cat: String, status: RsvpStatus, count: Int, dietary: String, contact: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Colleghi Reparto") }
    var count by remember { mutableStateOf("1") }
    var dietary by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(RsvpStatus.CONFIRMED) }

    val categories = listOf("Colleghi Reparto", "Docenti & Medici", "Famigliari", "Amici Università", "Specializzandi")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Aggiungi Nuovo Invitato",
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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome e Cognome *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_guest_name_input")
                )

                // Category chips
                Text("Categoria / Gruppo:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = count,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) count = it },
                        label = { Text("N. Persone (Coperti)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = dietary,
                    onValueChange = { dietary = it },
                    label = { Text("Esigenze Alimentari (Celiaco, Veg...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contatto (Telefono o Email)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val parsedCount = count.toIntOrNull() ?: 1
                        onAdd(name, category, status, parsedCount, dietary, contact)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_guest_button")
            ) {
                Text("Salva Invitato")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}
