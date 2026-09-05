package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MapPoint
import com.example.ui.theme.LaurelGold
import com.example.ui.theme.NeuroDarkNavy
import com.example.ui.theme.NeuroPrimary
import com.example.ui.theme.SynapseCyan

@Composable
fun InteractiveMapCanvas(
    points: List<MapPoint>,
    selectedPoint: MapPoint,
    onSelectPoint: (MapPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier) {
        // Map Surface Box with Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A))
                .testTag("interactive_map_canvas")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures { tapOffset ->
                            val width = size.width
                            val height = size.height

                            // Check collision with the 3 points
                            val p1 = Offset(width * 0.22f, height * 0.32f)
                            val p2 = Offset(width * 0.38f, height * 0.52f)
                            val p3 = Offset(width * 0.78f, height * 0.75f)

                            val radius = 50f
                            if ((tapOffset - p1).getDistance() <= radius && points.isNotEmpty()) {
                                onSelectPoint(points[0])
                            } else if ((tapOffset - p2).getDistance() <= radius && points.size > 1) {
                                onSelectPoint(points[1])
                            } else if ((tapOffset - p3).getDistance() <= radius && points.size > 2) {
                                onSelectPoint(points[2])
                            }
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Base grid lines representing city blocks
                val gridStroke = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f), 0f))
                for (x in 0..canvasWidth.toInt() step 90) {
                    drawLine(Color(0x1A38BDF8), Offset(x.toFloat(), 0f), Offset(x.toFloat(), canvasHeight), strokeWidth = 1f)
                }
                for (y in 0..canvasHeight.toInt() step 70) {
                    drawLine(Color(0x1A38BDF8), Offset(0f, y.toFloat()), Offset(canvasWidth, y.toFloat()), strokeWidth = 1f)
                }

                // Highway / Route path connecting Policlinico -> Fermata Bus -> Villa
                val p1 = Offset(canvasWidth * 0.22f, canvasHeight * 0.32f)
                val p2 = Offset(canvasWidth * 0.38f, canvasHeight * 0.52f)
                val p3 = Offset(canvasWidth * 0.78f, canvasHeight * 0.75f)

                val routePath = Path().apply {
                    moveTo(p1.x, p1.y)
                    quadraticTo((p1.x + p2.x) / 2, (p1.y + p2.y) / 2 + 10f, p2.x, p2.y)
                    cubicTo(canvasWidth * 0.50f, canvasHeight * 0.40f, canvasWidth * 0.65f, canvasHeight * 0.90f, p3.x, p3.y)
                }

                // Outer route glow
                drawPath(
                    path = routePath,
                    color = Color(0x4038BDF8),
                    style = Stroke(width = 9f)
                )
                // Inner dashed animated route
                drawPath(
                    path = routePath,
                    color = Color(0xFF38BDF8),
                    style = Stroke(
                        width = 4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f)
                    )
                )

                // Highlight circle around selected point
                val activeOffset = when (selectedPoint.id) {
                    "seduta" -> p1
                    "bus" -> p2
                    else -> p3
                }
                drawCircle(
                    color = Color(0x33FBBF24),
                    radius = 34f,
                    center = activeOffset
                )
                drawCircle(
                    color = Color(0x66FBBF24),
                    radius = 24f,
                    center = activeOffset
                )

                // Draw pins
                drawCircle(color = Color(0xFF1B3B6F), radius = 16f, center = p1)
                drawCircle(color = Color(0xFFFFFFFF), radius = 6f, center = p1)

                drawCircle(color = Color(0xFF00B4D8), radius = 16f, center = p2)
                drawCircle(color = Color(0xFFFFFFFF), radius = 6f, center = p2)

                drawCircle(color = Color(0xFFD4AF37), radius = 18f, center = p3)
                drawCircle(color = Color(0xFFFFFFFF), radius = 7f, center = p3)
            }

            // Interactive Point Switcher Bar at the top of the map
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEach { point ->
                    val isSelected = point.id == selectedPoint.id
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) LaurelGold else Color(0xCC1E293B),
                        contentColor = if (isSelected) NeuroDarkNavy else Color.White,
                        modifier = Modifier
                            .clickable { onSelectPoint(point) }
                            .testTag("map_point_chip_${point.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = when (point.iconType) {
                                    "GRADUATION" -> Icons.Default.School
                                    "BUS" -> Icons.Default.DirectionsBus
                                    else -> Icons.Default.Celebration
                                },
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (point.id) {
                                    "seduta" -> "Seduta Laurea"
                                    "bus" -> "Fermata Bus"
                                    else -> "Villa Festa"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Legend bottom right
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xAA000000),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = "Tocca i punti per i dettagli",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected Location Card
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically()
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("map_selected_point_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (selectedPoint.iconType) {
                                            "GRADUATION" -> NeuroPrimary
                                            "BUS" -> SynapseCyan
                                            else -> LaurelGold
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (selectedPoint.iconType) {
                                        "GRADUATION" -> Icons.Default.School
                                        "BUS" -> Icons.Default.DirectionsBus
                                        else -> Icons.Default.Celebration
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = selectedPoint.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = selectedPoint.timeLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = selectedPoint.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = selectedPoint.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Open in Google Maps Navigation Intent
                    Button(
                        onClick = {
                            val uri = Uri.parse(
                                "geo:${selectedPoint.latitude},${selectedPoint.longitude}?q=${Uri.encode(selectedPoint.address)}"
                            )
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(mapIntent)
                            } catch (_: Exception) {
                                // Fallback to any browser / maps provider
                                val webMapsIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://maps.google.com/?q=${selectedPoint.latitude},${selectedPoint.longitude}")
                                )
                                context.startActivity(webMapsIntent)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_maps_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apri in Google Maps & Navigatore", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
