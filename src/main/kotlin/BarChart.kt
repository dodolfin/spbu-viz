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
    var graphRectangle = Rectangle2D.Double()
    var gridRectangle = Rectangle2D.Double()
    var legendRectangle = Rectangle2D.Double()

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
            CLUSTERED -> data.values.flatten().maxOrNull()
            STACKED -> data.values.map { row -> row.sum() }.maxOrNull()
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
     * Renders Y (vertical) axis labels. Note that depending on [style].orientation, those might be [valuesLabelsLayouts],
     * as well as [rowsLabelsLayouts].
     */
    fun renderYAxisLabels(yAxisLabelsLayouts: List<TextLayout>) {
        getLinearInterpolation(
            gridRectangle.minY,
            gridRectangle.maxY,
            yAxisLabelsLayouts.size
        ).forEachIndexed { index, y ->
            val layout = yAxisLabelsLayouts[index]
            layout.draw(
                SVGCanvas,
                (gridRectangle.minX - layout.bounds.width - layout.bounds.x - defaultMargin).toFloat(),
                (y - (layout.bounds.y / 2.0)).toFloat()
            )
        }
    }

    /**
     * Renders X (horizontal) axis labels. Note that depending on [style].orientation, those might be [valuesLabelsLayouts],
     * as well as [rowsLabelsLayouts].
     */
    fun renderXAxisLabels(xAxisLabelsLayouts: List<TextLayout>) {
        getLinearInterpolationMiddles(
            gridRectangle.minX,
            gridRectangle.maxX,
            xAxisLabelsLayouts.size
        ).forEachIndexed { index, x ->
            val layout = xAxisLabelsLayouts[index]
            layout.draw(
                SVGCanvas, (x - (layout.bounds.width / 2.0) - layout.bounds.x).toFloat(),
                (gridRectangle.maxY - layout.bounds.y + defaultMargin).toFloat()
            )
        }
    }

    /**
     * Calculates bars rectangles and assigns color to bars corresponding to different columns. See [barsRectangles]
     * for more information
     */
    fun setBarsRectanglesAndColors() {
        val isVertical = style.orientation == VERTICAL
        val minCoordinate = if (isVertical) gridRectangle.minX else gridRectangle.minY
        val maxCoordinate = if (isVertical) gridRectangle.maxX else gridRectangle.maxY

        val oneBarDimension = getLinearInterpolationDelta(minCoordinate, maxCoordinate, data.values.size + 1)

        getLinearInterpolation(minCoordinate, maxCoordinate, data.values.size + 1).dropLast(1).forEach { coordinate ->
            val startCoordinate = coordinate + ((1 - style.barWidthRate) / 2.0) * oneBarDimension
            val endCoordinate = coordinate + (0.5 + style.barWidthRate / 2.0) * oneBarDimension

            barsRectangles.add(
                if (isVertical) {
                    Rectangle2D.Double(startCoordinate, gridRectangle.minY, endCoordinate - startCoordinate, gridRectangle.height)
                } else {
                    Rectangle2D.Double(gridRectangle.minX, startCoordinate, gridRectangle.width, endCoordinate - startCoordinate)
                }
            )
        }

        repeat(data.values[0].size) {
            columnsColors.add(style.barColors[nextColor()])
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

                            renderShapeWithOutline(barRectangle, columnsColors[rowIndex], SVGCanvas)
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

                        renderShapeWithOutline(barRectangle, columnsColors[rowIndex], SVGCanvas)

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

                            renderShapeWithOutline(barRectangle, columnsColors[rowIndex], SVGCanvas)
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

                        renderShapeWithOutline(barRectangle, columnsColors[rowIndex], SVGCanvas)

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
        legendRectangle = setLegendRectangle(style.size, columnsLabelsLayouts, style.displayLegend)
        graphRectangle = getGraphRectangle(titleRectangle, style.size, legendRectangle)
        gridRectangle = getGridRectangle(
            graphRectangle,
            if (style.orientation == VERTICAL) rowsLabelsLayouts else valuesLabelsLayouts,
            if (style.orientation == VERTICAL) valuesLabelsLayouts else rowsLabelsLayouts,
        )

        renderYAxisLabels(if (style.orientation == VERTICAL) valuesLabelsLayouts else rowsLabelsLayouts)
        renderXAxisLabels(if (style.orientation == VERTICAL) rowsLabelsLayouts else valuesLabelsLayouts)
        renderGrid(gridRectangle, valuesAxisLabels.size, style.orientation, SVGCanvas)

        setBarsRectanglesAndColors()

        when (style.orientation) {
            VERTICAL -> renderVerticalBars()
            HORIZONTAL -> renderHorizontalBars()
        }

        renderLegend(style.displayLegend, columnsLabelsLayouts, columnsColors, legendRectangle, SVGCanvas)
    }
}
