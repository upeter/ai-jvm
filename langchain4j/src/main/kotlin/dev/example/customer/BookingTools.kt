package dev.example.customer

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.example.customer.domain.Booking
import dev.example.customer.domain.Ticker
import dev.langchain4j.agent.tool.Tool
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.Properties

@Component
class BookingTools(val bookingService: BookingService){

    @Tool
    fun getBookingDetails(bookingNumber: String, customerName: String, customerSurname: String): Booking {
        println("==========================================================================================")
        System.out.printf(
            "[Tool]: Getting details for booking %s for %s %s...%n",
            bookingNumber,
            customerName,
            customerSurname
        )
        println("==========================================================================================")

        return bookingService.getBookingDetails(bookingNumber, customerName, customerSurname)
    }

    @Tool
    fun cancelBooking(bookingNumber: String, customerName: String, customerSurname: String) {
        println("==========================================================================================")
        System.out.printf("[Tool]: Cancelling booking %s for %s %s...%n", bookingNumber, customerName, customerSurname)
        println("==========================================================================================")

        bookingService.cancelBooking(bookingNumber, customerName, customerSurname)
    }




}

@Component
class FinancialTools {


    @Tool("Provides historical financial data for a stock ticker. The financials data is extracted from XBRL from company SEC filings. Provides the following information: Balance sheet, Income statement, Statement of comprehensive Income, Cash flow statement,  ")
    fun getFinancial(ticker: String): List<Ticker> {
        println("==========================================================================================")
        println("Getting financial info for: $ticker")
        println("==========================================================================================")
        val financialResults = getFinancials(ticker)
        return financialResults
    }


    companion object {
        val mapper = jacksonObjectMapper()

        fun getFinancials(ticker: String): List<Ticker> {
            val restTemplate = RestTemplate()
            val response = restTemplate.getForEntity(formUrl(ticker), String::class.java)
            val financialJson = response.body

            println("==========================================================================================")
            println("Json: $financialJson")
            println("==========================================================================================")
            val factory = mapper.factory
            // to prevent exception when encountering unknown property:
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            // to allow coercion of JSON empty String ("") to null Object value:
            mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            val parser = factory.createParser(financialJson)
            val root = mapper.readTree<JsonNode>(parser)
            val resultList = mapper.readValue<List<Ticker>>(root["results"].toPrettyString())
            return resultList
        }
        val POLYGON_API_KEY = Properties().apply { load(java.io.File("/Users/urs/development/github/ai/kotlin-ai-talk/key/open-api-key.txt").inputStream()) }.getProperty("poligion.io")


        private fun formUrl(ticker: String): String =
            "https://api.polygon.io/v3/reference/dividends?apiKey=${POLYGON_API_KEY}&ticker=$ticker"


    }
}

fun main() {
    println(FinancialTools.getFinancials("IBM"))
}
