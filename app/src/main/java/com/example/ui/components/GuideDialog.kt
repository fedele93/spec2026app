package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Guide dell'app (stesse della PWA, adattate alle funzioni dell'app Android).
 * 1) Guida alla prima apertura: cosa fa ogni scheda e da dove cominciare. Si apre da sola la prima
 *    volta ([com.example.data.AppPrefs.guideSeen]), poi dal pulsante "?" nella tab Programma e da ⚙️.
 * 2) Guide dei ruoli: quando in ⚙️ si salva un token che il server riconosce (organizzatore,
 *    festeggiato; campo "roles" di GET /api/state) si apre una volta sola la guida di quel ruolo,
 *    con le funzioni sbloccate e dove trovarle. Si riapre da ⚙️.
 */
data class GuideStep(val icon: String, val title: String, val text: String, val tip: String = "")

/** Ruoli con una guida dedicata nell'app Android (il cruscotto cassiere esiste solo nella PWA). */
object GuideRoles {
    const val ADMIN = "admin"
    const val GRADUATE = "graduate"
    val LABELS = mapOf(ADMIN to "organizzatore", GRADUATE to "festeggiato")
}

/** Passi della guida generale. [serverMode] = backend configurato (altrimenti modalità locale/demo). */
fun welcomeGuideSteps(serverMode: Boolean): List<GuideStep> {
    val steps = mutableListOf(
        GuideStep(
            "🎓", "Benvenuto a NeuroParty",
            "Questa è l'app della festa di specializzazione in Neurologia: programma, conferma della presenza, navetta, auguri, foto e regali, tutto in un posto. Non serve un account: l'app riconosce questo telefono e ti chiede solo il nome.",
            "Le notifiche degli organizzatori (cambi di orario, partenza della navetta) arrivano come notifiche di sistema: accetta il permesso quando te lo chiede."
        ),
        GuideStep(
            "🧭", "Come è fatta",
            "Le cinque schede in basso sono tutto quello che c'è: Programma, Invitati, Navetta, Bacheca e Regali. In alto nella scheda Programma trovi la campanella con gli avvisi degli organizzatori, il pulsante ? per riaprire questa guida e ⚙️ per le impostazioni.",
            "L'icona ⚙️ diventa dorata quando il server non è raggiungibile: l'app mostra gli ultimi dati scaricati."
        ),
        GuideStep(
            "📅", "Programma e mappa",
            "In Programma vedi la seduta di specializzazione e la festa con orari e luoghi. Tocca un punto sulla mappa per i dettagli e il pulsante per aprire il navigatore; con i chip del calendario aggiungi seduta e festa al calendario del telefono.",
            "Nella stessa scheda scorrono gli ultimi auguri: tocca il banner per andare alla Bacheca."
        ),
        GuideStep(
            "👥", "Conferma la presenza",
            "In Invitati tocca il pulsante + e inserisci nome, categoria e quante persone vengono con te: la tua risposta vale subito. Puoi cambiarla quando vuoi (confermato, in attesa, declinato) e indicare menu e allergie.",
            if (serverMode) "Vedi e modifichi le persone inserite da questo telefono; l'elenco completo lo vedono solo organizzatori e neo-specialisti." else "In questa modalità i dati restano solo su questo telefono."
        ),
        GuideStep(
            "🚌", "Navetta gratuita",
            "In Navetta prenoti i posti sul pullman: numero di persone, fermata di salita e se ti serve anche il ritorno notturno. Ricevi il codice della prenotazione; se i posti sono finiti entri in lista d'attesa e vieni avvisato appena se ne libera uno.",
            "Alla partenza basta dire il tuo nome o mostrare il codice della prenotazione."
        ),
        GuideStep(
            "🎉", "Bacheca: auguri e foto",
            "In Bacheca lasci un augurio ai neo-specialisti, a tutti o a uno in particolare, con un'emoji, e carichi le foto della giornata dalla galleria o dalla fotocamera. I cuori e i like sono per tutti.",
            "Le foto vengono ridimensionate in automatico prima dell'invio."
        ),
        GuideStep(
            "🎁", "Regali",
            "In Regali ci sono due strade. Regalo diretto: ogni neo-specialista ha IBAN, PayPal e, dove c'è, Satispay; copi e versi, senza registrare nulla. Quota unica: un solo versamento a uno dei cassieri, che lo ripartisce fra i neo-specialisti come indichi tu (in parti uguali o personalizzato).",
            "Nessuna cifra raccolta viene mostrata: ognuno decide liberamente. Se lasci l'email, il cassiere ti conferma quando la quota è arrivata."
        ),
        GuideStep(
            "🔑", "Hai un ruolo nella festa?",
            "Organizzatori e neo-specialisti ricevono un token personale. Inseriscilo in ⚙️ Impostazioni: si sbloccano le funzioni del tuo ruolo (cruscotto, import invitati, area festeggiato) e l'app ti spiega come usarle.",
            "Se sei un invitato non ti serve nessun token: puoi ignorare quei campi. Il cruscotto dei cassieri è nella versione web dell'app."
        ),
        GuideStep(
            "✅", "Da dove cominciare",
            "1. In Invitati conferma la presenza con il pulsante +. 2. In Navetta prenota i posti, se ti servono. 3. Accetta le notifiche per non perdere gli avvisi. Il resto, auguri, foto e regali, quando vuoi.",
            "Puoi riaprire questa guida quando vuoi dal pulsante ? nella scheda Programma."
        )
    )
    return steps
}

/** Passi della guida di un ruolo. [graduateName] = nome (senza titolo) del festeggiato, se noto. */
fun roleGuideSteps(role: String, graduateName: String = ""): List<GuideStep> = when (role) {
    GuideRoles.ADMIN -> listOf(
        GuideStep(
            "🗂️", "Sei un organizzatore",
            "Il token organizzatore è attivo su questo telefono. In Invitati compare la card Organizzatori con cruscotto e import della lista; nelle schede compaiono i comandi riservati: invio notifiche, cancellazione di invitati, prenotazioni, foto e auguri.",
            "Il token vale solo su questo telefono: su un altro dispositivo va reinserito. Non condividerlo con gli invitati."
        ),
        GuideStep(
            "📊", "Cruscotto e cose da fare",
            "Il cruscotto riassume in tempo reale confermati, menu ancora da scegliere, posti sulla navetta e lista d'attesa, email non consegnate e il numero di quote uniche registrate. Sotto, la lista delle cose da fare con le scadenze.",
            "Apri il cruscotto dalla card Organizzatori nella scheda Invitati."
        ),
        GuideStep(
            "📥", "Importa gli invitati",
            "Incolla la lista (nome, categoria, email, telefono, persone) copiata da Excel, Google Fogli o WhatsApp: l'app crea i profili e, se vuoi, manda a ciascuno l'email con il link personale. Chi esiste già viene aggiornato, non duplicato.",
            "Prima usa l'anteprima: vedi cosa verrebbe creato senza salvare né inviare nulla."
        ),
        GuideStep(
            "🔔", "Notifiche e pulizia",
            "Dalla scheda Programma invii una notifica push a tutti (cambi di orario, partenza della navetta, foto), anche programmata. In Bacheca puoi eliminare foto e auguri inopportuni; dalla campanella svuoti la cronologia delle notifiche.",
            "Le cancellazioni sono definitive: l'app chiede conferma ogni volta."
        ),
        GuideStep(
            "🔒", "Cosa non vedi",
            "Le cifre delle quote uniche restano ai cassieri: tu vedi solo quante quote sono state registrate. Gli elenchi completi di invitati e navetta sono visibili a organizzatori, cassieri e neo-specialisti; gli invitati vedono solo i propri dati. Esportazioni CSV ed email si gestiscono dalla versione web.",
            "Riapri questa guida da ⚙️ Impostazioni → Guida organizzatore."
        )
    )
    GuideRoles.GRADUATE -> listOf(
        GuideStep(
            "🎓", if (graduateName.isNotBlank()) "Ciao ${graduateName.substringBefore(' ')}, questa è la tua area" else "Sei un neo-specialista",
            "Il token festeggiato è attivo: nella scheda Programma compare la card Area festeggiato, aggiornata in tempo reale. Qui sotto cosa ci trovi.",
            "Il token vale solo su questo telefono. Non condividerlo."
        ),
        GuideStep(
            "👥", "Chi viene",
            "Quanti hanno confermato, gli accompagnatori, i menu scelti e chi ha prenotato la navetta. In Invitati e Navetta vedi anche gli elenchi completi, che agli invitati non sono visibili.",
            "Chi manca all'appello? Puoi chiedere agli organizzatori di mandargli il link personale."
        ),
        GuideStep(
            "💌", "Gli auguri per te",
            "I messaggi indirizzati a te, con i cuori ricevuti; quelli per tutti restano in Bacheca.",
            "I messaggi e le foto li possono eliminare solo gli organizzatori."
        ),
        GuideStep(
            "💳", "Le tue coordinate",
            "IBAN, PayPal e Satispay come li vedono gli invitati per il regalo diretto. Controlla che siano giusti: se c'è da correggere qualcosa, avvisa gli organizzatori (si cambiano nel file dell'evento). Le quote uniche sono riservate ai cassieri: niente cifre né elenco dei donatori, nemmeno per te.",
            "Riapri questa guida da ⚙️ Impostazioni → Guida festeggiato."
        )
    )
    else -> emptyList()
}

/** Dialogo a passi con puntini, Indietro/Avanti e "Salta la guida". [onFinish] alla chiusura (anche saltata). */
@Composable
fun GuideDialog(steps: List<GuideStep>, role: String? = null, onFinish: () -> Unit) {
    if (steps.isEmpty()) { onFinish(); return }
    var index by remember { mutableStateOf(0) }
    val step = steps[index]
    val last = index == steps.size - 1

    AlertDialog(
        onDismissRequest = onFinish,
        modifier = Modifier.testTag(if (role == null) "guide_dialog" else "guide_dialog_$role"),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(step.icon, fontSize = 44.sp, modifier = Modifier.padding(top = 4.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    step.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("guide_title")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(step.text, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                if (step.tip.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "💡 ${step.tip}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    steps.indices.forEach { i ->
                        Box(
                            modifier = Modifier
                                .height(7.dp)
                                .width(if (i == index) 16.dp else 7.dp)
                                .background(
                                    if (i == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    CircleShape
                                )
                        )
                    }
                }
                if (!last) {
                    TextButton(onClick = onFinish, modifier = Modifier.testTag("guide_skip")) {
                        Text("Salta la guida", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (last) onFinish() else index += 1 },
                modifier = Modifier.testTag("guide_next")
            ) {
                Text(if (!last) "Avanti →" else if (role == null) "Inizia! 🎉" else "Ho capito ✓")
            }
        },
        dismissButton = {
            if (index > 0) {
                TextButton(onClick = { index -= 1 }, modifier = Modifier.testTag("guide_prev")) { Text("← Indietro") }
            }
        }
    )
}
