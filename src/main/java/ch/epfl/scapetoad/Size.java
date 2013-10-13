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
     * 
     */
    public int iX = 0;
    /**
     * 
     */
    public int iY = 0;

    /**
     * Initializes the size using the provided integer values.
     * 
     * @param x
     *            the X size
     * @param y
     *            the Y size
     */
    public Size(int x, int y) {
        iX = x;
        iY = y;
    }

}
