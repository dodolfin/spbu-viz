/**
 * The data for rendering chart itself. [values] stores the 2D table with data (as in Excel). Only the first column is
 * taken as data, all others are ignored.
 */
data class HistogramChartData(
    val chartTitle: String,
    val values: List<List<Double>>,
    val barsCount: Int
) {
    /**
     * Returns data for building bar chart.
     */
    fun getBarChartData(): BarChartData {
        val firstColumn = values.map { it[0] }

        val minValue = firstColumn.minOrNull()
        checkNotNull(minValue)
        val maxValue = firstColumn.maxOrNull()
        checkNotNull(maxValue)

        val values = mutableListOf<Double>()
        val rowsTitles = mutableListOf<String>()

        val rangeLength = getLinearInterpolationDelta(minValue, maxValue, barsCount + 1)
        with(getLinearInterpolation(minValue, maxValue, barsCount + 1).dropLast(1)) {
            this.forEachIndexed { index, value ->
                val leftBound = value
                val rightBound = value + rangeLength

                values.add(firstColumn.count { it >= leftBound && (it < rightBound || (index == this.lastIndex && it <= rightBound)) }.toDouble())
                rowsTitles.add("[${String.format("%.3f", leftBound)}, ${String.format("%.3f", rightBound)}${if (index != this.lastIndex) ")" else "]"}")
            }
        }

        return BarChartData(chartTitle, listOf("Data"), rowsTitles, values.map { listOf(it) })
    }
}

/**
 * Represents all data needed for rendering HistogramChart.
 */
data class HistogramChart(val data: HistogramChartData, val style: HistogramChartStyle, val SVGCanvas: SVGCanvas) {
    /**
     * Renders the histogram chart. Note that histogram rendering is the special case of rendering BarChart.
     */
    fun render() {
        BarChart(
            data.getBarChartData(),
            BarChartStyle(
                size = style.size,
                barColors = listOf(style.barColor),
                displayLegend = false,
                barWidthRate = 1.0
            ),
            SVGCanvas
        ).render()
    }
}
