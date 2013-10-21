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

package ch.epfl.scapetoad;

import java.awt.Color;
import java.awt.Font;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.LabelStyle;

/**
 * The cartogram class is the main computation class. It is a subclass of the
 * SwingWorker class. It has methods for setting all the parameters and for
 * launching the computation.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-11-30
 */
public class Cartogram extends com.sun.swing.SwingWorker {

    /**
     * The logger
     */
    private static Log logger = LogFactory.getLog(Cartogram.class);

    /**
     * The cartogram wizard. We need the wizard reference for updating the
     * progress status informations.
     */
    private CartogramWizard iCartogramWizard = null;

    /**
     * The layer manager used for cartogram computation.
     */
    private LayerManager iLayerManager = null;

    /**
     * The category name for our cartogram layers.
     */
    private String iCategoryName = null;

    /**
     * The name of the master layer.
     */
    private String iMasterLayer = null;

    /**
     * The name of the master attribute.
     */
    private String iMasterAttribute = null;

    /**
     * Is the master attribute already a density value, or must the value be
     * weighted by the polygon area (only available for polygons).
     */
    private boolean iMasterAttributeIsDensityValue = true;

    /**
     * 
     */
    private String iMissingValue = "";

    /**
     * The projected master layer. We store this in order to make the
     * computation report after the projection.
     */
    private Layer iProjectedMasterLayer = null;

    /**
     * The layers to deform simultaneously.
     */
    private Vector<Layer> iSlaveLayers = null;

    /**
     * The layers used for the constrained deformation.
     */
    private Vector<Layer> iConstrainedDeforamtionLayers = null;

    /**
     * The initial envelope for all layers.
     */
    private Envelope iEnvelope = new Envelope(0.0, 1.0, 0.0, 1.0);

    /**
     * The X size of the cartogram grid.
     */
    private int iGridSizeX = 1000;

    /**
     * The X size of the cartogram grid.
     */
    private int iGridSizeY = 1000;

    /**
     * All the deformation is done on this cartogram grid.
     */
    private CartogramGrid iGrid = null;

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
    private boolean iAdvancedOptionsEnabled = false;

    /**
     * The maximum length of one line segment. In the projection process, a
     * straight line might be deformed to a curve. If a line segment is too
     * long, it might result in a self intersection, especially for polygons.
     * This parameter can be controlled manually or estimated using the
     * maximumSegmentLength heuristic.
     */
    private double iMaximumSegmentLength = 500;

    /**
     * Should we create a grid layer ?
     */
    private boolean iCreateGridLayer = true;

    /**
     * The size of the grid which can be added as a deformation grid.
     */
    private int iGridLayerSize = 100;

    /**
     * The layer containing the deformation grid.
     */
    private Layer iDeformationGrid = null;

    /**
     * Should we create a legend layer ?
     */
    private boolean iCreateLegendLayer = true;

    /**
     * An array containing the legend values which should be represented in the
     * legend layer.
     */
    private double[] iLegendValues = null;

    /**
     * The layer containing the cartogram legend.
     */
    private Layer mLegendLayer = null;

    /**
     * The computation report.
     */
    private String iComputationReport = "";

    /**
     * Used for storing the start time of computation. The computation duration
     * is computed based on this value which is set before starting the
     * compuation.
     */
    private long iComputationStartTime = 0;

    /**
     * 
     */
    private boolean iErrorOccured = false;

    /**
     * The constructor for the cartogram class.
     * 
     * @param aCartogramWizard
     *            the cartogram wizard
     */
    public Cartogram(CartogramWizard aCartogramWizard) {
        // Storing the cartogram wizard reference.
        iCartogramWizard = aCartogramWizard;
    }

    /**
     * The construct method is an overridden method from SwingWorker which does
     * initiate the computation process.
     */
    @Override
    public Object construct() {
        try {
            iComputationStartTime = System.nanoTime();

            if (iAdvancedOptionsEnabled == false) {
                // Automatic estimation of the parameters using the amount of
                // deformation slider.
                // The deformation slider modifies only the grid size between
                // 500 (quality 0) and 3000 (quality 100).
                iGridSizeX = iAmountOfDeformation * 15 + 250;
                iGridSizeY = iGridSizeX;
            }

            // User information.
            iCartogramWizard.updateRunningStatus(0,
                    "Preparing the cartogram computation...",
                    "Computing the cartogram bounding box");

            // Compute the envelope given the initial layers.
            // The envelope will be somewhat larger than just the layers.
            updateEnvelope();

            // Adjust the cartogram grid size in order to be proportional
            // to the envelope.
            adjustGridSizeToEnvelope();
            logger.debug(String.format("Adjusted grid size: %1$sx%2$s",
                    iGridSizeX, iGridSizeY));

            iCartogramWizard.updateRunningStatus(20,
                    "Preparing the cartogram computation...",
                    "Creating the cartogram grid");

            // Create the cartogram grid.
            iGrid = new CartogramGrid(iGridSizeX, iGridSizeY, iEnvelope);

            if (Thread.interrupted()) {
                // Raise an InterruptedException.
                throw new InterruptedException(
                        "Computation has been interrupted by the user.");
            }

            // Check the master attribute for invalid values.

            iCartogramWizard.updateRunningStatus(50,
                    "Check the cartogram attribute values...", "");

            Layer masterLayer = AppContext.layerManager.getLayer(iMasterLayer);
            CartogramLayer.cleanAttributeValues(masterLayer, iMasterAttribute);

            // Replace the missing values with the layer mean value.
            if (iMissingValue != "" && iMissingValue != null) {
                double mean = CartogramLayer.meanValueForAttribute(masterLayer,
                        iMasterAttribute);

                Double missVal = new Double(iMissingValue);

                CartogramLayer.replaceAttributeValue(masterLayer,
                        iMasterAttribute, missVal.doubleValue(), mean);
            }

            // Compute the density values for the cartogram grid using
            // the master layer and the master attribute.

            iCartogramWizard.updateRunningStatus(100,
                    "Computing the density for the cartogram grid...", "");

            iGrid.computeOriginalDensityValuesWithLayer(masterLayer,
                    iMasterAttribute, iMasterAttributeIsDensityValue);

            if (Thread.interrupted()) {
                // Raise an InterruptedException.
                throw new InterruptedException(
                        "Computation has been interrupted by the user.");
            }

            // *** PREPARE THE GRID FOR THE CONSTRAINED DEFORMATION ***

            if (iConstrainedDeforamtionLayers != null) {
                iCartogramWizard.updateRunningStatus(300,
                        "Prepare constrained deformation...", "");

                iGrid.prepareGridForConstrainedDeformation(iConstrainedDeforamtionLayers);
            }

            if (Thread.interrupted()) {
                // Raise an InterruptedException.
                throw new InterruptedException(
                        "Computation has been interrupted by the user.");
            }

            // *** COMPUTE THE CARTOGRAM USING THE DIFFUSION ALGORITHM ***

            iCartogramWizard.updateRunningStatus(350,
                    "Computing cartogram diffusion...",
                    "Starting the diffusion process");
            CartogramNewman cnewm = new CartogramNewman(iGrid);

            // Enable the CartogramNewman instance to update the running status.
            cnewm.iRunningStatusWizard = iCartogramWizard;
            cnewm.iRunningStatusMinimumValue = 350;
            cnewm.iRunningStatusMaximumValue = 700;
            cnewm.iRunningStatusMainString = "Computing cartogram diffusion...";

            // Let's go!
            cnewm.compute();

            if (Thread.interrupted()) {
                // Raise an InterruptedException.
                throw new InterruptedException(
                        "Computation has been interrupted by the user.");
            }

            // *** CONSTRAINED DEFORMATION ***
            if (iConstrainedDeforamtionLayers != null) {
                iCartogramWizard.updateRunningStatus(700,
                        "Applying the constrained deformation layers", "");

                iGrid.conformToConstrainedDeformation();
            }

            if (Thread.interrupted()) {
                // Raise an InterruptedException.
                throw new InterruptedException(
                        "Computation has been interrupted by the user.");
            }

            // *** PROJECTION OF ALL LAYERS ***

            iCartogramWizard.updateRunningStatus(750,
                    "Projecting the layers...", "");

            Layer[] projLayers = projectLayers();

            if (Thread.interrupted()) {
                // Raise an InterruptedException.
                throw new InterruptedException(
                        "Computation has been interrupted by the user.");
            }

            // *** CREATE THE DEFORMATION GRID LAYER ***
            if (iCreateGridLayer) {
                createGridLayer();
            }

            // *** CREATE THE LEGEND LAYER ***
            if (iCreateLegendLayer) {
                createLegendLayer();
            }

            iCartogramWizard.updateRunningStatus(950,
                    "Producing the computation report...", "");

            return projLayers;
        } catch (Exception exception) {
            logger.error("", exception);
            String exceptionType = exception.getClass().getName();

            if (exceptionType == "java.lang.InterruptedException") {
                iCartogramWizard
                        .setComputationError(
                                "The cartogram computation has been cancelled.",
                                "", "");
                iErrorOccured = true;
            } else if (exceptionType == "java.util.zip.DataFormatException") {
                // Retrieve the complete stack trace and display.
                iCartogramWizard
                        .setComputationError(
                                "An error occured during cartogram computation!",
                                "All attribute values are zero",
                                exception.getMessage());
                iErrorOccured = true;
            }

            iCartogramWizard.goToFinishedPanel();
            return null;
        }
    }

    /**
     * This method is called once the construct method has finished. It
     * terminates the computation, adds all layers and produces the computation
     * report.
     */
    @Override
    public void finished() {
        // If there was an error, stop here.
        if (iErrorOccured) {
            return;
        }

        // *** GET THE PROJECTED LAYERS ***

        Layer[] lyr = (Layer[]) get();

        if (lyr == null) {
            iCartogramWizard
                    .setComputationError(
                            "An error occured during cartogram computation!",
                            "",
                            "An unknown error has occured.\n\nThere may be unsufficient memory resources available. Try to:\n\n1.\tUse a smaller cartogram grid (through the transformation\n\tquality slider at the wizard step 5, or through the\n\t\"Advanced options...\" button, also at step 5.\n\n2.\tYou also may to want to increase the memory available\n\tto ScapeToad. To do so, you need the cross platform\n\tJAR file and \n\tlaunch ScapeToad from the command\n\tline, using the -Xmx flag of \n\tyour Java Virtual\n\tMachine. By default, ScapeToad has 1024 Mo of memory.\n\tDepending on your system, there may be less available.\n\n3.\tIf you think there is a bug in ScapeToad, you can file\n\ta bug \n\ton Sourceforge \n\t(http://sourceforge.net/projects/scapetoad).\n\tPlease describe in detail your problem and provide all\n\tnecessary \n\tdata for reproducing your error.\n\n");
            iCartogramWizard.goToFinishedPanel();
            return;
        }

        // *** HIDE ALL LAYERS ALREADY PRESENT ***
        @SuppressWarnings("unchecked")
        List<Layer> layerList = iLayerManager.getLayers();
        Iterator<Layer> layerIter = layerList.iterator();
        while (layerIter.hasNext()) {
            Layer l = layerIter.next();
            l.setVisible(false);
        }

        // *** ADD ALL THE LAYERS ***

        String catName = getCategoryName();

        if (iLayerManager.getCategory(catName) == null) {
            iLayerManager.addCategory(catName);
        }

        int nlyrs = lyr.length;
        for (int lyrcnt = 0; lyrcnt < nlyrs; lyrcnt++) {
            iLayerManager.addLayer(catName, lyr[lyrcnt]);
        }

        if (iDeformationGrid != null) {
            iLayerManager.addLayer(catName, iDeformationGrid);
        }

        if (mLegendLayer != null) {
            iLayerManager.addLayer(catName, mLegendLayer);
        }

        // *** PRODUCE THE COMPUTATION REPORT ***
        produceComputationReport(iProjectedMasterLayer);

        // *** CREATE A THEMATIC MAP USING THE SIZE ERROR ATTRIBUTE ***

        // Create a color table for the size error attribute.

        BasicStyle bs = (BasicStyle) iProjectedMasterLayer
                .getStyle(BasicStyle.class);
        bs.setFillColor(Color.WHITE);

        SizeErrorStyle errorStyle = new SizeErrorStyle();

        errorStyle.setAttributeName("SizeError");

        errorStyle.addColor(new BasicStyle(new Color(91, 80, 153)));
        errorStyle.addColor(new BasicStyle(new Color(133, 122, 179)));
        errorStyle.addColor(new BasicStyle(new Color(177, 170, 208)));
        errorStyle.addColor(new BasicStyle(new Color(222, 218, 236)));
        errorStyle.addColor(new BasicStyle(new Color(250, 207, 187)));
        errorStyle.addColor(new BasicStyle(new Color(242, 153, 121)));
        errorStyle.addColor(new BasicStyle(new Color(233, 95, 64)));

        errorStyle.addLimit(new Double(70));
        errorStyle.addLimit(new Double(80));
        errorStyle.addLimit(new Double(90));
        errorStyle.addLimit(new Double(100));
        errorStyle.addLimit(new Double(110));
        errorStyle.addLimit(new Double(120));

        lyr[0].addStyle(errorStyle);
        errorStyle.setEnabled(true);
        lyr[0].getStyle(BasicStyle.class).setEnabled(false);

        new SizeErrorLegend().setVisible(true);

        try {
            AppContext.layerViewPanel.getViewport().zoomToFullExtent();
        } catch (Exception exception) {
            logger.error("", exception);
        }

        // *** SHOW THE FINISHED PANEL
        iCartogramWizard.goToFinishedPanel();
    }

    /**
     * Sets the layer manager.
     * 
     * @param aLayerManager
     *            the layer manager
     */
    public void setLayerManager(LayerManager aLayerManager) {
        iLayerManager = aLayerManager;
    }

    /**
     * Sets the name of the cartogram master layer.
     * 
     * @param aLayerName
     *            the master layer name
     */
    public void setMasterLayer(String aLayerName) {
        iMasterLayer = aLayerName;
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
    public void setSlaveLayers(Vector<Layer> aSlaveLayers) {
        iSlaveLayers = aSlaveLayers;
    }

    /**
     * Defines the layers which should not be deformed.
     * 
     * @param aLayers
     *            the layers not deformed
     */
    public void setConstrainedDeformationLayers(Vector<Layer> aLayers) {
        iConstrainedDeforamtionLayers = aLayers;
    }

    /**
     * Defines the grid size in x and y dimensions.
     * 
     * @param aX
     *            the X grid size
     * @param aY
     *            the Y grid size
     */
    public void setGridSize(int aX, int aY) {
        iGridSizeX = aX;
        iGridSizeY = aY;
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
        Envelope envelope = iLayerManager.getLayer(iMasterLayer)
                .getFeatureCollectionWrapper().getEnvelope();
        iEnvelope = new Envelope(envelope.getMinX(), envelope.getMaxX(),
                envelope.getMinY(), envelope.getMaxY());

        // Expanding the initial envelope using the slave and
        // constrained deformation layers.
        if (iSlaveLayers != null) {
            for (Layer layer : iSlaveLayers) {
                iEnvelope.expandToInclude(layer.getFeatureCollectionWrapper()
                        .getEnvelope());
            }
        }

        if (iConstrainedDeforamtionLayers != null) {
            for (Layer layer : iConstrainedDeforamtionLayers) {
                iEnvelope.expandToInclude(layer.getFeatureCollectionWrapper()
                        .getEnvelope());
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
            iGridSizeX = (int) Math.round(iGridSizeY * (width / height));
        } else if (width > height) {
            // Adjust the y grid size.
            iGridSizeY = (int) Math.round(iGridSizeX * (height / width));
        }
    }

    /**
     * Projects all layers. Creates a new layer for each projected layer.
     * 
     * @return the project layers
     * @throws InterruptedException
     *             when the computation was interrupted
     */
    private Layer[] projectLayers() throws InterruptedException {
        // Get the number of layers to project
        // (one master layer and all slave layers).
        int nlyrs = 1;
        if (iSlaveLayers != null) {
            nlyrs += iSlaveLayers.size();
        }

        // We store the projected layers in an array.
        Layer[] layers = new Layer[nlyrs];

        // Compute the maximum segment length for the layers.
        iMaximumSegmentLength = estimateMaximumSegmentLength();

        // Project the master layer.

        iCartogramWizard.updateRunningStatus(750, "Projecting the layers...",
                "Layer 1 of " + nlyrs);

        Layer masterLayer = iLayerManager.getLayer(iMasterLayer);
        CartogramLayer.regularizeLayer(masterLayer, iMaximumSegmentLength);
        iProjectedMasterLayer = CartogramLayer.projectLayerWithGrid(
                masterLayer, iGrid);

        layers[0] = iProjectedMasterLayer;

        if (Thread.interrupted()) {
            // Raise an InterruptedException.
            throw new InterruptedException(
                    "Computation has been interrupted by the user.");
        }

        // Project the slave layers.
        for (int lyrcnt = 0; lyrcnt < nlyrs - 1; lyrcnt++) {
            iCartogramWizard.updateRunningStatus(800 + (lyrcnt + 1)
                    / (nlyrs - 1) * 150, "Projecting the layers...", "Layer "
                    + (lyrcnt + 2) + " of " + nlyrs);

            Layer slaveLayer = iSlaveLayers.get(lyrcnt);
            CartogramLayer.regularizeLayer(slaveLayer, iMaximumSegmentLength);
            layers[lyrcnt + 1] = CartogramLayer.projectLayerWithGrid(
                    slaveLayer, iGrid);
        }

        return layers;
    }

    /**
     * Sets the flag for creating or not a grid layer.
     * 
     * @param aCreateGridLayer
     *            <code>true</code> to create a grid layer
     */
    public void setCreateGridLayer(boolean aCreateGridLayer) {
        iCreateGridLayer = aCreateGridLayer;
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
     * Sets the flag which says whether to create a legend layer or not.
     * 
     * @param aCreateLegendLayer
     *            <code>true</code> to create the legend layer
     */
    public void setCreateLegendLayer(boolean aCreateLegendLayer) {
        iCreateLegendLayer = aCreateLegendLayer;
    }

    /**
     * @param aEnabled
     *            <code>true</code> to enable the advanced options
     */
    public void setAdvancedOptionsEnabled(boolean aEnabled) {
        iAdvancedOptionsEnabled = aEnabled;
    }

    /**
     * Returns the category name for our cartogram layers.
     * 
     * @return the category name
     */
    private String getCategoryName() {
        if (iCategoryName == null) {
            // Create a new category in the layer manager in order to
            // properly separate the cartogram layers. We call the new category
            // «Cartogram x», where x is a serial number.

            int catNumber = 1;
            String categoryName = "Cartogram " + catNumber;
            while (iLayerManager.getCategory(categoryName) != null) {
                catNumber++;
                categoryName = "Cartogram " + catNumber;
            }

            iCategoryName = categoryName;
        }

        return iCategoryName;
    }

    /**
     * Creates a layer with the deformation grid.
     */
    private void createGridLayer() {
        Envelope env = iEnvelope;

        // Compute the deformation grid size in x and y direction.

        double resolution = Math.max(env.getWidth() / (iGridLayerSize + 1),
                env.getHeight() / (iGridLayerSize + 1));

        int sizeX = (int) Math.round(Math.floor(env.getWidth() / resolution)) - 1;

        int sizeY = (int) Math.round(Math.floor(env.getHeight() / resolution)) - 1;

        // CREATE THE NEW LAYER

        // Create a new Feature Schema for the new layer.
        FeatureSchema fs = new FeatureSchema();
        fs.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        fs.addAttribute("ID", AttributeType.INTEGER);

        // Create a new empty Feature Dataset.
        FeatureDataset fd = new FeatureDataset(fs);

        // Create a Geometry Factory for creating the points.
        GeometryFactory gf = new GeometryFactory();

        // CREATE ALL FEATURES AND LINES
        int j, k;
        int i = 0;

        // Horizontal lines
        for (k = 0; k < sizeY; k++) {
            // Create an empty Feature.
            BasicFeature feat = new BasicFeature(fs);

            // Create the line string and add it to the Feature.
            Coordinate[] coords = new Coordinate[sizeX];
            for (j = 0; j < sizeX; j++) {
                double x = env.getMinX() + j * resolution;
                double y = env.getMinY() + k * resolution;
                coords[j] = iGrid.projectPointAsCoordinate(x, y);
            }

            LineString ls = null;
            ls = gf.createLineString(coords);

            if (ls != null) {
                feat.setGeometry(ls);

                // Add the other attributes.
                Integer idobj = new Integer(i);
                feat.setAttribute("ID", idobj);
                i++;

                // Add Feature to the Feature Dataset.
                fd.add(feat);
            }
        }

        // Vertical lines
        for (j = 0; j < sizeX; j++) {
            // Create an empty Feature.
            BasicFeature feat = new BasicFeature(fs);

            // Create the line string and add it to the Feature.
            Coordinate[] coords = new Coordinate[sizeY];
            for (k = 0; k < sizeY; k++) {
                double x = env.getMinX() + j * resolution;
                double y = env.getMinY() + k * resolution;
                coords[k] = iGrid.projectPointAsCoordinate(x, y);
            }

            LineString ls = null;
            ls = gf.createLineString(coords);

            if (ls != null) {
                feat.setGeometry(ls);

                // Add the other attributes.
                Integer idobj = new Integer(i);
                feat.setAttribute("ID", idobj);
                i++;

                // Add Feature to the Feature Dataset.
                fd.add(feat);
            }
        }

        // Create the layer.
        iDeformationGrid = new Layer("Deformation grid", Color.GRAY, fd,
                iLayerManager);
    }

    /**
     * Creates an optional legend layer.
     */
    private void createLegendLayer() {
        // The master layer.
        Layer masterLayer = iLayerManager.getLayer(iMasterLayer);

        double distanceBetweenSymbols = masterLayer
                .getFeatureCollectionWrapper().getEnvelope().getWidth() / 10;

        // Estimate legend values if there are none.

        double attrMax = CartogramLayer.maxValueForAttribute(masterLayer,
                iMasterAttribute);

        if (iLegendValues == null) {
            int nvalues = 3;

            double maxLog = Math.floor(Math.log10(attrMax));
            double maxValue = Math.pow(10, maxLog);
            double secondValue = Math.pow(10, maxLog - 1);

            iLegendValues = new double[nvalues];
            iLegendValues[0] = secondValue;
            iLegendValues[1] = maxValue;
            iLegendValues[2] = attrMax;
        }

        // CREATE THE NEW LAYER

        // Create a new Feature Schema for the new layer.
        FeatureSchema fs = new FeatureSchema();
        fs.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        fs.addAttribute("ID", AttributeType.INTEGER);
        fs.addAttribute("VALUE", AttributeType.DOUBLE);
        fs.addAttribute("AREA", AttributeType.DOUBLE);
        fs.addAttribute("COMMENT", AttributeType.STRING);

        // Create a new empty Feature Dataset.
        FeatureDataset fd = new FeatureDataset(fs);

        // Create a Geometry Factory for creating the points.
        GeometryFactory gf = new GeometryFactory();

        // CREATE THE FEATURES FOR THE LEGEND LAYER.
        int nvals = iLegendValues.length;
        double totalArea = CartogramLayer.totalArea(masterLayer);
        double valuesSum = CartogramLayer.sumForAttribute(masterLayer,
                iMasterAttribute);
        double x = iEnvelope.getMinX();
        double y = iEnvelope.getMinY();
        int id = 1;
        int valcnt;
        for (valcnt = 0; valcnt < nvals; valcnt++) {
            double valsize = totalArea / valuesSum * iLegendValues[valcnt];
            double rectsize = Math.sqrt(valsize);

            // Create the coordinate points.
            Coordinate[] coords = new Coordinate[5];
            coords[0] = new Coordinate(x, y);
            coords[1] = new Coordinate(x + rectsize, y);
            coords[2] = new Coordinate(x + rectsize, y - rectsize);
            coords[3] = new Coordinate(x, y - rectsize);
            coords[4] = new Coordinate(x, y);

            // Create geometry.
            LinearRing lr = gf.createLinearRing(coords);
            Polygon poly = gf.createPolygon(lr, null);

            // Create the Feature.
            BasicFeature feat = new BasicFeature(fs);
            feat.setAttribute("GEOMETRY", poly);
            feat.setAttribute("ID", id);
            feat.setAttribute("VALUE", iLegendValues[valcnt]);
            feat.setAttribute("AREA", valsize);

            if (valcnt == 0) {
                feat.setAttribute("COMMENT", "Mean value");
            } else if (valcnt == 1) {
                feat.setAttribute("COMMENT", "Rounded value of maximum ("
                        + attrMax + ")");
            }

            // Add the Feature to the Dataset.
            fd.add(feat);

            // Change the coordinates.
            x += rectsize + distanceBetweenSymbols;

            id++;
        }

        // Create the layer.
        iLayerManager.setFiringEvents(false);
        mLegendLayer = new Layer("Legend", Color.GREEN, fd, iLayerManager);
        LabelStyle legendLabels = mLegendLayer.getLabelStyle();
        legendLabels.setAttribute("VALUE");
        legendLabels.setEnabled(true);
        legendLabels.setFont(new Font(null, Font.PLAIN, 10));
        iLayerManager.setFiringEvents(true);
    }

    /**
     * Creates the computation report and stores it in the object attribute.
     * 
     * @param aProjectedMasterLayer
     *            the projected master layer
     */
    private void produceComputationReport(Layer aProjectedMasterLayer) {
        StringBuffer rep = new StringBuffer();

        rep.append("CARTOGRAM COMPUTATION REPORT\n\n");

        rep.append("CARTOGRAM PARAMETERS:\n");
        rep.append("Cartogram layer: " + iMasterLayer + "\n");
        rep.append("Cartogram attribute: " + iMasterAttribute + "\n");

        String attrType = "Population value";
        if (iMasterAttributeIsDensityValue) {
            attrType = "Density value";
        }
        rep.append("Attribute type: " + attrType + "\n");

        String transformationQuality = "";
        if (iAdvancedOptionsEnabled) {
            transformationQuality = "disabled";
        } else {
            transformationQuality = "" + iAmountOfDeformation + " of 100";
        }
        rep.append("Transformation quality: " + transformationQuality + "\n");

        rep.append("Cartogram grid size: " + iGridSizeX + " x " + iGridSizeY
                + "\n");
        rep.append("\n");
        // rep.append("Diffusion grid size: "+ mDiffusionGridSize +"\n");
        // rep.append("Diffusion iterations: "+ mDiffusionIterations +"\n\n");

        rep.append("CARTOGRAM LAYER & ATTRIBUTE STATISTICS:\n");
        Layer masterLayer = iLayerManager.getLayer(iMasterLayer);
        int nfeat = masterLayer.getFeatureCollectionWrapper().getFeatures()
                .size();
        rep.append("Number of features: " + nfeat + "\n");

        double mean = CartogramLayer.meanValueForAttribute(masterLayer,
                iMasterAttribute);
        rep.append("Attribute mean value: " + mean + "\n");

        double min = CartogramLayer.minValueForAttribute(masterLayer,
                iMasterAttribute);
        rep.append("Attribute minimum value: " + min + "\n");

        double max = CartogramLayer.maxValueForAttribute(masterLayer,
                iMasterAttribute);
        rep.append("Attribute maximum value: " + max + "\n\n");

        rep.append("SIMULTANEOUSLY TRANSFORMED LAYERS:\n");
        Vector<Layer> simLayers = iCartogramWizard.getSimultaneousLayers();
        if (simLayers == null || simLayers.size() == 0) {
            rep.append("None\n\n");
        } else {
            Iterator<Layer> simLayerIter = simLayers.iterator();
            while (simLayerIter.hasNext()) {
                Layer lyr = simLayerIter.next();
                rep.append(lyr.getName() + "\n");
            }
            rep.append("\n");
        }

        rep.append("CONSTRAINED DEFORMATION LAYERS:\n");
        Vector<Layer> constLayers = iCartogramWizard
                .getConstrainedDeformationLayers();
        if (constLayers == null || constLayers.size() == 0) {
            rep.append("None\n\n");
        } else {
            Iterator<Layer> constLayerIter = constLayers.iterator();
            while (constLayerIter.hasNext()) {
                Layer lyr = constLayerIter.next();
                rep.append(lyr.getName() + "\n");
            }
            rep.append("\n");
        }

        // Compute the cartogram error.
        double meanError = CartogramLayer.computeCartogramSizeError(
                aProjectedMasterLayer, iMasterAttribute, masterLayer,
                "SizeError");

        rep.append("CARTOGRAM ERROR\n");
        rep.append("The cartogram error is a measure for the quality of the result.\n");
        rep.append("Mean cartogram error: " + meanError + "\n");

        double stdDev = CartogramLayer.standardDeviationForAttribute(
                aProjectedMasterLayer, "SizeError");
        rep.append("Standard deviation: " + stdDev + "\n");

        double pctl25 = CartogramLayer.percentileForAttribute(
                aProjectedMasterLayer, "SizeError", 25);
        rep.append("25th percentile: " + pctl25 + "\n");

        double pctl50 = CartogramLayer.percentileForAttribute(
                aProjectedMasterLayer, "SizeError", 50);
        rep.append("50th percentile: " + pctl50 + "\n");

        double pctl75 = CartogramLayer.percentileForAttribute(
                aProjectedMasterLayer, "SizeError", 75);
        rep.append("75th percentile: " + pctl75 + "\n");

        // Compute the number of features between the 25th and 75th
        // percentile and the percentage.

        FeatureCollectionWrapper fcw = aProjectedMasterLayer
                .getFeatureCollectionWrapper();

        @SuppressWarnings("unchecked")
        Iterator<Feature> featIter = fcw.iterator();
        int nFeaturesInStdDev = 0;
        int nFeatures = fcw.size();
        while (featIter.hasNext()) {
            Feature feat = featIter.next();

            double value = CartogramFeature.getAttributeAsDouble(feat,
                    "SizeError");

            if (value >= meanError - stdDev && value <= meanError + stdDev) {
                nFeaturesInStdDev++;
            }
        }

        double percFeaturesInStdDev = (double) nFeaturesInStdDev
                / (double) nFeatures * 100;

        int pfint = (int) Math.round(percFeaturesInStdDev);

        rep.append("Features with mean error +/- 1 standard deviation: "
                + nFeaturesInStdDev + " of " + nFeatures + " (" + pfint
                + "%)\n\n");

        long estimatedTime = System.nanoTime() - iComputationStartTime;
        estimatedTime /= 1000000000;
        rep.append("Computation time: " + estimatedTime + " seconds\n");

        iComputationReport = rep.toString();
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
    public double estimateMaximumSegmentLength() {
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
        Layer layer = iLayerManager.getLayer(iMasterLayer);
        if (layer == null) {
            return 500.0;
        }

        // Compute the edge length of the square having the same area as
        // the cartogram envelope.
        // Compute the length per feature.
        // 1/10 of the length per feature is our estimate for the maximum
        // segment length.
        return Math.sqrt(envArea)
                / Math.sqrt(layer.getFeatureCollectionWrapper().getFeatures()
                        .size()) / 10;
    }

    /**
     * @param aValue
     *            the missing value
     */
    public void setMissingValue(String aValue) {
        iMissingValue = aValue;
    }
}