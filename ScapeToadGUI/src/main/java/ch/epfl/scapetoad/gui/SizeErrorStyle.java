/*

	Copyright 2007 91NORD

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

package ch.epfl.scapetoad.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;
import com.vividsolutions.jump.workbench.ui.renderer.style.StyleUtil;

/**
 * The SizeErrorStyle produces the thematic map based on the SizeError
 * attribute.
 */
public class SizeErrorStyle implements Style {

    /**
     * <code>true</code> if the style is enabled.
     */
    private boolean iEnabled = false;

    /**
     * The attribute name used to set the style.
     */
    private String iAttributeName;

    /**
     * The style value limits.
     */
    private List<Double> iLimits = new ArrayList<Double>();

    /**
     * the style colors.
     */
    private List<BasicStyle> iColors = new ArrayList<BasicStyle>();

    /**
     * The style fill stroke.
     */
    private Stroke iFillStroke = new BasicStroke(1);

    /**
     * Constructor.
     * 
     * @param aAttributeName
     *            the attribute name used to set the style
     */
    public SizeErrorStyle(String aAttributeName) {
        iAttributeName = aAttributeName;
    }

    @Override
    public Object clone() {
        return null;
    }

    @Override
    public void initialize(Layer aLayer) {
        // Nothing to do
    }

    @Override
    public boolean isEnabled() {
        return iEnabled;
    }

    @Override
    public void paint(Feature aFeature, Graphics2D aGraphics, Viewport aViewport)
            throws NoninvertibleTransformException {
        BasicStyle style = getStyleForFeature(aFeature);

        StyleUtil.paint(aFeature.getGeometry(), aGraphics, aViewport,
                style.isRenderingFill(), iFillStroke, style.getFillColor(),
                style.isRenderingLine(), style.getLineStroke(),
                style.getLineColor());
    }

    @Override
    public void setEnabled(boolean aEnabled) {
        iEnabled = aEnabled;
    }

    /**
     * Add a color.
     * 
     * @param aColor
     *            the color
     */
    public void addColor(Color aColor) {
        iColors.add(new BasicStyle(aColor));
    }

    /**
     * add a limit.
     * 
     * @param aLimit
     *            the limit
     */
    public void addLimit(double aLimit) {
        iLimits.add(aLimit);
    }

    /**
     * Returns the style for a JUMP feature.
     * 
     * @param aFeature
     *            the feature
     * @return the style
     */
    private BasicStyle getStyleForFeature(Feature aFeature) {
        // Get the attribute value.
        double value = (Double) aFeature.getAttribute(iAttributeName);

        boolean founded = false;
        int index = 0;
        while (founded == false && index < iLimits.size()) {
            if (value <= iLimits.get(index)) {
                founded = true;
            }
            index++;
        }

        return iColors.get(index);
    }
}