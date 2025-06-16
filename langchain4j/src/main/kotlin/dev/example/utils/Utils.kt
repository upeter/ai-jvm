package dev.example.utils

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.StreamingResponseHandler
import dev.langchain4j.model.chat.StreamingChatModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.LoggerFactory
import reactor.core.publisher.Sinks
import java.io.*
inline val <reified T> T.logger
    get() = LoggerFactory.getLogger(T::class.java)


fun String.toCoordinate() =  this.replace(".", "").replace("E7", "").replace("E8", "").let{n -> (if(n.startsWith("-")) 3 else 2).let{n.take(it) + "." + n.drop(it)}.toDouble()}

val googleMapsKey = File("/Users/urs/.googlemaps").readText().trim()

fun googleMapsMarker(color:String = "blue", label:String, lat:Double, lon:Double) =
    "&markers=color:${color}|label:${label.first().uppercase()}|${lat.toString().toCoordinate()},${lon.toString().toCoordinate()}"

val propertyColors = (listOf("apartment", "hotel", "restaurant", "shop","industrial") zip listOf("green", "yellow", "blue", "red", "black")).toMap()

fun getDayPeriodAll(hourOfDay: Int): String {
    return when (hourOfDay) {
        in 5..7 -> "Early-Morning-5_7" // Covers 5:00 AM to 7:59 AM
        in 8..10 -> "Breakfast-Time-8_10" // Covers 8:00 AM to 10:59 AM
        in 11..13 -> "Lunch-Time-11_13" // Covers 11:00 AM to 1:59 PM
        in 14..16 -> "Afternoon-14_16" // Covers 2:00 PM to 4:59 PM
        in 17..19 -> "Evening-17_19" // Covers 5:00 PM to 7:59 PM
        in 20..20 -> "Dinner-Time-20_20" // Specifically covers 8:00 PM to 8:59 PM
        in 21..23 -> "Night-21_23" // Covers 9:00 PM to 11:59 PM
        in 0..4 -> "Late-Night-0_4" // Covers 12:00 AM to 4:59 AM
        24, 0 -> "Midnight-0_1" // Specifically covers the transition at midnight from 12:00 AM to 1:00 AM
        else -> "Invalid-Hour"
    }
}

