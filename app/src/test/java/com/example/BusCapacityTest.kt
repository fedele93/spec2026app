package com.example

import com.example.ui.BusCapacity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusCapacityTest {
    @Test
    fun `accetta finche ci sono posti`() {
        assertTrue(BusCapacity.canBook(alreadyBooked = 10, requested = 44, maxSeats = 54))
        assertTrue(BusCapacity.canBook(alreadyBooked = 0, requested = 1, maxSeats = 54))
    }

    @Test
    fun `rifiuta overbooking e richieste non valide`() {
        assertFalse(BusCapacity.canBook(alreadyBooked = 10, requested = 45, maxSeats = 54))
        assertFalse(BusCapacity.canBook(alreadyBooked = 54, requested = 1, maxSeats = 54))
        assertFalse(BusCapacity.canBook(alreadyBooked = 0, requested = 0, maxSeats = 54))
    }
}
