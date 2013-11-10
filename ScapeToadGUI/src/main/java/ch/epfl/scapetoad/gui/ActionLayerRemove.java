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

package ch.epfl.scapetoad.gui;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;

import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layer;

/**
 * This class is an action performed on a remove layer event.
 * 
 * @author christian@swisscarto.ch
 */
public class ActionLayerRemove extends AbstractAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Removes the selected layer from the layer manager.
     */
    @Override
    public void actionPerformed(ActionEvent aEvent) {
        for (Layer layer : AppContext.layerListPanel.getSelectedLayers()) {
            AppContext.layerManager.remove(layer);
        }

        // Remove all empty categories (but we keep at least one).
        @SuppressWarnings("unchecked")
        List<Category> categories = AppContext.layerManager.getCategories();
        if (categories.size() > 1) {
            for (Category category : categories) {
                AppContext.layerManager.removeIfEmpty(category);
            }
        }

        // If there is only one empty category left, rename it to
        // "Original layers".
        if (categories.size() == 1) {
            Category category = categories.get(0);
            if (category.isEmpty()) {
                category.setName("Original layers");
            }
        }

        AppContext.mainWindow.update();
    }
}