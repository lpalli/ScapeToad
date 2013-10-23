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

package ch.epfl.scapetoad;

import java.awt.FileDialog;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.Ostermiller.util.Browser;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.Layer;

/**
 * The cartogram wizard guiding the user through the process of cartogram
 * creation.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-11-30
 */
public class CartogramWizard extends JFrame {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 
     */
    private int iCurrentStep = -1;

    /**
     * 
     */
    private CartogramWizardPanelZero iPanelZero = null;

    /**
     * 
     */
    private CartogramWizardPanelOne iPanelOne = null;

    /**
     * 
     */
    private CartogramWizardPanelTwo iPanelTwo = null;

    /**
     * 
     */
    private CartogramWizardPanelThree iPanelThree = null;

    /**
     * 
     */
    private CartogramWizardPanelFour iPanelFour = null;

    /**
     * 
     */
    private Cartogram iCartogram = null;

    /**
     * The panel shown during cartogram computation.
     */
    protected final CartogramWizardRunningPanel iRunningPanel = new CartogramWizardRunningPanel();

    /**
     * The panel shown after cartogram computation.
     */
    private final CartogramWizardFinishedPanel iFinishedPanel = new CartogramWizardFinishedPanel();

    /**
     * The name of the selected cartogram layer (the master layer).
     */
    private String iCartogramLayerName = null;

    /**
     * The name of the selected cartogram attribute.
     */
    private String iCartogramAttributeName = null;

    /**
     * Some parameters for the cartogram computation.
     */
    private AbstractList<Layer> iSimultaneousLayers = null;

    /**
     * 
     */
    private AbstractList<Layer> iConstrainedDeformationLayers = null;

    /**
     * 
     */
    private int iAmountOfDeformation = 50;

    /**
     * 
     */
    private int iCartogramGridSizeX = 1000;

    /**
     * 
     */
    private int iCartogramGridSizeY = 1000;

    /**
     * 
     */
    private boolean iAdvancedOptionsEnabled = false;

    /**
     * Defines whether we should create a layer with the deformation grid.
     */
    private boolean iCreateGridLayer = true;

    /**
     * Defines the size of the deformation grid which can be created as
     * additional layer.
     */
    private int iDeformationGridSize = 100;

    /**
     * The icon panel at the left side of each wizard window.
     */
    private ScapeToadIconPanel iScapeToadIconPanel = null;

    /**
     * The step icon panel at the upper right side of the wizard.
     */
    private WizardStepIconPanel iWizardStepIconPanel = null;

    /**
     * 
     */
    private JButton iCancelButton = null;

    /**
     * 
     */
    private String iMissingValue = "";

    /**
     * Interface for bias value.
     */
    public double bias = 0.000001;

    /**
     * The default constructor for the wizard.
     */
    public CartogramWizard() {
        // Set the window parameters.
        setTitle("ScapeToad _ Cartogram Wizard");
        setSize(640, 480);
        setLocation(30, 40);
        setResizable(false);
        setLayout(null);
        // this.setModal(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new CartogramWizardWindowListener());

        // Adding the cartogram wizard to the app context.
        AppContext.cartogramWizard = this;

        // Add icon panel at the left of the wizard window.
        // This panel contains the ScapeToad icon.
        if (iScapeToadIconPanel == null) {
            iScapeToadIconPanel = new ScapeToadIconPanel();
        }

        iScapeToadIconPanel.setLocation(30, 90);
        add(iScapeToadIconPanel);

        // Add title panel.
        CartogramWizardTitlePanel titlePanel = new CartogramWizardTitlePanel();

        titlePanel.setLocation(30, 20);
        add(titlePanel);

        // Add icon panel at the left of the wizard window.
        // This panel contains the ScapeToad icon.
        iWizardStepIconPanel = new WizardStepIconPanel();
        iWizardStepIconPanel.setLocation(380, 20);
        add(iWizardStepIconPanel);

        // Ajouter l'introduction au wizard.
        // Explication des étapes à suivre :
        // 1. Sélectionner la couche des polygones (master layer).
        // 2. Sélectionner l'information statistique.
        // 3. Sélection des couches à transformer simultanément.

        iPanelZero = new CartogramWizardPanelZero(this);
        getContentPane().add(iPanelZero);

        iCurrentStep = 0;

        // Add the running panel which is already created.
        iRunningPanel.setVisible(false);
        add(iRunningPanel);

        // Add the finished panel which is already created.
        iFinishedPanel.setVisible(false);
        add(iFinishedPanel);

        // Add the Cancel button.
        iCancelButton = new JButton("Cancel");
        iCancelButton.setLocation(30, 404);
        iCancelButton.setSize(100, 26);
        iCancelButton.addActionListener(new CartogramWizardCloseAction());
        getContentPane().add(iCancelButton);

    }

    /**
     * Returns the wizard panel 2.
     * 
     * @return the panel
     */
    protected CartogramWizardPanelTwo getPanelTwo() {
        return iPanelTwo;
    }

    /**
     * Returns the wizard panel 4.
     * 
     * @return the panel
     */
    protected CartogramWizardPanelFour getPanelFour() {
        return iPanelFour;
    }

    /**
     * Returns the wizard's running panel.
     * 
     * @return the panel
     */
    protected CartogramWizardRunningPanel getRunningPanel() {
        return iRunningPanel;
    }

    /**
     * Returns the wizard step icon panel.
     * 
     * @return the panel
     */
    public WizardStepIconPanel getWizardStepIconPanel() {
        return iWizardStepIconPanel;
    }

    /**
     * Switches the wizard to the given step. The step number must be between 0
     * (introduction) and 3.
     * 
     * @param step
     *            the step
     */
    public void goToStep(int step) {
        if (step < 0 || step > 4) {
            return;
        }

        if (iCurrentStep == step) {
            return;
        }

        // Hide the current step.
        switch (iCurrentStep) {
            case 0:
                iPanelZero.setVisible(false);
                break;
            case 1:
                iPanelOne.setVisible(false);
                break;
            case 2:
                iPanelTwo.setVisible(false);
                break;
            case 3:
                iPanelThree.setVisible(false);
                break;
            case 4:
                iPanelFour.setVisible(false);
                break;
            default:
                throw new UnsupportedOperationException();
        }

        // Show the new step.
        switch (step) {
            case 0:
                if (iPanelZero == null) {
                    iPanelZero = new CartogramWizardPanelZero(this);
                    getContentPane().add(iPanelZero);
                }
                iPanelZero.setVisible(true);
                iCurrentStep = 0;
                iWizardStepIconPanel.setStepIcon(1);
                break;
            case 1:
                if (iPanelOne == null) {
                    iPanelOne = new CartogramWizardPanelOne(this);
                    getContentPane().add(iPanelOne);
                }
                iPanelOne.setVisible(true);
                iCurrentStep = 1;
                iWizardStepIconPanel.setStepIcon(2);
                break;
            case 2:
                if (iPanelTwo == null) {
                    iPanelTwo = new CartogramWizardPanelTwo(this);
                    getContentPane().add(iPanelTwo);
                }
                iPanelTwo.setVisible(true);
                iCurrentStep = 2;
                iWizardStepIconPanel.setStepIcon(3);
                break;
            case 3:
                if (iPanelThree == null) {
                    iPanelThree = new CartogramWizardPanelThree(this);
                    getContentPane().add(iPanelThree);
                }
                iPanelThree.setVisible(true);
                iCurrentStep = 3;
                iWizardStepIconPanel.setStepIcon(4);
                break;

            case 4:
                if (iPanelFour == null) {
                    iPanelFour = new CartogramWizardPanelFour(this);
                    getContentPane().add(iPanelFour);
                }
                iPanelFour.setVisible(true);
                iCurrentStep = 4;
                iWizardStepIconPanel.setStepIcon(5);
                break;

            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * Shows the finished panel.
     */
    public void goToFinishedPanel() {
        iRunningPanel.setVisible(false);
        iFinishedPanel.setVisible(true);
        iWizardStepIconPanel.setStepIcon(7);
    }

    /**
     * Returns the cartogram computation process.
     * 
     * @return the cartogram
     */
    public Cartogram getCartogram() {
        return iCartogram;
    }

    /**
     * Sets the cartogram computation process.
     * 
     * @param cg
     *            the cartogram
     */
    public void setCartogram(Cartogram cg) {
        iCartogram = cg;
    }

    /**
     * Returns the name of the selected cartogram layer. This is the master
     * layer for the cartogram transformation.
     * 
     * @return the cartogram layer name
     */
    public String getCartogramLayerName() {
        return iCartogramLayerName;
    }

    /**
     * Sets the cartogram layer name.
     * 
     * @param layerName
     *            the layer name
     */
    public void setCartogramLayerName(String layerName) {
        iCartogramLayerName = layerName;
    }

    /**
     * Returns the cartogram attribute name.
     * 
     * @return the cartogram attribute name
     */
    public String getCartogramAttributeName() {
        return iCartogramAttributeName;
    }

    /**
     * Sets the cartogram attribute name.
     * 
     * @param attrName
     *            the attribute name
     */
    public void setCartogramAttributeName(String attrName) {
        iCartogramAttributeName = attrName;
    }

    /**
     * Returns the parameter for the creation of a deformation grid layer.
     * 
     * @return whether we should create or not a deformation grid layer.
     */
    public boolean getCreateGridLayer() {
        return iCreateGridLayer;
    }

    /**
     * Sets the parameter for the creation of a deformation grid layer.
     * 
     * @param createGridLayer
     *            true if we should create a deformation grid layer, false
     *            otherwise.
     */
    public void setCreateGridLayer(boolean createGridLayer) {
        iCreateGridLayer = createGridLayer;
    }

    /**
     * Returns the size of the deformation grid which can be created as an
     * additional layer.
     * 
     * @return the size of the deformation grid.
     */
    public int getDeformationGridSize() {
        return iDeformationGridSize;
    }

    /**
     * Sets the size of the deformation grid which can be created as an
     * additional layer. The effective grid size is adapted to the layer extent;
     * this parameter sets the larger side of the layer extent rectangle.
     * 
     * @param gridSize
     *            the size of the deformation grid.
     */
    public void setDeformationGridSize(int gridSize) {
        iDeformationGridSize = gridSize;
    }

    /**
     * Updates the progress bar and the progress labels during cartogram
     * computation.
     * 
     * @param progress
     *            the progress status (integer 0-1000).
     * @param label1
     *            the progress main message.
     * @param label2
     *            the progress secondary message.
     */
    public void updateRunningStatus(final int progress, final String label1,
            final String label2) {
        Runnable doSetRunningStatus = new Runnable() {
            @Override
            public void run() {
                iRunningPanel.updateProgressBar(progress);
                iRunningPanel.updateProgressLabel1(label1);
                iRunningPanel.updateProgressLabel2(label2);
            }
        };

        SwingUtilities.invokeLater(doSetRunningStatus);
    }

    /**
     * Returns the list of simultaneous layers.
     * 
     * @return the layers
     */
    public AbstractList<Layer> getSimultaneousLayers() {
        return iSimultaneousLayers;
    }

    /**
     * Sets the list of simultaneous layers.
     * 
     * @param layers
     *            the layer
     */
    public void setSimultaneousLayers(AbstractList<Layer> layers) {
        iSimultaneousLayers = layers;
    }

    /**
     * Returns the list of constrained deformation layers.
     * 
     * @return the layers
     */
    public AbstractList<Layer> getConstrainedDeformationLayers() {
        return iConstrainedDeformationLayers;
    }

    /**
     * Sets the list of constrained deformation layers.
     * 
     * @param layers
     *            the layers
     */
    public void setConstrainedDeformationLayers(AbstractList<Layer> layers) {
        iConstrainedDeformationLayers = layers;
    }

    /**
     * Returns the amount of deformation, an integer value between 0 (low
     * deformation) and 100 (high deformation).
     * 
     * @return the deformation
     */
    public int getAmountOfDeformation() {
        return iAmountOfDeformation;
    }

    /**
     * Changes the amount of deformation. This must be an integer value between
     * 0 and 100.
     * 
     * @param deformation
     *            the deformation
     */
    public void setAmountOfDeformation(int deformation) {
        iAmountOfDeformation = deformation;
    }

    /**
     * Returns the cartogram grid size in x direction. The cartogram grid is the
     * grid which is deformed by the cartogram computing process. It is not the
     * same grid as the one used by Gastner's algorithm. The cartogram grid can
     * have an arbitrary size; it is only limited by the available amount of
     * memory and disk space.
     * 
     * @return the grid X size
     */
    public int getCartogramGridSizeInX() {
        return iCartogramGridSizeX;
    }

    /**
     * Changes the cartogram grid size in x direction.
     * 
     * @param gridSizeX
     *            the grid X size
     */
    public void setCartogramGridSizeInX(int gridSizeX) {
        iCartogramGridSizeX = gridSizeX;
    }

    /**
     * Returns the cartogram grid size in y direction.
     * 
     * @return the grid Y size
     */
    public int getCartogramGridSizeInY() {
        return iCartogramGridSizeY;
    }

    /**
     * Changes the cartogram grid size in y direction.
     * 
     * @param gridSizeY
     *            the grid Y size
     */
    public void setCartogramGridSizeInY(int gridSizeY) {
        iCartogramGridSizeY = gridSizeY;
    }

    /**
     * Returns true if the advanced options for the cartogram computation are
     * enabled.
     * 
     * @return true if the advanced parameters should be taken in account, and
     *         false otherwise.
     */
    public boolean getAdvancedOptionsEnabled() {
        return iAdvancedOptionsEnabled;
    }

    /**
     * Defines whether the advances options should be taken into account.
     * 
     * @param enabled
     *            true if the advanced options should be taken into account, and
     *            false if the advanced options should be ignored.
     */
    public void setAdvancedOptionsEnabled(boolean enabled) {
        iAdvancedOptionsEnabled = enabled;

        if (enabled) {
            iPanelFour.enableAmountOfDeformationSlider(false);
        } else {
            iPanelFour.enableAmountOfDeformationSlider(true);
        }
    }

    /**
     * Sets a cartogram computation error message for the user.
     * 
     * @param title
     *            the title
     * @param message
     *            the message
     * @param stackTrace
     *            the stack trace
     */
    public void setComputationError(String title, String message,
            String stackTrace) {
        iFinishedPanel.setErrorOccured(true);
        iFinishedPanel.setErrorMessage(title, message, stackTrace);
    }

    /**
     * Returns the cancel button of the cartogram wizard.
     * 
     * @return the button
     */
    public JButton getCancelButton() {
        return iCancelButton;
    }

    /**
     * @return the missing value
     */
    public String getMissingValue() {
        return iMissingValue;
    }

    /**
     * @param value
     *            the value
     */
    public void setMissingValue(String value) {
        iMissingValue = value;
    }
}

/**
 * This class represents a panel containing the ScapeToad icon with a size of 97
 * x 152 pixels. It is used to be displayed in the different wizard steps. The
 * panel is always located at the left side of the wizard window.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-12-01
 */
class ScapeToadIconPanel extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The default constructor for the ScapeToad icon panel.
     */
    ScapeToadIconPanel() {
        // Setting some panel parameters.
        setSize(100, 155);
        setLayout(null);

        // Loading the ScapeToad icon from the resources.
        ClassLoader cldr = getClass().getClassLoader();
        URL iconURL = cldr.getResource("resources/ScapeToad-25p.png");
        ImageIcon scapeToadIcon = new ImageIcon(iconURL);

        // Create a new label containing the icon.
        JLabel iconLabel = new JLabel(scapeToadIcon);

        // Setting the label parameters.
        iconLabel.setLayout(null);
        iconLabel.setSize(97, 152);
        iconLabel.setLocation(1, 1);

        // Add the icon label to this panel.
        add(iconLabel);
    }
}

/**
 * This class represents the overall cartogram wizard title for all wizard
 * steps.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-12-01
 */
class CartogramWizardTitlePanel extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The default constructor for the panel.
     */
    CartogramWizardTitlePanel() {
        // Setting panel parameters.
        setSize(350, 45);
        setLayout(null);

        // Create the title text.
        JLabel title = new JLabel("Cartogram creation wizard");
        title.setFont(new Font(null, Font.BOLD, 13));
        title.setLocation(0, 0);
        title.setSize(400, 20);
        add(title);
    }
}

/**
 *
 */
class WizardStepIconPanel extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 
     */
    private JLabel iIconLabel;

    /**
     * 
     */
    private ImageIcon iIcon1;

    /**
     * 
     */
    private ImageIcon iIcon2;

    /**
     * 
     */
    private ImageIcon iIcon3;

    /**
     * 
     */
    private ImageIcon iIcon4;

    /**
     * 
     */
    private ImageIcon iIcon5;

    /**
     * 
     */
    private ImageIcon iIcon6;

    /**
     * 
     */
    private ImageIcon iIcon7;

    /**
     * The default constructor for the ScapeToad icon panel.
     */
    WizardStepIconPanel() {
        // Setting some panel parameters.
        setSize(220, 30);
        setLayout(null);

        // Loading the step icons from the resources.
        ClassLoader cldr = this.getClass().getClassLoader();
        iIcon1 = new ImageIcon(cldr.getResource("resources/WizardStep1.png"));
        iIcon2 = new ImageIcon(cldr.getResource("resources/WizardStep2.png"));
        iIcon3 = new ImageIcon(cldr.getResource("resources/WizardStep3.png"));
        iIcon4 = new ImageIcon(cldr.getResource("resources/WizardStep4.png"));
        iIcon5 = new ImageIcon(cldr.getResource("resources/WizardStep5.png"));
        iIcon6 = new ImageIcon(cldr.getResource("resources/WizardStep6.png"));
        iIcon7 = new ImageIcon(cldr.getResource("resources/WizardStep7.png"));

        // Create a new label containing the icon.
        iIconLabel = new JLabel(iIcon1);

        // Setting the label parameters.
        iIconLabel.setLayout(null);
        iIconLabel.setSize(206, 27);
        iIconLabel.setLocation(1, 1);

        // Add the icon label to this panel.
        add(iIconLabel);
    }

    /**
     * @param step
     *            the number of steps
     */
    public void setStepIcon(int step) {
        switch (step) {
            case 1:
                iIconLabel.setIcon(iIcon1);
                break;
            case 2:
                iIconLabel.setIcon(iIcon2);
                break;
            case 3:
                iIconLabel.setIcon(iIcon3);
                break;
            case 4:
                iIconLabel.setIcon(iIcon4);
                break;
            case 5:
                iIconLabel.setIcon(iIcon5);
                break;
            case 6:
                iIconLabel.setIcon(iIcon6);
                break;
            case 7:
                iIconLabel.setIcon(iIcon7);
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }
}

/**
 * This class represents the first screen in the cartogram wizard. It contains
 * general information on cartograms and on the steps needed in cartogram
 * creation.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-12-01
 */
class CartogramWizardPanelZero extends JPanel implements HyperlinkListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The default constructor for the panel.
     * 
     * @param contentFrame
     *            the content frame
     */
    CartogramWizardPanelZero(JFrame contentFrame) {
        setLocation(160, 90);
        setSize(440, 340);
        setLayout(null);

        // Add the Next button
        JButton nextButton = new JButton("Next >");
        nextButton.setLocation(320, 314);
        nextButton.setSize(120, 26);
        nextButton.setMnemonic(KeyEvent.VK_ENTER);

        nextButton.addActionListener(new CartogramWizardGoToStepAction(
                (CartogramWizard) contentFrame, 1));
        add(nextButton);

        // Create the text pane which displays the message.
        // The message itself is read from a RTF file.

        JTextPane text = new JTextPane();

        // Get the wizard content from a text file.
        ClassLoader cldr = this.getClass().getClassLoader();
        URL wizardStepZeroURL = cldr
                .getResource("resources/WizardIntroduction.html");

        // Get the content from the text file.
        String wizardStepZeroContent = null;
        try {
            InputStream inStream = wizardStepZeroURL.openStream();
            StringBuffer inBuffer = new StringBuffer();
            int c;

            while ((c = inStream.read()) != -1) {
                inBuffer.append((char) c);
            }

            inStream.close();

            wizardStepZeroContent = inBuffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        text.setContentType("text/html");
        text.setText(wizardStepZeroContent);
        text.setEditable(false);
        text.addHyperlinkListener(this);
        text.setBackground(null);
        text.setLocation(0, 0);
        text.setSize(440, 300);
        add(text);

        // Add the help button
        java.net.URL imageURL = cldr.getResource("resources/help-22.png");
        ImageIcon helpIcon = new ImageIcon(imageURL);

        JButton helpButton = new JButton(helpIcon);

        helpButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        helpButton.setHorizontalTextPosition(SwingConstants.CENTER);
        helpButton.setSize(30, 30);
        helpButton.setLocation(0, 312);
        helpButton.setFocusable(false);
        helpButton.setContentAreaFilled(false);
        helpButton.setBorderPainted(false);

        helpButton.addActionListener(new CartogramWizardShowURL(
                "http://scapetoad.choros.ch/help/a-cartogram-creation.php"));

        add(helpButton);
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        try {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                Browser.init();
                Browser.displayURL(e.getURL().toString());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

/**
 * This class represents the first screen in the cartogram wizard. It contains a
 * pop-up menu for selecting the master layer.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-12-01
 */
class CartogramWizardPanelOne extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 
     */
    CartogramWizard iCartogramWizard = null;

    /**
     * 
     */
    JComboBox iLayerMenu = null;

    /**
     * The "Next" button.
     */
    JButton iNextButton = null;

    /**
     * The default constructor for the panel.
     * 
     * @param contentFrame
     *            the content frame
     */
    CartogramWizardPanelOne(JFrame contentFrame) {
        iCartogramWizard = (CartogramWizard) contentFrame;

        int width = 440;
        int height = 340;

        setLocation(160, 90);
        setSize(width, height);
        setLayout(null);

        // Add the Next button
        iNextButton = new JButton("Next >");
        iNextButton.setLocation(320, 314);
        iNextButton.setSize(120, 26);
        iNextButton.setMnemonic(KeyEvent.VK_ACCEPT);

        iNextButton.addActionListener(new CartogramWizardGoToStepAction(
                iCartogramWizard, 2));
        add(iNextButton);

        // Add the Back button
        JButton backButton = new JButton("< Back");
        backButton.setLocation(195, 314);
        backButton.setSize(120, 26);
        backButton.addActionListener(new CartogramWizardGoToStepAction(
                iCartogramWizard, 0));
        add(backButton);

        // Add a pop-up menu with the list of available layers.
        JLabel layerMenuLabel = new JLabel("Spatial coverage:");
        layerMenuLabel.setFont(new Font(null, Font.PLAIN, 11));
        layerMenuLabel.setBounds(0, 0, 190, 14);
        add(layerMenuLabel);

        iLayerMenu = new JComboBox();
        iLayerMenu.setBounds(0, 20, 190, 26);
        iLayerMenu.setFont(new Font(null, Font.PLAIN, 11));
        iLayerMenu.setMaximumRowCount(20);

        // Add all polygon layers to the list.
        int nlayers = AppContext.layerManager.size();

        // Check for each layer whether it is a polygon layer or not.
        for (int lyrcnt = 0; lyrcnt < nlayers; lyrcnt++) {
            Layer lyr = AppContext.layerManager.getLayer(lyrcnt);
            FeatureCollectionWrapper fcw = lyr.getFeatureCollectionWrapper();
            int nfeat = fcw.size();
            if (nfeat > 0) {
                Feature feat = (Feature) fcw.getFeatures().get(0);
                Geometry geom = feat.getGeometry();
                if (geom.getArea() != 0.0) {
                    iLayerMenu.addItem(lyr.getName());
                }
            }

        }

        // If there is no layer for the cartogram deformation,
        // add a menu item "<none>" and disable the "Next" button.
        if (iLayerMenu.getItemCount() == 0) {
            iLayerMenu.addItem("<none>");
            iNextButton.setEnabled(false);
        } else {
            iNextButton.setEnabled(true);
        }

        add(iLayerMenu);

        // Adding the polygon image
        ClassLoader cldr = this.getClass().getClassLoader();
        URL iconURL = cldr.getResource("resources/Topology.png");
        ImageIcon topologyImage = new ImageIcon(iconURL);

        // Create a new label containing the image.
        JLabel iconLabel = new JLabel(topologyImage);

        // Setting the label parameters.
        iconLabel.setLayout(null);
        iconLabel.setSize(192, 239);
        iconLabel.setLocation(240, 30);

        // Add the icon label to this panel.
        add(iconLabel);

        // Adding the explanatory text.
        // The message itself is read from a RTF file.
        JTextPane layerMenuTextPane = new JTextPane();

        // Get the content from the text file.
        String layerMenuText = null;
        try {
            InputStream inStream = cldr.getResource(
                    "resources/LayerMenuText.rtf").openStream();

            StringBuffer inBuffer = new StringBuffer();
            int c;
            while ((c = inStream.read()) != -1) {
                inBuffer.append((char) c);
            }

            inStream.close();
            layerMenuText = inBuffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        layerMenuTextPane.setContentType("text/rtf");
        layerMenuTextPane.setText(layerMenuText);
        layerMenuTextPane.setEditable(false);
        layerMenuTextPane.setFont(new Font(null, Font.PLAIN, 11));
        layerMenuTextPane.setBackground(null);
        layerMenuTextPane.setLocation(0, 60);
        layerMenuTextPane.setSize(220, 240);
        add(layerMenuTextPane);

        // Add the Help button
        java.net.URL imageURL = cldr.getResource("resources/help-22.png");
        ImageIcon helpIcon = new ImageIcon(imageURL);
        JButton helpButton = new JButton(helpIcon);
        helpButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        helpButton.setHorizontalTextPosition(SwingConstants.CENTER);
        helpButton.setSize(30, 30);
        helpButton.setLocation(0, 312);
        helpButton.setFocusable(false);
        helpButton.setContentAreaFilled(false);
        helpButton.setBorderPainted(false);
        helpButton
                .addActionListener(new CartogramWizardShowURL(
                        "http://scapetoad.choros.ch/help/a-cartogram-creation.php#cartogram-layer"));
        add(helpButton);
    }

    /**
     * If this panel is hidden, set the selected layer name in the cartogram
     * wizard. This is an overriden method from its super class. Once the job
     * done, we call the super class' setVisible method.
     */
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            updateLayerList();

            // If there is no layer for the cartogram deformation,
            // add a menu item "<none>" and disable the "Next" button.
            if (iLayerMenu.getItemCount() == 0) {
                iLayerMenu.addItem("<none>");
                iNextButton.setEnabled(false);
            } else {
                iNextButton.setEnabled(true);
            }
        }

        if (!visible) {
            iCartogramWizard.setCartogramLayerName((String) iLayerMenu
                    .getSelectedItem());
        }

        super.setVisible(visible);
    }

    /**
     * Updates the pop-up menu with the cartogram layers.
     */
    public void updateLayerList() {
        String selectedLayer = null;
        if (iLayerMenu != null) {
            selectedLayer = (String) iLayerMenu.getSelectedItem();
        }

        iLayerMenu.removeAllItems();

        // Add all polygon layers to the list.
        int nlayers = AppContext.layerManager.size();

        // Check for each layer whether it is a polygon layer or not.
        for (int lyrcnt = 0; lyrcnt < nlayers; lyrcnt++) {
            Layer lyr = AppContext.layerManager.getLayer(lyrcnt);
            FeatureCollectionWrapper fcw = lyr.getFeatureCollectionWrapper();
            int nfeat = fcw.size();
            if (nfeat > 0) {
                Feature feat = (Feature) fcw.getFeatures().get(0);
                Geometry geom = feat.getGeometry();
                if (geom.getArea() != 0.0) {
                    String layerName = lyr.getName();
                    iLayerMenu.addItem(layerName);
                    if (layerName == selectedLayer) {
                        iLayerMenu.setSelectedItem(layerName);
                    }
                }
            }
        }
    }
}

/**
 * This class represents the third screen in the cartogram wizard. It is used
 * for the selection of cartogram attribute.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-12-01
 */
class CartogramWizardPanelTwo extends JPanel implements HyperlinkListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 
     */
    private CartogramWizard iCartogramWizard = null;
    /**
     * 
     */
    private JComboBox iAttributeMenu = null;
    /**
     * 
     */
    private String iCurrentCartogramLayer = null;

    /**
     * The "Next" button.
     */
    private JButton iNextButton = null;

    /**
     * The attribute type radio button.
     */
    private ButtonGroup iAttributeTypeButtonGroup = null;
    /**
     * 
     */
    private JRadioButton iAttributeTypeDensityButton = null;
    /**
     * 
     */
    private JRadioButton iAttributeTypePopulationButton = null;

    /**
     * The default constructor for the panel.
     * 
     * @param contentFrame
     *            the content frame
     */
    CartogramWizardPanelTwo(JFrame contentFrame) {
        iCartogramWizard = (CartogramWizard) contentFrame;

        this.setLocation(160, 90);
        this.setSize(440, 340);
        setLayout(null);

        // Add the Next button
        iNextButton = new JButton("Next >");
        iNextButton.setLocation(320, 314);
        iNextButton.setSize(120, 26);
        iNextButton.setMnemonic(KeyEvent.VK_ACCEPT);
        iNextButton.addActionListener(new CartogramWizardGoToStepAction(
                (CartogramWizard) contentFrame, 3));
        add(iNextButton);

        // Add the Back button
        JButton backButton = new JButton("< Back");
        backButton.setLocation(195, 314);
        backButton.setSize(120, 26);
        backButton.addActionListener(new CartogramWizardGoToStepAction(
                (CartogramWizard) contentFrame, 1));
        add(backButton);

        // Create the attribute label
        JLabel attributeLabel = new JLabel("Cartogram attribute:");
        attributeLabel.setFont(new Font(null, Font.PLAIN, 11));
        attributeLabel.setBounds(0, 0, 190, 14);
        add(attributeLabel);

        // Create the attribute pop-up menu
        iAttributeMenu = new JComboBox();
        iAttributeMenu.setBounds(0, 20, 190, 26);
        iAttributeMenu.setFont(new Font(null, Font.PLAIN, 11));
        iAttributeMenu.setMaximumRowCount(20);

        // Find out the current cartogram layer name.
        iCurrentCartogramLayer = iCartogramWizard.getCartogramLayerName();

        // Get the numerical attributes of the current cartogram layer.
        if (iCurrentCartogramLayer != null && iCurrentCartogramLayer != ""
                && iCurrentCartogramLayer != "<none>") {
            Layer lyr = AppContext.layerManager
                    .getLayer(iCurrentCartogramLayer);

            FeatureSchema fs = lyr.getFeatureCollectionWrapper()
                    .getFeatureSchema();

            int nattrs = fs.getAttributeCount();

            for (int attrcnt = 0; attrcnt < nattrs; attrcnt++) {

                AttributeType attrtype = fs.getAttributeType(attrcnt);
                if (attrtype == AttributeType.DOUBLE
                        || attrtype == AttributeType.INTEGER) {
                    iAttributeMenu.addItem(fs.getAttributeName(attrcnt));
                }
            }
        }

        if (iAttributeMenu.getItemCount() == 0) {
            iAttributeMenu.addItem("<none>");
            iNextButton.setEnabled(false);
        } else {
            iNextButton.setEnabled(true);
        }

        // Create the attribute type label
        JLabel attributeTypeLabel = new JLabel("Attribute type:");
        attributeTypeLabel.setFont(new Font(null, Font.PLAIN, 11));
        attributeTypeLabel.setBounds(220, 0, 190, 14);
        add(attributeTypeLabel);

        // Create the attribute type radio buttons.
        iAttributeTypePopulationButton = new JRadioButton("Mass");
        iAttributeTypePopulationButton.setSelected(true);
        iAttributeTypePopulationButton.setFont(new Font(null, Font.PLAIN, 11));
        iAttributeTypePopulationButton.setBounds(220, 20, 190, 20);

        iAttributeTypeDensityButton = new JRadioButton("Density");
        iAttributeTypeDensityButton.setSelected(false);
        iAttributeTypeDensityButton.setFont(new Font(null, Font.PLAIN, 11));
        iAttributeTypeDensityButton.setBounds(220, 45, 190, 20);

        iAttributeTypeButtonGroup = new ButtonGroup();
        iAttributeTypeButtonGroup.add(iAttributeTypePopulationButton);
        iAttributeTypeButtonGroup.add(iAttributeTypeDensityButton);

        add(iAttributeTypePopulationButton);
        add(iAttributeTypeDensityButton);

        add(iAttributeMenu);

        // Create the text pane which displays the attribute message.
        // The message itself is read from a RTF file.

        JTextPane attributeMenuTextPane = new JTextPane();

        // Get the wizard content from a text file.
        ClassLoader cldr = this.getClass().getClassLoader();

        // Get the content from the text file.
        String attributeMenuText = null;
        try {
            InputStream inStream = cldr.getResource(
                    "resources/AttributeMenuText.rtf").openStream();

            StringBuffer inBuffer = new StringBuffer();
            int c;

            while ((c = inStream.read()) != -1) {
                inBuffer.append((char) c);
            }

            inStream.close();
            attributeMenuText = inBuffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        attributeMenuTextPane.setContentType("text/rtf");
        attributeMenuTextPane.setText(attributeMenuText);
        attributeMenuTextPane.setEditable(false);
        attributeMenuTextPane.setFont(new Font(null, Font.PLAIN, 11));
        attributeMenuTextPane.setBackground(null);
        attributeMenuTextPane.setLocation(0, 80);
        attributeMenuTextPane.setSize(190, 100);
        this.add(attributeMenuTextPane);

        // Create the text pane which displays the attribute type message.
        // The message itself is read from a RTF file.

        JTextPane attributeTypeTextPane = new JTextPane();

        // Get the content from the text file.
        String attributeTypeText = null;
        try {
            InputStream inStream = cldr.getResource(
                    "resources/AttributeTypeText.html").openStream();

            StringBuffer inBuffer = new StringBuffer();
            int c;

            while ((c = inStream.read()) != -1) {
                inBuffer.append((char) c);
            }

            inStream.close();
            attributeTypeText = inBuffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        attributeTypeTextPane.setContentType("text/html");
        attributeTypeTextPane.setText(attributeTypeText);
        attributeTypeTextPane.setEditable(false);
        attributeTypeTextPane.addHyperlinkListener(this);
        attributeTypeTextPane.setBackground(null);
        attributeTypeTextPane.setLocation(220, 80);
        attributeTypeTextPane.setSize(190, 200);
        add(attributeTypeTextPane);

        // Add the Help button
        java.net.URL imageURL = cldr.getResource("resources/help-22.png");
        ImageIcon helpIcon = new ImageIcon(imageURL);
        JButton helpButton = new JButton(helpIcon);
        helpButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        helpButton.setHorizontalTextPosition(SwingConstants.CENTER);
        helpButton.setSize(30, 30);
        helpButton.setLocation(0, 312);
        helpButton.setFocusable(false);
        helpButton.setContentAreaFilled(false);
        helpButton.setBorderPainted(false);
        helpButton
                .addActionListener(new CartogramWizardShowURL(
                        "http://scapetoad.choros.ch/help/a-cartogram-creation.php#cartogram-attribute"));
        add(helpButton);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            updateAttributeName();
        } else {
            iCartogramWizard.setCartogramAttributeName((String) iAttributeMenu
                    .getSelectedItem());
        }

        super.setVisible(visible);
    }

    /**
     * 
     */
    public void updateAttributeName() {
        // Find out the current layer name.
        String layerName = iCartogramWizard.getCartogramLayerName();

        if (iCurrentCartogramLayer != layerName) {
            // Change the layer name attribute.
            iCurrentCartogramLayer = layerName;

            // Remove all existing items.
            iAttributeMenu.removeAllItems();

            // Get the numerical attributes of the current cartogram layer.
            if (iCurrentCartogramLayer != null && iCurrentCartogramLayer != ""
                    && iCurrentCartogramLayer != "<none>") {
                Layer lyr = AppContext.layerManager
                        .getLayer(iCurrentCartogramLayer);

                FeatureSchema fs = lyr.getFeatureCollectionWrapper()
                        .getFeatureSchema();

                int nattrs = fs.getAttributeCount();

                for (int attrcnt = 0; attrcnt < nattrs; attrcnt++) {

                    AttributeType attrtype = fs.getAttributeType(attrcnt);
                    if (attrtype == AttributeType.DOUBLE
                            || attrtype == AttributeType.INTEGER) {
                        iAttributeMenu.addItem(fs.getAttributeName(attrcnt));
                    }
                }
            }

            // If there is no attribute we can select,
            // add an item "<none>" and disable the "Next" button.
            if (iAttributeMenu.getItemCount() == 0) {
                iAttributeMenu.addItem("<none>");
                iNextButton.setEnabled(false);
            } else {
                iNextButton.setEnabled(true);
            }
        }
    }

    /**
     * Tells us whether the attribute type is a density or population value.
     * 
     * @return <code>true</code> if the attribute is a density value
     */
    public boolean attributeIsDensityValue() {
        return iAttributeTypeDensityButton.isSelected();
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        try {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                Browser.init();
                Browser.displayURL(e.getURL().toString());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @return the missing value
     */
    public static String getMissingValue() {
        return "";
    }

} // CartogramWizardPanelTwo

/**
 * This class represents the panel for the selection of the layers for
 * simultaneous and constrained transformation. There is also a slider for the
 * amount of deformation and the size for the grid to overlay.
 */
class CartogramWizardPanelThree extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The default constructor for the panel.
     * 
     * @param contentFrame
     *            the content frame
     */
    CartogramWizardPanelThree(JFrame contentFrame) {
        this.setLocation(160, 90);
        this.setSize(440, 340);
        setLayout(null);

        // Button for simultanous layers.
        JButton simLayerButton = new JButton("Layers to transform...");
        simLayerButton.setLocation(0, 0);
        simLayerButton.setSize(240, 26);
        simLayerButton
                .addActionListener(new CartogramWizardSimulaneousLayerAction(
                        "showDialog", null));
        add(simLayerButton);

        // Create the text pane which displays the help text for the
        // simultaneous layers.
        JTextPane simLayerTextPane = new JTextPane();
        ClassLoader cldr = this.getClass().getClassLoader();
        String simLayerText = null;
        try {
            InputStream inStream = cldr.getResource(
                    "resources/SimLayersText.rtf").openStream();
            StringBuffer inBuffer = new StringBuffer();
            int c;
            while ((c = inStream.read()) != -1) {
                inBuffer.append((char) c);
            }
            inStream.close();
            simLayerText = inBuffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        simLayerTextPane.setContentType("text/rtf");
        simLayerTextPane.setText(simLayerText);
        simLayerTextPane.setEditable(false);
        simLayerTextPane.setFont(new Font(null, Font.PLAIN, 11));
        simLayerTextPane.setBackground(null);
        simLayerTextPane.setLocation(40, 35);
        simLayerTextPane.setSize(400, 80);
        add(simLayerTextPane);

        // Button for constrained layers.
        JButton constLayersButton = new JButton("Constrained transformation...");
        constLayersButton.setLocation(0, 140);
        constLayersButton.setSize(240, 26);
        constLayersButton
                .addActionListener(new CartogramWizardConstrainedLayerAction(
                        "showDialog", null));
        add(constLayersButton);

        // Create the text pane which displays the help text for the
        // simultaneous layers.
        JTextPane constLayerTextPane = new JTextPane();
        String constLayerText = null;
        try {
            InputStream inStream = cldr.getResource(
                    "resources/ConstLayersText.rtf").openStream();
            StringBuffer inBuffer = new StringBuffer();
            int c;
            while ((c = inStream.read()) != -1) {
                inBuffer.append((char) c);
            }
            inStream.close();
            constLayerText = inBuffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        constLayerTextPane.setContentType("text/rtf");
        constLayerTextPane.setText(constLayerText);
        constLayerTextPane.setEditable(false);
        constLayerTextPane.setFont(new Font(null, Font.PLAIN, 11));
        constLayerTextPane.setBackground(null);
        constLayerTextPane.setLocation(40, 175);
        constLayerTextPane.setSize(400, 60);
        add(constLayerTextPane);

        // Add the Next button
        JButton computeButton = new JButton("Next >");
        computeButton.setLocation(320, 314);
        computeButton.setSize(120, 26);
        computeButton.setMnemonic(KeyEvent.VK_ENTER);
        computeButton.addActionListener(new CartogramWizardGoToStepAction(
                (CartogramWizard) contentFrame, 4));
        add(computeButton);

        // Add the Back button
        JButton backButton = new JButton("< Back");
        backButton.setLocation(195, 314);
        backButton.setSize(120, 26);
        backButton.addActionListener(new CartogramWizardGoToStepAction(
                (CartogramWizard) contentFrame, 2));
        add(backButton);

        // Add the Help button
        java.net.URL imageURL = cldr.getResource("resources/help-22.png");
        ImageIcon helpIcon = new ImageIcon(imageURL);
        JButton helpButton = new JButton(helpIcon);
        helpButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        helpButton.setHorizontalTextPosition(SwingConstants.CENTER);
        helpButton.setSize(30, 30);
        helpButton.setLocation(0, 312);
        helpButton.setFocusable(false);
        helpButton.setContentAreaFilled(false);
        helpButton.setBorderPainted(false);
        helpButton.addActionListener(new CartogramWizardShowURL(
                "http://scapetoad.choros.ch/help/b-other-layers.php"));
        add(helpButton);
    }
}

/**
 * This class represents the panel for the slider for the amount of deformation
 * the size for the grid to overlay.
 */
class CartogramWizardPanelFour extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 
     */
    private CartogramWizard iCartogramWizard = null;

    /**
     * Slider for the amount of deformation (high area error and low shape error
     * or low area error and high shape error).
     */
    private JSlider iDeformationSlider = null;

    /**
     * The default constructor for the panel.
     * 
     * @param contentFrame
     *            the content frame
     */
    CartogramWizardPanelFour(JFrame contentFrame) {
        iCartogramWizard = (CartogramWizard) contentFrame;

        setLocation(160, 90);
        setSize(440, 340);
        setLayout(null);

        ClassLoader cldr = this.getClass().getClassLoader();

        // Add the slider for the amount of deformation.
        Font smallFont = new Font(null, Font.PLAIN, 11);
        iDeformationSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 50);
        iDeformationSlider.setMajorTickSpacing(25);
        iDeformationSlider.setMinorTickSpacing(5);
        iDeformationSlider.setPaintTicks(true);
        iDeformationSlider.setFont(smallFont);
        iDeformationSlider.setSize(440, 40);
        iDeformationSlider.setLocation(0, 20);

        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        JLabel sliderLabel = new JLabel("Low");
        sliderLabel.setFont(smallFont);
        labelTable.put(new Integer(0), sliderLabel);
        sliderLabel = new JLabel("Medium");
        sliderLabel.setFont(smallFont);
        labelTable.put(new Integer(50), sliderLabel);
        sliderLabel = new JLabel("High");
        sliderLabel.setFont(smallFont);
        labelTable.put(new Integer(100), sliderLabel);

        iDeformationSlider.setLabelTable(labelTable);
        iDeformationSlider.setPaintLabels(true);
        add(iDeformationSlider);

        // Add the label for the amount of deformation.
        JLabel deformationLabel = new JLabel("Transformation quality:");
        deformationLabel.setSize(440, 14);
        deformationLabel.setFont(new Font(null, Font.BOLD, 11));
        deformationLabel.setLocation(0, 0);
        add(deformationLabel);

        // Create the text pane which displays the help text for the
        // amount of deformation.
        JTextPane deformationTextPane = new JTextPane();
        String deformationText = null;
        try {
            InputStream inStream = cldr.getResource(
                    "resources/AmountOfDeformationText.rtf").openStream();
            StringBuffer inBuffer = new StringBuffer();
            int c;
            while ((c = inStream.read()) != -1) {
                inBuffer.append((char) c);
            }
            inStream.close();
            deformationText = inBuffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        deformationTextPane.setContentType("text/rtf");
        deformationTextPane.setText(deformationText);
        deformationTextPane.setEditable(false);
        deformationTextPane.setFont(new Font(null, Font.PLAIN, 11));
        deformationTextPane.setBackground(null);
        deformationTextPane.setLocation(40, 70);
        deformationTextPane.setSize(400, 70);
        add(deformationTextPane);

        // ADVANCED OPTIONS

        // A button and an explanatory text for the advanced options.
        JButton advancedButton = new JButton("Advanced options...");
        advancedButton.setLocation(0, 170);
        advancedButton.setSize(240, 26);
        advancedButton
                .addActionListener(new CartogramWizardAdvancedOptionsAction(
                        "showDialog", null));
        add(advancedButton);

        // Create the text pane which displays the help text for the
        // simultaneous layers.
        JTextPane advancedTextPane = new JTextPane();
        String advancedText = null;
        try {
            InputStream inStream = cldr.getResource(
                    "resources/AdvancedOptionsText.rtf").openStream();
            StringBuffer inBuffer = new StringBuffer();
            int c;
            while ((c = inStream.read()) != -1) {
                inBuffer.append((char) c);
            }
            inStream.close();
            advancedText = inBuffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        advancedTextPane.setContentType("text/rtf");
        advancedTextPane.setText(advancedText);
        advancedTextPane.setEditable(false);
        advancedTextPane.setFont(new Font(null, Font.PLAIN, 11));
        advancedTextPane.setBackground(null);
        advancedTextPane.setLocation(40, 205);
        advancedTextPane.setSize(400, 60);
        add(advancedTextPane);

        // Add the Compute button
        JButton computeButton = new JButton("Compute");
        computeButton.setLocation(320, 314);
        computeButton.setSize(120, 26);
        computeButton.setMnemonic(KeyEvent.VK_ENTER);
        computeButton.addActionListener(new CartogramWizardComputeAction(
                (CartogramWizard) contentFrame));
        add(computeButton);

        // Add the Back button
        JButton backButton = new JButton("< Back");
        backButton.setLocation(195, 314);
        backButton.setSize(120, 26);
        backButton.addActionListener(new CartogramWizardGoToStepAction(
                (CartogramWizard) contentFrame, 3));
        add(backButton);

        // Add the Help button
        java.net.URL imageURL = cldr.getResource("resources/help-22.png");
        ImageIcon helpIcon = new ImageIcon(imageURL);
        JButton helpButton = new JButton(helpIcon);
        helpButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        helpButton.setHorizontalTextPosition(SwingConstants.CENTER);
        helpButton.setSize(30, 30);
        helpButton.setLocation(0, 312);
        helpButton.setFocusable(false);
        helpButton.setContentAreaFilled(false);
        helpButton.setBorderPainted(false);
        helpButton
                .addActionListener(new CartogramWizardShowURL(
                        "http://scapetoad.choros.ch/help/c-transformation-parameters.php"));
        add(helpButton);
    }

    /**
     * If the panel is shown, update the layer list before displaying the panel.
     */
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            // this.updateLayerList();
            // this.updateConstrainedLayerList();
        } else {
            // Update the amount of deformation.
            iCartogramWizard.setAmountOfDeformation(iDeformationSlider
                    .getValue());
        }

        super.setVisible(visible);
    }

    /**
     * @param enable
     *            <code>true</code> to enable the deformation
     */
    public void enableAmountOfDeformationSlider(boolean enable) {
        iDeformationSlider.setEnabled(enable);
    }
}

/**
 * This class represents the panel shown during the cartogram computation. It
 * shows a progress bar and a label explaining the advances in the computation.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-12-01
 */
class CartogramWizardRunningPanel extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The progress bar.
     */
    private JProgressBar iProgressBar = null;

    /**
     * The progress labels. The label 1 is for the current task name. In a
     * lengthy task, the label 2 may be used for more detailed user information.
     */
    private JLabel iProgressLabel1 = null;
    /**
     * 
     */
    private JLabel iProgressLabel2 = null;

    /**
     * The default constructor.
     */
    CartogramWizardRunningPanel() {
        setLocation(160, 90);
        setSize(440, 340);
        setLayout(null);

        // Creating the progress bar.
        iProgressBar = new JProgressBar();
        iProgressBar.setMaximum(1000);
        iProgressBar.setValue(0);
        iProgressBar.setStringPainted(false);
        iProgressBar.setSize(300, 26);
        iProgressBar.setLocation(0, 0);
        add(iProgressBar);

        // Creating the progess label 1.
        iProgressLabel1 = new JLabel("Starting cartogram computation...");
        iProgressLabel1.setFont(new Font(null, Font.BOLD, 11));
        iProgressLabel1.setSize(400, 14);
        iProgressLabel1.setLocation(0, 30);
        add(iProgressLabel1);

        // Creating the progress label 2.
        iProgressLabel2 = new JLabel("");
        iProgressLabel2.setFont(new Font(null, Font.PLAIN, 11));
        iProgressLabel2.setSize(400, 14);
        iProgressLabel2.setLocation(0, 50);
        add(iProgressLabel2);
    }

    /**
     * Updates the progress bar using the nloops parameter. The parameter must
     * be between 0 and 1000.
     * 
     * @param aLoops
     *            the progress status (0-1000).
     */
    public void updateProgressBar(int aLoops) {
        int nloops = aLoops;
        if (nloops < 0) {
            nloops = 0;
        }
        if (nloops > 1000) {
            nloops = 1000;
        }
        iProgressBar.setValue(nloops);
    }

    /**
     * Updates the progress label 1.
     * 
     * @param label1
     *            the label 1
     */
    public void updateProgressLabel1(String label1) {
        iProgressLabel1.setText(label1);
        iProgressLabel1.repaint();
    }

    /**
     * Updates the progress label 2.
     * 
     * @param label2
     *            the label 2
     */
    public void updateProgressLabel2(String label2) {
        iProgressLabel2.setText(label2);

    }
}

/**
 * This class shows the finished panel in the cartogram wizard.
 */
class CartogramWizardFinishedPanel extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Attributes for the text to display and for the report.
     */
    private String iShortMessage = null;

    /**
     * Says whether an error has occurred during the cartogram computation
     * process.
     */
    private boolean iErrorOccured = false;

    /**
     * The title of the error message, if there is one.
     */
    private String iErrorTitle = null;

    /**
     * The error message itself, if there is one.
     */
    private String iErrorMessage = null;

    /**
     * The technical details of the error, if there is one.
     */
    private String iStackTrace = null;

    /**
     * The help button.
     */
    private JButton iHelpButton = null;

    /**
     * The save report button.
     */
    private JButton iSaveReportButton = null;

    /**
     * Initializes the new panel.
     */
    CartogramWizardFinishedPanel() {
        setLocation(160, 90);
        setSize(440, 340);
        setLayout(null);

        // Add the Help button
        ClassLoader cldr = this.getClass().getClassLoader();

        java.net.URL imageURL = cldr.getResource("resources/help-22.png");
        ImageIcon helpIcon = new ImageIcon(imageURL);

        iHelpButton = new JButton(helpIcon);
        iHelpButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        iHelpButton.setHorizontalTextPosition(SwingConstants.CENTER);
        iHelpButton.setSize(30, 30);
        iHelpButton.setLocation(0, 312);
        iHelpButton.setFocusable(false);
        iHelpButton.setContentAreaFilled(false);
        iHelpButton.setBorderPainted(false);
        iHelpButton.addActionListener(new CartogramWizardShowURL(
                "http://scapetoad.choros.ch/help/d-computation-report.php"));

        iSaveReportButton = new JButton("Save report...");
        iSaveReportButton.setBounds(300, 312, 130, 26);
        iSaveReportButton.setVisible(false);
        iSaveReportButton
                .addActionListener(new CartogramWizardSaveReportAction());
    }

    /**
     * Adapts the finished panels according to the current cartogram wizard
     * settings (parameters and error message).
     */
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            JButton cancelButton = AppContext.cartogramWizard.getCancelButton();
            cancelButton.setText("End");

            // Remove all elements in this pane.
            removeAll();

            if (iErrorOccured) {
                JLabel errorTitle = new JLabel(iErrorTitle);
                errorTitle.setFont(new Font(null, Font.BOLD, 11));
                errorTitle.setBounds(0, 0, 400, 14);
                add(errorTitle);

                JLabel finishedMessage = new JLabel(iErrorMessage);
                finishedMessage.setFont(new Font(null, Font.PLAIN, 11));
                finishedMessage.setBounds(0, 22, 400, 14);
                add(finishedMessage);

                JTextArea finishedReport = new JTextArea(iStackTrace);
                finishedReport.setFont(new Font(null, Font.PLAIN, 11));
                finishedReport.setEditable(false);

                JScrollPane scrollPane = new JScrollPane(finishedReport);
                scrollPane.setBounds(0, 45, 430, 250);
                add(scrollPane);
            } else {
                JLabel finishedTitle = new JLabel(
                        "Cartogram computation successfully terminated");
                finishedTitle.setFont(new Font(null, Font.BOLD, 11));
                finishedTitle.setBounds(0, 0, 400, 14);
                add(finishedTitle);

                JLabel finishedMessage = new JLabel(iShortMessage);
                finishedMessage.setFont(new Font(null, Font.PLAIN, 11));
                finishedMessage.setBounds(0, 22, 400, 14);
                add(finishedMessage);

                JTextArea finishedReport = new JTextArea(
                        AppContext.cartogramWizard.getCartogram()
                                .getComputationReport());

                finishedReport.setFont(new Font(null, Font.PLAIN, 11));
                finishedReport.setEditable(false);

                JScrollPane scrollPane = new JScrollPane(finishedReport);
                scrollPane.setBounds(0, 45, 430, 250);
                add(scrollPane);

                add(iSaveReportButton);
                iSaveReportButton.setVisible(true);
            }

            add(iHelpButton);
        }

        super.setVisible(visible);
    }

    /**
     * Defines whether an error has occured or not. This parameter will define
     * whether the error message will be displayed or a report generated.
     * 
     * @param errorOccured
     *            true if an error has occured.
     */
    public void setErrorOccured(boolean errorOccured) {
        iErrorOccured = errorOccured;
    }

    /**
     * Defines the error message.
     * 
     * @param title
     *            the title of the error message.
     * @param message
     *            a short description of the message.
     * @param stackTrace
     *            the complete stack trace in the case of an exception.
     */
    public void setErrorMessage(String title, String message, String stackTrace) {
        iErrorTitle = title;
        iErrorMessage = message;
        iStackTrace = stackTrace;
    }

    /**
     * Defines the short message.
     * 
     * @param message
     *            the message
     */
    public void setShortMessage(String message) {
        iShortMessage = message;
    }
}

/**
 * This class moves the wizard from the given step.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-12-01
 */
class CartogramWizardGoToStepAction extends AbstractAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 
     */
    private CartogramWizard iWizard = null;

    /**
     * 
     */
    private int iStep = -1;

    /**
     * The constructor needs a reference to the CartogramWizard object.
     * 
     * @param wizard
     *            the wizard
     * @param step
     *            the number of steps
     */
    CartogramWizardGoToStepAction(CartogramWizard wizard, int step) {
        iWizard = wizard;
        iStep = step;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        iWizard.goToStep(iStep);
    }
}

/**
 * This class closes the cartogram wizard.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2008-02-12
 */
class CartogramWizardCloseAction extends AbstractAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void actionPerformed(ActionEvent e) {
        Cartogram cg = AppContext.cartogramWizard.getCartogram();
        if (cg != null) {
            boolean cgRunning = cg.isRunning();
            if (cgRunning) {
                AppContext.cartogramWizard.getCartogram().interrupt();
            }
        }

        AppContext.cartogramWizard.setVisible(false);
        AppContext.cartogramWizard.dispose();
        AppContext.cartogramWizard = null;
    }
}

/**
 * This class launches the cartogram computation process.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-12-01
 */
class CartogramWizardComputeAction extends AbstractAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * A reference to the cartogram wizard. This is needed in order to extract
     * the selected options.
     */
    private CartogramWizard iCartogramWizard = null;

    /**
     * The default constructor.
     * 
     * @param cartogramWizard
     *            the cartogram wizard
     */
    CartogramWizardComputeAction(CartogramWizard cartogramWizard) {
        iCartogramWizard = cartogramWizard;
    }

    /**
     * This method launches the cartogram computation process.
     */
    @Override
    public void actionPerformed(ActionEvent aEvent) {
        // Hide the 3rd wizard panel
        iCartogramWizard.getPanelFour().setVisible(false);

        // Show the running panel
        iCartogramWizard.getRunningPanel().setVisible(true);

        iCartogramWizard.getWizardStepIconPanel().setStepIcon(6);

        // Get the attribute type (population or density value)
        boolean isDensityValue = iCartogramWizard.getPanelTwo()
                .attributeIsDensityValue();

        iCartogramWizard.getPanelTwo();
        iCartogramWizard.setMissingValue(CartogramWizardPanelTwo
                .getMissingValue());

        // Create a new cartogram instance and set the parameters
        Cartogram cartogram = new Cartogram(iCartogramWizard);
        cartogram.setLayerManager(AppContext.layerManager);
        cartogram.setMasterLayer(iCartogramWizard.getCartogramLayerName());
        cartogram.setMasterAttribute(iCartogramWizard
                .getCartogramAttributeName());
        cartogram.setMasterAttributeIsDensityValue(isDensityValue);
        cartogram.setMissingValue(iCartogramWizard.getMissingValue());
        cartogram.setSlaveLayers(iCartogramWizard.getSimultaneousLayers());
        cartogram.setConstrainedDeformationLayers(iCartogramWizard
                .getConstrainedDeformationLayers());
        cartogram.setAmountOfDeformation(iCartogramWizard
                .getAmountOfDeformation());
        cartogram.setAdvancedOptionsEnabled(iCartogramWizard
                .getAdvancedOptionsEnabled());
        cartogram.setGridSize(iCartogramWizard.getCartogramGridSizeInX(),
                iCartogramWizard.getCartogramGridSizeInY());

        // Set the parameters for the deformation grid layer
        cartogram.setCreateGridLayer(iCartogramWizard.getCreateGridLayer());
        cartogram.setGridLayerSize(iCartogramWizard.getDeformationGridSize());

        // Set the parameters for the legend layer
        // We have to estimate the legend values
        if (isDensityValue) {
            cartogram.setCreateLegendLayer(false);
        } else {
            cartogram.setCreateLegendLayer(true);
        }

        iCartogramWizard.setCartogram(cartogram);

        // Start the cartogram computation.
        cartogram.start();
    }
}

/**
 * Dialog window for specifying some advanced parameters for the cartogram
 * computation process. The following parameters can be modified: -- the
 * creation of a deformation grid layer and its size -- the cartogram grid size
 * -- the number of iterations for the Gastner algorithm -- the size of the
 * Gastner grid size
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-02-01
 */
class CartogramWizardOptionsWindow extends JDialog implements
        HyperlinkListener, ChangeListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 
     */
    private JCheckBox iAdvancedOptionsCheckBox = null;

    /**
     * The check box for creating a deformation grid layer.
     */
    private JCheckBox iGridLayerCheckBox = null;

    /**
     * The text field for the deformation grid size.
     */
    private JTextField iGridSizeTextField = null;

    /**
     * 
     */
    private JTextField iCartogramGridSizeTextField = null;

    /**
     * 
     */
    private JTextPane iManualParametersPane = null;

    /**
     * 
     */
    private JTextPane iGrid1Pane = null;

    /**
     * 
     */
    private JLabel iCartogramGridSizeLabel = null;

    /**
     * 
     */
    private JTextPane iBiasPane = null;

    /**
     * 
     */
    private JLabel iBiasLabel = null;

    /**
     * 
     */
    private JTextField iBiasTextField = null;

    /**
     * Constructor for the options window.
     */
    CartogramWizardOptionsWindow() {
        // Set the window parameters.
        setTitle("Advanced options");

        setSize(500, 530);
        setLocation(40, 50);
        setResizable(false);
        setLayout(null);
        setModal(true);

        // GRID LAYER CHECK BOX
        iGridLayerCheckBox = new JCheckBox("Create a transformation grid layer");
        iGridLayerCheckBox.setSelected(AppContext.cartogramWizard
                .getCreateGridLayer());
        iGridLayerCheckBox.setFont(new Font(null, Font.BOLD, 11));
        iGridLayerCheckBox.setLocation(20, 20);
        iGridLayerCheckBox.setSize(300, 26);
        add(iGridLayerCheckBox);

        // DEFORMATION GRID LAYER HELP TEXT

        ClassLoader cldr = getClass().getClassLoader();

        JTextPane deformationGridPane = new JTextPane();
        String deformationText = null;
        try {
            InputStream inStream = cldr.getResource(
                    "resources/DeformationGridLayerText.html").openStream();
            StringBuffer inBuffer = new StringBuffer();
            int c;
            while ((c = inStream.read()) != -1) {
                inBuffer.append((char) c);
            }
            inStream.close();
            deformationText = inBuffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        deformationGridPane.setContentType("text/html");
        deformationGridPane.setText(deformationText);
        deformationGridPane.setEditable(false);
        deformationGridPane.addHyperlinkListener(this);
        deformationGridPane.setBackground(null);
        deformationGridPane.setLocation(45, 45);
        deformationGridPane.setSize(400, 30);
        add(deformationGridPane);

        // GRID SIZE TEXT FIELD
        JLabel gridSizeLabel = new JLabel("Enter the number of rows:");
        gridSizeLabel.setLocation(45, 85);
        gridSizeLabel.setSize(140, 26);
        gridSizeLabel.setFont(new Font(null, Font.PLAIN, 11));
        add(gridSizeLabel);

        int gridSize = AppContext.cartogramWizard.getDeformationGridSize();
        String gridSizeString = "" + gridSize;
        iGridSizeTextField = new JTextField(gridSizeString);
        iGridSizeTextField.setLocation(240, 85);
        iGridSizeTextField.setSize(50, 26);
        iGridSizeTextField.setFont(new Font(null, Font.PLAIN, 11));
        iGridSizeTextField.setHorizontalAlignment(SwingConstants.RIGHT);
        add(iGridSizeTextField);

        // Separator
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setLocation(20, 120);
        separator.setSize(460, 10);
        add(separator);

        // ADVANCED OPTIONS CHECK BOX
        iAdvancedOptionsCheckBox = new JCheckBox(
                "Define cartogram parameters manually");

        iAdvancedOptionsCheckBox.setSelected(AppContext.cartogramWizard
                .getAdvancedOptionsEnabled());

        iAdvancedOptionsCheckBox.setFont(new Font(null, Font.BOLD, 11));
        iAdvancedOptionsCheckBox.setLocation(20, 140);
        iAdvancedOptionsCheckBox.setSize(360, 26);
        iAdvancedOptionsCheckBox.addChangeListener(this);
        add(iAdvancedOptionsCheckBox);

        // Manual parameters text
        iManualParametersPane = new JTextPane();
        String manualParametersText = null;
        try {
            InputStream inStream = cldr.getResource(
                    "resources/ManualParametersText.html").openStream();
            StringBuffer inBuffer = new StringBuffer();
            int c;
            while ((c = inStream.read()) != -1) {
                inBuffer.append((char) c);
            }
            inStream.close();
            manualParametersText = inBuffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        iManualParametersPane.setContentType("text/html");
        iManualParametersPane.setText(manualParametersText);
        iManualParametersPane.setEditable(false);
        iManualParametersPane.addHyperlinkListener(this);
        iManualParametersPane.setBackground(null);
        iManualParametersPane.setLocation(45, 170);
        iManualParametersPane.setSize(400, 60);
        iManualParametersPane.setEnabled(iAdvancedOptionsCheckBox.isSelected());
        add(iManualParametersPane);

        // Grid 1 text
        iGrid1Pane = new JTextPane();
        String grid1Text = null;
        try {
            InputStream inStream = cldr.getResource("resources/Grid1Text.html")
                    .openStream();
            StringBuffer inBuffer = new StringBuffer();
            int c;
            while ((c = inStream.read()) != -1) {
                inBuffer.append((char) c);
            }
            inStream.close();
            grid1Text = inBuffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        iGrid1Pane.setContentType("text/html");
        iGrid1Pane.setText(grid1Text);
        iGrid1Pane.setEditable(false);
        iGrid1Pane.addHyperlinkListener(this);
        iGrid1Pane.setBackground(null);
        iGrid1Pane.setLocation(45, 240);
        iGrid1Pane.setSize(400, 60);
        iGrid1Pane.setEnabled(iAdvancedOptionsCheckBox.isSelected());
        add(iGrid1Pane);

        // Cartogram grid size
        iCartogramGridSizeLabel = new JLabel("Enter the number of grid rows:");

        iCartogramGridSizeLabel.setLocation(45, 300);
        iCartogramGridSizeLabel.setSize(170, 26);
        iCartogramGridSizeLabel.setFont(new Font(null, Font.PLAIN, 11));
        iCartogramGridSizeLabel.setEnabled(iAdvancedOptionsCheckBox
                .isSelected());
        add(iCartogramGridSizeLabel);

        int cgGridSizeX = AppContext.cartogramWizard.getCartogramGridSizeInX();
        int cgGridSizeY = AppContext.cartogramWizard.getCartogramGridSizeInY();
        int cgGridSize = Math.max(cgGridSizeX, cgGridSizeY);
        String cgGridSizeString = "" + cgGridSize;
        iCartogramGridSizeTextField = new JTextField(cgGridSizeString);
        iCartogramGridSizeTextField.setLocation(240, 300);
        iCartogramGridSizeTextField.setSize(50, 26);
        iCartogramGridSizeTextField.setFont(new Font(null, Font.PLAIN, 11));
        iCartogramGridSizeTextField
                .setHorizontalAlignment(SwingConstants.RIGHT);
        iCartogramGridSizeTextField.setEnabled(iAdvancedOptionsCheckBox
                .isSelected());
        add(iCartogramGridSizeTextField);

        // Bias text
        iBiasPane = new JTextPane();
        String biasText = null;
        try {
            InputStream inStream = cldr.getResource("resources/BiasText.html")
                    .openStream();
            StringBuffer inBuffer = new StringBuffer();
            int c;
            while ((c = inStream.read()) != -1) {
                inBuffer.append((char) c);
            }
            inStream.close();
            biasText = inBuffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        iBiasPane.setContentType("text/html");
        iBiasPane.setText(biasText);
        iBiasPane.setEditable(false);
        iBiasPane.addHyperlinkListener(this);
        iBiasPane.setBackground(null);
        iBiasPane.setLocation(45, 360);
        iBiasPane.setSize(400, 40);
        iBiasPane.setEnabled(iAdvancedOptionsCheckBox.isSelected());
        add(iBiasPane);

        // Bias label
        iBiasLabel = new JLabel("Bias value:");
        iBiasLabel.setLocation(45, 400);
        iBiasLabel.setSize(170, 26);
        iBiasLabel.setFont(new Font(null, Font.PLAIN, 11));
        iBiasLabel.setEnabled(iAdvancedOptionsCheckBox.isSelected());
        add(iBiasLabel);

        // Bias text field
        iBiasTextField = new JTextField(new Double(
                AppContext.cartogramWizard.bias).toString());
        iBiasTextField.setLocation(190, 400);
        iBiasTextField.setSize(100, 26);
        iBiasTextField.setFont(new Font(null, Font.PLAIN, 11));
        iBiasTextField.setHorizontalAlignment(SwingConstants.RIGHT);
        iBiasTextField.setEnabled(iAdvancedOptionsCheckBox.isSelected());
        add(iBiasTextField);

        // Cancel button
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setLocation(270, 460);
        cancelButton.setSize(100, 26);
        cancelButton
                .addActionListener(new CartogramWizardAdvancedOptionsAction(
                        "closeDialogWithoutSaving", this));
        add(cancelButton);

        // Ok button
        JButton okButton = new JButton("OK");
        okButton.setLocation(380, 460);
        okButton.setSize(100, 26);
        okButton.addActionListener(new CartogramWizardAdvancedOptionsAction(
                "closeDialogWithSaving", this));
        add(okButton);

        // Add the Help button
        java.net.URL imageURL = cldr.getResource("resources/help-22.png");
        ImageIcon helpIcon = new ImageIcon(imageURL);
        JButton helpButton = new JButton(helpIcon);
        helpButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        helpButton.setHorizontalTextPosition(SwingConstants.CENTER);
        helpButton.setSize(30, 30);
        helpButton.setLocation(20, 460);
        helpButton.setFocusable(false);
        helpButton.setContentAreaFilled(false);
        helpButton.setBorderPainted(false);
        helpButton
                .addActionListener(new CartogramWizardShowURL(
                        "http://scapetoad.choros.ch/help/c-transformation-parameters.php#advanced-options"));
        add(helpButton);
    }

    /**
     * Saves the changes done by the user.
     */
    public void saveChanges() {
        AppContext.cartogramWizard
                .setAdvancedOptionsEnabled(iAdvancedOptionsCheckBox
                        .isSelected());

        AppContext.cartogramWizard.setCreateGridLayer(iGridLayerCheckBox
                .isSelected());

        try {
            String gridSizeString = iGridSizeTextField.getText();
            Integer gridSizeInt = new Integer(gridSizeString);
            AppContext.cartogramWizard.setDeformationGridSize(gridSizeInt
                    .intValue());
        } catch (NumberFormatException e1) {
            // Nothing to do
        }

        try {
            String gridSizeString = iCartogramGridSizeTextField.getText();
            Integer gridSizeInt = new Integer(gridSizeString);
            AppContext.cartogramWizard.setCartogramGridSizeInX(gridSizeInt
                    .intValue());
            AppContext.cartogramWizard.setCartogramGridSizeInY(gridSizeInt
                    .intValue());
        } catch (NumberFormatException e2) {
            // Nothing to do
        }

        try {
            String biasString = iBiasTextField.getText();
            Double biasDbl = new Double(biasString);
            AppContext.cartogramWizard.bias = biasDbl.doubleValue();
        } catch (NumberFormatException e3) {
            // Nothing to do
        }
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        try {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                Browser.init();
                Browser.displayURL(e.getURL().toString());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This method gets called on a state change of the advanced options check
     * box. It enables or disables the advanced options.
     */
    @Override
    public void stateChanged(ChangeEvent e) {
        boolean enabled = iAdvancedOptionsCheckBox.isSelected();
        iManualParametersPane.setEnabled(enabled);
        iGrid1Pane.setEnabled(enabled);
        iCartogramGridSizeLabel.setEnabled(enabled);
        iCartogramGridSizeTextField.setEnabled(enabled);
        iBiasPane.setEnabled(enabled);
        iBiasLabel.setEnabled(enabled);
        iBiasTextField.setEnabled(enabled);
    }
}

/**
 * Creates the dialog for the advanced options.
 */
class CartogramWizardAdvancedOptionsAction extends AbstractAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 
     */
    private String iActionToPerform = "showDialog";
    /**
     * 
     */
    private CartogramWizardOptionsWindow iDialog = null;

    /**
     * The default creator for the action.
     * 
     * @param actionToPerform
     *            defines the action to perform. Can be "showDialog", if we
     *            should create a new dialog and display it. Is
     *            "closeDialogWithSaving" if we should close the dialog and save
     *            the changes. is "closeDialogWithoutSaving" if we should
     *            discard the changes and close the dialog.
     * @param dialog
     *            a reference to the dialog or null if it does not yet exist
     *            (for the showDialog action).
     */
    CartogramWizardAdvancedOptionsAction(String actionToPerform,
            CartogramWizardOptionsWindow dialog) {
        iActionToPerform = actionToPerform;
        iDialog = dialog;
    }

    /**
     * Method which performs this action; it creates and opens the Advanced
     * Options dialog.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (iActionToPerform == "showDialog") {
            iDialog = new CartogramWizardOptionsWindow();
            iDialog.setVisible(true);
        } else if (iActionToPerform == "closeDialogWithoutSaving") {
            iDialog.setVisible(false);
            iDialog.dispose();
        } else if (iActionToPerform == "closeDialogWithSaving") {
            iDialog.saveChanges();
            iDialog.setVisible(false);
            iDialog.dispose();
        }
    }
}

/**
 * Opens the provided URL.
 */
class CartogramWizardShowURL extends AbstractAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 
     */
    private String iUrl = null;

    /**
     * The default creator for the action.
     * 
     * @param url
     *            the URL to show.
     */
    CartogramWizardShowURL(String url) {
        iUrl = url;
    }

    /**
     * Method which performs this action.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            Browser.init();
            Browser.displayURL(iUrl);
        } catch (IOException exc) {
            exc.printStackTrace();
        }
    }
}

/**
 * Handles the window events of the wizard window. It handles the
 * windowActivated event and the windowClosed event.
 */
class CartogramWizardWindowListener implements WindowListener {

    @Override
    public void windowActivated(WindowEvent e) {
        // Nothing to do
    }

    /**
     * Method invoked in response to a window close event.
     */
    @Override
    public void windowClosed(WindowEvent e) {
        // Nothing to do
    }

    /**
     * Method invoked in response to a window closing event. It creates a
     * CartogramWizardCloseAction which is automatically performed.
     */
    @Override
    public void windowClosing(WindowEvent e) {
        ActionEvent closeEvent = new ActionEvent(e.getSource(), e.getID(),
                "windowClosed");

        CartogramWizardCloseAction closeAction = new CartogramWizardCloseAction();

        closeAction.actionPerformed(closeEvent);
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        // Nothing to do
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        // Nothing to do
    }

    @Override
    public void windowIconified(WindowEvent e) {
        // Nothing to do
    }

    @Override
    public void windowOpened(WindowEvent e) {
        // Nothing to do
    }
}

/**
 * Dialog window for specifying the simultaneous deformation layers.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-02-01
 */
class CartogramWizardSimulaneousLayerWindow extends JDialog {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * An inline panel contained in the scroll view, containing the layer check
     * boxes.
     */
    private JPanel iLayerListPanel = null;

    /**
     * The scroll pane containing the layer list panel.
     */
    private JScrollPane iLayerListScrollPane = null;

    /**
     * The list with all the check boxes for the simultaneous layers.
     */
    private AbstractList<JCheckBox> iCheckBoxList = null;

    /**
     * The currently selected cartogram layer.
     */
    private String iCurrentCartogramLayer = null;

    /**
     * Label displayed if no layer is present to be selected.
     */
    private JLabel iNoLayerLabel = null;

    /**
     * Constructor for the simultaneous layer window.
     */
    CartogramWizardSimulaneousLayerWindow() {
        // Set the window parameters.
        setTitle("Simultaneous transformation layers");

        setSize(300, 400);
        setLocation(40, 50);
        setResizable(false);
        setLayout(null);
        setModal(true);

        // LIST WITH SIMULTANEOUS LAYERS

        // Create a new pane containing the check boxes with
        // the layers.
        iLayerListPanel = new JPanel(new GridLayout(0, 1));

        // Create the check boxes for all layers except the selected
        // cartogram layer.
        iCurrentCartogramLayer = AppContext.cartogramWizard
                .getCartogramLayerName();

        // Create the checkbox array.
        iCheckBoxList = new ArrayList<JCheckBox>();

        Font smallFont = new Font(null, Font.PLAIN, 11);

        // Create the check boxes.
        AbstractList<Layer> simLayers = AppContext.cartogramWizard
                .getSimultaneousLayers();
        int nlayers = AppContext.layerManager.size();
        if (nlayers > 1) {
            int layersInList = 0;
            int lyrcnt = 0;
            for (lyrcnt = 0; lyrcnt < nlayers; lyrcnt++) {
                Layer lyr = AppContext.layerManager.getLayer(lyrcnt);
                if (lyr.getName() != iCurrentCartogramLayer) {
                    JCheckBox checkbox = new JCheckBox(lyr.getName());
                    checkbox.setFont(smallFont);

                    // Find if this layer is already selected as a
                    // simultaneous layer.
                    if (simLayers != null && simLayers.contains(lyr)) {
                        checkbox.setSelected(true);
                    } else {
                        checkbox.setSelected(false);
                    }

                    iCheckBoxList.add(checkbox);
                    iLayerListPanel.add(checkbox);
                    layersInList++;
                }
            }

            // Compute the height of the new scroll pane.
            int scrollPaneHeight = layersInList * 26;
            if (layersInList == 0) {
                scrollPaneHeight = 260;
            }

            if (scrollPaneHeight > 260) {
                scrollPaneHeight = 260;
            }

            // Create a new scroll pane where we will display the
            // list of layers.
            iLayerListScrollPane = new JScrollPane(iLayerListPanel);
            iLayerListScrollPane.setSize(260, scrollPaneHeight);
            iLayerListScrollPane.setLocation(20, 50);
            iLayerListScrollPane.setBorder(BorderFactory.createEmptyBorder(0,
                    0, 0, 0));
            add(iLayerListScrollPane);
        }

        // Label for the layers to deform.
        JLabel layerListLabel = new JLabel(
                "Select layers to deform simultaneously:");
        layerListLabel.setSize(260, 14);
        layerListLabel.setFont(smallFont);
        layerListLabel.setLocation(20, 20);
        add(layerListLabel);

        // Label for no present layers.
        if (nlayers <= 1) {
            iNoLayerLabel = new JLabel("No other layer to be deformed.");
            iNoLayerLabel.setSize(260, 14);
            iNoLayerLabel.setFont(smallFont);
            iNoLayerLabel.setLocation(20, 50);
            add(iNoLayerLabel);
        }

        // Cancel button
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setLocation(70, 330);
        cancelButton.setSize(100, 26);
        cancelButton
                .addActionListener(new CartogramWizardSimulaneousLayerAction(
                        "closeDialogWithoutSaving", this));
        add(cancelButton);

        // Ok button
        JButton okButton = new JButton("OK");
        okButton.setLocation(180, 330);
        okButton.setSize(100, 26);
        okButton.addActionListener(new CartogramWizardSimulaneousLayerAction(
                "closeDialogWithSaving", this));
        add(okButton);
    }

    /**
     * Saves the changes done by the user.
     */
    public void saveChanges() {
        int nlayers = iCheckBoxList.size();
        AbstractList<Layer> layers = new ArrayList<Layer>();

        for (int i = 0; i < nlayers; i++) {
            JCheckBox checkBox = iCheckBoxList.get(i);
            if (checkBox.isSelected()) {
                String layerName = checkBox.getText();
                Layer lyr = AppContext.layerManager.getLayer(layerName);
                layers.add(lyr);
            }
        }

        AppContext.cartogramWizard.setSimultaneousLayers(layers);
    }
}

/**
 * The actions for the simultaneous layer dialog.
 */
class CartogramWizardSimulaneousLayerAction extends AbstractAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 
     */
    private String iActionToPerform = "showDialog";

    /**
     * 
     */
    private CartogramWizardSimulaneousLayerWindow iDialog = null;

    /**
     * The default creator for the action.
     * 
     * @param actionToPerform
     *            defines the action to perform. Can be "showDialog", if we
     *            should create a new dialog and display it. Is
     *            "closeDialogWithSaving" if we should close the dialog and save
     *            the changes. is "closeDialogWithoutSaving" if we should
     *            discard the changes and close the dialog.
     * @param dialog
     *            a reference to the dialog or null if it does not yet exist
     *            (for the showDialog action).
     */
    CartogramWizardSimulaneousLayerAction(String actionToPerform,
            CartogramWizardSimulaneousLayerWindow dialog) {
        iActionToPerform = actionToPerform;
        iDialog = dialog;
    }

    /**
     * Method which performs the previously specified action.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (iActionToPerform == "showDialog") {
            iDialog = new CartogramWizardSimulaneousLayerWindow();
            iDialog.setVisible(true);
        } else if (iActionToPerform == "closeDialogWithoutSaving") {
            iDialog.setVisible(false);
            iDialog.dispose();
        } else if (iActionToPerform == "closeDialogWithSaving") {
            iDialog.saveChanges();
            iDialog.setVisible(false);
            iDialog.dispose();
        }
    }
}

/**
 * Dialog window for specifying the constrained transformation layers.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-02-01
 */
class CartogramWizardConstrainedLayerWindow extends JDialog {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * An inline panel contained in the scroll view, containing the layer check
     * boxes.
     */
    private JPanel iLayerListPanel = null;

    /**
     * The scroll pane containing the layer list panel.
     */
    private JScrollPane iLayerListScrollPane = null;

    /**
     * The list with all the check boxes for the constrained layers.
     */
    private AbstractList<JCheckBox> iCheckBoxList = null;

    /**
     * The currently selected cartogram layer.
     */
    private String iCurrentCartogramLayer = null;

    /**
     * Label displayed if no layer is present to be selected.
     */
    private JLabel iNoLayerLabel = null;

    /**
     * Constructor for the constrained layer window.
     */
    CartogramWizardConstrainedLayerWindow() {
        // Set the window parameters.
        setTitle("ScapeToad _ Cartogram Wizard _ Constrained transformation layers");
        setSize(300, 400);
        setLocation(40, 50);
        setResizable(false);
        setLayout(null);
        setModal(true);

        // LIST WITH CONSTRAINED LAYERS

        // Create a new pane containing the check boxes with
        // the layers.
        iLayerListPanel = new JPanel(new GridLayout(0, 1));

        // Create the check boxes for all layers except the selected
        // cartogram layer.
        iCurrentCartogramLayer = AppContext.cartogramWizard
                .getCartogramLayerName();

        // Create the checkbox array.
        iCheckBoxList = new ArrayList<JCheckBox>();

        Font smallFont = new Font(null, Font.PLAIN, 11);

        // Create the check boxes.
        AbstractList<Layer> constrLayers = AppContext.cartogramWizard
                .getConstrainedDeformationLayers();
        int nlayers = AppContext.layerManager.size();
        if (nlayers > 1) {
            int layersInList = 0;
            int lyrcnt = 0;
            for (lyrcnt = 0; lyrcnt < nlayers; lyrcnt++) {
                Layer lyr = AppContext.layerManager.getLayer(lyrcnt);
                if (lyr.getName() != iCurrentCartogramLayer) {
                    JCheckBox checkbox = new JCheckBox(lyr.getName());
                    checkbox.setFont(smallFont);

                    // Find if this layer is already selected as a
                    // constrained layer.
                    if (constrLayers != null && constrLayers.contains(lyr)) {
                        checkbox.setSelected(true);
                    } else {
                        checkbox.setSelected(false);
                    }

                    iCheckBoxList.add(checkbox);
                    iLayerListPanel.add(checkbox);
                    layersInList++;
                }
            }

            // Compute the height of the new scroll pane.
            int scrollPaneHeight = layersInList * 26;
            if (layersInList == 0) {
                scrollPaneHeight = 260;
            }

            if (scrollPaneHeight > 260) {
                scrollPaneHeight = 260;
            }

            // Create a new scroll pane where we will display the
            // list of layers.
            iLayerListScrollPane = new JScrollPane(iLayerListPanel);
            iLayerListScrollPane.setSize(260, scrollPaneHeight);
            iLayerListScrollPane.setLocation(20, 50);
            iLayerListScrollPane.setBorder(BorderFactory.createEmptyBorder(0,
                    0, 0, 0));
            add(iLayerListScrollPane);
        }

        // Label for the layers to deform.
        JLabel layerListLabel = new JLabel(
                "Select layers with limited deformation:");
        layerListLabel.setSize(260, 14);
        layerListLabel.setFont(smallFont);
        layerListLabel.setLocation(20, 20);
        add(layerListLabel);

        // Label for no present layers.
        if (nlayers <= 1) {
            iNoLayerLabel = new JLabel(
                    "No layer available for limited deformation.");
            iNoLayerLabel.setSize(260, 14);
            iNoLayerLabel.setFont(smallFont);
            iNoLayerLabel.setLocation(20, 50);
            add(iNoLayerLabel);
        }

        // Cancel button
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setLocation(70, 330);
        cancelButton.setSize(100, 26);
        cancelButton
                .addActionListener(new CartogramWizardConstrainedLayerAction(
                        "closeDialogWithoutSaving", this));
        add(cancelButton);

        // Ok button
        JButton okButton = new JButton("OK");
        okButton.setLocation(180, 330);
        okButton.setSize(100, 26);
        okButton.addActionListener(new CartogramWizardConstrainedLayerAction(
                "closeDialogWithSaving", this));
        add(okButton);
    }

    /**
     * Saves the changes done by the user.
     */
    public void saveChanges() {
        int nlayers = iCheckBoxList.size();
        AbstractList<Layer> layers = new ArrayList<Layer>();

        for (int i = 0; i < nlayers; i++) {
            JCheckBox checkBox = iCheckBoxList.get(i);
            if (checkBox.isSelected()) {
                String layerName = checkBox.getText();
                Layer lyr = AppContext.layerManager.getLayer(layerName);
                layers.add(lyr);
            }
        }

        AppContext.cartogramWizard.setConstrainedDeformationLayers(layers);
    }
}

/**
 * The actions for the constrained layer dialog.
 */
class CartogramWizardConstrainedLayerAction extends AbstractAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 
     */
    private String iActionToPerform = "showDialog";
    /**
     * 
     */
    private CartogramWizardConstrainedLayerWindow iDialog = null;

    /**
     * The default creator for the action.
     * 
     * @param actionToPerform
     *            defines the action to perform. Can be "showDialog", if we
     *            should create a new dialog and display it. Is
     *            "closeDialogWithSaving" if we should close the dialog and save
     *            the changes. is "closeDialogWithoutSaving" if we should
     *            discard the changes and close the dialog.
     * @param dialog
     *            a reference to the dialog or null if it does not yet exist
     *            (for the showDialog action).
     */
    CartogramWizardConstrainedLayerAction(String actionToPerform,
            CartogramWizardConstrainedLayerWindow dialog) {
        iActionToPerform = actionToPerform;
        iDialog = dialog;
    }

    /**
     * Method which performs the previously specified action.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (iActionToPerform == "showDialog") {
            iDialog = new CartogramWizardConstrainedLayerWindow();
            iDialog.setVisible(true);
        } else if (iActionToPerform == "closeDialogWithoutSaving") {
            iDialog.setVisible(false);
            iDialog.dispose();
        } else if (iActionToPerform == "closeDialogWithSaving") {
            iDialog.saveChanges();
            iDialog.setVisible(false);
            iDialog.dispose();
        }
    }
}

/**
 * This actions saves the computation report.
 */
class CartogramWizardSaveReportAction extends AbstractAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Shows a save dialog and writes the computation report to the specified
     * file.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // Create the File Save dialog.
        FileDialog fd = new FileDialog(AppContext.cartogramWizard,
                "Save Computation Report As...", FileDialog.SAVE);
        fd.setModal(true);
        fd.setBounds(20, 30, 150, 200);
        fd.setVisible(true);

        // Get the selected File name.
        if (fd.getFile() == null) {
            return;
        }

        String path = fd.getDirectory() + fd.getFile();
        if (path.endsWith(".txt") == false) {
            path = path + ".txt";
        }

        // Write the report to the file.
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(path));
            out.write(AppContext.cartogramWizard.getCartogram()
                    .getComputationReport());
            out.close();
        } catch (IOException exc) {
            // Nothing to do
        }
    }
}