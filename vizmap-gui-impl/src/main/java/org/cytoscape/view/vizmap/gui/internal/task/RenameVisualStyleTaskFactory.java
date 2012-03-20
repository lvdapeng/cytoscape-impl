package org.cytoscape.view.vizmap.gui.internal.task;

import java.awt.Component;

import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.gui.SelectedVisualStyleManager;
import org.cytoscape.view.vizmap.gui.internal.util.VizMapperUtil;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class RenameVisualStyleTaskFactory extends AbstractTaskFactory {

	private final VisualMappingManager vmm;
	private final SelectedVisualStyleManager manager;
	private final VizMapperUtil util;
	private final Component parent;

	public RenameVisualStyleTaskFactory(final VisualMappingManager vmm,
			final SelectedVisualStyleManager manager, final VizMapperUtil util,
			final Component parent) {
		this.manager = manager;
		this.vmm = vmm;
		this.util = util;
		this.parent = parent;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new RenameVisualStyleTask(manager, vmm, util,
				parent));
	}

}
