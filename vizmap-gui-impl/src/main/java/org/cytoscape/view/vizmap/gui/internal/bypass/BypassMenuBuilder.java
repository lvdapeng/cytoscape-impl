package org.cytoscape.view.vizmap.gui.internal.bypass;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexiconNode;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.gui.editor.EditorManager;
import org.cytoscape.view.vizmap.gui.editor.ValueEditor;
import org.cytoscape.view.vizmap.gui.util.PropertySheetUtil;

final class BypassMenuBuilder {
	private static final String ROOT_MENU_LABEL = "Bypass Visual Style";
	
	// Try to set it at the bottom of context menu
	private static final float ROOT_GRAVITY = 1000000f;

	private static final Font ENABLED_FONT = new Font("Helvatica", Font.BOLD, 14);
	private static final Icon ENABLED_ICON = new ImageIcon(
			BypassMenuBuilder.class.getResource("/images/icons/CrystalClearIcons_Action-lock-silver-icon.png"));
	private static final Color ENABLED_COLOR = Color.RED;

	private final VisualLexiconNode root;
	private final EditorManager editorManager;
	private final VisualMappingManager vmm;
	private final Collection<VisualProperty<?>> vpSet;

	public BypassMenuBuilder(final VisualLexiconNode root, final EditorManager editorManager,
			final VisualMappingManager vmm, final Collection<VisualProperty<?>> vpSet) {
		this.root = root;
		this.editorManager = editorManager;
		this.vmm = vmm;
		this.vpSet = vpSet;
	}

	public CyMenuItem build(final CyNetworkView netView, final View<? extends CyIdentifiable> nodeView) {
		final Queue<VisualLexiconNode> queue = new PriorityQueue<VisualLexiconNode>(50,
				new VisualLexiconNodeComparator());
		final Map<VisualLexiconNode, JMenuItem> menuMap = new HashMap<VisualLexiconNode, JMenuItem>();

		final JMenu rootJMenu = new JMenu(ROOT_MENU_LABEL);

		final CyMenuItem rootMenu = new CyMenuItem(rootJMenu, ROOT_GRAVITY);
		queue.addAll(root.getChildren());
		menuMap.put(root, rootMenu.getMenuItem());

		final Set<VisualLexiconNode> nextNodes = new HashSet<VisualLexiconNode>();

		while (!queue.isEmpty()) {
			final VisualLexiconNode curretNode = queue.poll();
			final VisualProperty<?> vp = curretNode.getVisualProperty();
			
			final Collection<VisualLexiconNode> children = curretNode.getChildren();
			nextNodes.addAll(children);

			final JMenuItem menu;
			if (children.size() == 0 && PropertySheetUtil.isCompatible(vp)) {
				final boolean lock = nodeView.isValueLocked(vp);
				if (lock) {
					menu = new JMenu(vp.getDisplayName());
					final JMenuItem clear = new JMenuItem("Clear");
					clear.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							nodeView.clearValueLock(vp);
							netView.updateView();
						}
					});
					final JMenuItem edit = new JMenuItem("Edit Bypass");
					edit.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							applBypassValue(netView, nodeView, vp);
						}
					});
					menu.add(clear);
					menu.add(edit);

					// Update color
					menu.setForeground(ENABLED_COLOR);
					menu.setIcon(ENABLED_ICON);
					menu.setFont(ENABLED_FONT);
					VisualLexiconNode parent = curretNode.getParent();
					while (parent != root) {
						JMenuItem enabledPath = menuMap.get(parent);
						enabledPath.setForeground(ENABLED_COLOR);
						enabledPath.setIcon(ENABLED_ICON);
						enabledPath.setFont(ENABLED_FONT);
						parent = parent.getParent();
					}
					rootJMenu.setIcon(ENABLED_ICON);
					rootJMenu.setForeground(ENABLED_COLOR);
					rootJMenu.setFont(ENABLED_FONT);

				} else {
					menu = new JMenuItem(vp.getDisplayName());
					menu.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							applBypassValue(netView, nodeView, vp);
						}
					});
				}

			} else {
				menu = new JMenu(vp.getDisplayName());
			}

			if(PropertySheetUtil.isCompatible(vp)) {
				menuMap.get(curretNode.getParent()).add(menu);
				menuMap.put(curretNode, menu);
			}

			if (queue.isEmpty()) {
				queue.addAll(nextNodes);
				nextNodes.clear();
			}
		}

		final JSeparator separator = new JSeparator();
		final JMenuItem resetMenu = new JMenuItem("Reset All");
		resetMenu.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				clearAll(netView, nodeView);
				
			}
		});
		
		rootJMenu.add(separator);
		rootJMenu.add(resetMenu);
		
		return rootMenu;
	}

	
	/**
	 * Apply bypass
	 * 
	 * @param netView
	 * @param graphObjectView
	 * @param vp
	 */
	private final void applBypassValue(final CyNetworkView netView, final View<? extends CyIdentifiable> graphObjectView,
			VisualProperty<?> vp) {
		final ValueEditor<Object> editor = (ValueEditor<Object>) editorManager.getValueEditor(vp.getRange().getType());
		final Object bypassValue = editor.showEditor(null, graphObjectView.getVisualProperty(vp));
		
		// Set lock for the vp
		graphObjectView.setLockedValue(vp, bypassValue);
		
		// Apply the new value only for the given view
		final CyRow row = netView.getModel().getRow(graphObjectView.getModel());
		vmm.getCurrentVisualStyle().apply(row, graphObjectView);
		
		// Redraw the view
		netView.updateView();
	}

	private final void clearAll(final CyNetworkView netView, final View<? extends CyIdentifiable> nodeView) {
		boolean needToUpdateView = false;
		final VisualStyle style = vmm.getCurrentVisualStyle();
		
		for (VisualProperty<?> vp : vpSet) {
			final boolean lock = nodeView.isValueLocked(vp);
			if (lock) {
				nodeView.clearValueLock(vp);
				needToUpdateView = true;
			}
		}

		if (needToUpdateView) {
			style.apply(netView);
			netView.updateView();
		}
	}

	private static final class VisualLexiconNodeComparator implements Comparator<VisualLexiconNode> {
		@Override
		public int compare(final VisualLexiconNode node1, final VisualLexiconNode node2) {
			return node1.compareTo(node2);
		}

	}

}
