package org.cytoscape.tableimport.internal.tunable;

/*
 * #%L
 * Cytoscape Table Import Impl (table-import-impl)
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

import java.awt.BorderLayout;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.swing.GroupLayout;
import javax.swing.JPanel;

import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.tableimport.internal.reader.AttributeMappingParameters;
import org.cytoscape.tableimport.internal.reader.TextTableReader.ObjectType;
import org.cytoscape.tableimport.internal.ui.ImportTablePanel;
import org.cytoscape.tableimport.internal.util.CytoscapeServices;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.swing.AbstractGUITunableHandler;

public class AttributeMappingParametersHandler  extends AbstractGUITunableHandler {
	
	private int dialogType;
    private CyTableManager tableManager;
    
	private ImportTablePanel importTablePanel;

	AttributeMappingParameters amp;
	private final FileUtil fileUtil;

	protected AttributeMappingParametersHandler(final Field field, final Object obj, final Tunable t,
			final int dialogType, final CyTableManager tableManager) {
		
		super(field, obj, t);
		this.dialogType = dialogType;
		this.tableManager = tableManager;
		//importTablePanel = null;
		this.fileUtil = CytoscapeServices.fileUtil;
		init();
		
		
	}
	
	protected AttributeMappingParametersHandler(final Method getter, final Method setter, final Object instance, final Tunable tunable,
			final int dialogType,final CyTableManager tableManager){
		
		super(getter, setter, instance, tunable);
		this.dialogType = dialogType;
		this.tableManager = tableManager;
		this.fileUtil = CytoscapeServices.fileUtil;
		
		init();
		
	}

	
	private void init() {

		try {
			amp = (AttributeMappingParameters) getValue();
		} catch (IllegalAccessException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InvocationTargetException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		
		try {
			importTablePanel =
				new ImportTablePanel(dialogType, amp.is,
				                     amp.fileType, null,null, null, null,
				                     null, null, null, tableManager, fileUtil); 
		} catch (Exception e) {
			throw new IllegalStateException("Could not initialize ImportTablePanel.", e);
		}
		
		panel = new JPanel(new BorderLayout(10,10));
		panel.add(importTablePanel, BorderLayout.CENTER);
		
		
	}
	@Override
	public void handle(){ 
		// TODO Auto-generated method stub
		try {
			amp = importTablePanel.getAttributeMappingParameters();
		
			setValue(amp);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	

}
