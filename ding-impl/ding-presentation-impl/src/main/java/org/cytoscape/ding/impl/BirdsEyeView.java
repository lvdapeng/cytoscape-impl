package org.cytoscape.ding.impl;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.print.Printable;
import java.util.Arrays;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.UIManager;

import org.cytoscape.ding.CyActivator;
import org.cytoscape.ding.debug.DebugProgressMonitor;
import org.cytoscape.ding.debug.FrameType;
import org.cytoscape.ding.impl.canvas.CompositeImageCanvas;
import org.cytoscape.ding.impl.work.ProgressMonitor;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkViewSnapshot;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.RenderingEngine;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

@SuppressWarnings("serial")
public final class BirdsEyeView extends JComponent implements RenderingEngine<CyNetwork> {

	private static final Dimension MIN_SIZE = new Dimension(180, 180);
	private final Color VIEW_WINDOW_COLOR;
	private final Color VIEW_WINDOW_BORDER_COLOR;
	
	
	// Ratio of the graph image to panel size 
	private static final double SCALE_FACTOR = 0.97;
		
	private final double[] extents = new double[4];
	
	private final DRenderingEngine re;
	private final CompositeImageCanvas canvas;
	private boolean contentChanged = true;
	
	
	public BirdsEyeView(DRenderingEngine re, CyServiceRegistrar registrar) {
		this.re = re;
		
		Color c = UIManager.getColor("Table.focusCellBackground");
		VIEW_WINDOW_COLOR = new Color(c.getRed(), c.getGreen(), c.getBlue(), 60);
		c = UIManager.getColor("Table.background");
		VIEW_WINDOW_BORDER_COLOR = new Color(c.getRed(), c.getGreen(), c.getBlue(), 90);
		
		var mouseListener = new InnerMouseListener();
		addMouseListener(mouseListener);
		addMouseMotionListener(mouseListener);
		addMouseWheelListener(mouseListener);
		
		setPreferredSize(MIN_SIZE);
		setMinimumSize(MIN_SIZE);
		
		var lod = new BirdsEyeViewLOD(re.getGraphLOD());
		canvas = new CompositeImageCanvas(re, lod);
		
		re.addTransformChangeListener(t -> {
			repaint();
		});
		re.addContentChangeListener(() -> {
			contentChanged = true;
			canvas.setBackgroundPaint(re.getBackgroundColor());
			repaint();
		});
	}	
	
	
	/**
	 * Returns the extents of the nodes, in node coordinates.
	 */
	private boolean getNetworkExtents(double[] extents) {
		CyNetworkViewSnapshot netViewSnapshot = re.getViewModelSnapshot();
		if(netViewSnapshot.getNodeCount() == 0) {
			Arrays.fill(extents, 0.0);
			return false;
		}
		netViewSnapshot.getSpacialIndex2D().getMBR(extents);
		return true;
	}
	
	
	private void fitCanvasToNetwork() {
		boolean hasComponents = getNetworkExtents(extents);
		hasComponents |= re.getCyAnnotator().adjustBoundsToIncludeAnnotations(extents);
		
		double myXCenter;
		double myYCenter;
		double myScaleFactor;
		
		if(hasComponents) {
			myXCenter = (extents[0] + extents[2]) / 2.0d;
			myYCenter = (extents[1] + extents[3]) / 2.0d;
			myScaleFactor = SCALE_FACTOR * 
			                  Math.min(((double) getWidth())  / (extents[2] - extents[0]), 
			                           ((double) getHeight()) / (extents[3] - extents[1]));
		} else {
			myXCenter = 0.0d;
			myYCenter = 0.0d;
			myScaleFactor = 1.0d;
		}
		
		canvas.setCenter(myXCenter, myYCenter);
		canvas.setScaleFactor(myScaleFactor);
	}
	
	@Override
	public void setBounds(int x, int y, int width, int height) {
		if(width == getWidth() && height == getHeight())
			return;
		super.setBounds(x, y, width, height);
		contentChanged = true;
		canvas.setViewport(width, height);
	}
	
	
	@Override 
	public void paintComponent(Graphics g) {
		if(contentChanged) {
			// render a new image
			fitCanvasToNetwork();
			// I guess we are assuming that rendering the BirdsEyeView is fast
			var pm = getProgressMonitor();
			canvas.paintOnCurrentThread(pm);
			contentChanged = false;
		} 
		g.drawImage(canvas.getImage(), 0, 0, null);
		drawRectangle(g);
	}
	
	private ProgressMonitor getProgressMonitor() {
		if(CyActivator.DEBUG)
			return new DebugProgressMonitor(FrameType.BIRDS_EYE_VIEW, null, re.getDebugCallback());
		return null;
	}
	
	private void drawRectangle(Graphics g) {
		// extents of network viewable in birds-eye-view (node coords)
		double myXCenter = canvas.getTransform().getCenterX();
		double myYCenter = canvas.getTransform().getCenterY();
		double myScaleFactor = canvas.getTransform().getScaleFactor();
		
		// get extents of the main network view (node coords)
		double viewXCenter = re.getTransform().getCenterX();
		double viewYCenter = re.getTransform().getCenterY();
		double viewScaleFactor = re.getTransform().getScaleFactor();
		
		// Swing width/height of main view (image coords)
		int viewWidth  = re.getTransform().getWidth();
		int viewHeight = re.getTransform().getHeight();
		
		// Compute view area
		final int rectWidth  = (int) (myScaleFactor * (((double) viewWidth)  / viewScaleFactor));
		final int rectHeight = (int) (myScaleFactor * (((double) viewHeight) / viewScaleFactor));
		
		final double rectXCenter = (((double) getWidth())  / 2.0d) + (myScaleFactor * (viewXCenter - myXCenter));
		final double rectYCenter = (((double) getHeight()) / 2.0d) + (myScaleFactor * (viewYCenter - myYCenter));
		
		final int x = (int) (rectXCenter - (rectWidth  / 2));
		final int y = (int) (rectYCenter - (rectHeight / 2));
		
		// Draw the view area window		
		g.setColor(VIEW_WINDOW_COLOR);
		g.fillRect(x, y, rectWidth, rectHeight);
		g.setColor(VIEW_WINDOW_BORDER_COLOR);
		g.drawRect(x, y, rectWidth, rectHeight);
	}
	

	private final class InnerMouseListener extends MouseAdapter {

		private int currMouseButton = 0;
		private int lastXMousePos = 0;
		private int lastYMousePos = 0;
		
		@Override public void mousePressed(MouseEvent e) {
			if(e.getButton() == MouseEvent.BUTTON1) {
				currMouseButton = 1;
				lastXMousePos = e.getX(); // needed by drag listener
				lastYMousePos = e.getY();
				
				double myXCenter = canvas.getTransform().getCenterX();
				double myYCenter = canvas.getTransform().getCenterY();
				double myScaleFactor = canvas.getTransform().getScaleFactor();
				
				double halfWidth  = (double)getWidth() / 2.0d;
				double halfHeight = (double)getHeight() / 2.0d;
				
				double centerX = ((lastXMousePos - halfWidth)  / myScaleFactor) + myXCenter;
				double centerY = ((lastYMousePos - halfHeight) / myScaleFactor) + myYCenter;
				
				re.setCenter(centerX, centerY);
				re.updateView(true);
			}
		}

		@Override 
		public void mouseReleased(MouseEvent e) {
			if(e.getButton() == MouseEvent.BUTTON1) {
				if(currMouseButton == 1) {
					currMouseButton = 0;
				}
			}
			re.updateView(true);
		}
		
		@Override
		public void mouseDragged(MouseEvent e) {
			if(currMouseButton == 1) {
				int currX = e.getX();
				int currY = e.getY();
				double deltaX = 0;
				double deltaY = 0;
				
				double myScaleFactor = canvas.getTransform().getScaleFactor();
				
				if (!re.getViewModel().isValueLocked(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION)) {
					deltaX = (currX - lastXMousePos) / myScaleFactor;
					lastXMousePos = currX;
				}
				if (!re.getViewModel().isValueLocked(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION)) {
					deltaY = (currY - lastYMousePos) / myScaleFactor;
					lastYMousePos = currY;
				}
				if (deltaX != 0 || deltaY != 0) {
					re.pan(deltaX, deltaY);
					re.updateView(false);
				}
			}
		}
		
		@Override 
		public void mouseWheelMoved(MouseWheelEvent e) {
			re.getInputHandlerGlassPane().processMouseWheelEvent(e);
		}
	}
	
	
	@Override
	public Dimension getMinimumSize() {
		return MIN_SIZE;
	}

	@Override
	public View<CyNetwork> getViewModel() {
		return re.getViewModel();
	}

	@Override
	public VisualLexicon getVisualLexicon() {
		return re.getVisualLexicon();
	}

	@Override
	public Properties getProperties() {
		return re.getProperties();
	}
	
	@Override
	public Printable createPrintable() {
		return re.createPrintable();
	}

	@Override
	public <V> Icon createIcon(VisualProperty<V> vp, V value, int width, int height) {
		return re.createIcon(vp, value, width, height);
	}

	@Override
	public void printCanvas(Graphics printCanvas) {
		throw new UnsupportedOperationException("Printing is not supported for Bird's eye view.");
	}

	@Override
	public void dispose() {
	}

	@Override
	public String getRendererId() {
		return DingRenderer.ID;
	}
	
}
