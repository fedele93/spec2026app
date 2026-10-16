package com.example.data

/**
 * Link personale dell'invitato, ricevuto via email (https://<app>/#profilo=<token>).
 * Nell'app si incolla il link intero (o solo il codice): da qui si ricava il token per l'header X-Guest-Token.
 */
object GuestLink {
    private val IN_LINK = Regex("[#&?]profilo=([A-Za-z0-9_-]{10,})")
    private val BARE = Regex("^[A-Za-z0-9_-]{10,}$")

    private val GRADUATE_LINK = Regex("[#&?]festeggiato=([A-Za-z0-9_-]{10,})")

    /** Token del festeggiato: dal link ricevuto via email (…#festeggiato=<token>) oppure il testo così com'è
     *  (token GRADUATE_TOKEN_<ID> dato dagli organizzatori). */
    fun graduateTokenFrom(input: String): String {
        val text = input.trim()
        return GRADUATE_LINK.find(text)?.groupValues?.get(1) ?: text
    }

    /** Il token contenuto nel testo incollato, oppure null se non sembra un link personale. */
    fun extractToken(input: String): String? {
        val text = input.trim()
        IN_LINK.find(text)?.let { return it.groupValues[1] }
        return if (BARE.matches(text)) text else null
    }
}
