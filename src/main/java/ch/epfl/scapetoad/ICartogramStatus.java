package ch.epfl.scapetoad;

/**
 * The cartogram status interface allow the exchange of messages during the
 * cartogram computation.
 * 
 * @author luca@palli.ch
 */
public interface ICartogramStatus {

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
    public abstract void updateRunningStatus(int aProgress, String aLabel1,
            String aLabel2);

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
    public abstract void setComputationError(String title, String message,
            String stackTrace);

    /**
     * Shows the finished panel.
     */
    public abstract void goToFinishedPanel();

    /**
     * Show the error legend and zoom to the results.
     */
    public void showLegendZoom();
}