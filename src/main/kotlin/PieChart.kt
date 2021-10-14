import java.awt.Color
import java.awt.Font
import java.awt.font.TextLayout
import java.awt.geom.Arc2D
import java.awt.geom.Rectangle2D

/**
 * The data for rendering chart itself. [values] stores the 2D table with data (as in Excel). Only
 * separate bars, and columns are treated as bars with different colors.
 */
data class PieChartData(
    val chartTitle: String,
    val columnsTitles: List<String>,
    val values: List<List<Double>>
)

/**
 * Represents all data needed for rendering pie chart.
 */
data class PieChart(val data: PieChartData, val style: PieChartStyle, val SVGCanvas: SVGCanvas) {
    /**
     * [titleRectangle], [graphRectangle], [pieRectangle] and [legendRectangle] represent rectangles in which corresponding
     * parts of the chart are rendered (with [defaultMargin] indentation)
     */
    val titleRectangle = Rectangle2D.Double()
    val graphRectangle = Rectangle2D.Double()
    val pieRectangle = Rectangle2D.Double()
    val legendRectangle = Rectangle2D.Double()

    /**
     * Stores the color of each column.
     */
    val columnsColors = mutableListOf<Color>()

    /**
     * Sum of all data values (needed to calculate angles of sectors)
     */
    var dataSum = 0.0

    /**
     * [columnsLabelsLayouts] stores TextLayout objects for virtually all text on the chart. We need to store
     * TextLayout objects to properly calculate different sizes & indentations.
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
        colorIndex = (colorIndex + 1) % style.sectorsColors.size
        return colorIndex
    }

    /**
     * Sets the [dataSum] value.
     */
    fun setDataSum() {
        dataSum = data.values[0].sum()
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
                SVGCanvas, (titleRectangle.centerX - (titleLayout.bounds.width / 2.0) - titleLayout.bounds.x).toFloat(),
                (titleRectangle.centerY - (titleLayout.bounds.height / 2.0) - titleLayout.bounds.y).toFloat()
            )
        }
    }

    /**
     * Generates all text layouts except chart title.
     */
    fun generateAllLabelsLayouts() {
        data.columnsTitles.forEach {
            columnsLabelsLayouts.add(TextLayout(it, labelFont, SVGCanvas.fontRenderContext))
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
            height = if (style.displayLegend) columnsLabelsLayouts.maxOf { it.bounds.height } else 0.0
        }
    }

    /**
     * Calculates graph and grid rectangles. Graph rectangle includes grid, and both axes labels.
     */
    fun setGraphAndPieRectangle() {
        graphRectangle.apply {
            x = defaultMargin
            y = titleRectangle.maxY
            width = style.size.width.toDouble() - 2 * defaultMargin
            height = legendRectangle.minY - titleRectangle.maxY
        }

        pieRectangle.apply {
            y = graphRectangle.y + defaultMargin
            height = graphRectangle.height - 2 * defaultMargin
            width = height
            x = graphRectangle.centerX - width / 2.0
        }
    }

    /**
     * Assigns color to sectors corresponding to different columns.
     */
    fun setSectorsColors() {
        repeat(data.values[0].size) {
            columnsColors.add(style.sectorsColors[nextColor()])
        }
    }

    /**
     * Renders the pie.
     */
    fun renderPie() {
        val firstRow = data.values[0]
        var currentAngle = 90.0
        firstRow.zip(columnsColors).reversed().forEach { (cell, color) ->
            val cellAngle = (cell / dataSum) * (360.0)
            val arc = Arc2D.Double(pieRectangle.x, pieRectangle.y, pieRectangle.width, pieRectangle.height, currentAngle, cellAngle, Arc2D.PIE)

            SVGCanvas.color = color
            SVGCanvas.fill(arc)
            SVGCanvas.color = Color.BLACK
            SVGCanvas.draw(arc)

            currentAngle += cellAngle
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

            layout.draw(SVGCanvas, (currentX - layout.bounds.x).toFloat(), (legendRectangle.minY + defaultMargin - layout.bounds.y).toFloat())

            currentX += layout.bounds.width + 2 * defaultMargin
        }
    }

    /**
     * Renders whole chart.
     */
    fun render() {
        renderTitleSetTitleRectangle()
        setDataSum()

        generateAllLabelsLayouts()
        setLegendRectangle()
        setGraphAndPieRectangle()

        setSectorsColors()
        renderPie()

        renderLegend()
    }
}