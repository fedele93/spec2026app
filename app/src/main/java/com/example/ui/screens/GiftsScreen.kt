package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GiftContributionEntity
import com.example.data.GiftTargetEntity
import com.example.ui.EventViewModel
import com.example.ui.theme.LaurelGold
import com.example.ui.theme.LaurelGoldDark
import com.example.ui.theme.NeuroDarkNavy
import com.example.ui.theme.NeuroPrimary
import com.example.ui.theme.SynapseCyan
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftsScreen(
    viewModel: EventViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val targets by viewModel.giftTargets.collectAsState()
    val contributions by viewModel.giftContributions.collectAsState()

    var selectedTargetForGift by remember { mutableStateOf<GiftTargetEntity?>(null) }

    val totalCollected = targets.sumOf { it.collectedAmount }
    val totalGoal = targets.sumOf { it.targetAmount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Regali di Specializzazione",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Partecipa ai regali con quote differenziali verso i diversi laureandi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Global Goal Header Card
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
                        Column {
                            Text(
                                text = "Raccolta Totale Quote",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${totalCollected.toInt()} €",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = LaurelGoldDark
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = LaurelGold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Obiettivo: ${totalGoal.toInt()} €",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = LaurelGoldDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val progress = if (totalGoal > 0) (totalCollected / totalGoal).toFloat().coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = LaurelGold,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${contributions.size} quote versate dagli invitati finora",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "Destinatari & Obiettivi Regalo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // List of Graduate Targets
        items(targets, key = { it.id }) { target ->
            GiftTargetCard(
                target = target,
                onContribute = { selectedTargetForGift = target }
            )
        }

        // Recent Contributions List
        if (contributions.isNotEmpty()) {
            item {
                Text(
                    text = "Ultime Quote Versate & Dediche",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            items(contributions, key = { it.id }) { cont ->
                ContributionItemCard(contribution = cont)
            }
        }
    }

    if (selectedTargetForGift != null) {
        DifferentialGiftDialog(
            target = selectedTargetForGift!!,
            onDismiss = { selectedTargetForGift = null },
            onConfirmContribution = { donor, amount, method, note, isAnon ->
                viewModel.contributeToGift(
                    donorName = donor,
                    targetId = selectedTargetForGift!!.id,
                    targetName = selectedTargetForGift!!.name,
                    amount = amount,
                    paymentMethod = method,
                    blessingNote = note,
                    isAnonymous = isAnon
                )
                selectedTargetForGift = null
                Toast.makeText(context, "Grazie per il tuo contributo di ${amount.toInt()}€!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun GiftTargetCard(
    target: GiftTargetEntity,
    onContribute: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gift_target_card_${target.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (target.id == "gruppo") LaurelGold.copy(alpha = 0.2f)
                                else NeuroPrimary.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (target.id == "gruppo") Icons.Default.Groups else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (target.id == "gruppo") LaurelGoldDark else NeuroPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = target.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = target.specialization,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "🎁 ${target.giftTitle}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = target.giftDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress towards goal
            val targetProgress = if (target.targetAmount > 0)
                (target.collectedAmount / target.targetAmount).toFloat().coerceIn(0f, 1f)
            else 0f

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Raccolti: ${target.collectedAmount.toInt()} € / ${target.targetAmount.toInt()} €",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(targetProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = LaurelGoldDark
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { targetProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (target.id == "gruppo") LaurelGold else SynapseCyan,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onContribute,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contribute_button_${target.id}"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (target.id == "gruppo") LaurelGold else NeuroPrimary,
                    contentColor = if (target.id == "gruppo") NeuroDarkNavy else Color.White
                )
            ) {
                Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dona Quota per ${target.name}", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ContributionItemCard(contribution: GiftContributionEntity) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(LaurelGold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${contribution.amount.toInt()}€",
                    fontWeight = FontWeight.Black,
                    color = LaurelGoldDark,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${contribution.donorName} → ${contribution.targetGraduateName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (contribution.note.isNotBlank()) {
                    Text(
                        text = "“${contribution.note}”",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Metodo: ${contribution.paymentMethod} • ${dateFormat.format(Date(contribution.contributedAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifferentialGiftDialog(
    target: GiftTargetEntity,
    onDismiss: () -> Unit,
    onConfirmContribution: (donor: String, amount: Double, method: String, note: String, isAnon: Boolean) -> Unit
) {
    val context = LocalContext.current
    var donorName by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isAnonymous by remember { mutableStateOf(false) }

    // Differential amount choices
    val presetAmounts = listOf(25.0, 50.0, 100.0, 150.0)
    var selectedPreset by remember { mutableStateOf<Double?>(50.0) }
    var customAmount by remember { mutableStateOf("") }

    val activeAmount: Double = selectedPreset ?: (customAmount.toDoubleOrNull() ?: 0.0)

    val paymentMethods = listOf("IBAN Bonifico", "Satispay", "PayPal", "Contanti")
    var selectedMethod by remember { mutableStateOf("IBAN Bonifico") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Quota Regalo • ${target.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Scegli l'importo della tua quota differenziale:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Preset amount chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetAmounts.forEach { amount ->
                            val isSelected = selectedPreset == amount
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) LaurelGold else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) NeuroDarkNavy else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedPreset = amount
                                        customAmount = ""
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${amount.toInt()} €",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Custom amount input
                item {
                    OutlinedTextField(
                        value = customAmount,
                        onValueChange = {
                            if (it.all { ch -> ch.isDigit() || ch == '.' }) {
                                customAmount = it
                                selectedPreset = null
                            }
                        },
                        label = { Text("Oppure inserisci Quota Personalizzata (€)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_gift_amount_input")
                    )
                }

                // Payment Method Selector
                item {
                    Text(
                        text = "Metodo di Pagamento Integrato:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(paymentMethods) { method ->
                            FilterChip(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                label = { Text(method, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Details for the chosen payment method
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            when (selectedMethod) {
                                "IBAN Bonifico" -> {
                                    Text(
                                        text = "Intestatario: ${target.ibanHolder}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = target.iban,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("IBAN", target.iban)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "IBAN copiato negli appunti!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copia Coordinate IBAN")
                                    }
                                }
                                "Satispay" -> {
                                    Text("Paga comodamente tramite Satispay:", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target.satispayUrl))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                    ) {
                                        Text("Apri Satispay")
                                    }
                                }
                                "PayPal" -> {
                                    Text("Invia la quota tramite PayPal.me:", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target.paypalMeUrl))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0070BA))
                                    ) {
                                        Text("Apri PayPal.me")
                                    }
                                }
                                else -> {
                                    Text(
                                        text = "Puoi consegnare la quota in contanti durante la festa agli organizzatori.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                // Donor Name & Note
                item {
                    OutlinedTextField(
                        value = donorName,
                        onValueChange = { donorName = it },
                        label = { Text("Il tuo Nome *") },
                        singleLine = true,
                        enabled = !isAnonymous,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("donor_name_input")
                    )
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isAnonymous = !isAnonymous }
                    ) {
                        Checkbox(checked = isAnonymous, onCheckedChange = { isAnonymous = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Regalo anonimo", fontSize = 12.sp)
                    }
                }

                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Dedica / Biglietto di auguri") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (activeAmount > 0) {
                        onConfirmContribution(donorName, activeAmount, selectedMethod, note, isAnonymous)
                    }
                },
                enabled = activeAmount > 0 && (isAnonymous || donorName.isNotBlank()),
                modifier = Modifier.testTag("confirm_gift_contribution_button")
            ) {
                Text("Conferma Versamento (${activeAmount.toInt()}€)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}
