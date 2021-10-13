import java.awt.Color
import java.awt.*
import BarChartOrientation.*
import BarChartMultipleValuesDisplay.*
import java.awt.font.TextLayout
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import kotlin.math.*

/**
 * Default value of indentation (in pixels)
 */
const val defaultMargin = 5.0

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
 * Represents the size of output chart (in pixels)
 */
data class Size(val width: Int, val height: Int)

/**
 * Represents different style properties of chart.
 */
data class BarChartStyle(
    val size: Size = Size(800, 600),
    val orientation: BarChartOrientation = VERTICAL,
    val multipleValuesDisplay: BarChartMultipleValuesDisplay = CLUSTERED,
    val gridColor: Color = Color.LIGHT_GRAY,
    val barColors: List<Color> = listOf(Color.CYAN, Color.GREEN, Color.YELLOW, Color.ORANGE),
    val displayLegend: Boolean = true
)

/**
 * The data for rendering chart itself. [values] stores the 2D table with data (as in Excel). Rows are treated as
 * separate bars, and columns are treated as bars with different colors.
 */
data class BarChartData(
    val chartTitle: String,
    val columnsTitles: List<String>,
    val rowsTitles: List<String>,
    val values: List<List<Double>>
)

/**
 * Return delta between two consecutive numbers from the output of [getLinearInterpolation] function.
 */
fun getLinearInterpolationDelta(start: Double, end: Double, steps: Int): Double = (end - start) / (steps - 1)

/**
 * Return [steps] values that match this pattern: start, start + delta, start + 2 * delta, ..., finish
 */
fun getLinearInterpolation(start: Double, end: Double, steps: Int): List<Double> {
    val delta = getLinearInterpolationDelta(start, end, steps)
    val res = mutableListOf<Double>()

    for (i in 0 until steps) {
        res.add(start + i.toDouble() * delta)
    }

    return res
}

/**
 * Return delta between two consecutive numbers from the output of [getLinearInterpolationMiddles] function.
 */
fun getLinearInterpolationMiddlesDelta(start: Double, end: Double, steps: Int): Double = (end - start) / (steps)

/**
 * Return [steps] values that match this pattern: start + 0.5 * delta, start + 1.5 * delta, ..., finish - 0.5 * delta
 */
fun getLinearInterpolationMiddles(start: Double, end: Double, steps: Int): List<Double> {
    val delta = getLinearInterpolationMiddlesDelta(start, end, steps)
    val res = mutableListOf<Double>()

    for (i in 0 until steps) {
        res.add(start + (i.toDouble() + 0.5) * delta)
    }

    return res
}

/**
 * Represents all data needed for rendering BarChart.

 * [columnsColors] stores colors for each column of data
 * [valuesAxisLabels] stores
 */
data class BarChart(val data: BarChartData, val style: BarChartStyle, val SVGCanvas: SVGCanvas) {
    /**
     * [titleRectangle], [graphRectangle], [gridRectangle] and [legendRectangle] represent rectangles in which corresponding
     * parts of the chart are rendered (with [defaultMargin] indentation)
     */
    val titleRectangle = Rectangle2D.Double()
    val graphRectangle = Rectangle2D.Double()
    val gridRectangle = Rectangle2D.Double()
    val legendRectangle = Rectangle2D.Double()

    /**
     * Despite its name, [barsRectangles] only represents parts of the grid in which bars may be present and not the bars themselves.
     * Actual bars may be smaller than [barsRectangles].
     */
    val barsRectangles = mutableListOf<Rectangle2D.Double>()

    /**
     * Stores the color of each column.
     */
    val columnsColors = mutableListOf<Color>()

    /**
     * [valuesAxisLabels] stores labels upon which grid is drawn. Each label can be represented by formula k * 10 ^ p,
     * where k is in {0, 1, 2, 3, 4, 5, 6, 7, 8, 9} and
     * p is minimal p (such as 10 ^ p is less than maximum value in data) minus 1.
     */
    val valuesAxisLabels = mutableListOf<Double>()

    /**
     * Maximum value of [valuesAxisLabels]
     */
    var gridMaxValue = 0.0

    /**
     * [valuesLabelsLayouts], [columnsLabelsLayouts] and [rowsLabelsLayouts] stores TextLayout objects for virtually
     * all text on the chart. We need to store TextLayout objects to properly calculate different sizes & indentations.
     */
    val valuesLabelsLayouts = mutableListOf<TextLayout>()
    val columnsLabelsLayouts = mutableListOf<TextLayout>()
    val rowsLabelsLayouts = mutableListOf<TextLayout>()

    /**
     * [titleFont] and [labelFont] store fonts for title and all other text on the chart respectively.
     */
    val titleFont = Font("Arial", Font.PLAIN, 36)
    val labelFont = Font("Arial", Font.PLAIN, 14)

    /**
     * Internal variable used for assigning colors to columns.
     */
    var colorIndex = -1

    /**
     * Internal function, which returns the pointer of the next color. Used for assigning colors to columns.
     */
    fun nextColor(): Int {
        colorIndex = (colorIndex + 1) % style.barColors.size
        return colorIndex
    }

    /**
     * Renders title and calculates title rectangle.
     */
    fun renderTitleSetTitleRectangle() {
        val titleLayout = TextLayout(data.chartTitle, titleFont, SVGCanvas.fontRenderContext)

        titleRectangle.apply {
            x = defaultMargin
            y = defaultMargin
            width = style.size.width.toDouble() - 2 * defaultMargin
            height = if (data.chartTitle.isEmpty()) 0.0 else 2 * defaultMargin + titleLayout.bounds.height
        }

        if (data.chartTitle.isNotEmpty()) {
            titleLayout.draw(
                SVGCanvas, (titleRectangle.centerX - (titleLayout.bounds.width / 2.0)).toFloat(),
                (titleRectangle.centerY + (titleLayout.ascent / 2.0)).toFloat()
            )
        }
    }

    /**
     * Makes labels upon which grid is drawn. See [valuesAxisLabels] documentation for more information.
     */
    fun generateValuesLabels() {
        val maxValue = when (style.multipleValuesDisplay) {
            CLUSTERED -> {
                data.values.flatten().maxOrNull()
            }
            STACKED -> {
                data.values.map { row -> row.sum() }.maxOrNull()
            }
        }
        checkNotNull(maxValue)

        val step = (10.0).pow(floor(log10(maxValue)) - if (style.multipleValuesDisplay == STACKED) 1.0 else 0.0)
        for (i in 0..ceil(maxValue / step).toInt()) {
            valuesAxisLabels.add(i.toDouble() * step)
        }

        this.gridMaxValue = valuesAxisLabels.last()

        if (style.orientation == VERTICAL) {
            valuesAxisLabels.reverse()
        }
    }

    /**
     * Generates all text layouts except chart title.
     */
    fun generateAllLabelsLayouts() {
        data.columnsTitles.forEach {
            columnsLabelsLayouts.add(TextLayout(it, labelFont, SVGCanvas.fontRenderContext))
        }
        data.rowsTitles.forEach {
            rowsLabelsLayouts.add(TextLayout(it, labelFont, SVGCanvas.fontRenderContext))
        }
        valuesAxisLabels.forEach {
            valuesLabelsLayouts.add(TextLayout(it.toString(), labelFont, SVGCanvas.fontRenderContext))
        }
    }

    /**
     * Calculates legend rectangle.
     */
    fun setLegendRectangle() {
        legendRectangle.apply {
            x = 5.0
            y = style.size.height.toDouble() - 5.0 - columnsLabelsLayouts.maxOf { it.bounds.height }
            width = style.size.width.toDouble() - 10.0
            height = if (style.displayLegend) columnsLabelsLayouts.maxOf { it.bounds.height } else 0.0
        }
    }

    /**
     * Calculates graph and grid rectangles. Graph rectangle includes grid, and both axes labels.
     */
    fun setGraphAndGridRectangle() {
        graphRectangle.apply {
            x = 5.0
            y = titleRectangle.maxY
            width = style.size.width.toDouble() - 10.0
            height = legendRectangle.minY - titleRectangle.maxY
        }

        gridRectangle.apply {
            when (style.orientation) {
                VERTICAL -> {
                    x = graphRectangle.minX + 5.0 + valuesLabelsLayouts.maxOf { it.bounds.width }
                    y = graphRectangle.minY + 5.0
                    width = graphRectangle.width - 10.0 - valuesLabelsLayouts.maxOf { it.bounds.width }
                    height = graphRectangle.height - 10.0 - rowsLabelsLayouts.maxOf { it.bounds.height }
                }
                HORIZONTAL -> {
                    x = graphRectangle.minX + 5.0 + rowsLabelsLayouts.maxOf { it.bounds.width }
                    y = graphRectangle.minY + 5.0
                    width = graphRectangle.width - 10.0 - rowsLabelsLayouts.maxOf { it.bounds.width }
                    height = graphRectangle.height - 10.0 - valuesLabelsLayouts.maxOf { it.bounds.height }
                }
            }
        }
    }

    /**
     * Renders Y (vertical) axis labels. Note that depending on [style].orientation, those might be [valuesLabelsLayouts],
     * as well as [rowsLabelsLayouts].
     */
    fun renderYAxisLabels() {
        when (style.orientation) {
            VERTICAL -> {
                getLinearInterpolation(gridRectangle.minY, gridRectangle.maxY, valuesAxisLabels.size).forEachIndexed { index, y ->
                    val layout = valuesLabelsLayouts[index]
                    layout.draw(SVGCanvas, (gridRectangle.minX - layout.bounds.width - defaultMargin).toFloat(),
                        (y + (layout.ascent / 2.0)).toFloat()
                    )
                }
            }
            HORIZONTAL -> {
                getLinearInterpolationMiddles(gridRectangle.minY, gridRectangle.maxY, rowsLabelsLayouts.size).forEachIndexed { index, y ->
                    val layout = rowsLabelsLayouts[index]
                    layout.draw(SVGCanvas, (gridRectangle.minX - layout.bounds.width - defaultMargin).toFloat(),
                        (y + (layout.ascent / 2.0)).toFloat()
                    )
                }
            }
        }
    }

    /**
     * Renders X (horizontal) axis labels. Note that depending on [style].orientation, those might be [valuesLabelsLayouts],
     * as well as [rowsLabelsLayouts].
     */
    fun renderXAxisLabels() {
        when (style.orientation) {
            VERTICAL -> {
                getLinearInterpolationMiddles(gridRectangle.minX, gridRectangle.maxX, rowsLabelsLayouts.size).forEachIndexed { index, x ->
                    val layout = rowsLabelsLayouts[index]
                    layout.draw(SVGCanvas, (x - (layout.bounds.width / 2.0)).toFloat(),
                        (gridRectangle.maxY + layout.ascent).toFloat()
                    )
                }
            }
            HORIZONTAL -> {
                getLinearInterpolation(gridRectangle.minX, gridRectangle.maxX, valuesLabelsLayouts.size).forEachIndexed { index, x ->
                    val layout = valuesLabelsLayouts[index]
                    layout.draw(SVGCanvas, (x - (layout.bounds.width / 2.0)).toFloat(),
                        (gridRectangle.maxY + layout.ascent).toFloat()
                    )
                }
            }
        }
    }

    /**
     * Renders grid (those light gray (by default) lines that help you estimate the value that bar represents)
     */
    fun renderGrid() {
        SVGCanvas.paint = style.gridColor
        when (style.orientation) {
            VERTICAL -> {
                getLinearInterpolation(gridRectangle.minY, gridRectangle.maxY, valuesAxisLabels.size).forEach { y ->
                    SVGCanvas.draw(Line2D.Double(gridRectangle.minX, y, gridRectangle.maxX, y))
                }
            }
            HORIZONTAL -> {
                getLinearInterpolation(gridRectangle.minX, gridRectangle.maxX, valuesAxisLabels.size).forEach { x ->
                    SVGCanvas.draw(Line2D.Double(x, gridRectangle.minY, x, gridRectangle.maxY))
                }
            }
        }
    }

    /**
     * Calculates bars rectangles and assigns color to bars corresponding to different columns. See [barsRectangles]
     * for more information
     */
    fun setBarsRectanglesAndColors() {
        when (style.orientation) {
            VERTICAL -> {
                val width = getLinearInterpolationDelta(gridRectangle.minX, gridRectangle.maxX, data.values.size + 1)
                getLinearInterpolation(gridRectangle.minX, gridRectangle.maxX, data.values.size + 1).dropLast(1).forEach { x ->
                    val startX = x + 0.1 * width
                    val endX = x + 0.9 * width
                    barsRectangles.add(Rectangle2D.Double(startX, gridRectangle.minY, endX - startX, gridRectangle.height))
                    columnsColors.add(style.barColors[nextColor()])
                }
            }
            HORIZONTAL -> {
                val height = getLinearInterpolationDelta(gridRectangle.minY, gridRectangle.maxY, data.values.size + 1)
                getLinearInterpolation(gridRectangle.minY, gridRectangle.maxY, data.values.size + 1).dropLast(1).forEach { y ->
                    val startY = y + 0.1 * height
                    val endY = y + 0.9 * height
                    barsRectangles.add(Rectangle2D.Double(gridRectangle.minX, startY, gridRectangle.width, endY - startY))
                    columnsColors.add(style.barColors[nextColor()])
                }
            }
        }
    }

    /**
     * Renders bars if [style].orientation is vertical.
     */
    fun renderVerticalBars() {
        when (style.multipleValuesDisplay) {
            CLUSTERED -> {
                barsRectangles.forEachIndexed { index, rect ->
                    val row = data.values[index]
                    val oneBarWidth = getLinearInterpolationDelta(rect.minX, rect.maxX, row.size + 1)
                    getLinearInterpolation(rect.minX, rect.maxX, row.size + 1).dropLast(1).forEachIndexed { rowIndex, x ->
                        val barHeight = rect.height * (row[rowIndex] / gridMaxValue)
                        val barRectangle = Rectangle2D.Double(x, gridRectangle.maxY - barHeight, oneBarWidth, barHeight)

                        SVGCanvas.color = columnsColors[rowIndex]
                        SVGCanvas.fill(barRectangle)
                        SVGCanvas.color = Color.BLACK
                        SVGCanvas.draw(barRectangle)
                    }
                }
            }
            STACKED -> {
                barsRectangles.forEachIndexed { index, rect ->
                    val row = data.values[index]
                    var currentMaxY = rect.maxY

                    row.forEachIndexed { rowIndex, cell ->
                        val barHeight = rect.height * (cell / gridMaxValue)
                        val barRectangle = Rectangle2D.Double(rect.x, currentMaxY - barHeight, rect.width, barHeight)

                        SVGCanvas.color = columnsColors[rowIndex]
                        SVGCanvas.fill(barRectangle)
                        SVGCanvas.color = Color.BLACK
                        SVGCanvas.draw(barRectangle)

                        currentMaxY -= barHeight
                    }
                }
            }
        }
    }

    /**
     * Renders bars if [style].orientation is horizontal.
     */
    fun renderHorizontalBars() {
        when (style.multipleValuesDisplay) {
            CLUSTERED -> {
                barsRectangles.forEachIndexed { index, rect ->
                    val row = data.values[index]
                    val oneBarHeight = getLinearInterpolationDelta(rect.minY, rect.maxY, row.size + 1)
                    getLinearInterpolation(rect.minY, rect.maxY, row.size + 1).dropLast(1).forEachIndexed { rowIndex, y ->
                        val barWidth = rect.width * (row[rowIndex] / gridMaxValue)
                        val barRectangle = Rectangle2D.Double(gridRectangle.minX, y, barWidth, oneBarHeight)

                        SVGCanvas.color = columnsColors[rowIndex]
                        SVGCanvas.fill(barRectangle)
                        SVGCanvas.color = Color.BLACK
                        SVGCanvas.draw(barRectangle)
                    }
                }
            }
            STACKED -> {
                barsRectangles.forEachIndexed { index, rect ->
                    val row = data.values[index]
                    var currentMinX = rect.minX

                    row.forEachIndexed { rowIndex, cell ->
                        val barWidth = rect.width * (cell / gridMaxValue)
                        val barRectangle = Rectangle2D.Double(currentMinX, rect.y, barWidth, rect.height)

                        SVGCanvas.color = columnsColors[rowIndex]
                        SVGCanvas.fill(barRectangle)
                        SVGCanvas.color = Color.BLACK
                        SVGCanvas.draw(barRectangle)

                        currentMinX += barWidth
                    }
                }
            }
        }
    }

    /**
     * Renders legend if [style].displayLegend is true.
     */
    fun renderLegend() {
        if (!style.displayLegend) {
            return
        }

        val n = columnsLabelsLayouts.size.toDouble()
        val legendHeight = columnsLabelsLayouts.maxOf { it.bounds.height }
        val legendWidth = n * legendHeight + columnsLabelsLayouts.sumOf { it.bounds.width } + (2 * (n - 1) + n) * defaultMargin
        var currentX = legendRectangle.centerX - (legendWidth / 2.0)
        columnsLabelsLayouts.forEachIndexed { index, layout ->
            val colorSquare = Rectangle2D.Double(currentX, legendRectangle.minY + defaultMargin, legendHeight, legendHeight)

            SVGCanvas.color = columnsColors[index]
            SVGCanvas.fill(colorSquare)
            SVGCanvas.color = Color.BLACK
            SVGCanvas.draw(colorSquare)

            currentX += legendHeight + defaultMargin

            layout.draw(SVGCanvas, currentX.toFloat(), (legendRectangle.minY + defaultMargin + layout.ascent).toFloat())

            currentX += layout.bounds.width + 2 * defaultMargin
        }
    }

    /**
     * Renders whole chart.
     */
    fun render() {
        renderTitleSetTitleRectangle()

        generateValuesLabels()
        generateAllLabelsLayouts()
        setLegendRectangle()
        setGraphAndGridRectangle()

        renderYAxisLabels()
        renderXAxisLabels()
        renderGrid()

        setBarsRectanglesAndColors()

        when (style.orientation) {
            VERTICAL -> renderVerticalBars()
            HORIZONTAL -> renderHorizontalBars()
        }

        renderLegend()
    }
}
