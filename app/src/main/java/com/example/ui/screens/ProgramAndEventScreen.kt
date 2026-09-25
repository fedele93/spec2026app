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
import com.example.data.SeedData
import com.example.data.WishEntity
import com.example.ui.EventViewModel
import com.example.ui.MapPoint
import com.example.ui.components.InteractiveMapCanvas
import com.example.ui.components.NotificationsHistoryDialog
import com.example.ui.components.SendPushNotificationDialog
import com.example.ui.components.ServerSettingsDialog
import com.example.ui.components.WishTickerBanner
import com.example.ui.theme.LaurelGold
import com.example.ui.theme.LaurelGoldDark
import com.example.ui.theme.NeuroDarkNavy
import com.example.ui.theme.NeuroPrimary
import com.example.ui.theme.SynapseCyan
import com.example.util.CalendarHelper

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
    var showServerSettings by remember { mutableStateOf(false) }
    val serverStatus by viewModel.serverStatus.collectAsState()

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
                                        text = SeedData.programBadge.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            // Server settings button (backend condiviso)
                            IconButton(
                                onClick = { showServerSettings = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                    .testTag("server_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Impostazioni server",
                                    tint = if (serverStatus.configured && !serverStatus.online) LaurelGold else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = SeedData.programTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Text(
                            text = SeedData.programSubtitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = LaurelGold,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = SeedData.graduates.joinToString(" • "),
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 8.dp)
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
                                    text = SeedData.programDateLabel,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = SynapseCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = SeedData.programLocationLabel,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Aggiungi seduta e festa al calendario del telefono (Intent di sistema)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            val sch = SeedData.schedule
                            val seduta = SeedData.mapPoints.firstOrNull { it.id == "seduta" }
                            val festa = SeedData.mapPoints.firstOrNull { it.id == "festa" }
                            CalendarChip(
                                label = "📅 Seduta 9 nov",
                                tag = "calendar_ceremony_button"
                            ) {
                                CalendarHelper.addEvent(
                                    context,
                                    title = "Seduta di Specializzazione in Neurologia",
                                    location = seduta?.address ?: SeedData.programLocationLabel,
                                    description = seduta?.description ?: "",
                                    dateIso = sch.ceremonyDate,
                                    time = sch.ceremonyTime,
                                    durationHours = 3
                                )
                            }
                            CalendarChip(
                                label = "📅 Festa 13 nov",
                                tag = "calendar_party_button"
                            ) {
                                CalendarHelper.addEvent(
                                    context,
                                    title = "Festa di Specializzazione in Neurologia",
                                    location = festa?.address ?: "",
                                    description = festa?.description ?: "",
                                    dateIso = sch.partyDate,
                                    time = sch.partyTime,
                                    durationHours = 5
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
                            text = when {
                                !serverStatus.configured -> "Modalità locale: la notifica viene mostrata solo su questo telefono. Configura il server in ⚙️ per raggiungere tutti."
                                viewModel.isAdmin -> "Invia aggiornamenti su seduta, navetta o festa: arrivano in push a tutti gli invitati (app e web)."
                                else -> "Serve il token organizzatore (⚙️ Impostazioni) per inviare notifiche a tutti gli invitati."
                            },
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
                    val tlIcons = listOf(
                        Icons.Default.School,
                        Icons.Default.Celebration,
                        Icons.Default.DirectionsBus,
                        Icons.Default.Nightlife,
                        Icons.Default.Cake
                    )
                    val tlColors = listOf(
                        NeuroPrimary,
                        LaurelGold,
                        SynapseCyan,
                        Color(0xFF8B5CF6),
                        Color(0xFFF43F5E)
                    )
                    SeedData.programTimeline.forEachIndexed { i, entry ->
                        TimelineItem(
                            time = entry.time,
                            title = entry.title,
                            location = entry.location,
                            details = entry.details,
                            icon = tlIcons.getOrElse(i) { Icons.Default.School },
                            accentColor = tlColors.getOrElse(i) { NeuroPrimary },
                            isLast = i == SeedData.programTimeline.lastIndex,
                            actionLabel = if (entry.title.contains("Navetta", ignoreCase = true)) "Prenota Posto" else null,
                            onAction = if (entry.title.contains("Navetta", ignoreCase = true)) onNavigateToBus else null
                        )
                    }
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

    if (showServerSettings) {
        ServerSettingsDialog(
            status = serverStatus,
            currentUrl = viewModel.serverUrl,
            currentToken = viewModel.adminToken,
            onSave = { url, token ->
                viewModel.updateServerSettings(url, token)
                showServerSettings = false
            },
            onDismiss = { showServerSettings = false }
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

@Composable
private fun CalendarChip(label: String, tag: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}
