package ch.epfl.scapetoad;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jump.workbench.model.Layer;

import ch.epfl.scapetoad.compute.Cartogram;

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
    @SuppressWarnings({ "hiding", "unused" })
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
     * Constructor
     * 
     * @param aCartogramWizard
     *            the cartogram wizard
     */
    public CartogramWorker(CartogramWizard aCartogramWizard) {
        iWizard = aCartogramWizard;

        iStatus = new CartogramWizardStatus(iWizard);
        iCartogram = new Cartogram(iStatus);
    }

    @Override
    protected Object construct() {
        return iCartogram.compute();
    }

    @Override
    protected void finished() {
        // Get the projected layers and finish the computation
        iCartogram.finish((Layer[]) get(), iWizard.getSimultaneousLayers(),
                iWizard.getConstrainedDeformationLayers());
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