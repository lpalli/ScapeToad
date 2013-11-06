package ch.epfl.scapetoad;


/**
 * This class is the wizard implementation of the cartogram status interface.
 * 
 * @author luca@palli.ch
 */
public class CartogramWizardStatus implements ICartogramStatus {

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
            String aLabelFormat, Object... aArgs) {
        iWizard.updateRunningStatus(aProgress, aLabel1,
                String.format(aLabelFormat, aArgs));
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
}