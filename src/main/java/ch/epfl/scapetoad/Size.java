//
//  Size.java
//  ScapeToad
//
//  Created by Christian on 29.10.08.
//  Copyright 2008 __MyCompanyName__. All rights reserved.
//

package ch.epfl.scapetoad;

/**
 * Defines a two-dimensional size with an x and y integer component.
 */
public class Size {

    /**
     * The X size.
     */
    private int iX = 0;

    /**
     * the Y size
     */
    private int iY = 0;

    /**
     * Initializes the size using the provided integer values.
     * 
     * @param aX
     *            the X size
     * @param aY
     *            the Y size
     */
    public Size(int aX, int aY) {
        setX(aX);
        setY(aY);
    }

    /**
     * Get the X size.
     * 
     * @return the X size
     */
    public int getX() {
        return iX;
    }

    /**
     * Set the X size.
     * 
     * @param aX
     *            the X size
     */
    public void setX(int aX) {
        iX = aX;
    }

    /**
     * Get the Y size.
     * 
     * @return the Y size
     */
    public int getY() {
        return iY;
    }

    /**
     * Set the Y size.
     * 
     * @param aY
     *            the Y size
     */
    public void setY(int aY) {
        iY = aY;
    }
}