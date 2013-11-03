package ch.epfl.scapetoad;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is the wizard implementation of the cartogram status interface.
 * 
 * @author luca@palli.ch
 */
public class CartogramWizardStatus implements ICartogramStatus {

    /**
     * The logger
     */
    private static Log logger = LogFactory.getLog(CartogramWizardStatus.class);

    /**
     * The cartogram wizard. We need the wizard reference for updating the
     * progress status informations.
     */
    private CartogramWizard iWizard = null;

    /**
     * Constructor.
     * 
     * @param aWizard
     *            the cartogram wiward
     */
    public CartogramWizardStatus(CartogramWizard aWizard) {
        iWizard = aWizard;
    }

    @Override
    public void updateRunningStatus(int aProgress, String aLabel1,
            String aLabel2) {
        iWizard.updateRunningStatus(aProgress, aLabel1, aLabel2);
    }

    @Override
    public void setComputationError(String title, String message,
            String stackTrace) {
        iWizard.setComputationError(title, message, stackTrace);
    }

    @Override
    public void goToFinishedPanel() {
        iWizard.goToFinishedPanel();
    }

    @Override
    public void showLegendZoom() {
        new SizeErrorLegend().setVisible(true);

        try {
            AppContext.layerViewPanel.getViewport().zoomToFullExtent();
        } catch (Exception exception) {
            logger.error("", exception);
        }
    }
}