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

package ch.epfl.scapetoad;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.NoninvertibleTransformException;
import java.util.Vector;

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
     * 
     */
    boolean _enabled = false;

    /**
     * 
     */
    String _attrName;
    /**
     * 
     */
    Vector<Double> _limits;
    /**
     * 
     */
    Vector<BasicStyle> _colors;
    /**
     * 
     */
    BasicStyle _defaultStyle;

    /**
     * 
     */
    private Stroke _fillStroke = new BasicStroke(1);

    /**
     * 
     */
    public SizeErrorStyle() {
        _limits = new Vector<Double>();
        _colors = new Vector<BasicStyle>();
        _defaultStyle = new BasicStyle(Color.ORANGE);
    }

    @Override
    public Object clone() {
        return null;
    }

    @Override
    public void initialize(Layer layer) {
        // Nothing to do
    }

    @Override
    public boolean isEnabled() {
        return _enabled;
    }

    @Override
    public void paint(Feature f, Graphics2D g, Viewport viewport)
            throws NoninvertibleTransformException {

        BasicStyle s = getStyleForFeature(f);

        StyleUtil.paint(f.getGeometry(), g, viewport, s.isRenderingFill(),
                _fillStroke, s.getFillColor(), s.isRenderingLine(),
                s.getLineStroke(), s.getLineColor());

    }

    @Override
    public void setEnabled(boolean enabled) {
        _enabled = enabled;
    }

    /**
     * @param defaultStyle
     *            the style
     */
    public void setDefaultStyle(BasicStyle defaultStyle) {
        _defaultStyle = defaultStyle;
    }

    /**
     * @return the number of colors
     */
    public int getNumberOfColors() {
        return _colors.size();
    }

    /**
     * @param index
     *            the index
     * @return the color
     */
    public BasicStyle getColorAtIndex(int index) {
        return _colors.get(index);
    }

    /**
     * @param color
     *            the color
     */
    public void addColor(BasicStyle color) {
        _colors.add(color);
    }

    /**
     * @param color
     *            the color
     * @param index
     *            the index
     */
    public void setColorAtIndex(BasicStyle color, int index) {
        _colors.set(index, color);
    }

    /**
     * @return the number of limits
     */
    public int getNumberOfLimits() {
        return _limits.size();
    }

    /**
     * @param index
     *            the index
     * @return the limit at the specified index
     */
    public Double getLimitAtIndex(int index) {
        return _limits.get(index);
    }

    /**
     * @param limit
     *            the limit
     */
    public void addLimit(Double limit) {
        _limits.add(limit);
    }

    /**
     * @param limit
     *            the limit
     * @param index
     *            the index
     */
    public void setLimitAtIndex(Double limit, int index) {
        _limits.set(index, limit);
    }

    /**
     * @param attrName
     *            the attribute name
     */
    public void setAttributeName(String attrName) {
        _attrName = attrName;
    }

    /**
     * @param f
     *            the feature
     * @return the style
     */
    private BasicStyle getStyleForFeature(Feature f) {

        // Get the attribute value.
        Double value = (Double) f.getAttribute(_attrName);

        boolean valueFound = false;
        int limitIndex = 0;
        while (valueFound == false && limitIndex < _limits.size()) {
            Double limit = _limits.get(limitIndex);
            if (value.doubleValue() <= limit.doubleValue()) {
                valueFound = true;
            }

            limitIndex++;
        }

        return _colors.get(limitIndex);

    }

}
