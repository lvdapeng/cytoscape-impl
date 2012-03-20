package org.cytoscape.search.internal;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.TaskIterator;


public class SearchTaskFactory extends AbstractNetworkTaskFactory {
	private EnhancedSearch searchMgr;
	private String query;
	
	private final CyNetworkViewManager viewManager;
	private final CyApplicationManager appManager;
	
	public SearchTaskFactory(EnhancedSearch searchMgr, String query,
			final CyNetworkViewManager viewManager, final CyApplicationManager appManager) {
		this.searchMgr = searchMgr;
		this.query = query;
		this.viewManager = viewManager;
		this.appManager = appManager;
	}

	public TaskIterator createTaskIterator(CyNetwork network) {
		return new TaskIterator(new IndexAndSearchTask(network, searchMgr, query, viewManager, appManager));
	}
}
