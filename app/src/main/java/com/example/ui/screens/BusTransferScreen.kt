package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.BusBookingEntity
import com.example.data.SeedData
import com.example.ui.EventViewModel
import com.example.ui.theme.LaurelGold
import com.example.ui.theme.NeuroDarkNavy
import com.example.ui.theme.NeuroPrimary
import com.example.ui.theme.SynapseCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusTransferScreen(
    viewModel: EventViewModel,
    modifier: Modifier = Modifier
) {
    val bookings by viewModel.busBookings.collectAsState()
    val bookedSeats by viewModel.bookedSeatsCount.collectAsState()
    val maxSeats = viewModel.maxBusSeats
    val availableSeats = maxOf(0, maxSeats - bookedSeats)

    var showBookDialog by remember { mutableStateOf(false) }
    var bookingErrorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showBookDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.AddRoad, contentDescription = null) },
                text = { Text("Aderisci all'Autobus", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("book_bus_fab")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Navetta Autobus Riservata",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = SeedData.busScheduleSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Seat Capacity Meter Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(SynapseCyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsBus,
                                        contentDescription = null,
                                        tint = SynapseCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Pullman Gran Turismo",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Capienza totale: $maxSeats posti",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (availableSeats > 10) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "$availableSeats liberi",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (availableSeats > 10) Color(0xFF047857) else Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Progress Bar
                        val progress = (bookedSeats.toFloat() / maxSeats.toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = if (progress > 0.9f) Color(0xFFEF4444) else SynapseCyan,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Adesioni raccolte: $bookedSeats posti",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(progress * 100).toInt()}% occupato",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Transfer Schedule & Info
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Orari & Fermate Transfer",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        BusScheduleRow(
                            time = SeedData.busAndata.timeLabel,
                            direction = "ANDATA",
                            from = SeedData.busAndata.from,
                            to = SeedData.busAndata.to,
                            notes = SeedData.busAndata.notes
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(10.dp))

                        BusScheduleRow(
                            time = SeedData.busRitorno.timeLabel,
                            direction = "RITORNO",
                            from = SeedData.busRitorno.from,
                            to = SeedData.busRitorno.to,
                            notes = SeedData.busRitorno.notes
                        )
                    }
                }
            }

            // Passenger List Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Elenco Passeggeri Prenotati (${bookings.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Passenger Bookings
            if (bookings.isEmpty()) {
                item {
                    Text(
                        text = "Nessun passeggero ha ancora prenotato la navetta.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(bookings, key = { it.id }) { booking ->
                    PassengerBookingCard(
                        booking = booking,
                        onCancel = { viewModel.cancelBusBooking(booking) }
                    )
                }
            }
        }
    }

    if (showBookDialog) {
        BookBusDialog(
            availableSeats = availableSeats,
            onDismiss = {
                showBookDialog = false
                bookingErrorMessage = null
            },
            onConfirm = { name, seats, stop, returnTrip, phone, notes ->
                val success = viewModel.bookBus(name, seats, stop, returnTrip, phone, notes)
                if (success) {
                    showBookDialog = false
                    bookingErrorMessage = null
                } else {
                    bookingErrorMessage = "Posti non sufficienti (disponibili solo $availableSeats posti)."
                }
            },
            errorMessage = bookingErrorMessage
        )
    }
}

@Composable
fun BusScheduleRow(
    time: String,
    direction: String,
    from: String,
    to: String,
    notes: String
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (direction == "ANDATA") SynapseCyan.copy(alpha = 0.2f) else LaurelGold.copy(alpha = 0.2f)
        ) {
            Text(
                text = direction,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (direction == "ANDATA") Color(0xFF0284C7) else Color(0xFFB45309),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = time,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$from ➔ $to",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PassengerBookingCard(
    booking: BusBookingEntity,
    onCancel: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("passenger_card_${booking.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SynapseCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${booking.seatsCount}",
                    fontWeight = FontWeight.ExtraBold,
                    color = SynapseCyan,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = booking.passengerName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Fermata: ${booking.pickupStop}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (booking.returnTripWanted) {
                    Text(
                        text = "Include corsa di rientro notturna",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Medium
                    )
                }
                if (booking.notes.isNotBlank()) {
                    Text(
                        text = "Note: ${booking.notes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Cancella prenotazione",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookBusDialog(
    availableSeats: Int,
    onDismiss: () -> Unit,
    onConfirm: (name: String, seats: Int, stop: String, returnTrip: Boolean, phone: String, notes: String) -> Unit,
    errorMessage: String?
) {
    var name by remember { mutableStateOf("") }
    var seats by remember { mutableStateOf("1") }
    var stop by remember { mutableStateOf(SeedData.busPickupStops.firstOrNull() ?: "") }
    var returnTrip by remember { mutableStateOf(true) }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val stops = SeedData.busPickupStops

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Prenota Posto Autobus Navetta",
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
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome e Cognome passeggero *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bus_passenger_name_input")
                )

                OutlinedTextField(
                    value = seats,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) seats = it },
                    label = { Text("Numero di posti richiesti") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bus_seats_count_input")
                )

                Text("Fermata di Salita:", style = MaterialTheme.typography.labelMedium)
                stops.forEach { s ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { stop = s }
                    ) {
                        RadioButton(selected = stop == s, onClick = { stop = s })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(s, fontSize = 12.sp)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { returnTrip = !returnTrip }
                ) {
                    Checkbox(checked = returnTrip, onCheckedChange = { returnTrip = it })
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Richiedo anche il transfer di rientro notturno", fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefono per comunicazioni corsa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note o esigenze speciali") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = seats.toIntOrNull() ?: 1
                    if (name.isNotBlank()) {
                        onConfirm(name, count, stop, returnTrip, phone, notes)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_bus_booking_button")
            ) {
                Text("Conferma Adesione")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}
