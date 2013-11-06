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
     * @param aLabelFormat
     *            the progress secondary message format
     * @param aArgs
     *            the progress secondary message arguments
     */
    public abstract void updateRunningStatus(int aProgress, String aLabel1,
            String aLabelFormat, Object... aArgs);

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
     * Notify the finish of the compute process. Don't specify if successfully
     * or not.
     */
    public abstract void finished();
}