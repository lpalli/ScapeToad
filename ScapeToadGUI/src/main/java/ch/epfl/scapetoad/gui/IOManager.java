/*

	Copyright 2007 91NORD

	This program is free software; you can redistribute it and/or
	modify it under the terms of the GNU General Public License as
	published by the Free Software Foundation; either version 2 of the
	License, or (at your option) any later version.

	This program is distributed in the hope that it will be useful, but
	WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program; if not, write to the Free Software
	Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
	02110-1301, USA.
	
 */

package ch.epfl.scapetoad.gui;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.DriverProperties;
import com.vividsolutions.jump.io.IllegalParametersException;
import com.vividsolutions.jump.io.ShapefileReader;
import com.vividsolutions.jump.io.ShapefileWriter;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.renderer.style.LabelStyle;

/**
 * The input/output manager reads and writes all the files for our application.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-11-28
 */
public class IOManager {

    /**
     * The logger
     */
    private static Log logger = LogFactory.getLog(IOManager.class);

    /**
     * Displays an open dialog and reads the shape file in.
     * 
     * @return the layer
     */
    public static Layer openShapefile() {
        // Open the file dialog
        FileDialog dialog = new FileDialog(AppContext.mainWindow,
                "Add Layer...", FileDialog.LOAD);
        dialog.setFilenameFilter(new ShapeFilenameFilter());
        dialog.setModal(true);
        dialog.setBounds(20, 30, 150, 200);
        dialog.setVisible(true);

        // Get the selected file name
        if (dialog.getFile() == null) {
            // User has cancelled
            return null;
        }

        // Check the file type
        if (dialog.getFile().toUpperCase().endsWith(".SHP") == false) {
            OpenLayerErrorDialog errorDialog = new OpenLayerErrorDialog();
            errorDialog.setModal(true);
            errorDialog.setVisible(true);
        }

        // Read the shape file
        try {
            return IOManager.readShapefile(dialog.getDirectory()
                    + dialog.getFile());
        } catch (Exception e) {
            logger.error("Error reading the shape file", e);
        }

        return null;
    }

    /**
     * Reads the provided shape file and returns a Layer.
     * 
     * @param aShapePath
     *            the shape file path
     * @return the layer
     * @throws Exception
     *             generic exception reading the shape file
     * @throws IllegalParametersException
     *             exception in the parameters
     */
    private static Layer readShapefile(String aShapePath)
            throws IllegalParametersException, Exception {
        // Read the Shape file
        FeatureCollection features = new ShapefileReader()
                .read(new DriverProperties(aShapePath));

        // If there is no category "Original layers", we add one
        if (AppContext.layerManager.getCategory("Original layers") == null) {
            AppContext.layerManager.addCategory("Original layers");
        }

        // Add the layer to the "Original layers" category
        String layerName = IOManager.fileNameFromPath(aShapePath);
        Layer layer = new Layer(layerName, Color.GREEN, features,
                AppContext.layerManager);
        layer = AppContext.layerManager.addLayer("Original layers", layerName,
                features);

        // If the number of layers is 1, zoom to full extent in the
        // layer view panel.
        if (AppContext.layerManager.getLayers().size() == 1) {
            AppContext.layerViewPanel.getViewport().zoomToFullExtent();
        }

        AppContext.layerViewPanel.getViewport().update();

        return layer;
    }

    /**
     * Returns the file name from a given complete file path.
     * 
     * @param aPath
     *            the file path
     * @return the file name
     */
    private static String fileNameFromPath(String aPath) {
        // Find last / or \ and eliminate text before.
        String fileName = aPath.substring(Math.max(aPath.lastIndexOf("/"),
                aPath.lastIndexOf("\\")) + 1);
        // Find last . and eliminate text after
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    /**
     * Shows a save dialog and writes the shape file out.
     * 
     * @param aFeatures
     *            the features
     */
    public static void saveShapefile(FeatureCollection aFeatures) {
        // Create the File Save dialog
        FileDialog dialog = new FileDialog(AppContext.mainWindow,
                "Save Layer As...", FileDialog.SAVE);
        dialog.setFilenameFilter(new ShapeFilenameFilter());
        dialog.setModal(true);
        dialog.setBounds(20, 30, 150, 200);
        dialog.setVisible(true);

        // Get the selected File name
        if (dialog.getFile() == null) {
            AppContext.mainWindow
                    .setStatusMessage("[Save layer...] User has cancelled the action.");
            return;
        }

        String path = dialog.getDirectory() + dialog.getFile();
        if (path.endsWith(".shp") == false) {
            path += ".shp";
        }

        IOManager.writeShapefile(aFeatures, path);
    }

    /**
     * Writes the provided Feature Collection into a Shape file.
     * 
     * @param aFeatures
     *            the FeatureCollection to write out
     * @param aPath
     *            the path of the .shp file
     */
    public static void writeShapefile(FeatureCollection aFeatures, String aPath) {
        DriverProperties driveProperties = new DriverProperties();
        driveProperties.set("DefaultValue", aPath);
        driveProperties.set("ShapeType", "xy");

        try {
            new ShapefileWriter().write(aFeatures, driveProperties);
        } catch (Exception e) {
            logger.error("Error writing the shape file", e);
        }
    }

    /**
     * Shows a save file dialog for exporting the layers into a SVG file.
     * 
     * @param aLayers
     *            an array with the layers to include in the SVG file
     */
    public static void saveSVG(Layer[] aLayers) {
        // Create the File Save dialog
        FileDialog dialog = new FileDialog(AppContext.mainWindow,
                "Save Layer As...", FileDialog.SAVE);
        dialog.setFilenameFilter(new SVGFilenameFilter());
        dialog.setModal(true);
        dialog.setBounds(20, 30, 150, 200);
        dialog.setVisible(true);

        // Get the selected File name
        if (dialog.getFile() == null) {
            AppContext.mainWindow
                    .setStatusMessage("[Export as SVG...] User has cancelled the action.");
            return;
        }

        String path = dialog.getDirectory() + dialog.getFile();
        if (path.endsWith(".svg") == false) {
            path += ".svg";
        }

        IOManager.writeSVG(aLayers, path);
    }

    /**
     * Writes the provided layers into a SVG file.
     * 
     * @param aLayers
     *            an array with the layers to include in the SVG file.
     * @param aPath
     *            the location of the SVG file to create.
     */
    private static void writeSVG(Layer[] aLayers, String aPath) {
        int nlyrs = aLayers.length;
        if (nlyrs < 1) {
            logger.warn("No layer available");
            return;
        }

        // Find the extent of all layers
        Envelope extent = new Envelope(aLayers[0].getFeatureCollectionWrapper()
                .getEnvelope());
        for (int i = 1; i < nlyrs; i++) {
            extent.expandToInclude(aLayers[i].getFeatureCollectionWrapper()
                    .getEnvelope());
        }

        // Find the dimensions of the output SVG file. We suppose a A4 document
        // with 595 x 842 pixels. The orientation depends on the extent
        int svgWidth = 595;
        int svgHeight = 842;
        if (extent.getWidth() > extent.getHeight()) {
            svgWidth = 842;
            svgHeight = 595;
        }

        // Define the margins
        int svgMarginLeft = 30;
        int svgMarginRight = 30;
        int svgMarginTop = 30;
        int svgMarginBottom = 30;

        // Compute the scaling factor for the coordinate conversion
        double scaleFactorX = (svgWidth - svgMarginLeft - svgMarginRight)
                / extent.getWidth();
        double scaleFactorY = (svgHeight - svgMarginTop - svgMarginBottom)
                / extent.getHeight();
        double sclfact = Math.min(scaleFactorX, scaleFactorY);

        PrintWriter writer = null;
        try {
            // Open the file to write out
            writer = new PrintWriter(new FileWriter(aPath));

            // Write the XML header
            writer.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            writer.println("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"");
            writer.println(" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">");

            // Write the SVG header
            writer.println("<svg version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\"");
            writer.println("     x=\"0px\" y=\"0px\" width=\"" + svgWidth
                    + "px\" height=\"" + svgHeight + "px\" viewBox=\"0 0 "
                    + svgWidth + " " + svgHeight + "\">");
            writer.println("");

            // Write layer by layer
            Geometry geometry;
            String type;
            String fillColorString;
            StringBuilder builder;
            double x;
            double y;
            LabelStyle style;
            String attrName;
            Feature feature;
            Point center;
            for (int j = nlyrs - 1; j >= 0; j--) {
                // Create a group for every layer
                writer.println("	<g id=\"" + aLayers[j].getName() + "\">");

                // Get the colors and transparency for this layer
                Color fillColor = aLayers[j].getBasicStyle().getFillColor();
                Color strokeColor = aLayers[j].getBasicStyle().getLineColor();

                // Output every Feature
                FeatureCollection fc = aLayers[j].getFeatureCollectionWrapper();
                @SuppressWarnings("unchecked")
                Iterator<Feature> iterator = fc.iterator();
                while (iterator.hasNext()) {
                    // If it is a point, we output a small rectangle, otherwise
                    // we output a path
                    geometry = iterator.next().getGeometry();
                    type = geometry.getGeometryType();

                    if (type == "Point" || type == "MultiPoint") {
                        for (Coordinate coordinate : geometry.getCoordinates()) {
                            x = (coordinate.x - extent.getMinX()) * sclfact
                                    + svgMarginLeft;
                            y = (coordinate.y - extent.getMinY())
                                    / extent.getHeight();

                            y = 1.0 - y;
                            y = y * extent.getHeight() * sclfact + svgMarginTop;

                            writer.println("		<rect fill=\"rgb("
                                    + fillColor.getRed() + ","
                                    + fillColor.getGreen() + ","
                                    + fillColor.getBlue() + ")\" x=\"" + x
                                    + "\" y=\"" + y
                                    + "\" width=\"2\" height=\"2\" />");
                        }
                    } else {
                        // Fill only a polygon.
                        fillColorString = "none";
                        if (type == "Polygon" || type == "MultiPolygon") {
                            fillColorString = "rgb(" + fillColor.getRed() + ","
                                    + fillColor.getGreen() + ","
                                    + fillColor.getBlue() + ")";
                        }

                        builder = new StringBuilder();
                        IOManager.geometryToSVGPath(builder, geometry, extent,
                                sclfact, svgMarginLeft, svgWidth
                                        - svgMarginRight, svgMarginTop,
                                svgHeight - svgMarginBottom);

                        if (builder.length() > 0) {
                            writer.print("		<path fill=\"" + fillColorString
                                    + "\" stroke=\"rgb(" + strokeColor.getRed()
                                    + "," + strokeColor.getGreen() + ","
                                    + strokeColor.getBlue()
                                    + ")\" stroke-width=\"0.5pt\" d=\"");

                            writer.print(builder.toString());

                            // Close the path if it is a polygon.
                            if (type == "Polygon" || type == "MultiPolygon") {
                                writer.print("z");
                            }

                            writer.println("\" />");
                        }
                    }
                }

                // Close the layer group.
                writer.println("	</g>");
                writer.println("");
            }

            // Write the labels if there are any.
            // (There are typically for the legend layer.)
            for (int i = nlyrs - 1; i >= 0; i--) {
                style = aLayers[i].getLabelStyle();
                if (style.isEnabled()) {
                    writer.println("<g>");

                    attrName = style.getAttribute();

                    // Output every Feature label.
                    @SuppressWarnings("unchecked")
                    Iterator<Feature> iterator = aLayers[i]
                            .getFeatureCollectionWrapper().iterator();
                    while (iterator.hasNext()) {
                        feature = iterator.next();
                        center = feature.getGeometry().getCentroid();

                        x = (center.getX() - extent.getMinX()) * sclfact
                                + svgMarginLeft;

                        y = (center.getY() - extent.getMinY())
                                / extent.getHeight();
                        y = 1.0 - y;
                        y = y * extent.getHeight() * sclfact + svgMarginTop;

                        writer.print("<text x=\"" + x + "\" y=\"" + y
                                + "\" text-anchor=\"middle\">");
                        writer.print(feature.getAttribute(attrName));
                        writer.print("</text>");
                    }

                    writer.println("</g>");
                }
            }

            // Write the SVG footer
            writer.print("</svg>");
        } catch (IOException e) {
            logger.error("Exception writing SVG file", e);
        } finally {
            if (writer != null) {
                // Close the output stream.
                writer.close();
            }
        }
    }

    /**
     * Converts a Geometry into a SVG path sequence.
     * 
     * @param aBuilder
     *            the string builder
     * @param aGeom
     *            the Geometry to convert.
     * @param aEnveloppe
     *            the envelope we use for the coordinate conversion.
     * @param aScaleFactor
     *            the scale factor
     * @param aMinX
     *            min X coordinates for the SVG coordinates and corresponding to
     *            the envelope
     * @param aMaxX
     *            max X
     * @param aMinY
     *            min Y
     * @param aMaxY
     *            max
     */
    private static void geometryToSVGPath(StringBuilder aBuilder,
            Geometry aGeom, Envelope aEnveloppe, double aScaleFactor,
            double aMinX, double aMaxX, double aMinY, double aMaxY) {
        int ngeoms = aGeom.getNumGeometries();
        if (ngeoms > 1) {
            for (int i = 0; i < ngeoms; i++) {
                IOManager.geometryToSVGPath(aBuilder, aGeom.getGeometryN(i),
                        aEnveloppe, aScaleFactor, aMinX, aMaxX, aMinY, aMaxY);
            }
        } else {
            // Get the type of the Geometry. If it is a Polygon, we should
            // handle the exterior and interior rings in an appropriate way
            if (aGeom.getGeometryType() == "Polygon") {
                // Exterior ring first.
                Polygon polygon = (Polygon) aGeom;
                IOManager.coordinatesToSVGPath(aBuilder, polygon
                        .getExteriorRing().getCoordinates(), aEnveloppe,
                        aScaleFactor, aMinX, aMinY);

                // Interior rings.
                int nrings = polygon.getNumInteriorRing();
                for (int i = 0; i < nrings; i++) {
                    IOManager.coordinatesToSVGPath(aBuilder, polygon
                            .getInteriorRingN(i).getCoordinates(), aEnveloppe,
                            aScaleFactor, aMinX, aMinY);
                }
            } else {
                IOManager.coordinatesToSVGPath(aBuilder,
                        aGeom.getCoordinates(), aEnveloppe, aScaleFactor,
                        aMinX, aMinY);
            }
        }
    }

    /**
     * Converts a Coordinate sequence into a SVG path sequence.
     * 
     * @param aBuilder
     *            the string builder
     * @param aCoordinates
     *            the coordinates to convert (a Coordinate[])
     * @param aEnveloppe
     *            the envelope we use for the coordinate conversion
     * @param aScaleFactor
     *            the scale factor
     * @param aMinX
     *            min X coordinates for the SVG coordinates and corresponding to
     *            the envelope
     * @param aMinY
     *            min Y
     */
    private static void coordinatesToSVGPath(StringBuilder aBuilder,
            Coordinate[] aCoordinates, Envelope aEnveloppe,
            double aScaleFactor, double aMinX, double aMinY) {
        int ncoords = aCoordinates.length;
        if (ncoords == 0) {
            return;
        }
        aBuilder.append("M ");

        int coordcnt = 0;
        for (coordcnt = 0; coordcnt < ncoords; coordcnt++) {
            double x = (aCoordinates[coordcnt].x - aEnveloppe.getMinX())
                    * aScaleFactor + aMinX;
            double y = (aCoordinates[coordcnt].y - aEnveloppe.getMinY())
                    / aEnveloppe.getHeight();
            y = 1.0 - y;
            y = y * aEnveloppe.getHeight() * aScaleFactor + aMinY;
            aBuilder.append(x + " " + y + " ");

            if (coordcnt < ncoords - 1) {
                aBuilder.append("L ");
            }
        }
    }
}

/**
 * This class allows the filtering of Shapefiles in the Java file dialog.
 */
class ShapeFilenameFilter implements FilenameFilter {

    /**
     * This is the method used for filtering.
     */
    @Override
    public boolean accept(File aDir, String aName) {
        return aName.toUpperCase().endsWith(".SHP");
    }
}

/**
 * This class allows the filtering of SVG files in the Java file dialog.
 */
class SVGFilenameFilter implements FilenameFilter {

    /**
     * This is the method used for filtering.
     */
    @Override
    public boolean accept(File aDir, String aName) {
        return aName.toUpperCase().endsWith(".SVG");
    }
}

/**
 * Dialog window for Shape file error. Normally, in the open layer dialog, there
 * is a filter for Shape files. But M$ does not think it useful...
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2008-05-20
 */
class OpenLayerErrorDialog extends JDialog {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for the export SVG file dialog.
     */
    protected OpenLayerErrorDialog() {
        // Set the window parameters.
        setTitle("Open layer error");
        setSize(300, 130);
        setLocation(40, 50);
        setResizable(false);
        setLayout(null);
        setModal(true);

        JLabel label = new JLabel("Not a Shape file.");
        label.setSize(260, 14);
        label.setFont(new Font(null, Font.PLAIN, 11));
        label.setLocation(20, 20);
        add(label);

        label = new JLabel("Please select a file with the extension .shp.");
        label.setSize(260, 14);
        label.setFont(new Font(null, Font.PLAIN, 11));
        label.setLocation(20, 40);
        add(label);

        // Ok button
        JButton button = new JButton("OK");
        button.setLocation(180, 70);
        button.setSize(100, 26);
        button.addActionListener(new OpenLayerErrorDialogAction(this));
        button.setMnemonic(KeyEvent.VK_ENTER);
        add(button);
    }
}

/**
 * The actions for the open layer error dialog.
 */
class OpenLayerErrorDialogAction extends AbstractAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The dialog
     */
    private OpenLayerErrorDialog iDialog;

    /**
     * Constructor.
     * 
     * @param aDialog
     *            the open layer dialog
     */
    protected OpenLayerErrorDialogAction(OpenLayerErrorDialog aDialog) {
        iDialog = aDialog;
    }

    @Override
    public void actionPerformed(ActionEvent aEvent) {
        iDialog.setVisible(false);
        iDialog.dispose();
    }
}