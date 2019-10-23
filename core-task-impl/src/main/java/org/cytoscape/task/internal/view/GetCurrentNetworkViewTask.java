package org.cytoscape.task.internal.view;

import java.util.Arrays;
import java.util.List;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.json.CyJSONUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.json.JSONResult;

/*
 * #%L
 * Cytoscape Core Task Impl (core-task-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2017 The Cytoscape Consortium
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

public class GetCurrentNetworkViewTask extends AbstractTask implements ObservableTask {
	
	private CyNetworkView view;
	private final CyServiceRegistrar serviceRegistrar;

	public GetCurrentNetworkViewTask(CyServiceRegistrar serviceRegistrar) {
		this.serviceRegistrar = serviceRegistrar;
	}

	@Override
	public void run(TaskMonitor tm) throws Exception {
		tm.setTitle("Get Current Network View");
		
		view = serviceRegistrar.getService(CyApplicationManager.class).getCurrentNetworkView();
		tm.showMessage(TaskMonitor.Level.INFO, "Current network view is " + view);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object getResults(Class type) {
		if (type == String.class)
			return view != null ? view.toString() : null;
		
		if (type == JSONResult.class) {
			String json = view != null ? serviceRegistrar.getService(CyJSONUtil.class).toJson(view) : null;
			JSONResult res = () -> { return "{ \"view\":"+json+"}"; };
			
			return res;
		}
		
		return view;
	}
	
	@Override
	public List<Class<?>> getResultClasses() {
		return Arrays.asList(CyNetworkView.class, String.class, JSONResult.class);
	}
}
