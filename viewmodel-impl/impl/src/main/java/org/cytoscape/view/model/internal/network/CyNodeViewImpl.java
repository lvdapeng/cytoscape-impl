package org.cytoscape.view.model.internal.network;

import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.model.events.ViewChangeRecord;
import org.cytoscape.view.model.events.ViewChangedEvent;
import org.cytoscape.view.model.internal.base.CyViewBase;
import org.cytoscape.view.model.internal.base.VPStore;
import org.cytoscape.view.model.internal.base.ViewLock;

public class CyNodeViewImpl extends CyViewBase<CyNode> {

	private final CyNetworkViewImpl parent;
	
	public CyNodeViewImpl(CyNetworkViewImpl parent, CyNode model) {
		super(model);
		this.parent = parent;
	}

	@Override
	public void setDirty() {
		parent.setDirty();
	}
	
	@Override
	public VPStore getVPStore() {
		return parent.nodeVPs;
	}

	@Override
	public ViewLock getLock() {
		return parent.nodeLock;
	}

	@Override
	public VisualLexicon getVisualLexicon() {
		return parent.getVisualLexicon();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void fireViewChangedEvent(VisualProperty<?> vp, Object value, boolean lockedValue) {
		var record = new ViewChangeRecord<>(this, vp, value, lockedValue);
		parent.getEventHelper().addEventPayload(parent, record, ViewChangedEvent.class);
	}
	
}
