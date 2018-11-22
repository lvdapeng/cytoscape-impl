package org.cytoscape.internal.view.help;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;

import org.cytoscape.application.CyVersion;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.cytoscape.util.swing.OpenBrowser;


/*
 * #%L
 * Cytoscape Swing Application Impl (swing-application-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2018 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

@SuppressWarnings("serial")
public class AboutDialog extends JDialog {

	private final CyServiceRegistrar serviceRegistrar;
	
	public AboutDialog(CyServiceRegistrar serviceRegistrar) {
		super(serviceRegistrar.getService(CySwingApplication.class).getJFrame(), "About Cytoscape",
				ModalityType.APPLICATION_MODAL);
		this.serviceRegistrar = serviceRegistrar;
		init();
	}

	public void init() {
		setResizable(false);
		
		AboutPanel panel = new AboutPanel();
		
		JButton closeButton = new JButton(new AbstractAction("Close") {
			@Override
			public void actionPerformed(ActionEvent evt) {
				dispose();
			}
		});

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        
        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER, false)
                .addComponent(panel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
                .addComponent(closeButton, Alignment.TRAILING)
        );
        layout.setVerticalGroup(layout.createSequentialGroup()
	            .addComponent(panel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
	            .addPreferredGap(ComponentPlacement.UNRELATED)
	            .addComponent(closeButton)
	            .addContainerGap()
        );
		
        LookAndFeelUtil.setDefaultOkCancelKeyStrokes(getRootPane(), closeButton.getAction(), closeButton.getAction());
        getRootPane().setDefaultButton(closeButton);
	}
	
	private class AboutPanel extends JPanel {

		private static final String CYTOSCAPE_URL = "https://cytoscape.org/";
		private static final String NRNB_URL = "https://nrnb.org/";
		private static final String UCSD_URL = "https://www.ucsd.edu/";
		private static final String UCSF_URL = "https://www.ucsf.edu/";
		private static final String UOFT_URL = "https://www.utoronto.ca/";
		private static final String GLADSTONE_URL = "https://gladstone.org/";
		private static final String AGILENT_URL = "https://www.agilent.com/";
		private static final String PASTEUR_URL = "https://www.pasteur.fr/";
		private static final String ISB_URL = "https://systemsbiology.org/";
		
		private static final String LEFT = "left";
		private static final String CENTER = "center";
		
		private JLabel aboutLabel;
		private JTextPane aboutPane;
		private JTextPane infoPane;
		private JTextPane creditsPane;

	    public AboutPanel() {
	    	JScrollPane scrollPane1 = new JScrollPane(getAboutPane());
	    	scrollPane1.setBorder(BorderFactory.createEmptyBorder());
	    	
	    	JScrollPane scrollPane2 = new JScrollPane(getInfoPane());
	    	scrollPane2.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, UIManager.getColor("Separator.foreground")));
	    	
	        int w = getAboutLabel().getPreferredSize().width;
	        
	        GroupLayout layout = new GroupLayout(this);
	        setLayout(layout);
	        
	        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER, false)
	                .addComponent(getAboutLabel())
	                .addComponent(scrollPane1, w, w, w)
	                .addComponent(scrollPane2, w, w, w)
	                .addComponent(getCreditsPane(), w, w, w)
	        );
	        layout.setVerticalGroup(layout.createSequentialGroup()
		            .addComponent(getAboutLabel())
		            .addGap(20)
		            .addComponent(scrollPane1)
		            .addGap(20)
		            .addComponent(scrollPane2, 60, 60, 60) // needs an explicit height--test with long values that span multiple lines
		            .addGap(20)
		            .addComponent(getCreditsPane())
		            .addGap(5)
	        );
	    }
		
		private JLabel getAboutLabel() {
			if (aboutLabel == null) {
				ImageIcon aboutIcon = new ImageIcon(getClass().getResource("/images/about.png"));
				aboutLabel = new JLabel(aboutIcon);
				aboutLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}
			
			return aboutLabel;
		}
		
		private JTextPane getAboutPane() {
			if (aboutPane == null) {
				aboutPane = createTextPane(String.format(
						"Cytoscape is an open source software platform for visualizing complex networks<br>"
						+ "and integrating these with any type of attribute data.<br><br>"
						+ "Cytoscape is supported by the <a href='%s'>National Resource for Network Biology</a> (NRNB).<br>"
						+ "For more information, please visit <a href='%s'>cytoscape.org</a>.",
						NRNB_URL,
						CYTOSCAPE_URL
				), LEFT);
				aboutPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
			}
			
			return aboutPane;
		}
		
		private JTextPane getInfoPane() {
			if (infoPane == null) {
				infoPane = createTextPane(String.format(
						  "<b>Version:</b> %s<br> "
						+ "<b>Java:</b> %s<br>"
						+ "<b>Java Home:</b> %s<br>"
						+ "<b>OS:</b> %s",
						getProductVersion(),
						getJavaValue(),
						getJavaHomeValue(),
						getOperatingSystemValue()
				), LEFT);
				infoPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
				infoPane.setBackground(UIManager.getColor("Table.background"));
				infoPane.setForeground(UIManager.getColor("Label.disabledForeground"));
			}

			return infoPane;
		}
		
		private JTextPane getCreditsPane() {
			if (creditsPane == null) {
				creditsPane = createTextPane(String.format(
						"<table style='border: none;' cellspacing='0' cellpadding='0' width='%s'><tr>"
						+ "<td style='text-align: center;'><a href='%s'>USCD</a></td>"
						+ "<td style='text-align: center;'><a href='%s'>UCSF</a></td>"
						+ "<td style='text-align: center;'><a href='%s'>UofT</a></td>"
						+ "<td style='text-align: center;'><a href='%s'>Gladstone Institutes</a></td>"
						+ "<td style='text-align: center;'><a href='%s'>Agilent</a></td>"
						+ "<td style='text-align: center;'><a href='%s'>Institut Pasteur</a></td>"
						+ "<td style='text-align: center;'><a href='%s'>ISB</a></td>"
						+ "</tr></table>",
						"100%",
						UCSD_URL,
						UCSF_URL,
						UOFT_URL,
						GLADSTONE_URL,
						AGILENT_URL,
						PASTEUR_URL,
						ISB_URL
				), CENTER);
			}
			
			return creditsPane;
		}

		private JTextPane createTextPane(String text, String textAlign) {
			JTextPane pane = new JTextPane();
			pane.setBackground(getBackground());
			pane.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
			pane.setEditable(false);
			pane.setContentType("text/html");
			pane.setText(
					"<div style='font-family: Helvetica, Arial, sans-serif; font-size: " + LookAndFeelUtil.getSmallFontSize() + "; text-align: " + textAlign + ";'>"
					+ text
					+ "</div>"
			);
			pane.setCaretPosition(0); // so that text is not scrolled down
			pane.addHyperlinkListener(evt -> {
				if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
					openURL(evt.getURL());
			});
			
			return pane;
		}
		
		private void openURL(URL url) {
			if (url != null)
				serviceRegistrar.getService(OpenBrowser.class).openURL(url.toExternalForm());
		}

	    public String getProductVersion() {
			return serviceRegistrar.getService(CyVersion.class).getVersion();
		}

		public String getJavaValue() {
			return System.getProperty("java.version", "unknown") + " by " + System.getProperty("java.vendor", "unknown");
		}

		public String getJavaHomeValue() {
			return System.getProperty("java.home", "unknown");
		}

		public String getOperatingSystemValue() {
			return System.getProperty("os.name", "unknown") + " " + System.getProperty("os.version", "") +
					" - " + System.getProperty("os.arch", "");
		}
	}
}
