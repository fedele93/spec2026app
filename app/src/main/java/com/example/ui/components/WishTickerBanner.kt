package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WishEntity
import com.example.ui.theme.LaurelGold
import com.example.ui.theme.LaurelGoldDark
import com.example.ui.theme.NeuroDarkNavy
import com.example.ui.theme.NeuroPrimary

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WishTickerBanner(
    wishes: List<WishEntity>,
    currentIndex: Int,
    onHeartWish: (Long) -> Unit,
    onClickBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (wishes.isEmpty()) return

    val safeIndex = if (wishes.isNotEmpty()) currentIndex % wishes.size else 0
    val currentWish = wishes[safeIndex]

    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LaurelGold.copy(alpha = 0.35f)),
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClickBanner() }
            .testTag("wish_ticker_banner")
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            NeuroDarkNavy,
                            NeuroPrimary,
                            NeuroDarkNavy
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Animated Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LaurelGold,
                    contentColor = NeuroDarkNavy,
                    modifier = Modifier.padding(end = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = LaurelGoldDark
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "AUGURI LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Rotating text container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    AnimatedContent(
                        targetState = currentWish,
                        transitionSpec = {
                            (slideInVertically { height -> height } + fadeIn()) with
                                    (slideOutVertically { height -> -height } + fadeOut())
                        },
                        label = "WishAnimation"
                    ) { wish ->
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${wish.emojiBadge} ${wish.authorName} ",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LaurelGold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "→ ${wish.targetGraduate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "“${wish.message}”",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Heart Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onHeartWish(currentWish.id) }
                        .testTag("ticker_heart_button_${currentWish.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Mi piace",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${currentWish.heartCount}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
