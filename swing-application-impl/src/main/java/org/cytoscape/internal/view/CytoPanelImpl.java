package org.cytoscape.internal.view;

import static org.cytoscape.application.swing.CytoPanelState.DOCK;
import static org.cytoscape.application.swing.CytoPanelState.FLOAT;
import static org.cytoscape.application.swing.CytoPanelState.HIDE;
import static org.cytoscape.internal.view.CytoPanelNameInternal.BOTTOM;
import static org.cytoscape.internal.view.CytoPanelNameInternal.EAST;
import static org.cytoscape.internal.view.CytoPanelNameInternal.SOUTH;
import static org.cytoscape.internal.view.CytoPanelNameInternal.WEST;
import static org.cytoscape.internal.view.CytoPanelUtil.BOTTOM_MIN_HEIGHT;
import static org.cytoscape.internal.view.CytoPanelUtil.BOTTOM_MIN_WIDTH;
import static org.cytoscape.internal.view.CytoPanelUtil.BUTTON_SIZE;
import static org.cytoscape.internal.view.CytoPanelUtil.EAST_MIN_HEIGHT;
import static org.cytoscape.internal.view.CytoPanelUtil.EAST_MIN_WIDTH;
import static org.cytoscape.internal.view.CytoPanelUtil.SOUTH_MIN_HEIGHT;
import static org.cytoscape.internal.view.CytoPanelUtil.SOUTH_MIN_WIDTH;
import static org.cytoscape.internal.view.CytoPanelUtil.WEST_MIN_HEIGHT;
import static org.cytoscape.internal.view.CytoPanelUtil.WEST_MIN_WIDTH;
import static org.cytoscape.util.swing.IconManager.ICON_CARET_DOWN;
import static org.cytoscape.util.swing.IconManager.ICON_SQUARE_O;
import static org.cytoscape.util.swing.IconManager.ICON_THUMB_TACK;
import static org.cytoscape.util.swing.IconManager.ICON_WINDOW_MINIMIZE;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedEvent;
import org.cytoscape.application.swing.events.CytoPanelStateChangedEvent;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.LookAndFeelUtil;

/*
 * #%L
 * Cytoscape Swing Application Impl (swing-application-impl)
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

@SuppressWarnings("serial")
public class CytoPanelImpl implements CytoPanel, ChangeListener {
	
	private static final int FLOAT_PANEL_SCALE_FACTOR = 2;

	private static final String TOOL_TIP_SWITCH = "Switch To...";
	private static final String TOOL_TIP_FLOAT = "Float";
	private static final String TOOL_TIP_DOCK = "Dock";
	private static final String TOOL_TIP_MINIMIZE = "Minimize";
	
	/* These are the minimum sizes for our CytoPanels. A CytoPanel can't exceed these values. */
	private final int NOTIFICATION_STATE_CHANGE = 0;
	private final int NOTIFICATION_COMPONENT_SELECTED = 1;
	private final int NOTIFICATION_COMPONENT_ADDED = 2;
	private final int NOTIFICATION_COMPONENT_REMOVED = 3;

	private JPanel titlePanel;
	private JTabbedPane tabbedPane;
	private JLabel floatLabel;
	private JButton switchCompButton;
	private JButton floatButton;
	private JButton minimizeButton;
	
	private final JComponent contentPane = new ContentPane();
	private final CytoPanelNameInternal compassDirection;
	private final int trimBarIndex;
	private final int tabPlacement;
	
	private CytoPanelState cytoPanelState;

	private final Map<String, CytoPanelComponent2> componentsById = new HashMap<>();

	private final CyServiceRegistrar serviceRegistrar;

	public CytoPanelImpl(
			final CytoPanelNameInternal compassDirection,
			final int trimBarIndex,
			final int tabPlacement,
			final CytoPanelState cytoPanelState,
			final CyServiceRegistrar serviceRegistrar
	) {
		this.compassDirection = compassDirection;
		this.trimBarIndex = trimBarIndex;
		this.tabPlacement = tabPlacement;
		this.serviceRegistrar = serviceRegistrar;
		
		init();
		setState(cytoPanelState);
	}
	
	String getTitle() {
		return compassDirection.getTitle();
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return compassDirection.toCytoPanelName();
	}
	
	public CytoPanelNameInternal getCytoPanelNameInternal() {
		return compassDirection;
	}
	
	public int getTrimBarIndex() {
		return trimBarIndex;
	}

	public void add(CytoPanelComponent comp) {
		insert(comp, getCytoPanelComponentCount());
	}
	
	public void insert(CytoPanelComponent comp, int index) {
		if (comp instanceof CytoPanelComponent2) {
			final CytoPanelComponent2 comp2 = (CytoPanelComponent2) comp;
			
			if (comp2.getIdentifier() == null)
				throw new NullPointerException("'CytoPanelComponent2.identifier' must not be null");
			
			componentsById.put(comp2.getIdentifier(), comp2);
		}
		
		checkSizes(comp.getComponent()); // Check our sizes, and override, if necessary
		insert(comp.getComponent(), comp.getTitle(), comp.getIcon(), index, true);
	}
	
	void insert(Component c, String title, Icon icon, int index, boolean notify) {
		if (getTabbedPane().indexOfComponent(c) == -1) {
			// Add tab to JTabbedPane
			getTabbedPane().insertTab(title, icon, c, null, index);
			
			if (notify) // Send out a notification
				notifyListeners(NOTIFICATION_COMPONENT_ADDED);
		}
	}

	@Override
	public int getCytoPanelComponentCount() {
		return getTabbedPane().getTabCount();
	}

	@Override
	public Component getSelectedComponent() {
		return getTabbedPane().getSelectedComponent();
	}

	@Override
	public Component getComponentAt(int index) {
		return getTabbedPane().getComponentAt(index);
	}

	@Override
	public int getSelectedIndex() {
		return getTabbedPane().getSelectedIndex();
	}

	@Override
	public int indexOfComponent(Component component) {
		return getTabbedPane().indexOfComponent(component);
	}
	
	@Override
	public int indexOfComponent(String identifier) {
		final CytoPanelComponent cpComp = componentsById.get(identifier);
		
		return cpComp != null ? indexOfComponent(cpComp.getComponent()) : -1;
	}

	public void remove(CytoPanelComponent comp) {
		getTabbedPane().remove(comp.getComponent());
		
		if (comp instanceof CytoPanelComponent2)
			componentsById.remove(((CytoPanelComponent2)comp).getIdentifier());
	}

	@Override
	public void setSelectedIndex(final int index) {
		if (getTabbedPane().getTabCount() <= index)
			return;
		
		getTabbedPane().setSelectedIndex(index);
	}

	@Override
	public void setState(CytoPanelState newState) {
		if (newState == null)
			throw new IllegalArgumentException("CytoPanelState must not be null.");
		
		if (newState != cytoPanelState) {
			cytoPanelState = newState;
			update();
			notifyListeners(NOTIFICATION_STATE_CHANGE);
		}
	}

	@Override
	public CytoPanelState getState() {
		return cytoPanelState;
	}
	
	@Override
	public Component getThisComponent() {
		return contentPane;
	}

	/**
	 * Our implementation of the ChangeListener interface, to determine when new tab has been selected
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		updateSwitchCompButton();
		notifyListeners(NOTIFICATION_COMPONENT_SELECTED);
	}
	
	/**
	 * Checks to make sure the CytoPanel is within the appropriate dimensions
	 * by overriding the sizes, if necessary
	 */
	private void checkSizes(Component comp) {
		if (compassDirection == WEST)
			comp.setMinimumSize(new Dimension(WEST_MIN_WIDTH, WEST_MIN_HEIGHT));
		else if (compassDirection == SOUTH)
			comp.setMinimumSize(new Dimension(SOUTH_MIN_WIDTH, SOUTH_MIN_HEIGHT));
		else if (compassDirection == EAST)
			comp.setMinimumSize(new Dimension(EAST_MIN_WIDTH, EAST_MIN_HEIGHT));
		else if (compassDirection == BOTTOM)
			comp.setMinimumSize(new Dimension(BOTTOM_MIN_WIDTH, BOTTOM_MIN_HEIGHT));
	}
	
	private void init() {
		titlePanel = new JPanel();
		initButtons();

		BoxLayout boxLayout = new BoxLayout(titlePanel, BoxLayout.X_AXIS);
		titlePanel.setLayout(boxLayout);
		
		titlePanel.add(Box.createHorizontalStrut(8));
		titlePanel.add(getFloatLabel());
		titlePanel.add(Box.createHorizontalGlue());
		titlePanel.add(switchCompButton);
		titlePanel.add(Box.createHorizontalStrut(8));
		titlePanel.add(floatButton);
		titlePanel.add(minimizeButton);
//		titlePanel.add(closeButton);
		titlePanel.add(Box.createHorizontalStrut(8));

		// set preferred size - we can use float or dock icon dimensions - they are the same
		FontMetrics fm = getFloatLabel().getFontMetrics(getFloatLabel().getFont());
		titlePanel.setMinimumSize(new Dimension((fm.stringWidth(getTitle()) + BUTTON_SIZE)
				* FLOAT_PANEL_SCALE_FACTOR, BUTTON_SIZE));
		
		if (compassDirection == BOTTOM)
			titlePanel.setPreferredSize(new Dimension((fm.stringWidth(getTitle()) + BUTTON_SIZE)
					* FLOAT_PANEL_SCALE_FACTOR, BUTTON_SIZE));
		else
			titlePanel.setPreferredSize(new Dimension((fm.stringWidth(getTitle()) + BUTTON_SIZE)
					* FLOAT_PANEL_SCALE_FACTOR, BUTTON_SIZE + 2));

		contentPane.setLayout(new BorderLayout());
		contentPane.add(titlePanel, BorderLayout.NORTH);
		contentPane.add(getTabbedPane(), BorderLayout.CENTER);
		contentPane.setBorder(BorderFactory.createEmptyBorder());
		
		update();
	}
	
	JTabbedPane getTabbedPane() {
		if (tabbedPane == null) {
			tabbedPane = new JTabbedPane(tabPlacement);
			tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
			tabbedPane.addChangeListener(this);
		}
		
		return tabbedPane;
	}

	private JLabel getFloatLabel() {
		if (floatLabel == null) {
			floatLabel = new JLabel(getTitle());
			floatLabel.setFont(floatLabel.getFont().deriveFont(LookAndFeelUtil.getSmallFontSize()));
			if (compassDirection == BOTTOM)
				floatLabel.setBorder(new EmptyBorder(0, 0, 0, 0));
			else
				floatLabel.setBorder(new EmptyBorder(0, 5, 0, 0));
		}
		
		return floatLabel;
	}

	private void initButtons() {
		final IconManager iconManager = serviceRegistrar.getService(IconManager.class);
		
		switchCompButton = new JButton(ICON_CARET_DOWN);
		switchCompButton.setToolTipText(TOOL_TIP_SWITCH);
		CytoPanelUtil.styleButton(switchCompButton);
		switchCompButton.setFont(iconManager.getIconFont(14));
		switchCompButton.setSelected(true);
		updateSwitchCompButton();
		
		switchCompButton.addActionListener(evt -> {
			if (getTabbedPane().getTabCount() == 0)
				return;

			final JPopupMenu popupMenu = new JPopupMenu();
			final int count = getTabbedPane().getTabCount();
			
			for (int i = 0; i < count; i++) {
				final int idx = i;
				final String title = getTabbedPane().getTitleAt(idx);
				final Icon icon = getTabbedPane().getIconAt(idx);
				final JCheckBoxMenuItem mi = new JCheckBoxMenuItem(title, icon);
				mi.setEnabled(getTabbedPane().isEnabledAt(idx));
				mi.setSelected(getTabbedPane().getSelectedIndex() == idx);
				mi.addActionListener(e -> setSelectedIndex(idx));
				popupMenu.add(mi);
			}
			
			popupMenu.show(switchCompButton, 0, switchCompButton.getHeight());
			popupMenu.requestFocusInWindow();
		});
		
		floatButton = new JButton(ICON_SQUARE_O);
		floatButton.setToolTipText(TOOL_TIP_FLOAT);
		CytoPanelUtil.styleButton(floatButton);
		floatButton.setFont(iconManager.getIconFont(12));
		floatButton.setSelected(true);
		floatButton.addActionListener(evt -> setState(cytoPanelState == FLOAT ? DOCK : FLOAT));
		
		minimizeButton = new JButton(ICON_WINDOW_MINIMIZE);
		minimizeButton.setToolTipText(TOOL_TIP_MINIMIZE);
		CytoPanelUtil.styleButton(minimizeButton);
		minimizeButton.setFont(iconManager.getIconFont(11));
		minimizeButton.setSelected(true);
		minimizeButton.addActionListener(evt -> setState(HIDE));
	}

	private void notifyListeners(int notificationType) {
		final CyEventHelper eventHelper = serviceRegistrar.getService(CyEventHelper.class);

		// determine what event to fire
		switch (notificationType) {
			case NOTIFICATION_STATE_CHANGE:
				eventHelper.fireEvent(new CytoPanelStateChangedEvent(this, this, cytoPanelState));
				break;
	
			case NOTIFICATION_COMPONENT_SELECTED:
				int selectedIndex = getTabbedPane().getSelectedIndex();
				eventHelper.fireEvent(new CytoPanelComponentSelectedEvent(this, this, selectedIndex));
				break;
	
			case NOTIFICATION_COMPONENT_ADDED:
				break;
	
			case NOTIFICATION_COMPONENT_REMOVED:
				break;
		}
	}
	
	void update() {
		getFloatLabel().setText(cytoPanelState == DOCK ? getTitle() : "");
		floatButton.setText(cytoPanelState == DOCK ? ICON_SQUARE_O : ICON_THUMB_TACK);
		floatButton.setToolTipText(cytoPanelState == DOCK ? TOOL_TIP_FLOAT : TOOL_TIP_DOCK);
		
		getThisComponent().setVisible(getState() != HIDE);
		getThisComponent().validate();
	}
	
	private void updateSwitchCompButton() {
		switchCompButton.setEnabled(getTabbedPane().getTabCount() > 0);
	}
	
	class ContentPane extends JPanel {
		
		@Override
		public void remove(Component component) {
			getTabbedPane().remove(component);
			notifyListeners(NOTIFICATION_COMPONENT_REMOVED);
		}
		
		@Override
		public void remove(int index) {
			getTabbedPane().remove(index);
			notifyListeners(NOTIFICATION_COMPONENT_REMOVED);
		}

		@Override
		public void removeAll() {
			getTabbedPane().removeAll();
			componentsById.clear();
			notifyListeners(NOTIFICATION_COMPONENT_REMOVED);
		}
	}
}
