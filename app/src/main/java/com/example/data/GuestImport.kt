package com.example.data

import com.example.data.remote.ImportRowRequest
import java.text.Normalizer

/**
 * Riconosce le righe incollate da Excel o da un CSV (una persona per riga) e le trasforma nelle
 * righe dell'import. Stessa logica della PWA (pwa/js/admin.js): separatore scelto fra ; tab e , ,
 * intestazione riconosciuta dai nomi delle colonne (maiuscole e accenti non contano), altrimenti
 * l'ordine fisso Nome;Categoria;Email;Telefono;Persone;Stato.
 */
object GuestImport {
    private val COLUMNS: Map<String, List<String>> = mapOf(
        "fullName" to listOf("nome", "nome e cognome", "nominativo", "invitato", "name"),
        "category" to listOf("categoria", "gruppo", "category"),
        "email" to listOf("email", "e-mail", "mail", "posta"),
        "phone" to listOf("telefono", "cellulare", "tel", "phone"),
        "guestsCount" to listOf("persone", "posti", "coperti", "n", "numero", "guests"),
        "rsvpStatus" to listOf("stato", "rsvp", "status")
    )
    private val DEFAULT_ORDER = listOf("fullName", "category", "email", "phone", "guestsCount", "rsvpStatus")
    private val STATUS_WORDS = mapOf(
        "confermato" to RsvpStatus.CONFIRMED, "confirmed" to RsvpStatus.CONFIRMED, "si" to RsvpStatus.CONFIRMED,
        "in attesa" to RsvpStatus.PENDING, "attesa" to RsvpStatus.PENDING, "pending" to RsvpStatus.PENDING,
        "declinato" to RsvpStatus.DECLINED, "declined" to RsvpStatus.DECLINED, "no" to RsvpStatus.DECLINED
    )

    data class Parsed(val rows: List<ImportRowRequest>, val hasHeader: Boolean, val separator: Char)

    private fun norm(s: String): String =
        Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    fun parse(text: String): Parsed {
        val lines = text.split(Regex("\\r?\\n")).map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return Parsed(emptyList(), false, ';')
        val sep = listOf(';', '\t', ',').maxByOrNull { c -> lines[0].count { it == c } } ?: ';'
        fun split(line: String) = line.split(sep).map { it.trim().removeSurrounding("\"") }
        val first = split(lines[0]).map(::norm)
        val map = first.map { h -> COLUMNS.entries.firstOrNull { h in it.value }?.key }
        val hasHeader = map.any { it != null } && first.none { it.contains("@") }
        val order = if (hasHeader) map else DEFAULT_ORDER
        val rows = lines.drop(if (hasHeader) 1 else 0).mapNotNull { line ->
            val cells = split(line)
            var fullName = ""; var category = ""; var email = ""; var phone = ""
            var guestsCount: Int? = null; var status: RsvpStatus? = null
            order.forEachIndexed { i, key ->
                val cell = cells.getOrNull(i)?.takeIf { it.isNotEmpty() } ?: return@forEachIndexed
                when (key) {
                    "fullName" -> fullName = cell
                    "category" -> category = cell
                    "email" -> email = cell.lowercase()
                    "phone" -> phone = cell
                    "guestsCount" -> cell.toIntOrNull()?.let { guestsCount = it }
                    "rsvpStatus" -> STATUS_WORDS[norm(cell)]?.let { status = it }
                }
            }
            if (fullName.isEmpty() && email.isEmpty()) null
            else ImportRowRequest(fullName = fullName, category = category, email = email, phone = phone, guestsCount = guestsCount, rsvpStatus = status)
        }
        return Parsed(rows, hasHeader, sep)
    }
}
