package dev.example.customer.domain

class BookingCannotBeCancelledException(bookingNumber: String) :
    RuntimeException("Booking $bookingNumber cannot be canceled")

class BookingNotFoundException(bookingNumber: String) : RuntimeException("Booking $bookingNumber not found")