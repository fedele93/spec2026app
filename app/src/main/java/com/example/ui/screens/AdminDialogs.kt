package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GuestImport
import com.example.data.remote.DashboardDto
import com.example.data.remote.ImportResultDto
import com.example.data.remote.ImportRowRequest
import com.example.ui.EventViewModel
import com.example.ui.theme.LaurelGold
import com.example.ui.theme.NeuroDarkNavy

private val MENU_LABELS = mapOf(
    "" to "Da scegliere", "STANDARD" to "Menu completo", "VEGETARIANO" to "Vegetariano", "VEGANO" to "Vegano",
    "SENZA_GLUTINE" to "Senza glutine", "ALTRO" to "Altro (vedi note)"
)

/** Card nella tab Invitati, solo per gli organizzatori con il server configurato. */
@Composable
fun OrganizerCard(onOpenDashboard: () -> Unit, onOpenImport: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("organizer_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Dashboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Organizzatori", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        "Cruscotto con i numeri e le cose da fare; import della lista invitati con invio dei link personali.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenDashboard, modifier = Modifier.weight(1f).testTag("dashboard_button"), shape = RoundedCornerShape(10.dp)) {
                    Text("Cruscotto", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onOpenImport, modifier = Modifier.weight(1f).testTag("import_button"), shape = RoundedCornerShape(10.dp)) {
                    Text("Importa invitati", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatTile(title: String, value: String, subtitle: String, color: Color, modifier: Modifier = Modifier) {
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

private fun fmtDeadline(iso: String?): String {
    if (iso.isNullOrBlank()) return "non fissata"
    val parts = iso.split("-")
    if (parts.size != 3) return iso
    val months = listOf("gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno", "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre")
    val m = parts[1].toIntOrNull()?.let { months.getOrNull(it - 1) } ?: parts[1]
    return "${parts[2].trimStart('0')} $m"
}

/** Cruscotto organizzatori: contatori, menu scelti e cose da fare (con il salto alla tab o al pannello). */
@Composable
fun DashboardDialog(
    viewModel: EventViewModel,
    onDismiss: () -> Unit,
    onOpenImport: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val dashboard by viewModel.dashboard.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadDashboard() }
    val d: DashboardDto? = dashboard

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📊 Cruscotto organizzatori", fontWeight = FontWeight.Bold) },
        text = {
            if (d == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Carico i dati dal server…", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                val g = d.guests; val b = d.bus; val m = d.mail; val p = d.pool
                val mailTotal = m.counts.values.sum()
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).testTag("dashboard_content"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Date limite: menu ${fmtDeadline(d.deadlines.menuDeadline)}${if (d.deadlines.menuLocked) " (chiuso)" else ""}, navetta ${fmtDeadline(d.deadlines.busDeadline)}${if (d.deadlines.busLocked) " (chiusa)" else ""}.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("Confermati", "${g.confirmed}", "${g.covers} coperti", Color(0xFF166534), Modifier.weight(1f))
                        StatTile("In attesa", "${g.pending}", "${g.pendingCovers} persone · ${g.declined} no", Color(0xFF854D0E), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("Senza menu", "${g.missingMenu}", "${g.allergies} allergie", if (g.missingMenu > 0) Color(0xFF991B1B) else Color(0xFF166534), Modifier.weight(1f))
                        StatTile("Navetta", "${b.bookedSeats}", "di ${b.maxSeats} · ${b.waitlistSeats} in attesa · ${b.checkedInSeats} saliti", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("Profili", "${g.withProfile}", "${g.withEmail} con email · ${g.consented} consensi", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        StatTile("Email", "$mailTotal", if (m.enabled) "${m.counts["DELIVERED"] ?: 0} consegnate · ${m.counts["FAILED"] ?: 0} fallite" else "n8n non attivo", if (m.enabled) Color(0xFF166534) else Color(0xFF991B1B), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("Quote uniche", "${p.contributions}", "${p.pending} in attesa · ${p.received} ricevute (cifre solo ai cassieri)", LaurelGold, Modifier.weight(1f))
                        StatTile("Bacheca", "${d.wishes}", "auguri · ${d.photos} foto · ${d.pushSubscriptions} push", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    }

                    Text("✅ Cose da fare (${d.todo.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    if (d.todo.isEmpty()) {
                        Text("Niente in sospeso: tutto a posto!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    d.todo.forEach { t ->
                        val target = t.action ?: t.section
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (target != null) Modifier.clickable {
                                    when (t.action) {
                                        "import" -> onOpenImport()
                                        null -> t.section?.let(onNavigate)
                                        else -> onNavigate("program") // mail, assistente e promemoria si gestiscono dalla PWA
                                    }
                                } else Modifier)
                                .testTag("todo_${t.key}")
                        ) {
                            Text("• ${t.text}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            if (target != null) Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Text("🍽️ Menu scelti", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    g.byMenu.forEach { x ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(MENU_LABELS[x.menu] ?: x.menu.ifBlank { "Da scegliere" }, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text("${x.covers}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        "Assistente: ${if (d.assistant.configured) "${d.assistant.ready} avatar pronti su ${d.assistant.avatars}" else "non configurato"} · aggiornati negli ultimi 7 giorni: ${g.updatedLastWeek}. Email, promemoria e avatar si gestiscono dalla PWA.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onOpenImport, modifier = Modifier.testTag("dashboard_import_button")) { Text("Importa invitati") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { viewModel.loadDashboard() }) { Text("Aggiorna") }
                TextButton(onClick = onDismiss) { Text("Chiudi") }
            }
        }
    )
}

/** Import della lista invitati: incolla le righe, anteprima dal server (dryRun), poi import vero. */
@Composable
fun ImportGuestsDialog(viewModel: EventViewModel, onDismiss: () -> Unit, onDone: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var sendLinks by remember { mutableStateOf(false) }
    var rows by remember { mutableStateOf<List<ImportRowRequest>>(emptyList()) }
    var preview by remember { mutableStateOf<ImportResultDto?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    val canImport = preview != null && !busy && (preview!!.created > 0 || preview!!.updated > 0 || preview!!.rows.any { it.willInvite })

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📥 Importa la lista invitati", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Incolla le righe da Excel o da un CSV, una persona per riga (colonne Nome, Categoria, Email, Telefono, Persone, Stato: l'intestazione viene riconosciuta). Chi c'è già viene aggiornato (abbinato per email o per nome), gli altri vengono creati in attesa di risposta.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; preview = null; message = "" },
                    placeholder = { Text("Nome;Categoria;Email;Telefono;Persone\nMaria Rossi;Familiari;maria@esempio.it;333 1234567;2") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("import_text")
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = sendLinks, onCheckedChange = { sendLinks = it; preview = null }, modifier = Modifier.testTag("import_send_links"))
                    Text("Invia il link personale via email a chi ha l'indirizzo", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(
                    onClick = {
                        val parsed = GuestImport.parse(text)
                        rows = parsed.rows
                        if (rows.isEmpty()) { message = "Nessuna riga riconosciuta: controlla separatori e intestazione."; preview = null; return@OutlinedButton }
                        busy = true
                        viewModel.importGuests(rows, sendLinks, dryRun = true) { result ->
                            busy = false
                            preview = result
                            message = result?.let { r ->
                                val invites = r.rows.count { it.willInvite }
                                "Anteprima: ${r.created} da creare, ${r.updated} da aggiornare, ${r.unchanged} già uguali, ${r.skipped} senza nome" +
                                    (if (sendLinks) ", $invites email con il link" else "") + ".\n" +
                                    r.rows.take(8).joinToString(" · ") { x ->
                                        (x.fullName.ifBlank { "(senza nome)" }) + ": " + when (x.action) {
                                            "created" -> "nuovo"; "updated" -> "aggiorna " + x.changes.joinToString(", "); "skipped" -> "saltato"; else -> "uguale"
                                        }
                                    } + (if (r.rows.size > 8) " · … (${r.rows.size} righe)" else "")
                            } ?: ""
                        }
                    },
                    enabled = !busy && text.isNotBlank(),
                    modifier = Modifier.testTag("import_preview_button")
                ) { Text(if (busy) "Un attimo…" else "Anteprima") }
                if (message.isNotBlank()) {
                    Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("import_result"))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    busy = true
                    viewModel.importGuests(rows, sendLinks, dryRun = false) { result -> busy = false; if (result != null) onDone() }
                },
                enabled = canImport,
                colors = ButtonDefaults.buttonColors(containerColor = LaurelGold, contentColor = NeuroDarkNavy),
                modifier = Modifier.testTag("import_confirm_button")
            ) { Text("Importa ${rows.size} righe") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}
