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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.Ostermiller.util.Browser;

/**
 * This class is an action performed on a quit event.
 * 
 * @author christian@swisscarto.ch
 */
public class ActionShowHelp extends AbstractAction {

    /**
     * The logger
     */
    private static Log logger = LogFactory.getLog(ActionShowHelp.class);

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Opens the browser and points it to the ScapeToad help web site.
     */
    @Override
    public void actionPerformed(ActionEvent aEvent) {
        try {
            Browser.init();
            Browser.displayURL("http://scapetoad.choros.ch/help/");
        } catch (Exception exception) {
            logger.error("", exception);
        }
    }
}