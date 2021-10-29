import java.awt.Color
import java.awt.Dimension
import java.awt.Font

/**
 * Default value of indentation (in pixels)
 */
const val defaultMargin = 5.0

/**
 * Default stroke color.
 */
val strokeColor = Color.BLACK

/**
 * Default grid color
 */
val gridColor: Color = Color.LIGHT_GRAY

/**
 * [titleFont] and [labelFont] store fonts for title and all other text on the chart respectively.
 */
val titleFont = Font("Arial", Font.PLAIN, 36)
val labelFont = Font("Arial", Font.PLAIN, 14)

/**
 * Bars can be placed either vertically or horizontally
 */
enum class Orientation {
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
 * Represents different style properties of bar chart.
 */
data class BarChartStyle(
    val size: Dimension = Dimension(800, 600),
    val orientation: Orientation = Orientation.VERTICAL,
    val multipleValuesDisplay: BarChartMultipleValuesDisplay = BarChartMultipleValuesDisplay.CLUSTERED,
    val barColors: List<Color> = listOf(
        Color.MAGENTA,
        Color.RED,
        Color.PINK,
        Color.ORANGE,
        Color.YELLOW,
        Color.GREEN,
        Color.CYAN,
        Color.BLUE,
        Color.DARK_GRAY
    ),
    val displayLegend: Boolean = true,
    val barWidthRate: Double = 0.8
)

/**
 * Represents different style properties of histogram chart.
 */
data class HistogramChartStyle(
    val size: Dimension = Dimension(800, 600),
    val barColor: Color = Color.BLUE
)

/**
 * Represents different style properties of pie chart.
 */
data class PieChartStyle(
    val size: Dimension = Dimension(800, 600),
    val sectorsColors: List<Color> = listOf(
        Color.MAGENTA,
        Color.RED,
        Color.PINK,
        Color.ORANGE,
        Color.YELLOW,
        Color.GREEN,
        Color.CYAN,
        Color.BLUE,
        Color.DARK_GRAY
    ),
    val displayLegend: Boolean = true
)

/**
 * Represents different style properties of scatter chart.
 */
data class ScatterChartStyle(
    val size: Dimension = Dimension(800, 600),
    val pointsColor: Color = Color.GREEN,
    val pointRadius: Double = 4.0
)