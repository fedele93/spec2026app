package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.EventViewModel
import com.example.ui.screens.*
import com.example.ui.theme.LaurelGold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeuroPrimary
import com.example.util.NotificationHelper

class MainActivity : ComponentActivity() {
    private val viewModel: EventViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)

        setContent {
            MyApplicationTheme {
                // Request Notification Permission on Android 13+
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Granted or denied */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                MainEventApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainEventApp(viewModel: EventViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val wishes by viewModel.wishes.collectAsState()
    val tickerIndex by viewModel.tickerIndex.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadNotifications.collectAsState()
    val guests by viewModel.allGuests.collectAsState()
    val bookedSeats by viewModel.bookedSeatsCount.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    colors = navItemColors,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = "Programma & Mappa"
                        )
                    },
                    label = { 
                        Text(
                            "Programma",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        ) 
                    },
                    modifier = Modifier.testTag("nav_item_program")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    colors = navItemColors,
                    icon = {
                        BadgedBox(
                            badge = {
                                val pending = guests.count { it.rsvpStatus == com.example.data.RsvpStatus.PENDING }
                                if (pending > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) { Text("$pending") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Invitati & RSVP"
                            )
                        }
                    },
                    label = { 
                        Text(
                            "Invitati",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        ) 
                    },
                    modifier = Modifier.testTag("nav_item_guests")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    colors = navItemColors,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = "Navetta Autobus"
                        )
                    },
                    label = { 
                        Text(
                            "Navetta",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                        ) 
                    },
                    modifier = Modifier.testTag("nav_item_bus")
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    colors = navItemColors,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = "Foto & Auguri"
                        )
                    },
                    label = { 
                        Text(
                            "Bacheca",
                            fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal
                        ) 
                    },
                    modifier = Modifier.testTag("nav_item_photos_wishes")
                )

                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { viewModel.setSelectedTab(4) },
                    colors = navItemColors,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = "Quote Regali"
                        )
                    },
                    label = { 
                        Text(
                            "Regali",
                            fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal
                        ) 
                    },
                    modifier = Modifier.testTag("nav_item_gifts")
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> ProgramAndEventScreen(
                viewModel = viewModel,
                wishes = wishes,
                tickerIndex = tickerIndex,
                notifications = notifications,
                unreadCount = unreadCount,
                onNavigateToWishes = { viewModel.setSelectedTab(3) },
                onNavigateToBus = { viewModel.setSelectedTab(2) },
                onNavigateToRsvp = { viewModel.setSelectedTab(1) },
                modifier = Modifier.padding(innerPadding)
            )
            1 -> GuestsRsvpScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            2 -> BusTransferScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            3 -> PhotoAndWishesScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            4 -> GiftsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

// Retained for tests and previews
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
