package com.example

import com.example.ui.BusCapacity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusCapacityTest {
    @Test
    fun `accetta finche ci sono posti`() {
        assertTrue(BusCapacity.canBook(alreadyBooked = 10, requested = 20, maxSeats = 30))
        assertTrue(BusCapacity.canBook(alreadyBooked = 0, requested = 1, maxSeats = 30))
    }

    @Test
    fun `rifiuta overbooking e richieste non valide`() {
        assertFalse(BusCapacity.canBook(alreadyBooked = 10, requested = 21, maxSeats = 30))
        assertFalse(BusCapacity.canBook(alreadyBooked = 30, requested = 1, maxSeats = 30))
        assertFalse(BusCapacity.canBook(alreadyBooked = 0, requested = 0, maxSeats = 30))
    }
}
