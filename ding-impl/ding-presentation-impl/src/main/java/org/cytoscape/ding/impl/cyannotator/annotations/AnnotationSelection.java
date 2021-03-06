package org.cytoscape.ding.impl.cyannotator.annotations;

import static org.cytoscape.ding.impl.cyannotator.annotations.AnchorLocation.isEast;
import static org.cytoscape.ding.impl.cyannotator.annotations.AnchorLocation.isNorth;
import static org.cytoscape.ding.impl.cyannotator.annotations.AnchorLocation.isSouth;
import static org.cytoscape.ding.impl.cyannotator.annotations.AnchorLocation.isWest;
import static org.cytoscape.view.presentation.property.values.Position.EAST;
import static org.cytoscape.view.presentation.property.values.Position.NORTH;
import static org.cytoscape.view.presentation.property.values.Position.NORTH_EAST;
import static org.cytoscape.view.presentation.property.values.Position.NORTH_WEST;
import static org.cytoscape.view.presentation.property.values.Position.SOUTH;
import static org.cytoscape.view.presentation.property.values.Position.SOUTH_EAST;
import static org.cytoscape.view.presentation.property.values.Position.SOUTH_WEST;
import static org.cytoscape.view.presentation.property.values.Position.WEST;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.cytoscape.ding.DVisualLexicon;
import org.cytoscape.ding.impl.DRenderingEngine;
import org.cytoscape.ding.impl.cyannotator.AnnotationTree;
import org.cytoscape.ding.impl.cyannotator.CyAnnotator;
import org.cytoscape.view.presentation.annotations.Annotation;
import org.cytoscape.view.presentation.property.values.Position;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2020 The Cytoscape Consortium
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

public class AnnotationSelection implements Iterable<DingAnnotation> {
	
	public static final int HANDLER_SIZE = 8;
	/** Border width. */
	public static final int WIDTH = 1;
	public static final Color COLOR_1 = Color.GRAY;
	public static final Color COLOR_2 = Color.WHITE;
	
	private final DRenderingEngine re;
	private final CyAnnotator cyAnnotator;
	private final Set<DingAnnotation> selectedAnnotations = new HashSet<>();
	
	// node coordinates
	private Rectangle2D union; 
	private Rectangle2D savedUnion;
	
	// Everything below in image coordinates
	private final Map<Position,Rectangle> anchors = new EnumMap<>(Position.class);
	private Point movingStartOffset;
	private AnchorLocation resizingAnchor;
	
	public AnnotationSelection(CyAnnotator cyAnnotator) {
		this.cyAnnotator = cyAnnotator;
		this.re = cyAnnotator.getRenderingEngine();
	}
	
	public void add(DingAnnotation a) {
		selectedAnnotations.add(a);
		updateBounds();
	}
	
	public void remove(Annotation a) {
		selectedAnnotations.remove(a);
		updateBounds();
	}
	
	public void clear() {
		if(!selectedAnnotations.isEmpty()) {
			selectedAnnotations.clear();
			updateBounds();
		}
	}
	
	public int size() {
		return selectedAnnotations.size();
	}
	
	public boolean contains(DingAnnotation a) {
		return selectedAnnotations.contains(a);
	}

	@Override
	public Iterator<DingAnnotation> iterator() {
		return selectedAnnotations.iterator();
	}
	
	public boolean isEmpty() {
		return selectedAnnotations.isEmpty();
	}
	
	public int count() {
		return selectedAnnotations.size();
	}
	
	public Collection<DingAnnotation> getSelectedAnnotations() {
		// This method exists for clients that want to avoid ConcurrentModificationException
		return new ArrayList<>(selectedAnnotations);
	}
	
	private void saveBounds() {
		savedUnion = union;
		for(DingAnnotation da : selectedAnnotations) {
			da.saveBounds();
		}
	}

	private void updateBounds() {
		union = null;
		for(var a : this) {
			var bounds = a.getBounds();
			union = (union == null) ? bounds : union.createUnion(bounds);
		}
	}
	
	/**
	 * Returns bounds in node coordinates.
	 */
	public Rectangle2D getBounds() {
		updateBounds();
		return union;
	}
	
	public Point2D getLocation() {
		Rectangle2D bounds = getBounds();
		if(bounds == null)
			return null;
		return new Point2D.Double(bounds.getX(), bounds.getY());
	}
	
	public AnchorLocation overAnchor(int mouseX, int mouseY) {
		for(Position p : Position.values()) {
			Rectangle rect = anchors.get(p);
			if(rect != null && rect.contains(mouseX, mouseY)) {
				int mouseOffsetX = mouseX - rect.x;
				int mouseOffsetY = mouseY - rect.y;
				return new AnchorLocation(p, rect.x, rect.y, mouseOffsetX, mouseOffsetY);
			}
		}
		return null;
	}

	public void startResizing(AnchorLocation resizingAnchor) {
		this.resizingAnchor = resizingAnchor;
		saveBounds();
	}
	
	public void stopResizing() {
		this.resizingAnchor = null;
	}
	
	public boolean isResizing() {
		return resizingAnchor != null;
	}
	
	public void resizeAnnotationsRelative(int mouseX, int mouseY, boolean keepAspectRatio) {
		// compensate for the difference between the anchor location and the mouse location
		var position = resizingAnchor.getPosition();
		
		if (isNorth(position))
			mouseY += WIDTH * 4 - resizingAnchor.getMouseOffsetY();
		if (isSouth(position))
			mouseY -= resizingAnchor.getMouseOffsetY();
		if (isWest(position))
			mouseX += WIDTH * 4 - resizingAnchor.getMouseOffsetX();
		if (isEast(position))
			mouseX -= resizingAnchor.getMouseOffsetX();

		var node = re.getTransform().getNodeCoordinates(mouseX, mouseY);
		var newOutlineBounds = resize(position, savedUnion, node.getX(), node.getY(), keepAspectRatio);

		for (var a : this) {
			((AbstractAnnotation) a).resizeAnnotationRelative(savedUnion, newOutlineBounds);
			a.update();
		}
		
		updateBounds();
	}
	
	public static Rectangle2D resize(Position position, Rectangle2D bounds, double mouseX, double mouseY,
			boolean keepAspectRatio) {
		keepAspectRatio = keepAspectRatio && (position == NORTH_WEST || position == NORTH_EAST || position == SOUTH_WEST
				|| position == SOUTH_EAST);
		
		double boundsX = bounds.getX();
		double boundsY = bounds.getY();
		double boundsWidth = bounds.getWidth();
		double boundsHeight = bounds.getHeight();
		double boundsYBottom = boundsY + boundsHeight;
		double boundsXRight = boundsX + boundsWidth;

		double x = boundsX;
		double y = boundsY;
		double w = boundsWidth;
		double h = boundsHeight;

		// y and height
		if (isNorth(position)) {
			if (mouseY > boundsYBottom) {
				y = boundsYBottom;
				h = mouseY - boundsYBottom;
			} else {
				y = mouseY;
				h = boundsYBottom - mouseY;
			}
		} else if (isSouth(position)) {
			if (mouseY < boundsY) {
				y = mouseY;
				h = boundsY - mouseY;
			} else {
				h = mouseY - boundsY;
			}
		}

		// x and width
		if (isWest(position)) {
			if (mouseX > boundsXRight) {
				x = boundsXRight;
				w = mouseX - boundsXRight;
			} else {
				x = mouseX;
				w = boundsXRight - mouseX;
			}
		} else if (isEast(position)) {
			if (mouseX < boundsX) {
				x = mouseX;
				w = boundsX - mouseX;
			} else {
				w = mouseX - boundsX;
			}
		}

		if (keepAspectRatio) {
			double f1 = w / boundsWidth;
			double f2 = h / boundsHeight;
			double f = Math.max(f1, f2);
			w = boundsWidth * f;
			h = boundsHeight * f;
			
			// y
			if (isNorth(position) && mouseY < boundsYBottom)
				y = boundsYBottom - h;
			else if (isSouth(position) && mouseY < boundsY)
				y = boundsY - h;

			// x
			if (isWest(position) && mouseX < boundsXRight)
				x = boundsXRight - w;
			else if (isEast(position) && mouseX < boundsX)
				x = boundsX - w;
		}
		
		return new Rectangle2D.Double(x, y, w, h);
	}
	
	public void setMovingStartOffset(Point offset) {
		this.movingStartOffset = offset;
	}
	
	public void moveSelection(Point p) {
		moveSelection(p.x, p.y);
	}
	/**
	 * Assumes x and y are component (mouse) coordinates.
	 * Moves the selection to the given point, setMovingStartOffset() must be called first.
	 */
	public void moveSelection(int x, int y) {
		if (movingStartOffset == null)
			return;
		
		// Avoid moving the same annotation twice
		var annotationsToMove = new HashSet<>(selectedAnnotations);
		
		for (var annotation : selectedAnnotations) {
			for (var ancestor : AnnotationTree.getAncestors(annotation)) {
				if (selectedAnnotations.contains(ancestor)) {
					annotationsToMove.remove(annotation);
					break;
				}
			}
		}

		var transform = cyAnnotator.getRenderingEngine().getTransform();
		var nodePt = transform.getNodeCoordinates(x, y);
		var offsetPt = transform.getNodeCoordinates(movingStartOffset);

		double dx = nodePt.getX() - offsetPt.getX();
		double dy = nodePt.getY() - offsetPt.getY();

		for (var a : annotationsToMove) {
			a.setLocation(a.getX() + dx, a.getY() + dy);
			a.update();
		}
		
		updateBounds();
	}

	public void stopMoving() {
		this.movingStartOffset = null;
	}
	
	/**
	 * Paint the selection box.
	 */
	public void paint(Graphics2D g) {
		if (isEmpty())
			return;
		
		/* Set up all of our anti-aliasing, etc. here to avoid doing it redundantly */
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

		// High quality color rendering is ON.
		g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		// Text antialiasing is ON.
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		var originalComposite = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

		// Draw the selection box
		g.setStroke(new BasicStroke(WIDTH));
		
		var r1 = getShapeImageCoords(); // Inner line
		g.setPaint(COLOR_1);
		g.draw(r1);
		
		var r2 = new Rectangle(r1.x - WIDTH, r1.y - WIDTH, r1.width + WIDTH * 2, r1.height + WIDTH * 2); // Outer line
		g.setPaint(COLOR_2);
		g.draw(r2);

		// Draw resize anchors
		updateAnchors(r1);
		
		if (annotationSelectionEnabled()) {
			g.setPaint(COLOR_2);
			anchors.values().forEach(g::fill);
			g.setPaint(COLOR_1);
			g.setStroke(new BasicStroke(1.0f));
			anchors.values().forEach(g::draw);
		}
		
		g.setComposite(originalComposite);
	}
	
	private Rectangle getShapeImageCoords() {
		var r = re.getTransform().getImageCoordinates(union);
		
		return new Rectangle(r.x - WIDTH, r.y - WIDTH, r.width + WIDTH * 2, r.height + WIDTH * 2);
	}
	
	/**
	 * @param shape in image coords
	 */
	private void updateAnchors(Rectangle shape) {
		int s = HANDLER_SIZE;
		int w = shape.width;
		int h = shape.height;
		
		int xw = s / 2;         // x for WEST side anchors
		int xe = w + s / 2;     // x for EAST side anchors
		int xc = w / 2 + s / 2; // x for horizontal CENTER (NORTH/SOUTH) side anchors
		int yn = s / 2;         // y for NORTH side anchors
		int ys = h + s / 2;     // y for SOUTH side anchors
		int yc = h / 2 + s / 2; // y for vertical CENTER (WEST/EAST) side anchors

		anchors.clear();
		
		anchors.put(NORTH_WEST, new Rectangle(xw, yn, s, s));
		anchors.put(NORTH,      new Rectangle(xc, yn, s, s));
		anchors.put(NORTH_EAST, new Rectangle(xe, yn, s, s));
		anchors.put(WEST,       new Rectangle(xw, yc, s, s));
		anchors.put(EAST,       new Rectangle(xe, yc, s, s));
		anchors.put(SOUTH_WEST, new Rectangle(xw, ys, s, s));
		anchors.put(SOUTH,      new Rectangle(xc, ys, s, s));
		anchors.put(SOUTH_EAST, new Rectangle(xe, ys, s, s));

		anchors.values().forEach(r -> r.translate(shape.x - s, shape.y - s));
	}
	
	private boolean annotationSelectionEnabled() {
		return re.getViewModelSnapshot().getVisualProperty(DVisualLexicon.NETWORK_ANNOTATION_SELECTION);
	}
}
