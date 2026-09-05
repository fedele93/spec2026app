package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EventNotificationEntity
import com.example.data.WishEntity
import com.example.ui.EventViewModel
import com.example.ui.MapPoint
import com.example.ui.components.InteractiveMapCanvas
import com.example.ui.components.NotificationsHistoryDialog
import com.example.ui.components.SendPushNotificationDialog
import com.example.ui.components.WishTickerBanner
import com.example.ui.theme.LaurelGold
import com.example.ui.theme.LaurelGoldDark
import com.example.ui.theme.NeuroDarkNavy
import com.example.ui.theme.NeuroPrimary
import com.example.ui.theme.SynapseCyan

@Composable
fun ProgramAndEventScreen(
    viewModel: EventViewModel,
    wishes: List<WishEntity>,
    tickerIndex: Int,
    notifications: List<EventNotificationEntity>,
    unreadCount: Int,
    onNavigateToWishes: () -> Unit,
    onNavigateToBus: () -> Unit,
    onNavigateToRsvp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedMapPoint by viewModel.selectedMapPoint.collectAsState()

    var showSendPushDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Hero Header Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("event_hero_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0A192F),
                                    Color(0xFF005FB0),
                                    Color(0xFF0284C7)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = LaurelGold,
                                contentColor = NeuroDarkNavy
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "SPECIALIZZAZIONE IN NEUROLOGIA",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            // Notification button with badge
                            Box {
                                IconButton(
                                    onClick = {
                                        showHistoryDialog = true
                                        viewModel.markNotificationsRead()
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                        .testTag("notification_history_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifiche",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Text("$unreadCount")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Festa di Laurea & Seduta Ufficiale",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Text(
                            text = "Dott. Andrea Riva • Dott.ssa Elena Moretti • Dott. Marco Ferri",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = LaurelGold,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = SynapseCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sabato 10 Ottobre",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = SynapseCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Padova & Colli",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Wishes Marquee Ticker
        item {
            WishTickerBanner(
                wishes = wishes,
                currentIndex = tickerIndex,
                onHeartWish = { viewModel.heartWish(it) },
                onClickBanner = onNavigateToWishes
            )
        }

        // Quick Push Notification Broadcaster Button
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSendPushDialog = true }
                    .testTag("broadcast_push_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SendTimeExtension,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Invia Notifica Push agli Invitati",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Invia aggiornamenti in tempo reale su seduta, navetta o festa con notifica di sistema Android.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Program Timeline Section
        item {
            Text(
                text = "Programma dell'Evento",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TimelineItem(
                        time = "10:30",
                        title = "Discussione Tesi di Specializzazione",
                        location = "Aula Magna Clinica Neurologica - Policlinico",
                        details = "I candidati presenteranno i risultati delle loro ricerche cliniche davanti alla commissione presieduta dal Direttore della Scuola.",
                        icon = Icons.Default.School,
                        accentColor = NeuroPrimary,
                        isLast = false
                    )

                    TimelineItem(
                        time = "13:00",
                        title = "Proclamazione Solenne & Brindisi Accademico",
                        location = "Chiostro Storico / Ingresso Clinica Neurologica",
                        details = "Consegna dei diplomi, tocco accademico, corona d'alloro e foto di rito con colleghi, docenti e parenti.",
                        icon = Icons.Default.Celebration,
                        accentColor = LaurelGold,
                        isLast = false
                    )

                    TimelineItem(
                        time = "18:30",
                        title = "Ritrovo & Imbarco Autobus Navetta",
                        location = "Piazzale Principale Policlinico",
                        details = "Partenza puntuale ore 18:45 con transfer riservato 54 posti verso Villa Delle Rose. Nessun problema di guida o parcheggio.",
                        icon = Icons.Default.DirectionsBus,
                        accentColor = SynapseCyan,
                        isLast = false,
                        actionLabel = "Prenota Posto",
                        onAction = onNavigateToBus
                    )

                    TimelineItem(
                        time = "19:30",
                        title = "Festa, Cena a Buffet & Cocktail Bar",
                        location = "Villa Delle Rose - Ricevimenti & Lounge Garden",
                        details = "Sunset cocktail nel parco della villa, aperitivi gourmet, cena placé e open bar riservato.",
                        icon = Icons.Default.Nightlife,
                        accentColor = Color(0xFF8B5CF6),
                        isLast = false
                    )

                    TimelineItem(
                        time = "23:00",
                        title = "Taglio della Torta Monumentale & Dj Set",
                        location = "Area Piscina & Dance Floor",
                        details = "Taglio della torta di specializzazione, video celebrativo a sorpresa, musica e balli fino a tarda notte.",
                        icon = Icons.Default.Cake,
                        accentColor = Color(0xFFF43F5E),
                        isLast = true
                    )
                }
            }
        }

        // Interactive Map Section
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Mappa Interattiva Punti di Ritrovo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        item {
            InteractiveMapCanvas(
                points = viewModel.mapPoints,
                selectedPoint = selectedMapPoint,
                onSelectPoint = { viewModel.setSelectedMapPoint(it) }
            )
        }
    }

    if (showSendPushDialog) {
        SendPushNotificationDialog(
            onDismiss = { showSendPushDialog = false },
            onSendNotification = { title, body, category ->
                viewModel.sendBroadcastNotification(context, title, body, category)
            }
        )
    }

    if (showHistoryDialog) {
        NotificationsHistoryDialog(
            notifications = notifications,
            onDismiss = { showHistoryDialog = false }
        )
    }
}

@Composable
fun TimelineItem(
    time: String,
    title: String,
    location: String,
    details: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isLast: Boolean,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Time & vertical line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(54.dp)
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(72.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (actionLabel != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "$actionLabel →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }
    }
}
