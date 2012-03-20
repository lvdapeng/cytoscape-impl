package org.cytoscape.ding.customgraphicsmgr.internal;

import java.io.File;

import org.cytoscape.ding.customgraphics.CustomGraphicsManager;
import org.cytoscape.session.events.SessionAboutToBeSavedEvent;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class SaveGraphicsToSessionTaskFactory extends AbstractTaskFactory {

	private final File location;
	private final CustomGraphicsManager manager;

	private final SessionAboutToBeSavedEvent e;

	SaveGraphicsToSessionTaskFactory(final File location, final CustomGraphicsManager manager,
			final SessionAboutToBeSavedEvent e) {
		this.manager = manager;
		this.location = location;
		this.e = e;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new PersistImageTask(location, manager), new SaveGraphicsToSessionTask(location, e));
	}

}
