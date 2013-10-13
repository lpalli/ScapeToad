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

import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

/**
 * The main menu of the ScapeToad application.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-11-28
 */
public class MainMenu extends JMenuBar {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The remove layer menu item
     */
    private JMenuItem iRemoveLayer;

    /**
     * The save layer menu item
     */
    private JMenuItem iSaveLayer;

    /**
     * The export as SVG menu item
     */
    private JMenuItem iExportAsSvg;

    /**
     * The default constructor for the main menu. Creates the main menu.
     */
    public MainMenu() {
        // Create the FILE menu.
        JMenu fileMenu = new JMenu("File");

        // File > Add layer...
        JMenuItem menuFile_AddLayer = new JMenuItem("Add layer...");
        menuFile_AddLayer.addActionListener(new ActionLayerAdd());
        menuFile_AddLayer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        fileMenu.add(menuFile_AddLayer);

        // File > Remove layer
        iRemoveLayer = new JMenuItem("Remove layer");
        iRemoveLayer.addActionListener(new ActionLayerRemove());
        iRemoveLayer.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_BACK_SPACE, Toolkit.getDefaultToolkit()
                        .getMenuShortcutKeyMask()));
        fileMenu.add(iRemoveLayer);

        // Separator
        JMenuItem sepMenu = new JMenuItem("-");
        fileMenu.add(sepMenu);

        // File > Save layer...
        iSaveLayer = new JMenuItem("Export layer as Shape file...");
        iSaveLayer.addActionListener(new ActionLayerSave());
        iSaveLayer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit
                .getDefaultToolkit().getMenuShortcutKeyMask()));
        fileMenu.add(iSaveLayer);

        // File > Export as SVG...
        iExportAsSvg = new JMenuItem("Export to SVG...");
        iExportAsSvg.addActionListener(new ActionExportAsSvg());
        iExportAsSvg.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        fileMenu.add(iExportAsSvg);

        // Add a quit menu if we are not on a Mac.
        // (on a Mac, there is a default quit menu under the program name's
        // menu).
        if (System.getProperty("os.name").indexOf("Mac OS") == -1
                || AppContext.DEBUG) {
            JMenuItem sepMenu2 = new JMenuItem("-");
            fileMenu.add(sepMenu2);

            // File > Quit
            JMenuItem menuFile_Quit = new JMenuItem("Quit ScapeToad");
            menuFile_Quit.addActionListener(new ActionQuit());
            menuFile_Quit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            fileMenu.add(menuFile_Quit);
        }

        add(fileMenu);

        // Add the Help menu.
        JMenu helpMenu = new JMenu("Help");
        JMenuItem menuHelp_Help = new JMenuItem("ScapeToad Help");
        menuHelp_Help.addActionListener(new ActionShowHelp());
        menuHelp_Help.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_HELP,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        helpMenu.add(menuHelp_Help);
        JMenuItem sepMenu3 = new JMenuItem("-");
        helpMenu.add(sepMenu3);

        JMenuItem menuHelp_About = new JMenuItem("About...");
        menuHelp_About.addActionListener(new ActionShowAbout());

        helpMenu.add(menuHelp_About);
        add(helpMenu);
    }

    /**
     * Enables/disables the menu items.
     */
    public void enableMenus() {
        if (AppContext.layerManager.getLayers().size() > 0) {
            iRemoveLayer.setEnabled(true);
            iSaveLayer.setEnabled(true);
            iExportAsSvg.setEnabled(true);
        } else {
            iRemoveLayer.setEnabled(false);
            iSaveLayer.setEnabled(false);
            iExportAsSvg.setEnabled(false);
        }
    }
}