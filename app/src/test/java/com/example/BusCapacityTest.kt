package com.example

import com.example.ui.BusCapacity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusCapacityTest {
    @Test
    fun `accetta finche ci sono posti`() {
        assertTrue(BusCapacity.canBook(alreadyBooked = 10, requested = 40, maxSeats = 50))
        assertTrue(BusCapacity.canBook(alreadyBooked = 0, requested = 1, maxSeats = 50))
    }

    @Test
    fun `rifiuta overbooking e richieste non valide`() {
        assertFalse(BusCapacity.canBook(alreadyBooked = 10, requested = 41, maxSeats = 50))
        assertFalse(BusCapacity.canBook(alreadyBooked = 50, requested = 1, maxSeats = 50))
        assertFalse(BusCapacity.canBook(alreadyBooked = 0, requested = 0, maxSeats = 50))
    }
}
