package ch.epfl.scapetoad.gui;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.LabelStyle;

import ch.epfl.scapetoad.Cartogram;
import ch.epfl.scapetoad.CartogramLayer;
import ch.epfl.scapetoad.ICartogramStatus;

/**
 * The cartogram worker class is the computation manager class. It is a subclass
 * of the SwingWorker class. It has methods for setting all the parameters and
 * for launching the computation.
 * 
 * @author luca@palli.ch
 */
public class CartogramWorker extends SwingWorker {

    /**
     * The logger
     */
    @SuppressWarnings({ "hiding" })
    private static Log logger = LogFactory.getLog(CartogramWorker.class);

    /**
     * The main cartogram computation object.
     */
    private Cartogram iCartogram;

    /**
     * The cartogram wizard. We need the wizard reference for updating the
     * progress status informations.
     */
    private CartogramWizard iWizard = null;

    /**
     * The object to exchange the messages with the cartogram wizard.
     */
    private ICartogramStatus iStatus;

    /**
     * The layer manager used for cartogram computation.
     */
    private LayerManager iLayerManager = null;

    /**
     * The category name for our cartogram layers.
     */
    private String iCategoryName = null;

    /**
     * Should we create a grid layer ?
     */
    private boolean iCreateGridLayer = true;

    /**
     * Should we create a legend layer ?
     */
    private boolean iCreateLegendLayer = true;

    /**
     * Constructor
     * 
     * @param aCartogramWizard
     *            the cartogram wizard
     * @param aLayerManager
     *            the layer manager
     * @param aCreateGridLayer
     *            <code>true</code> to create the grid layer
     * @param aCreateLegendLayer
     *            <code>true</code> to create the legend layer
     */
    public CartogramWorker(CartogramWizard aCartogramWizard,
            LayerManager aLayerManager, boolean aCreateGridLayer,
            boolean aCreateLegendLayer) {
        iWizard = aCartogramWizard;
        iLayerManager = aLayerManager;
        iCreateGridLayer = aCreateGridLayer;
        iCreateLegendLayer = aCreateLegendLayer;

        iStatus = new CartogramWizardStatus(iWizard);
        iCartogram = new Cartogram(iStatus);
    }

    @Override
    protected Object construct() {
        return iCartogram.compute(iCreateGridLayer, iCreateLegendLayer);
    }

    @Override
    protected void finished() {
        // Get the projected layers and finish the computation
        @SuppressWarnings("unchecked")
        List<CartogramLayer> layers = (List<CartogramLayer>) get();
        iCartogram.finish(layers, iWizard.getSimultaneousLayers(),
                iWizard.getConstrainedDeformationLayers());

        // Hide all the already present layers
        @SuppressWarnings("unchecked")
        List<Layer> layerList = iLayerManager.getLayers();
        for (Layer layer : layerList) {
            layer.setVisible(false);
        }

        // Add the new layers category
        String category = getCategoryName();
        if (iLayerManager.getCategory(category) == null) {
            iLayerManager.addCategory(category);
        }

        // Add all the new layers
        Layer projectedMasterLayer = null; // The master layer
        Layer layer;
        for (CartogramLayer cartogramLayer : layers) {
            layer = iLayerManager.addLayer(category,
                    Utils.convert(cartogramLayer, iLayerManager));

            if (projectedMasterLayer == null) {
                projectedMasterLayer = layer;
            }
        }

        // Add the deformation grid layer
        CartogramLayer deformationGrid = iCartogram.getDeformationGrid();
        if (deformationGrid != null) {
            iLayerManager.addLayer(category,
                    Utils.convert(deformationGrid, iLayerManager));
        }

        // Add the legend layer
        CartogramLayer iLegendLayer = iCartogram.getLegendLayer();
        if (iLegendLayer != null) {
            layer = iLayerManager.addLayer(category,
                    Utils.convert(iLegendLayer, iLayerManager));

            LabelStyle legendLabels = layer.getLabelStyle();
            legendLabels.setAttribute("VALUE");
            legendLabels.setEnabled(true);
            legendLabels.setFont(new Font(null, Font.PLAIN, 10));
        }

        // Create the size error style for the master layer
        if (projectedMasterLayer != null) {
            // Create a color table for the size error attribute
            BasicStyle style = (BasicStyle) projectedMasterLayer
                    .getStyle(BasicStyle.class);
            style.setFillColor(Color.WHITE);

            SizeErrorStyle errorStyle = new SizeErrorStyle("SizeError");

            errorStyle.addColor(new Color(91, 80, 153));
            errorStyle.addColor(new Color(133, 122, 179));
            errorStyle.addColor(new Color(177, 170, 208));
            errorStyle.addColor(new Color(222, 218, 236));
            errorStyle.addColor(new Color(250, 207, 187));
            errorStyle.addColor(new Color(242, 153, 121));
            errorStyle.addColor(new Color(233, 95, 64));

            errorStyle.addLimit(70);
            errorStyle.addLimit(80);
            errorStyle.addLimit(90);
            errorStyle.addLimit(100);
            errorStyle.addLimit(110);
            errorStyle.addLimit(120);

            projectedMasterLayer.addStyle(errorStyle);
            errorStyle.setEnabled(true);
            projectedMasterLayer.getStyle(BasicStyle.class).setEnabled(false);
        }

        CartogramWorker.showLegendZoom();
        iStatus.finished();
    }

    /**
     * Returns the category name for our cartogram layers.
     * 
     * @return the category name
     */
    private String getCategoryName() {
        if (iCategoryName == null) {
            // Create a new category in the layer manager in order to
            // properly separate the cartogram layers. We call the new category
            // «Cartogram x», where x is a serial number.

            int catNumber = 1;
            String categoryName = "Cartogram " + catNumber;
            while (iLayerManager.getCategory(categoryName) != null) {
                catNumber++;
                categoryName = "Cartogram " + catNumber;
            }

            iCategoryName = categoryName;
        }

        return iCategoryName;
    }

    /**
     * Show the error legend and zoom to the results.
     */
    private static void showLegendZoom() {
        new SizeErrorLegend().setVisible(true);

        try {
            AppContext.layerViewPanel.getViewport().zoomToFullExtent();
        } catch (Exception exception) {
            logger.error("", exception);
        }
    }

    /**
     * Returns the cartogram computation object.
     * 
     * @return the cartogram computation object
     */
    public Cartogram getCartogram() {
        return iCartogram;
    }

    /**
     * Return the cartogram wizard status object.
     * 
     * @return the cartogram wizard status
     */
    public ICartogramStatus getStatus() {
        return iStatus;
    }
}