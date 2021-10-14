import java.awt.Color

/**
 * Default value of indentation (in pixels)
 */
const val defaultMargin = 5.0

/**
 * Represents the size of output chart (in pixels)
 */
data class Size(val width: Int, val height: Int)

/**
 * Bars can be placed either vertically or horizontally
 */
enum class BarChartOrientation {
    VERTICAL, HORIZONTAL
}

/**
 * How are multiple (if present) columns with data displayed. If clustered, they are displayed side-by-side,
 * otherwise they stack on top of each other (useful for seeing the input from each column to the total)
 */
enum class BarChartMultipleValuesDisplay {
    CLUSTERED, STACKED
}

/**
 * Represents different style properties of chart.
 */
data class BarChartStyle(
    val size: Size = Size(800, 600),
    val orientation: BarChartOrientation = BarChartOrientation.VERTICAL,
    val multipleValuesDisplay: BarChartMultipleValuesDisplay = BarChartMultipleValuesDisplay.CLUSTERED,
    val gridColor: Color = Color.LIGHT_GRAY,
    val barColors: List<Color> = listOf(Color.CYAN, Color.GREEN, Color.YELLOW, Color.ORANGE),
    val displayLegend: Boolean = true,
    val barWidthRate: Double = 0.8
)

data class HistogramChartStyle(
    val size: Size = Size(800, 600),
    val gridColor: Color = Color.LIGHT_GRAY,
    val barColor: Color = Color.BLUE
)

data class PieChartStyle(
    val size: Size = Size(800, 600),
    val sectorsColors: List<Color> = listOf(Color.CYAN, Color.GREEN, Color.YELLOW, Color.ORANGE),
    val displayLegend: Boolean = true
)