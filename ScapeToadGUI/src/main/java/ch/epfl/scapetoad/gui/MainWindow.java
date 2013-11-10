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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerTreeModel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelContext;
import com.vividsolutions.jump.workbench.ui.TreeLayerNamePanel;
import com.vividsolutions.jump.workbench.ui.renderer.RenderingManager;

/**
 * The main window of the ScapeToad application.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-11-28
 */
public class MainWindow extends JFrame implements LayerViewPanelContext {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The logger.
     */
    private static Log logger = LogFactory.getLog(MainWindow.class);

    /**
     * The main panel.
     */
    private MainPanel iMainPanel = null;

    /**
     * The main munz.
     */
    private MainMenu iMainMenu = null;

    /**
     * The default constructor for the main window.
     */
    public MainWindow() {
        // Set the window parameters
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("ScapeToad");
        setSize(640, 480);
        setLocation(20, 30);

        // Add the window content
        iMainPanel = new MainPanel(this);
        getContentPane().add(iMainPanel);

        // Create the menu bar
        iMainMenu = new MainMenu();
        setJMenuBar(iMainMenu);

        update();
    }

    /**
     * Displays a message indicating the status of current operations in the
     * status bar.
     * 
     * @param aMessage
     *            the message to display in the status bar.
     */
    @Override
    public void setStatusMessage(String aMessage) {
        // Nothing to do
    }

    /**
     * Notifies the user in an alert box about a minor issue.
     * 
     * @param warning
     *            the warning message to display.
     */
    @Override
    public void warnUser(String warning) {
        // Nothing to do
    }

    /**
     * Handles an exception.
     */
    @Override
    public void handleThrowable(Throwable aThrowable) {
        logger.error("", aThrowable);
    }

    /**
     * Updates the window content.
     */
    public void update() {
        try {
            AppContext.mapPanel.update();
            AppContext.layerViewPanel.getViewport().update();
            AppContext.layerViewPanel.repaint();
            iMainMenu.update();
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    /**
     * Displays a dialog for exporting a Shape file.
     */
    public static void exportShapeFile() {
        new ExportShapeFileDialog().setVisible(true);
    }

    /**
     * Displays a dialog for export the layers as a SVG file.
     */
    public static void exportSvgFile() {
        new ExportSVGFileDialog().setVisible(true);
    }
}

/**
 * The main panel.
 */
class MainPanel extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * 
     * @param aContentFrame
     *            the content frame
     */
    protected MainPanel(JFrame aContentFrame) {
        // Set the layout parameters
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(5, 20, 20, 20));

        // Create the toolbar.
        MainToolbar toolbar = new MainToolbar();
        toolbar.setAlignmentX(LEFT_ALIGNMENT);
        add(toolbar);

        // Create the two scroll views with the content pane

        AppContext.mapPanel = new MapPanel(aContentFrame);

        JScrollPane rightScrollPane = new JScrollPane(AppContext.mapPanel);
        JScrollPane leftScrollPane = new JScrollPane(new LayerListPanel());

        // Set the minimum sizes for the scroll panes
        Dimension minimumSize = new Dimension(150, 200);
        leftScrollPane.setMinimumSize(minimumSize);
        rightScrollPane.setMinimumSize(minimumSize);

        // Create a new split pane and add the two scroll panes
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                leftScrollPane, rightScrollPane);

        // Set the divider location for our split pane
        splitPane.setDividerLocation(150);

        splitPane.setAlignmentX(LEFT_ALIGNMENT);

        add(splitPane);
    }
}

/**
 * This class represents the layer list panel which is located inside the main
 * window (table of contents for the layers).
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-11-28
 */
class LayerListPanel extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The default constructor for the layer list panel.
     */
    protected LayerListPanel() {
        AppContext.layerListPanel = new TreeLayerNamePanel(
                AppContext.layerViewPanel, new LayerTreeModel(
                        AppContext.layerViewPanel), new RenderingManager(
                        AppContext.layerViewPanel),
                new TreeMap<Object, Object>());

        add(AppContext.layerListPanel);
    }
}

/**
 * This class represents the map panel which is located inside the main window.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-11-28
 */
class MapPanel extends JPanel {

    /**
     * The logger.
     */
    private static Log logger = LogFactory.getLog(MapPanel.class);

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The default constructor for the map panel.
     * 
     * @param aContentFrame
     *            the content frame
     */
    protected MapPanel(JFrame aContentFrame) {
        setSize(aContentFrame.getSize());
        setLayout(null);
        setLocation(0, 0);
        setBackground(Color.WHITE);

        // Create the new layer view panel taken from the JUMP project
        AppContext.layerViewPanel = new LayerViewPanel(AppContext.layerManager,
                AppContext.mainWindow);

        // If this flag is false, the viewport will be zoomed to the
        // extent of the layer when a new layer is added
        AppContext.layerViewPanel.setViewportInitialized(false);
        AppContext.layerViewPanel.setLocation(0, 0);
        AppContext.layerViewPanel.setSize(this.getSize());

        // Add the layer view panel to the panel
        this.add(AppContext.layerViewPanel);

        // Zoom to full extent, a NoninvertibleTransformException might be
        // thrown
        try {
            AppContext.layerViewPanel.getViewport().zoomToFullExtent();
            AppContext.layerViewPanel.getViewport().update();
        } catch (NoninvertibleTransformException exception) {
            logger.error("Error while zooming to full extent.", exception);
            AppContext.mainWindow
                    .warnUser("Error while zooming to full extent.");
        }
    }

    /**
     * Update this panel.
     */
    protected void update() {
        AppContext.layerViewPanel.setSize(this.getSize());
    }
}

/**
 * This class represents the main toolbar at the top of the main window.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-11-28
 */
class MainToolbar extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The default constructor for the map panel.
     */
    protected MainToolbar() {
        ClassLoader loader = this.getClass().getClassLoader();

        // Full extent button
        JButton fullExtentButton = new JButton("Full extent", new ImageIcon(
                loader.getResource("resources/full-extent-32.gif")));
        fullExtentButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        fullExtentButton.setHorizontalTextPosition(SwingConstants.CENTER);
        fullExtentButton.setSize(53, 53);
        fullExtentButton.setFocusable(false);
        fullExtentButton.setContentAreaFilled(false);
        fullExtentButton.setBorderPainted(false);
        fullExtentButton.addActionListener(new ActionZoomToFullExtent());
        add(fullExtentButton);

        // Add layer button
        JButton addLayerButton = new JButton("Add layer", new ImageIcon(
                loader.getResource("resources/addLayer-32.png")));
        addLayerButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        addLayerButton.setHorizontalTextPosition(SwingConstants.CENTER);
        addLayerButton.setSize(53, 53);
        addLayerButton.setFocusable(false);
        addLayerButton.setContentAreaFilled(false);
        addLayerButton.setBorderPainted(false);
        addLayerButton.addActionListener(new ActionLayerAdd());
        add(addLayerButton);

        // Create cartogram button
        JButton createCartogramButton = new JButton(
                "Create cartogram",
                new ImageIcon(loader.getResource("resources/buildAndGo-32.png")));
        createCartogramButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        createCartogramButton.setHorizontalTextPosition(SwingConstants.CENTER);
        createCartogramButton.setSize(53, 53);
        createCartogramButton.setFocusable(false);
        createCartogramButton.setContentAreaFilled(false);
        createCartogramButton.setBorderPainted(false);
        createCartogramButton.addActionListener(new ActionCreateCartogram());
        add(createCartogramButton);

        // Export to SVG button
        JButton svgButton = new JButton("Export to SVG", new ImageIcon(
                loader.getResource("resources/export-to-svg-32.png")));
        svgButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        svgButton.setHorizontalTextPosition(SwingConstants.CENTER);
        svgButton.setSize(53, 53);
        svgButton.setFocusable(false);
        svgButton.setContentAreaFilled(false);
        svgButton.setBorderPainted(false);
        svgButton.addActionListener(new ActionExportAsSvg());
        add(svgButton);

        // Export to SHP button
        JButton shpButton = new JButton("Export to Shape", new ImageIcon(
                loader.getResource("resources/export-to-shp-32.png")));
        shpButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        shpButton.setHorizontalTextPosition(SwingConstants.CENTER);
        shpButton.setSize(53, 53);
        shpButton.setFocusable(false);
        shpButton.setContentAreaFilled(false);
        shpButton.setBorderPainted(false);
        shpButton.addActionListener(new ActionLayerSave());
        add(shpButton);
    }
}

/**
 * This class is an action zooming to the full layer extent.
 * 
 * @author christian@swisscarto.ch
 */
class ActionZoomToFullExtent extends AbstractAction {

    /**
     * The logger
     */
    private static Log logger = LogFactory.getLog(ActionZoomToFullExtent.class);

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void actionPerformed(ActionEvent aEvent) {
        AppContext.mainWindow.update();

        try {
            AppContext.layerViewPanel.getViewport().zoomToFullExtent();
        } catch (NoninvertibleTransformException exception) {
            logger.error("", exception);
        }
    }
}

/**
 * This class is an action creating the cartogram.
 */
class ActionCreateCartogram extends AbstractAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void actionPerformed(ActionEvent aEvent) {
        if (AppContext.cartogramWizard == null
                || AppContext.cartogramWizard.isVisible() == false) {
            AppContext.cartogramWizard = null;
            AppContext.cartogramWizard = new CartogramWizard();
        }

        AppContext.cartogramWizard.setVisible(true);
    }
}

/**
 * Dialog window for specifying the layer to export into a Shape file.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2008-05-20
 */
class ExportShapeFileDialog extends JDialog {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The layer menu.
     */
    private JComboBox iLayerMenu;

    /**
     * Constructor for the export Shape file dialog.
     */
    protected ExportShapeFileDialog() {
        // Set the window parameters
        setTitle("Export layer as Shape file");
        setSize(300, 140);
        setLocation(40, 50);
        setResizable(false);
        setLayout(null);
        setModal(true);

        // Cancel button
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setLocation(70, 80);
        cancelButton.setSize(100, 26);
        cancelButton.addActionListener(new ExportShapeFileDialogAction(
                "closeDialogWithoutSaving", this));
        cancelButton.setMnemonic(KeyEvent.VK_ESCAPE);
        add(cancelButton);

        // OK button
        JButton okButton = new JButton("OK");
        okButton.setLocation(180, 80);
        okButton.setSize(100, 26);
        okButton.addActionListener(new ExportShapeFileDialogAction(
                "closeDialogWithSaving", this));
        okButton.setMnemonic(KeyEvent.VK_ENTER);
        add(okButton);

        // Add a pop-up menu with the list of available layers
        JLabel layerMenuLabel = new JLabel("Select the layer to export:");
        layerMenuLabel.setFont(new Font(null, Font.PLAIN, 11));
        layerMenuLabel.setBounds(20, 20, 210, 14);
        add(layerMenuLabel);

        iLayerMenu = new JComboBox();
        iLayerMenu.setBounds(20, 40, 210, 26);
        iLayerMenu.setFont(new Font(null, Font.PLAIN, 11));
        iLayerMenu.setMaximumRowCount(20);

        int nlayers = AppContext.layerManager.size();
        for (int lyrcnt = 0; lyrcnt < nlayers; lyrcnt++) {
            Layer lyr = AppContext.layerManager.getLayer(lyrcnt);
            iLayerMenu.addItem(lyr.getName());
        }

        // If there is no layer for the cartogram deformation, add a menu item
        // "<none>" and disable the "Next" button
        if (iLayerMenu.getItemCount() == 0) {
            iLayerMenu.addItem("<none>");
            okButton.setEnabled(false);
        } else {
            okButton.setEnabled(true);
        }

        add(iLayerMenu);
    }

    /**
     * 
     */
    protected void saveLayer() {
        String layerName = (String) iLayerMenu.getSelectedItem();
        if (layerName == "<none>") {
            return;
        }
        IOManager.saveShapefile(AppContext.layerManager.getLayer(layerName)
                .getFeatureCollectionWrapper());
    }
}

/**
 * The actions for the export shape file dialog.
 */
class ExportShapeFileDialogAction extends AbstractAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The action to perform.
     */
    private String iAction = "closeDialogWithoutSaving";

    /**
     * The export shape file dialog.
     */
    private ExportShapeFileDialog iDialog = null;

    /**
     * The default creator for the action.
     * 
     * @param aAction
     *            defines the action to perform. Can be "showDialog", if we
     *            should create a new dialog and display it. Is
     *            "closeDialogWithSaving" if we should close the dialog and save
     *            the changes. is "closeDialogWithoutSaving" if we should
     *            discard the changes and close the dialog.
     * @param aDialog
     *            a reference to the dialog or null if it does not yet exist
     *            (for the showDialog action).
     */
    protected ExportShapeFileDialogAction(String aAction,
            ExportShapeFileDialog aDialog) {
        iAction = aAction;
        iDialog = aDialog;
    }

    /**
     * Method which performs the previously specified action.
     */
    @Override
    public void actionPerformed(ActionEvent aEvent) {
        if (iAction == "closeDialogWithoutSaving") {
            iDialog.setVisible(false);
            iDialog.dispose();
        } else if (iAction == "closeDialogWithSaving") {
            iDialog.saveLayer();
            iDialog.setVisible(false);
            iDialog.dispose();
        }
    }
}

/**
 * Dialog window for specifying the layers to export into a SVG file.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2008-05-20
 */
class ExportSVGFileDialog extends JDialog {

    /**
     * The logger.
     */
    private static Log logger = LogFactory.getLog(ExportSVGFileDialog.class);

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The OK button.
     */
    private JButton iOkButton;

    /**
     * The cancel button.
     */
    private JButton iCancelButton;

    /**
     * The check box list.
     */
    private List<JCheckBox> iCheckBoxList;

    /**
     * Constructor for the export SVG file dialog.
     */
    protected ExportSVGFileDialog() {
        // Set the window parameters
        setTitle("Export layers to SVG file");
        setSize(300, 400);
        setLocation(40, 50);
        setResizable(false);
        setLayout(null);
        setModal(true);

        // List with simultaneous layer

        // Create a new pane containing the check boxes with the layers
        JPanel layerListPanel = new JPanel(new GridLayout(0, 1));
        // Create the checkbox array
        iCheckBoxList = new ArrayList<JCheckBox>();

        Font smallFont = new Font(null, Font.PLAIN, 11);
        int nlayers = AppContext.layerManager.size();
        Layer layer;
        JCheckBox checkbox;
        for (int i = 0; i < nlayers; i++) {
            layer = AppContext.layerManager.getLayer(i);
            checkbox = new JCheckBox(layer.getName());
            checkbox.setFont(smallFont);

            if (layer.isVisible()) {
                checkbox.setSelected(true);
            } else {
                checkbox.setSelected(false);
            }

            iCheckBoxList.add(checkbox);
            layerListPanel.add(checkbox);
        }

        // Compute the height of the new scroll pane.
        int scrollPaneHeight = nlayers * 26;
        if (nlayers == 0) {
            scrollPaneHeight = 260;
        }

        if (scrollPaneHeight > 260) {
            scrollPaneHeight = 260;
        }

        // Create a new scroll pane where we will display the list of layers
        JScrollPane layerListScrollPane = new JScrollPane(layerListPanel);
        layerListScrollPane.setSize(260, scrollPaneHeight);
        layerListScrollPane.setLocation(20, 50);
        layerListScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0,
                0));
        add(layerListScrollPane);

        // Label for the layers to deform
        JLabel layerListLabel = new JLabel(
                "Select layers to export into a SVG file:");
        layerListLabel.setSize(260, 14);
        layerListLabel.setFont(smallFont);
        layerListLabel.setLocation(20, 20);
        add(layerListLabel);

        // Cancel button
        iCancelButton = new JButton("Cancel");
        iCancelButton.setLocation(70, 330);
        iCancelButton.setSize(100, 26);
        iCancelButton.addActionListener(new ExportSVGFileDialogAction(
                "closeDialogWithoutSaving", this));
        iCancelButton.setMnemonic(KeyEvent.VK_ESCAPE);
        add(iCancelButton);

        // Ok button
        iOkButton = new JButton("OK");
        iOkButton.setLocation(180, 330);
        iOkButton.setSize(100, 26);
        iOkButton.addActionListener(new ExportSVGFileDialogAction(
                "closeDialogWithSaving", this));
        iOkButton.setMnemonic(KeyEvent.VK_ENTER);
        add(iOkButton);

        // Label for no present layers
        if (nlayers == 0) {
            JLabel noLayerLabel = new JLabel("No layers to be exported.");
            noLayerLabel.setSize(260, 14);
            noLayerLabel.setFont(smallFont);
            noLayerLabel.setLocation(20, 50);
            iOkButton.setEnabled(false);
            add(noLayerLabel);
        }
    }

    /**
     * Export the layers as SVG file.
     */
    protected void exportLayers() {
        iOkButton.setEnabled(false);
        iCancelButton.setEnabled(false);

        if (iCheckBoxList.size() > 0) {
            List<Layer> layers = new ArrayList<Layer>();
            Layer layer;
            for (JCheckBox checkBox : iCheckBoxList) {
                if (checkBox.isSelected()) {
                    layer = AppContext.layerManager
                            .getLayer(checkBox.getText());
                    if (layer == null) {
                        logger.warn("Layer " + checkBox.getText()
                                + " not found.");
                    } else {
                        layers.add(layer);
                    }
                }
            }

            IOManager.saveSVG(layers.toArray(new Layer[layers.size()]));
        }
    }
}

/**
 * The actions for the export SVG file dialog.
 */
class ExportSVGFileDialogAction extends AbstractAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The action to perform.
     */
    private String iAction = "closeDialogWithoutSaving";

    /**
     * The export SVG file dialog.
     */
    private ExportSVGFileDialog iDialog = null;

    /**
     * Constructor.
     * 
     * @param aAction
     *            the action to perform
     * @param aDialog
     *            the dialog
     */
    protected ExportSVGFileDialogAction(String aAction,
            ExportSVGFileDialog aDialog) {
        iAction = aAction;
        iDialog = aDialog;
    }

    /**
     * Method which performs the previously specified action.
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        if (iAction == "closeDialogWithoutSaving") {
            iDialog.setVisible(false);
            iDialog.dispose();
        }

        else if (iAction == "closeDialogWithSaving") {
            iDialog.exportLayers();
            iDialog.setVisible(false);
            iDialog.dispose();
        }
    }
}