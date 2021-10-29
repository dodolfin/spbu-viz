import BarChartMultipleValuesDisplay.CLUSTERED
import BarChartMultipleValuesDisplay.STACKED
import Orientation.HORIZONTAL
import Orientation.VERTICAL
import java.awt.Color
import java.awt.font.TextLayout
import java.awt.geom.Rectangle2D

/**
 * The data for rendering chart itself. [values] stores the 2D table with data (as in Excel). Only the first row is taken
 * as data, others are ignored.
 */
data class BarChartData(
    val chartTitle: String,
    val columnsTitles: List<String>,
    val rowsTitles: List<String>,
    val values: List<List<Double>>
)

/**
 * Represents all data needed for rendering BarChart.
 */
data class BarChart(val data: BarChartData, val style: BarChartStyle, val SVGCanvas: SVGCanvas) {
    /**
     * [titleRectangle], [graphRectangle], [gridRectangle] and [legendRectangle] represent rectangles in which corresponding
     * parts of the chart are rendered (with [defaultMargin] indentation)
     */
    val titleRectangle = getTitleRectangle(data.chartTitle, style.size.width, SVGCanvas)
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

        valuesAxisLabels.addAll(getTenPowers(maxValue, if (style.multipleValuesDisplay == STACKED) -1.0 else 0.0))

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
            x = defaultMargin
            y = style.size.height.toDouble() - defaultMargin - columnsLabelsLayouts.maxOf { it.bounds.height }
            width = style.size.width.toDouble() - 2 * defaultMargin
            height =
                if (style.displayLegend) columnsLabelsLayouts.maxOf { it.bounds.height } else 0.0 + 2 * defaultMargin
        }
    }

    /**
     * Calculates graph and grid rectangles. Graph rectangle includes grid, and both axes labels.
     */
    fun setGraphAndGridRectangle() {
        graphRectangle.apply {
            x = defaultMargin
            y = titleRectangle.maxY
            width = style.size.width.toDouble() - 2 * defaultMargin
            height = legendRectangle.minY - titleRectangle.maxY
        }

        gridRectangle.apply {
            when (style.orientation) {
                VERTICAL -> {
                    x = graphRectangle.minX + defaultMargin + valuesLabelsLayouts.maxOf { it.bounds.width }
                    y = graphRectangle.minY + defaultMargin
                    width = graphRectangle.width - 2 * defaultMargin - valuesLabelsLayouts.maxOf { it.bounds.width }
                    height = graphRectangle.height - 2 * defaultMargin - rowsLabelsLayouts.maxOf { it.bounds.height }
                }
                HORIZONTAL -> {
                    x = graphRectangle.minX + defaultMargin + rowsLabelsLayouts.maxOf { it.bounds.width }
                    y = graphRectangle.minY + defaultMargin
                    width = graphRectangle.width - 2 * defaultMargin - rowsLabelsLayouts.maxOf { it.bounds.width }
                    height = graphRectangle.height - 2 * defaultMargin - valuesLabelsLayouts.maxOf { it.bounds.height }
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
                getLinearInterpolation(
                    gridRectangle.minY,
                    gridRectangle.maxY,
                    valuesAxisLabels.size
                ).forEachIndexed { index, y ->
                    val layout = valuesLabelsLayouts[index]
                    layout.draw(
                        SVGCanvas,
                        (gridRectangle.minX - layout.bounds.width - layout.bounds.x - defaultMargin).toFloat(),
                        (y - (layout.bounds.y / 2.0)).toFloat()
                    )
                }
            }
            HORIZONTAL -> {
                getLinearInterpolationMiddles(
                    gridRectangle.minY,
                    gridRectangle.maxY,
                    rowsLabelsLayouts.size
                ).forEachIndexed { index, y ->
                    val layout = rowsLabelsLayouts[index]
                    layout.draw(
                        SVGCanvas,
                        (gridRectangle.minX - layout.bounds.width - layout.bounds.x - defaultMargin).toFloat(),
                        (y - (layout.bounds.y / 2.0)).toFloat()
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
                getLinearInterpolationMiddles(
                    gridRectangle.minX,
                    gridRectangle.maxX,
                    rowsLabelsLayouts.size
                ).forEachIndexed { index, x ->
                    val layout = rowsLabelsLayouts[index]
                    layout.draw(
                        SVGCanvas, (x - (layout.bounds.width / 2.0) - layout.bounds.x).toFloat(),
                        (gridRectangle.maxY - layout.bounds.y + defaultMargin).toFloat()
                    )
                }
            }
            HORIZONTAL -> {
                getLinearInterpolation(
                    gridRectangle.minX,
                    gridRectangle.maxX,
                    valuesLabelsLayouts.size
                ).forEachIndexed { index, x ->
                    val layout = valuesLabelsLayouts[index]
                    layout.draw(
                        SVGCanvas, (x - (layout.bounds.width / 2.0) - layout.bounds.x).toFloat(),
                        (gridRectangle.maxY - layout.bounds.y + defaultMargin).toFloat()
                    )
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
                getLinearInterpolation(gridRectangle.minX, gridRectangle.maxX, data.values.size + 1).dropLast(1)
                    .forEach { x ->
                        val startX = x + ((1 - style.barWidthRate) / 2.0) * width
                        val endX = x + (0.5 + style.barWidthRate / 2.0) * width
                        barsRectangles.add(
                            Rectangle2D.Double(
                                startX,
                                gridRectangle.minY,
                                endX - startX,
                                gridRectangle.height
                            )
                        )
                    }
                repeat(data.values[0].size) {
                    columnsColors.add(style.barColors[nextColor()])
                }
            }
            HORIZONTAL -> {
                val height = getLinearInterpolationDelta(gridRectangle.minY, gridRectangle.maxY, data.values.size + 1)
                getLinearInterpolation(gridRectangle.minY, gridRectangle.maxY, data.values.size + 1).dropLast(1)
                    .forEach { y ->
                        val startY = y + ((1 - style.barWidthRate) / 2.0) * height
                        val endY = y + (0.5 + style.barWidthRate / 2.0) * height
                        barsRectangles.add(
                            Rectangle2D.Double(
                                gridRectangle.minX,
                                startY,
                                gridRectangle.width,
                                endY - startY
                            )
                        )
                    }
                repeat(data.values[0].size) {
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
                    getLinearInterpolation(rect.minX, rect.maxX, row.size + 1).dropLast(1)
                        .forEachIndexed { rowIndex, x ->
                            val barHeight = rect.height * (row[rowIndex] / gridMaxValue)
                            val barRectangle =
                                Rectangle2D.Double(x, gridRectangle.maxY - barHeight, oneBarWidth, barHeight)

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
                    getLinearInterpolation(rect.minY, rect.maxY, row.size + 1).dropLast(1)
                        .forEachIndexed { rowIndex, y ->
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
     * Renders whole chart.
     */
    fun render() {
        renderTitle(data.chartTitle, titleRectangle, SVGCanvas)

        generateValuesLabels()
        generateAllLabelsLayouts()
        setLegendRectangle()
        setGraphAndGridRectangle()

        renderYAxisLabels()
        renderXAxisLabels()
        renderGrid(gridRectangle, valuesAxisLabels.size, style.orientation, SVGCanvas)

        setBarsRectanglesAndColors()

        when (style.orientation) {
            VERTICAL -> renderVerticalBars()
            HORIZONTAL -> renderHorizontalBars()
        }

        renderLegend(style.displayLegend, columnsLabelsLayouts, columnsColors, legendRectangle, SVGCanvas)
    }
}
