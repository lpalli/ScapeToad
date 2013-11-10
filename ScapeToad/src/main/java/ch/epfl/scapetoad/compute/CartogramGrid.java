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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DataFormatException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * The cartogram grid class represents the grid which is overlaid on all the
 * layers and which is used for the deformation computation. The grid has nodes
 * and cells. Each node has x/y-coordinates, and each cell has a density value.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-11-30
 */
public class CartogramGrid {

    /**
     * The logger.
     */
    private static Log logger = LogFactory.getLog(Cartogram.class);

    /**
     * The grid size in x and y direction.
     */
    private int[] iGridSize = new int[2];

    /**
     * The real world initial envelope for the grid. The grid is constructed
     * upon this region.
     */
    private Envelope iEnvelope = null;

    /**
     * The X arrays for storing the nodes and the cells.
     */
    private double[][] iNodeX;

    /**
     * The Y arrays for storing the nodes and the cells.
     */
    private double[][] iNodeY;

    /**
     * The array of original density.
     */
    private double[][] iCellOriginalDensity;

    /**
     * The array of current density.
     */
    private double[][] iCellCurrentDensity;

    /**
     * The array of deformation.
     */
    private short[][] iCellConstrainedDeformation;

    /**
     * The mean density is the optimal density for a cell.
     */
    private double iMeanDensity = -1.0;

    /**
     * The size of one cell in x and y direction. This is used for internal
     * purpose only. Do not modify these values directly.
     */
    private double[] iCellSize = new double[2];

    /**
     * The bias value is a small value bigger than 0 which is added to every
     * grid cell in order to avoid computation problems with 0 values.
     * Additionally, we will rescale all the values in order to have a minimum
     * value of 10 at least.
     */
    private double bias = 0.00001;

    /**
     * The constructor for the cartogram grid.
     * 
     * @param aGridSizeX
     *            the X grid size
     * @param aGridSizeY
     *            the Y grid size
     * @param aEnvelope
     *            the envelope
     */
    public CartogramGrid(int aGridSizeX, int aGridSizeY, Envelope aEnvelope) {
        // Store the attributes.
        iGridSize[0] = aGridSizeX;
        iGridSize[1] = aGridSizeY;
        iEnvelope = aEnvelope;

        // Allocate memory for the grid arrays.
        iNodeX = new double[aGridSizeX][aGridSizeY];
        iNodeY = new double[aGridSizeX][aGridSizeY];
        iCellOriginalDensity = new double[aGridSizeX - 1][aGridSizeY - 1];
        iCellCurrentDensity = new double[aGridSizeX - 1][aGridSizeY - 1];
        iCellConstrainedDeformation = new short[aGridSizeX - 1][aGridSizeY - 1];

        // Compute the node coordinates.
        computeNodeCoordinates();
    }

    /**
     * Returns the grid's bounding box.
     * 
     * @return an Envelope representing the bounding box.
     */
    public Envelope getEnvelope() {
        return iEnvelope;
    }

    /**
     * Returns the x coordinates array.
     * 
     * @return the X coordnates
     */
    public double[][] getXCoordinates() {
        return iNodeX;
    }

    /**
     * Returns the y coordinates array.
     * 
     * @return the Y coordinates
     */
    public double[][] getYCoordinates() {
        return iNodeY;
    }

    /**
     * Returns the array containing the current densities for the grid.
     * 
     * @return the densities
     */
    public double[][] getCurrentDensityArray() {
        return iCellCurrentDensity;
    }

    /**
     * Returns the cartogram grid size.
     * 
     * @return the grid size
     */
    public int[] getGridSize() {
        return iGridSize;
    }

    /**
     * Computes the node coordinates and fills them into the nodeX and nodeY
     * arrays.
     */
    private void computeNodeCoordinates() {
        // Verify the grid size.
        if (iGridSize[0] <= 0 || iGridSize[1] <= 0) {
            return;
        }

        // Compute the size of a cell in x and y.
        iCellSize[0] = iEnvelope.getWidth() / (iGridSize[0] - 1);
        iCellSize[1] = iEnvelope.getHeight() / (iGridSize[1] - 1);

        double x;
        double y = iEnvelope.getMinY();

        // Create all nodes.
        for (int j = 0; j < iGridSize[1]; j++) {
            x = iEnvelope.getMinX();

            for (int i = 0; i < iGridSize[0]; i++) {
                iNodeX[i][j] = x;
                iNodeY[i][j] = y;
                x += iCellSize[0];
            }

            y += iCellSize[1];
        }
    }

    /**
     * Computes the density value given a layer and an attribute name.
     * 
     * @param aLayer
     *            the master layer
     * @param aAttrName
     *            the name of the master attribute
     * @param aAttrIsDensityValue
     *            is true if the master attribute is a density value, and false
     *            if it is a population value.
     * @param aStatus
     *            the cartogram status
     * @throws InterruptedException
     *             when was interrupted
     * @throws DataFormatException
     *             when the data format is wrong
     */
    public void computeOriginalDensityValuesWithLayer(CartogramLayer aLayer,
            String aAttrName, boolean aAttrIsDensityValue,
            ICartogramStatus aStatus) throws InterruptedException,
            DataFormatException {
        // If the attribute is not a density value, we create a new
        // attribute for the computed density value.
        String densityAttrName = aAttrName;
        if (!aAttrIsDensityValue) {
            densityAttrName = aAttrName + "Density";
            aLayer.addDensityAttribute(aAttrName, densityAttrName);
        }

        // Compute the mean density.
        iMeanDensity = aLayer.meanDensityWithAttribute(densityAttrName);

        // For each Feature in the layer, we find all grid cells which are at
        // least in part inside the Feature. We add the density weighted by the
        // Feature's proportion of coverage of the cell. For this, we set to 0
        // all optimal density values. At the same time we set the current
        // density value to the mean density value and the value for constrained
        // deformation to 0.
        for (int j = 0; j < iGridSize[1] - 1; j++) {
            for (int i = 0; i < iGridSize[0] - 1; i++) {
                iCellCurrentDensity[i][j] = iMeanDensity;
                iCellOriginalDensity[i][j] = iMeanDensity;
                iCellConstrainedDeformation[i][j] = -1;
            }
        }

        int nFeat = aLayer.getFeatures().size();
        int featCnt = 0;
        for (CartogramFeature feature : aLayer.getFeatures()) {
            // Interrupt the process ?
            if (Thread.interrupted()) {
                // Raise an InterruptedException.
                throw new InterruptedException(
                        "Computation has been interrupted by the user.");
            }

            aStatus.updateRunningStatus(100 + featCnt * 100 / nFeat,
                    "Computing the density for the cartogram grid...",
                    "Treating feature %1$s of %2$s", featCnt + 1, nFeat);

            fillDensityValueWithFeature(feature, densityAttrName);

            featCnt++;
        }

        // Rescale and the bias value to every cell.
        rescaleValues();
        addBias();
    }

    /**
     * Rescales the density value to have a value of at least this.minValue for
     * all non-zero cells.
     */
    private void rescaleValues() {
        // Find out the smallest non-zero value in the grid.
        double min = Double.MAX_VALUE;
        for (int j = 0; j < iGridSize[1] - 1; j++) {
            for (int i = 0; i < iGridSize[0] - 1; i++) {
                if (iCellCurrentDensity[i][j] > 0.0
                        && iCellCurrentDensity[i][j] < min) {
                    min = iCellCurrentDensity[i][j];
                }
            }
        }

        // Compute the scaling factor
        if (min < Double.MAX_VALUE) {
            double factor = 10 / min;
            if (factor > 1) {
                for (int j = 0; j < iGridSize[1] - 1; j++) {
                    for (int i = 0; i < iGridSize[0] - 1; i++) {
                        iCellCurrentDensity[i][j] *= factor;
                    }
                }
            }
        }
    }

    /**
     * Adds the bias value to every grid cell.
     */
    private void addBias() {
        for (int j = 0; j < iGridSize[1] - 1; j++) {
            for (int i = 0; i < iGridSize[0] - 1; i++) {
                iCellCurrentDensity[i][j] += bias;
            }
        }
    }

    /**
     * Prepares the original grid for a constrained deformation process. After
     * this preparation, the grid can be deformed using a cartogram algorithm.
     * After this deformation, the grid can be corrected using the constrained
     * deformation information by a call to the CartogramGrid method
     * conformToConstrainedDeformation.
     * 
     * @param aLayers
     *            a Vector containing the constrained layer names.
     */
    public void prepareGridForConstrainedDeformation(
            List<CartogramLayer> aLayers) {
        if (aLayers == null) {
            return;
        }

        // For all cells containing a constrained feature and no deformation
        // feature, we set the constrained cell value to 1.
        // The cell values of 0 are for deformation cells, and -1 for
        // empty cells.
        for (CartogramLayer layer : aLayers) {
            for (CartogramFeature feature : layer.getFeatures()) {
                prepareGridForConstrainedDeformationWithFeature(feature);
            }
        }
    }

    /**
     * Prepares the grid for constrained deformation using the provided feature.
     * 
     * @param aFeature
     *            the feature
     */
    private void prepareGridForConstrainedDeformationWithFeature(
            CartogramFeature aFeature) {
        // Extract the minimum and maximum coordinates from the Feature
        Geometry geometry = aFeature.getGeometry();
        Envelope envelope = geometry.getEnvelopeInternal();

        // Find the maximum cell indexes for this Feature
        int maxI = originalCellIndexForCoordinateX(envelope.getMaxX());
        int maxJ = originalCellIndexForCoordinateY(envelope.getMaxY());

        // Create a new Geometry Factory
        // We need to create a new geometry with the cell in order to know
        // whether the cell intersects with the feature
        GeometryFactory factory = new GeometryFactory();

        Geometry cellEnvGeom;
        Envelope cellEnv;
        double minX;
        double minY;
        for (int j = originalCellIndexForCoordinateY(envelope.getMinY()); j <= maxJ; j++) {
            for (int i = originalCellIndexForCoordinateX(envelope.getMinX()); i <= maxI; i++) {
                // We treat this cell only if it does not intersect with a
                // deformation feature or if it is already a constrained
                // deformation cell
                if (iCellConstrainedDeformation[i][j] == -1) {
                    minX = coordinateXForOriginalCellIndex(i);
                    minY = coordinateYForOriginalCellIndex(j);

                    cellEnv = new Envelope(minX, minX + iCellSize[0], minY,
                            minY + iCellSize[1]);
                    cellEnvGeom = factory.toGeometry(cellEnv);
                    if (geometry.contains(cellEnvGeom)
                            || geometry.intersects(cellEnvGeom)) {
                        iCellConstrainedDeformation[i][j] = 1;
                    }
                }
            }
        }
    }

    /**
     * Updates the optimal density value for the grid cells inside the provided
     * Feature.
     * 
     * @param aFeature
     *            the CartoramFeature which serves as update source.
     * @param aDensityAttribute
     *            the name of the attribute containing the density value for the
     *            Feature.
     */
    private void fillDensityValueWithFeature(CartogramFeature aFeature,
            String aDensityAttribute) {
        // Extract the minimum and maximum coordinates from the Feature.
        Geometry geometry = aFeature.getGeometry();
        Envelope envelope = aFeature.getGeometry().getEnvelopeInternal();

        // Get the density attribute value.
        double densityValue = aFeature.getAttributeAsDouble(aDensityAttribute);

        // Find the maximum cell indexes for this Feature
        int maxI = originalCellIndexForCoordinateX(envelope.getMaxX());
        int maxJ = originalCellIndexForCoordinateY(envelope.getMaxY());

        // Create a new Geometry Factory.
        GeometryFactory factory = new GeometryFactory();

        for (int j = originalCellIndexForCoordinateY(envelope.getMinY()); j <= maxJ; j++) {
            for (int i = originalCellIndexForCoordinateX(envelope.getMinX()); i <= maxI; i++) {
                if (geometry
                        .contains(factory.createPoint(new Coordinate(
                                coordinateXForOriginalCellIndex(i)
                                        + iCellSize[0] / 2,
                                coordinateYForOriginalCellIndex(j)
                                        + iCellSize[1] / 2)))) {
                    iCellOriginalDensity[i][j] = densityValue;
                    iCellCurrentDensity[i][j] = densityValue;
                    iCellConstrainedDeformation[i][j] = 0;
                }
            }
        }
    }

    /**
     * Corrects the grid for corresponding the constrained deformation
     * information computed by the prepareGridForConstrainedDeformation method.
     */
    public void conformToConstrainedDeformation() {
        // Algorithm outline:
        // 1. Identify constrained cells.
        // 2. Is there a node which can move?
        // 3. If yes, where should this node go?
        // 4. Is this movement partially or completely feasible?
        // (no topologic problem)
        // 5. If yes, move point.

        boolean canMove;
        for (int j = 0; j < iGridSize[1] - 1; j++) {
            for (int i = 0; i < iGridSize[0] - 1; i++) {
                if (iCellConstrainedDeformation[i][j] == 1) {
                    // Can we move a node ?
                    canMove = false;

                    // If there is a corner, we can move.
                    if (i == 0 && j == 0 || i == 0 && j == iGridSize[1] - 2
                            || i == iGridSize[0] - 2 && j == 0
                            || i == iGridSize[0] - 2 && j == iGridSize[1] - 1) {
                        canMove = true;
                    }

                    // If the cell is on the border but not a corner,
                    // we can move depending on the neighbours.

                    else if (i == 0 || i == iGridSize[0] - 2) {
                        // Left or right border
                        if (iCellConstrainedDeformation[i][j + 1] != 0
                                || iCellConstrainedDeformation[i][j - 1] != 0) {
                            canMove = true;
                        }
                    }

                    else if (j == 0 || j == iGridSize[1] - 2) {
                        // Lower or upper border
                        if (iCellConstrainedDeformation[i - 1][j] != 0
                                || iCellConstrainedDeformation[i + 1][j] != 0) {
                            canMove = true;
                        }
                    }

                    // If there is an empty cell or a constrained cell
                    // in the neighbourhood, we can propably move (it
                    // depends on the exact configuration). We have to test
                    // for each node of the cell whether it can move or not.

                    if (i > 0 && j > 0 && i < iGridSize[0] - 2
                            && j < iGridSize[1] - 2) {
                        // Test upper left node.
                        if (iCellConstrainedDeformation[i - 1][j] != 0
                                && iCellConstrainedDeformation[i - 1][j + 1] != 0
                                && iCellConstrainedDeformation[i][j + 1] != 0) {
                            canMove = true;
                        }

                        // Test upper right node.
                        if (iCellConstrainedDeformation[i][j + 1] != 0
                                && iCellConstrainedDeformation[i + 1][j + 1] != 0
                                && iCellConstrainedDeformation[i + 1][j] != 0) {
                            canMove = true;
                        }

                        // Test lower left node.
                        if (iCellConstrainedDeformation[i - 1][j] != 0
                                && iCellConstrainedDeformation[i - 1][j - 1] != 0
                                && iCellConstrainedDeformation[i][j - 1] != 0) {
                            canMove = true;
                        }

                        // Test lower right node.
                        if (iCellConstrainedDeformation[i][j - 1] != 0
                                && iCellConstrainedDeformation[i + 1][j - 1] != 0
                                && iCellConstrainedDeformation[i + 1][j] != 0) {
                            canMove = true;
                        }
                    }

                    // Try to apply the constrained deformation to the node.
                    if (canMove) {
                        applyConstrainedDeformationToCell(i, j);
                    }
                }
            }
        }
    }

    /**
     * Tries to give the original form to the provided cell.
     * 
     * @param aI
     *            ???
     * @param aJ
     *            ???
     */
    private void applyConstrainedDeformationToCell(int aI, int aJ) {
        // Compute the location where each of the 4 nodes should go.

        // Compute the ideal x/y values for the cell.
        double minX = (iNodeX[aI][aJ + 1] + iNodeX[aI][aJ]) / 2;
        double maxX = (iNodeX[aI + 1][aJ + 1] + iNodeX[aI + 1][aJ]) / 2;
        double minY = (iNodeY[aI][aJ] + iNodeY[aI + 1][aJ]) / 2;
        double maxY = (iNodeY[aI][aJ + 1] + iNodeX[aI + 1][aJ + 1]) / 2;

        double edgeLength = Math.sqrt((maxX - minX) * (maxY - minY));

        double diffX = edgeLength - (maxX - minX);
        double diffY = edgeLength - (maxY - minY);

        minX -= diffX / 2;
        maxX += diffX / 2;
        minY -= diffY / 2;
        maxY += diffY / 2;

        // Try to move each of the 4 nodes to the new position.

        // Upper left node
        if (aI == 0 && aJ == iGridSize[1] - 2 || aI == 0
                && iCellConstrainedDeformation[aI][aJ + 1] != 0
                || aJ == iGridSize[1] - 2
                && iCellConstrainedDeformation[aI - 1][aJ] != 0
                || iCellConstrainedDeformation[aI - 1][aJ] != 0
                && iCellConstrainedDeformation[aI - 1][aJ + 1] != 0
                && iCellConstrainedDeformation[aI][aJ + 1] != 0) {
            tryToMoveNode(aI, aJ + 1, minX, maxY);
        }

        // Upper right node
        if (aI == iGridSize[0] - 2 && aJ == iGridSize[1] - 2
                || aI == iGridSize[0] - 2
                && iCellConstrainedDeformation[aI][aJ + 1] != 0
                || aJ == iGridSize[1] - 2
                && iCellConstrainedDeformation[aI + 1][aJ] != 0
                || iCellConstrainedDeformation[aI + 1][aJ] != 0
                && iCellConstrainedDeformation[aI + 1][aJ + 1] != 0
                && iCellConstrainedDeformation[aI][aJ + 1] != 0) {
            tryToMoveNode(aI + 1, aJ + 1, maxX, maxY);
        }

        // Lower right node
        if (aI == iGridSize[0] - 2 && aJ == 0 || aI == iGridSize[0] - 2
                && iCellConstrainedDeformation[aI][aJ - 1] != 0 || aJ == 0
                && iCellConstrainedDeformation[aI + 1][aJ] != 0
                || iCellConstrainedDeformation[aI + 1][aJ] != 0
                && iCellConstrainedDeformation[aI + 1][aJ - 1] != 0
                && iCellConstrainedDeformation[aI][aJ - 1] != 0) {
            tryToMoveNode(aI + 1, aJ, maxX, minY);
        }

        // Lower left node
        if (aI == 0 && aJ == 0 || aI == 0
                && iCellConstrainedDeformation[aI][aJ - 1] != 0 || aJ == 0
                && iCellConstrainedDeformation[aI - 1][aJ] != 0
                || iCellConstrainedDeformation[aI][aJ - 1] != 0
                && iCellConstrainedDeformation[aI - 1][aJ - 1] != 0
                && iCellConstrainedDeformation[aI - 1][aJ] != 0) {
            tryToMoveNode(aI, aJ, minX, minY);
        }
    }

    /**
     * Tries to move the provided node to the provided location. The decision to
     * move or not depends on the neighbourhood structure. The topology must be
     * respected in all cases.
     * 
     * @param aI
     *            ???
     * @param aJ
     *            ???
     * @param aX
     *            ???
     * @param aY
     *            ???
     */
    private void tryToMoveNode(int aI, int aJ, double aX, double aY) {
        double x = aX;
        double y = aY;

        // Create a polygon with the neighboring nodes.
        // If the new location is inside this polygon, we can potentially
        // move the node. However, we will insure that the point does not
        // move too far. There is a maximum distance which is 1/10 of the
        // original cell size.

        double moveDistance = Math.sqrt((iNodeX[aI][aJ] - x)
                * (iNodeX[aI][aJ] - x) + (iNodeY[aI][aJ] - y)
                * (iNodeY[aI][aJ] - y));

        // If the distance to move is too big, we compute a new, closer
        // location.
        if (moveDistance > iCellSize[0] / 10) {
            double newMoveDistance = iCellSize[0] / 10;

            double moveVectorX = x - iNodeX[aI][aJ];
            double moveVectorY = y - iNodeY[aI][aJ];

            double correctionFactor = newMoveDistance / moveDistance;

            x = iNodeX[aI][aJ] + correctionFactor * moveVectorX;
            y = iNodeY[aI][aJ] + correctionFactor * moveVectorY;
            moveDistance = newMoveDistance;
        }

        boolean canMove = true;

        if (aI > 0) {
            if (aJ < iGridSize[1] - 2 && iNodeX[aI - 1][aJ + 1] >= x) {
                canMove = false;
            }

            if (iNodeX[aI - 1][aJ] >= x) {
                canMove = false;
            }

            if (aJ > 0 && iNodeX[aI - 1][aJ - 1] >= x) {
                canMove = false;
            }
        }

        if (aI < iGridSize[0] - 2) {
            if (aJ < iGridSize[1] - 2 && iNodeX[aI + 1][aJ + 1] <= x) {
                canMove = false;
            }

            if (iNodeX[aI + 1][aJ] <= x) {
                canMove = false;
            }

            if (aJ > 0 && iNodeX[aI + 1][aJ - 1] <= x) {
                canMove = false;
            }
        }

        if (aJ > 0) {
            if (aI > 0 && iNodeY[aI - 1][aJ - 1] >= y) {
                canMove = false;
            }

            if (iNodeY[aI][aJ - 1] >= y) {
                canMove = false;
            }

            if (aI < iGridSize[0] - 2 && iNodeY[aI + 1][aJ - 1] >= y) {
                canMove = false;
            }
        }

        if (aJ < iGridSize[1] - 2) {
            if (aI > 0 && iNodeY[aI - 1][aJ + 1] <= y) {
                canMove = false;
            }

            if (iNodeY[aI][aJ + 1] <= y) {
                canMove = false;
            }

            if (aI < iGridSize[0] - 2 && iNodeY[aI + 1][aJ + 1] <= y) {
                canMove = false;
            }
        }

        if (canMove) {
            iNodeX[aI][aJ] = x;
            iNodeY[aI][aJ] = y;
        }
    }

    /**
     * Converts the provided x coordinate into the grid's cell index.
     * 
     * @param aX
     *            the real world x coordinate.
     * @return the cell index in x direction.
     */
    private int originalCellIndexForCoordinateX(double aX) {
        if (iEnvelope == null) {
            return -1;
        }

        if (aX == iEnvelope.getMinX()) {
            return 0;
        }

        return (int) Math.round(Math.ceil((aX - iEnvelope.getMinX())
                / iCellSize[0]) - 1);
    }

    /**
     * Converts the provided y coordinate into the grid's cell index.
     * 
     * @param aY
     *            the real world y coordinate.
     * @return the cell index in y direction.
     */
    private int originalCellIndexForCoordinateY(double aY) {
        if (iEnvelope == null) {
            return -1;
        }

        if (aY == iEnvelope.getMinY()) {
            return 0;
        }

        return (int) Math.round(Math.ceil((aY - iEnvelope.getMinY())
                / iCellSize[1]) - 1);
    }

    /**
     * Converts a grid cell index in x direction into real world x coordinate.
     * The coordinate of the cell's lower left corner is returned.
     * 
     * @param aI
     *            the cell index in x direction.
     * @return the x coordinate of the cell's lower left corner.
     */
    private double coordinateXForOriginalCellIndex(int aI) {
        if (iEnvelope == null) {
            return 0.0;
        }

        return iEnvelope.getMinX() + aI * iCellSize[0];
    }

    /**
     * Converts a grid cell index in y direction into real world y coordinate.
     * The coordinate of the cell's lower left corner is returned.
     * 
     * @param aJ
     *            the cell index in y direction.
     * @return the y coordinate of the cell's lower left corner.
     */
    private double coordinateYForOriginalCellIndex(int aJ) {
        if (iEnvelope == null) {
            return 0.0;
        }

        return iEnvelope.getMinY() + aJ * iCellSize[1];
    }

    /**
     * Fills a regular grid with the mean density. If there is no information,
     * the mean density for the whole grid is assumed to be the desired value.
     * 
     * @param aDensityGrid
     *            the density grid
     * @param aMinX
     *            the min X
     * @param aMaxX
     *            the max X
     * @param aMinY
     *            the min Y
     * @param aMaxY
     *            the max Y
     */
    public void fillRegularDensityGrid(double[][] aDensityGrid, double aMinX,
            double aMaxX, double aMinY, double aMaxY) {
        // Compute the grid size.
        int gridSizeX = aDensityGrid.length;
        int gridSizeY = aDensityGrid[0].length;

        // Compute the width, height and cell size of the density grid.
        double gridWidth = aMaxX - aMinX;
        double gridHeight = aMaxY - aMinY;
        double cellSizeX = gridWidth / gridSizeX;
        double cellSizeY = gridHeight / gridSizeY;

        // For each node at the lower left corner of a cell,
        // we compute the regular grid cells concerned by the
        // cartogram grid cell.

        // Initialize the counting grid and the density grid.
        short[][] cntgrid = new short[gridSizeX][gridSizeY];
        for (int i = 0; i < gridSizeX; i++) {
            for (int j = 0; j < gridSizeY; j++) {
                aDensityGrid[i][j] = 0;
                cntgrid[i][j] = 0;
            }
        }

        for (int i = 0; i < iGridSize[0] - 1; i++) {
            for (int j = 0; j < iGridSize[1] - 1; j++) {
                // Compute the cell index in which the node is located.

                int llx = (int) Math.round(Math.floor((iNodeX[i][j] - aMinX)
                        / cellSizeX));
                int lly = (int) Math.round(Math.floor((iNodeY[i][j] - aMinY)
                        / cellSizeY));

                int lrx = (int) Math.round(Math
                        .floor((iNodeX[i + 1][j] - aMinX) / cellSizeX));
                int lry = (int) Math.round(Math
                        .floor((iNodeY[i + 1][j] - aMinY) / cellSizeY));

                int urx = (int) Math.round(Math
                        .floor((iNodeX[i + 1][j + 1] - aMinX) / cellSizeX));
                int ury = (int) Math.round(Math
                        .floor((iNodeY[i + 1][j + 1] - aMinY) / cellSizeY));

                int ulx = (int) Math.round(Math
                        .floor((iNodeX[i][j + 1] - aMinX) / cellSizeX));
                int uly = (int) Math.round(Math
                        .floor((iNodeY[i][j + 1] - aMinY) / cellSizeY));

                int x, y;
                int minx = Math.max(Math.min(llx, ulx), 0);
                int maxx = Math.min(Math.max(lrx, urx), gridSizeX - 1);
                int miny = Math.max(Math.min(lly, lry), 0);
                int maxy = Math.min(Math.max(uly, ury), gridSizeY - 1);
                for (x = minx; x <= maxx; x++) {
                    for (y = miny; y <= maxy; y++) {
                        aDensityGrid[x][y] += iCellCurrentDensity[i][j];
                        cntgrid[x][y]++;
                    }
                }
            }
        }

        for (int i = 0; i < gridSizeX; i++) {
            for (int j = 0; j < gridSizeY; j++) {

                if (cntgrid[i][j] == 0) {
                    aDensityGrid[i][j] = iMeanDensity;
                } else {
                    aDensityGrid[i][j] /= cntgrid[i][j];
                }
            }
        }
    }

    /**
     * Projects one point using this grid.
     * 
     * @param aX
     *            the x coordinate of the point to project.
     * @param aY
     *            the y coordinate of the point to project.
     * @return a double array with the coordinates of the projected point.
     */
    public double[] projectPoint(double aX, double aY) {
        double p1x = (aX - iEnvelope.getMinX()) * iGridSize[0]
                / iEnvelope.getWidth();

        double p1y = (aY - iEnvelope.getMinY()) * iGridSize[1]
                / iEnvelope.getHeight();

        int i = (int) Math.round(Math.floor(p1x));
        int j = (int) Math.round(Math.floor(p1y));

        if (i < 0) {
            i = 0;
        }
        if (i >= iGridSize[0] - 1) {
            i = iGridSize[0] - 2;
        }
        if (j < 0) {
            j = 0;
        }
        if (j >= iGridSize[1] - 1) {
            j = iGridSize[1] - 2;
        }

        double ti = p1x - i;
        double tj = p1y - j;

        double ax = iNodeX[i][j];
        double ay = iNodeY[i][j];
        double bx = iNodeX[i + 1][j];
        double by = iNodeY[i + 1][j];
        double cx = iNodeX[i + 1][j + 1];
        double cy = iNodeY[i + 1][j + 1];
        double dx = iNodeX[i][j + 1];
        double dy = iNodeY[i][j + 1];

        return ch.epfl.scapetoad.compute.Geometry.intersectionOfSegments(ax
                + ti * (bx - ax), ay + ti * (by - ay), dx + ti * (cx - dx), dy
                + ti * (cy - dy), bx + tj * (cx - bx), by + tj * (cy - by), ax
                + tj * (dx - ax), ay + tj * (dy - ay));
    }

    /**
     * Projects one point using this grid.
     * 
     * @param aX
     *            the x coordinate of the point to project.
     * @param aY
     *            the y coordinate of the point to project.
     * @return a Coordinate with the projected point.
     */
    public Coordinate projectPointAsCoordinate(double aX, double aY) {
        double[] coord = projectPoint(aX, aY);
        return new Coordinate(coord[0], coord[1]);
    }

    /**
     * Projects a line segment. Returns two or more coordinates.
     * 
     * @param aCoordinate1
     *            the first coordinate
     * @param aCoordinate2
     *            the second coordinate
     * @return the result coordinates
     */
    private Coordinate[] projectLineSegment(Coordinate aCoordinate1,
            Coordinate aCoordinate2) {
        // Compute the index of the grid cells for each coordinate.
        double d1x = (aCoordinate1.x - iEnvelope.getMinX()) / iCellSize[0];
        double d1y = (aCoordinate1.y - iEnvelope.getMinY()) / iCellSize[1];
        double d2x = (aCoordinate2.x - iEnvelope.getMinX()) / iCellSize[0];
        double d2y = (aCoordinate2.y - iEnvelope.getMinY()) / iCellSize[1];

        int i1x = (int) Math.round(Math.floor(d1x));
        int i1y = (int) Math.round(Math.floor(d1y));
        int i2x = (int) Math.round(Math.floor(d2x));
        int i2y = (int) Math.round(Math.floor(d2y));
        if (d1x - i1x > 0.99) {
            i1x++;
        }
        if (d1y - i1y > 0.99) {
            i2y++;
        }
        if (d2x - i2x > 0.99) {
            i2x++;
        }
        if (d2y - i2y > 0.99) {
            i2y++;
        }

        // Get the minimum and maximum index for x and y.
        int iminx = Math.min(i1x, i2x);
        int imaxx = Math.max(i1x, i2x);
        int iminy = Math.min(i1y, i2y);
        int imaxy = Math.max(i1y, i2y);

        // Compute the parameters a and b of the equation :
        // y = a*x + b
        double d = d2x - d1x;
        double a = 0, b = 0;
        boolean aIsInfinite = false;
        if (d > 0.0001 || d < -0.0001) {
            a = (d2y - d1y) / (d2x - d1x);
            b = d2y - d2x * (d2y - d1y) / (d2x - d1x);
        } else {
            aIsInfinite = true;
        }

        // Compute the number of intersections and allocate the t value array.
        int nIntersections = imaxx - iminx + imaxy - iminy;
        double[] tValues = new double[nIntersections];

        // For each intersection, compute the t value (between 0 and 1).
        int tcnt = 0;
        int i;
        for (i = iminx + 1; i <= imaxx; i++) {
            if (!aIsInfinite) {
                // Compute the y coordinate for each intersection with
                // a vertical grid line.
                double sy = a * i + b;

                // Compute the t value for the intersection point S(i,sy).
                tValues[tcnt] = Math.sqrt((i - d1x) * (i - d1x) + (sy - d1y)
                        * (sy - d1y))
                        / Math.sqrt((d2x - d1x) * (d2x - d1x) + (d2y - d1y)
                                * (d2y - d1y));
                tcnt++;
            } else {
                logger.warn("a is infinite");
            }
        }

        for (i = iminy + 1; i <= imaxy; i++) {
            // Compute the x coordinate for each intersection with
            // a horizontal grid line.
            double sx;
            if (!aIsInfinite) {
                sx = (i - b) / a;
            } else {
                sx = (d1x + d2x) / 2;
            }

            // Compute the t value for the intersection point S(i,sy).
            tValues[tcnt] = Math.sqrt((sx - d1x) * (sx - d1x) + (i - d1y)
                    * (i - d1y))
                    / Math.sqrt((d2x - d1x) * (d2x - d1x) + (d2y - d1y)
                            * (d2y - d1y));
            tcnt++;
        }

        // Sort the array of t values.
        Arrays.sort(tValues);

        // Project all coordinate points.

        Coordinate[] coords = new Coordinate[2 + nIntersections];
        coords[0] = projectPointAsCoordinate(aCoordinate1.x, aCoordinate1.y);

        tcnt = 1;
        for (i = 0; i < nIntersections; i++) {
            // Compute the coordinates of the given intersection using
            // the associated t value.
            // Compute only if the t value is between 0 and 1.
            if (tValues[i] > 0 && tValues[i] < 1) {
                double sx = aCoordinate1.x + tValues[i]
                        * (aCoordinate2.x - aCoordinate1.x);
                double sy = aCoordinate1.y + tValues[i]
                        * (aCoordinate2.y - aCoordinate1.y);
                coords[tcnt] = projectPointAsCoordinate(sx, sy);
                tcnt++;
            }
        }

        coords[tcnt] = projectPointAsCoordinate(aCoordinate2.x, aCoordinate2.y);

        return coords;
    }

    /**
     * Projects a coordinate sequence using this grid.
     * 
     * @param aCoordinates
     *            the coordinates
     * @return the projected coordinates
     */
    public Coordinate[] projectCoordinates(Coordinate[] aCoordinates) {
        int ncoords = aCoordinates.length;
        List<Coordinate> projCoords = new ArrayList<Coordinate>();

        // Project each line segment in the coordinate sequence.
        int i, j, nProjCoords = 0;
        Coordinate[] cs = null;
        for (i = 0; i < ncoords - 1; i++) {
            cs = projectLineSegment(aCoordinates[i], aCoordinates[i + 1]);

            // Copy the coordinates into a Vector.
            // Don't copy the last coordinate, otherwise it will be twice
            // in the vector. Instead, we add the last coordinate at the end
            // of the process.
            nProjCoords = cs.length;
            for (j = 0; j < nProjCoords; j++) {
                if (cs[j] != null) {
                    projCoords.add(cs[j]);
                }
            }
            if (i < ncoords - 2) {
                projCoords.remove(projCoords.size() - 1);
            }
        }

        // Add the last coordinate.
        // projCoords.add(cs[(nProjCoords - 1)]);

        // Transform the Vector into an array.
        nProjCoords = projCoords.size();
        cs = new Coordinate[nProjCoords];
        for (i = 0; i < nProjCoords; i++) {
            cs[i] = projCoords.get(i);
        }

        return cs;
    }
}