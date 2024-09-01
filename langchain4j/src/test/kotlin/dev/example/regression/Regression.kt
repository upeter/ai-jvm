package dev.example.regression

import io.kotest.assertions.print.printWithType
import smile.classification.DecisionTree
import smile.data.formula.Formula
import smile.io.Read

fun main() {
//
//// Load your dataset. Assuming CSV format for simplicity
//    val dataPath:String = "/Users/urs/development/github/ai/kotlin-ai-talk/langchain4j/src/main/resources/dataset/burglaries_enriched.csv"
//    val rawData: DataFrame = read.csv(dataPath, header=true)
//
//// Transform 'hourOfDay' into categorical time windows: morning (0), afternoon (1), evening (2)
//    var data = rawData
//        .merge(IntVector.of("dayOfWeekNumber", rawData.stringVector("DayOfWeek").toStringArray().map { dayOfWeek ->
//            when (dayOfWeek) {
//                "MONDAY" -> 1
//                "TUESDAY" -> 2
//                "WEDNESDAY" -> 3
//                "THURSDAY" -> 4
//                "FRIDAY" -> 5
//                "SATURDAY" -> 6
//                "SUNDAY" -> 7
//                else -> 0
//            }
//        }.toIntArray()))
//        .merge(StringVector.of("class", * rawData.intVector("hourOfDay").toIntArray().map { hour ->
//            when (hour) {
//                in 0..11 -> "Morning"   // Morning
//                in 12..17 -> "Afternoon"  // Afternoon
//                else -> "Evening"       // Evening
//            }
//        }.toTypedArray()))
//        .drop("DayOfWeek").drop("CategoryOfItemsStolen").drop("TypeOfProperty").drop("AmountStolen")
//
//// Assuming 'dayOfWeek' and 'AmountStolen' are already suitable for use and do not require transformation
//// Setup formula: Predicting time windows as multi-labels
//    val formula = Formula.lhs("class")
//
//// Convert categorical features to dummy variables if needed
//    data = formula.frame(data)
//    println("Feature Schema: \n${data}")
//    println(data.summary())
//
//// Split data into training and test sets
//
////val splits = (data, 0.8)
////val trainData = splits.train
////val testData = splits.test
////
//    //Train the model
//    val model = RandomForest.fit(formula, data)
//    println("OOB error = ${rf.metrics()}")
//// Predict on test data


    // Load data
//    val path = "/Users/urs/development/github/ai/kotlin-ai-talk/langchain4j/src/main/resources/dataset/burglaries_enriched_2.csv"
//    val data = Read.csv(path, "header=true")
    val data = Read.arff("/Users/urs/development/github/ai/kotlin-ai-talk/langchain4j/src/main/resources/dataset/burglaries.arff")
    val updatedData = data
    println(updatedData)
    println(updatedData.summary())
    val rf = DecisionTree.fit(Formula.lhs("class"), updatedData)
    println("OOB error = ${rf.printWithType()}")
//
    // Assume 'hour' is already transformed to 'timeWindow' in your CSV or do it programmatically as below
    // Transform 'hour' into 'timeWindow' categories
//    val timeWindow = data.intVector("hour").toIntArray().map { hour -> getDayPeriod(hour) }.toTypedArray()
//
//    // Add the 'timeWindow' as a column in DataFrame
//    val updatedData = data.merge(StringVector.of("timeWindow", * timeWindow))
//
////    println(updatedData)
//
//    Write.csv(updatedData, Path.of("/Users/urs/development/github/ai/kotlin-ai-talk/langchain4j/src/main/resources/dataset/burglaries_enriched_2.csv"))

    // Define the formula representing the machine learning problem
//    val formula = Formula.lhs("class")
//
//    // Train a Random Forest model
//    val model = RandomForest.fit(formula, updatedData)
//
//    // Example prediction for a new day
//    // Define new data with dayOfWeek = 1 (Monday), hour = 10 (morning), amountStolen = 5000
//    val hours = intArrayOf(10)  // Example hour
//    val amountsStolen = intArrayOf(5000)  // Example amount stolen
//    val daysOfWeek = intArrayOf(1)  // Example day of week (Monday)
//
    // Create DataFrame for the new data
//    val newData = DataFrame.of(
//        IntVector.of("hour", hours),
//        IntVector.of("amountStolen", amountsStolen),
//        IntVector.of("dayOfWeek", daysOfWeek)
//    )
//    val timeWindowPrediction = model.predict(newData)
//
//    // Output the predicted highest risk period
//    println("Predicted High-Risk Time Window: ${getDayPeriod(timeWindowPrediction[0])}")
}

//fun timeWindowToString(timeWindow: Int): String = when(timeWindow) {
//    0 -> "Morning"
//    1 -> "Afternoon"
//    2 -> "Evening"
//    else -> "Invalid time window"
//}






