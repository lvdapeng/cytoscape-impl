package org.cytoscape.ding.impl.cyannotator.tasks;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.cytoscape.ding.impl.DGraphView;
import org.cytoscape.ding.impl.cyannotator.CyAnnotator;
import org.cytoscape.ding.impl.cyannotator.annotations.DingAnnotation;
import org.cytoscape.task.AbstractNetworkViewTask;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.annotations.Annotation;
import org.cytoscape.view.presentation.annotations.GroupAnnotation;
import org.cytoscape.work.TaskMonitor;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
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

public class UngroupAnnotationsTask extends AbstractNetworkViewTask {
	
	private Set<GroupAnnotation> groups;

	public UngroupAnnotationsTask(CyNetworkView view, DingAnnotation annotation) {
		super(view);
		
		if (annotation instanceof GroupAnnotation)
			groups = Collections.singleton((GroupAnnotation) annotation);
	}
	
	public UngroupAnnotationsTask(CyNetworkView view, Collection<GroupAnnotation> annotations) {
		super(view);
		groups = annotations != null ? new LinkedHashSet<>(annotations) : Collections.emptySet();
	}

	@Override
	public void run(TaskMonitor tm) throws Exception {
		if (view instanceof DGraphView) {
			
			CyAnnotator annotator = ((DGraphView)view).getCyAnnotator();
			annotator.markUndoEdit("Ungroup Annotations");
			
			for(GroupAnnotation ga : groups) {
				for(Annotation a : ga.getMembers()) {
					ga.removeMember(a);
					a.setSelected(true);
				}
				
				ga.removeAnnotation();
			}
			
			annotator.postUndoEdit();
		}
	}
}
