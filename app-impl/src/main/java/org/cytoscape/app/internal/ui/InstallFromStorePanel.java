package org.cytoscape.app.internal.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.cytoscape.app.internal.exception.AppDownloadException;
import org.cytoscape.app.internal.exception.AppInstallException;
import org.cytoscape.app.internal.exception.AppParsingException;
import org.cytoscape.app.internal.manager.App;
import org.cytoscape.app.internal.manager.AppManager;
import org.cytoscape.app.internal.manager.AppParser;
import org.cytoscape.app.internal.net.ResultsFilterer;
import org.cytoscape.app.internal.net.WebApp;
import org.cytoscape.app.internal.net.WebQuerier;
import org.cytoscape.app.internal.net.WebQuerier.AppTag;
import org.cytoscape.app.internal.util.DebugHelper;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;

/**
 * This class represents the panel in the App Manager dialog's tab used for installing new apps.
 * Its UI setup code is generated by the Netbeans 7 GUI builder.
 */
public class InstallFromStorePanel extends javax.swing.JPanel {
	
	/** Long serial version identifier required by the Serializable class */
	private static final long serialVersionUID = -1208176142084829272L;
	
	private javax.swing.JPanel descriptionPanel;
    private javax.swing.JScrollPane descriptionScrollPane;
    private javax.swing.JSplitPane descriptionSplitPane;
    private javax.swing.JTextPane descriptionTextPane;
    private javax.swing.JTextField filterTextField;
    private javax.swing.JButton installButton;
    private javax.swing.JButton installFromFileButton;
    private javax.swing.JScrollPane resultsScrollPane;
    private javax.swing.JTree resultsTree;
    private javax.swing.JLabel searchAppsLabel;
    private javax.swing.JScrollPane tagsScrollPane;
    private javax.swing.JSplitPane tagsSplitPane;
    private javax.swing.JTree tagsTree;
    private javax.swing.JButton viewOnAppStoreButton;
	
	private JFileChooser fileChooser;
	
	private AppManager appManager;
	private FileUtil fileUtil;
	private TaskManager taskManager;
	private Container parent;
	
	private WebApp selectedApp;
	private WebQuerier.AppTag currentSelectedAppTag;
	
    public InstallFromStorePanel(final AppManager appManager, FileUtil fileUtil, TaskManager taskManager, Container parent) {
        this.appManager = appManager;
        this.fileUtil = fileUtil;
        this.taskManager = taskManager;
        this.parent = parent;
    	initComponents();
        
    	tagsTree.addTreeSelectionListener(new TreeSelectionListener() {
			
			@Override
			public void valueChanged(TreeSelectionEvent arg0) {
				updateResultsTree();
			}
		});
		
		resultsTree.addTreeSelectionListener(new TreeSelectionListener() {
			
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				updateDescriptionBox();
			}
		});
		
		setupTextFieldListener();
    	
		taskManager.execute(new TaskIterator(new Task() {
			
			// Obtain information for all available apps, then append tag information
			@Override
			public void run(TaskMonitor taskMonitor) throws Exception {
				taskMonitor.setTitle("Obtaining Apps from App Store");
				
				WebQuerier webQuerier = appManager.getWebQuerier();
		    	
				taskMonitor.setStatusMessage("Getting available apps");
				Set<WebApp> availableApps = webQuerier.getAllApps();
				
				// Note: Code below not used because web store now returns tag information when
				// returning all apps
				
				/* 
		    	// Obtain available tags
		    	Set<WebQuerier.AppTag> availableTags = webQuerier.getAllTags();
		    	
		    	double progress = 0;
		    	
		    	for (WebQuerier.AppTag appTag : availableTags) {

		    		taskMonitor.setStatusMessage("Getting apps for tag: " + appTag.getFullName());
		    		progress += 1.0 / availableTags.size();
		    		taskMonitor.setProgress(progress);
		    		
		    		// Obtain apps for the current tag
		    		Set<WebApp> tagApps = webQuerier.getAppsByTag(appTag.getName());
		    		
		    		// Assume the set of apps returned is a subset of all available apps
		    		for (WebApp tagApp : tagApps) {
		    			tagApp.getAppTags().add(appTag);
		    		}
		    	}
				*/
				
				// Once the information is obtained, update the tree
				
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						// populateTree(appManager.getWebQuerier().getAllApps());
						buildTagsTree();
						
						fillResultsTree(null);
					}
					
				});
				
			}

			@Override
			public void cancel() {
			}
			
		}));
    	
		/*
		addTagInformation();
        populateTree(appManager.getWebQuerier().getAllApps());
        */
		
		/*
        setupDescriptionListener();
        setupHyperlinkListener();
        setupTextFieldListener();
        */
		
		
    }

    private void initComponents() {

    	searchAppsLabel = new javax.swing.JLabel();
        installFromFileButton = new javax.swing.JButton();
        filterTextField = new javax.swing.JTextField();
        descriptionSplitPane = new javax.swing.JSplitPane();
        tagsSplitPane = new javax.swing.JSplitPane();
        tagsScrollPane = new javax.swing.JScrollPane();
        tagsTree = new javax.swing.JTree();
        resultsScrollPane = new javax.swing.JScrollPane();
        resultsTree = new javax.swing.JTree();
        descriptionPanel = new javax.swing.JPanel();
        descriptionScrollPane = new javax.swing.JScrollPane();
        descriptionTextPane = new javax.swing.JTextPane();
        installButton = new javax.swing.JButton();
        viewOnAppStoreButton = new javax.swing.JButton();

        searchAppsLabel.setText("Search:");

        installFromFileButton.setText("Install from File...");
        installFromFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                installFromFileButtonActionPerformed(evt);
            }
        });

        descriptionSplitPane.setDividerLocation(421);

        tagsSplitPane.setDividerLocation(160);

        tagsTree.setRootVisible(false);
        tagsScrollPane.setViewportView(tagsTree);

        tagsSplitPane.setLeftComponent(tagsScrollPane);

        resultsTree.setRootVisible(false);
        resultsScrollPane.setViewportView(resultsTree);

        tagsSplitPane.setRightComponent(resultsScrollPane);

        descriptionSplitPane.setLeftComponent(tagsSplitPane);

        descriptionPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        descriptionTextPane.setContentType("text/html");
        descriptionTextPane.setEditable(false);
        descriptionTextPane.setText("<html>\n  <head>\n\n  </head>\n  <body>\n    <p style=\"margin-top: 0\">\n      App description is displayed here.\n    </p>\n  </body>\n</html>\n");
        descriptionScrollPane.setViewportView(descriptionTextPane);

        installButton.setText("Install");
        installButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                installButtonActionPerformed(evt);
            }
        });

        viewOnAppStoreButton.setText("View on App Store");
        viewOnAppStoreButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewOnAppStoreButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout descriptionPanelLayout = new javax.swing.GroupLayout(descriptionPanel);
        descriptionPanel.setLayout(descriptionPanelLayout);
        descriptionPanelLayout.setHorizontalGroup(
            descriptionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(descriptionScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addComponent(installButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(viewOnAppStoreButton, javax.swing.GroupLayout.DEFAULT_SIZE, 172, Short.MAX_VALUE)
        );
        descriptionPanelLayout.setVerticalGroup(
            descriptionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(descriptionPanelLayout.createSequentialGroup()
                .addComponent(descriptionScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 289, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(viewOnAppStoreButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(installButton, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        descriptionSplitPane.setRightComponent(descriptionPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(descriptionSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 533, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(installFromFileButton)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(searchAppsLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(filterTextField)
                        .addGap(247, 247, 247))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(searchAppsLabel)
                    .addComponent(filterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(descriptionSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 354, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(installFromFileButton))
        );

        // Make the JTextPane render HTML using the default UI font
        Font font = UIManager.getFont("Label.font");
        String bodyRule = "body { font-family: " + font.getFamily() + "; " +
                "font-size: " + font.getSize() + "pt; }";
        ((HTMLDocument) descriptionTextPane.getDocument()).getStyleSheet().addRule(bodyRule);
    }

    private void searchButtonActionPerformed(java.awt.event.ActionEvent evt) {                                             
        // TODO add your handling code here:
    }
    
    private void installFromFileButtonActionPerformed(java.awt.event.ActionEvent evt) {
    	// Setup a the file filter for the open file dialog
    	FileChooserFilter fileChooserFilter = new FileChooserFilter("Jar, Zip Files (*.jar, *.zip)",
    			new String[]{"jar", "zip"});
    	
    	Collection<FileChooserFilter> fileChooserFilters = new LinkedList<FileChooserFilter>();
    	fileChooserFilters.add(fileChooserFilter);
    	
    	// Show the dialog
    	File[] files = fileUtil.getFiles(parent, 
    			"Choose file(s)", FileUtil.LOAD, FileUtil.LAST_DIRECTORY, "Install", true, fileChooserFilters);
    	
        if (files != null) {
        	
        	for (int index = 0; index < files.length; index++) {
        		AppParser appParser = appManager.getAppParser();
        		
        		App app = null;
        		
        		// Attempt to parse each file as an App object
        		try {
					app = appParser.parseApp(files[index]);
					
				} catch (AppParsingException e) {
					
					// TODO: Replace DebugHelper.print() messages with exception or a pop-up message box
					DebugHelper.print("Error parsing app: " + e.getMessage());
					
					JOptionPane.showMessageDialog(parent, "Error opening app: " + e.getMessage(),
		                       "Error", JOptionPane.ERROR_MESSAGE);
				} finally {
					
					// Install the app if parsing was successful
					if (app != null) {
						try {
							appManager.installApp(app);
						} catch (AppInstallException e) {
							JOptionPane.showMessageDialog(parent, "Error installing app: " + e.getMessage(),
				                       "Error", JOptionPane.ERROR_MESSAGE);
						}
					}
				}
        	}
        }
    }
    
    /**
     * Attempts to insert newlines into a given string such that each line has no 
     * more than the specified number of characters.
     */
    private String splitIntoLines(String text, int charsPerLine) {
    	return null;
    }

    private void setupTextFieldListener() {
        filterTextField.getDocument().addDocumentListener(new DocumentListener() {
        	
        	ResultsFilterer resultsFilterer = new ResultsFilterer();
        	
        	private void showFilteredApps() {
        		SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						if (currentSelectedAppTag == null) {
						
							fillResultsTree(resultsFilterer.findMatches(filterTextField.getText(), 
								appManager.getWebQuerier().getAllApps()));
						} else {
							fillResultsTree(resultsFilterer.findMatches(filterTextField.getText(), 
								appManager.getWebQuerier().getAppsByTag(currentSelectedAppTag.getName())));
						}
					}
					
				});
        	}
        	
			@Override
			public void removeUpdate(DocumentEvent arg0) {
				showFilteredApps();
			}
			
			@Override
			public void insertUpdate(DocumentEvent arg0) {
				showFilteredApps();
			}
			
			@Override
			public void changedUpdate(DocumentEvent arg0) {
			}
		});
    }
    
    
    private void searchComboBoxPropertyChange(java.beans.PropertyChangeEvent evt) {

    }
    
    
    private void installButtonActionPerformed(java.awt.event.ActionEvent evt) {
    	/*
    	final WebQuerier webQuerier = appManager.getWebQuerier();
        final WebApp appToDownload = selectedApp;
        
		taskManager.execute(new TaskIterator(new Task() {

			@Override
			public void run(TaskMonitor taskMonitor) throws Exception {
				taskMonitor.setTitle("Installing App from App Store");
				
				double progress = 0;
					
				taskMonitor.setStatusMessage("Installing app: " + appToDownload.getFullName());
				
				// Download app
        		File appFile = webQuerier.downloadApp(appToDownload.getName(), null, new File(appManager.getDownloadedAppsPath()));
				
        		if (appFile != null) {
	        		// Parse app
	        		App parsedApp = appManager.getAppParser().parseApp(appFile);
	        		
	        		// Install app
					appManager.installApp(parsedApp);
        		} else {
        			// Log error: no download links were found for app
        			DebugHelper.print("Unable to find download url for: " + appToDownload.getFullName());
        		}
	        		
	        	taskMonitor.setProgress(1.0);
			}

			@Override
			public void cancel() {
			}
			
		}));
		*/
    }
    
    private void buildTagsTree() {
    	WebQuerier webQuerier = appManager.getWebQuerier();
    	
    	// Obtain available tags
    	Set<WebQuerier.AppTag> availableTags = webQuerier.getAllTags();
    	
    	
    	
    	DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
    	
    	DefaultMutableTreeNode allAppsTreeNode = new DefaultMutableTreeNode("all apps" 
    			+ " (" + webQuerier.getAllApps().size() + ")");
    	root.add(allAppsTreeNode);
    	
    	DefaultMutableTreeNode appsByTagTreeNode = new DefaultMutableTreeNode("apps by tag");
    	root.add(appsByTagTreeNode);
    	
    	DefaultMutableTreeNode treeNode = null;
    	for (final WebQuerier.AppTag appTag : availableTags) {
    		if (appTag.getCount() > 0) {
    			treeNode = new DefaultMutableTreeNode(appTag);
    			appsByTagTreeNode.add(treeNode);
    		}
    	}
    	
    	tagsTree.setModel(new DefaultTreeModel(root));
    	// tagsTree.expandRow(2);
    	
    	currentSelectedAppTag = null;
    }
 
    private void updateResultsTree() {
    	
    	DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tagsTree.getSelectionPath().getLastPathComponent();
    	
//    	DebugHelper.print(String.valueOf(selectedNode.getUserObject()));
    	currentSelectedAppTag = null;
    	
    	// Check if the "all apps" node is selected
    	if (selectedNode.getLevel() == 1 
    			&& String.valueOf(selectedNode.getUserObject()).startsWith("all apps")) {
    		fillResultsTree(null);
    		
    	} else if (selectedNode.getUserObject() instanceof WebQuerier.AppTag) {
    		WebQuerier.AppTag selectedTag = (WebQuerier.AppTag) selectedNode.getUserObject();
    		
    		fillResultsTree(appManager.getWebQuerier().getAppsByTag(selectedTag.getName()));
    		currentSelectedAppTag = selectedTag;
    	} else {
    		// Clear tree
    		resultsTree.setModel(new DefaultTreeModel(null));
    	}
    }
    
    private void fillResultsTree(Set<WebApp> webApps) {
    	Set<WebApp> appsToShow = webApps;
    	if (appsToShow == null) {
    		appsToShow = appManager.getWebQuerier().getAllApps();
    	}
    	
    	DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
    	
    	DefaultMutableTreeNode treeNode;
    	for (WebApp webApp : appsToShow) {
    		treeNode = new DefaultMutableTreeNode(webApp);
    		root.add(treeNode);
    	}
    	
    	resultsTree.setModel(new DefaultTreeModel(root));
    }
    
    private void updateDescriptionBox() {
    	
    	TreePath selectedPath = resultsTree.getSelectionPath();
    	
    	if (selectedPath != null) {
    		DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) resultsTree.getSelectionPath().getLastPathComponent();
    		WebApp selectedApp = (WebApp) selectedNode.getUserObject();
        	
    		String text = "";
    		
    		text += "<html> <head> </head> <body>";
    		
    		// App hyperlink to web store page
    		// text += "<p style=\"margin-top: 0\"> <a href=\"" + selectedApp.getPageUrl() + "\">" + selectedApp.getPageUrl() + "</a> </p>";
    		
    		// App image
    		text += "<img border=\"0\" src=\"" + appManager.getWebQuerier().getAppStoreUrl() + selectedApp.getIconUrl() + "\" alt=\"" + selectedApp.getFullName() + "\"/>";
    		
    		// App name
    		text += "<p> <b>" + selectedApp.getFullName() + "</b> </p>";
    		
    		// App description
    		text += "<p>" + (String.valueOf(selectedApp.getDescription()).equalsIgnoreCase("null") ? "App description not found." : selectedApp.getDescription()) + "</p>";
    		text += "</body> </html>";
    		descriptionTextPane.setText(text);
    		
    		this.selectedApp = selectedApp;
		
    	} else {
    		
    		descriptionTextPane.setText("App description is displayed here.");
    		this.selectedApp = null;
    	}
    }

    private void resetButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void viewOnAppStoreButtonActionPerformed(java.awt.event.ActionEvent evt) {
    	if (selectedApp == null) {
    		return;
    	}
    	
    	if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			
			try {
				desktop.browse((new URL(selectedApp.getPageUrl())).toURI());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }
    
    /**
     * Populate the current tree of results with the available apps from the web store.
     */
    /*
    private void populateTreeOld() {
    	WebQuerier webQuerier = appManager.getWebQuerier();
    	
    	DefaultMutableTreeNode root = new DefaultMutableTreeNode("Available Apps (" + webQuerier.getAllApps().size() + ")");
    	
    	// Obtain available tags
    	Set<WebQuerier.AppTag> availableTags = webQuerier.getAllTags();
    	
    	for (WebQuerier.AppTag appTag : availableTags) {
    		DefaultMutableTreeNode tagNode = new DefaultMutableTreeNode(appTag.getFullName() + " (" + appTag.getCount() + ")");
    		
    		// Obtain apps for the current tag
    		DebugHelper.print("Getting apps for tag: " + appTag.getName());
    		Set<WebApp> tagApps = webQuerier.getAppsByTag(appTag.getName());
    		
    		for (WebApp tagApp : tagApps) {
    			tagNode.add(new DefaultMutableTreeNode(tagApp));
    		}
    		
    		root.add(tagNode);
    	}
    	
    	resultsTree.setModel(new DefaultTreeModel(root));
    }
    */
    
}
