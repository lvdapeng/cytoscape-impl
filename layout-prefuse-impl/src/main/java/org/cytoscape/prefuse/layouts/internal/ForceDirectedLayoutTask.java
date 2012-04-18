/*
 Copyright (c) 2007, 2010, The Cytoscape Consortium (www.cytoscape.org)

 This library is free software; you can redistribute it and/or modify it
 under the terms of the GNU Lesser General Public License as published
 by the Free Software Foundation; either version 2.1 of the License, or
 any later version.

 This library is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 documentation provided hereunder is on an "as is" basis, and the
 Institute for Systems Biology and the Whitehead Institute
 have no obligations to provide maintenance, support,
 updates, enhancements or modifications.  In no event shall the
 Institute for Systems Biology and the Whitehead Institute
 be liable to any party for direct, indirect, special,
 incidental or consequential damages, including lost profits, arising
 out of the use of this software and its documentation, even if the
 Institute for Systems Biology and the Whitehead Institute
 have been advised of the possibility of such damage.  See
 the GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
*/
package org.cytoscape.prefuse.layouts.internal; 


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.AbstractPartitionLayoutTask;
import org.cytoscape.view.layout.EdgeWeighter;
import org.cytoscape.view.layout.LayoutEdge;
import org.cytoscape.view.layout.LayoutNode;
import org.cytoscape.view.layout.LayoutPartition;
import org.cytoscape.view.layout.LayoutPoint;
import org.cytoscape.view.layout.WeightTypes;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import prefuse.util.force.DragForce;
import prefuse.util.force.ForceItem;
import prefuse.util.force.ForceSimulator;
import prefuse.util.force.NBodyForce;
import prefuse.util.force.SpringForce;


/**
 * This class wraps the Prefuse force-directed layout algorithm.
 * See {@link http://prefuse.org} for more detail.
 */
public class ForceDirectedLayoutTask extends AbstractPartitionLayoutTask {

	private static final String groupName = "Edge Weight Settings";

	/** A tunable for determining how to interpret weight values. */	
	@Tunable(description="How to interpret weight values",groups=groupName)
	public ListSingleSelection<WeightTypes> weightChoices;

	/** A tunable for determining the minimum edge weight to consider. */
	@Tunable(description="The minimum edge weight to consider",groups=groupName)
	public double minWeight = 0;	

	/** A tunable for determining the maximum edge weight to consider. */
	@Tunable(description="The maximum edge weight to consider",groups=groupName)
	public double maxWeight = Double.MAX_VALUE;	

	private ForceSimulator m_fsim;

//	public int numIterations;
//	public double defaultSpringCoefficient;
//	public double defaultSpringLength;
//	public double defaultNodeMass;
	private ForceDirectedLayout.Integrators integrator;
	
	private boolean supportWeights = true;
	private Map<LayoutNode,ForceItem> forceItems;

	private ForceDirectedLayoutContext context;

	/**
	 * Creates a new ForceDirectedLayout object.
	 */
	public ForceDirectedLayoutTask(final String name, CyNetworkView networkView, Set<View<CyNode>> nodesToLayOut, Set<Class<?>> supportedNodeAttributeTypes, Set<Class<?>> supportedEdgeAttributeTypes, List<String> initialAttributes, final ForceDirectedLayoutContext context,
				       final ForceDirectedLayout.Integrators integrator) {
		super(name, context.singlePartition, networkView, nodesToLayOut, supportedNodeAttributeTypes, supportedEdgeAttributeTypes, initialAttributes);

		weightChoices = new ListSingleSelection<WeightTypes>( WeightTypes.values() );	

		this.context = context;
		this.integrator = integrator;
		
		if (edgeWeighter == null)
			edgeWeighter = new EdgeWeighter();

		m_fsim = new ForceSimulator();
		m_fsim.addForce(new NBodyForce());
		m_fsim.addForce(new SpringForce());
		m_fsim.addForce(new DragForce());

		forceItems = new HashMap<LayoutNode,ForceItem>();
	}
	
	public String getName() {
		return "force-directed";
	}

	public String toString() {
		return "Force-Directed Layout";
	}

	public void layoutPartion(LayoutPartition part) {
		LayoutPoint initialLocation = null;
		// System.out.println("layoutPartion: "+part.getEdgeList().size()+" edges");
		// Calculate our edge weights
		part.calculateEdgeWeights();
		// System.out.println("layoutPartion: "+part.getEdgeList().size()+" edges after calculateEdgeWeights");

		m_fsim.setIntegrator(integrator.getNewIntegrator());
		m_fsim.clear();

		// initialize nodes
		for (LayoutNode ln: part.getNodeList()) {
			ForceItem fitem = forceItems.get(ln); 
			if ( fitem == null ) {
				fitem = new ForceItem();
				forceItems.put(ln, fitem);
			}
			fitem.mass = getMassValue(ln);
			fitem.location[0] = 0f; 
			fitem.location[1] = 0f; 
			m_fsim.addItem(fitem);
		}
		
		// initialize edges
		for (LayoutEdge e: part.getEdgeList()) {
			LayoutNode n1 = e.getSource();
			ForceItem f1 = forceItems.get(n1); 
			LayoutNode n2 = e.getTarget();
			ForceItem f2 = forceItems.get(n2); 
			if ( f1 == null || f2 == null )
				continue;
			// System.out.println("Adding edge "+e+" with spring coeffficient = "+getSpringCoefficient(e)+" and length "+getSpringLength(e));
			m_fsim.addSpring(f1, f2, getSpringCoefficient(e), getSpringLength(e)); 
		}

		// setTaskStatus(5); // This is a rough approximation, but probably good enough
		if (taskMonitor != null) {
			taskMonitor.setStatusMessage("Initializing partition "+part.getPartitionNumber());
		}

		// Figure out our starting point
		initialLocation = part.getAverageLocation();

		// perform layout
		long timestep = 1000L;
		for ( int i = 0; i < context.numIterations && !cancelled; i++ ) {
			timestep *= (1.0 - i/(double)context.numIterations);
			long step = timestep+50;
			m_fsim.runSimulator(step);
			setTaskStatus((int)(((double)i/(double)context.numIterations)*90.+5));
		}
		
		// update positions
		for (LayoutNode ln: part.getNodeList()) {
			if (!ln.isLocked()) {
				ForceItem fitem = forceItems.get(ln); 
				ln.setX(fitem.location[0]);
				ln.setY(fitem.location[1]);
				part.moveNodeToLocation(ln);
			}
		}
		// Not quite done, yet.  If we're only laying out selected nodes, we need
		// to migrate the selected nodes back to their starting position
		double xDelta = 0.0;
		double yDelta = 0.0;
		final LayoutPoint finalLocation = part.getAverageLocation();
		xDelta = finalLocation.getX() - initialLocation.getX();
		yDelta = finalLocation.getY() - initialLocation.getY();
		for (LayoutNode v: part.getNodeList()) {
			if (!v.isLocked()) {
				v.decrement(xDelta, yDelta);
				part.moveNodeToLocation(v);
			}
		}
	}

	/**
	 * Get the mass value associated with the given node. Subclasses should
	 * override this method to perform custom mass assignment.
	 * @param n the node for which to compute the mass value
	 * @return the mass value for the node. By default, all items are given
	 * a mass value of 1.0.
	 */
	protected float getMassValue(LayoutNode n) {
		return (float)context.defaultNodeMass;
	}

	/**
	 * Get the spring length for the given edge. Subclasses should
	 * override this method to perform custom spring length assignment.
	 * @param e the edge for which to compute the spring length
	 * @return the spring length for the edge. A return value of
	 * -1 means to ignore this method and use the global default.
	*/
	protected float getSpringLength(LayoutEdge e) {
		double weight = e.getWeight();
		return (float)(context.defaultSpringLength/weight);
	}

	/**
	 * Get the spring coefficient for the given edge, which controls the
	 * tension or strength of the spring. Subclasses should
	 * override this method to perform custom spring tension assignment.
	 * @param e the edge for which to compute the spring coefficient.
	 * @return the spring coefficient for the edge. A return value of
	 * -1 means to ignore this method and use the global default.
	 */
	protected float getSpringCoefficient(LayoutEdge e) {
		return (float)context.defaultSpringCoefficient;
	}
}
