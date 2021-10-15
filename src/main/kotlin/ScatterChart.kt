import java.awt.font.TextLayout
import java.awt.geom.Ellipse2D
import java.awt.geom.Rectangle2D
import kotlin.math.*

data class ScatterChartData(
    val chartTitle: String,
    val values: List<List<Double>>
)

data class ScatterChart(val data: ScatterChartData, val style: ScatterChartStyle, val SVGCanvas: SVGCanvas) {
    /**
     * [titleRectangle], [graphRectangle], [gridRectangle] and [legendRectangle] represent rectangles in which corresponding
     * parts of the chart are rendered (with [defaultMargin] indentation)
     */
    val titleRectangle = Rectangle2D.Double()
    val graphRectangle = Rectangle2D.Double()
    val gridRectangle = Rectangle2D.Double()

    /**
     * [valuesAxisLabels] stores labels upon which grid is drawn. Each label can be represented by formula k * 10 ^ p,
     * where k is in {0, 1, 2, 3, 4, 5, 6, 7, 8, 9} and
     * p is minimal p (such as 10 ^ p is less than maximum value in data) minus 1.
     */
    val xAxisLabels = mutableListOf<Double>()
    val yAxisLabels = mutableListOf<Double>()

    /**
     * Maximum value of [valuesAxisLabels]
     */
    var xDelta = 0.0
    var xMaxValue = 0.0
    var yDelta = 0.0
    var yMaxValue = 0.0

    /**
     * [valuesLabelsLayouts], [columnsLabelsLayouts] and [rowsLabelsLayouts] stores TextLayout objects for virtually
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

    fun getDelta(a: List<Double>) = -a.minOf { it }

    /**
     * Makes labels upon which grid is drawn. See [valuesAxisLabels] documentation for more information.
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
        yAxisLabelsLayouts.zip(getLinearInterpolation(gridRectangle.minY, gridRectangle.maxY, yAxisLabels.size)).forEach { (layout, y) ->
            layout.draw(SVGCanvas, (gridRectangle.minX - layout.bounds.width - layout.bounds.x - defaultMargin).toFloat(),
                (y - (layout.bounds.y / 2.0)).toFloat()
            )
        }
    }

    /**
     * Renders X (horizontal) axis labels. Note that depending on [style].orientation, those might be [valuesLabelsLayouts],
     * as well as [rowsLabelsLayouts].
     */
    fun renderXAxisLabels() {
        xAxisLabelsLayouts.zip(getLinearInterpolation(gridRectangle.minX, gridRectangle.maxX, xAxisLabels.size)).forEach { (layout, x) ->
            layout.draw(SVGCanvas, (x - (layout.bounds.width / 2.0) - layout.bounds.x).toFloat(),
                (gridRectangle.maxY - layout.bounds.y + defaultMargin).toFloat()
            )
        }
    }

    /**
     * Renders bars if [style].orientation is vertical.
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