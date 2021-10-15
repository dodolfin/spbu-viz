import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import org.apache.batik.swing.JSVGCanvas
import org.apache.batik.swing.gvt.GVTTreeRendererAdapter
import org.apache.batik.swing.gvt.GVTTreeRendererEvent
import org.apache.batik.swing.svg.GVTTreeBuilderAdapter
import org.apache.batik.swing.svg.GVTTreeBuilderEvent
import org.apache.batik.swing.svg.SVGDocumentLoaderAdapter
import org.apache.batik.swing.svg.SVGDocumentLoaderEvent
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel

class Viz : CliktCommand() {
    val outputFile: File by option("-o", "--output", help = "the name of the output file").file(canBeDir = false, canBeSymlink = false, mustBeReadable = true, mustBeWritable = true).default(File("output.svg"))
    val inputFile: File by option("-d", "--data", help = "the name of the data file (in CSV format)").file(canBeDir = false, canBeSymlink = false, mustExist = true, mustBeReadable = true).required()
    val styleFile: File? by option("-y", "--style", help = "the name of the style file (in JSON format)").file(canBeDir = false, canBeSymlink = false, mustExist = true, mustBeReadable = true)
    val chartSize: Pair<Int, Int> by option("-s", "--size", help = "dimensions of the output file: first width, then height").int().pair().default(Pair(800, 600))
    val renderPNG: Boolean by option("-p", "--PNG", help = "render PNG, if this option is present").flag()
    val extractRowsLabels: Boolean by option("-r", "--rows-labels", help = "treat first column as labels for rows in CSV").flag()
    val extractColumnLabels: Boolean by option("-c", "--columns-labels", help = "treat first row as labels for columns in CSV").flag()
    val chartType: String by option("-t", "--type", help = "the type of the chart").choice("bar", "histogram", "pie").required()
    val chartTitle: String? by option("--title", help = "set the chart title")

    override fun run() {
        val parsedCSV = parseCSVBarChartData(inputFile, extractRowsLabels, extractColumnLabels)

        if (parsedCSV == null) {
            println("Malformed input file.")
            return
        }

        val size = Size(chartSize.first, chartSize.second)
        val SVGChart = getEmptyChart(size)

        when (chartType) {
            "bar" -> {
                BarChart(
                    BarChartData(
                        chartTitle ?: "",
                        parsedCSV.columnsLabels,
                        parsedCSV.rowsLabels,
                        parsedCSV.values
                    ),
                    BarChartStyle(size = Size(chartSize.first, chartSize.second)),
                    SVGChart.SVGCanvas
                ).render()
            }
            "histogram" -> {
                HistogramChart(
                    HistogramChartData(
                        chartTitle ?: "",
                        parsedCSV.values,
                        10
                    ),
                    HistogramChartStyle(size = size),
                    SVGChart.SVGCanvas
                ).render()
            }
            "pie" -> {
                PieChart(
                    PieChartData(
                        chartTitle ?: "",
                        parsedCSV.columnsLabels,
                        parsedCSV.values
                    ),
                    PieChartStyle(size = size),
                    SVGChart.SVGCanvas
                ).render()
            }
        }

        val outSVGName = "${outputFile.nameWithoutExtension}.svg"
        val outPNGName = "${outputFile.nameWithoutExtension}.png"
        SVGChart.SVGCanvas.stream(outSVGName)

        if (renderPNG) {
            rasterize(outSVGName, outPNGName, size)
        }

        createWindow("pf-2021-viz", outSVGName)
    }
}

/**
 * Magic function that creates window.
 */
fun createWindow(title: String, filename: String) = runBlocking(Dispatchers.Swing) {
    val f = JFrame(title)
    val app = SVGApplication(f)

    f.contentPane.add(app.createComponents(filename))

    f.addWindowListener(object : WindowAdapter() {
        override fun windowClosing(e: WindowEvent) {
            System.exit(0)
        }
    })
    f.setSize(1024, 768)
    f.isVisible = true
}

/**
 * Class that represents the window with rendered SVG.
 */
class SVGApplication(val frame: JFrame) {
    /**
     * Debug label which is displayed on top of the window to track the state of SVG rendering.
     */
    val label = JLabel()

    /**
     * Widget that renders SVG. Provided by the Apache Batik library
     */
    val svgCanvas = JSVGCanvas()

    /**
     * Magic functions that places widgets on window. Copied from Apache Batik documentation and slightly modified.
     */
    fun createComponents(filename: String): JComponent {
        val panel = JPanel(BorderLayout())
        val p = JPanel(FlowLayout(FlowLayout.LEFT))
        p.add(label)
        panel.add("North", p)
        panel.add("Center", svgCanvas)

        svgCanvas.uri = File(filename).toURI().toString()
        svgCanvas.setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC)
        // Set the JSVGCanvas listeners.
        svgCanvas.addSVGDocumentLoaderListener(object : SVGDocumentLoaderAdapter() {
            override fun documentLoadingStarted(e: SVGDocumentLoaderEvent) {
                label.text = "Document Loading..."
            }

            override fun documentLoadingCompleted(e: SVGDocumentLoaderEvent) {
                label.text = "Document Loaded."
            }
        })
        svgCanvas.addGVTTreeBuilderListener(object : GVTTreeBuilderAdapter() {
            override fun gvtBuildStarted(e: GVTTreeBuilderEvent) {
                label.text = "Build Started..."
            }

            override fun gvtBuildCompleted(e: GVTTreeBuilderEvent) {
                label.text = "Build Done."
                frame.pack()
            }
        })
        svgCanvas.addGVTTreeRendererListener(object : GVTTreeRendererAdapter() {
            override fun gvtRenderingPrepare(e: GVTTreeRendererEvent) {
                label.text = "Rendering Started..."
            }

            override fun gvtRenderingCompleted(e: GVTTreeRendererEvent) {
                label.text = ""
            }
        })

        return panel
    }
}