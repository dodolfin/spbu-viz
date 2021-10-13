import org.apache.batik.svggen.SVGGraphics2D
import org.apache.batik.dom.GenericDOMImplementation

import org.w3c.dom.DOMImplementation
import org.w3c.dom.Document

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
fun getEmptyChart(): SVGChart {
    // Get a DOMImplementation
    val domImpl: DOMImplementation = GenericDOMImplementation.getDOMImplementation()

    // Create an instance of org.w3c.dom.Document
    val svgNS = "http://www.w3.org/2000/svg"
    val document = domImpl.createDocument(svgNS, "svg", null)

    // Return an instance of the SVGChart
    return SVGChart(document, SVGCanvas(document))
}