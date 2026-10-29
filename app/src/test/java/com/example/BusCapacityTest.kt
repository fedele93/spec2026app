package com.example

import com.example.ui.BusCapacity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusCapacityTest {
    @Test
    fun `accetta finche ci sono posti`() {
        assertTrue(BusCapacity.canBook(alreadyBooked = 10, requested = 30, maxSeats = 40))
        assertTrue(BusCapacity.canBook(alreadyBooked = 0, requested = 1, maxSeats = 40))
    }

    @Test
    fun `rifiuta overbooking e richieste non valide`() {
        assertFalse(BusCapacity.canBook(alreadyBooked = 10, requested = 31, maxSeats = 40))
        assertFalse(BusCapacity.canBook(alreadyBooked = 40, requested = 1, maxSeats = 40))
        assertFalse(BusCapacity.canBook(alreadyBooked = 0, requested = 0, maxSeats = 40))
    }
}
