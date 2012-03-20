/**
 * Copyright (c) 2006 The Regents of the University of California.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *   1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions, and the following disclaimer.
 *   2. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions, and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *   3. Redistributions must acknowledge that this software was
 *      originally developed by the UCSF Computer Graphics Laboratory
 *      under support by the NIH National Center for Research Resources,
 *      grant P41-RR01081.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package csapps.layout.algorithms.bioLayout;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cytoscape.view.layout.AbstractPartitionLayoutTask;
import org.cytoscape.view.layout.EdgeWeighter;
import org.cytoscape.view.layout.LayoutPartition;


/**
 * Superclass for the two bioLayout algorithms (KK and FR).
 *
 * @author <a href="mailto:scooter@cgl.ucsf.edu">Scooter Morris</a>
 * @version 0.9
 */
public abstract class BioLayoutAlgorithmTask extends AbstractPartitionLayoutTask {

	/**
	 * A small value used to avoid division by zero
	 */
	protected double EPSILON = 0.0000001;

	/**
	 * Value to set for doing unweighted layouts
	 */
	public static final String UNWEIGHTEDATTRIBUTE = "(unweighted)";

	/**
	 * Enables/disables debugging messages
	 */
	private final static boolean DEBUG = false;
	protected static boolean debug = DEBUG; // so we can overload it with a property
	
	/**
	 * Whether or not to use edge weights for layout
	 */
	protected boolean supportWeights = true;

	/**
	 * This is the constructor for the bioLayout algorithm.
	 */
	public BioLayoutAlgorithmTask(final String name, final BioLayoutContext context,
				  final boolean singlePartition) {
		super(name, context, singlePartition);
		
		if (edgeWeighter == null)
			edgeWeighter = new EdgeWeighter();
	}

	/**
	 * Tells Cytoscape whether we support selected nodes only or not
	 *
	 * @return  true - we do support selected only
	 */
	public boolean supportsSelectedOnly() { return true; }


	/**
	 * Tells Cytoscape whether we support edge attribute based layouts
	 *
	 * @return  null if supportWeights is false, otherwise return the attribute
	 *          types that can be used for weights.
	 */
	public Set<Class<?>> supportsEdgeAttributes() {
		Set<Class<?>> ret = new HashSet<Class<?>>();
		if (!supportWeights)
			return ret;

		ret.add(Integer.class);
		ret.add(Double.class);

		return ret;
	}

	/**
	 * Returns "(unweighted)", which is the "attribute" we
	 * use to tell the algorithm not to use weights
	 *
	 * @returns List of our "special" weights
	 */
	public List<String> getInitialAttributeList() {
		ArrayList<String> list = new ArrayList<String>();
		list.add(UNWEIGHTEDATTRIBUTE);

		return list;
	}

	/**
	 * Sets the debug flag
	 *
	 * @param flag boolean value that turns debugging on or off
	 */
	public void setDebug(boolean flag) {
		debug = flag;
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @param value DOCUMENT ME!
	 */
	public void setDebug(String value) {
		Boolean val = new Boolean(value);
		debug = val.booleanValue();
	}

	/**
	 * Main function that must be implemented by the child class.
	 */
	public abstract void layoutPartion(LayoutPartition partition);

	protected void initialize_local() {
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @param message DOCUMENT ME!
	 */
	public static void debugln(String message) {
		if (debug) {
			System.err.println(message);
		}
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @param message DOCUMENT ME!
	 */
	public static void debug(String message) {
		if (debug) {
			System.err.print(message);
		}
	}
}
