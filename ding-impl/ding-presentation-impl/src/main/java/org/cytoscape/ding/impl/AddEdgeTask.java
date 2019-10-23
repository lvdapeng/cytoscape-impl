package org.cytoscape.ding.impl;

import org.cytoscape.ding.impl.undo.AddEdgeEdit;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;

public class AddEdgeTask extends AbstractTask implements ObservableTask {

	private final CyServiceRegistrar registrar;
	private final CyNetworkView netView;
	private final View<CyNode> sourceNodeView;
	private final View<CyNode> targetNodeView;
	
	private boolean postUndo = true;
	private CyEdge edge = null;
	
	public AddEdgeTask(CyServiceRegistrar registrar, CyNetworkView netView, View<CyNode> sourceNodeView, View<CyNode> targetNodeView) {
		this.registrar = registrar;
		this.netView = netView;
		this.sourceNodeView = sourceNodeView;
		this.targetNodeView = targetNodeView;
	}

	public void setPostUndo(boolean postUndo) {
		this.postUndo = postUndo;
	}

	@Override
	public void run(TaskMonitor tm) throws Exception {
		tm.setTitle("Add Edge");
		
		CyNetwork net = netView.getModel();
		View<CyNode> mutableSourceNodeView = netView.getNodeView(sourceNodeView.getSUID());
		View<CyNode> mutableTargetNodeView = netView.getNodeView(targetNodeView.getSUID());
		if(mutableSourceNodeView == null || mutableTargetNodeView == null)
			return;
		
		CyNode sourceNode = mutableSourceNodeView.getModel();
		CyNode targetNode = mutableTargetNodeView.getModel();
		
		String sourceName = net.getRow(sourceNode).get(CyRootNetwork.SHARED_NAME, String.class);
		String targetName = net.getRow(targetNode).get(CyRootNetwork.SHARED_NAME, String.class);
		String interaction = "interacts with";
		String edgeName = sourceName + " (" + interaction + ") " + targetName;
		tm.setStatusMessage("Adding edge '" + edgeName + "'...");
		
		edge = net.addEdge(sourceNode, targetNode, true);
		
		CyRow edgeRow = net.getRow(edge, CyNetwork.DEFAULT_ATTRS);
		edgeRow.set(CyNetwork.NAME, edgeName);
		edgeRow.set(CyEdge.INTERACTION, interaction);

		// Apply visual style
		// To make sure the edge view is created before applying the style
		registrar.getService(CyEventHelper.class).flushPayloadEvents(net);
		
		VisualStyle vs = registrar.getService(VisualMappingManager.class).getVisualStyle(netView);
		View<CyEdge> edgeView = netView.getEdgeView(edge);
		if (edgeView != null) {
			vs.apply(edgeRow, edgeView);
			
			if(postUndo) {
				AddEdgeEdit addEdgeEdit = new AddEdgeEdit(registrar, netView, sourceNodeView, targetNodeView, edgeView);
				addEdgeEdit.post();
			}
		}
	}

	@Override
	public <R> R getResults(Class<? extends R> type) {
		if(CyEdge.class.equals(type)) {
			return type.cast(edge);
		}
		return null;
	}

}
