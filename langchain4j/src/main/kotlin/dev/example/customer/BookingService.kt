package dev.example.customer

import dev.example.customer.domain.Booking
import dev.example.customer.domain.BookingCannotBeCancelledException
import dev.example.customer.domain.BookingNotFoundException
import dev.example.customer.domain.Customer
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class BookingService {
    fun getBookingDetails(bookingNumber: String, customerName: String, customerSurname: String): Booking {
        ensureExists(bookingNumber, customerName, customerSurname)

        // Imitating retrieval from DB
        val bookingFrom = LocalDate.now().plusDays(1)
        val bookingTo = LocalDate.now().plusDays(3)
        val customer = Customer(customerName, customerSurname)
        return Booking(bookingNumber, bookingFrom, bookingTo, customer)
    }

    fun cancelBooking(bookingNumber: String, customerName: String, customerSurname: String) {
        ensureExists(bookingNumber, customerName, customerSurname)

        // Imitating cancellation
        throw BookingCannotBeCancelledException(bookingNumber)
    }

    private fun ensureExists(bookingNumber: String, customerName: String, customerSurname: String) {
        // Imitating check
        if (!(bookingNumber == "123-456" && customerName == "Klaus" && customerSurname == "Heisler")) {
            throw BookingNotFoundException(bookingNumber)
        }
    }
}
