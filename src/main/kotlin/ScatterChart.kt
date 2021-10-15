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
    val titleRectangle = Rectangle2D.Double()
    val graphRectangle = Rectangle2D.Double()
    val gridRectangle = Rectangle2D.Double()

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
     * Renders title and calculates title rectangle.
     */
    fun renderTitleSetTitleRectangle() {
        titleRectangle.apply {
            x = defaultMargin
            y = defaultMargin
            width = style.size.width.toDouble() - 2 * defaultMargin
            height = 0.0
        }

        if (data.chartTitle.isNotEmpty()) {
            val titleLayout = TextLayout(data.chartTitle, titleFont, SVGCanvas.fontRenderContext)
            titleRectangle.height = 2 * defaultMargin + titleLayout.bounds.height

            titleLayout.draw(
                SVGCanvas, (titleRectangle.centerX - (titleLayout.bounds.width / 2.0) - titleLayout.bounds.x).toFloat(),
                (titleRectangle.centerY - (titleLayout.bounds.height / 2.0) - titleLayout.bounds.y).toFloat()
            )
        }
    }

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
     * Calculates graph and grid rectangles. Graph rectangle includes grid, and both axes labels.
     */
    fun setGraphAndGridRectangle() {
        graphRectangle.apply {
            x = defaultMargin
            y = titleRectangle.maxY
            width = style.size.width.toDouble() - 2 * defaultMargin
            height = style.size.height - defaultMargin - titleRectangle.maxY
        }

        gridRectangle.apply {
            x = graphRectangle.minX + defaultMargin + yAxisLabelsLayouts.maxOf { it.bounds.width }
            y = graphRectangle.minY + defaultMargin
            width = graphRectangle.width - 2 * defaultMargin - yAxisLabelsLayouts.maxOf { it.bounds.width }
            height = graphRectangle.height - 2 * defaultMargin - xAxisLabelsLayouts.maxOf { it.bounds.height }
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

            SVGCanvas.color = style.dotsColor
            SVGCanvas.fill(point)
            SVGCanvas.color = strokeColor
            SVGCanvas.draw(point)
        }
    }

    /**
     * Renders whole chart.
     */
    fun render() {
        renderTitleSetTitleRectangle()

        generateValuesLabels()
        generateAllLabelsLayouts()
        setGraphAndGridRectangle()

        renderYAxisLabels()
        renderXAxisLabels()
        renderGrid(gridRectangle, yAxisLabels.size, Orientation.VERTICAL, SVGCanvas)
        renderGrid(gridRectangle, xAxisLabels.size, Orientation.HORIZONTAL, SVGCanvas)

        renderPoints()
    }
}