package ch.epfl.scapetoad.gui;

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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JWindow;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jump.task.DummyTaskMonitor;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;

import ch.epfl.scapetoad.Cartogram;
import ch.epfl.scapetoad.CartogramLayer;
import ch.epfl.scapetoad.ICartogramStatus;

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
     * The logger.
     */
    private static Log logger;

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
        logger = LogFactory.getLog(ScapeToad.class);
        logger.debug("Starting...");

        // Launch the GUI if no arguments are givens
        if (aArgs.length == 0) {
            ScapeToad.launchGUI();
            return;
        }

        commandLine(aArgs);
    }

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
     * Launch the cartogram GUI.
     */
    private static void launchGUI() {
        // Set the Look & Feel and application name for for MacOS X
        // environments, on other environments, this properties does not have
        // any effect
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name",
                "ScapeToad");

        // Create a new JUMP workbench, an exception might be thrown when
        // creating a new workbench
        try {
            @SuppressWarnings("unused")
            JUMPWorkbench jump = new JUMPWorkbench("ScapeToad", new String[0],
                    new ImageIcon("resources/scapetoad-icon-small.gif"),
                    new JWindow(), new DummyTaskMonitor());
        } catch (Exception e) {
            logger.error("Exception creating JUMP Workbench", e);
            System.exit(-1);
            return;
        }

        // Create a new layer manager
        AppContext.layerManager = new LayerManager();
        AppContext.layerManager.addCategory("Original layers");

        // Create the main window and display it
        AppContext.mainWindow = new MainWindow();
        AppContext.mainWindow.setVisible(true);
    }

    /**
     * Command line management.
     * 
     * @param aArgs
     *            the arguments
     */
    @SuppressWarnings("static-access")
    private static void commandLine(String aArgs[]) {
        // Configure the options
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("master")
                .withType(File.class).isRequired()
                .withDescription("master shape file").hasArg()
                .withArgName("master.shp").create('m'));
        options.addOption(OptionBuilder.withLongOpt("attribute").isRequired()
                .withDescription("master attribute").hasArg()
                .withArgName("Pop2006").create('a'));
        options.addOption(OptionBuilder.withLongOpt("cartogram")
                .withType(File.class).isRequired()
                .withDescription("cartogram destination shape file").hasArg()
                .withArgName("cartogram.shp").create('c'));
        options.addOption("h", "help", false, "print this message");

        HelpFormatter formatter = new HelpFormatter();
        String commandLineSyntax = "master slaves [options]";
        CommandLine line;
        try {
            CommandLineParser parser = new BasicParser();
            line = parser.parse(options, aArgs);
            line.getArgList();
        } catch (ParseException e) {
            logger.error("Exception parsing command line arguments: ", e);

            System.out.println();
            formatter.printHelp(commandLineSyntax, options);
            System.exit(-1);
            return;
        }

        // Print the help
        if (line.hasOption('h')) {
            formatter.printHelp(commandLineSyntax, options);
            return;
        }

        File masterLayerFile = null;
        String masterAttribute = null;
        File cartogramLayerFile = null;
        try {
            masterLayerFile = (File) line.getParsedOptionValue("m");
            masterAttribute = line.getOptionValue('a');
            cartogramLayerFile = (File) line.getParsedOptionValue("c");
        } catch (ParseException e) {
            logger.error("Exception parsing command line arguments: ", e);
            System.exit(-1);
            return;
        }

        launch(masterLayerFile, masterAttribute, cartogramLayerFile);
    }

    /**
     * Launch the cartogram in the CLI mode.
     * 
     * @param aMasterLayerFile
     *            the master layer shape file
     * @param aMasterAttribute
     *            the master attribute
     * @param aCartogramLayerFile
     *            the destination cartogram layer shape file
     */
    private static void launch(File aMasterLayerFile, String aMasterAttribute,
            File aCartogramLayerFile) {
        // Create a new layer manager
        AppContext.layerManager = new LayerManager();
        AppContext.layerManager.addCategory("Original layers");

        try {
            // Load the master layer
            Layer masterLayer = IOManager.readShapefile(aMasterLayerFile
                    .getAbsolutePath());

            // Configure the cartogram
            ICartogramStatus status = new CartogramCLIStatus();
            Cartogram cartogram = new Cartogram(status);
            cartogram.setMasterLayer(Utils.convert(masterLayer));
            cartogram.setMasterAttribute(aMasterAttribute);
            cartogram.setMasterAttributeIsDensityValue(false);
            cartogram.setAdvancedOptionsEnabled(false);

            // Compute and finish
            List<CartogramLayer> layers = cartogram.compute(false, false);

            // Store the result
            IOManager.writeShapefile(
                    Utils.convert(layers.get(0), AppContext.layerManager)
                            .getFeatureCollectionWrapper(), aCartogramLayerFile
                            .getAbsolutePath());

            // Close the program
            System.exit(0);
        } catch (Exception e) {
            logger.error("Exception running cartogram: ", e);
            System.exit(-1);
            return;
        }
    }
}