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

import java.io.InputStream;

import javax.swing.JDialog;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.Ostermiller.util.Browser;

/**
 * The about box for ScapeToad
 * 
 * @author Christian Kaiser <christian@361degres.ch>
 * @version v1.0.0, 2009-05-21
 */
public class AboutBox extends JDialog implements HyperlinkListener {

    /**
     * The logger
     */
    private static Log logger = LogFactory.getLog(AboutBox.class);

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The constructor for the about box window.
     */
    public AboutBox() {
        // Set the window parameters.
        setTitle("About ScapeToad");

        setSize(500, 400);
        setLocation(40, 50);
        setResizable(false);
        setLayout(null);
        setModal(true);

        // About box content
        ClassLoader cldr = getClass().getClassLoader();
        JTextPane aboutPane = new JTextPane();
        String aboutText = null;
        try {
            InputStream inStream = cldr.getResource("resources/AboutText.html")
                    .openStream();
            StringBuffer inBuffer = new StringBuffer();
            int c;
            while ((c = inStream.read()) != -1) {
                inBuffer.append((char) c);
            }
            inStream.close();
            aboutText = inBuffer.toString();
        } catch (Exception exception) {
            logger.error("", exception);
        }
        aboutPane.setContentType("text/html");
        aboutPane.setText(aboutText);
        aboutPane.setEditable(false);
        aboutPane.addHyperlinkListener(this);
        aboutPane.setBackground(null);
        aboutPane.setLocation(50, 50);
        aboutPane.setSize(400, 300);
        add(aboutPane);
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent event) {
        try {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                Browser.init();
                Browser.displayURL(event.getURL().toString());
            }
        } catch (Exception exception) {
            logger.error("", exception);
        }
    }
}