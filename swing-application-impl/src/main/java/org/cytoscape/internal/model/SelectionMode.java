package org.cytoscape.internal.model;

import org.cytoscape.util.swing.IconManager;

public enum SelectionMode {
	NODE_SELECTION("Nodes", IconManager.ICON_CIRCLE, "NETWORK_NODE_SELECTION"),
	EDGE_SELECTION("Edges", IconManager.ICON_ALIGN_JUSTIFY, "NETWORK_EDGE_SELECTION"),
	ANNOTATION_SELECTION("Annotations", IconManager.ICON_STICKY_NOTE_O, "NETWORK_ANNOTATION_SELECTION");
	
	private final String text;
	private final String iconText;
	private final String propertyId;

	private SelectionMode(String text, String iconText, String propertyId) {
		this.text = text;
		this.iconText = iconText;
		this.propertyId = propertyId;
	}
	
	public String getText() {
		return text;
	}
	
	public String getIconText() {
		return iconText;
	}
	
	public String getPropertyId() {
		return propertyId;
	}
}
