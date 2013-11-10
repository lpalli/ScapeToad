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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Contains some static methods for geometrical computations.
 * 
 * @author Christian Kaiser <christian@swisscarto.ch"
 * @version v1.0.0, 2007-11-28
 */
public class Geometry {

    /**
     * The logger
     */
    private static Log logger = LogFactory.getLog(Geometry.class);

    /**
     * Computes the intersection of two segments AB and CD.
     * 
     * @param aAx
     *            the x coordinate of point A
     * @param aAy
     *            the y coordinate of point A
     * @param aBx
     *            the x coordinate of point B
     * @param aBy
     *            the y coordinate of point B
     * @param aCx
     *            the x coordinate of point C
     * @param aCy
     *            the y coordinate of point C
     * @param aDx
     *            the x coordinate of point D
     * @param aDy
     *            the y coordinate of point D
     * @return the intersection
     */
    public static double[] intersectionOfSegments(double aAx, double aAy,
            double aBx, double aBy, double aCx, double aCy, double aDx,
            double aDy) {
        // This function has been adapted from the JUMP project,
        // the function GeoUtils.intersectSegments.

        double vx = aBx - aAx;
        double vy = aBy - aAy;
        double wx = aDx - aCx;
        double wy = aDy - aCy;

        double d = wy * vx - wx * vy;

        if (d != 0.0) {
            double t1 = (wy * (aCx - aAx) - wx * (aCy - aAy)) / d;
            double t2 = (vy * (aCx - aAx) - vx * (aCy - aAy)) / d;
            double epsilon = 0.001;
            double lowbound = 0.0 - epsilon;
            double hibound = 1.0 + epsilon;
            if (t1 >= lowbound && t1 <= hibound && t2 >= lowbound
                    && t2 <= hibound) {
                double[] e = new double[2];
                e[0] = aAx + vx * t1;
                e[1] = aAy + vy * t1;
                return e;
            }

            logger.info("The intersection point does not lie on one or both segments.");
            return null;
        }
        logger.info("The lines are parallel; no intersection.");
        return null;
    }
}