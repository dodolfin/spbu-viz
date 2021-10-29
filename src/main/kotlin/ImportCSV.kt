import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.File
import java.text.NumberFormat
import java.util.*

/**
 * Parsed CSV is split into values themselves, rowsLabels for series of data and columnsLabels
 * for different sources of data
 */
data class ParsedCSV(
    val rowsLabels: List<String>,
    val columnsLabels: List<String>,
    val values: List<List<Double>>
)

/**
 * Returns ParsedCSV, if the parsing was successful. Returns null otherwise.
 */
fun parseCSV(CSVFile: File, extractRowsLabels: Boolean, extractColumnsLabels: Boolean): ParsedCSV? {
    val data: List<List<String>>
    try {
        data = csvReader { delimiter = ';' }.readAll(CSVFile)
    } catch (exception: Exception) {
        return null
    }

    if (data.isEmpty()) {
        return null
    }
    if (data.any { it.isEmpty() }) {
        return null
    }
    val columnsLabels: List<String> = if (extractColumnsLabels) {
        data[0].drop(if (extractRowsLabels) 1 else 0).map { if (it == "") " " else it }
    } else {
        List(data[0].size - if (extractRowsLabels) 1 else 0) { index -> "Input ${index + 1}" }
    }
    val rowsLabels: List<String> = if (extractRowsLabels) {
        data.map { it[0] }.drop(if (extractColumnsLabels) 1 else 0).map { if (it == "") " " else it }
    } else {
        List(data.size - if (extractColumnsLabels) 1 else 0) { index -> "Series ${index + 1}" }
    }

    val valuesInString = data.drop(if (extractColumnsLabels) 1 else 0).map { it.drop(if (extractRowsLabels) 1 else 0) }

    if (valuesInString.size != rowsLabels.size || valuesInString.any { it.size != columnsLabels.size }) {
        return null
    }

    val format = NumberFormat.getInstance(Locale.forLanguageTag("ru-RU"))
    val valuesInDouble: List<List<Double>>
    try {
        valuesInDouble = valuesInString.map { it.map { format.parse(it.trim()).toDouble() } }
    } catch (exception: Exception) {
        return null
    }

    return ParsedCSV(rowsLabels, columnsLabels, valuesInDouble)
}