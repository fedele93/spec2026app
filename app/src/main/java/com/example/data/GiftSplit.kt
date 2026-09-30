package com.example.data

import com.example.data.remote.GiftPoolAllocation
import com.example.data.remote.GiftPoolContribution
import com.example.data.remote.PoolContributionRequest
import kotlin.math.roundToLong

/**
 * Ripartizione di una quota unica fra i neo-specialisti: stessa logica del backend
 * (app/routers/gifts.py) e della PWA, usata per l'anteprima nel dialogo e in modalità locale.
 * Si lavora in centesimi per non perdere nulla negli arrotondamenti.
 */
object GiftSplit {
    fun cents(amount: Double): Long = (amount * 100).roundToLong()

    /** Divide i centesimi in parti uguali; i centesimi di resto vanno ai primi della lista. */
    fun splitEqually(totalCents: Long, count: Int): List<Long> {
        require(count > 0) { "Nessun neo-specialista da includere nella quota" }
        val base = totalCents / count
        val rest = (totalCents % count).toInt()
        return List(count) { i -> base + if (i < rest) 1 else 0 }
    }

    /** Costruisce la quota (con la ripartizione) come farebbe il server; lancia IllegalArgumentException con
     *  un messaggio per l'utente se la richiesta non è valida. */
    fun build(request: PoolContributionRequest, targets: List<GiftTargetEntity>, id: Long = 0): GiftPoolContribution {
        require(request.donorName.isNotBlank()) { "Il nome è obbligatorio: serve al cassiere per riconoscere il versamento" }
        val byId = targets.associateBy { it.id }
        val allocations: List<GiftPoolAllocation>
        if (request.splitMode == "CUSTOM") {
            val rows = request.allocations.filter { it.graduateId in byId && cents(it.amount) > 0 }
            require(rows.isNotEmpty()) { "Indica almeno un importo personalizzato" }
            require(rows.map { it.graduateId }.distinct().size == rows.size) { "Neo-specialista indicato due volte" }
            allocations = rows.map { GiftPoolAllocation(it.graduateId, byId.getValue(it.graduateId).name, cents(it.amount) / 100.0) }
        } else {
            val total = cents(request.totalAmount ?: 0.0)
            require(total > 0) { "Inserisci un importo valido" }
            val ids = (request.graduateIds.ifEmpty { targets.map { it.id } }).filter { it in byId }
            require(ids.isNotEmpty()) { "Nessun neo-specialista da includere nella quota" }
            require(ids.distinct().size == ids.size) { "Neo-specialista indicato due volte" }
            require(total >= ids.size) { "Importo troppo basso per essere diviso fra i neo-specialisti scelti" }
            val parts = splitEqually(total, ids.size)
            allocations = ids.mapIndexed { i, g -> GiftPoolAllocation(g, byId.getValue(g).name, parts[i] / 100.0) }
        }
        val totalCents = allocations.sumOf { cents(it.amount) }
        return GiftPoolContribution(
            id = id,
            donorName = request.donorName.trim(),
            contact = request.contact.trim(),
            paymentMethod = request.paymentMethod,
            splitMode = request.splitMode,
            totalAmount = totalCents / 100.0,
            note = request.note.trim(),
            status = "PENDING",
            createdAt = System.currentTimeMillis(),
            allocations = allocations
        )
    }
}

/** "1234.5" -> "1.234,50 €" (formato italiano). */
fun formatEuro(amount: Double): String {
    val nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.ITALY)
    nf.minimumFractionDigits = 2
    nf.maximumFractionDigits = 2
    return nf.format(amount) + " €"
}
