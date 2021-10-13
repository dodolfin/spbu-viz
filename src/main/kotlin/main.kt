/**
 * Main function.
 */
fun main(args: Array<String>) {
    val SVGChart = getEmptyChart()
    val chart = BarChart(
        BarChartData("Test",
            listOf("Org1", "Org2", "Org3", "Org4"),
            listOf("Quarter 1/21", "Q2/21", "Q3/21", "Q4/21", "Planned"),
            listOf(
                listOf(150.0, 342.0, 234.0, 500.0),
                listOf(180.0, 400.0, 210.0, 487.999),
                listOf(120.0, 378.0, 260.0, 420.0),
                listOf(147.0, 350.0, 215.55, 610.0),
                listOf(160.0, 321.0, 275.111, 543.43)
            )
        ),
        BarChartStyle(size = Size(800, 600), orientation = BarChartOrientation.VERTICAL, multipleValuesDisplay = BarChartMultipleValuesDisplay.STACKED),
        SVGChart.SVGCanvas
    )
    chart.render()

    SVGChart.SVGCanvas.stream("produce.svg")

    createWindow("pf-2021-viz", "produce.svg")
}
