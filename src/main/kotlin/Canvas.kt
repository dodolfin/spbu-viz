import org.apache.batik.dom.GenericDOMImplementation
import org.apache.batik.svggen.SVGGraphics2D
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import org.w3c.dom.DOMImplementation
import org.w3c.dom.Document
import java.awt.Dimension
import java.io.File
import java.io.FileOutputStream


/**
 * Since SVGGraphics2D works like a canvas that has methods like "draw this" and "draw that", the name SVGCanvas is
 * more suitable.
 */
typealias SVGCanvas = SVGGraphics2D

/**
 * Class that stores technical information about SVG document which we render.
 */
data class SVGChart(val document: Document, val SVGCanvas: SVGCanvas)

/**
 * Function that returns empty chart with SVGCanvas. Copied from Apache Batik documentation and slightly modified.
 */
fun getEmptyChart(size: Dimension): SVGChart {
    // Get a DOMImplementation
    val domImpl: DOMImplementation = GenericDOMImplementation.getDOMImplementation()

    // Create an instance of org.w3c.dom.Document
    val svgNS = "http://www.w3.org/2000/svg"
    val document = domImpl.createDocument(svgNS, "svg", null)

    val SVGCanvas = SVGCanvas(document)
    SVGCanvas.svgCanvasSize = Dimension(size.width, size.height)

    // Return an instance of the SVGChart
    return SVGChart(document, SVGCanvas(document))
}

fun rasterize(inputFilename: String, outputFilename: String, size: Dimension) {
    // Create a JPEGTranscoder and set its quality hint.
    val t = PNGTranscoder()

    // Set the transcoder input and output.
    val input = TranscoderInput(File(inputFilename).toURI().toString())
    val ostream = FileOutputStream(outputFilename)
    val output = TranscoderOutput(ostream)

    // Perform the transcoding.
    t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (size.width.toFloat() + defaultMargin).toFloat())
    t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (size.height.toFloat() + defaultMargin).toFloat())
    t.transcode(input, output)
    ostream.flush()
    ostream.close()
}