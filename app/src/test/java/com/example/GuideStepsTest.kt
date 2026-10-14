package com.example

import com.example.data.remote.RemoteClient
import com.example.data.remote.StateDto
import com.example.ui.components.GuideRoles
import com.example.ui.components.roleGuideSteps
import com.example.ui.components.welcomeGuideSteps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guida alla prima apertura, guide dei ruoli e campo roles di /api/state. */
class GuideStepsTest {
    @Test
    fun `guida generale: nove passi, schede e da dove cominciare`() {
        val steps = welcomeGuideSteps(serverMode = true)
        assertEquals(9, steps.size)
        assertTrue(steps.first().title.contains("Benvenuto"))
        assertTrue(steps.last().title.contains("Da dove cominciare"))
        for (needle in listOf("Programma", "Conferma la presenza", "Navetta", "Bacheca", "Regali", "ruolo")) {
            assertTrue(needle, steps.any { it.title.contains(needle) })
        }
        val roles = steps.first { it.title.contains("ruolo") }
        assertTrue(roles.text.contains("token") && roles.text.contains("Impostazioni"))
        // in modalità locale il passo sulla presenza avvisa che i dati restano sul telefono
        assertTrue(welcomeGuideSteps(serverMode = false).first { it.title.contains("Conferma") }.tip.contains("solo su questo telefono"))
        assertTrue(steps.all { it.text.isNotBlank() && it.icon.isNotBlank() })
    }

    @Test
    fun `guide dei ruoli: organizzatore e festeggiato con il nome`() {
        val admin = roleGuideSteps(GuideRoles.ADMIN)
        assertEquals(5, admin.size)
        assertTrue(admin.first().title.contains("organizzatore"))
        assertTrue(admin.any { it.text.contains("cruscotto", ignoreCase = true) } && admin.any { it.title.contains("Importa") })
        val grad = roleGuideSteps(GuideRoles.GRADUATE, graduateName = "Fedele Luisi")
        assertEquals(4, grad.size)
        assertEquals("Ciao Fedele, questa è la tua area", grad.first().title)
        assertTrue(roleGuideSteps(GuideRoles.GRADUATE).first().title.contains("neo-specialista"))
        assertTrue(roleGuideSteps("cassiere").isEmpty())
        assertEquals(setOf(GuideRoles.ADMIN, GuideRoles.GRADUATE), GuideRoles.LABELS.keys)
    }

    @Test
    fun `roles di api state letto da Moshi, assente sui server vecchi`() {
        val adapter = RemoteClient.moshi.adapter(StateDto::class.java)
        val withRoles = adapter.fromJson("""{"version":3,"serverTime":1,"adminEnabled":true,"canSeeLists":true,"roles":{"admin":false,"treasurer":true,"treasurerIds":["aki"],"graduate":"luisi"}}""")
        assertNotNull(withRoles?.roles)
        assertFalse(withRoles!!.roles!!.admin)
        assertTrue(withRoles.roles!!.treasurer)
        assertEquals(listOf("aki"), withRoles.roles!!.treasurerIds)
        assertEquals("luisi", withRoles.roles!!.graduate)
        val old = adapter.fromJson("""{"version":3,"serverTime":1}""")
        assertNull(old!!.roles)
        assertTrue(old.canSeeLists)
    }
}
