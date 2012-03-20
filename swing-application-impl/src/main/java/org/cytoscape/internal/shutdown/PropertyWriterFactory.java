package org.cytoscape.internal.shutdown;


import java.io.File;

import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.write.CyPropertyWriterManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;


final class PropertyWriterFactory extends AbstractTaskFactory {
	private final CyPropertyWriterManager writerManager;
	private final CyProperty property;
	private final File outputFile;
	private final CyFileFilter fileFilter;

	PropertyWriterFactory(final CyPropertyWriterManager writerManager, final CyProperty property,
                              final CyFileFilter fileFilter, final File outputFile)
	{
		this.writerManager = writerManager;
		this.property      = property;
		this.outputFile    = outputFile;
		this.fileFilter    = fileFilter;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(
			new PropertyWriter(writerManager, fileFilter, property, outputFile));
	}
}