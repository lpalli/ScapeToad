package ch.epfl.scapetoad;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JWindow;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jump.task.DummyTaskMonitor;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.model.LayerManager;

/**
 * This class contains the main method of the ScapeToad application.
 * 
 * For launching ScapeToad with the GUI, just type the following command: java
 * -Xmx1024M -jar ScapeToad.jar
 * 
 * @author christian@361degres.ch
 * @version v1.2.0, 2010-03-03
 */
public class ScapeToad {

    /**
     * Initialize the logging system.
     * 
     * @throws ClassNotFoundException
     *             if the logger configurator class isn't found
     * @throws SecurityException
     *             if the logger configuration file can't be accessed
     * @throws NoSuchMethodException
     *             if the logger configurator method isn't found
     * @throws IllegalArgumentException
     *             if the logger configurator parameters are wrongs
     * @throws IllegalAccessException
     *             if the logger configurator isn't accessible
     * @throws InvocationTargetException
     *             if the logger configurator can't be invoked
     */
    private static void initLog() throws ClassNotFoundException,
            SecurityException, NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        // Get the log4j configuration file URL
        URL log4jFile = ScapeToad.class.getClassLoader().getResource(
                "resources/log4j.xml");
        // Get the class and method
        Class<?> domConfigurator = Class
                .forName("org.apache.log4j.xml.DOMConfigurator");
        Method configure = domConfigurator.getMethod("configure", URL.class);
        // Initialize the logging system using the log4j configuration file
        configure.invoke(null, log4jFile);
    }

    /**
     * The main method for the ScapeToad application.
     * 
     * @param aArgs
     *            the arguments
     */
    public static void main(String aArgs[]) {
        // Initialize the logging system
        try {
            initLog();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log logger = LogFactory.getLog(ScapeToad.class);
        logger.debug("Starting...");

        // Set the Look & Feel properties.
        // This is specific for MacOS X environments.
        // On other environments, this property does not have any effect.
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        // Create a new JUMP workbench.
        ImageIcon icon = new ImageIcon("resources/scapetoad-icon-small.gif");
        JWindow window = new JWindow();
        DummyTaskMonitor tm = new DummyTaskMonitor();

        // An exception might be thrown when creating a new workbench.
        try {
            @SuppressWarnings("unused")
            JUMPWorkbench jump = new JUMPWorkbench("ScapeToad", aArgs, icon,
                    window, tm);
        } catch (Exception e) {
            logger.error("Exception creating JUMP Workbench", e);
            System.exit(-1);
            return;
        }

        // Create a new layer manager.
        AppContext.layerManager = new LayerManager();
        AppContext.layerManager.addCategory("Original layers");

        // Create the main window and display it.
        AppContext.mainWindow = new MainWindow();
        AppContext.mainWindow.setVisible(true);
    }
}