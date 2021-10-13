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
     * Widget that renders SVG provided by the Apache Batik library
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