package dev.example.customer.domain

import java.time.LocalDate

data class Booking(
    val bookingNumber: String,
    val bookingFrom: LocalDate,
    val bookingTo: LocalDate,
    val customer: Customer,
)

data class Customer(val name: String, val surname: String)

data class Ticker(
    val cash_amount: String,
    val currency: String,
    val declaration_date: String,
    val dividend_type: String,
    val ex_dividend_date: String,
    val frequency: String,
    val pay_date: String,
    val record_date: String,
    val ticker: String,
)

