package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.GraduateMeDto
import com.example.ui.EventViewModel
import com.example.ui.theme.LaurelGold
import com.example.ui.theme.LaurelGoldDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun shortName(name: String): String = name.replace(Regex("^Dott\\.(ssa)?\\s*", RegexOption.IGNORE_CASE), "")

/** Card nella tab Programma, solo per chi ha il token festeggiato e il server configurati. */
@Composable
fun GraduateAreaCard(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = LaurelGold.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, LaurelGold.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("graduate_area_card")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
            Icon(Icons.Default.School, contentDescription = null, tint = LaurelGoldDark, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Area festeggiato", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Chi viene alla festa, gli auguri per te, le tue coordinate di pagamento e la navetta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Stat(title: String, value: String, subtitle: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** L'area personale del festeggiato: numeri, auguri, chi viene, coordinate, avatar (si configura dalla PWA). */
@Composable
fun GraduateAreaDialog(viewModel: EventViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val area by viewModel.graduateArea.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadGraduateArea() }
    val me: GraduateMeDto? = area
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    var showGuests by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (me == null) "🎓 Area festeggiato" else "🎓 Ciao ${shortName(me.graduate.name).substringBefore(' ')}!", fontWeight = FontWeight.Bold) },
        text = {
            if (me == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Carico i dati dal server…", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                val g = me.graduate
                val myShort = shortName(g.name).lowercase()
                val forMe = me.wishes.filter { shortName(it.targetGraduate).lowercase() == myShort }
                val forAll = me.wishes.filter { it !in forMe }
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).testTag("graduate_area_content"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Stat("Confermati", "${me.guests.confirmedGuests}", "${me.guests.covers} persone", Color(0xFF166534), Modifier.weight(1f))
                        Stat("In attesa", "${me.guests.pendingGuests}", "${me.guests.pendingCovers} persone", Color(0xFF854D0E), Modifier.weight(1f))
                        Stat("Navetta", "${me.bus.bookedSeats}", "di ${me.bus.maxSeats} posti", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    }

                    Text("💌 Auguri per te · ${forMe.size} per te, ${forAll.size} per tutti", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (forMe.isEmpty()) {
                        Text("Nessun augurio dedicato a te per ora: arriveranno!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    (forMe + forAll.take(5)).forEach { w ->
                        Surface(shape = RoundedCornerShape(8.dp), color = LaurelGold.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(w.message, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "— ${w.authorName} · ${dateFormat.format(Date(w.createdAt))}" + (if (w.heartCount > 0) " · ❤️ ${w.heartCount}" else "") +
                                        (if (w in forAll) " · per tutti" else ""),
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { showGuests = !showGuests }) {
                        Text("👥 Chi viene · ${me.guests.confirmedGuests} confermati", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Icon(if (showGuests) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                    }
                    if (showGuests) {
                        me.guests.confirmed.forEach { x ->
                            val comps = x.companions.filter { it.isNotBlank() }
                            Text(
                                "${x.fullName}${if (x.guestsCount > 1) " (${x.guestsCount} persone)" else ""} · ${x.category}" + (if (comps.isNotEmpty()) " · con ${comps.joinToString(", ")}" else ""),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (me.guests.pending.isNotEmpty()) {
                            Text("Ancora in attesa: ${me.guests.pending.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Text("💳 Le tue coordinate (come le vedono gli invitati)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Intestatario: ${g.ibanHolder}", style = MaterialTheme.typography.bodySmall)
                    Text(g.iban, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("IBAN", g.iban))
                            Toast.makeText(context, "IBAN copiato", Toast.LENGTH_SHORT).show()
                        }, modifier = Modifier.weight(1f)) { Text("Copia IBAN", fontSize = 12.sp) }
                        if (g.paypalMeUrl.isNotBlank()) {
                            OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(g.paypalMeUrl))) }, modifier = Modifier.weight(1f)) { Text("PayPal", fontSize = 12.sp) }
                        }
                        if (g.satispayUrl.isNotBlank()) {
                            OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(g.satispayUrl))) }, modifier = Modifier.weight(1f)) { Text("Satispay", fontSize = 12.sp) }
                        }
                    }
                    Text(
                        if (g.email.isNullOrBlank()) "Se gli organizzatori aggiungono la tua email al file dell'evento, ricevi un'email per ogni augurio dedicato a te."
                        else "Ricevi un'email a ${g.email} per ogni augurio dedicato a te.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val a = me.avatar
                    Text("🎙️ Il tuo avatar dell'assistente", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            a == null -> "Nessun avatar collegato al tuo id."
                            !me.assistantConfigured -> "Assistente non ancora attivo sul server (MISTRAL_API_KEY)."
                            a.ready -> "Pronto · voce ${a.voiceKind}" + (if (a.sampleFilename.isNotBlank()) " · campione ${a.sampleFilename}" else "")
                            a.enabled -> "Abilitato ma senza voce: carica un campione dalla PWA."
                            else -> "Disabilitato: si abilita dalla PWA."
                        } + " Persona e campione vocale si configurano dalla PWA con lo stesso token.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.loadGraduateArea() }) { Text("Aggiorna") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } }
    )
}
