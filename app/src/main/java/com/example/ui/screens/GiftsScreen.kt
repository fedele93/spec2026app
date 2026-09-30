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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GiftCollectorEntity
import com.example.data.GiftSplit
import com.example.data.GiftTargetEntity
import com.example.data.formatEuro
import com.example.data.paymentMethods
import com.example.data.remote.GiftPoolContribution
import com.example.data.remote.PoolAllocationRequest
import com.example.data.remote.PoolContributionRequest
import com.example.data.remote.isReceived
import com.example.ui.EventViewModel
import com.example.ui.theme.LaurelGold
import com.example.ui.theme.LaurelGoldDark
import com.example.ui.theme.NeuroDarkNavy
import com.example.ui.theme.NeuroPrimary
import java.text.SimpleDateFormat
import java.util.*

/**
 * Sezione Regali. Nessuna cifra raccolta è visibile. Due modi per partecipare:
 * - quota unica al cassiere (registrata sul server con la ripartizione fra i neo-specialisti,
 *   visibile solo a lui: il cruscotto è nella PWA);
 * - regalo diretto a un neo-specialista: copia IBAN / PayPal / Satispay, senza registrare nulla.
 */
private val PayPalBlue = Color(0xFF0070BA)
private val SatispayRed = Color(0xFFEF4444)

private fun shortName(name: String): String = name.replace(Regex("^Dott\\.(ssa)?\\s*", RegexOption.IGNORE_CASE), "")

private fun copyToClipboard(context: Context, label: String, text: String, toast: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
}

private fun openUrl(context: Context, url: String) {
    if (url.isBlank()) return
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        .onFailure { Toast.makeText(context, "Nessuna app disponibile per aprire il link", Toast.LENGTH_SHORT).show() }
}

@Composable
fun GiftsScreen(
    viewModel: EventViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val targets by viewModel.giftTargets.collectAsState()
    val collector by viewModel.giftCollector.collectAsState()
    val myPool by viewModel.myPoolContributions.collectAsState()
    var showPoolDialog by remember { mutableStateOf(false) }
    var contributionToDelete by remember { mutableStateOf<GiftPoolContribution?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshMyPoolContributions() }

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
                    text = "Scegli come partecipare: una quota unica tramite il cassiere, oppure un regalo diretto a ciascun neo-specialista. Nessuna cifra raccolta viene mostrata.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            CollectorCard(
                collector = collector,
                targetsCount = targets.size,
                myPool = myPool,
                onParticipate = { showPoolDialog = true },
                onDelete = { contributionToDelete = it }
            )
        }

        item {
            Column {
                Text(
                    text = "Regalo diretto a un neo-specialista",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Copia l'IBAN o apri PayPal/Satispay: l'importo lo decidi tu e non serve registrarlo qui.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(targets.size, key = { targets[it].id }) { index ->
            val target = targets[index]
            GiftTargetCard(
                target = target,
                onCopyIban = { copyToClipboard(context, "IBAN", target.iban, "IBAN di ${shortName(target.name)} copiato!") },
                onOpenUrl = { openUrl(context, it) }
            )
        }
    }

    val c = collector
    if (showPoolDialog && c != null) {
        PoolContributionDialog(
            targets = targets,
            collector = c,
            onDismiss = { showPoolDialog = false },
            onConfirm = { request ->
                viewModel.addPoolContribution(request)
                showPoolDialog = false
            }
        )
    }

    contributionToDelete?.let { pending ->
        AlertDialog(
            onDismissRequest = { contributionToDelete = null },
            title = { Text("Annullare la quota?") },
            text = { Text("La quota di ${formatEuro(pending.totalAmount)} non sarà più visibile al cassiere.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deletePoolContribution(pending.id); contributionToDelete = null }) { Text("Annulla quota") }
            },
            dismissButton = { TextButton(onClick = { contributionToDelete = null }) { Text("Indietro") } }
        )
    }
}

@Composable
private fun CollectorCard(
    collector: GiftCollectorEntity?,
    targetsCount: Int,
    myPool: List<GiftPoolContribution>,
    onParticipate: () -> Unit,
    onDelete: (GiftPoolContribution) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LaurelGold.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gift_collector_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(LaurelGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = LaurelGoldDark, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Quota unica tramite ${collector?.name ?: "il cassiere"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = collector?.roleTitle ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (!collector?.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = collector?.description ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Metodi: " + (collector?.paymentMethods ?: emptyList()).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onParticipate,
                enabled = collector != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pool_open_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LaurelGold, contentColor = NeuroDarkNavy)
            ) {
                Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Partecipa con una quota unica", fontWeight = FontWeight.Bold)
            }

            if (myPool.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = LaurelGold.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Le tue quote registrate", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                myPool.forEach { c ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${formatEuro(c.totalAmount)} · ${c.paymentMethod}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusPill(received = c.isReceived)
                            }
                            val who = if (c.allocations.size == targetsCount && targetsCount > 0) "tutti i neo-specialisti"
                            else c.allocations.joinToString(", ") { shortName(it.graduateName) }
                            Text(
                                text = "${c.donorName} · $who · ${dateFormat.format(Date(c.createdAt))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!c.isReceived) {
                            IconButton(onClick = { onDelete(c) }, modifier = Modifier.testTag("pool_delete_${c.id}")) {
                                Icon(Icons.Default.Delete, contentDescription = "Annulla quota", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(received: Boolean) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (received) Color(0xFFDCFCE7) else Color(0xFFFEF9C3)
    ) {
        Text(
            text = if (received) "Ricevuta" else "In attesa",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (received) Color(0xFF166534) else Color(0xFF854D0E),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun GiftTargetCard(
    target: GiftTargetEntity,
    onCopyIban: () -> Unit,
    onOpenUrl: (String) -> Unit
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(NeuroPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = NeuroPrimary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = target.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Intestatario: ${target.ibanHolder}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = target.iban,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onCopyIban,
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("copy_iban_${target.id}"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copia IBAN", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                if (target.paypalMeUrl.isNotBlank()) {
                    Button(
                        onClick = { onOpenUrl(target.paypalMeUrl) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PayPalBlue),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) { Text("PayPal", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
                if (target.satispayUrl.isNotBlank()) {
                    Button(
                        onClick = { onOpenUrl(target.satispayUrl) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SatispayRed),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) { Text("Satispay", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

private const val MODE_EQUAL = "EQUAL"
private const val MODE_CUSTOM = "CUSTOM"

/** Dialogo della quota unica: importo, ripartizione (parti uguali o personalizzata), metodo e dati per pagare. */
@Composable
fun PoolContributionDialog(
    targets: List<GiftTargetEntity>,
    collector: GiftCollectorEntity,
    onDismiss: () -> Unit,
    onConfirm: (PoolContributionRequest) -> Unit
) {
    val context = LocalContext.current
    var donorName by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(MODE_EQUAL) }

    val presetAmounts = listOf(50.0, 100.0, 150.0, 200.0)
    var selectedPreset by remember { mutableStateOf<Double?>(100.0) }
    var customAmount by remember { mutableStateOf("") }
    val equalTotal: Double = selectedPreset ?: (customAmount.replace(',', '.').toDoubleOrNull() ?: 0.0)

    val selectedIds = remember { mutableStateListOf<String>().apply { addAll(targets.map { it.id }) } }
    val customAmounts = remember { mutableStateMapOf<String, String>() }
    val customRows = targets.mapNotNull { t ->
        val v = customAmounts[t.id]?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        if (v > 0) PoolAllocationRequest(t.id, v) else null
    }

    val methods = collector.paymentMethods.filter { it in listOf("IBAN", "PayPal", "Contanti") }.ifEmpty { listOf("IBAN") }
    var method by remember { mutableStateOf(methods.first()) }

    val total = if (mode == MODE_EQUAL) equalTotal else customRows.sumOf { it.amount }
    val equalParts: Map<String, Double> = if (mode == MODE_EQUAL && equalTotal > 0 && selectedIds.isNotEmpty()) {
        val parts = GiftSplit.splitEqually(GiftSplit.cents(equalTotal), selectedIds.size)
        selectedIds.mapIndexed { i, id -> id to parts[i] / 100.0 }.toMap()
    } else emptyMap()
    val canConfirm = donorName.isNotBlank() && total > 0 &&
        (mode == MODE_CUSTOM || (selectedIds.isNotEmpty() && GiftSplit.cents(equalTotal) >= selectedIds.size))
    val transferReason = "${collector.transferReason.ifBlank { "Regalo specializzazione" }} - ${donorName.trim().ifBlank { "Nome Cognome" }}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Quota unica • ${collector.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Un solo versamento: ${shortName(collector.name)} lo ripartisce fra i neo-specialisti secondo le tue indicazioni.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = donorName,
                    onValueChange = { donorName = it },
                    label = { Text("Il tuo nome e cognome *") },
                    supportingText = { Text("Serve al cassiere per riconoscere il versamento") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pool_donor_input")
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contatto (facoltativo)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Come vuoi ripartire la quota?", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = mode == MODE_EQUAL, onClick = { mode = MODE_EQUAL }, label = { Text("In parti uguali", fontSize = 12.sp) })
                    FilterChip(selected = mode == MODE_CUSTOM, onClick = { mode = MODE_CUSTOM }, label = { Text("Personalizza", fontSize = 12.sp) })
                }

                if (mode == MODE_EQUAL) {
                    Text("Importo totale", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        presetAmounts.forEach { amount ->
                            val isSelected = selectedPreset == amount
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) LaurelGold else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) NeuroDarkNavy else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedPreset = amount; customAmount = "" }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text(text = "${amount.toInt()} €", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = customAmount,
                        onValueChange = { v ->
                            if (v.all { ch -> ch.isDigit() || ch == '.' || ch == ',' }) { customAmount = v; selectedPreset = null }
                        },
                        label = { Text("Oppure un altro importo (€)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pool_amount_input")
                    )
                    Text(
                        "Per chi (togli la spunta per escludere qualcuno, es. te stesso)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    targets.forEach { t ->
                        val checked = t.id in selectedIds
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (checked) selectedIds.remove(t.id) else selectedIds.add(t.id) }
                        ) {
                            Checkbox(checked = checked, onCheckedChange = { if (it) selectedIds.add(t.id) else selectedIds.remove(t.id) })
                            Text(t.name, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text(
                                text = equalParts[t.id]?.let { formatEuro(it) } ?: "—",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        "Importo per ciascun neo-specialista (lascia vuoto chi non vuoi includere)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    targets.forEach { t ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(t.name, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = customAmounts[t.id] ?: "",
                                onValueChange = { v ->
                                    if (v.all { ch -> ch.isDigit() || ch == '.' || ch == ',' }) customAmounts[t.id] = v
                                },
                                placeholder = { Text("0") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(96.dp)
                            )
                        }
                    }
                }

                Text("Metodo di pagamento", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    methods.forEach { m ->
                        FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m, fontSize = 12.sp) })
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        when (method) {
                            "IBAN" -> {
                                Text("Intestatario: ${collector.ibanHolder}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = collector.iban,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Causale suggerita: $transferReason",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { copyToClipboard(context, "IBAN", collector.iban, "IBAN del cassiere copiato!") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                    ) { Text("Copia IBAN", fontSize = 12.sp) }
                                    OutlinedButton(
                                        onClick = { copyToClipboard(context, "Causale", transferReason, "Causale copiata!") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                    ) { Text("Copia causale", fontSize = 12.sp) }
                                }
                            }
                            "PayPal" -> {
                                Text("Invia la quota con PayPal.me indicando nel messaggio il tuo nome:", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = { openUrl(context, collector.paypalMeUrl) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PayPalBlue)
                                ) { Text("Apri PayPal.me") }
                            }
                            else -> {
                                Text(
                                    text = "Consegna i contanti a ${shortName(collector.name)} (in reparto o alla festa): registra comunque la quota qui, così sa cosa aspettarsi.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota per il cassiere (facoltativa)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val request = if (mode == MODE_EQUAL) {
                        PoolContributionRequest(
                            donorName = donorName.trim(), contact = contact.trim(), paymentMethod = method,
                            splitMode = MODE_EQUAL, totalAmount = equalTotal, graduateIds = selectedIds.toList(), note = note.trim()
                        )
                    } else {
                        PoolContributionRequest(
                            donorName = donorName.trim(), contact = contact.trim(), paymentMethod = method,
                            splitMode = MODE_CUSTOM, allocations = customRows, note = note.trim()
                        )
                    }
                    onConfirm(request)
                },
                enabled = canConfirm,
                modifier = Modifier.testTag("pool_confirm_button")
            ) {
                Text("Registra quota (${formatEuro(total)})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
