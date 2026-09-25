package com.example.util

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import java.util.Calendar

/**
 * Apre l'app Calendario del telefono con l'evento precompilato (nessuna dipendenza Google:
 * è un semplice Intent ACTION_INSERT). Date in formato "AAAA-MM-GG", orario "HH:MM" oppure
 * vuoto = evento di tutto il giorno.
 */
object CalendarHelper {

    fun addEvent(
        context: Context,
        title: String,
        location: String,
        description: String,
        dateIso: String,
        time: String,
        durationHours: Int
    ) {
        val parts = dateIso.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) {
            Toast.makeText(context, "Data dell'evento non ancora disponibile", Toast.LENGTH_SHORT).show()
            return
        }
        val hm = Regex("^(\\d{1,2})[:.](\\d{2})$").find(time.trim())?.groupValues?.let { it[1].toInt() to it[2].toInt() }
        val start = Calendar.getInstance().apply {
            clear()
            set(parts[0], parts[1] - 1, parts[2], hm?.first ?: 0, hm?.second ?: 0, 0)
        }
        val endMillis = if (hm == null) {
            start.timeInMillis + 24L * 3600_000L
        } else {
            start.timeInMillis + durationHours * 3600_000L
        }
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.timeInMillis)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, hm == null)
            .putExtra(CalendarContract.Events.TITLE, title)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, location)
            .putExtra(CalendarContract.Events.DESCRIPTION, description)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Nessuna app Calendario trovata sul dispositivo", Toast.LENGTH_SHORT).show()
        }
    }
}
