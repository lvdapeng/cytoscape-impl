package org.cytoscape.task.internal.network;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.task.create.NewNetworkSelectedNodesOnlyTaskFactory;
import org.cytoscape.work.TaskIterator;

/*
 * #%L
 * Cytoscape Core Task Impl (core-task-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2019 The Cytoscape Consortium
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

public class NewNetworkSelectedNodesOnlyTaskFactoryImpl extends AbstractNetworkTaskFactory
                                                        implements NewNetworkSelectedNodesOnlyTaskFactory {
	
	private final CyServiceRegistrar serviceRegistrar;

	public NewNetworkSelectedNodesOnlyTaskFactoryImpl(CyServiceRegistrar serviceRegistrar) {
		this.serviceRegistrar = serviceRegistrar;
	}

	@Override
	public TaskIterator createTaskIterator(CyNetwork network) {
		return new TaskIterator(3, new NewNetworkSelectedNodesOnlyTask(network, serviceRegistrar));
	}
	
	@Override
	public boolean isReady(CyNetwork network) {
		if (!super.isReady(network))
			return false;
		
		return network != null && network.getDefaultNodeTable().countMatchingRows(CyNetwork.SELECTED, true) > 0;
	}
}
