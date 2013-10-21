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

import java.util.Vector;

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
                return ((Double) aFeature.getAttribute(aAttrName))
                        .doubleValue();
            } else if (type == AttributeType.INTEGER) {
                return ((Integer) aFeature.getAttribute(aAttrName))
                        .doubleValue();
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
            aFeature.setAttribute(aAttrName, new Double(aValue));
        } else if (type == AttributeType.INTEGER) {
            int intValue = (int) Math.round(aValue);
            aFeature.setAttribute(aAttrName, new Integer(intValue));
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
            Point pt = (Point) geometry;
            double[] c = aGrid.projectPoint(pt.getX(), pt.getY());
            Point pt2 = factory.createPoint(new Coordinate(c[0], c[1]));
            feature.setGeometry(pt2);
        } else if (type == "LineString") {
            LineString l1 = (LineString) geometry;
            Coordinate[] cs = aGrid.projectCoordinates(l1.getCoordinates());
            LineString l2 = factory.createLineString(cs);
            feature.setGeometry(l2);
        } else if (type == "LinearRing") {
            LinearRing l1 = (LinearRing) geometry;
            Coordinate[] cs = aGrid.projectCoordinates(l1.getCoordinates());
            LinearRing l2 = factory.createLinearRing(cs);
            feature.setGeometry(l2);
        } else if (type == "MultiLineString") {
            MultiLineString mls1 = (MultiLineString) geometry;
            int ngeoms = mls1.getNumGeometries();
            LineString[] lineStrings = new LineString[ngeoms];
            for (int geomcnt = 0; geomcnt < ngeoms; geomcnt++) {
                LineString l1 = (LineString) mls1.getGeometryN(geomcnt);
                Coordinate[] cs = aGrid.projectCoordinates(l1.getCoordinates());
                lineStrings[geomcnt] = factory.createLineString(cs);
            }
            MultiLineString mls2 = factory.createMultiLineString(lineStrings);
            feature.setGeometry(mls2);
        } else if (type == "MultiPoint") {
            MultiPoint mp1 = (MultiPoint) geometry;
            int npts = mp1.getNumPoints();
            Point[] points = new Point[npts];
            for (int ptcnt = 0; ptcnt < npts; ptcnt++) {
                Point pt = (Point) mp1.getGeometryN(ptcnt);
                Coordinate c = aGrid.projectPointAsCoordinate(pt.getX(),
                        pt.getY());
                points[ptcnt] = factory.createPoint(c);
            }
            MultiPoint mp2 = factory.createMultiPoint(points);
            feature.setGeometry(mp2);
        } else if (type == "Polygon") {
            Polygon p1 = (Polygon) geometry;
            Coordinate[] exteriorRingCoords = aGrid.projectCoordinates(p1
                    .getExteriorRing().getCoordinates());
            LinearRing exteriorRing = factory
                    .createLinearRing(exteriorRingCoords);
            LinearRing[] interiorRings = null;
            int nrings = p1.getNumInteriorRing();
            if (nrings > 0) {
                interiorRings = new LinearRing[nrings];
                for (int ringcnt = 0; ringcnt < nrings; ringcnt++) {
                    Coordinate[] interiorRingCoords = aGrid
                            .projectCoordinates(p1.getInteriorRingN(ringcnt)
                                    .getCoordinates());
                    interiorRings[ringcnt] = factory
                            .createLinearRing(interiorRingCoords);
                }
            }
            Polygon p2 = factory.createPolygon(exteriorRing, interiorRings);
            if (p2 == null) {
                logger.error("Polygon creation failed.");
            }
            feature.setGeometry(p2);
        } else if (type == "MultiPolygon") {
            MultiPolygon mp1 = (MultiPolygon) geometry;
            int npolys = mp1.getNumGeometries();
            Polygon[] polys = new Polygon[npolys];
            for (int polycnt = 0; polycnt < npolys; polycnt++) {
                Polygon p1 = (Polygon) mp1.getGeometryN(polycnt);
                Coordinate[] exteriorRingCoords = aGrid.projectCoordinates(p1
                        .getExteriorRing().getCoordinates());
                LinearRing exteriorRing = factory
                        .createLinearRing(exteriorRingCoords);
                LinearRing[] interiorRings = null;
                int nrings = p1.getNumInteriorRing();
                if (nrings > 0) {
                    interiorRings = new LinearRing[nrings];
                    for (int ringcnt = 0; ringcnt < nrings; ringcnt++) {
                        Coordinate[] interiorRingCoords = aGrid
                                .projectCoordinates(p1
                                        .getInteriorRingN(ringcnt)
                                        .getCoordinates());

                        interiorRings[ringcnt] = factory
                                .createLinearRing(interiorRingCoords);
                    }
                }
                polys[polycnt] = factory.createPolygon(exteriorRing,
                        interiorRings);
            }

            MultiPolygon mp2 = factory.createMultiPolygon(polys);
            if (mp2 == null) {
                logger.error("Multi-polygon creation failed.");
            }

            feature.setGeometry(mp2);
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
            MultiLineString mls = (MultiLineString) aGeometry;
            int ngeoms = mls.getNumGeometries();
            LineString[] lss = new LineString[ngeoms];
            for (int lscnt = 0; lscnt < ngeoms; lscnt++) {
                LineString ls = (LineString) mls.getGeometryN(lscnt);
                lss[lscnt] = (LineString) CartogramFeature.regularizeGeometry(
                        ls, aMaxLength);
            }
            mls = factory.createMultiLineString(lss);
            return mls;
        }

        if (type == "MultiPolygon") {
            MultiPolygon mpoly = (MultiPolygon) aGeometry;
            int ngeoms = mpoly.getNumGeometries();
            Polygon[] polys = new Polygon[ngeoms];
            for (int polycnt = 0; polycnt < ngeoms; polycnt++) {
                Polygon poly = (Polygon) mpoly.getGeometryN(polycnt);
                polys[polycnt] = (Polygon) CartogramFeature.regularizeGeometry(
                        poly, aMaxLength);
            }
            mpoly = factory.createMultiPolygon(polys);
            return mpoly;
        }

        if (type == "LineString") {
            Coordinate[] cs1 = aGeometry.getCoordinates();
            Coordinate[] cs2 = CartogramFeature.regularizeCoordinates(cs1,
                    aMaxLength);

            LineString ls = factory.createLineString(cs2);
            return ls;
        }

        if (type == "LinearRing") {
            Coordinate[] cs1 = aGeometry.getCoordinates();
            Coordinate[] cs2 = CartogramFeature.regularizeCoordinates(cs1,
                    aMaxLength);

            LinearRing lr = factory.createLinearRing(cs2);
            return lr;
        }

        if (type == "Polygon") {
            Polygon p = (Polygon) aGeometry;
            LineString shell = p.getExteriorRing();
            Coordinate[] shellCoords = CartogramFeature.regularizeCoordinates(
                    shell.getCoordinates(), aMaxLength);
            LinearRing regShell = factory.createLinearRing(shellCoords);

            int nholes = p.getNumInteriorRing();
            LinearRing[] holes = null;
            if (nholes > 0) {
                holes = new LinearRing[nholes];

                for (int holecnt = 0; holecnt < nholes; holecnt++) {
                    LineString hole = p.getInteriorRingN(holecnt);
                    Coordinate[] holeCoords = CartogramFeature
                            .regularizeCoordinates(hole.getCoordinates(),
                                    aMaxLength);

                    holes[holecnt] = factory.createLinearRing(holeCoords);
                }
            }

            Polygon p2 = factory.createPolygon(regShell, holes);

            return p2;
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
        Vector<Coordinate> newCoords = new Vector<Coordinate>();
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
        for (int coordcnt = 0; coordcnt < ncoords; coordcnt++) {
            newCoordsArray[coordcnt] = newCoords.get(coordcnt);
        }

        return newCoordsArray;
    }
}