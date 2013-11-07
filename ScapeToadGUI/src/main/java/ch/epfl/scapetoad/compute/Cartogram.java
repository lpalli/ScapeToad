/*

	Copyright 2007-2009 361DEGRES

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

package ch.epfl.scapetoad.compute;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * The cartogram class is the main computation class. It is a subclass of the
 * SwingWorker class. It has methods for setting all the parameters and for
 * launching the computation.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-11-30
 */
public class Cartogram {

    /**
     * The logger.
     */
    private static Log logger = LogFactory.getLog(Cartogram.class);

    /**
     * The cartogram status.
     */
    private ICartogramStatus iStatus;

    /**
     * The name of the master layer.
     */
    private CartogramLayer iMasterLayer;

    /**
     * The name of the master attribute.
     */
    private String iMasterAttribute;

    /**
     * Is the master attribute already a density value, or must the value be
     * weighted by the polygon area (only available for polygons).
     */
    private boolean iMasterAttributeIsDensityValue = true;

    /**
     * The missing value to be replaced with the mean value.
     */
    private String iMissingValue = "";

    /**
     * The projected master layer. We store this in order to make the
     * computation report after the projection.
     */
    private CartogramLayer iProjectedMasterLayer;

    /**
     * The layers to deform simultaneously.
     */
    private List<CartogramLayer> iSlaveLayers;

    /**
     * The layers used for the constrained deformation.
     */
    private List<CartogramLayer> iConstrainedDeforamtionLayers;

    /**
     * The initial envelope for all layers.
     */
    private Envelope iEnvelope;

    /**
     * The size of the cartogram grid.
     */
    private int[] iGridSize = { 1000, 1000 };

    /**
     * All the deformation is done on this cartogram grid.
     */
    private CartogramGrid iGrid;

    /**
     * The amount of deformation is a simple stopping criterion. It is an
     * integer value between 0 (low deformation, early stopping) and 100 (high
     * deformation, late stopping).
     */
    private int iAmountOfDeformation = 50;

    /**
     * Are the advanced options enabled or should the parameters be estimated
     * automatically by the program?
     */
    private boolean iAdvancedOptionsEnabled;

    /**
     * The maximum length of one line segment. In the projection process, a
     * straight line might be deformed to a curve. If a line segment is too
     * long, it might result in a self intersection, especially for polygons.
     * This parameter can be controlled manually or estimated using the
     * maximumSegmentLength heuristic.
     */
    private double iMaximumSegmentLength;

    /**
     * The size of the grid which can be added as a deformation grid.
     */
    private int iGridLayerSize = 100;

    /**
     * The layer containing the deformation grid.
     */
    private CartogramLayer iDeformationGrid;

    /**
     * An array containing the legend values which should be represented in the
     * legend layer.
     */
    private double[] iLegendValues;

    /**
     * The layer containing the cartogram legend.
     */
    private CartogramLayer iLegendLayer;

    /**
     * The computation report.
     */
    private String iComputationReport;

    /**
     * Used for storing the start time of computation. The computation duration
     * is computed based on this value which is set before starting the
     * computation.
     */
    private long iComputationStartTime;

    /**
     * <code>true</code> if an error occurred during the computation.
     */
    private boolean iErrorOccured = false;

    /**
     * The constructor for the cartogram class.
     * 
     * @param aStatus
     *            the cartogram status
     */
    public Cartogram(ICartogramStatus aStatus) {
        iStatus = aStatus;
    }

    /**
     * Compute the cartogram layers.
     * 
     * @param aCreateGridLayer
     *            <code>true</code> to create the grid layer
     * @param aCreateLegendLayer
     *            <code>true</code> to create the legend layer
     * 
     * @return the projected layers
     */
    public List<CartogramLayer> compute(boolean aCreateGridLayer,
            boolean aCreateLegendLayer) {
        try {
            iComputationStartTime = System.nanoTime();

            if (iAdvancedOptionsEnabled == false) {
                // Automatic estimation of the parameters using the amount of
                // deformation slider.
                // The deformation slider modifies only the grid size between
                // 500 (quality 0) and 3000 (quality 100).
                iGridSize[0] = iAmountOfDeformation * 15 + 250;
                iGridSize[1] = iGridSize[0];
            }

            // User information.
            iStatus.updateRunningStatus(0,
                    "Preparing the cartogram computation...",
                    "Computing the cartogram bounding box");

            // Compute the envelope given the initial layers.
            // The envelope will be somewhat larger than just the layers.
            updateEnvelope();

            // Adjust the cartogram grid size in order to be proportional
            // to the envelope.
            adjustGridSizeToEnvelope();
            logger.debug(String.format("Adjusted grid size: %1$sx%2$s",
                    iGridSize[0], iGridSize[1]));

            iStatus.updateRunningStatus(20,
                    "Preparing the cartogram computation...",
                    "Creating the cartogram grid");

            // Create the cartogram grid.
            iGrid = new CartogramGrid(iGridSize[0], iGridSize[1], iEnvelope);

            if (Thread.interrupted()) {
                // Raise an InterruptedException.
                throw new InterruptedException(
                        "Computation has been interrupted by the user.");
            }

            // Check the master attribute for invalid values.

            iStatus.updateRunningStatus(50,
                    "Check the cartogram attribute values...", "");

            iMasterLayer.cleanAttributeValues(iMasterAttribute);

            // Replace the missing values with the layer mean value.
            if (iMissingValue != "" && iMissingValue != null) {
                iMasterLayer.replaceAttributeValue(iMasterAttribute,
                        Double.parseDouble(iMissingValue),
                        iMasterLayer.meanValueForAttribute(iMasterAttribute));
            }

            // Compute the density values for the cartogram grid using
            // the master layer and the master attribute.

            iStatus.updateRunningStatus(100,
                    "Computing the density for the cartogram grid...", "");

            iGrid.computeOriginalDensityValuesWithLayer(iMasterLayer,
                    iMasterAttribute, iMasterAttributeIsDensityValue, iStatus);

            if (Thread.interrupted()) {
                // Raise an InterruptedException.
                throw new InterruptedException(
                        "Computation has been interrupted by the user.");
            }

            // *** PREPARE THE GRID FOR THE CONSTRAINED DEFORMATION ***

            if (iConstrainedDeforamtionLayers != null) {
                iStatus.updateRunningStatus(300,
                        "Prepare constrained deformation...", "");

                iGrid.prepareGridForConstrainedDeformation(iConstrainedDeforamtionLayers);
            }

            if (Thread.interrupted()) {
                // Raise an InterruptedException.
                throw new InterruptedException(
                        "Computation has been interrupted by the user.");
            }

            // *** COMPUTE THE CARTOGRAM USING THE DIFFUSION ALGORITHM ***

            iStatus.updateRunningStatus(350,
                    "Computing cartogram diffusion...",
                    "Starting the diffusion process");
            CartogramNewman cnewm = new CartogramNewman(iGrid);

            // Enable the CartogramNewman instance to update the running status.
            cnewm.initializeStatus(iStatus, 350, 750,
                    "Computing cartogram diffusion...");

            // Let's go!
            cnewm.compute();

            if (Thread.interrupted()) {
                // Raise an InterruptedException.
                throw new InterruptedException(
                        "Computation has been interrupted by the user.");
            }

            // *** CONSTRAINED DEFORMATION ***
            if (iConstrainedDeforamtionLayers != null) {
                iStatus.updateRunningStatus(700,
                        "Applying the constrained deformation layers", "");

                iGrid.conformToConstrainedDeformation();
            }

            if (Thread.interrupted()) {
                // Raise an InterruptedException.
                throw new InterruptedException(
                        "Computation has been interrupted by the user.");
            }

            // Project all the layers
            iStatus.updateRunningStatus(750, "Projecting the layers...", "");
            List<CartogramLayer> layers = projectLayers();

            if (Thread.interrupted()) {
                // Raise an InterruptedException.
                throw new InterruptedException(
                        "Computation has been interrupted by the user.");
            }

            // Create the deformation grid layer
            if (aCreateGridLayer) {
                createGridLayer();
            }

            // Create the legend layer
            if (aCreateLegendLayer) {
                createLegendLayer();
            }

            iStatus.updateRunningStatus(950,
                    "Producing the computation report...", "");

            return layers;
        } catch (InterruptedException exception) {
            logger.error("Computation cancelled", exception);
            iStatus.setComputationError(
                    "The cartogram computation has been cancelled.", "", "");
            iErrorOccured = true;
            iStatus.finished();
        } catch (DataFormatException exception) {
            logger.error("All attribute values are zero", exception);
            // Retrieve the complete stack trace and display.
            iStatus.setComputationError(
                    "An error occured during cartogram computation!",
                    "All attribute values are zero", exception.getMessage());
            iErrorOccured = true;
            iStatus.finished();
        }
        return null;
    }

    /**
     * Finish the computation: adds all layers and produces the computation
     * report.
     * 
     * @param aLayers
     *            the projected layers
     * @param aSimultaneousLayers
     *            the simultaneous layers
     * @param aConstrainedDeformationLayers
     *            the constrained deformation layers
     */
    public void finish(List<CartogramLayer> aLayers,
            List<CartogramLayer> aSimultaneousLayers,
            List<CartogramLayer> aConstrainedDeformationLayers) {
        // If there was an error, stop here.
        if (iErrorOccured) {
            return;
        }

        if (aLayers == null) {
            iStatus.setComputationError(
                    "An error occured during cartogram computation!",
                    "",
                    "An unknown error has occured.\n\nThere may be unsufficient memory resources available. Try to:\n\n1.\tUse a smaller cartogram grid (through the transformation\n\tquality slider at the wizard step 5, or through the\n\t\"Advanced options...\" button, also at step 5.\n\n2.\tYou also may to want to increase the memory available\n\tto ScapeToad. To do so, you need the cross platform\n\tJAR file and \n\tlaunch ScapeToad from the command\n\tline, using the -Xmx flag of \n\tyour Java Virtual\n\tMachine. By default, ScapeToad has 1024 Mo of memory.\n\tDepending on your system, there may be less available.\n\n3.\tIf you think there is a bug in ScapeToad, you can file\n\ta bug \n\ton Sourceforge \n\t(http://sourceforge.net/projects/scapetoad).\n\tPlease describe in detail your problem and provide all\n\tnecessary \n\tdata for reproducing your error.\n\n");
            iStatus.finished();
            return;
        }

        // Produce the computation report
        produceComputationReport(iProjectedMasterLayer, aSimultaneousLayers,
                aConstrainedDeformationLayers);
    }

    /**
     * Sets the cartogram master layer.
     * 
     * @param aLayer
     *            the master layer
     */
    public void setMasterLayer(CartogramLayer aLayer) {
        iMasterLayer = aLayer;
    }

    /**
     * Sets the name of the cartogram master attribute.
     * 
     * @param aAttributeName
     *            the master cartogram name
     */
    public void setMasterAttribute(String aAttributeName) {
        iMasterAttribute = aAttributeName;
    }

    /**
     * Lets define us whether the master attribute is a density value or a
     * population value.
     * 
     * @param aIsDensityValue
     *            <code>true</code> for a density population
     */
    public void setMasterAttributeIsDensityValue(boolean aIsDensityValue) {
        iMasterAttributeIsDensityValue = aIsDensityValue;
    }

    /**
     * Defines the layers to deform during the cartogram process.
     * 
     * @param aSlaveLayers
     *            the slave layers
     */
    public void setSlaveLayers(List<CartogramLayer> aSlaveLayers) {
        iSlaveLayers = aSlaveLayers;
    }

    /**
     * Defines the layers which should not be deformed.
     * 
     * @param aLayers
     *            the layers not deformed
     */
    public void setConstrainedDeformationLayers(List<CartogramLayer> aLayers) {
        iConstrainedDeforamtionLayers = aLayers;
    }

    /**
     * Defines the grid size in x and y dimensions.
     * 
     * @param aSize
     *            the grid size
     */
    public void setGridSize(int[] aSize) {
        iGridSize[0] = aSize[0];
        iGridSize[1] = aSize[1];
    }

    /**
     * Defines the amount of deformation. This is an integer value between 0 and
     * 100. The default value is 50.
     * 
     * @param aDeformation
     *            the deformation
     */
    public void setAmountOfDeformation(int aDeformation) {
        iAmountOfDeformation = aDeformation;
    }

    /**
     * Computes the cartogram envelope using the provided layers. The envelope
     * will be larger than the layers in order to allow the cartogram
     * deformation inside this envelope.
     */
    private void updateEnvelope() {
        // Setting the initial envelope using the master layer.
        iEnvelope = iMasterLayer.getEnvelope();

        // Expanding the initial envelope using the slave and
        // constrained deformation layers.
        if (iSlaveLayers != null) {
            for (CartogramLayer layer : iSlaveLayers) {
                iEnvelope.expandToInclude(layer.getEnvelope());
            }
        }

        if (iConstrainedDeforamtionLayers != null) {
            for (CartogramLayer layer : iConstrainedDeforamtionLayers) {
                iEnvelope.expandToInclude(layer.getEnvelope());
            }
        }

        // Enlarge the envelope by 20%.
        iEnvelope.expandBy(iEnvelope.getWidth() * 0.2,
                iEnvelope.getHeight() * 0.2);
    }

    /**
     * Adjusts the grid size in order to be proportional to the envelope. It
     * will not increase the grid size, but it will decrease the grid size on
     * the shorter side.
     */
    private void adjustGridSizeToEnvelope() {
        if (iEnvelope == null) {
            return;
        }

        double width = iEnvelope.getWidth();
        double height = iEnvelope.getHeight();

        if (width < height) {
            // Adjust the x grid size.
            iGridSize[0] = (int) Math.round(iGridSize[1] * width / height);
        } else if (width > height) {
            // Adjust the y grid size.
            iGridSize[1] = (int) Math.round(iGridSize[0] * height / width);
        }
    }

    /**
     * Projects all layers. Creates a new layer for each projected layer.
     * 
     * @return the projected layers
     * @throws InterruptedException
     *             when the computation was interrupted
     */
    private List<CartogramLayer> projectLayers() throws InterruptedException {
        // Get the number of layers to project (one master layer and all slave
        // layers)
        int size = 1;
        if (iSlaveLayers != null) {
            size += iSlaveLayers.size();
        }

        // We store the projected layers in an array
        List<CartogramLayer> layers = new ArrayList<CartogramLayer>(size);

        // Compute the maximum segment length for the layers
        iMaximumSegmentLength = estimateMaximumSegmentLength();

        // Project the master layer
        iStatus.updateRunningStatus(750, "Projecting the layers...",
                "Layer 1 of %1$s", size);
        iMasterLayer.regularizeLayer(iMaximumSegmentLength);
        iProjectedMasterLayer = iMasterLayer.projectLayerWithGrid(iGrid);
        layers.add(iProjectedMasterLayer);

        if (Thread.interrupted()) {
            // Raise an InterruptedException.
            throw new InterruptedException(
                    "Computation has been interrupted by the user.");
        }

        // Project the slave layers
        if (iSlaveLayers != null) {
            int count = 1;
            for (CartogramLayer slaveLayer : iSlaveLayers) {
                iStatus.updateRunningStatus(800 + count / (size - 1) * 150,
                        "Projecting the layers...", "Layer %1$s of %2$s",
                        count + 1, size);

                slaveLayer.regularizeLayer(iMaximumSegmentLength);
                layers.add(slaveLayer.projectLayerWithGrid(iGrid));
                count++;
            }
        }

        return layers;
    }

    /**
     * Changes the size of the grid layer to produce.
     * 
     * @param aGridLayerSize
     *            the size of the grid layer
     */
    public void setGridLayerSize(int aGridLayerSize) {
        iGridLayerSize = aGridLayerSize;
    }

    /**
     * @param aEnabled
     *            <code>true</code> to enable the advanced options
     */
    public void setAdvancedOptionsEnabled(boolean aEnabled) {
        iAdvancedOptionsEnabled = aEnabled;
    }

    /**
     * Creates a layer with the deformation grid.
     */
    private void createGridLayer() {
        // Compute the deformation grid size in x and y direction.
        double resolution = Math.max(iEnvelope.getWidth()
                / (iGridLayerSize + 1), iEnvelope.getHeight()
                / (iGridLayerSize + 1));
        int sizeX = (int) Math.round(Math.floor(iEnvelope.getWidth()
                / resolution)) - 1;
        int sizeY = (int) Math.round(Math.floor(iEnvelope.getHeight()
                / resolution)) - 1;

        // Create a new attribute list for the new layer
        @SuppressWarnings("rawtypes")
        Map<String, Class> attributes = new HashMap<String, Class>(2);
        attributes.put("GEOMETRY", Geometry.class);
        attributes.put("ID", Integer.class);

        // Create a Geometry Factory for creating the points.
        GeometryFactory factory = new GeometryFactory();

        // Crate the features and lines
        int i = 0;
        List<CartogramFeature> features = new ArrayList<CartogramFeature>(sizeY
                + sizeX);
        CartogramFeature feature;
        Coordinate[] coords;
        LineString line;
        // Horizontal lines
        for (int k = 0; k < sizeY; k++) {
            // Create the line string and add it to the feature
            coords = new Coordinate[sizeX];
            for (int j = 0; j < sizeX; j++) {
                coords[j] = iGrid.projectPointAsCoordinate(iEnvelope.getMinX()
                        + j * resolution, iEnvelope.getMinY() + k * resolution);
            }
            line = factory.createLineString(coords);

            if (line != null) {
                // Create the feature
                feature = new CartogramFeature(line,
                        new HashMap<String, Object>(attributes.size()));

                // Add the other attributes
                feature.setAttribute("ID", i);
                i++;

                // Add the feature to the list
                features.add(feature);
            }
        }

        // Vertical lines
        for (int j = 0; j < sizeX; j++) {
            // Create the line string and add it to the feature
            coords = new Coordinate[sizeY];
            for (int k = 0; k < sizeY; k++) {
                coords[k] = iGrid.projectPointAsCoordinate(iEnvelope.getMinX()
                        + j * resolution, iEnvelope.getMinY() + k * resolution);
            }
            line = factory.createLineString(coords);

            if (line != null) {
                // Create the feature
                feature = new CartogramFeature(line,
                        new HashMap<String, Object>(attributes.size()));

                // Add the other attributes
                feature.setAttribute("ID", i);
                i++;

                // Add Feature to the Feature Dataset
                features.add(feature);
            }
        }

        // Create the layer
        iDeformationGrid = new CartogramLayer("Deformation grid", Color.GRAY,
                attributes, features);
    }

    /**
     * Creates an optional legend layer.
     */
    private void createLegendLayer() {
        double distanceBetweenSymbols = iMasterLayer.getEnvelope().getWidth() / 10;
        double attrMax = iMasterLayer.maxValueForAttribute(iMasterAttribute);

        // Estimate legend values if there are none
        if (iLegendValues == null) {
            double maxLog = Math.floor(Math.log10(attrMax));

            iLegendValues = new double[3];
            iLegendValues[0] = Math.pow(10, maxLog - 1);
            iLegendValues[1] = Math.pow(10, maxLog);
            iLegendValues[2] = attrMax;
        }

        // Create a new attribute list for the new layer
        @SuppressWarnings("rawtypes")
        Map<String, Class> attributes = new HashMap<String, Class>(5);
        attributes.put("GEOMETRY", Geometry.class);
        attributes.put("ID", Integer.class);
        attributes.put("VALUE", Double.class);
        attributes.put("AREA", Double.class);
        attributes.put("COMMENT", String.class);

        // Create a Geometry Factory for creating the points
        GeometryFactory factory = new GeometryFactory();

        // CREATE THE FEATURES FOR THE LEGEND LAYER
        int nvals = iLegendValues.length;
        double totalArea = iMasterLayer.totalArea();
        double valuesSum = iMasterLayer.sumForAttribute(iMasterAttribute);
        double x = iEnvelope.getMinX();
        double y = iEnvelope.getMinY();
        List<CartogramFeature> features = new ArrayList<CartogramFeature>(nvals);
        double valsize;
        double rectsize;
        Coordinate[] coords;
        CartogramFeature feature;
        for (int i = 0; i < nvals; i++) {
            valsize = totalArea / valuesSum * iLegendValues[i];
            rectsize = Math.sqrt(valsize);

            // Create the coordinate points
            coords = new Coordinate[5];
            coords[0] = new Coordinate(x, y);
            coords[1] = new Coordinate(x + rectsize, y);
            coords[2] = new Coordinate(x + rectsize, y - rectsize);
            coords[3] = new Coordinate(x, y - rectsize);
            coords[4] = new Coordinate(x, y);

            // Create the feature
            feature = new CartogramFeature(factory.createPolygon(
                    factory.createLinearRing(coords), null),
                    new HashMap<String, Object>(attributes.size()));
            feature.setAttribute("ID", i + 1);
            feature.setAttribute("VALUE", iLegendValues[i]);
            feature.setAttribute("AREA", valsize);

            if (i == 0) {
                feature.setAttribute("COMMENT", "Mean value");
            } else if (i == 1) {
                feature.setAttribute("COMMENT", String.format(
                        "Rounded value of maximum (%1$s)", attrMax));
            }

            // Add the feature to the list
            features.add(feature);

            // Change the coordinates
            x += rectsize + distanceBetweenSymbols;
        }

        // Create the layer
        iLegendLayer = new CartogramLayer("Legend", Color.GREEN, attributes,
                features);
    }

    /**
     * Creates the computation report and stores it in the object attribute.
     * 
     * @param aProjectedMasterLayer
     *            the projected master layer
     * @param aSimultaneousLayers
     *            the simultaneous layers
     * @param aConstrainedDeformationLayers
     *            the constrained deformation layers
     */
    private void produceComputationReport(CartogramLayer aProjectedMasterLayer,
            List<CartogramLayer> aSimultaneousLayers,
            List<CartogramLayer> aConstrainedDeformationLayers) {
        StringBuilder builder = new StringBuilder();

        builder.append("CARTOGRAM COMPUTATION REPORT\n\n");

        builder.append("CARTOGRAM PARAMETERS:\n");
        builder.append("Cartogram layer: ");
        builder.append(iMasterLayer.getName());
        builder.append('\n');
        builder.append("Cartogram attribute: ");
        builder.append(iMasterAttribute);
        builder.append('\n');

        builder.append("Attribute type: ");
        if (iMasterAttributeIsDensityValue) {
            builder.append("Density value");
        } else {
            builder.append("Population value");
        }
        builder.append('\n');

        builder.append("Transformation quality: ");
        if (iAdvancedOptionsEnabled) {
            builder.append("disabled");
        } else {
            builder.append(iAmountOfDeformation);
            builder.append(" of 100");
        }
        builder.append('\n');

        builder.append("Cartogram grid size: ");
        builder.append(iGridSize[0]);
        builder.append(" x ");
        builder.append(iGridSize[1]);
        builder.append("\n\n");

        builder.append("CARTOGRAM LAYER & ATTRIBUTE STATISTICS:\n");
        builder.append("Number of features: ");
        builder.append(iMasterLayer.getFeatures().size());
        builder.append('\n');

        builder.append("Attribute mean value: ");
        builder.append(iMasterLayer.meanValueForAttribute(iMasterAttribute));
        builder.append('\n');

        builder.append("Attribute minimum value: ");
        builder.append(iMasterLayer.minValueForAttribute(iMasterAttribute));
        builder.append('\n');

        builder.append("Attribute maximum value: ");
        builder.append(iMasterLayer.maxValueForAttribute(iMasterAttribute));
        builder.append("\n\n");

        builder.append("SIMULTANEOUSLY TRANSFORMED LAYERS:\n");
        if (aSimultaneousLayers == null || aSimultaneousLayers.size() == 0) {
            builder.append("None\n\n");
        } else {
            for (CartogramLayer layer : aSimultaneousLayers) {
                builder.append(layer.getName());
                builder.append('\n');
            }
            builder.append('\n');
        }

        builder.append("CONSTRAINED DEFORMATION LAYERS:\n");
        if (aConstrainedDeformationLayers == null
                || aConstrainedDeformationLayers.size() == 0) {
            builder.append("None\n\n");
        } else {
            for (CartogramLayer layer : aConstrainedDeformationLayers) {
                builder.append(layer.getName());
                builder.append('\n');
            }
            builder.append('\n');
        }

        // Compute the cartogram error
        double meanError = aProjectedMasterLayer.computeCartogramSizeError(
                iMasterAttribute, iMasterLayer, "SizeError");

        builder.append("CARTOGRAM ERROR\n");
        builder.append("The cartogram error is a measure for the quality of the result.\n");
        builder.append("Mean cartogram error: ");
        builder.append(meanError);
        builder.append('\n');

        double stdDev = aProjectedMasterLayer
                .standardDeviationForAttribute("SizeError");
        builder.append("Standard deviation: ");
        builder.append(stdDev);
        builder.append('\n');

        builder.append("25th percentile: ");
        builder.append(aProjectedMasterLayer.percentileForAttribute(
                "SizeError", 25));
        builder.append('\n');

        builder.append("50th percentile: ");
        builder.append(aProjectedMasterLayer.percentileForAttribute(
                "SizeError", 50));
        builder.append('\n');

        builder.append("75th percentile: ");
        builder.append(aProjectedMasterLayer.percentileForAttribute(
                "SizeError", 75));
        builder.append('\n');

        // Compute the number of features between the 25th and 75th
        // percentile and the percentage.
        List<CartogramFeature> features = aProjectedMasterLayer.getFeatures();
        int nFeaturesInStdDev = 0;
        int nFeatures = features.size();
        double value;
        for (CartogramFeature feature : features) {
            value = feature.getAttributeAsDouble("SizeError");

            if (value >= meanError - stdDev && value <= meanError + stdDev) {
                nFeaturesInStdDev++;
            }
        }

        builder.append("Features with mean error +/- 1 standard deviation: ");
        builder.append(nFeaturesInStdDev);
        builder.append(" of ");
        builder.append(nFeatures);
        builder.append(" (");
        builder.append((int) Math.round((double) nFeaturesInStdDev
                / (double) nFeatures * 100));
        builder.append("%)\n\n");

        builder.append("Computation time: ");
        builder.append((System.nanoTime() - iComputationStartTime) / 1000000000);
        builder.append(" seconds\n");

        iComputationReport = builder.toString();
    }

    /**
     * @return the computation report
     */
    public String getComputationReport() {
        return iComputationReport;
    }

    /**
     * Tries to estimate the maximum segment length allowed for a geometry. The
     * length is estimated using the envelope of the master layer and the number
     * of features present in the master layer. The area of the envelope is
     * considered as a square. The length of the square's edge is divided by the
     * square root of the number of features. This gives us an estimate of the
     * number of features along the square's edge. It is further considered that
     * there should be about 10 vertices for one feature along the square's
     * edge.
     * 
     * @return the estimated maximum segment length
     */
    private double estimateMaximumSegmentLength() {
        // Check the input variables. Otherwise, return a default value.
        if (iEnvelope == null) {
            return 500.0;
        }
        if (iMasterLayer == null) {
            return 500.0;
        }
        double envArea = iEnvelope.getWidth() * iEnvelope.getHeight();
        if (envArea <= 0.0) {
            return 500.0;
        }

        // Compute the edge length of the square having the same area as
        // the cartogram envelope.
        // Compute the length per feature.
        // 1/10 of the length per feature is our estimate for the maximum
        // segment length.
        return Math.sqrt(envArea)
                / Math.sqrt(iMasterLayer.getFeatures().size()) / 10;
    }

    /**
     * @param aValue
     *            the missing value
     */
    public void setMissingValue(String aValue) {
        iMissingValue = aValue;
    }

    /**
     * Returns the cartogram deformation grid.
     * 
     * @return the deformation grid
     */
    public CartogramLayer getDeformationGrid() {
        return iDeformationGrid;
    }

    /**
     * Returns the cartogram legend layer.
     * 
     * @return the legend layer
     */
    public CartogramLayer getLegendLayer() {
        return iLegendLayer;
    }
}