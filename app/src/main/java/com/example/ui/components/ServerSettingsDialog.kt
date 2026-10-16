package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.ServerStatus

/** Impostazioni del backend condiviso: URL del server e token organizzatore. */
@Composable
fun ServerSettingsDialog(
    status: ServerStatus,
    currentUrl: String,
    currentToken: String,
    onSave: (url: String, token: String, graduateToken: String) -> Unit,
    onDismiss: () -> Unit,
    currentGraduateToken: String = "",
    activeRoles: List<String> = emptyList(),      // ruoli confermati dal server: pulsanti "Guida <ruolo>"
    onOpenGuide: (() -> Unit)? = null,            // riapre la guida alla prima apertura
    onOpenRoleGuide: ((String) -> Unit)? = null,
    guestName: String? = null,                    // profilo invitato collegato (foto e auguri), null = nessuno
    onDisconnectGuest: (() -> Unit)? = null,
    onRequestGraduateLink: ((String) -> Unit)? = null // festeggiato: link della propria area via email
) {
    var url by remember { mutableStateOf(currentUrl) }
    var token by remember { mutableStateOf(currentToken) }
    var graduateToken by remember { mutableStateOf(currentGraduateToken) }
    var graduateEmail by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⚙️ Server & Organizzatori",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val stateText = when {
                    !status.configured -> "Modalità locale: i dati restano solo su questo telefono."
                    status.online -> "Connesso al server (versione dati ${status.version})."
                    else -> "Server non raggiungibile: mostro gli ultimi dati scaricati." +
                        (status.lastError?.let { "\n$it" } ?: "")
                }
                Text(
                    text = stateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL del server (es. https://festa.tuodominio.it)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("server_url_input")
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Token organizzatore (opzionale)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_token_input")
                )
                Text(
                    text = "Con il token puoi inviare notifiche push a tutti gli invitati e cancellare qualsiasi record.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = graduateToken,
                    onValueChange = { graduateToken = it },
                    label = { Text("Token o link festeggiato (solo neo-specialisti)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("graduate_token_input")
                )
                Text(
                    text = "Con il tuo token personale compare la card 🎓 Area festeggiato: chi viene, gli auguri per te, le tue coordinate di pagamento. Va bene anche il link ricevuto via email (…#festeggiato=…): incollalo qui.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (onRequestGraduateLink != null && status.configured) {
                    OutlinedTextField(
                        value = graduateEmail,
                        onValueChange = { graduateEmail = it },
                        label = { Text("Sei un festeggiato? La tua email") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("graduate_email_input")
                    )
                    OutlinedButton(
                        onClick = { onRequestGraduateLink(graduateEmail.trim()) },
                        enabled = graduateEmail.isNotBlank(),
                        modifier = Modifier.testTag("request_graduate_link_button")
                    ) { Text("📧 Mandami il link della mia area") }
                }
                if (guestName != null && onDisconnectGuest != null) {
                    Text(
                        text = "👤 Profilo invitato collegato" + (if (guestName.isNotBlank()) ": $guestName" else "") +
                            ". Con il profilo vedi e pubblichi foto e auguri.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onDisconnectGuest, modifier = Modifier.testTag("disconnect_guest_button")) {
                        Text("Scollega il profilo da questo telefono")
                    }
                }
                if (activeRoles.isNotEmpty() && onOpenRoleGuide != null) {
                    Text(
                        text = "🔑 Guida del tuo ruolo: le funzioni sbloccate dal tuo token e dove trovarle (si apre da sola la prima volta che il server riconosce il token).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        activeRoles.forEach { role ->
                            OutlinedButton(
                                onClick = { onOpenRoleGuide(role) },
                                modifier = Modifier.testTag("role_guide_$role")
                            ) { Text("Guida ${GuideRoles.LABELS[role] ?: role}") }
                        }
                    }
                }
                if (onOpenGuide != null) {
                    TextButton(onClick = onOpenGuide, modifier = Modifier.testTag("reopen_guide_button")) {
                        Text("❓ Riapri la guida all'app")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(url.trim(), token.trim(), graduateToken.trim()) },
                modifier = Modifier.testTag("save_server_settings_button")
            ) {
                Text("Salva e connetti")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}
