package org.cytoscape.ding.impl;

import java.awt.BorderLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.RootPaneContainer;
import javax.swing.Timer;

import org.cytoscape.ding.DVisualLexicon;
import org.cytoscape.ding.PrintLOD;
import org.cytoscape.ding.debug.DebugCallback;
import org.cytoscape.ding.icon.VisualPropertyIconFactory;
import org.cytoscape.ding.impl.cyannotator.AnnotationFactoryManager;
import org.cytoscape.ding.impl.cyannotator.CyAnnotator;
import org.cytoscape.ding.impl.work.ProgressMonitor;
import org.cytoscape.ding.internal.util.CoalesceTimer;
import org.cytoscape.ding.internal.util.ViewUtil;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.graph.render.stateful.EdgeDetails;
import org.cytoscape.graph.render.stateful.GraphLOD;
import org.cytoscape.graph.render.stateful.NodeDetails;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewConfig;
import org.cytoscape.view.model.CyNetworkViewListener;
import org.cytoscape.view.model.CyNetworkViewSnapshot;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.model.events.UpdateNetworkPresentationEvent;
import org.cytoscape.view.model.spacial.SpacialIndex2D;
import org.cytoscape.view.model.spacial.SpacialIndex2DFactory;
import org.cytoscape.view.presentation.RenderingEngine;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.values.HandleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2016 The Cytoscape Consortium
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


public class DRenderingEngine implements RenderingEngine<CyNetwork>, Printable, CyNetworkViewListener {

	private static final Logger logger = LoggerFactory.getLogger(DRenderingEngine.class);
	protected static int DEF_SNAPSHOT_SIZE = 400;
	
	private final CyServiceRegistrar serviceRegistrar;
	private final CyEventHelper eventHelper;
	
	private final DVisualLexicon lexicon;

	private final CyNetworkView viewModel;
	private CyNetworkViewSnapshot viewModelSnapshot;
	
	// Common object lock used for state synchronization
	final DingLock dingLock = new DingLock();

	private final NodeDetails nodeDetails;
	private final EdgeDetails edgeDetails;
	
	private PrintLOD printLOD;
	private final DingGraphLODAll dingGraphLODAll = new DingGraphLODAll();
	private final DingGraphLOD dingGraphLOD;

	private CompositeCanvas fastCanvas; // treat this as the 'main' canvas
	private CompositeCanvas slowCanvas;
	
	private RendererComponent renderComponent;
	private NetworkPicker picker;
	private FontMetrics fontMetrics;
	
	// Snapshot of current view.  Will be updated by CONTENT_CHANGED event.
	private BufferedImage snapshotImage;
	// Represents current snapshot is latest version or not.
	private boolean latest;

	private final Properties props;
	private final CyAnnotator cyAnnotator;
	private boolean largeModel = false;
	
	//Flag that indicates that the content has changed and the graph needs to be redrawn.
	private volatile boolean contentChanged;
	// State variable for when zooming/panning have changed.
	private volatile boolean transformChanged;

	private final List<ContentChangeListener> contentChangeListeners = new CopyOnWriteArrayList<>();
	private final List<TransformChangeListener> transformChangeListeners = new CopyOnWriteArrayList<>();
	
//	private Timer animationTimer;
	private final Timer checkDirtyTimer;
	private final CoalesceTimer coalesceTimer;
	
	private final BendStore bendStore;
	private InputHandlerGlassPane inputHandler = null;
	private ExecutorService executor;
	
	public DRenderingEngine(
			final CyNetworkView view,
			final DVisualLexicon dingLexicon,
			final AnnotationFactoryManager annMgr,
			final DingGraphLOD dingGraphLOD,
			final HandleFactory handleFactory,
			final CyServiceRegistrar registrar
	) {
		this.serviceRegistrar = registrar;
		this.eventHelper = registrar.getService(CyEventHelper.class);
		this.props = new Properties();
		this.viewModel = view;
		this.lexicon = dingLexicon;
		this.dingGraphLOD = dingGraphLOD;
		
		this.executor = Executors.newCachedThreadPool(r -> {
			Thread thread = Executors.defaultThreadFactory().newThread(r);
			thread.setName("ding-" + thread.getName());
			return thread;
		});
		
		SpacialIndex2DFactory spacialIndexFactory = registrar.getService(SpacialIndex2DFactory.class);
		this.bendStore = new BendStore(this, handleFactory, spacialIndexFactory);
		
		nodeDetails = new DNodeDetails(this, registrar);
		edgeDetails = new DEdgeDetails(this);
		printLOD = new PrintLOD();
		
		fastCanvas = new CompositeCanvas(registrar, this, dingGraphLOD.faster());
		slowCanvas = new CompositeCanvas(registrar, this, dingGraphLOD);
		
		renderComponent = new RendererComponent();
		picker = new NetworkPicker(this, null);

		// Finally, intialize our annotations
		cyAnnotator = new CyAnnotator(this, annMgr, registrar);
		registrar.registerService(cyAnnotator, SessionAboutToBeSavedListener.class, new Properties());
		
		//Updating the snapshot for nested networks
//		this.addContentChangeListener(new DGraphViewContentChangeListener());

		CyNetworkViewSnapshot snapshot = view.createSnapshot();
		if (!dingGraphLOD.detail(snapshot.getNodeCount(), snapshot.getEdgeCount()))
			largeModel = true;

		viewModelSnapshot = viewModel.createSnapshot();
		
//		cyAnnotator.loadAnnotations();
		
		coalesceTimer = new CoalesceTimer();
		
		// Check if the view model has changed approximately 30 times per second
		checkDirtyTimer = new Timer(30, e -> checkModelIsDirty());
		checkDirtyTimer.setRepeats(true);
		checkDirtyTimer.start();
	}
	
	
	public void install(RootPaneContainer rootPane) {
		InputHandlerGlassPane glassPane = getInputHandlerGlassPane();
		rootPane.setGlassPane(glassPane);
		rootPane.setContentPane(renderComponent);
		glassPane.setVisible(true);
	}
	
	public void install(JComponent component) {
		component.setLayout(new BorderLayout());
		component.add(renderComponent, BorderLayout.CENTER);
	}
	
	public void setDebugCallback(DebugCallback callback) {
		renderComponent.setDebugCallback(callback);
	}
	
	
	/**
	 * This is the interface between the renderer and Swing.
	 */
	@SuppressWarnings("serial")
	private class RendererComponent extends JComponent {
		
		private boolean annotationsLoaded = false;
		private ImageFuture slowFuture;
		private ImageFuture fastFuture;
		
		private DebugCallback debugCallback;
		
		public void setDebugCallback(DebugCallback callback) {
			this.debugCallback = callback;
		}
		
		@Override
		public void setBounds(int x, int y, int width, int height) {
			if(width == getWidth() && height == getHeight())
				return;
			
			super.setBounds(x, y, width, height);
			fastCanvas.setViewport(width, height);
			slowCanvas.setViewport(width, height);
			
			// If this is the first call to setBounds, load any annotations
			if(!annotationsLoaded) {
				annotationsLoaded = true;
				// MKTODO we should not load annotations here!
				cyAnnotator.loadAnnotations();
			}
			
			getViewModel().batch(netView -> {
				netView.setVisualProperty(BasicVisualLexicon.NETWORK_WIDTH,  (double) width);
				netView.setVisualProperty(BasicVisualLexicon.NETWORK_HEIGHT, (double) height);
			}, false); // don't set the dirty flag
			
			updateView(true);
		}
		
		public void updateView(boolean startRenderSlow) {
			ViewUtil.invokeOnEDTAndWait(() -> {
				// run this on the EDT so there is no race condition with paint() ?????
				if(slowFuture != null) {
					slowFuture.cancel();
					slowFuture = null;
				}
				if(fastFuture != null) {
					fastFuture.cancel();
					fastFuture = null;
				}
				
				// don't do this constantly while panning
				if(startRenderSlow) {
					ProgressMonitor pm = getInputHandlerGlassPane().createProgressMonitor();
					long startTime = System.currentTimeMillis();
					slowFuture = slowCanvas.startPainting(pm, executor);
					slowFuture.thenRun(this::repaint);
					slowFuture.thenRun(() -> {
						if(debugCallback != null) {
							long endTime = System.currentTimeMillis();
							long time = endTime - startTime;
							debugCallback.addFrameTime(false, time);
						}
					});
				}
				
				repaint();
			});
		}
		
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			ImageFuture future;
			
			// MKTODO add framerate monitor?
			// MKTODO compare render flags to see if a slow frame is even needed
			// MKTODO make sure that the progress bar repainting is using a clip
			// MKTODO try rendering on a single threaded thread pool
			// MKTODO fast renderering could just render on the current thread and return a completed future
			
			if(slowFuture != null && slowFuture.isDone()) {
				System.out.println("PAINT slowFuture isDone");
				future = slowFuture;
			} else if(fastFuture != null) {
				System.out.println("PAINT fastFuture isDone");
				future = fastFuture;
			} else {
				System.out.println("PAINT fastFuture startPainting");
				long startTime = System.currentTimeMillis();
				fastFuture = fastCanvas.startPainting(executor);
				fastFuture.thenRun(() -> {
					if(debugCallback != null) {
						long endTime = System.currentTimeMillis();
						long time = endTime - startTime;
						debugCallback.addFrameTime(true, time);
					}
				});
				future = fastFuture;
			}
			
			Image image = future.join();
			picker.setRenderDetailFlags(future.getLastRenderDetail());
			g.drawImage(image, 0, 0, null);
		}
		
		@Override
		public void update(Graphics g) {
			if(fontMetrics == null) {
				fontMetrics = g.getFontMetrics(); // needed to compute label widths
			}
			super.update(g);
		}
	}
	
	
	public Rectangle getComponentBounds() {
		return renderComponent.getBounds();
	}
	
	public NetworkPicker getPicker() {
		return picker;
	}

	public NetworkTransform getTransform() {
		return fastCanvas.getTransform();
	}
	
	public GraphLOD getGraphLOD() {
		return dingGraphLOD;
	}
	
	
	/**
	 * TEMPORARY
	 * 
	 * This is being called by a Swing Timer, so this method is being run on the EDT.
	 * Painting is also done on the EDT. This is how we make sure that viewModelSnapshot does not
	 * change while a frame is being rendered.
	 * 
	 * Also the EDT will coalesce paint events, so if the timer runs faster than the frame rate the
	 * EDT will take care of that.
	 * 
	 * MKTODO Move drawing off the EDT.
	 * If we move drawing off the EDT then we need another solution for ensuring that viewModelSnapshot
	 * does not get re-assigned while a frame is being drawn.
	 */
	private void checkModelIsDirty() {
		final boolean updateModel = viewModel.isDirty();
		final boolean updateView = updateModel || contentChanged;
		
		if(updateModel) {
			updateModel();
		}
		if(updateView) {
			updateView(true);
		}
		contentChanged = false;
	}
	
	private void updateModelAndView() {
		updateModel();
		updateView(true);
	}
	
	public void updateView() {
		updateView(true);
	}
	
	public void updateView(boolean startSlowPaint) {
		if(contentChanged)
			fireContentChanged();
		if(transformChanged)
			fireTransformChanged();
		
		setContentChanged(false);
		setTransformChanged(false);
	
		renderComponent.updateView(startSlowPaint);
		
		// Fire this event on another thread (and debounce) so that it doesn't block the renderer
		// MKTODO should this go here???
		coalesceTimer.coalesce(() -> eventHelper.fireEvent(new UpdateNetworkPresentationEvent(getViewModel())));
	}
	
	private void updateModel() {
		// create a new snapshot, this should be very fast
		viewModelSnapshot = viewModel.createSnapshot(); // sets viewModel.isDirty() to false
		
		// Check for important changes between snapshots
		Paint backgroundPaint = viewModelSnapshot.getVisualProperty(BasicVisualLexicon.NETWORK_BACKGROUND_PAINT);
		fastCanvas.setBackgroundPaint(backgroundPaint);
		slowCanvas.setBackgroundPaint(backgroundPaint);
		
		Collection<View<CyEdge>> selectedEdges = viewModelSnapshot.getTrackedEdges(CyNetworkViewConfig.SELECTED_EDGES);
		bendStore.updateSelectedEdges(selectedEdges);
		
//		Collection<View<CyEdge>> animatedEdges = viewModelSnapshot.getTrackedEdges(DingNetworkViewFactory.ANIMATED_EDGES);
//		edgeDetails.updateAnimatedEdges(animatedEdges);
//		if(animatedEdges.isEmpty() && animationTimer != null) {
//			animationTimer.stop();
//			animationTimer = null;
//		} else if(!animatedEdges.isEmpty() && animationTimer == null) {
//			animationTimer = new Timer(200, e -> advanceAnimatedEdges());
//			animationTimer.setRepeats(true);
//			animationTimer.start();
//		}
		
		// update LOD
		boolean hd = viewModelSnapshot.getVisualProperty(DVisualLexicon.NETWORK_FORCE_HIGH_DETAIL);
		// regular canvas still 
//		fastCanvas.setLOD(dingGraphLOD.faster()); // never change the lod on the fast canvas ???
		slowCanvas.setLOD(hd ? dingGraphLODAll : dingGraphLOD);
		
		// update view (for example if "fit selected" was run)
		double x = viewModelSnapshot.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION);
		double y = viewModelSnapshot.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION);
		slowCanvas.setCenter(x, y);
		fastCanvas.setCenter(x, y);
		
		double scaleFactor = viewModelSnapshot.getVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR);
		slowCanvas.setScaleFactor(scaleFactor);
		fastCanvas.setScaleFactor(scaleFactor);
		
		setContentChanged(false);
//		updateView(true);
	}
	
	
//	private void advanceAnimatedEdges() {
//		edgeDetails.advanceAnimatedEdges();
//		// This is more lightweight than calling updateView(). And if the animation thread is faster 
//		// than the renderer the EDT will coalesce the extra paint events.
//		setContentChanged();
//		networkCanvas.repaint();
//	}
	
	
//	public boolean adjustBoundsToIncludeAnnotations(double[] extentsBuff) {
//		return cyAnnotator.adjustBoundsToIncludeAnnotations(extentsBuff);
//	}
	
	public BendStore getBendStore() {
		return bendStore;
	}
	
	public synchronized InputHandlerGlassPane getInputHandlerGlassPane() {
		if(inputHandler == null) {
			inputHandler = new InputHandlerGlassPane(serviceRegistrar, this);
		}
		return inputHandler;
	}
	
	public ExecutorService getExecutorService() {
		return executor;
	}
	
	/**
	 * Mainly for using as a parent when showing dialogs and menus.
	 */
	public JComponent getComponent() {
		return getInputHandlerGlassPane();
	}

	
	public NodeDetails getNodeDetails() {
		return nodeDetails;
	}
	
	public EdgeDetails getEdgeDetails() {
		return edgeDetails;
	}
	
	
	
	public void setContentChanged() {
		setContentChanged(true);
	}
	
	private void setContentChanged(boolean b) {
		contentChanged = b;
	}
	
	private void fireContentChanged() {
		for(ContentChangeListener l : contentChangeListeners) {
			l.contentChanged();
		}
	}
	
	public void addContentChangeListener(ContentChangeListener l) {
		contentChangeListeners.add(l);
	}

	public void removeContentChangeListener(ContentChangeListener l) {
		contentChangeListeners.remove(l);
	}

	public void setTransformChanged() {
		setTransformChanged(true);
	}
	
	private void setTransformChanged(boolean b) {
		this.transformChanged = b;
	}
	
	private void fireTransformChanged() {
		// MKTODO should we use an immutable copy of the transform here?, do we even need to pass the transform to the listener?
		NetworkTransform transform = fastCanvas.getTransform();
		for(TransformChangeListener l : transformChangeListeners) {
			l.transformChanged(transform);
		}
	}
	
	public void addTransformChangeListener(TransformChangeListener l) {
		transformChangeListeners.add(l);
	}
	
	public void removeTransformChangeListener(TransformChangeListener l) {
		transformChangeListeners.remove(l);
	}
	
	

	public boolean isLargeModel() {
		return largeModel;
	}
	
	public PrintLOD getPrintLOD() {
		return printLOD;
	}
	
	/**
	 * Set the zoom level and redraw the view.
	 */
	public void setZoom(double zoom) {
		synchronized (dingLock) {
			slowCanvas.setScaleFactor(checkZoom(zoom, slowCanvas.getTransform().getScaleFactor()));
			fastCanvas.setScaleFactor(checkZoom(zoom, fastCanvas.getTransform().getScaleFactor()));
		}
	}
	
	public double getZoom() {
		return slowCanvas.getTransform().getScaleFactor();
	}
	
	private void fitContent(final boolean updateView) {
		eventHelper.flushPayloadEvents();

		synchronized (dingLock) {
			// make sure we use the latest snapshot
			CyNetworkViewSnapshot netViewSnapshot = viewModel.createSnapshot();
			if(netViewSnapshot.getNodeCount() == 0)
				return;
			
			NetworkTransform transform = fastCanvas.getTransform();
			if(transform.getWidth() == 0 || transform.getHeight() == 0)
				return;
			
			final double[] extentsBuff = new double[4];
			netViewSnapshot.getSpacialIndex2D().getMBR(extentsBuff); // extents of the network
			cyAnnotator.adjustBoundsToIncludeAnnotations(extentsBuff); // extents of the annotation canvases

			netViewSnapshot.getMutableNetworkView().batch(netView -> {
				if (!netView.isValueLocked(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION))
					netView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION, (extentsBuff[0] + extentsBuff[2]) / 2.0d);
				
				if (!netView.isValueLocked(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION))
					netView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION, (extentsBuff[1] + extentsBuff[3]) / 2.0d);
	
				if (!netView.isValueLocked(BasicVisualLexicon.NETWORK_SCALE_FACTOR)) {
					// Apply a factor 0.98 to zoom, so that it leaves a small border around the network and any annotations.
					final double zoom = Math.min(((double) transform.getWidth())  /  (extentsBuff[2] - extentsBuff[0]), 
					                             ((double) transform.getHeight()) /  (extentsBuff[3] - extentsBuff[1])) * 0.98;
					// Update view model.  Zoom Level should be modified.
					netView.setVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR, zoom);
				}
			});
		}
		
		// MKTODO is this necessary, the timer will check the dirty flag
		if (updateView)
			updateModelAndView();
	}
	
	@Override
	public void handleFitContent() {
		fitContent(/* updateView = */ true);
	}
	
	@Override
	public void handleUpdateView() {
		updateModelAndView();
	}
	
	
	public void zoom(int ticks) {
		if(getViewModelSnapshot().isValueLocked(BasicVisualLexicon.NETWORK_SCALE_FACTOR))
			return;
		
		double factor;
		if (ticks < 0)
			factor = 1.1; // scroll up, zoom in
		else if (ticks > 0)
			factor = 0.9; // scroll down, zoom out
		else
			return;
		
		double scaleFactor = fastCanvas.getTransform().getScaleFactor() * factor;
		
//			setHideEdges();
		setZoom(scaleFactor);
		
		getViewModel().batch(netView -> {
			netView.setVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR, scaleFactor);
		}, false);
		
//		updateView();
	}
	
	
	public void pan(double deltaX, double deltaY) {
		synchronized (dingLock) {
			NetworkTransform transform = fastCanvas.getTransform();
			double x = transform.getCenterX() + deltaX;
			double y = transform.getCenterY() + deltaY;
			setCenter(x, y);
		}
		
//		updateView();
	}
	
	public void setCenter(double x, double y) {
		synchronized (dingLock) {
			fastCanvas.setCenter(x,y);
			slowCanvas.setCenter(x,y);
			
			// Update view model
			// TODO: don't do it from here?
			getViewModel().batch(netView -> {
				NetworkTransform transform = fastCanvas.getTransform();
				netView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION, transform.getCenterX());
				netView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION, transform.getCenterY());
			}, false); // don't set the dirty flag
		}
	}

	
	@Override
	public void handleFitSelected() {
		eventHelper.flushPayloadEvents();
		
		CyNetworkViewSnapshot netViewSnapshot = getViewModelSnapshot();
		SpacialIndex2D<Long> spacial = netViewSnapshot.getSpacialIndex2D();
		Collection<View<CyNode>> selectedElms = netViewSnapshot.getTrackedNodes(CyNetworkViewConfig.SELECTED_NODES);
		if(selectedElms.isEmpty())
			return;
		
		float[] extentsBuff = new float[4];

		float xMin = Float.POSITIVE_INFINITY;
		float yMin = Float.POSITIVE_INFINITY;
		float xMax = Float.NEGATIVE_INFINITY;
		float yMax = Float.NEGATIVE_INFINITY;

		View<CyNode> leftMost = null;
		View<CyNode> rightMost = null;

		for(View<CyNode> nodeView : selectedElms) {
			spacial.get(nodeView.getSUID(), extentsBuff);
			if (extentsBuff[0] < xMin) {
				xMin = extentsBuff[0];
				leftMost = nodeView;
			}

			if (extentsBuff[2] > xMax) {
				xMax = extentsBuff[2];
				rightMost = nodeView;
			}

			yMin = Math.min(yMin, extentsBuff[1]);
			yMax = Math.max(yMax, extentsBuff[3]);
		}

		float xMinF = xMin - (getLabelWidth(leftMost) / 2);
		float xMaxF = xMax + (getLabelWidth(rightMost) / 2);
		float yMaxF = yMax;
		float yMinF = yMin;

		netViewSnapshot.getMutableNetworkView().batch(netView -> {
			NetworkTransform transform = fastCanvas.getTransform();
			if (!netView.isValueLocked(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION)) {
				double zoom = Math.min(((double) transform.getWidth()) / (((double) xMaxF) - ((double) xMinF)),
						((double) transform.getHeight()) / (((double) yMaxF) - ((double) yMinF)));
				zoom = checkZoom(zoom, transform.getScaleFactor());
				
				// Update view model.  Zoom Level should be modified.
				netView.setVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR, zoom);
			}
			
			if (!netView.isValueLocked(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION)) {
				double xCenter = (((double) xMinF) + ((double) xMaxF)) / 2.0d;
				netView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION, xCenter);
			}
			
			if (!netView.isValueLocked(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION)) {
				double yCenter = (((double) yMinF) + ((double) yMaxF)) / 2.0d;
				netView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION, yCenter);
			}
		});
			
		updateModelAndView();
	}

	
	private int getLabelWidth(View<CyNode> nodeView) {
		if (nodeView == null || fontMetrics == null)
			return 0;

		String s = nodeDetails.getLabelText(nodeView);
		if (s == null)
			return 0;

		char[] lab = s.toCharArray();
		return fontMetrics.charsWidth(lab, 0, lab.length);
	}


	public void setPrintingTextAsShape(boolean textAsShape) {
		synchronized (dingLock) {
			printLOD.setPrintingTextAsShape(textAsShape);
		}
	}

	
	@Override
	public int print(Graphics g, PageFormat pageFormat, int page) {
		if(page != 0)
			return NO_SUCH_PAGE;
		
		((Graphics2D) g).translate(pageFormat.getImageableX(), pageFormat.getImageableY());

		// make sure the whole image on the screen will fit to the printable
		// area of the paper
		NetworkTransform transform = fastCanvas.getTransform();
		double image_scale = Math.min(pageFormat.getImageableWidth()  / transform.getWidth(),
									  pageFormat.getImageableHeight() / transform.getHeight());

		if (image_scale < 1.0d) {
			((Graphics2D) g).scale(image_scale, image_scale);
		}

		// old school
		// g.clipRect(0, 0, getComponent().getWidth(),
		// getComponent().getHeight());
		// getComponent().print(g);

		// from InternalFrameComponent
		g.clipRect(0, 0, renderComponent.getWidth(), renderComponent.getHeight());
//		canvas.print(g);

		return PAGE_EXISTS;
	}


	
	/**
	 * Method to return a reference to an Image object, which represents the current network view.
	 *
	 * @param width Width of desired image.
	 * @param height Height of desired image.
	 * @param shrink Percent to shrink the network shown in the image.
	 * This doesn't shrink the image, just the network shown, as if the user zoomed out.
	 * Can be between 0 and 1, if not it will default to 1.  
	 */
	private Image createImage(int width, final int height, double scale) {
		if (width < 0 || height < 0)
			throw new IllegalArgumentException("width and height arguments must be greater than zero");
		if (scale < 0 || scale > 1.0)
			scale = 1.0;

		final Image image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		
		NetworkTransform transform = slowCanvas.getTransform();
		int origWidth   = transform.getWidth();
		int origHeight  = transform.getHeight();
		double origZoom = transform.getScaleFactor();
		
		CyNetworkViewSnapshot netVewSnapshot = getViewModelSnapshot();
		Double centerX = netVewSnapshot.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION);
		Double centerY = netVewSnapshot.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION);
		Double scaleFactor = netVewSnapshot.getVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR);
		
		
		slowCanvas.setViewport(width, height);
		fitContent(true);
		setZoom(slowCanvas.getTransform().getScaleFactor() * scale);
		
//		Graphics g = image.getGraphics();
//		slowCanvas.paint(g, null);
		
		ImageFuture future = slowCanvas.startPainting(executor);
		Image frame = future.join(); // block and wait for render to complete
		Graphics g = image.getGraphics();
		g.drawImage(frame, 0, 0, null);
		
		slowCanvas.setViewport(origWidth, origHeight);
		setZoom(origZoom);
		
		CyNetworkView netView = getViewModel();
		netView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION, centerX);
		netView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION, centerY);
		netView.setVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR, scaleFactor);
		
		updateModelAndView();
		
		return image;
	}

	private double checkZoom(double zoom, double orig) {
		if (zoom > 0)
			return zoom;

		logger.debug("invalid zoom: " + zoom + "   using orig: " + orig);
		return orig;
	}

	/**
	 * This method is used by freehep lib to export network as graphics.
	 */
	public void print(Graphics g) {
//		boolean opaque = backgroundCanvas.isOpaque();
//		boolean transparentBackground = "true".equalsIgnoreCase(props.getProperty("exportTransparentBackground"));
//		backgroundCanvas.setOpaque(!transparentBackground);
//		backgroundCanvas.print(g);
//		
//		backgroundCanvas.setOpaque(opaque); // restore the previous opaque value
//		networkCanvas.print(g);
//		foregroundCanvas.print(g);
	}
//
//	/**
//	 * This method is used by BitmapExporter to export network as graphics (png, jpg, bmp)
//	 */
//	public void printNoImposter(Graphics g) {
//		backgroundCanvas.print(g);
//		networkCanvas.printNoImposter(g);
//		foregroundCanvas.print(g);
//	}


	@Override
	public Printable createPrintable() {
		return this;
	}
	
	@Override
	public Properties getProperties() {
		return this.props;
	}
	
	@Override 
	public Image createImage(int width, int height) {
		return createImage(width, height, 1);
	}

	@Override
	public DVisualLexicon getVisualLexicon() {
		return lexicon;
	}


	// For now the viewModelSnapshot should only be re-assigned on the EDT.
	public CyNetworkViewSnapshot getViewModelSnapshot() {
		return viewModelSnapshot;
	}
	
	@Override
	public CyNetworkView getViewModel() {
		return viewModel;
	}

	@Override
	public <V> Icon createIcon(VisualProperty<V> vp, V value, int w, int h) {
		return VisualPropertyIconFactory.createIcon(value, w, h);
	}

	/**
	 * Returns the current snapshot image of this view.
	 *
	 * <p>
	 * No unnecessary image object will be created if networks in the current
	 * session does not contain any nested network, i.e., should not have
	 * performance/memory issue.
	 *
	 * @return Image of this view.  It is always up-to-date.
	 */
	TexturePaint getSnapshot(final double width, final double height) {
		if (!latest) {
			// Need to update snapshot.
			snapshotImage = (BufferedImage) createImage(DEF_SNAPSHOT_SIZE, DEF_SNAPSHOT_SIZE, 1);
			latest = true;
		}

		// Handle non-square images
		// Get the height and width of the image
		int imageWidth = snapshotImage.getWidth();
		int imageHeight = snapshotImage.getHeight();
		double ratio = (double)imageHeight / (double) imageWidth;
		int adjustedWidth = (int)((double)width/ratio)+1;

		final Rectangle2D rect = new Rectangle2D.Double(-adjustedWidth / 2, -height / 2, adjustedWidth, height);
		final TexturePaint texturePaint = new TexturePaint(snapshotImage, rect);
		return texturePaint;
	}


	/**
	 * Listener for update flag of snapshot image.
	 */
	private final class DGraphViewContentChangeListener implements ContentChangeListener {
		public void contentChanged() {
			latest = false;
		}
	}

	@Override
	public void printCanvas(Graphics printCanvas) {
//		final boolean contentChanged = isContentChanged();
//		final boolean viewportChanged = isViewportChanged();
//		
//		// Check properties related to printing:
//		boolean exportAsShape = "true".equalsIgnoreCase(props.getProperty("exportTextAsShape"));
//		
//		setPrintingTextAsShape(exportAsShape);
//		print(printCanvas);
//		
//		// Keep previous dirty flags, otherwise the actual view canvas may not be updated next time.
//		// (this method is usually only used to export the View as image, create thumbnails, etc,
//		// therefore it should not flag the Graph View as updated, because the actual view canvas
//		// may still have to be redrawn after this).
//		setContentChanged(contentChanged);
//		setViewportChanged(viewportChanged);
	}

	public CyAnnotator getCyAnnotator() {
		return cyAnnotator;
	}
	
	
	@Override
	public void handleDispose() {
		dispose();
	}
	
	@Override
	public void dispose() {
		synchronized(this) {
			checkDirtyTimer.stop();
			coalesceTimer.shutdown();
			cyAnnotator.dispose();
			serviceRegistrar.unregisterAllServices(cyAnnotator);
			fastCanvas.dispose();
			slowCanvas.dispose();
		}
	}

	@Override
	public String getRendererId() {
		return DingRenderer.ID;
	}
	
	
}