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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.Ostermiller.util.Browser;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.Layer;

import ch.epfl.scapetoad.compute.Cartogram;
import ch.epfl.scapetoad.compute.CartogramLayer;

/**
 * The cartogram wizard guiding the user through the process of cartogram
 * creation.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-11-30
 */
public class CartogramWizard extends JFrame {

    /**
     * The logger.
     */
    private static Log logger = LogFactory.getLog(CartogramWizard.class);

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The current step.
     */
    private int iCurrentStep = -1;

    /**
     * The 0 panel.
     */
    private CartogramWizardPanelZero iPanelZero = null;

    /**
     * The 1 panel.
     */
    private CartogramWizardPanelOne iPanelOne = null;

    /**
     * The 2 panel.
     */
    private CartogramWizardPanelTwo iPanelTwo = null;

    /**
     * The 3 panel.
     */
    private CartogramWizardPanelThree iPanelThree = null;

    /**
     * The 4 panel.
     */
    private CartogramWizardPanelFour iPanelFour = null;

    /**
     * The cartogram worker process object.
     */
    private CartogramWorker iWorker = null;

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
    private List<CartogramLayer> iSimultaneousLayers = null;

    /**
     * The list of constrained deformation layer.
     */
    private List<CartogramLayer> iConstrainedDeformationLayers = null;

    /**
     * The amount of deformation.
     */
    private int iAmountOfDeformation = 50;

    /**
     * The grid size.
     */
    private int[] iCartogramGridSize = { 1000, 1000 };

    /**
     * <code>true</code> if the advanced options are enabled.
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
     * The cancel button.
     */
    private JButton iCancelButton = null;

    /**
     * The missing value.
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
        // setModal(true);
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
     * Returns the cartogram worker computation process.
     * 
     * @return the cartogram worker
     */
    public CartogramWorker getWorker() {
        return iWorker;
    }

    /**
     * Sets the cartogram worker computation process.
     * 
     * @param aWorker
     *            the cartogram worker
     */
    public void setCartogram(CartogramWorker aWorker) {
        iWorker = aWorker;
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
     * @param aProgress
     *            the progress status (integer 0-1000).
     * @param aLabel1
     *            the progress main message.
     * @param aLabel2
     *            the progress secondary message.
     */
    public void updateRunningStatus(final int aProgress, final String aLabel1,
            final String aLabel2) {
        logger.debug(String.format("%1$s - %2$s - %3$s", aProgress, aLabel1,
                aLabel2));
        Runnable doSetRunningStatus = new Runnable() {
            @Override
            public void run() {
                iRunningPanel.updateProgressBar(aProgress);
                iRunningPanel.updateProgressLabel1(aLabel1);
                iRunningPanel.updateProgressLabel2(aLabel2);
            }
        };

        SwingUtilities.invokeLater(doSetRunningStatus);
    }

    /**
     * Returns the list of simultaneous layers.
     * 
     * @return the layers
     */
    public List<CartogramLayer> getSimultaneousLayers() {
        return iSimultaneousLayers;
    }

    /**
     * Sets the list of simultaneous layers.
     * 
     * @param layers
     *            the layer
     */
    public void setSimultaneousLayers(List<CartogramLayer> layers) {
        iSimultaneousLayers = layers;
    }

    /**
     * Returns the list of constrained deformation layers.
     * 
     * @return the layers
     */
    public List<CartogramLayer> getConstrainedDeformationLayers() {
        return iConstrainedDeformationLayers;
    }

    /**
     * Sets the list of constrained deformation layers.
     * 
     * @param layers
     *            the layers
     */
    public void setConstrainedDeformationLayers(List<CartogramLayer> layers) {
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
    public int[] getCartogramGridSize() {
        return iCartogramGridSize;
    }

    /**
     * Changes the cartogram grid size in x direction.
     * 
     * @param gridSizeX
     *            the grid X size
     */
    public void setCartogramGridSizeInX(int gridSizeX) {
        iCartogramGridSize[0] = gridSizeX;
    }

    /**
     * Changes the cartogram grid size in y direction.
     * 
     * @param gridSizeY
     *            the grid Y size
     */
    public void setCartogramGridSizeInY(int gridSizeY) {
        iCartogramGridSize[1] = gridSizeY;
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
     * The icon label.
     */
    private JLabel iIconLabel;

    /**
     * The icon 1.
     */
    private ImageIcon iIcon1;

    /**
     * The icon 2.
     */
    private ImageIcon iIcon2;

    /**
     * The icon 3.
     */
    private ImageIcon iIcon3;

    /**
     * The icon 4.
     */
    private ImageIcon iIcon4;

    /**
     * The icon 5.
     */
    private ImageIcon iIcon5;

    /**
     * The icon 6.
     */
    private ImageIcon iIcon6;

    /**
     * The icon 7.
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
        ClassLoader loader = getClass().getClassLoader();
        iIcon1 = new ImageIcon(loader.getResource("resources/WizardStep1.png"));
        iIcon2 = new ImageIcon(loader.getResource("resources/WizardStep2.png"));
        iIcon3 = new ImageIcon(loader.getResource("resources/WizardStep3.png"));
        iIcon4 = new ImageIcon(loader.getResource("resources/WizardStep4.png"));
        iIcon5 = new ImageIcon(loader.getResource("resources/WizardStep5.png"));
        iIcon6 = new ImageIcon(loader.getResource("resources/WizardStep6.png"));
        iIcon7 = new ImageIcon(loader.getResource("resources/WizardStep7.png"));

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
     * Set the step icon.
     * 
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
        ClassLoader loader = getClass().getClassLoader();
        URL wizardStepZeroURL = loader
                .getResource("resources/WizardIntroduction.html");

        // Get the content from the text file.
        String wizardStepZeroContent = null;
        try {
            InputStream stream = wizardStepZeroURL.openStream();
            StringBuffer inBuffer = new StringBuffer();
            int c;

            while ((c = stream.read()) != -1) {
                inBuffer.append((char) c);
            }

            stream.close();

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
        java.net.URL imageURL = loader.getResource("resources/help-22.png");
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
    private CartogramWizard iCartogramWizard = null;

    /**
     * 
     */
    private JComboBox iLayerMenu = null;

    /**
     * The "Next" button.
     */
    private JButton iNextButton = null;

    /**
     * The default constructor for the panel.
     * 
     * @param aContentFrame
     *            the content frame
     */
    protected CartogramWizardPanelOne(CartogramWizard aContentFrame) {
        iCartogramWizard = aContentFrame;

        setLocation(160, 90);
        setSize(440, 340);
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
        JButton button = new JButton("< Back");
        button.setLocation(195, 314);
        button.setSize(120, 26);
        button.addActionListener(new CartogramWizardGoToStepAction(
                iCartogramWizard, 0));
        add(button);

        // Add a pop-up menu with the list of available layers
        JLabel label = new JLabel("Spatial coverage:");
        label.setFont(new Font(null, Font.PLAIN, 11));
        label.setBounds(0, 0, 190, 14);
        add(label);

        iLayerMenu = new JComboBox();
        iLayerMenu.setBounds(0, 20, 190, 26);
        iLayerMenu.setFont(new Font(null, Font.PLAIN, 11));
        iLayerMenu.setMaximumRowCount(20);
        // Add all polygon layers to the list
        int nlayers = AppContext.layerManager.size();
        // Check for each layer whether it is a polygon layer or not
        Layer layer;
        FeatureCollectionWrapper wrapper;
        for (int i = 0; i < nlayers; i++) {
            layer = AppContext.layerManager.getLayer(i);
            wrapper = layer.getFeatureCollectionWrapper();
            int nfeat = wrapper.size();
            if (nfeat > 0) {
                if (((Feature) wrapper.getFeatures().get(0)).getGeometry()
                        .getArea() != 0.0) {
                    iLayerMenu.addItem(layer.getName());
                }
            }
        }
        // If there is no layer for the cartogram deformation, add a menu item
        // "<none>" and disable the "Next" button
        if (iLayerMenu.getItemCount() == 0) {
            iLayerMenu.addItem("<none>");
            iNextButton.setEnabled(false);
        } else {
            iNextButton.setEnabled(true);
        }
        add(iLayerMenu);

        ClassLoader loader = getClass().getClassLoader();

        // Create a new label containing the image
        label = new JLabel(new ImageIcon(
                loader.getResource("resources/Topology.png")));
        // Setting the label parameters.
        label.setLayout(null);
        label.setSize(192, 239);
        label.setLocation(240, 30);
        // Add the icon label to this panel
        add(label);

        // Adding the explanatory text
        // The message itself is read from a RTF file
        JTextPane textPane = new JTextPane();
        // Get the content from the text file
        String layerMenuText = null;
        try {
            InputStream stream = loader.getResource(
                    "resources/LayerMenuText.rtf").openStream();

            StringBuilder builder = new StringBuilder();
            int c;
            while ((c = stream.read()) != -1) {
                builder.append((char) c);
            }

            stream.close();
            layerMenuText = builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        textPane.setContentType("text/rtf");
        textPane.setText(layerMenuText);
        textPane.setEditable(false);
        textPane.setFont(new Font(null, Font.PLAIN, 11));
        textPane.setBackground(null);
        textPane.setLocation(0, 60);
        textPane.setSize(220, 240);
        add(textPane);

        // Add the Help button
        button = new JButton(new ImageIcon(
                loader.getResource("resources/help-22.png")));
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setSize(30, 30);
        button.setLocation(0, 312);
        button.setFocusable(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.addActionListener(new CartogramWizardShowURL(
                "http://scapetoad.choros.ch/help/a-cartogram-creation.php#cartogram-layer"));
        add(button);
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

            // If there is no layer for the cartogram deformation, add a menu
            // item "<none>" and disable the "Next" button
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

        // Add all polygon layers to the list
        int nlayers = AppContext.layerManager.size();

        // Check for each layer whether it is a polygon layer or not
        Layer layer;
        FeatureCollectionWrapper wrapper;
        String layerName;
        for (int i = 0; i < nlayers; i++) {
            layer = AppContext.layerManager.getLayer(i);
            wrapper = layer.getFeatureCollectionWrapper();
            if (wrapper.size() > 0) {
                if (((Feature) wrapper.getFeatures().get(0)).getGeometry()
                        .getArea() != 0.0) {
                    layerName = layer.getName();
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
     * The logger.
     */
    private static Log logger = LogFactory
            .getLog(CartogramWizardPanelTwo.class);

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The cartogram wizard.
     */
    private CartogramWizard iCartogramWizard = null;

    /**
     * The attribute menu.
     */
    private JComboBox iAttributeMenu = null;

    /**
     * The current cartogram layer.
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
     * The attribute type density button.
     */
    private JRadioButton iAttributeTypeDensityButton = null;

    /**
     * The attribute type population button.
     */
    private JRadioButton iAttributeTypePopulationButton = null;

    /**
     * The default constructor for the panel.
     * 
     * @param aContentFrame
     *            the content frame
     */
    protected CartogramWizardPanelTwo(CartogramWizard aContentFrame) {
        iCartogramWizard = aContentFrame;

        setLocation(160, 90);
        setSize(440, 340);
        setLayout(null);

        // Add the Next button
        iNextButton = new JButton("Next >");
        iNextButton.setLocation(320, 314);
        iNextButton.setSize(120, 26);
        iNextButton.setMnemonic(KeyEvent.VK_ACCEPT);
        iNextButton.addActionListener(new CartogramWizardGoToStepAction(
                aContentFrame, 3));
        add(iNextButton);

        // Add the Back button
        JButton button = new JButton("< Back");
        button.setLocation(195, 314);
        button.setSize(120, 26);
        button.addActionListener(new CartogramWizardGoToStepAction(
                aContentFrame, 1));
        add(button);

        // Create the attribute label
        JLabel label = new JLabel("Cartogram attribute:");
        label.setFont(new Font(null, Font.PLAIN, 11));
        label.setBounds(0, 0, 190, 14);
        add(label);

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
            Layer layer = AppContext.layerManager
                    .getLayer(iCurrentCartogramLayer);
            FeatureSchema schema = layer.getFeatureCollectionWrapper()
                    .getFeatureSchema();
            int nattrs = schema.getAttributeCount();
            AttributeType attrtype;
            for (int i = 0; i < nattrs; i++) {
                attrtype = schema.getAttributeType(i);
                if (attrtype == AttributeType.DOUBLE
                        || attrtype == AttributeType.INTEGER) {
                    iAttributeMenu.addItem(schema.getAttributeName(i));
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
        label = new JLabel("Attribute type:");
        label.setFont(new Font(null, Font.PLAIN, 11));
        label.setBounds(220, 0, 190, 14);
        add(label);

        // Create the attribute type radio buttons
        iAttributeTypePopulationButton = new JRadioButton("Mass");
        iAttributeTypePopulationButton.setSelected(true);
        iAttributeTypePopulationButton.setFont(new Font(null, Font.PLAIN, 11));
        iAttributeTypePopulationButton.setBounds(220, 20, 190, 20);
        add(iAttributeTypePopulationButton);

        iAttributeTypeDensityButton = new JRadioButton("Density");
        iAttributeTypeDensityButton.setSelected(false);
        iAttributeTypeDensityButton.setFont(new Font(null, Font.PLAIN, 11));
        iAttributeTypeDensityButton.setBounds(220, 45, 190, 20);

        iAttributeTypeButtonGroup = new ButtonGroup();
        iAttributeTypeButtonGroup.add(iAttributeTypePopulationButton);
        iAttributeTypeButtonGroup.add(iAttributeTypeDensityButton);
        add(iAttributeTypeDensityButton);

        add(iAttributeMenu);

        // Get the wizard content from a text file.
        ClassLoader loader = getClass().getClassLoader();

        // Create the text pane which displays the attribute message
        // The message itself is read from a RTF file
        JTextPane textPane = new JTextPane();
        // Get the content from the text file
        String attributeMenuText = null;
        try {
            InputStream stream = loader.getResource(
                    "resources/AttributeMenuText.rtf").openStream();
            StringBuilder builder = new StringBuilder();
            int c;
            while ((c = stream.read()) != -1) {
                builder.append((char) c);
            }
            stream.close();
            attributeMenuText = builder.toString();
        } catch (Exception e) {
            logger.error("", e);
        }
        textPane.setContentType("text/rtf");
        textPane.setText(attributeMenuText);
        textPane.setEditable(false);
        textPane.setFont(new Font(null, Font.PLAIN, 11));
        textPane.setBackground(null);
        textPane.setLocation(0, 80);
        textPane.setSize(190, 100);
        add(textPane);

        // Create the text pane which displays the attribute type message
        // The message itself is read from a RTF file
        textPane = new JTextPane();
        // Get the content from the text file
        String attributeTypeText = null;
        try {
            InputStream stream = loader.getResource(
                    "resources/AttributeTypeText.html").openStream();
            StringBuilder builder = new StringBuilder();
            int c;
            while ((c = stream.read()) != -1) {
                builder.append((char) c);
            }
            stream.close();
            attributeTypeText = builder.toString();
        } catch (Exception e) {
            logger.error("", e);
        }
        textPane.setContentType("text/html");
        textPane.setText(attributeTypeText);
        textPane.setEditable(false);
        textPane.addHyperlinkListener(this);
        textPane.setBackground(null);
        textPane.setLocation(220, 80);
        textPane.setSize(190, 200);
        add(textPane);

        // Add the Help button
        button = new JButton(new ImageIcon(
                loader.getResource("resources/help-22.png")));
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setSize(30, 30);
        button.setLocation(0, 312);
        button.setFocusable(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.addActionListener(new CartogramWizardShowURL(
                "http://scapetoad.choros.ch/help/a-cartogram-creation.php#cartogram-attribute"));
        add(button);
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
        // Find out the current layer name
        String layerName = iCartogramWizard.getCartogramLayerName();

        if (iCurrentCartogramLayer != layerName) {
            // Change the layer name attribute
            iCurrentCartogramLayer = layerName;

            // Remove all existing items
            iAttributeMenu.removeAllItems();

            // Get the numerical attributes of the current cartogram layer
            if (iCurrentCartogramLayer != null && iCurrentCartogramLayer != ""
                    && iCurrentCartogramLayer != "<none>") {
                FeatureSchema schema = AppContext.layerManager
                        .getLayer(iCurrentCartogramLayer)
                        .getFeatureCollectionWrapper().getFeatureSchema();

                int nattrs = schema.getAttributeCount();

                AttributeType attrtype;
                for (int i = 0; i < nattrs; i++) {
                    attrtype = schema.getAttributeType(i);
                    if (attrtype == AttributeType.DOUBLE
                            || attrtype == AttributeType.INTEGER) {
                        iAttributeMenu.addItem(schema.getAttributeName(i));
                    }
                }
            }

            // If there is no attribute we can select, add an item "<none>" and
            // disable the "Next" button.
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
            logger.error("", ex);
        }
    }

    /**
     * @return the missing value
     */
    public static String getMissingValue() {
        return "";
    }
}

/**
 * This class represents the panel for the selection of the layers for
 * simultaneous and constrained transformation. There is also a slider for the
 * amount of deformation and the size for the grid to overlay.
 */
class CartogramWizardPanelThree extends JPanel {

    /**
     * The logger.
     */
    private static Log logger = LogFactory
            .getLog(CartogramWizardPanelThree.class);

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The default constructor for the panel.
     * 
     * @param aContentFrame
     *            the content frame
     */
    protected CartogramWizardPanelThree(CartogramWizard aContentFrame) {
        setLocation(160, 90);
        setSize(440, 340);
        setLayout(null);

        // Button for simultanous layers.
        JButton button = new JButton("Layers to transform...");
        button.setLocation(0, 0);
        button.setSize(240, 26);
        button.addActionListener(new CartogramWizardSimulaneousLayerAction(
                "showDialog", null));
        add(button);

        // Create the text pane which displays the help text for the
        // simultaneous layers.
        JTextPane textPane = new JTextPane();
        ClassLoader loader = getClass().getClassLoader();
        String simLayerText = null;
        try {
            InputStream stream = loader.getResource(
                    "resources/SimLayersText.rtf").openStream();
            StringBuilder builder = new StringBuilder();
            int c;
            while ((c = stream.read()) != -1) {
                builder.append((char) c);
            }
            stream.close();
            simLayerText = builder.toString();
        } catch (Exception e) {
            logger.error("", e);
        }
        textPane.setContentType("text/rtf");
        textPane.setText(simLayerText);
        textPane.setEditable(false);
        textPane.setFont(new Font(null, Font.PLAIN, 11));
        textPane.setBackground(null);
        textPane.setLocation(40, 35);
        textPane.setSize(400, 80);
        add(textPane);

        // Button for constrained layers.
        button = new JButton("Constrained transformation...");
        button.setLocation(0, 140);
        button.setSize(240, 26);
        button.addActionListener(new CartogramWizardConstrainedLayerAction(
                "showDialog", null));
        add(button);

        // Create the text pane which displays the help text for the
        // simultaneous layers.
        textPane = new JTextPane();
        String constLayerText = null;
        try {
            InputStream stream = loader.getResource(
                    "resources/ConstLayersText.rtf").openStream();
            StringBuilder builder = new StringBuilder();
            int c;
            while ((c = stream.read()) != -1) {
                builder.append((char) c);
            }
            stream.close();
            constLayerText = builder.toString();
        } catch (Exception e) {
            logger.error("", e);
        }
        textPane.setContentType("text/rtf");
        textPane.setText(constLayerText);
        textPane.setEditable(false);
        textPane.setFont(new Font(null, Font.PLAIN, 11));
        textPane.setBackground(null);
        textPane.setLocation(40, 175);
        textPane.setSize(400, 60);
        add(textPane);

        // Add the Next button
        button = new JButton("Next >");
        button.setLocation(320, 314);
        button.setSize(120, 26);
        button.setMnemonic(KeyEvent.VK_ENTER);
        button.addActionListener(new CartogramWizardGoToStepAction(
                aContentFrame, 4));
        add(button);

        // Add the Back button
        button = new JButton("< Back");
        button.setLocation(195, 314);
        button.setSize(120, 26);
        button.addActionListener(new CartogramWizardGoToStepAction(
                aContentFrame, 2));
        add(button);

        // Add the Help button
        button = new JButton(new ImageIcon(
                loader.getResource("resources/help-22.png")));
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setSize(30, 30);
        button.setLocation(0, 312);
        button.setFocusable(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.addActionListener(new CartogramWizardShowURL(
                "http://scapetoad.choros.ch/help/b-other-layers.php"));
        add(button);
    }
}

/**
 * This class represents the panel for the slider for the amount of deformation
 * the size for the grid to overlay.
 */
class CartogramWizardPanelFour extends JPanel {

    /**
     * The logger.
     */
    private static Log logger = LogFactory
            .getLog(CartogramWizardPanelFour.class);

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The cartogram wizard.
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
    protected CartogramWizardPanelFour(JFrame contentFrame) {
        iCartogramWizard = (CartogramWizard) contentFrame;

        setLocation(160, 90);
        setSize(440, 340);
        setLayout(null);

        ClassLoader loader = getClass().getClassLoader();

        // Add the slider for the amount of deformation.
        Font font = new Font(null, Font.PLAIN, 11);
        iDeformationSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 50);
        iDeformationSlider.setMajorTickSpacing(25);
        iDeformationSlider.setMinorTickSpacing(5);
        iDeformationSlider.setPaintTicks(true);
        iDeformationSlider.setFont(font);
        iDeformationSlider.setSize(440, 40);
        iDeformationSlider.setLocation(0, 20);

        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        JLabel label = new JLabel("Low");
        label.setFont(font);
        labelTable.put(new Integer(0), label);
        label = new JLabel("Medium");
        label.setFont(font);
        labelTable.put(new Integer(50), label);
        label = new JLabel("High");
        label.setFont(font);
        labelTable.put(new Integer(100), label);

        iDeformationSlider.setLabelTable(labelTable);
        iDeformationSlider.setPaintLabels(true);
        add(iDeformationSlider);

        // Add the label for the amount of deformation.
        label = new JLabel("Transformation quality:");
        label.setSize(440, 14);
        label.setFont(new Font(null, Font.BOLD, 11));
        label.setLocation(0, 0);
        add(label);

        // Create the text pane which displays the help text for the
        // amount of deformation.
        JTextPane textPane = new JTextPane();
        String deformationText = null;
        try {
            InputStream stream = loader.getResource(
                    "resources/AmountOfDeformationText.rtf").openStream();
            StringBuilder builder = new StringBuilder();
            int c;
            while ((c = stream.read()) != -1) {
                builder.append((char) c);
            }
            stream.close();
            deformationText = builder.toString();
        } catch (Exception e) {
            logger.error("", e);
        }
        textPane.setContentType("text/rtf");
        textPane.setText(deformationText);
        textPane.setEditable(false);
        textPane.setFont(new Font(null, Font.PLAIN, 11));
        textPane.setBackground(null);
        textPane.setLocation(40, 70);
        textPane.setSize(400, 70);
        add(textPane);

        // ADVANCED OPTIONS

        // A button and an explanatory text for the advanced options.
        JButton button = new JButton("Advanced options...");
        button.setLocation(0, 170);
        button.setSize(240, 26);
        button.addActionListener(new CartogramWizardAdvancedOptionsAction(
                "showDialog", null));
        add(button);

        // Create the text pane which displays the help text for the
        // simultaneous layers.
        textPane = new JTextPane();
        String advancedText = null;
        try {
            InputStream stream = loader.getResource(
                    "resources/AdvancedOptionsText.rtf").openStream();
            StringBuilder builder = new StringBuilder();
            int c;
            while ((c = stream.read()) != -1) {
                builder.append((char) c);
            }
            stream.close();
            advancedText = builder.toString();
        } catch (Exception e) {
            logger.error("", e);
        }
        textPane.setContentType("text/rtf");
        textPane.setText(advancedText);
        textPane.setEditable(false);
        textPane.setFont(new Font(null, Font.PLAIN, 11));
        textPane.setBackground(null);
        textPane.setLocation(40, 205);
        textPane.setSize(400, 60);
        add(textPane);

        // Add the Compute button
        button = new JButton("Compute");
        button.setLocation(320, 314);
        button.setSize(120, 26);
        button.setMnemonic(KeyEvent.VK_ENTER);
        button.addActionListener(new CartogramWizardComputeAction(
                (CartogramWizard) contentFrame));
        add(button);

        // Add the Back button
        button = new JButton("< Back");
        button.setLocation(195, 314);
        button.setSize(120, 26);
        button.addActionListener(new CartogramWizardGoToStepAction(
                (CartogramWizard) contentFrame, 3));
        add(button);

        // Add the Help button
        button = new JButton(new ImageIcon(
                loader.getResource("resources/help-22.png")));
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setSize(30, 30);
        button.setLocation(0, 312);
        button.setFocusable(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.addActionListener(new CartogramWizardShowURL(
                "http://scapetoad.choros.ch/help/c-transformation-parameters.php"));
        add(button);
    }

    /**
     * If the panel is shown, update the layer list before displaying the panel.
     */
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            // updateLayerList();
            // updateConstrainedLayerList();
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
     * The progress labels. The label 1 is for the current task name.
     */
    private JLabel iProgressLabel1 = null;

    /**
     * The progress labels. In a lengthy task, the label 2 may be used for more
     * detailed user information.
     */
    private JLabel iProgressLabel2 = null;

    /**
     * The default constructor.
     */
    protected CartogramWizardRunningPanel() {
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
    protected CartogramWizardFinishedPanel() {
        setLocation(160, 90);
        setSize(440, 340);
        setLayout(null);

        // Add the Help button
        ImageIcon helpIcon = new ImageIcon(getClass().getClassLoader()
                .getResource("resources/help-22.png"));

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
            JButton button = AppContext.cartogramWizard.getCancelButton();
            button.setText("End");

            // Remove all elements in this pane.
            removeAll();

            if (iErrorOccured) {
                JLabel label = new JLabel(iErrorTitle);
                label.setFont(new Font(null, Font.BOLD, 11));
                label.setBounds(0, 0, 400, 14);
                add(label);

                label = new JLabel(iErrorMessage);
                label.setFont(new Font(null, Font.PLAIN, 11));
                label.setBounds(0, 22, 400, 14);
                add(label);

                JTextArea finishedReport = new JTextArea(iStackTrace);
                finishedReport.setFont(new Font(null, Font.PLAIN, 11));
                finishedReport.setEditable(false);

                JScrollPane scrollPane = new JScrollPane(finishedReport);
                scrollPane.setBounds(0, 45, 430, 250);
                add(scrollPane);
            } else {
                JLabel label = new JLabel(
                        "Cartogram computation successfully terminated");
                label.setFont(new Font(null, Font.BOLD, 11));
                label.setBounds(0, 0, 400, 14);
                add(label);

                label = new JLabel(iShortMessage);
                label.setFont(new Font(null, Font.PLAIN, 11));
                label.setBounds(0, 22, 400, 14);
                add(label);

                JTextArea finishedReport = new JTextArea(
                        AppContext.cartogramWizard.getWorker().getCartogram()
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
     * The cartogram wizard.
     */
    private CartogramWizard iWizard = null;

    /**
     * The step.
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
    protected CartogramWizardGoToStepAction(CartogramWizard wizard, int step) {
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
        CartogramWorker worker = AppContext.cartogramWizard.getWorker();
        if (worker != null && worker.isRunning()) {
            AppContext.cartogramWizard.getWorker().interrupt();
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
    protected CartogramWizardComputeAction(CartogramWizard cartogramWizard) {
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
        CartogramWorker worker = new CartogramWorker(iCartogramWizard,
                AppContext.layerManager, iCartogramWizard.getCreateGridLayer(),
                !isDensityValue);
        Cartogram cartogram = worker.getCartogram();
        cartogram.setMasterLayer(Utils.convert(AppContext.layerManager
                .getLayer(iCartogramWizard.getCartogramLayerName())));
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
        cartogram.setGridSize(iCartogramWizard.getCartogramGridSize());

        // Set the parameters for the deformation grid layer
        cartogram.setGridLayerSize(iCartogramWizard.getDeformationGridSize());

        iCartogramWizard.setCartogram(worker);

        // Start the cartogram computation.
        worker.start();
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
     * The advanced options check box.
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
     * The grid size field.
     * 
     */
    private JTextField iCartogramGridSizeTextField = null;

    /**
     * The manual parameters pane.
     */
    private JTextPane iManualParametersPane = null;

    /**
     * The grid pane.
     */
    private JTextPane iGrid1Pane = null;

    /**
     * The cartogram grid size label.
     */
    private JLabel iCartogramGridSizeLabel = null;

    /**
     * The bias pane.
     */
    private JTextPane iBiasPane = null;

    /**
     * The bias label.
     */
    private JLabel iBiasLabel = null;

    /**
     * The bias text field.
     */
    private JTextField iBiasTextField = null;

    /**
     * Constructor for the options window.
     */
    protected CartogramWizardOptionsWindow() {
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

        // Deformation grid layer help text
        ClassLoader loader = getClass().getClassLoader();

        JTextPane deformationGridPane = new JTextPane();
        String deformationText = null;
        try {
            InputStream inStream = loader.getResource(
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
            InputStream inStream = loader.getResource(
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
            InputStream inStream = loader.getResource(
                    "resources/Grid1Text.html").openStream();
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

        int[] size = AppContext.cartogramWizard.getCartogramGridSize();
        iCartogramGridSizeTextField = new JTextField(""
                + Math.max(size[0], size[1]));
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
            InputStream inStream = loader
                    .getResource("resources/BiasText.html").openStream();
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
        java.net.URL imageURL = loader.getResource("resources/help-22.png");
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
    protected CartogramWizardAdvancedOptionsAction(String actionToPerform,
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
    protected CartogramWizardShowURL(String url) {
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
    private List<JCheckBox> iCheckBoxList = null;

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
    protected CartogramWizardSimulaneousLayerWindow() {
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
        List<CartogramLayer> simLayers = AppContext.cartogramWizard
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
        List<CartogramLayer> layers = new ArrayList<CartogramLayer>(nlayers);

        JCheckBox checkBox;
        for (int i = 0; i < nlayers; i++) {
            checkBox = iCheckBoxList.get(i);
            if (checkBox.isSelected()) {
                layers.add(Utils.convert(AppContext.layerManager
                        .getLayer(checkBox.getText())));
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
    protected CartogramWizardSimulaneousLayerAction(String actionToPerform,
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
    private List<JCheckBox> iCheckBoxList = null;

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
    protected CartogramWizardConstrainedLayerWindow() {
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
        List<CartogramLayer> constrLayers = AppContext.cartogramWizard
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
        List<CartogramLayer> layers = new ArrayList<CartogramLayer>();

        for (int i = 0; i < nlayers; i++) {
            JCheckBox checkBox = iCheckBoxList.get(i);
            if (checkBox.isSelected()) {
                String layerName = checkBox.getText();
                layers.add(Utils.convert(AppContext.layerManager
                        .getLayer(layerName)));
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
    protected CartogramWizardConstrainedLayerAction(String actionToPerform,
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
        // Create the File Save dialog
        FileDialog dialog = new FileDialog(AppContext.cartogramWizard,
                "Save Computation Report As...", FileDialog.SAVE);
        dialog.setModal(true);
        dialog.setBounds(20, 30, 150, 200);
        dialog.setVisible(true);

        // Get the selected File name
        if (dialog.getFile() == null) {
            return;
        }

        String path = dialog.getDirectory() + dialog.getFile();
        if (path.endsWith(".txt") == false) {
            path = path + ".txt";
        }

        // Write the report to the file
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writer.write(AppContext.cartogramWizard.getWorker().getCartogram()
                    .getComputationReport());
            writer.close();
        } catch (IOException exc) {
            // Nothing to do
        }
    }
}