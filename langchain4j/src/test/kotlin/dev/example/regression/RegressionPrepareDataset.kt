package dev.example.regression

import smile.data.vector.StringVector
import smile.io.Read
import smile.io.Write
import java.nio.file.Path

fun main() {
    adjustDataset()
}

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

fun getDayPeriod(hourOfDay: Int): String {
    return when (hourOfDay) {
        in 8..10 -> "Breakfast-Time" // Covers 8:00 AM to 10:59 AM
        in 11..14 -> "Lunch-Time" // Covers 11:00 AM to 1:59 PM
        in 18..20 -> "Dinner-Time" // Specifically covers 8:00 PM to 8:59 PM
        else -> "No-Food-Time"
    }
}

fun adjustDataset() {
    val data = Read.csv("/Users/urs/development/github/ai/kotlin-ai-talk/langchain4j/src/main/resources/dataset/burglaries_simpel.csv", "header=true")
    println(data)
    println(data.summary())
//    val rf = RandomForest.fit(Formula.lhs("class"), data)
//    println("OOB error = ${rf.metrics()}")

    val timeWindow = data.intVector("hour").toIntArray().map { hour -> getDayPeriod(hour) }.toTypedArray()

    // Add the 'timeWindow' as a column in DataFrame
    val updatedData = data.merge(StringVector.of("timeWindow", * timeWindow))

    println(updatedData)

    Write.csv(updatedData.drop("amountStolen"), Path.of("/Users/urs/development/github/ai/kotlin-ai-talk/langchain4j/src/main/resources/dataset/burglaries_enriched_2.csv"))

}




