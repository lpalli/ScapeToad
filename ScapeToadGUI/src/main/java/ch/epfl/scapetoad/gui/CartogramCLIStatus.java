package ch.epfl.scapetoad.gui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ch.epfl.scapetoad.ICartogramStatus;

/**
 * This class is the CLI implementation of the cartogram status interface.
 * 
 * @author luca@palli.ch
 */
public class CartogramCLIStatus implements ICartogramStatus {

    /**
     * The logger.
     */
    private static Log logger = LogFactory.getLog(CartogramCLIStatus.class);

    @Override
    public void updateRunningStatus(int aProgress, String aLabel1,
            String aLabelFormat, Object... aArgs) {
        logger.debug(String.format("%1$s - %2$s - %3$s", aProgress, aLabel1,
                String.format(aLabelFormat, aArgs)));
    }

    @Override
    public void setComputationError(String title, String message,
            String stackTrace) {
        logger.error(String.format("%1$s - %2$s: %3$s", title, message,
                stackTrace));
    }

    @Override
    public void finished() {
        logger.info("Finished");
    }
}
