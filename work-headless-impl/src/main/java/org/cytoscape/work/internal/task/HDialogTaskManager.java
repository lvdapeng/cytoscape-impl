package org.cytoscape.work.internal.task;

/*
 * #%L
 * org.cytoscape.work-headless-impl
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
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


import java.awt.Window;

import javax.swing.JDialog;

import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.swing.DialogTaskManager;

public class HDialogTaskManager implements DialogTaskManager {

	@Override
	public JDialog getConfiguration(TaskFactory factory, Object tunableContext) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setExecutionContext(Window context) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void execute(TaskIterator iterator) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void execute(TaskIterator iterator, TaskObserver observer) {
		// TODO Auto-generated method stub
		
	}



}
