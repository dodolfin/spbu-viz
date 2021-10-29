import java.io.File
import kotlin.test.*

internal class ImportCSVTests {
    @Test
    fun parseCSVTestNormalFile() {
        val CSVFile = File("test_csvs/unit_tests/simple_rc.csv")
        val parsedCSV = parseCSV(CSVFile, true, true)
        val ans = ParsedCSV(
            listOf("row1", "row2"),
            listOf("column1", "column2"),
            listOf(listOf(1.0, 2.0), listOf(4.0, 5.0))
        )

        assertEquals(ans, parsedCSV)
    }

    @Test
    fun parseCSVTestNormalFileWithoutRowsLabels() {
        val CSVFile = File("test_csvs/unit_tests/simple_c.csv")
        val parsedCSV = parseCSV(CSVFile, false, true)
        val ans = ParsedCSV(
            listOf("Series 1", "Series 2"),
            listOf("column1", "column2"),
            listOf(listOf(1.0, 2.0), listOf(4.0, 5.0))
        )

        assertEquals(ans, parsedCSV)
    }

    @Test
    fun parseCSVTestNormalFileWithoutColumnsLabels() {
        val CSVFile = File("test_csvs/unit_tests/simple_r.csv")
        val parsedCSV = parseCSV(CSVFile, true, false)
        val ans = ParsedCSV(
            listOf("row1", "row2"),
            listOf("Input 1", "Input 2"),
            listOf(listOf(1.0, 2.0), listOf(4.0, 5.0))
        )

        assertEquals(ans, parsedCSV)
    }

    @Test
    fun parseCSVTestNormalFileWithoutLabels() {
        val CSVFile = File("test_csvs/unit_tests/simple.csv")
        val parsedCSV = parseCSV(CSVFile, false, false)
        val ans = ParsedCSV(
            listOf("Series 1", "Series 2"),
            listOf("Input 1", "Input 2"),
            listOf(listOf(1.0, 2.0), listOf(4.0, 5.0))
        )

        assertEquals(ans, parsedCSV)
    }

    @Test
    fun parseCSVTestNonCSVFile() {
        val CSVFile = File("src/main/kotlin/main.kt")
        assertNull(parseCSV(CSVFile, true, true))
    }

    @Test
    fun parseCSVTestCellsAreMissing() {
        val CSVFile = File("test_csvs/unit_tests/missing_cells.csv")
        assertNull(parseCSV(CSVFile, true, true))
    }

    @Test
    fun parseCSVTestNonDoubleCells() {
        val CSVFile = File("test_csvs/unit_tests/cells_with_strings.csv")
        assertNull(parseCSV(CSVFile, true, true))
    }

    @Test
    fun parseCSVTestEmptyFile() {
        val CSVFile = File("test_csvs/unit_tests/empty.csv")
        assertNull(parseCSV(CSVFile, true, true))

    }
}