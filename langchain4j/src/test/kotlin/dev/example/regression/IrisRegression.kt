package dev.example.regression
import smile.classification.RandomForest
import smile.data.DataFrame
import smile.data.formula.Formula
import smile.io.Read

fun main() {
    val iris = Read.arff("/Users/urs/development/github/ai/kotlin-ai-talk/langchain4j/src/main/resources/dataset/iris.arff")
    println(iris)
    iris.summary()
    val rf = RandomForest.fit(Formula.lhs("class"), iris)
    println("OOB error = ${rf.metrics()}")
    // Split the dataset (e.g., 80% train, 20% test)
    val (trainData, testData) = trainTestSplit(iris, 0.8)
    val predictions: MutableList<Pair<Int, Any>> = testData.stream().map { row ->
        val tuple = smile.data.Tuple.of(row.toArray(), iris.schema())
        rf.predict(tuple) to tuple.get("class")
    }.toList()
    predictions.fold(0 to 0){(ok, nok), (pred, real) ->  if(pred.toDouble() == real)ok + 1 to nok else ok to nok + 1}.let { (ok, nok) -> println("Accuracy: ${ok.toDouble() / (ok + nok)}") }
}


// Function to split the dataset into training and test sets
fun trainTestSplit(data: DataFrame, trainSize: Double): Pair<DataFrame, DataFrame> {
    val trainCount = (data.nrow() * trainSize).toInt()
    println("Train count: $trainCount, ${data.nrow()}")
    val trainData = data.slice(0, trainCount)
    //println("Train data: ${trainData.size}")
    val testData = data.slice(trainCount - 1, data.nrow() - 1)
    return trainData to testData
}

