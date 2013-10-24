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

import java.util.AbstractList;
import java.util.ArrayList;

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
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;

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
     * @param aFeature
     *            the feature
     * @param aAttrName
     *            the attribute name
     * @return the value
     */
    public static double getAttributeAsDouble(Feature aFeature, String aAttrName) {
        FeatureSchema schema = aFeature.getSchema();
        if (schema.hasAttribute(aAttrName)) {
            AttributeType type = schema.getAttributeType(aAttrName);
            if (type == AttributeType.DOUBLE) {
                return (Double) aFeature.getAttribute(aAttrName);
            } else if (type == AttributeType.INTEGER) {
                return (Integer) aFeature.getAttribute(aAttrName);
            }
        }

        return 0.0;
    }

    /**
     * @param aFeature
     *            the feature
     * @param aAttrName
     *            the attribute name
     * @param aValue
     *            the value
     */
    public static void setDoubleAttributeValue(Feature aFeature,
            String aAttrName, double aValue) {
        AttributeType type = aFeature.getSchema().getAttributeType(aAttrName);
        if (type == AttributeType.DOUBLE) {
            aFeature.setAttribute(aAttrName, aValue);
        } else if (type == AttributeType.INTEGER) {
            aFeature.setAttribute(aAttrName, (int) Math.round(aValue));
        }
    }

    /**
     * Projects the provided Feature using the provided cartogram grid.
     * 
     * @param aFeature
     *            the feature
     * @param aGrid
     *            the grid
     * @return the projected feature
     */
    public static Feature projectFeatureWithGrid(Feature aFeature,
            CartogramGrid aGrid) {
        Geometry geometry = aFeature.getGeometry();
        GeometryFactory factory = geometry.getFactory();
        String type = geometry.getGeometryType();

        // Create a copy of the Feature, but without the geometry.
        Feature feature = aFeature.clone(true);

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
     * Regularizes a geometry.
     * 
     * @param aGeometry
     *            the geometry
     * @param aMaxLength
     *            the maximum length
     * @return the regularized geometry
     */
    public static Geometry regularizeGeometry(Geometry aGeometry,
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
    public static Coordinate[] regularizeCoordinates(Coordinate[] aCoordinates,
            double aMaxLength) {
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