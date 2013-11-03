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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Represents a basic feature from the Jump package, but with a couple of
 * additional methods useful for the cartogram project.
 * 
 * @author Christian Kaiser <christian@swisscarto.ch>
 * @version v1.0.0, 2007-11-30
 */
public class CartogramFeature {

    /**
     * The logger
     */
    private static Log logger = LogFactory.getLog(CartogramFeature.class);

    /**
     * The geometry.
     */
    private Geometry iGeometry;

    /**
     * The attribute values.
     */
    private Map<String, Object> iAttributes;

    /**
     * Deep clone constructor.
     * 
     * @param aFeature
     *            the feature to clone
     */
    private CartogramFeature(CartogramFeature aFeature) {
        iGeometry = (Geometry) aFeature.iGeometry.clone();
        iAttributes = new Hashtable<String, Object>(aFeature.iAttributes.size());
        for (Map.Entry<String, Object> entry : aFeature.iAttributes.entrySet()) {
            iAttributes.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Constructor.
     * 
     * @param aGeometry
     *            the geometry
     * @param aAttributes
     *            the attributes
     */
    public CartogramFeature(Geometry aGeometry, Map<String, Object> aAttributes) {
        iAttributes = aAttributes;
        setGeometry(aGeometry);
    }

    /**
     * Returns the geometry.
     * 
     * @return the geometry
     */
    public Geometry getGeometry() {
        return iGeometry;
    }

    /**
     * Set the geometry.
     * 
     * @param aGeometry
     *            the geometry
     */
    public void setGeometry(Geometry aGeometry) {
        iGeometry = aGeometry;
        iAttributes.put("GEOMETRY", aGeometry);
    }

    /**
     * Set the attribute value
     * 
     * @param aAttrName
     *            the attribute name
     * @param aValue
     *            the value
     */
    public void setAttribute(String aAttrName, Object aValue) {
        iAttributes.put(aAttrName, aValue);
    }

    /**
     * Returns the attribute value.
     * 
     * @param aAttrName
     *            the attribute name
     * @return the value
     */
    public Object getAttribute(String aAttrName) {
        if (iAttributes.containsKey(aAttrName)) {
            return iAttributes.get(aAttrName);
        }
        return null;
    }

    /**
     * Returns the attribute value.
     * 
     * @param aAttrName
     *            the attribute name
     * @return the value
     */
    public double getAttributeAsDouble(String aAttrName) {
        if (iAttributes.containsKey(aAttrName)) {
            Object value = iAttributes.get(aAttrName);
            if (value instanceof Double) {
                return (Double) value;
            } else if (value instanceof Integer) {
                return (Integer) value;
            }
        }
        return 0;
    }

    /**
     * Projects the provided Feature using the provided cartogram grid.
     * 
     * @param aGrid
     *            the grid
     * @return the projected feature
     */
    public CartogramFeature projectFeatureWithGrid(CartogramGrid aGrid) {
        Geometry geometry = getGeometry();
        GeometryFactory factory = geometry.getFactory();
        String type = geometry.getGeometryType();

        // Create a copy of the Feature, but without the geometry.
        CartogramFeature feature = new CartogramFeature(this);

        if (type == "Point") {
            Point point = (Point) geometry;
            double[] coordinate = aGrid
                    .projectPoint(point.getX(), point.getY());
            feature.setGeometry(factory.createPoint(new Coordinate(
                    coordinate[0], coordinate[1])));
        } else if (type == "LineString") {
            feature.setGeometry(factory.createLineString(aGrid
                    .projectCoordinates(((LineString) geometry)
                            .getCoordinates())));
        } else if (type == "LinearRing") {
            feature.setGeometry(factory.createLinearRing(aGrid
                    .projectCoordinates(((LinearRing) geometry)
                            .getCoordinates())));
        } else if (type == "MultiLineString") {
            MultiLineString multiLine = (MultiLineString) geometry;
            int ngeoms = multiLine.getNumGeometries();
            LineString[] lineStrings = new LineString[ngeoms];
            for (int i = 0; i < ngeoms; i++) {
                lineStrings[i] = factory.createLineString(aGrid
                        .projectCoordinates(((LineString) multiLine
                                .getGeometryN(i)).getCoordinates()));
            }
            feature.setGeometry(factory.createMultiLineString(lineStrings));
        } else if (type == "MultiPoint") {
            MultiPoint multiPoint = (MultiPoint) geometry;
            int npts = multiPoint.getNumPoints();
            Point[] points = new Point[npts];
            Point point;
            for (int i = 0; i < npts; i++) {
                point = (Point) multiPoint.getGeometryN(i);
                points[i] = factory.createPoint(aGrid.projectPointAsCoordinate(
                        point.getX(), point.getY()));
            }
            feature.setGeometry(factory.createMultiPoint(points));
        } else if (type == "Polygon") {
            Polygon polygon = (Polygon) geometry;
            LinearRing[] interiorRings = null;
            int nrings = polygon.getNumInteriorRing();
            if (nrings > 0) {
                interiorRings = new LinearRing[nrings];
                for (int i = 0; i < nrings; i++) {
                    interiorRings[i] = factory.createLinearRing(aGrid
                            .projectCoordinates(polygon.getInteriorRingN(i)
                                    .getCoordinates()));
                }
            }
            polygon = factory.createPolygon(factory.createLinearRing(aGrid
                    .projectCoordinates(polygon.getExteriorRing()
                            .getCoordinates())), interiorRings);
            if (polygon == null) {
                logger.error("Polygon creation failed.");
            }
            feature.setGeometry(polygon);
        } else if (type == "MultiPolygon") {
            MultiPolygon multiPolygon = (MultiPolygon) geometry;
            int npolys = multiPolygon.getNumGeometries();
            Polygon[] polygons = new Polygon[npolys];
            Polygon polygon;
            LinearRing[] interiorRings;
            for (int i = 0; i < npolys; i++) {
                polygon = (Polygon) multiPolygon.getGeometryN(i);
                interiorRings = null;
                int nrings = polygon.getNumInteriorRing();
                if (nrings > 0) {
                    interiorRings = new LinearRing[nrings];
                    for (int j = 0; j < nrings; j++) {
                        interiorRings[j] = factory.createLinearRing(aGrid
                                .projectCoordinates(polygon.getInteriorRingN(j)
                                        .getCoordinates()));
                    }
                }
                polygons[i] = factory.createPolygon(factory
                        .createLinearRing(aGrid.projectCoordinates(polygon
                                .getExteriorRing().getCoordinates())),
                        interiorRings);
            }

            multiPolygon = factory.createMultiPolygon(polygons);
            if (multiPolygon == null) {
                logger.error("Multi-polygon creation failed.");
            }

            feature.setGeometry(multiPolygon);
        } else {
            logger.error("Unknown feature type");
        }

        return feature;
    }

    /**
     * Regularizes the geometry.
     * 
     * @param aMaxLength
     *            the maximum length
     */
    public void regularizeGeometry(double aMaxLength) {
        setGeometry(CartogramFeature.regularizeGeometry(iGeometry, aMaxLength));
    }

    /**
     * Regularizes a geometry.
     * 
     * @param aGeometry
     *            the geometry
     * @param aMaxLength
     *            the maximum length
     * @return the regularized geometry
     */
    private static Geometry regularizeGeometry(Geometry aGeometry,
            double aMaxLength) {
        GeometryFactory factory = aGeometry.getFactory();
        String type = aGeometry.getGeometryType();

        if (type == "Point" || type == "MultiPoint") {
            return aGeometry;
        }

        if (type == "MultiLineString") {
            MultiLineString multiLine = (MultiLineString) aGeometry;
            int ngeoms = multiLine.getNumGeometries();
            LineString[] linestrings = new LineString[ngeoms];
            for (int i = 0; i < ngeoms; i++) {
                linestrings[i] = (LineString) CartogramFeature
                        .regularizeGeometry(multiLine.getGeometryN(i),
                                aMaxLength);
            }
            return factory.createMultiLineString(linestrings);
        }

        if (type == "MultiPolygon") {
            MultiPolygon multiPolygon = (MultiPolygon) aGeometry;
            int ngeoms = multiPolygon.getNumGeometries();
            Polygon[] polygons = new Polygon[ngeoms];
            for (int i = 0; i < ngeoms; i++) {
                polygons[i] = (Polygon) CartogramFeature.regularizeGeometry(
                        multiPolygon.getGeometryN(i), aMaxLength);
            }
            return factory.createMultiPolygon(polygons);
        }

        if (type == "LineString") {
            return factory.createLineString(CartogramFeature
                    .regularizeCoordinates(aGeometry.getCoordinates(),
                            aMaxLength));
        }

        if (type == "LinearRing") {
            return factory.createLinearRing(CartogramFeature
                    .regularizeCoordinates(aGeometry.getCoordinates(),
                            aMaxLength));
        }

        if (type == "Polygon") {
            Polygon polygon = (Polygon) aGeometry;

            int nholes = polygon.getNumInteriorRing();
            LinearRing[] holes = null;
            if (nholes > 0) {
                holes = new LinearRing[nholes];

                for (int i = 0; i < nholes; i++) {
                    holes[i] = factory.createLinearRing(CartogramFeature
                            .regularizeCoordinates(polygon.getInteriorRingN(i)
                                    .getCoordinates(), aMaxLength));
                }
            }

            return factory.createPolygon(factory
                    .createLinearRing(CartogramFeature.regularizeCoordinates(
                            polygon.getExteriorRing().getCoordinates(),
                            aMaxLength)), holes);
        }

        return null;
    }

    /**
     * Regularizes a coordinate sequence.
     * 
     * @param aCoordinates
     *            the coordinates
     * @param aMaxLength
     *            the maximum length
     * @return the regularizeds coordinates
     */
    private static Coordinate[] regularizeCoordinates(
            Coordinate[] aCoordinates, double aMaxLength) {
        int ncoords = aCoordinates.length;
        if (ncoords < 1) {
            return aCoordinates;
        }

        // The vector where we will temporarily store the regularized
        // coordinates.
        AbstractList<Coordinate> newCoords = new ArrayList<Coordinate>();
        newCoords.add(aCoordinates[0]);

        // Compute for each line segment the length. If the length is
        // more than maxlen, we divide it in 2 until all the line segments
        // are shorter than maxlen.

        double sqMaxLen = aMaxLength * aMaxLength;
        double sqSegLen;
        int nseg;
        double t;
        double abx;
        double aby;
        for (int i = 0; i < ncoords - 1; i++) {
            sqSegLen = (aCoordinates[i].x - aCoordinates[i + 1].x)
                    * (aCoordinates[i].x - aCoordinates[i + 1].x)
                    + (aCoordinates[i].y - aCoordinates[i + 1].y)
                    * (aCoordinates[i].y - aCoordinates[i + 1].y);

            if (sqSegLen > sqMaxLen) {

                // How much times we have to divide the line segment into 2?
                nseg = (int) Math.round(Math.pow(
                        2.0,
                        Math.ceil(Math.log(Math.sqrt(sqSegLen) / aMaxLength)
                                / Math.log(2))));

                // Compute the vector AB (from coord i to coord i+1)
                abx = aCoordinates[i + 1].x - aCoordinates[i].x;
                aby = aCoordinates[i + 1].y - aCoordinates[i].y;

                // Compute the new coordinates
                for (int j = 1; j < nseg; j++) {
                    t = (double) j / (double) nseg;

                    // Now we can compute the coordinate for the new point
                    newCoords.add(new Coordinate(aCoordinates[i].x + t * abx,
                            aCoordinates[i].y + t * aby));
                }
            }

            newCoords.add(aCoordinates[i + 1]);
        }

        // Convert the vector holding all coordinates into an array.
        ncoords = newCoords.size();
        Coordinate[] newCoordsArray = new Coordinate[ncoords];
        for (int i = 0; i < ncoords; i++) {
            newCoordsArray[i] = newCoords.get(i);
        }

        return newCoordsArray;
    }
}