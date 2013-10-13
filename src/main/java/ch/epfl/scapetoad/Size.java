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
    public int iX = 0;

    /**
     * the Y size
     */
    public int iY = 0;

    /**
     * Initializes the size using the provided integer values.
     * 
     * @param aX
     *            the X size
     * @param aY
     *            the Y size
     */
    public Size(int aX, int aY) {
        iX = aX;
        iY = aY;
    }
}