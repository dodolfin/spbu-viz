import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.File
import java.text.NumberFormat
import java.util.*

data class ParsedCSV(
    val rowsLabels: List<String>,
    val columnsLabels: List<String>,
    val values: List<List<Double>>
)

fun parseCSVBarChartData(CSVFile: File, extractRowsLabels: Boolean, extractColumnsLabels: Boolean): ParsedCSV? {
    val data = csvReader { delimiter = ';' }.readAll(CSVFile)

    if (data.isEmpty()) {
        return null
    }
    if (data.any { it.isEmpty() }) {
        return null
    }
    val columnsLabels: List<String> = if (extractRowsLabels) {
        data[0].drop(1).map { if (it == "") " " else it }
    } else {
        List(data[0].size) { index -> "Input ${index + 1}" }
    }
    val rowsLabels: List<String> = if (extractColumnsLabels) {
        data.map { it[0] }.drop(1).map { if (it == "") " " else it }
    } else {
        List(data.size) { index -> "Series ${index + 1}"}
    }

    val valuesInString = data.drop(if (extractColumnsLabels) 1 else 0).map { it.drop(if (extractRowsLabels) 1 else 0) }

    if (valuesInString.size != rowsLabels.size || valuesInString.any { it.size != columnsLabels.size }) {
        return null
    }

    val format = NumberFormat.getInstance(Locale.getDefault())
    val valuesInDouble: List<List<Double>>
    try {
        valuesInDouble = valuesInString.map { it.map { format.parse(it.trim()).toDouble() } }
    } catch (exception: Exception) {
        return null
    }

    return ParsedCSV(rowsLabels, columnsLabels, valuesInDouble)
}