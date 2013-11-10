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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 *
 */
public class CartogramLayer {

    /**
     * The name.
     */
    private String iName;

    /**
     * The default fill color.
     */
    private Color iColor;

    /**
     * The attributes.
     */
    @SuppressWarnings("rawtypes")
    private Map<String, Class> iAttributes;

    /**
     * The features.
     */
    private List<CartogramFeature> iFeatures;

    /**
     * Constructor.
     * 
     * @param aName
     *            the layer name
     * @param aColor
     *            the default fill color
     * @param aAttributes
     *            the attributes
     * @param aFeatures
     *            the features
     */
    public CartogramLayer(String aName, Color aColor,
            @SuppressWarnings("rawtypes")
            Map<String, Class> aAttributes, List<CartogramFeature> aFeatures) {
        iName = aName;
        iColor = aColor;
        iAttributes = aAttributes;
        iFeatures = aFeatures;
    }

    /**
     * Constructor using a base layer with new features.
     * 
     * @param aLayer
     *            the base layer
     * @param aFeatires
     *            the new features
     */
    public CartogramLayer(CartogramLayer aLayer,
            List<CartogramFeature> aFeatires) {
        iName = aLayer.iName;
        iColor = aLayer.iColor;
        iAttributes = aLayer.iAttributes;
        iFeatures = aFeatires;
    }

    /**
     * Returns the features.
     * 
     * @return the features
     */
    public List<CartogramFeature> getFeatures() {
        return iFeatures;
    }

    /**
     * Returns the envelope.
     * 
     * @return the envelope
     */
    public Envelope getEnvelope() {
        Envelope envelope = new Envelope();
        for (CartogramFeature feature : iFeatures) {
            envelope.expandToInclude(feature.getGeometry()
                    .getEnvelopeInternal());
        }
        return envelope;
    }

    /**
     * Returns the name.
     * 
     * @return the name
     */
    public String getName() {
        return iName;
    }

    /**
     * Returns the default fill color.
     * 
     * @return the color
     */
    public Color getColor() {
        return iColor;
    }

    /**
     * Returns the attributes.
     * 
     * @return the attributes
     */
    @SuppressWarnings("rawtypes")
    public Map<String, Class> getAttributes() {
        return iAttributes;
    }

    /**
     * Adds a new attribute containing the density value for a given attribute.
     * We check at the same time if there are some values bigger than 0. If not,
     * we raise an exception as we cannot compute a cartogram on a 0 surface.
     * 
     * @param aPopulationAttr
     *            the name of the (existing) attribute for which we shall
     *            compute the density.
     * @param aDensityAttr
     *            the name of the new density attribute.
     * @throws DataFormatException
     *             when the data format is wrong
     */
    public void addDensityAttribute(String aPopulationAttr, String aDensityAttr)
            throws DataFormatException {
        // Add the attribute metadata
        iAttributes.put(aDensityAttr, Double.class);

        boolean allValuesAreZero = true;
        double geomArea;
        double attrValue;
        double density;
        for (CartogramFeature feature : iFeatures) {
            geomArea = feature.getGeometry().getArea();
            attrValue = feature.getAttributeAsDouble(aPopulationAttr);

            density = 0.0;
            if (geomArea > 0 && attrValue > 0) {
                density = attrValue / geomArea;
                allValuesAreZero = false;
            }

            feature.setAttribute(aDensityAttr, density);
        }

        if (allValuesAreZero) {
            throw new DataFormatException(
                    String.format(
                            "All values in the attribute '%1$s' of layer '%2$s' \nare zero.\n\nThis might be an error related to the underlying JUMP framework.\n\nPlease check your layer with a GIS software such as: \n- QuantumGIS (www.qgis.org) or\n- OpenJUMP (www.openjump.org).",
                            aPopulationAttr, getName()));
        }
    }

    /**
     * Computes the mean value for the given attribute weighted by the feature
     * area.
     * 
     * @param aAttrName
     *            the attribute name
     * @return the mean density
     */
    public double meanDensityWithAttribute(String aAttrName) {
        double totalArea = totalArea();
        double meanDensity = 0.0;
        for (CartogramFeature feature : iFeatures) {
            meanDensity += feature.getGeometry().getArea() / totalArea
                    * feature.getAttributeAsDouble(aAttrName);
        }
        return meanDensity;
    }

    /**
     * Returns the mean value for the given attribute.
     * 
     * @param aAttrName
     *            the attribute name
     * @return the mean value
     */
    public double meanValueForAttribute(String aAttrName) {
        int nobj = 0;
        double meanValue = 0.0;
        for (CartogramFeature feature : iFeatures) {
            meanValue += feature.getAttributeAsDouble(aAttrName);
            nobj++;
        }
        return meanValue / nobj;
    }

    /**
     * Returns the minimum value for the given attribute.
     * 
     * @param aAttrName
     *            the attribute name
     * @return the minimum value
     */
    public double minValueForAttribute(String aAttrName) {
        double minValue = Double.MAX_VALUE;
        double attrValue;
        for (CartogramFeature feature : iFeatures) {
            attrValue = feature.getAttributeAsDouble(aAttrName);
            if (attrValue < minValue) {
                minValue = attrValue;
            }
        }

        if (minValue == Double.MAX_VALUE) {
            return 0.0;
        }
        return minValue;
    }

    /**
     * Returns the maximum value for the given attribute.
     * 
     * @param aAttrName
     *            the attribute name
     * @return the maximum value
     */
    public double maxValueForAttribute(String aAttrName) {
        double maxValue = Double.MIN_VALUE;
        double attrValue;
        for (CartogramFeature feature : iFeatures) {
            attrValue = feature.getAttributeAsDouble(aAttrName);
            if (attrValue > maxValue) {
                maxValue = attrValue;
            }
        }

        if (maxValue == Double.MIN_VALUE) {
            return 0.0;
        }
        return maxValue;
    }

    /**
     * Computes the sum of the provided attribute.
     * 
     * @param aAttrName
     *            the attribute name
     * @return the sum
     */
    public double sumForAttribute(String aAttrName) {
        double sum = 0.0;
        for (CartogramFeature feature : iFeatures) {
            sum += feature.getAttributeAsDouble(aAttrName);
        }
        return sum;
    }

    /**
     * Computes the variance of the provided attribute.
     * 
     * @param aAttrName
     *            the attribute name
     * @return the variance
     */
    public double varianceForAttribute(String aAttrName) {
        double mean = meanValueForAttribute(aAttrName);
        double diffSum = 0.0;
        double nFeat = 0;

        double val;
        for (CartogramFeature feature : iFeatures) {
            val = feature.getAttributeAsDouble(aAttrName);
            diffSum += (val - mean) * (val - mean);
            nFeat += 1.0;
        }

        return diffSum / nFeat;
    }

    /**
     * Computes the standard deviation of the provided attribute.
     * 
     * @param aAttrName
     *            the attribute name
     * @return the standard deviation
     */
    public double standardDeviationForAttribute(String aAttrName) {
        return Math.sqrt(varianceForAttribute(aAttrName));
    }

    /**
     * Returns the n-th percentile of the provided attribute.
     * 
     * @param aAttrName
     *            the attribute name
     * @param aPercentile
     *            the percentile, must be between 0 and 100
     * @return the percentile
     */
    public double percentileForAttribute(String aAttrName, int aPercentile) {
        double dblN = aPercentile;
        if (aPercentile < 0) {
            dblN = 0;
        }
        if (aPercentile > 100) {
            dblN = 100;
        }

        // Create a new TreeSet and store the attribute values inside
        TreeSet<Double> set = new TreeSet<Double>();
        for (CartogramFeature feature : iFeatures) {
            set.add(feature.getAttributeAsDouble(aAttrName));
        }

        // Get the number of features
        int nfeat = set.size();

        // Create a Vector from the TreeSet
        List<Double> attrVector = new ArrayList<Double>(set);

        // Get the indexes of the bounding features
        double dblIndex = dblN / 100 * nfeat;
        int lowerIndex = Math.round((float) Math.floor(dblIndex));
        int upperIndex = Math.round((float) Math.ceil(dblIndex));

        if (lowerIndex == upperIndex) {
            return attrVector.get(lowerIndex);
        }

        double lowerPctl = (double) lowerIndex / (double) nfeat * 100;
        double lowerValue = attrVector.get(lowerIndex);
        double upperPctl = (double) upperIndex / (double) nfeat * 100;
        double upperValue = attrVector.get(upperIndex);

        double scalingFactor = 1.0;
        if (upperPctl - lowerPctl > 0) {
            scalingFactor = (dblN - lowerPctl) / (upperPctl - lowerPctl);
        }

        return scalingFactor * (upperValue - lowerValue) + lowerValue;
    }

    /**
     * Replaces a double attribute value with another.
     * 
     * @param aAttrName
     *            the attribute name
     * @param aOldValue
     *            the old value
     * @param aNewValue
     *            the new value
     */
    public void replaceAttributeValue(String aAttrName, double aOldValue,
            double aNewValue) {
        double value;
        for (CartogramFeature feature : iFeatures) {
            value = feature.getAttributeAsDouble(aAttrName);
            if (value == aOldValue) {
                feature.setAttribute(aAttrName, aNewValue);
            }
        }
    }

    /**
     * Computes the total area of all features in this layer.
     * 
     * @return the total area
     */
    public double totalArea() {
        double totalArea = 0.0;
        for (CartogramFeature feature : iFeatures) {
            totalArea += feature.getGeometry().getArea();
        }
        return totalArea;
    }

    /**
     * Computes the contour of this layer.
     * 
     * @return the contour as a Geometry
     */
    public Geometry contour() {
        Geometry contour = null;
        Geometry geometry;
        for (CartogramFeature feature : iFeatures) {
            geometry = feature.getGeometry();
            if (contour == null) {
                contour = geometry;
            } else {
                contour = contour.union(geometry);
            }
        }
        return contour;
    }

    /**
     * Regularizes a layer. This means the length of all line segments does not
     * exceed a given value. In the case of a too long line segment, the line is
     * repeatedly divided in two until the length is less than the given value.
     * 
     * @param aMaxlen
     *            the maximum length of the line segments
     */
    public void regularizeLayer(double aMaxlen) {
        for (CartogramFeature feature : iFeatures) {
            feature.regularizeGeometry(aMaxlen);
        }
    }

    /**
     * Projects a layer using a cartogram grid. Returns the projected layer.
     * 
     * @param aGrid
     *            the grid
     * @return the projected layer
     */
    public CartogramLayer projectLayerWithGrid(CartogramGrid aGrid) {
        List<CartogramFeature> featires = new ArrayList<CartogramFeature>(
                iFeatures.size());
        // Project each Feature one by one
        for (CartogramFeature feature : iFeatures) {
            feature = feature.projectFeatureWithGrid(aGrid);

            if (feature != null) {
                featires.add(feature);
            }
        }

        // Create a layer with the FeatureDataset
        return new CartogramLayer(this, featires);
    }

    /**
     * Computes the cartogram size error and stores it in the layer's attribute
     * with the provided name. The size error is computed as follows: err = 100
     * * ((areaOptimal * Sum(areaReal)) / (areaReal * Sum(areaOptimal))) where
     * err : the size error areaOptimal : the optimal or theoretical area of a
     * polygon areaReal : the current area of a polygon
     * 
     * @param aCartogramAttribute
     *            the cartogram attribute name
     * @param aOriginalLayer
     *            the original layer
     * @param aErrorAttribute
     *            the error attribute name
     * 
     * @return the mean size error.
     */
    public double computeCartogramSizeError(String aCartogramAttribute,
            CartogramLayer aOriginalLayer, String aErrorAttribute) {
        double sumOfRealAreas = totalArea();
        double sumOfOptimalAreas = aOriginalLayer.totalArea();
        double sumOfValues = sumForAttribute(aCartogramAttribute);

        if (sumOfOptimalAreas == 0 || sumOfValues == 0 || sumOfRealAreas == 0) {
            return 0.0;
        }

        iAttributes.put(aErrorAttribute, Double.class);

        double geomArea;
        double featError;
        for (CartogramFeature feature : iFeatures) {
            geomArea = feature.getGeometry().getArea();

            featError = 0.0;
            if (geomArea > 0.0) {
                // Compute the optimal cartogram area
                featError = 100 * (feature
                        .getAttributeAsDouble(aCartogramAttribute)
                        / sumOfValues * sumOfOptimalAreas * sumOfRealAreas / (geomArea * sumOfOptimalAreas));
            }

            feature.setAttribute(aErrorAttribute, new Double(featError));
        }

        return meanValueForAttribute(aErrorAttribute);
    }

    /**
     * Checks the attribute values for invalid values and replaces them with a
     * zero value. This method works only with double value attributes.
     * 
     * @param aAttrName
     *            the attribute name
     */
    public void cleanAttributeValues(String aAttrName) {
        Object value;
        for (CartogramFeature feature : iFeatures) {
            value = feature.getAttribute(aAttrName);
            if (value == null || value instanceof Double
                    && ((Double) value).isNaN()) {
                feature.setAttribute(aAttrName, new Double(0.0));
            }
        }
    }
}