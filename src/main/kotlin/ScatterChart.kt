import java.awt.font.TextLayout
import java.awt.geom.Ellipse2D
import java.awt.geom.Rectangle2D

/**
 * The data for rendering chart itself. [values] stores the 2D table with data (as in Excel). Only the first two columns
 * are taken from [values]; others are ignored. First column is treated as x-coordinate of a point,
 * and second as y-coordinate.
 */
data class ScatterChartData(
    val chartTitle: String,
    val values: List<List<Double>>
)

/**
 * Represents all data needed for rendering scatter chart.
 */
data class ScatterChart(val data: ScatterChartData, val style: ScatterChartStyle, val SVGCanvas: SVGCanvas) {
    /**
     * [titleRectangle], [graphRectangle], [gridRectangle] represent rectangles in which corresponding
     * parts of the chart are rendered (with [defaultMargin] indentation)
     */
    val titleRectangle = getTitleRectangle(data.chartTitle, style.size.width, SVGCanvas)
    var graphRectangle = Rectangle2D.Double()
    var gridRectangle = Rectangle2D.Double()

    /**
     * [xAxisLabels] and [yAxisLabels] stores labels upon which grid is drawn.
     */
    val xAxisLabels = mutableListOf<Double>()
    val yAxisLabels = mutableListOf<Double>()

    /**
     * To build xAxisLabels and yAxisLabels, we reuse function [getTenPowers], which was firstly written for BarChart.
     * It only supports non-negative values, so we have to perform some calculations to put our data set to that form.
     * [xDelta] and [yDelta], applied to x and y coordinates respectively, makes every point non-negative.
     * [xMaxValue] and [yMaxValue] is maximum values of x and y in relative coordinates, described earlier.
     */
    var xDelta = 0.0
    var xMaxValue = 0.0
    var yDelta = 0.0
    var yMaxValue = 0.0

    /**
     * [xAxisLabelsLayouts] and [yAxisLabelsLayouts] stores TextLayout objects for virtually
     * all text on the chart. We need to store TextLayout objects to properly calculate different sizes & indentations.
     */
    val xAxisLabelsLayouts = mutableListOf<TextLayout>()
    val yAxisLabelsLayouts = mutableListOf<TextLayout>()

    /**
     * Returns minimal number such that all elements of the list become (or stay) non-negative after adding it to each
     * element of the list.
     */
    fun getDelta(a: List<Double>) = -a.minOf { it }

    /**
     * Makes labels upon which grid is drawn.
     */
    fun generateValuesLabels() {
        val xs = data.values.map { it[0] }
        val ys = data.values.map { it[1] }

        xDelta = getDelta(xs)
        yDelta = getDelta(ys)

        xMaxValue = xs.maxOf { it } + xDelta
        yMaxValue = ys.maxOf { it } + yDelta

        xAxisLabels.addAll(getTenPowers(xMaxValue, 0.0).map { it - xDelta })
        yAxisLabels.addAll(getTenPowers(yMaxValue, 0.0).map { it - yDelta })
        yAxisLabels.reverse()
    }

    /**
     * Generates all text layouts except chart title.
     */
    fun generateAllLabelsLayouts() {
        xAxisLabels.forEach {
            xAxisLabelsLayouts.add(TextLayout(it.toString(), labelFont, SVGCanvas.fontRenderContext))
        }
        yAxisLabels.forEach {
            yAxisLabelsLayouts.add(TextLayout(it.toString(), labelFont, SVGCanvas.fontRenderContext))
        }
    }

    /**
     * Renders Y (vertical) axis labels.
     */
    fun renderYAxisLabels() {
        yAxisLabelsLayouts.zip(getLinearInterpolation(gridRectangle.minY, gridRectangle.maxY, yAxisLabels.size))
            .forEach { (layout, y) ->
                layout.draw(
                    SVGCanvas, (gridRectangle.minX - layout.bounds.width - layout.bounds.x - defaultMargin).toFloat(),
                    (y - (layout.bounds.y / 2.0)).toFloat()
                )
            }
    }

    /**
     * Renders X (horizontal) axis labels.
     */
    fun renderXAxisLabels() {
        xAxisLabelsLayouts.zip(getLinearInterpolation(gridRectangle.minX, gridRectangle.maxX, xAxisLabels.size))
            .forEach { (layout, x) ->
                layout.draw(
                    SVGCanvas, (x - (layout.bounds.width / 2.0) - layout.bounds.x).toFloat(),
                    (gridRectangle.maxY - layout.bounds.y + defaultMargin).toFloat()
                )
            }
    }

    /**
     * Renders points on scatter chart.
     */
    fun renderPoints() {
        val radius = style.pointRadius
        data.values.forEach { row ->
            val pointX = gridRectangle.minX + ((row[0] + xDelta) / xMaxValue) * gridRectangle.width
            val pointY = gridRectangle.minY + ((-row[1] + yDelta) / yMaxValue) * gridRectangle.height

            val point = Ellipse2D.Double(pointX - radius, pointY - radius, 2 * radius, 2 * radius)

            renderShapeWithOutline(point, style.pointsColor, SVGCanvas)
        }
    }

    /**
     * Renders whole chart.
     */
    fun render() {
        renderTitle(data.chartTitle, titleRectangle, SVGCanvas)

        generateValuesLabels()
        generateAllLabelsLayouts()
        graphRectangle = getGraphRectangle(titleRectangle, style.size, Rectangle2D.Double())
        gridRectangle = getGridRectangle(graphRectangle, xAxisLabelsLayouts, yAxisLabelsLayouts)

        renderYAxisLabels()
        renderXAxisLabels()
        renderGrid(gridRectangle, yAxisLabels.size, Orientation.VERTICAL, SVGCanvas)
        renderGrid(gridRectangle, xAxisLabels.size, Orientation.HORIZONTAL, SVGCanvas)

        renderPoints()
    }
}