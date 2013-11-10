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

import javax.swing.AbstractAction;

/**
 * This class is an action performed on a Export as SVG event.
 * 
 * @author christian@swisscarto.ch
 */
public class ActionExportAsSvg extends AbstractAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * This method is automatically called after a export as SVG event.
     * 
     * Writes the currently selected layers into a SVG file. A Save file dialog
     * is presented to the user in order to choose the place on the disc.
     */
    @Override
    public void actionPerformed(ActionEvent aEvent) {
        MainWindow.exportSvgFile();
    }
}