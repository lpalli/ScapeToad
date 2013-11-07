//
//  CartogramNewman.java
//  ScapeToad
//
//  Created by Christian on 29.10.08.
//  Copyright 2008 __MyCompanyName__. All rights reserved.
//

package ch.epfl.scapetoad.compute;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import edu.emory.mathcs.jtransforms.dct.DoubleDCT_2D;

/**
 * Implementation of the diffusion algorithm of Gastner and Newman. This version
 * is a Java adaptation of Mark Newman's C code.
 */
public class CartogramNewman {

    /**
     * The logger.
     */
    private static Log logger = LogFactory.getLog(CartogramNewman.class);

    /**
     * The cartogram grid on which we will apply the cartogram transformation.
     */
    private CartogramGrid iCartogramGrid;

    /**
     * The size of the diffusion grid.
     */
    private int[] iGridSize = new int[2];

    /**
     * The cartogram bounding box. It is slightly larger than the grid bounding
     * box for computational reasons.
     */
    private Envelope iExtent;

    /**
     * Pop density at time t (five snaps needed).
     */
    private double[][][] iRhot;

    /**
     * FT of initial density.
     */
    private double[][] iFftrho;

    /**
     * FT of density at time t.
     */
    private double[][] iFftexpt;

    /**
     * Array needed for the Gaussian convolution.
     */
    private double[] iExpky;

    /**
     * Array for storing the X grid points.
     */
    private double[] iGridPointsX;

    /**
     * Array for storing the Y grid points.
     */
    private double[] iGridPointsY;

    /**
     * Initial size of a time step.
     */
    private static double INITH = 0.001;

    /**
     * Desired accuracy per step in pixels.
     */
    private static double TARGETERROR = 0.01;

    /**
     * Max ratio to increase step size by.
     */
    private static double MAXRATIO = 4.0;

    /**
     * Guess of the time it will take. Only used for the completion estimation.
     */
    private static double EXPECTEDTIME = 100000000.0f;

    /**
     * The maximum integration error found for any polygon vertex for the
     * complete two-step integration process.
     */
    private double iErrorp;

    /**
     * Maximum distance moved by any point during an integration step.
     */
    private double iDrp;

    /**
     * The cartogram status.
     */
    private ICartogramStatus iStatus;

    /**
     * The value of the running status at the begin of the cartogram
     * computation.
     */
    private int iMinimumStatus;

    /**
     * The value of the running status at the end of the cartogram computation.
     */
    private int iMaximumStatus;

    /**
     * The main string of the running status which is set by the caller. This
     * string will be displayed in the first line of the running status wizard.
     * The second line is freely used by this class.
     */
    private String iStatusMessage;

    /**
     * Constructor for the CartogramNewman class.
     * 
     * @param aCartogramGrid
     *            the grid
     */
    public CartogramNewman(CartogramGrid aCartogramGrid) {
        iCartogramGrid = aCartogramGrid;

        iGridSize[0] = iCartogramGrid.getGridSize()[0] - 1;
        iGridSize[1] = iCartogramGrid.getGridSize()[1] - 1;
    }

    /**
     * 
     * 
     * @param aStatus
     *            the cartogram status
     * @param aMinimumStatus
     *            the minimum status value
     * @param aMaximumStatus
     *            the maximum status value
     * @param aStatusMessage
     *            the status message
     */
    public void initializeStatus(ICartogramStatus aStatus, int aMinimumStatus,
            int aMaximumStatus, String aStatusMessage) {
        iStatus = aStatus;
        iMinimumStatus = aMinimumStatus;
        iMaximumStatus = aMaximumStatus;
        iStatusMessage = aStatusMessage;
    }

    /**
     * Starts the cartogram computation.
     * 
     * @throws InterruptedException
     *             when it was interrupted
     */
    public void compute() throws InterruptedException {
        // Allocate space for the cartogram code to use
        initializeArrays();

        // Read in the population data, and store it in fftrho.
        computeInitialDensity();

        // Create the grid of points.
        createGridOfPoints();

        // Compute the cartogram.
        makeCartogram(0.0);

        // Project the cartogram grid.
        projectCartogramGrid();
    }

    /**
     * Private method for initializing the class arrays.
     * 
     * @throws InterruptedException
     *             when was interrupted
     */
    private void initializeArrays() throws InterruptedException {
        try {
            iRhot = new double[5][iGridSize[0]][iGridSize[1]];
            iFftrho = new double[iGridSize[0]][iGridSize[1]];
            iFftexpt = new double[iGridSize[0]][iGridSize[1]];
            iExpky = new double[iGridSize[1]];
        } catch (Exception e) {
            logger.error("Out of memory error.");
            throw new InterruptedException(
                    "Out of memory. Use a smaller cartogram grid or increase memory.");
        }
    }

    /**
     * Method to read population data, transform it, and store it in fftrho.
     */
    private void computeInitialDensity() {
        // Read population density into fftrho.
        readPopulationDensity();

        // Transform fftrho.
        DoubleDCT_2D transform = new DoubleDCT_2D(iGridSize[0], iGridSize[1]);
        transform.forward(iFftrho, false);
    }

    /**
     * Reads the population density into fftrho.
     */
    private void readPopulationDensity() {
        // Copy the cartogram bounding box.
        iExtent = iCartogramGrid.getEnvelope();

        // Fill the diffusion grid using the cartogram grid values.
        fillDiffusionGrid(iCartogramGrid.getCurrentDensityArray());
    }

    /**
     * Fills fftrho using the provided grid values.
     * 
     * @param aValue
     *            the grid values
     */
    private void fillDiffusionGrid(double[][] aValue) {
        for (int i = 0; i < iGridSize[0]; i++) {
            for (int j = 0; j < iGridSize[1]; j++) {
                iFftrho[i][j] = aValue[i][j];
            }
        }
    }

    /**
     * Create the grid of points (store the coordinates of the diffusion grid
     * points).
     */
    private void createGridOfPoints() {
        iGridPointsX = new double[(iGridSize[0] + 1) * (iGridSize[1] + 1)];
        iGridPointsY = new double[(iGridSize[0] + 1) * (iGridSize[1] + 1)];

        int i = 0;
        for (int y = 0; y <= iGridSize[1]; y++) {
            for (int x = 0; x <= iGridSize[0]; x++) {
                iGridPointsX[i] = x;
                iGridPointsY[i] = y;
                i++;
            }
        }
    }

    /**
     * Do the transformation of the given set of points to the cartogram
     * 
     * @param aBlur
     *            the blur value
     */
    private void makeCartogram(double aBlur) {
        // Calculate the initial density for snapshot zero */
        int s = 0;
        densitySnapshot(0.0, s);

        // Integrate the points.
        double t = 0.5 * aBlur * aBlur;
        double h = INITH;

        int sp;
        iDrp = 1.0f;
        double desiredratio;
        do {
            // Do a combined (triple) integration step
            sp = integrateTwoSteps(t, h, s);

            // Increase the time by 2h and rotate snapshots
            t += 2.0 * h;
            s = sp;

            // Adjust the time-step.
            // Factor of 2 arises because the target for the two-step process is
            // twice the target for an individual step
            desiredratio = Math.pow(2 * TARGETERROR / iErrorp, 0.2);

            if (desiredratio > MAXRATIO) {
                h *= MAXRATIO;
            } else {
                h *= desiredratio;
            }

            updateRunningStatus(t);
        } while (iDrp > 0.0f); // If no point moved then we are finished
    }

    /**
     * Function to calculate the population density at arbitrary time by back-
     * transforming and put the result in a particular rhot[] snapshot array.
     * 
     * @param aT
     *            the time
     * @param aS
     *            the snapshot
     */
    private void densitySnapshot(double aT, int aS) {
        double ky;

        // Calculate the expky array, to save time in the next part
        for (int y = 0; y < iGridSize[1]; y++) {
            ky = Math.PI * y / iGridSize[1];
            iExpky[y] = Math.exp(-ky * ky * aT);
        }

        double kx;
        double expkx;
        // Multiply the FT of the density by the appropriate factors
        for (int x = 0; x < iGridSize[0]; x++) {
            kx = Math.PI * x / iGridSize[0];
            expkx = Math.exp(-kx * kx * aT);
            for (int y = 0; y < iGridSize[1]; y++) {
                iFftexpt[x][y] = expkx * iExpky[y] * iFftrho[x][y];

                // Save a copy to the rhot[s] array on which we will perform the
                // DCT back-transform.
                iRhot[aS][x][y] = iFftexpt[x][y];
            }
        }

        // Perform the back-transform
        DoubleDCT_2D dct = new DoubleDCT_2D(iGridSize[0], iGridSize[1]);
        dct.inverse(iRhot[aS], false);
    }

    /**
     * Integrates 2h time into the future two different ways using fourth-order
     * Runge-Kutta and compare the differences for the purposes of the adaptive
     * step size.
     * 
     * @param aT
     *            the current time, i.e., start time of these two steps
     * 
     * @param aH
     *            delta t
     * 
     * @param aS
     *            snapshot index of the initial time
     * 
     * @return the snapshot index for the final function evaluation
     */
    private int integrateTwoSteps(double aT, double aH, int aS) {
        int s0 = aS;
        int s1 = (aS + 1) % 5;
        int s2 = (aS + 2) % 5;
        int s3 = (aS + 3) % 5;
        int s4 = (aS + 4) % 5;

        // Compute the density field for the four new time slices.
        densitySnapshot(aT + 0.5 * aH, s1);
        densitySnapshot(aT + 1.0 * aH, s2);
        densitySnapshot(aT + 1.5 * aH, s3);
        densitySnapshot(aT + 2.0 * aH, s4);

        // Do all three Runga-Kutta steps for each point in turn.
        double esqmax = 0.0;
        double drsqmax = 0.0;
        int npoints = (iGridSize[0] + 1) * (iGridSize[1] + 1);
        for (int p = 0; p < npoints; p++) {
            double rx1 = iGridPointsX[p];
            double ry1 = iGridPointsY[p];

            // Do the big combined (2h) Runga-Kutta step.

            Coordinate v1 = velocity(rx1, ry1, s0);
            double k1x = 2 * aH * v1.x;
            double k1y = 2 * aH * v1.y;
            Coordinate v2 = velocity(rx1 + 0.5 * k1x, ry1 + 0.5 * k1y, s2);
            double k2x = 2 * aH * v2.x;
            double k2y = 2 * aH * v2.y;
            Coordinate v3 = velocity(rx1 + 0.5 * k2x, ry1 + 0.5 * k2y, s2);
            double k3x = 2 * aH * v3.x;
            double k3y = 2 * aH * v3.y;
            Coordinate v4 = velocity(rx1 + k3x, ry1 + k3y, s4);
            double k4x = 2 * aH * v4.x;
            double k4y = 2 * aH * v4.y;

            double dx12 = (k1x + k4x + 2.0 * (k2x + k3x)) / 6.0;
            double dy12 = (k1y + k4y + 2.0 * (k2y + k3y)) / 6.0;

            // Do the first small Runge-Kutta step.
            // No initial call to the velocity method is done
            // because it would be the same as the one above, so there's no need
            // to do it again

            k1x = aH * v1.x;
            k1y = aH * v1.y;
            v2 = velocity(rx1 + 0.5 * k1x, ry1 + 0.5 * k1y, s1);
            k2x = aH * v2.x;
            k2y = aH * v2.y;
            v3 = velocity(rx1 + 0.5 * k2x, ry1 + 0.5 * k2y, s1);
            k3x = aH * v3.x;
            k3y = aH * v3.y;
            v4 = velocity(rx1 + k3x, ry1 + k3y, s2);
            k4x = aH * v4.x;
            k4y = aH * v4.y;

            double dx1 = (k1x + k4x + 2.0 * (k2x + k3x)) / 6.0;
            double dy1 = (k1y + k4y + 2.0 * (k2y + k3y)) / 6.0;

            // Do the second small RK step

            double rx2 = rx1 + dx1;
            double ry2 = ry1 + dy1;

            v1 = velocity(rx2, ry2, s2);
            k1x = aH * v1.x;
            k1y = aH * v1.y;
            v2 = velocity(rx2 + 0.5 * k1x, ry2 + 0.5 * k1y, s3);
            k2x = aH * v2.x;
            k2y = aH * v2.y;
            v3 = velocity(rx2 + 0.5 * k2x, ry2 + 0.5 * k2y, s3);
            k3x = aH * v3.x;
            k3y = aH * v3.y;
            v4 = velocity(rx2 + k3x, ry2 + k3y, s4);
            k4x = aH * v4.x;
            k4y = aH * v4.y;

            double dx2 = (k1x + k4x + 2.0 * (k2x + k3x)) / 6.0;
            double dy2 = (k1y + k4y + 2.0 * (k2y + k3y)) / 6.0;

            // Calculate the (squared) error.

            double ex = (dx1 + dx2 - dx12) / 15;
            double ey = (dy1 + dy2 - dy12) / 15;
            double esq = ex * ex + ey * ey;
            if (esq > esqmax) {
                esqmax = esq;
            }

            // Update the position of the vertex using the more accurate (two
            // small steps) result,
            // and deal with the boundary conditions.
            // This code does 5th-order "local extrapolation" (which just means
            // taking the estimate of
            // the 5th-order term above and adding it to our 4th-order result
            // get a result accurate to
            // the next highest order).

            double dxtotal = dx1 + dx2 + ex; // Last term is local extrapolation
            double dytotal = dy1 + dy2 + ey; // Last term is local extrapolation
            double drsq = dxtotal * dxtotal + dytotal * dytotal;
            if (drsq > drsqmax) {
                drsqmax = drsq;
            }

            double rx3 = rx1 + dxtotal;
            double ry3 = ry1 + dytotal;

            if (rx3 < 0) {
                rx3 = 0;
            } else if (rx3 > iGridSize[0]) {
                rx3 = iGridSize[0];
            }

            if (ry3 < 0) {
                ry3 = 0;
            } else if (ry3 > iGridSize[1]) {
                ry3 = iGridSize[1];
            }

            iGridPointsX[p] = rx3;
            iGridPointsY[p] = ry3;
        }

        iErrorp = Math.sqrt(esqmax);
        iDrp = Math.sqrt(drsqmax);

        return s4;
    }

    /**
     * Computes the velocity at an arbitrary point from the grid velocities for
     * a specified snapshot by interpolating between grid points. If the
     * requested point is outside the boundaries, we extrapolate (ensures smooth
     * flow back in if we get outside by mistake, although we should never
     * actually do this because the calling method integrateTwoSteps() contains
     * code to prevent it).
     * 
     * @param aRx
     *            the x coordinate of the point for which we compute the
     *            velocity.
     * 
     * @param aRy
     *            the y coordinate of the point for which we compute the
     *            velocity.
     * 
     * @param aS
     *            the snapshot
     * 
     * @return the velocity in x and y as a coordinate.
     */
    private Coordinate velocity(double aRx, double aRy, int aS) {
        // Deal with the boundary conditions.

        int ix = (int) aRx;
        if (ix < 0) {
            ix = 0;
        } else if (ix >= iGridSize[0]) {
            ix = iGridSize[0] - 1;
        }

        int ixm1 = ix - 1;
        if (ixm1 < 0) {
            ixm1 = 0;
        }
        int ixp1 = ix + 1;
        if (ixp1 >= iGridSize[0]) {
            ixp1 = iGridSize[0] - 1;
        }

        int iy = (int) aRy;
        if (iy < 0) {
            iy = 0;
        } else if (iy >= iGridSize[1]) {
            iy = iGridSize[1] - 1;
        }

        int iym1 = iy - 1;
        if (iym1 < 0) {
            iym1 = 0;
        }
        int iyp1 = iy + 1;
        if (iyp1 >= iGridSize[1]) {
            iyp1 = iGridSize[1] - 1;
        }

        // Calculate the densities at the nine surrounding grid points
        double rho00 = iRhot[aS][ixm1][iym1];
        double rho10 = iRhot[aS][ix][iym1];
        double rho20 = iRhot[aS][ixp1][iym1];
        double rho01 = iRhot[aS][ixm1][iy];
        double rho11 = iRhot[aS][ix][iy];
        double rho21 = iRhot[aS][ixp1][iy];
        double rho02 = iRhot[aS][ixm1][iyp1];
        double rho12 = iRhot[aS][ix][iyp1];
        double rho22 = iRhot[aS][ixp1][iyp1];

        // Calculate velocities at the four surrounding grid points

        double mid11 = rho00 + rho10 + rho01 + rho11;
        double vx11 = -2.0 * (rho10 - rho00 + rho11 - rho01) / mid11;
        double vy11 = -2.0 * (rho01 - rho00 + rho11 - rho10) / mid11;

        double mid21 = rho10 + rho20 + rho11 + rho21;
        double vx21 = -2.0 * (rho20 - rho10 + rho21 - rho11) / mid21;
        double vy21 = -2.0 * (rho11 - rho10 + rho21 - rho20) / mid21;

        double mid12 = rho01 + rho11 + rho02 + rho12;
        double vx12 = -2.0 * (rho11 - rho01 + rho12 - rho02) / mid12;
        double vy12 = -2.0 * (rho02 - rho01 + rho12 - rho11) / mid12;

        double mid22 = rho11 + rho21 + rho12 + rho22;
        double vx22 = -2.0 * (rho21 - rho11 + rho22 - rho12) / mid22;
        double vy22 = -2.0 * (rho12 - rho11 + rho22 - rho21) / mid22;

        // Calculate the weights for the bilinear interpolation

        double dx = aRx - ix;
        double dy = aRy - iy;

        double dx1m = 1.0 - dx;
        double dy1m = 1.0 - dy;

        double w11 = dx1m * dy1m;
        double w21 = dx * dy1m;
        double w12 = dx1m * dy;
        double w22 = dx * dy;

        // Perform the interpolation for x and y components of velocity
        return new Coordinate(
                w11 * vx11 + w21 * vx21 + w12 * vx12 + w22 * vx22, w11 * vy11
                        + w21 * vy21 + w12 * vy12 + w22 * vy22);
    }

    /**
     * 
     */
    private void projectCartogramGrid() {
        // Project each point in the cartogram grid.
        double[][] x = iCartogramGrid.getXCoordinates();
        double[][] y = iCartogramGrid.getYCoordinates();

        int gridSizeX = x.length;
        int gridSizeY = x[0].length;

        double cellSizeX = iExtent.getWidth() / iGridSize[0];
        double cellSizeY = iExtent.getHeight() / iGridSize[1];

        double minX = iExtent.getMinX();
        double minY = iExtent.getMinY();

        int index = 0;
        for (int j = 0; j < gridSizeY; j++) {
            for (int i = 0; i < gridSizeX; i++) {
                x[i][j] = iGridPointsX[index] * cellSizeX + minX;
                y[i][j] = iGridPointsY[index] * cellSizeY + minY;
                index++;
            }
        }
    }

    /**
     * Estimates the computation progress in percentages and displays the value
     * in the wizard. Works only if a wizard is defined, does nothing otherwise.
     * 
     * @param time
     *            current time from the makeCartogram method.
     */
    private void updateRunningStatus(double time) {
        if (iStatus != null) {
            int perc = (int) Math.round(100.0 * Math.log(time / INITH)
                    / Math.log(EXPECTEDTIME / INITH));
            if (perc > 100) {
                perc = 100;
            }

            int res = (iMaximumStatus - iMinimumStatus) * perc / 100;
            res += iMinimumStatus;
            iStatus.updateRunningStatus(res, iStatusMessage,
                    "Diffusion process: %1$s%% done", perc);
        }
    }
}