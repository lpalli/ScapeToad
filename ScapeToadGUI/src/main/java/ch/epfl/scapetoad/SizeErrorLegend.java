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

package ch.epfl.scapetoad;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.Ostermiller.util.Browser;

/**
 * The size error legend window.
 * 
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2008-04-30
 */
public class SizeErrorLegend extends JFrame {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public SizeErrorLegend() {
        setTitle("Size Error");
        setBounds(10, 30, 120, 220);
        setVisible(false);

        // Loading the size error legend image from the resources
        ImageIcon sizeErrorImage = new ImageIcon(getClass().getClassLoader()
                .getResource("resources/SizeErrorLegend.png"));

        // Create a new label containing the icon.
        JLabel iconLabel = new JLabel(sizeErrorImage);

        // Setting the label parameters.
        iconLabel.setLayout(null);
        iconLabel.setSize(98, 198);
        iconLabel.setLocation(1, 1);
        iconLabel.addMouseListener(new IconMouseListener());

        // Add the icon label to this panel.
        add(iconLabel);
    }
}

/**
 *
 */
class IconMouseListener extends MouseAdapter {

    /**
     * The logger.
     */
    private static Log logger = LogFactory.getLog(IconMouseListener.class);

    @Override
    public void mouseClicked(MouseEvent aEvent) {
        try {
            Browser.init();
            Browser.displayURL("http://scapetoad.choros.ch/help/d-computation-report.php#cartogram-error");
        } catch (Exception exception) {
            logger.error("", exception);
        }
    }
}